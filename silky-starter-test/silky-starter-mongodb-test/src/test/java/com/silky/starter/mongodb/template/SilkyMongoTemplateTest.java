package com.silky.starter.mongodb.template;

import cn.hutool.core.convert.Convert;
import com.silky.starter.mongodb.MongodbApplicationTest;
import com.silky.starter.mongodb.entity.PageResult;
import com.silky.starter.mongodb.support.LambdaQueryWrapper;
import com.silky.starter.mongodb.template.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SilkyMongoTemplate测试类
 *
 * @author: zy
 * @date: 2025-11-19
 */
public class SilkyMongoTemplateTest extends MongodbApplicationTest {

    @Autowired
    private SilkyMongoTemplate silkyMongoTemplate;

    /**
     * 保存用户
     */
    @Test
    public void testSave() {
        User user = new User();
        user.setName("John");
        user.setAge(25);
        user.setId(1L);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        silkyMongoTemplate.save(user);
    }

    /**
     * 批量保存用户
     */
    @Test
    public void testSaveBatch() {
        // 批量保存
        List<User> users = new ArrayList<>(20);
        for (int i = 1; i < 20; i++) {
            User user = new User();
            user.setName("John" + i);
            user.setAge(i);
            user.setId(Convert.toLong(i));
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            users.add(user);
        }
        silkyMongoTemplate.saveBatch(users);
    }

    /**
     * 查询用户列表
     */
    @Test
    public void testList() {
        // Lambda查询
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>(User.class)
                .eq(User::getName, "John")
                .gt(User::getAge, 20)
                .orderByDesc(User::getCreateTime);
        List<User> userList = silkyMongoTemplate.list(queryWrapper, User.class);
        log.info("用户列表: {}", userList);
    }

    /**
     * 分页查询
     */
    @Test
    public void testPage() {
        // Lambda查询
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>(User.class)
                .eq(User::getName, "John1")
//                .gt(User::getAge, 20)
                .orderByDesc(User::getCreateTime);
        // 分页查询
        PageResult<User> pageResult = silkyMongoTemplate.page(1, 10, queryWrapper, User.class);
        log.info("分页查询结果: {}", pageResult);
    }

    /**
     * 根据id查询
     */
    @Test
    public void testGetById() {
        User user = silkyMongoTemplate.getById("691ef184dba63c6bc7fd5617", User.class);
        log.info("查询用户详情: {}", user);
    }
}
