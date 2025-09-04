package com.silky.starter.mongodb.model.base;

import com.silky.starter.mongodb.annotation.CreateTime;
import com.silky.starter.mongodb.annotation.UpdateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * mongodb基础类
 *
 * @author zy
 * @date 2022-11-23 11:11
 **/
public class BaseMongodbModel implements Serializable {

    private static final long serialVersionUID = 6380370958571308646L;

    /**
     * 主键
     */
    @Id
    private String mongoId;

    /**
     * 创建时间
     */
    @CreateTime
    @Field(name = "create_time", order = 2)
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @UpdateTime
    @Field(name = "update_time", order = 3)
    private LocalDateTime updateTime;

    public String getMongoId() {
        return mongoId;
    }

    public void setMongoId(String mongoId) {
        this.mongoId = mongoId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "BaseMongodbModel{" +
                "mongoId='" + mongoId + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }

}
