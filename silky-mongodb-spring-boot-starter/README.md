# silky-mongodb-spring-boot-starter

<p align="center">
  <img src="https://img.shields.io/badge/Silky%20Starter-MongoDB-brightgreen" alt="Silky Starter MongoDB"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7+-blue" alt="Spring Boot 2.7+"/>
  <img src="https://img.shields.io/badge/MongoDB-4.0+-orange" alt="MongoDB 4.0+"/>
  <img src="https://img.shields.io/badge/License-Apache%202.0-green" alt="License"/>
</p>

> 增强型 MongoDB 操作组件，支持多数据源、Lambda 条件构造器、读写分离、自动审计等功能。

## 特性

- **多数据源支持**：轻松配置和管理多个 MongoDB 数据源
- **Lambda 条件构造器**：类型安全的链式查询条件构建
- **读写分离**：自动路由读写操作到不同节点
- **自动审计**：自动填充创建时间、更新时间等字段
- **操作日志**：可选的 MongoDB 操作日志记录
- **事务支持**：简化 MongoDB 事务使用

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>top.silky</groupId>
    <artifactId>silky-mongodb-spring-boot-starter</artifactId>
    <version>1.0.0</version>  <!-- 建议使用最新版本 -->
</dependency>
```

### 2. 基础配置

```yaml
silky:
  mongodb:
    enabled: true
    # 主数据源名称
    primary: master
    # 是否启用事务
    transaction-enabled: false
    # 是否打印操作日志
    print-log: true
    # 慢查询时间阈值（毫秒）
    slow-time: 1000
    # 数据源配置
    datasource:
      master:
        uri: mongodb://username:password@localhost:27017/mydb
        database: mydb
        # 读写分离配置
        read-write-separation:
          enabled: true
          read-uri: mongodb://username:password@localhost:27018/mydb
          read-database: mydb
      slave:
        uri: mongodb://username:password@localhost:27019/mydb
        database: mydb
```

### 3. 实体类定义

```java
import com.silky.starter.mongodb.annotation.CreateTime;
import com.silky.starter.mongodb.annotation.UpdateTime;
import com.silky.starter.mongodb.model.base.BaseMongodbModel;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "user")
public class User extends BaseMongodbModel {
    
    @Id
    private String id;
    
    private String username;
    
    private String email;
    
    private Integer age;
    
    @CreateTime
    private LocalDateTime createTime;
    
    @UpdateTime
    private LocalDateTime updateTime;
}
```

## 核心API详解

### SilkyMongoTemplate 接口

`SilkyMongoTemplate` 是组件的核心接口，提供了增强的 MongoDB 操作方法。

#### 数据源操作

```java
public interface SilkyMongoTemplate {
    
    /**
     * 获取 MongoTemplate
     * 
     * @param dataSourceName 数据源名称
     * @param readOnly       是否只读
     * @return MongoTemplate
     */
    MongoTemplate getMongoTemplate(String dataSourceName, boolean readOnly);
    
    /**
     * 切换数据源
     * 切换后后续操作将使用指定数据源
     * 
     * @param dataSourceName 数据源名称
     */
    void switchDataSource(String dataSourceName);
    
    /**
     * 清空数据源
     * 清除当前线程的数据源绑定，恢复默认数据源
     */
    void clearDataSource();
    
    /**
     * 获取当前数据源名称
     * 
     * @return 数据源名称
     */
    String getCurrentDataSource();
}
```

#### 查询操作

```java
public interface SilkyMongoTemplate {
    
    /**
     * 根据 ID 查询
     * 
     * @param id          文档 ID
     * @param entityClass 实体类
     * @return 实体对象
     */
    <T> T getById(String id, Class<T> entityClass);
    
    /**
     * 查询所有数据
     * 
     * @param entityClass 实体类
     * @return 实体列表
     */
    <T> List<T> list(Class<T> entityClass);
    
    /**
     * 根据条件查询
     * 使用 LambdaQueryWrapper 构建条件
     * 
     * @param wrapper     查询条件
     * @param entityClass 实体类
     * @return 实体列表
     */
    <T> List<T> list(LambdaQueryWrapper<T> wrapper, Class<T> entityClass);
    
    /**
     * 查询单条数据
     * 
     * @param wrapper     查询条件
     * @param entityClass 实体类
     * @return 单个实体对象
     */
    <T> T getOne(LambdaQueryWrapper<T> wrapper, Class<T> entityClass);
    
