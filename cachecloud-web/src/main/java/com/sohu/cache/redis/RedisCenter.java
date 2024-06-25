package com.sohu.cache.redis;

import com.sohu.cache.entity.*;
import com.sohu.cache.web.enums.BooleanEnum;
import com.sohu.cache.web.vo.RedisSlowLog;
import org.apache.commons.lang3.tuple.Pair;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.commands.ProtocolCommand;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * redis相关操作接口
 * Created by yijunzhang on 14-6-10.
 */
public interface RedisCenter {

    /**
     * 收集redis统计信息
     *
     * @param appId
     * @param host
     * @param port
     * @return
     */
    public Map<Object, Map<String, Object>> collectRedisInfo(long appId, long collectTime, String host,
                                                             int port);

    /**
     * 收集redis统计信息
     *
     * @param host
     * @param port
     * @return
     */
    public Map<Object, Map<String, Object>> getInfoStats(long appId, String host, int port);

    /**
     * 节点cluster info信息
     *
     * @param appId
     * @param host
     * @param port
     * @return
     */
    public Map<String, Object> getClusterInfoStats(long appId, String host, int port);

    /**
     * 节点cluster info信息
     *
     * @param appId
     * @param instanceInfo
     * @return
     */
    public Map<String, Object> getClusterInfoStats(long appId, InstanceInfo instanceInfo);

    /**
     * 根据ip和port判断redis实例当前是主还是从
     *
     * @param ip   ip
     * @param port port
     * @return 主返回true，从返回false,返回null代表未知；
     */
    public BooleanEnum isMaster(long appId, String ip, int port);

    /**
     * 判断实例是否为从节点，并且与主节点连接有效
     *
     * @param appDesc
     * @param slaveInstance
     * @param masterInstance
     * @return
     */
    public BooleanEnum isSlaveAndPointedMasterUp(AppDesc appDesc, InstanceInfo slaveInstance, InstanceInfo masterInstance);


    /**
     * 获取行数
     *
     * @param appId
     * @param ip
     * @param port
     * @return
     */
    long getDbSize(long appId, String ip, int port);


    Future<List<String>> findInstancePatternKeys(long appId, String ip, int port, String pattern);

    List<String> findInstanceBigKey(long appId, String ip, int port, long startBytes, long endBytes);

    List<String> findClusterBigKey(long appId, long startBytes, long endBytes);

    List<String> findInstanceIdleKeys(long appId, String ip, int port, long idleDays);

    List<String> findClusterIdleKeys(long appId, long idleDays);

    void delInstancePatternKeys(long appId, String ip, int port, String pattern);

    void delClusterPatternKey(long appId, String pattern);

    /**
     * 根据ip和port判断redis实例当前是否有从节点
     *
     * @param ip   ip
     * @param port port
     * @return 主返回true，从返回false；
     */
    public BooleanEnum hasSlaves(long appId, String ip, int port);

    /**
     * 获取从节点的主节点地址
     *
     * @param ip
     * @param port
     * @param password
     * @return
     */
    public HostAndPort getMaster(String ip, int port, String password);

    public HostAndPort getSlave0(String ip, int port, String password);

    /**
     * 判断实例是否运行
     *
     * @param appId
     * @param ip
     * @param port
     * @return
     */
    public boolean isRun(final long appId, String ip, int port);

    /**
     * 判断实例是否运行
     *
     * @param ip
     * @param port
     * @param retryTimes 重试次数
     * @return
     */
    public boolean isRun(final String ip, final int port, final int retryTimes);

    /**
     * 判断实例是否运行
     *
     * @param ip
     * @param port
     * @return
     */
    public boolean isRun(String ip, int port);

    /**
     * 判断实例是否运行
     *
     * @param ip
     * @param port
     * @param password
     * @return
     */
    public boolean isRun(String ip, int port, String password);

    /**
     * 下线指定实例
     *
     * @param ip
     * @param port
     * @return
     */
    public boolean shutdown(String ip, int port);

    /**
     * 下线指定实例
     *
     * @param appId
     * @param ip
     * @param port
     * @return
     */
    public boolean shutdown(long appId, String ip, int port);

    /**
     * 校验下线指定实例是否成功
     *
     * @param instanceInfo
     * @return
     */
    public boolean checkShutdownSuccess(InstanceInfo instanceInfo);

    /**
     * forget指定实例
     *
     * @param appId
     * @param ip
     * @param port
     * @param nodeClusterId
     * @return
     */
    public boolean forget(long appId, String ip, int port, String nodeClusterId);

    /**
     * 获取cluster myid
     *
     * @param ip
     * @param port
     * @return
     */
    public String getClusterMyId(long appId, String ip, int port);

