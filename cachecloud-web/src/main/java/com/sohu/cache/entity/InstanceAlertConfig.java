package com.sohu.cache.entity;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSONObject;
import com.sohu.cache.redis.enums.InstanceAlertCheckCycleEnum;
import com.sohu.cache.redis.enums.InstanceAlertCompareTypeEnum;
import com.sohu.cache.redis.enums.InstanceAlertTypeEnum;
import lombok.Data;

/**
 * 实例报警阀值配置
 * @author leifu
 */
@Data
public class InstanceAlertConfig {

    /**
     * 自增id
     */
    private long id;

    /**
     * 报警配置
     */
    private String alertConfig;

    /**
     * 报警阀值
     */
    private String alertValue;

    /**
     * 详见CompareTypeEnumNew
     */
    private int compareType;

    /**
     * 配置说明
     */
    private String configInfo;

    /**
     * 详见TypeEnum
     */
    private int type;

    /**
     * -1全局配置，其他代表实例id
     */
    private long instanceId;

    /**
     * 实例信息
     */
    private InstanceInfo instanceInfo;

    /**
     * 相关StatusEnum
     */
    private int status;

    /**
     * 详见CheckCycleEnum
     */
    private int checkCycle;

    /**
     * 配置更新时间
     */
    private Date updateTime;

    /**
     * 上次检测时间
     */
    private Date lastCheckTime;

    /**
     * 重要度（参照ImportantLevelTypeEnum）（0:一般；1：重要；2：紧急）
     */
    private Integer importantLevel;

    /**
     * 应用类型（0：redis;)
     */
    private Integer appType;

    public Date getUpdateTime() {
        return (Date) updateTime.clone();
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = (Date) updateTime.clone();
    }

    public Date getLastCheckTime() {
        return (Date) lastCheckTime.clone();
    }

    public void setLastCheckTime(Date lastCheckTime) {
        this.lastCheckTime = (Date) lastCheckTime.clone();
    }

    public String getCompareInfo() {
        return InstanceAlertCompareTypeEnum.getInstanceAlertCompareTypeEnum(compareType).getInfo();
    }

    public Long getCheckCycleMillionTime() {
        if (InstanceAlertCheckCycleEnum.ONE_MINUTE.getValue() == checkCycle) {
            return TimeUnit.MINUTES.toMillis(1);
        } else if (InstanceAlertCheckCycleEnum.FIVE_MINUTE.getValue() == checkCycle) {
            return TimeUnit.MINUTES.toMillis(5);
        } else if (InstanceAlertCheckCycleEnum.HALF_HOUR.getValue() == checkCycle) {
            return TimeUnit.MINUTES.toMillis(30);
        } else if (InstanceAlertCheckCycleEnum.ONE_HOUR.getValue() == checkCycle) {
            return TimeUnit.MINUTES.toMillis(60);
        } else if (InstanceAlertCheckCycleEnum.ONE_DAY.getValue() == checkCycle) {
            return TimeUnit.DAYS.toMillis(1);
        }
        return null;
    }

    public boolean isSpecail() {
        return instanceId > 0 &&
                (type == InstanceAlertTypeEnum.INSTANCE_ALERT.getValue() ||
                        type == InstanceAlertTypeEnum.APP_ALERT.getValue());
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }


}