    /**
     * 根据 ID 列表查询
     * 
     * @param ids         ID 列表
     * @param entityClass 实体类
     * @return 实体列表
     */
    <T> List<T> findByIds(Collection<String> ids, Class<T> entityClass);
}
```

#### 统计与判断

```java
public interface SilkyMongoTemplate {
    
    /**
     * 统计总数
     * 
     * @param entityClass 实体类
     * @return 数量
     */
    <T> long count(Class<T> entityClass);
    
    /**
     * 根据条件统计
     * 
     * @param wrapper     查询条件
     * @param entityClass 实体类
     * @return 数量
     */
    <T> long count(LambdaQueryWrapper<T> wrapper, Class<T> entityClass);
    
    /**
     * 判断是否存在
     * 
     * @param wrapper     查询条件
     * @param entityClass 实体类
     * @return true 表示存在
     */
    <T> boolean exists(LambdaQueryWrapper<T> wrapper, Class<T> entityClass);
    
    /**
     * 根据 ID 判断是否存在
     * 
     * @param id          ID
     * @param entityClass 实体类
     * @return true 表示存在
     */
    <T> boolean existsById(String id, Class<T> entityClass);
}
```

#### 分页查询

```java
public interface SilkyMongoTemplate {
    
    /**
     * 分页查询
     * 
     * @param current     当前页（从 1 开始）
     * @param size        每页数量
     * @param entityClass 实体类
     * @return 分页结果
     */
    <T> PageResult<T> page(long current, long size, Class<T> entityClass);
    
    /**
     * 使用 Query 对象分页查询
     * 
     * @param pageNum     当前页
     * @param size        每页数量
     * @param query       MongoDB Query 对象
     * @param entityClass 实体类
     * @return 分页结果
     */
    <T> PageResult<T> page(long pageNum, long size, Query query, Class<T> entityClass);
    
    /**
     * 使用 LambdaQueryWrapper 分页查询
     * 
     * @param pageNum     当前页
     * @param size        每页数量
     * @param wrapper     Lambda 条件构造器
     * @param entityClass 实体类
     * @return 分页结果
     */
    <T> PageResult<T> page(long pageNum, long size, LambdaQueryWrapper<T> wrapper, Class<T> entityClass);
}
```

#### 插入操作

```java
public interface SilkyMongoTemplate {
    
    /**
     * 保存单个实体
     * 自动填充创建时间、更新时间
     * 
     * @param entity 实体对象
     * @return 保存后的实体（包含生成的 ID）
     */
    <T> T save(T entity);
    
    /**
     * 批量保存
     * 
     * @param entityList 实体列表
     * @return 保存后的实体列表
     */
    <T> List<T> saveBatch(Collection<T> entityList);
    
    /**
     * 批量保存（指定实体类）
     * 
     * @param entityList  实体列表
     * @param entityClass 实体类
     * @return 保存后的实体列表
     */
    <T> List<T> saveBatch(Collection<T> entityList, Class<T> entityClass);
}
```

#### 更新操作

```java
public interface SilkyMongoTemplate {
    
    /**
     * 根据 ID 更新
     * 自动填充更新时间
     * 
     * @param entity 实体对象（需包含 ID）
     * @return true 表示更新成功
     */
    <T> boolean updateById(T entity);
    
    /**
     * 根据条件更新
     * 
     * @param queryWrapper  查询条件
     * @param updateWrapper 更新条件
     * @param entityClass   实体类
     * @return true 表示更新成功
     */
    <T> boolean update(LambdaQueryWrapper<T> queryWrapper,
                       LambdaUpdateWrapper<T> updateWrapper, Class<T> entityClass);
    
    /**
     * 批量更新
     * 更新所有匹配条件的文档
     * 
     * @param query       查询条件
     * @param update      更新内容
     * @param entityClass 实体类
     * @return 更新的文档数量
     */
    <T> long updateMulti(Query query, Update update, Class<T> entityClass);
    
    /**
     * 查询并更新（原子操作）
     * 
     * @param query       查询条件
     * @param update      更新内容
     * @param entityClass 实体类
     * @return 更新后的实体
     */
    <T> T findAndModify(LambdaQueryWrapper<T> query, LambdaUpdateWrapper<T> update, Class<T> entityClass);
}
```

#### 删除操作

```java
public interface SilkyMongoTemplate {
    
    /**
     * 根据 ID 删除
     * 
     * @param id          文档 ID
     * @param entityClass 实体类
     * @return true 表示删除成功
     */
    <T> boolean removeById(String id, Class<T> entityClass);
    
