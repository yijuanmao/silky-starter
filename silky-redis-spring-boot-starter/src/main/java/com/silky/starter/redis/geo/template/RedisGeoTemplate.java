package com.silky.starter.redis.geo.template;

import cn.hutool.core.collection.CollUtil;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * redis地理位置操作模板类
 *
 * @author zy
 * @date 2025-10-21 16:09
 **/
public class RedisGeoTemplate {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisGeoTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 添加地理位置
     *
     * @param key       键
     * @param longitude 经度
     * @param latitude  纬度
     * @param member    成员
     * @return 添加成功的成员数量
     */
    public Long add(String key, double longitude, double latitude, String member) {
        return redisTemplate.opsForGeo().add(key, new Point(longitude, latitude), member);
    }

    /**
     * 批量添加地理位置
     *
     * @param key     键
     * @param members 成员及其对应的地理位置
     * @return 添加成功的成员数量
     */
    public Long addAll(String key, Map<String, Point> members) {
        List<RedisGeoCommands.GeoLocation<Object>> locations = new ArrayList<>(members.size());
        for (Map.Entry<String, Point> entry : members.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            validateCoordinates(entry.getValue().getX(), entry.getValue().getY());
            locations.add(new RedisGeoCommands.GeoLocation<>(entry.getKey(), entry.getValue()));
        }
        return redisTemplate.opsForGeo().add(key, locations);
    }

    /**
     * 获取地理位置
     *
     * @param key    键
     * @param member 成员
     */
    public Point get(String key, String member) {
        List<Point> points = redisTemplate.opsForGeo().position(key, member);
        return CollUtil.isEmpty(points) ? null : points.get(0);
    }

    /**
     * 计算距离
     *
     * @param key     键
     * @param member1 成员1
     * @param member2 成员2
     * @param metric  距离单位
     */
    public Distance distance(String key, String member1, String member2, Metric metric) {
        return redisTemplate.opsForGeo().distance(key, member1, member2, metric);
    }

    /**
     * 搜索附近的位置
     *
     * @param key    键
     * @param member 成员
     * @param radius 半径
     * @param metric 距离单位
     * @param limit  返回的成员数量
     */
    public GeoResults<RedisGeoCommands.GeoLocation<Object>> radius(String key, String member,
                                                                   double radius, Metric metric,
                                                                   long limit) {
        Circle circle = new Circle(get(key, member), new Distance(radius, metric));
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending()
                .limit(limit);

        return redisTemplate.opsForGeo().radius(key, circle, args);
    }

    /**
     * 根据坐标搜索附近的位置
     *
     * @param key       键
     * @param longitude 经度
     * @param latitude  纬度
     * @param radius    半径
     * @param metric    距离单位
     * @param limit     返回的成员数量
     */
    public GeoResults<RedisGeoCommands.GeoLocation<Object>> radius(String key, double longitude,
                                                                   double latitude, double radius,
                                                                   Metric metric, long limit) {
        Point point = new Point(longitude, latitude);
        Circle circle = new Circle(point, new Distance(radius, metric));
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending()
                .limit(limit);

        return redisTemplate.opsForGeo().radius(key, circle, args);
    }

    /**
     * 获取GEO Hash
     */
    public List<String> hash(String key, String... members) {
        return redisTemplate.opsForGeo().hash(key, members);
    }

    /**
     * 校验地理位置的坐标
     *
     * @param longitude 经度
     *                  latitude 纬度
     */
    private void validateCoordinates(double longitude, double latitude) {
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid longitude value: " + longitude);
        }
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Invalid latitude value: " + latitude);
        }
    }
}
