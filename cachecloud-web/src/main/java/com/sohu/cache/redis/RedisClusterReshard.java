package com.sohu.cache.redis;

import com.sohu.cache.constant.ReshardStatusEnum;
import com.sohu.cache.dao.AppDao;
import com.sohu.cache.dao.InstanceReshardProcessDao;
import com.sohu.cache.dao.ResourceDao;
import com.sohu.cache.entity.AppDesc;
import com.sohu.cache.entity.InstanceInfo;
import com.sohu.cache.entity.InstanceReshardProcess;
import com.sohu.cache.entity.SystemResource;
import com.sohu.cache.util.IdempotentConfirmer;
import com.sohu.cache.util.TypeUtil;
import com.sohu.cache.web.enums.BooleanEnum;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.MigrateParams;
import redis.clients.jedis.util.SafeEncoder;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 水平扩容重构
 *
 * @author leifu
 * @Date 2016年12月7日
 * @Time 上午10:13:00
 */
public class RedisClusterReshard {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Map<String, String> nodeIdCachedMap = new HashMap<String, String>();
    /**
     * migrate超时时间
     */
    private int migrateTimeout = 10000;
    /**
     * 普通jedis操作超时时间
     */
    private int defaultTimeout = Protocol.DEFAULT_TIMEOUT * 5;
    /**
     * 每次迁移key个数
     */
    private int migrateBatch = 10;
    /**
     * 所有有效节点
     */
    private Set<HostAndPort> hosts;
    /**
     * redis操作封装
     */
    private RedisCenter redisCenter;
    private InstanceReshardProcessDao instanceReshardProcessDao;
    private AppDao appDao;
    private ResourceDao resourceDao;

    public RedisClusterReshard(Set<HostAndPort> hosts, RedisCenter redisCenter, InstanceReshardProcessDao instanceReshardProcessDao) {
        this.hosts = hosts;
        this.redisCenter = redisCenter;
        this.instanceReshardProcessDao = instanceReshardProcessDao;
    }

    /**
     * <p>
     * Description: 需要获取应用密码
     * </p>
     *
     * @param
     * @return
     * @author chenshi
     * @version 1.0
     * @date 2017/12/25
     */
    public RedisClusterReshard(Set<HostAndPort> hosts, RedisCenter redisCenter, InstanceReshardProcessDao instanceReshardProcessDao, AppDao appDao, ResourceDao resourceDao) {
        this.hosts = hosts;
        this.redisCenter = redisCenter;
        this.instanceReshardProcessDao = instanceReshardProcessDao;
        this.appDao = appDao;
        this.resourceDao = resourceDao;
    }