    /**
     * 根据条件删除
     * 
     * @param wrapper     查询条件
     * @param entityClass 实体类
     * @return true 表示删除成功
     */
    <T> boolean remove(LambdaQueryWrapper<T> wrapper, Class<T> entityClass);
    
    /**
     * 批量删除
     * 
     * @param ids         ID 列表
     * @param entityClass 实体类
     * @return 删除的文档数量
     */
    <T> long removeBatch(Collection<String> ids, Class<T> entityClass);
}
```

#### 集合操作

```java
public interface SilkyMongoTemplate {
    
    /**
     * 创建集合
     * 
     * @param entityClass 实体类
     */
    <T> void createCollection(Class<T> entityClass);
    
    /**
     * 删除集合
     * 
     * @param entityClass 实体类
     */
    <T> void dropCollection(Class<T> entityClass);
}
```

### LambdaQueryWrapper 条件构造器

`LambdaQueryWrapper` 提供了类型安全的条件构建方式。

#### 基础条件

```java
public class LambdaQueryWrapper<T> {
    
    /**
     * 等于
     * 
     * @param column 字段（Lambda 表达式）
     * @param value  值
     */
    public LambdaQueryWrapper<T> eq(SFunction<T, ?> column, Object value);
    
    /**
     * 不等于
     * 
     * @param column 字段
     * @param value  值
     */
    public LambdaQueryWrapper<T> ne(SFunction<T, ?> column, Object value);
    
    /**
     * 模糊查询
     * 
     * @param column 字段
     * @param value  值
     */
    public LambdaQueryWrapper<T> like(SFunction<T, ?> column, String value);
    
    /**
     * IN 查询
     * 
     * @param column 字段
     * @param values 值集合
     */
    public LambdaQueryWrapper<T> in(SFunction<T, ?> column, Collection<?> values);
    
    /**
     * 大于
     * 
     * @param column 字段
     * @param value  值
     */
    public LambdaQueryWrapper<T> gt(SFunction<T, ?> column, Object value);
    
    /**
     * 小于
     * 
     * @param column 字段
     * @param value  值
     */
    public LambdaQueryWrapper<T> lt(SFunction<T, ?> column, Object value);
}
```

#### 排序

```java
public class LambdaQueryWrapper<T> {
    
    /**
     * 升序排序
     * 
     * @param column 字段
     */
    public LambdaQueryWrapper<T> orderByAsc(SFunction<T, ?> column);
    
    /**
     * 降序排序
     * 
     * @param column 字段
     */
    public LambdaQueryWrapper<T> orderByDesc(SFunction<T, ?> column);
}
```

### LambdaUpdateWrapper 更新构造器

```java
public class LambdaUpdateWrapper<T> {
    
    /**
     * 设置字段值
     * 
     * @param column 字段
     * @param value  值
     */
    public LambdaUpdateWrapper<T> set(SFunction<T, ?> column, Object value);
    
    /**
     * 自增操作
     * 
     * @param column 字段
     * @param value  增加值
     */
    public LambdaUpdateWrapper<T> inc(SFunction<T, ?> column, Number value);
}
```

## 使用示例

### 基础CRUD操作

```java
import com.silky.starter.mongodb.template.SilkyMongoTemplate;
import com.silky.starter.mongodb.support.LambdaQueryWrapper;
import com.silky.starter.mongodb.support.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    
    @Autowired
    private SilkyMongoTemplate mongoTemplate;
    
    // 新增用户
    @PostMapping
    public User createUser(@RequestBody User user) {
        return mongoTemplate.save(user);
    }
    
    // 根据ID查询
    @GetMapping("/{id}")
    public User getById(@PathVariable String id) {
        return mongoTemplate.getById(id, User.class);
    }
    
    // 查询所有
    @GetMapping
    public List<User> list() {
        return mongoTemplate.list(User.class);
    }
    
    // 根据ID更新
    @PutMapping
    public boolean updateById(@RequestBody User user) {
        return mongoTemplate.updateById(user);
    }
    
    // 根据ID删除
    @DeleteMapping("/{id}")
    public boolean deleteById(@PathVariable String id) {
        return mongoTemplate.removeById(id, User.class);
    }
}
```

### 条件查询示例

```java
@RestController
@RequestMapping("/users")
public class UserController {
    
    @Autowired
    private SilkyMongoTemplate mongoTemplate;
    
