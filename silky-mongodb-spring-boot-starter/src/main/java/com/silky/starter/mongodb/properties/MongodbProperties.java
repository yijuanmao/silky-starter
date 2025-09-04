package com.silky.starter.mongodb.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.silky.starter.mongodb.properties.MongodbProperties.MONGO_DB_PROVIDER;

/**
 * Mongodb配置属性类
 *
 * @author zy
 * @date 2025-09-04 16:04
 **/
@ConfigurationProperties(prefix = MONGO_DB_PROVIDER)
public class MongodbProperties {

    public static final String MONGO_DB_PROVIDER = "silky.mongodb";

    /**
     * 是否打印Mongodb操作日志，默认不打印
     */
    private boolean printLog ;

    /**
     * 是否记录慢查询到数据库中，默认不记录
     */
    private boolean slowQuery;

    /**
     * 打印执行时间
     */
    protected Long slowTime;

    public boolean isPrintLog() {
        return printLog;
    }

    public void setPrintLog(boolean printLog) {
        this.printLog = printLog;
    }

    public boolean isSlowQuery() {
        return slowQuery;
    }

    public void setSlowQuery(boolean slowQuery) {
        this.slowQuery = slowQuery;
    }

    public Long getSlowTime() {
        return slowTime;
    }

    public void setSlowTime(Long slowTime) {
        this.slowTime = slowTime;
    }
}