    /**
     * 加入主从分片
     */
    public boolean joinCluster(long appId, String masterHost, int masterPort, final String slaveHost, final int slavePort) {
        Jedis mJedis = null;
        Jedis sJedis = null;
        try{
            //1. 确认主从节点是否正常
            mJedis = redisCenter.getAdminJedis(appId, masterHost, masterPort, defaultTimeout, defaultTimeout);
            final Jedis masterJedis = mJedis;
            boolean isRun = redisCenter.isRun(appId, masterHost, masterPort);
            if (!isRun) {
                logger.error(String.format("joinCluster: master host=%s,port=%s is not run", masterHost, masterPort));
                return false;
            }
            boolean hasSlave = StringUtils.isNotBlank(slaveHost) && slavePort > 0;
            sJedis = hasSlave ? redisCenter.getAdminJedis(appId, slaveHost, slavePort, defaultTimeout, defaultTimeout) : null;
            final Jedis slaveJedis = sJedis;
            if (hasSlave) {
                isRun = redisCenter.isRun(appId, slaveHost, slavePort);
                if (!isRun) {
                    logger.error(String.format("joinCluster: slave host=%s,port=%s is not run", slaveHost, slavePort));
                    return false;
                }
            }

            //2. 对主从节点进行meet操作
            //获取所有主节点
            List<HostAndPort> masterHostAndPostList = getMasterNodeList(appId);
            //meet master
            boolean isClusterMeet = clusterMeet(appId, masterHostAndPostList, masterHost, masterPort);
            if (!isClusterMeet) {
                logger.error("master isClusterMeet failed {}:{}", masterHost, masterPort);
                return false;
            }
            if (hasSlave) {
                isClusterMeet = clusterMeet(appId, masterHostAndPostList, slaveHost, slavePort);
                if (!isClusterMeet) {
                    logger.error("slave isClusterMeet failed {}:{}", slaveHost, slavePort);
                    return false;
                }
            }

            //3.复制
            if (hasSlave) {
                final String masterNodeId = getNodeId(appId, masterJedis);
                if (masterNodeId == null) {
                    logger.error(String.format("joinCluster:host=%s,port=%s nodeId is null", masterHost, masterPort));
                    return false;
                }
                return new IdempotentConfirmer() {
                    @Override
                    public boolean execute() {
                        try {
                            //等待广播节点
                            TimeUnit.SECONDS.sleep(2);
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                        String response = slaveJedis.clusterReplicate(masterNodeId);
                        logger.info("clusterReplicate-{}:{}={}", slaveHost, slavePort, response);
                        return response != null && response.equalsIgnoreCase("OK");
                    }
                }.run();
            } else {
                return true;
            }
        }finally {
            if(mJedis != null){
                mJedis.close();
            }
            if(sJedis != null){
                sJedis.close();
            }
        }
    }

    /**
     * 将source中的startSlot到endSlot迁移到target
     *
     */
//    public boolean migrateSlotOld(long appId, long appAuditId, InstanceInfo sourceInstanceInfo, InstanceInfo targetInstanceInfo, int startSlot, int endSlot, PipelineEnum pipelineEnum) {
//        long startTime = System.currentTimeMillis();
//        InstanceReshardProcess instanceReshardProcess = saveInstanceReshardProcess(appId, appAuditId, sourceInstanceInfo, targetInstanceInfo, startSlot, endSlot, pipelineEnum);
//        //源和目标Jedis
//        Jedis sourceJedis = redisCenter.getJedis(appId, sourceInstanceInfo.getIp(), sourceInstanceInfo.getPort(), defaultTimeout, defaultTimeout);
//        Jedis targetJedis = redisCenter.getJedis(appId, targetInstanceInfo.getIp(), targetInstanceInfo.getPort(), defaultTimeout, defaultTimeout);
//        //逐个slot迁移
//        boolean hasError = false;
//        for (int slot = startSlot; slot <= endSlot; slot++) {
//            long slotStartTime = System.currentTimeMillis();
//            try {
//                instanceReshardProcessDao.updateMigratingSlot(instanceReshardProcess.getId(), slot);
//                //num是迁移key的总数
//                int num = migrateSlotData(appId, sourceJedis, targetJedis, slot, pipelineEnum);
//                instanceReshardProcessDao.increaseFinishSlotNum(instanceReshardProcess.getId());
//                logger.warn("clusterReshard:{}->{}, slot={}, keys={}, costTime={} ms", sourceInstanceInfo.getHostPort(),
//                        targetInstanceInfo.getHostPort(), slot, num, (System.currentTimeMillis() - slotStartTime));
//            } catch (Exception e) {
//                logger.error(e.getMessage(), e);
//                hasError = true;
//                break;
//            }
//        }
//        long endTime = System.currentTimeMillis();
//        logger.warn("clusterReshard:{}->{}, slot:{}->{}, costTime={} ms", sourceInstanceInfo.getHostPort(),
//                targetInstanceInfo.getHostPort(), startSlot, endSlot, (endTime - startTime));
//        if (hasError) {
//            instanceReshardProcessDao.updateStatus(instanceReshardProcess.getId(), ReshardStatusEnum.ERROR.getValue());
//            return false;
//        } else {
//            instanceReshardProcessDao.updateStatus(instanceReshardProcess.getId(), ReshardStatusEnum.FINISH.getValue());
//            instanceReshardProcessDao.updateEndTime(instanceReshardProcess.getId(), new Date());
//            return true;
//        }
//    }

    /**
     * 节点meet
     *
     * @param masterHostAndPostList
     * @param host
     * @param port
     * @return
     */
    private boolean clusterMeet(long appId, List<HostAndPort> masterHostAndPostList, final String host, final int port) {
        boolean isSingleNode = redisCenter.isSingleClusterNode(appId, host, port);
        if (!isSingleNode) {
            logger.error("{}:{} isNotSingleNode", host, port);
            return false;
        } else {
            logger.warn("{}:{} isSingleNode", host, port);
        }
        for (HostAndPort hostAndPort : masterHostAndPostList) {
            String clusterHost = hostAndPort.getHost();
            int clusterPort = hostAndPort.getPort();
            final Jedis jedis = redisCenter.getAdminJedis(appId, clusterHost, clusterPort, defaultTimeout, defaultTimeout);
            //logger.info("jedis {}:{} meet singlenode",clusterHost,clusterPort);
            try {
                boolean isClusterMeet = new IdempotentConfirmer() {
                    @Override
                    public boolean execute() {
                        //将新节点添加到集群当中,成为集群中已知新节点
                        String meet = jedis.clusterMeet(host, port);
                        return meet != null && meet.equalsIgnoreCase("OK");
                    }
                }.run();
                if (isClusterMeet) {
                    return true;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                if (jedis != null)
                    jedis.close();
            }
        }
        return false;
    }

    /**
     * 将source中的startSlot到endSlot迁移到target
     */
    public boolean migrateSlot(InstanceReshardProcess instanceReshardProcess) {
        long appId = instanceReshardProcess.getAppId();
        AppDesc appDesc = appDao.getAppDescById(appId);
        String password = null;
        List<Integer> versionList = new ArrayList<>();
        boolean isRedisType = TypeUtil.isRedisType(appDesc.getType());
        if(appDesc != null && isRedisType){
            password = appDesc.getPasswordMd5();
            // Redis版本信息
            SystemResource resource = resourceDao.getResourceById(appDesc.getVersionId());
            String name = resource.getName();
            if(name != null){
                String[] split = name.split("-");
                if(split != null && split.length == 2){
                    String[] versionArray = split[1].split("\\.");
                    versionList = Arrays.asList(versionArray).stream().map(ver -> Integer.valueOf(ver)).collect(Collectors.toList());
                }
            }
        }
        int migratingSlot = instanceReshardProcess.getMigratingSlot();
        int endSlot = instanceReshardProcess.getEndSlot();
        int isPipeline = instanceReshardProcess.getIsPipeline();
        InstanceInfo sourceInstanceInfo = instanceReshardProcess.getSourceInstanceInfo();
        InstanceInfo targetInstanceInfo = instanceReshardProcess.getTargetInstanceInfo();

        long startTime = System.currentTimeMillis();

        //源和目标Jedis
        try(Jedis sourceJedis = redisCenter.getAdminJedis(appId, sourceInstanceInfo.getIp(), sourceInstanceInfo.getPort(), defaultTimeout, defaultTimeout);
            Jedis targetJedis = redisCenter.getAdminJedis(appId, targetInstanceInfo.getIp(), targetInstanceInfo.getPort(), defaultTimeout, defaultTimeout);){
            //逐个slot迁移
            boolean hasError = false;
            for (int slot = migratingSlot; slot <= endSlot; slot++) {
                long slotStartTime = System.currentTimeMillis();
                try {
                    instanceReshardProcessDao.updateMigratingSlot(instanceReshardProcess.getId(), slot);
                    //num是迁移key的总数
                    int num = 0;
                    if(isRedisType){
                        num = migrateSlotData(appId, sourceJedis, targetJedis, slot, isPipeline, password, versionList);
                    }
                    instanceReshardProcessDao.increaseFinishSlotNum(instanceReshardProcess.getId());
                    logger.warn("clusterReshard:{}->{}, slot={}, keys={}, costTime={} ms", sourceInstanceInfo.getHostPort(),
                            targetInstanceInfo.getHostPort(), slot, num, (System.currentTimeMillis() - slotStartTime));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    hasError = true;
                    break;
                }
            }
            long endTime = System.currentTimeMillis();
            logger.warn("clusterReshard:{}->{}, slot:{}->{}, costTime={} ms", sourceInstanceInfo.getHostPort(),
                    targetInstanceInfo.getHostPort(), migratingSlot, endSlot, (endTime - startTime));
            if (hasError) {
                instanceReshardProcessDao.updateStatus(instanceReshardProcess.getId(), ReshardStatusEnum.ERROR.getValue());
                return false;
            } else {
                instanceReshardProcessDao.updateStatus(instanceReshardProcess.getId(), ReshardStatusEnum.FINISH.getValue());
                instanceReshardProcessDao.updateEndTime(instanceReshardProcess.getId(), new Date());
                return true;
            }
        }
    }

    /**
     * 迁移slot数据，并稳定slot配置
     *
     * @throws Exception
     */
    private int moveSlotData(final long appId, final Jedis source, final Jedis target, final int slot, int isPipeline, String password, List<Integer> versionList) throws Exception {
        int num = 0;
        while (true) {
            final Set<String> keys = new HashSet<String>();
            boolean isGetKeysInSlot = new IdempotentConfirmer() {
                @Override
                public boolean execute() {
                    List<String> perKeys = source.clusterGetKeysInSlot(slot, migrateBatch);
                    if (perKeys != null && perKeys.size() > 0) {
                        keys.addAll(perKeys);
                    }
                    return true;
                }
            }.run();
            if (!isGetKeysInSlot) {
                throw new RuntimeException(String.format("get keys failed slot=%d num=%d", slot, num));
            }
            if (keys.isEmpty()) {
                break;
            }
            for (final String key : keys) {
                boolean isKeyMigrate = new IdempotentConfirmer() {
                    // 失败后，迁移时限加倍
                    private int migrateTimeOutFactor = 1;

                    @Override
                    public boolean execute() {
                        String response = null;
                        Integer bigVer = 0;
                        Integer smallVer = 0;
                        Integer fixVer = 0;
                        if(CollectionUtils.isNotEmpty(versionList)){
                            bigVer = versionList.get(0);
                            smallVer = versionList.get(1);
                            fixVer = versionList.get(2);
                        }
                        if(bigVer < 4 || (bigVer == 4 && smallVer == 0 && fixVer < 7)){
                            response = source.migrate(target.getClient().getHost(), target.getClient().getPort(),
                                    key, 0, migrateTimeout * (migrateTimeOutFactor++));
                        }else{
                            response = source.migrate(target.getClient().getHost(), target.getClient().getPort(),
                                    0, migrateTimeout * (migrateTimeOutFactor++), MigrateParams.migrateParams().auth(password), key);
                        }
                        return response != null && (response.equalsIgnoreCase("OK") || response.equalsIgnoreCase("NOKEY"));
                    }
                }.run();
                if (!isKeyMigrate) {
                    throw new RuntimeException("migrate key=" + key + failedInfo(source, slot));
                } else {
                    num++;
//                    logger.info("migrate key={};response=OK", key);
                }
            }
        }
        final String targetNodeId = getNodeId(appId, target);
        boolean isClusterSetSlotNode;
        //设置 slot新归属节点
        isClusterSetSlotNode = new IdempotentConfirmer() {
            @Override
            public boolean execute() {
                boolean isOk = false;
                List<HostAndPort> masterNodesList = getMasterNodeList(appId);
                // 增加cluster SETSLOT slot NODE node-id sequence
                // target, source, and optional other master nodes
                HostAndPort targetHostPort = new HostAndPort(target.getClient().getHost(), target.getClient().getPort());
                HostAndPort sourceHostPort = new HostAndPort(source.getClient().getHost(), source.getClient().getPort());
                masterNodesList = masterNodesList.stream().
                        filter(hostAndPort -> !hostAndPort.equals(targetHostPort) && !hostAndPort.equals(sourceHostPort))
                        .collect(Collectors.toList());
                masterNodesList.add(0, targetHostPort);
                masterNodesList.add(1, sourceHostPort);
                for (HostAndPort hostAndPort : masterNodesList) {
                    Jedis jedis = null;
                    try {
                        jedis = redisCenter.getAdminJedis(appId, hostAndPort.getHost(), hostAndPort.getPort());
                        String response = jedis.clusterSetSlotNode(slot, targetNodeId);
                        isOk = response != null && response.equalsIgnoreCase("OK");
                        if (!isOk) {
                            logger.error("clusterSetSlotNode-{}={}", getNodeId(appId, target), response);
                            break;
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    } finally {
                        if (jedis != null)
                            jedis.close();
                    }
                }
                return isOk;
            }
        }.run();
        if (!isClusterSetSlotNode) {
            throw new RuntimeException("clusterSetSlotNode:" + failedInfo(target, slot));
        }
        return num;
    }

    /**
     * 指派迁移节点数据
     * CLUSTER SETSLOT <slot> IMPORTING <node_id> 从 node_id 指定的节点中导入槽 slot 到本节点。
     * CLUSTER SETSLOT <slot> MIGRATING <node_id> 将本节点的槽 slot 迁移到 node_id 指定的节点中。
     * CLUSTER GETKEYSINSLOT <slot> <count> 返回 count 个 slot 槽中的键。
     * MIGRATE host port key destination-db timeout [COPY] [REPLACE]
     * CLUSTER SETSLOT <slot> NODE <node_id> 将槽 slot 指派给 node_id 指定的节点，如果槽已经指派给另一个节点，那么先让另一个节点删除该槽>，然后再进行指派。
     */
    private int migrateSlotData(long appId, final Jedis source, final Jedis target, final int slot, int isPipeline, String password, List<Integer> versionList) {
        int num = 0;
        final String sourceNodeId = getNodeId(appId, source);
        final String targetNodeId = getNodeId(appId, target);
        boolean isError = false;
        if (sourceNodeId == null || targetNodeId == null) {
            throw new JedisException(String.format("sourceNodeId = %s || targetNodeId = %s", sourceNodeId, targetNodeId));
        }
        boolean isImport = new IdempotentConfirmer() {
            @Override
            public boolean execute() {
                String importing = target.clusterSetSlotImporting(slot, sourceNodeId);
                logger.info("slot={},clusterSetSlotImporting={}", slot, importing);
                return importing != null && importing.equalsIgnoreCase("OK");
            }
        }.run();
        if (!isImport) {
            isError = true;
            logger.error("clusterSetSlotImporting" + failedInfo(target, slot));
        }
        boolean isMigrate = new IdempotentConfirmer() {
            @Override
            public boolean execute() {
                String migrating = source.clusterSetSlotMigrating(slot, targetNodeId);
                logger.info("slot={},clusterSetSlotMigrating={}", slot, migrating);
                return migrating != null && migrating.equalsIgnoreCase("OK");
            }
        }.run();

        if (!isMigrate) {
            isError = true;
            logger.error("clusterSetSlotMigrating" + failedInfo(source, slot));
        }

        try {
            num = moveSlotData(appId, source, target, slot, isPipeline, password, versionList);
        } catch (Exception e) {
            isError = true;
            logger.error(e.getMessage(), e);
        }
        if (!isError) {
            return num;
        } else {
            String errorMessage = "source=%s target=%s slot=%d num=%d reShard failed";
            throw new RuntimeException(String.format(errorMessage, getNodeKey(source), getNodeKey(target), slot, num));
        }
    }

    private String failedInfo(Jedis jedis, int slot) {
        return String.format(" failed %s:%d slot=%d", jedis.getClient().getHost(), jedis.getClient().getPort(), slot);
    }

    /**
     * 获取所有主节点
     *
     * @return
     */
    private List<HostAndPort> getMasterNodeList(long appId) {
        List<HostAndPort> masterNodeList = new ArrayList<HostAndPort>();
        /**
         * 获取RedisCluster所有节点：新加节点从jedisClusterInfoCache获取master node不全, 如果新加节点且没有分配slot,jedisClusterInfoCache node节点不会缓存该节点
         * 解决:直接遍历 host 节点,判断是否master节点
         */
        // 获取master节点列表
        try {
            for (HostAndPort host : hosts) {
                String ip = host.getHost();
                int port = host.getPort();
                if (redisCenter.isMaster(appId, ip, port) != BooleanEnum.TRUE) {
                    continue;
                }
                // 添加当前所有master节点
                masterNodeList.add(new HostAndPort(ip, port));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException("migrate get host:" + hosts + " masterNodelist error!");
        }

        return masterNodeList;
    }

    public String getNodeId(final long appId, final Jedis jedis) {
        String nodeKey = getNodeKey(jedis);
        if (nodeIdCachedMap.get(nodeKey) != null) {
            return nodeIdCachedMap.get(nodeKey);
        } else {
            String nodeId = redisCenter.getNodeId(appId, jedis.getClient().getHost(), jedis.getClient().getPort());
            nodeIdCachedMap.put(nodeKey, nodeId);
            return nodeId;
        }
    }

    protected String getNodeKey(Jedis jedis) {
        return jedis.getClient().getHost() + ":" + jedis.getClient().getPort();
    }

    public void setMigrateTimeout(int migrateTimeout) {
        this.migrateTimeout = migrateTimeout;
    }

    public void setDefaultTimeout(int defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    public void setInstanceReshardProcessDao(InstanceReshardProcessDao instanceReshardProcessDao) {
        this.instanceReshardProcessDao = instanceReshardProcessDao;
    }

    public void setRedisCenter(RedisCenter redisCenter) {
        this.redisCenter = redisCenter;
    }

    public void setAppDao(AppDao appDao) {
        this.appDao = appDao;
    }
}