    // 条件查询 - 等于
    @GetMapping("/by-username")
    public User getByUsername(@RequestParam String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>(User.class)
                .eq(User::getUsername, username);
        return mongoTemplate.getOne(wrapper, User.class);
    }
    
    // 条件查询 - 模糊查询
    @GetMapping("/search")
    public List<User> searchByUsername(@RequestParam String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>(User.class)
                .like(User::getUsername, keyword);
        return mongoTemplate.list(wrapper, User.class);
    }
    
    // 条件查询 - 多条件组合
    @GetMapping("/advanced-search")
    public List<User> advancedSearch(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge) {
        
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>(User.class);
        
        if (username != null) {
            wrapper.like(User::getUsername, username);
        }
        if (minAge != null) {
            wrapper.gt(User::getAge, minAge);
        }
        if (maxAge != null) {
            wrapper.lt(User::getAge, maxAge);
        }
        
        // 按创建时间降序
        wrapper.orderByDesc(User::getCreateTime);
        
        return mongoTemplate.list(wrapper, User.class);
    }
    
    // IN 查询
    @PostMapping("/by-ids")
    public List<User> getByIds(@RequestBody List<String> ids) {
        return mongoTemplate.findByIds(ids, User.class);
    }
    
    // 判断是否存在
    @GetMapping("/exists")
    public boolean exists(@RequestParam String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>(User.class)
                .eq(User::getUsername, username);
        return mongoTemplate.exists(wrapper, User.class);
    }
}
```

### 分页查询示例

```java
@RestController
@RequestMapping("/users")
public class UserController {
    
    @Autowired
    private SilkyMongoTemplate mongoTemplate;
    
    // 基础分页
    @GetMapping("/page")
    public PageResult<User> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return mongoTemplate.page(current, size, User.class);
    }
    
    // 带条件的分页
    @GetMapping("/page-search")
    public PageResult<User> pageSearch(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer minAge) {
        
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>(User.class);
        
        if (username != null) {
            wrapper.like(User::getUsername, username);
        }
        if (minAge != null) {
            wrapper.gt(User::getAge, minAge);
        }
        
        wrapper.orderByDesc(User::getCreateTime);
        
        return mongoTemplate.page(current, size, wrapper, User.class);
    }
}
```

### 更新操作示例

```java
@RestController
@RequestMapping("/users")
public class UserController {
    
    @Autowired
    private SilkyMongoTemplate mongoTemplate;
    
    // 根据ID更新（全字段）
    @PutMapping("/{id}")
    public boolean updateById(@PathVariable String id, @RequestBody User user) {
        user.setId(id);
        return mongoTemplate.updateById(user);
    }
    
    // 条件更新 - 更新指定字段
    @PutMapping("/update-age")
    public boolean updateAgeByUsername(
            @RequestParam String username,
            @RequestParam int age) {
        
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>(User.class)
                .eq(User::getUsername, username);
        
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>()
                .set(User::getAge, age);
        
        return mongoTemplate.update(queryWrapper, updateWrapper, User.class);
    }
    
    // 自增操作
    @PutMapping("/increment-age/{id}")
    public boolean incrementAge(@PathVariable String id) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>(User.class)
                .eq(User::getId, id);
        
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>()
                .inc(User::getAge, 1);
        
        return mongoTemplate.update(queryWrapper, updateWrapper, User.class);
    }
    
    // 批量更新
    @PutMapping("/update-multi")
    public long updateMulti(@RequestParam int oldAge, @RequestParam int newAge) {
        Query query = new Query(Criteria.where("age").is(oldAge));
        Update update = new Update().set("age", newAge);
        
        return mongoTemplate.updateMulti(query, update, User.class);
    }
}
```

### 多数据源切换示例

```java
@Service
public class UserService {
    
    @Autowired
    private SilkyMongoTemplate mongoTemplate;
    
    // 使用注解切换数据源
    @DataSource("slave")
    public List<User> queryFromSlave() {
        return mongoTemplate.list(User.class);
    }
    
    // 手动切换数据源
    public List<User> queryFromMasterThenSlave() {
        try {
            // 切换到主库查询
            mongoTemplate.switchDataSource("master");
            List<User> masterUsers = mongoTemplate.list(User.class);
            
            // 切换到从库查询
            mongoTemplate.switchDataSource("slave");
            List<User> slaveUsers = mongoTemplate.list(User.class);
            
            return slaveUsers;
        } finally {
            // 清除数据源绑定，恢复默认
            mongoTemplate.clearDataSource();
        }
    }
}
```

### 批量操作示例

```java
@Service
public class UserService {
    