    /**
     * 获取cluster nodes
     *
     * @param ip
     * @param port
     * @return
     */
    public String getClusterNodes(long appId, String ip, int port);

    /**
     * 执行redis命令返回结果
     *
     * @param appDesc
     * @param command
     * @param userName
     * @return
     */
    public String executeCommand(AppDesc appDesc, String command, String userName);

    /**
     * 实例执行redis命令
     *
     * @param appId
     * @param host
     * @param port
     * @param command
     * @return
     */
    public String executeCommand(long appId, String host, int port, String command);

    /**
     * 执行redis命令，无白名单限制，仅供管理员使用
     *
     * @param appDesc
     * @param command
     * @param args
     * @return
     */
    Object executeAdminCommand(AppDesc appDesc, ProtocolCommand command, String... args);


    /**
     * 实例执行redis命令，无白名单限制，仅供管理员使用
     *
     * @param appId
     * @param host
     * @param port
     * @param command
     * @param timeout
     * @return
     */
    String executeAdminCommand(long appId, String host, int port, String command, Integer timeout);

    /**
     * 执行redis命令， 无黑白名单限制，仅供管理员使用
     *
     * @param jedis
     * @param command
     * @param args
     * @return
     */
    Object executeAdminRedisCommandByJedis(Jedis jedis, ProtocolCommand command, String... args);

    /**
     * 获取jedisSentinelPool实例,必须是sentinel类型应用
     *
     * @param appDesc
     * @return
     */
    public JedisSentinelPool getJedisSentinelPool(AppDesc appDesc);

    /**
     * 获取redis实例配置信息
     *
     * @param instanceId
     * @return
     */
    public Map<String, String> getRedisConfigList(int instanceId);

    /**
     * 获取redis 命令列表
     * @param instanceId
     * @return
     */
    public List<String> getRedisCommand(int instanceId);

    /**
     * 获取重命名命令列表
     * @param instanceId
     * @return
     */
    List<Pair<String, String>> getConfigsInConfigFile(int instanceId, String configName);

    /**
     * 获取redis实例慢查询
     *
     * @param instanceId
     * @return
     */
    public List<RedisSlowLog> getRedisSlowLogs(int instanceId, int maxCount);

    /**
     * 获取client连接信息
     *
     * @param instanceId
     * @return
     */
    List<String> getClientList(int instanceId);

    List<Map<String, Object>> formatClientList(List<String> clientList);

    List<Map<String, Object>> getAppClientList(long appId, int condition);

    /**
     * 配置重写
     *
     * @return
     */
    public boolean configRewrite(final long appId, final String host, final int port);

    /**
     * 配置重写
     *
     * @param jedis
     * @return
     */
    public boolean configRewrite(Jedis jedis);

    /**
     * 获取maxmemory配置
     *
     * @param appId
     * @param host
     * @param port
     * @return
     */
    public Long getRedisMaxMemory(long appId, String host, int port);

    /**
     * 清理app数据
     *
     * @param appDesc
     * @param appUser
     * @return
     */
    public boolean cleanAppData(AppDesc appDesc, AppUser appUser);

    /**
     * 判断是否为孤立节点
     *
     * @param appId
     * @param host
     * @param port
     * @return
     */
    public boolean isSingleClusterNode(long appId, String host, int port);

    /**
     * 获取集群中失联的slots
     *
     * @param appId
     * @return
     */
    public Map<String, String> getClusterLossSlots(long appId);

    /**
     * 获取集群中失联的slots
     * @param appId
     * @param instanceInfo
     * @return
     */
    public Map<String, String> getClusterLossSlots(long appId, InstanceInfo instanceInfo);


        /**
         * 获取集群中失联的slots
         *
         * @param appId
         * @param host
         * @param port
         * @return
         */
    public List<Integer> getClusterLossSlots(long appId, String host, int port);

    /**
     * 获取集群中失联的slots
     *
     * @param appId
     * @param healthyHost
     * @param healthyPort
     * @param lossSlotsHost
     * @param lossSlotsPort
     * @return
     */
    public List<Integer> getInstanceSlots(long appId, String healthyHost, int healthyPort, String lossSlotsHost,
                                          int lossSlotsPort);

    /**
     * 从一个应用中获取一个健康的实例
     *
     * @param appId
     * @return
     */
    public InstanceInfo getHealthyInstanceInfo(long appId);

    /**
     * 从一个应用中获取所有健康的master实例
     *
     * @param appId
     * @return
     */
    public List<InstanceInfo> getAllHealthyInstanceInfo(long appId);

