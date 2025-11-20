package com.silky.starter.mongodb.model.base;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * mongodb基础类
 *
 * @author zy
 * @date 2025-10-23 11:11
 **/
@Data
@ToString
public class BaseMongodbModel implements Serializable {

    private static final long serialVersionUID = 6380370958571308646L;

    /**
     * 主键
     */
    @Id
    private String mongoId;

    /**
     * 保存到MongoDB创建时间
     */
    @Field(name = "create_time", order = 2)
    private LocalDateTime mgCreateTime;

    /**
     * 保存到MongoDB修改时间
     */
    @Field(name = "update_time", order = 3)
    private LocalDateTime mgUpdateTime;

}