    @Autowired
    private SilkyMongoTemplate mongoTemplate;
    
    // 批量插入
    public List<User> batchInsert(List<User> users) {
        return mongoTemplate.saveBatch(users);
    }
    
    // 批量删除
    public long batchDelete(List<String> ids) {
        return mongoTemplate.removeBatch(ids, User.class);
    }
}
```

## 注解说明

### @DataSource 数据源切换

```java
@Service
public class OrderService {
    
    // 使用 slave 数据源
    @DataSource("slave")
    public List<Order> queryOrders() {
        // 此方法内的所有 MongoDB 操作都使用 slave 数据源
        return mongoTemplate.list(Order.class);
    }
}
```

### @CreateTime 创建时间

```java
@Data
@Document(collection = "user")
public class User {
    
    @CreateTime
    private LocalDateTime createTime;  // 插入时自动填充
}
```

### @UpdateTime 更新时间

```java
@Data
@Document(collection = "user")
public class User {
    
    @UpdateTime
    private LocalDateTime updateTime;  // 插入和更新时自动填充
}
```

### @ReadOnly 只读操作

```java
@Service
public class ReportService {
    
    // 标记为只读操作，会自动路由到读节点
    @ReadOnly
    public List<Report> generateReport() {
        return mongoTemplate.list(Report.class);
    }
}
```

### @IgnoreColumn 忽略字段

```java
@Data
@Document(collection = "user")
public class User {
    
    private String username;
    
    @IgnoreColumn
    private String tempField;  // 该字段不会保存到 MongoDB
}
```

## 配置属性详解

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `silky.mongodb.enabled` | true | 是否启用 MongoDB 组件 |
| `silky.mongodb.primary` | - | 主数据源名称 |
| `silky.mongodb.transaction-enabled` | false | 是否启用事务支持 |
| `silky.mongodb.print-log` | false | 是否打印操作日志 |
| `silky.mongodb.slow-time` | - | 慢查询时间阈值（毫秒） |
| `silky.mongodb.datasource.{name}.uri` | - | 数据源连接 URI |
| `silky.mongodb.datasource.{name}.database` | - | 数据库名称 |
| `silky.mongodb.datasource.{name}.read-write-separation.enabled` | false | 是否启用读写分离 |
| `silky.mongodb.datasource.{name}.read-write-separation.read-uri` | - | 读节点 URI |
| `silky.mongodb.datasource.{name}.read-write-separation.read-database` | - | 读节点数据库名 |

## 常见问题

### Q1: 如何配置多个数据源？

A: 在配置文件中定义多个数据源：

```yaml
silky:
  mongodb:
    primary: master
    datasource:
      master:
        uri: mongodb://localhost:27017/db1
        database: db1
      slave1:
        uri: mongodb://localhost:27018/db1
        database: db1
      slave2:
        uri: mongodb://localhost:27019/db2
        database: db2
```

### Q2: 读写分离如何工作？

A: 启用读写分离后，写操作自动路由到主节点，读操作路由到从节点：

```yaml
silky:
  mongodb:
    datasource:
      master:
        uri: mongodb://master:27017/mydb
        read-write-separation:
          enabled: true
          read-uri: mongodb://slave:27017/mydb
```

代码中无需特殊处理，组件会自动路由。

### Q3: 事务如何使用？

A: 需要先启用事务支持：

```yaml
silky:
  mongodb:
    transaction-enabled: true
```

然后使用 Spring 的 `@Transactional` 注解：

```java
@Service
public class OrderService {
    
    @Transactional
    public void createOrder(Order order) {
        // 插入订单
        mongoTemplate.save(order);
        // 扣减库存
        inventoryService.decrease(order.getProductId(), order.getQuantity());
    }
}
```

### Q4: LambdaQueryWrapper 支持哪些条件？

A: 目前支持的条件包括：
- `eq` - 等于
- `ne` - 不等于
- `like` - 模糊查询
- `in` - IN 查询
- `gt` - 大于
- `lt` - 小于
- `orderByAsc` - 升序排序
- `orderByDesc` - 降序排序

## 相关链接

- [GitHub 仓库](https://github.com/yijuanmao/silky-starter)
- [Gitee 仓库](https://gitee.com/zeng_er/silky-starter)
- [Spring Data MongoDB 文档](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)

## License

Apache License 2.0
