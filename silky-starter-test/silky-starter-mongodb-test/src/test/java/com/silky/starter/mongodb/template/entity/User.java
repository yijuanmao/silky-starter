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
    @Field(name = "user_id", order = 5)
    private Long id;

    /**
     * 用户名
     */
    @Field(name = "name", order = 6)
    private String name;

    /**
     * 密码
     */
    @Field(name = "password", order = 7)
    private String password;

    /**
     * 年龄
     */
    @Field(name = "age", order = 8)
    private int age;
}
