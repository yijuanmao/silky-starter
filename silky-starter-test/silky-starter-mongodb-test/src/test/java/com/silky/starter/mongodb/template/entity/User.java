package com.silky.starter.mongodb.template.entity;

import com.silky.starter.mongodb.model.base.BaseMongodbModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * 用户实体
 *
 * @author zy
 * @date 2025-10-23 16:14
 **/
@Data
@ToString
@Document("test_user")
@EqualsAndHashCode(callSuper = true)
public class User extends BaseMongodbModel {

    /**
     * 用户ID
     */
    @Field(name = "user_id")
    private Long id;

    /**
     * 用户名
     */
    @Field(name = "name")
    private String name;

    /**
     * 密码
     */
    @Field(name = "password")
    private String password;

    /**
     * 年龄
     */
    @Field(name = "age")
    private int age;

    /**
     * 创建时间
     */
//    @Field(name = "create_time")
    private LocalDateTime createTime;
}