    /**
     * 收集redis延迟信息
     *
     * @param appId
     * @param collectTime
     * @param host
     * @param port
     * @return
     */
    List<InstanceLatencyHistory> collectRedisLatencyInfo(long appId, long collectTime, String host,
                                                         int port);

    /**
     * 收集redis慢查询日志
     *
     * @param appId
     * @param host
     * @param port
     * @return
     */
    public List<InstanceSlowLog> collectRedisSlowLog(long appId, long collectTime, String host,
                                                     int port);

    /**
     * 按照appid获取慢查询日志
     *
     * @param appId
     * @return
     */
    public List<InstanceSlowLog> getInstanceSlowLogByAppId(long appId);

    /**
     * 按照appid获取慢查询日志
     *
     * @param appId
     * @param startDate
     * @param endDate
     * @return
     */
    public List<InstanceSlowLog> getInstanceSlowLogByAppId(long appId, Date startDate, Date endDate);

    /**
     * 按照appid获取慢查询日志数关系
     *
     * @param appId
     * @param startDate
     * @param endDate
     * @return
     */
    public Map<String, Long> getInstanceSlowLogCountMapByAppId(Long appId, Date startDate, Date endDate);

    /**
     * 获取集群的slots分布
     *
     * @param appId
     * @return
     */
    Map<String, InstanceSlotModel> getClusterSlotsMap(long appId);


    /**
     * 获取集群slot分布
     * <K,V> -> <slot start-slot end, List<IP:PORT>>
     *
     * @param appId
     * @param instanceInfo
     * @return
     */
    Map<String, List<HostAndPort>> getClusterSlotMap(long appId, InstanceInfo instanceInfo);

    /**
     * 获取Redis版本
     *
     * @param appId
     * @param ip
     * @param port
     * @return
     */
    public String getRedisVersion(long appId, String ip, int port);

    /**
     * <p>
     * Description: 获取redis failover之后数据状态，判断是否failover完成
     * </p>
     *
     * @param ip   当前failover slave ip
     * @param port 当前failover slave port
     * @return false:定时轮询检测 true:检测完毕
     * @version 1.0
     * @date 2018/9/17
     */
    public Boolean getRedisReplicationStatus(long appId, String ip, int port);

    /**
     * @param appId
     * @param ip
     * @param port
     * @return
     */
    public Map<String, String> getRedisRoleAndMasterStatus(long appId, String ip, int port);

    /**
     * <p>
     * Description: 获取redis failover之后状态，判断是否failover完成
     * </p>
     *
     * @param ip   当前failover slave ip
     * @param port 当前failover slave port
     * @return false:定时轮询检测 true:检测完毕
     * @version 1.0
     * @date 2023/2/28
     */
    public Boolean getRedisFailoverForceStatus(long appId, String ip, int port);

    /**
     * 获取nodeId
     *
     * @param appId
     * @param ip
     * @param port
     * @return
     */
    public String getNodeId(long appId, String ip, int port);

    Jedis getJedis(String host, int port, String password);

    Jedis getAdminJedis(String host, int port, String password);

    Jedis getJedis(String host, int port);

    Jedis getJedis(long appId, String host, int port);

    Jedis getAdminJedis(long appId, String host, int port);

    Jedis getJedis(long appId, String host, int port, int connectionTimeout, int soTimeout);

    Jedis getAdminJedis(long appId, String host, int port, int connectionTimeout, int soTimeout);

    Jedis getJedis(String host, int port, String password, int connectionTimeout, int soTimeout);

    public boolean sendDeployRedisRelateCollectionMsg(long appId, String host, int port);

    /**
     * 检查配置nutcracker配置是否一致
     *
     * @param appId
     * @return
     */
    public boolean checkNutCrackerConfIsSame(long appId);

    /**
     * 检查nutcracker哈希是否一致
     *
     * @param appId
     * @param isDelete 检查后删除数据
     * @return
     */
    public List<InstanceInfo> checkNutCrackerHashIsSame(long appId, boolean isDelete);

    String configGet(long appId, String host, int port, String key);

    boolean configSetAndRewrite(long appId, String host, int port, String key, String value);

    //检测从节点是否准备OK
    boolean checkSlaveReady(long appId, String ip, int port, long offset);

    //检测从节点是否准备OK
    boolean checkSlaveReady(Jedis jedis, Long offset);

    //获取节点角色描述
    String getInstanceRole(Jedis jedis);

    //获取节点角色描述
    String getInstanceRole(long appId, String ip, int port);

    //检测bgsave是否完成
    boolean checkBgsaveFinish(Jedis jedis, int checkTimes);

    //检测load rdb是否完成
    boolean checkLoadFinish(Jedis jedis, int checkTimes);

    //获取rdb文件名
    String getRdbFileName(Jedis jedis);

}
