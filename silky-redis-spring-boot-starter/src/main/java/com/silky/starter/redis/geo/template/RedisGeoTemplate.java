package com.silky.starter.redis.geo.template;

import com.silky.starter.redis.geo.enums.CoordinateSystem;
import com.silky.starter.redis.geo.enums.CoordinateUtils;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Redis 地理位置操作模板类
 *
 * @author zy
 * @date 2025-10-21 16:09
 **/
public class RedisGeoTemplate {

    private final StringRedisTemplate stringRedisTemplate;

    private final CoordinateSystem defaultCoordinateSystem;

    /**
     * 构造器（使用默认坐标系 WGS84）
     *
     * @param stringRedisTemplate StringRedisTemplate
     */
    public RedisGeoTemplate(StringRedisTemplate stringRedisTemplate) {
        this(stringRedisTemplate, CoordinateSystem.WGS84);
    }

    /**
     * 构造器
     *
     * @param stringRedisTemplate     StringRedisTemplate
     * @param defaultCoordinateSystem 默认坐标系
     */
    public RedisGeoTemplate(StringRedisTemplate stringRedisTemplate, CoordinateSystem defaultCoordinateSystem) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.defaultCoordinateSystem = defaultCoordinateSystem != null ? defaultCoordinateSystem : CoordinateSystem.WGS84;
    }


    /**
     * 添加地理位置（使用默认坐标系）
     *
     * @param key       键
     * @param longitude 经度
     * @param latitude  纬度
     * @param member    成员
     * @return 添加成功的成员数量
     */
    public Long add(String key, double longitude, double latitude, String member) {
        return add(key, longitude, latitude, member, defaultCoordinateSystem);
    }

    /**
     * 添加地理位置（指定坐标系）
     * <p>
     * 如果指定的坐标系不是 WGS84，会先将坐标转换为 WGS-84 再存储到 Redis。
     *
     * @param key              键
     * @param longitude        经度
     * @param latitude         纬度
     * @param member           成员
     * @param coordinateSystem 输入坐标的坐标系
     * @return 添加成功的成员数量
     */
    public Long add(String key, double longitude, double latitude, String member,
                    CoordinateSystem coordinateSystem) {
        double[] wgs84 = CoordinateUtils.toWgs84(coordinateSystem, longitude, latitude);
        return stringRedisTemplate.opsForGeo().add(key, new Point(wgs84[0], wgs84[1]), member);
    }

    /**
     * 批量添加地理位置（使用默认坐标系）
     *
     * @param key     键
     * @param members 成员及其对应的地理位置
     * @return 添加成功的成员数量
     */
    public Long addAll(String key, Map<String, Point> members) {
        return addAll(key, members, defaultCoordinateSystem);
    }

    /**
     * 批量添加地理位置（指定坐标系）
     * <p>
     * 如果指定的坐标系不是 WGS84，会先将所有坐标转换为 WGS-84 再存储到 Redis。
     *
     * @param key              键
     * @param members          成员及其对应的地理位置
     * @param coordinateSystem 输入坐标的坐标系
     * @return 添加成功的成员数量
     */
    public Long addAll(String key, Map<String, Point> members, CoordinateSystem coordinateSystem) {
        List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(members.size());
        for (Map.Entry<String, Point> entry : members.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            validateCoordinates(entry.getValue().getX(), entry.getValue().getY());
            double[] wgs84 = CoordinateUtils.toWgs84(coordinateSystem,
                    entry.getValue().getX(), entry.getValue().getY());
            locations.add(new RedisGeoCommands.GeoLocation<>(entry.getKey(),
                    new Point(wgs84[0], wgs84[1])));
        }
        return stringRedisTemplate.opsForGeo().add(key, locations);
    }

    /**
     * 获取地理位置（使用默认坐标系）
     *
     * @param key    键
     * @param member 成员
     * @return 坐标点（已转换为默认坐标系）
     */
    public Point get(String key, String member) {
        return get(key, member, defaultCoordinateSystem);
    }

    /**
     * 获取地理位置（指定输出坐标系）
     *
     * @param key              键
     * @param member           成员
     * @param coordinateSystem 输出坐标系
     * @return 坐标点（已转换为指定坐标系）
     */
    public Point get(String key, String member, CoordinateSystem coordinateSystem) {
        List<Point> points = stringRedisTemplate.opsForGeo().position(key, member);
        if (points == null || points.isEmpty() || points.get(0) == null) {
            return null;
        }
        Point wgs84Point = points.get(0);
        if (coordinateSystem == null || coordinateSystem == CoordinateSystem.WGS84) {
            return wgs84Point;
        }
        double[] converted = CoordinateUtils.fromWgs84(coordinateSystem,
                wgs84Point.getX(), wgs84Point.getY());
        return new Point(converted[0], converted[1]);
    }

    /**
     * 计算两个成员之间的距离（使用默认坐标系，距离不受坐标系影响）
     *
     * @param key     键
     * @param member1 成员1
     * @param member2 成员2
     * @param metric  距离单位
     * @return 距离
     */
    public Distance distance(String key, String member1, String member2, Metric metric) {
        return stringRedisTemplate.opsForGeo().distance(key, member1, member2, metric);
    }

    /**
     * 根据成员搜索附近的位置（使用默认坐标系）
     *
     * @param key    键
     * @param member 成员
     * @param radius 半径
     * @param metric 距离单位
     * @param limit  返回的成员数量
     * @return 搜索结果（坐标已转换为默认坐标系）
     */
    public GeoResults<RedisGeoCommands.GeoLocation<String>> radius(String key, String member,
                                                                   double radius, Metric metric,
                                                                   long limit) {
        return radius(key, member, radius, metric, limit, defaultCoordinateSystem);
    }

    /**
     * 根据成员搜索附近的位置（指定输出坐标系）
     *
     * @param key              键
     * @param member           成员
     * @param radius           半径
     * @param metric           距离单位
     * @param limit            返回的成员数量
     * @param coordinateSystem 输出坐标系
     * @return 搜索结果（坐标已转换为指定坐标系）
     */
    public GeoResults<RedisGeoCommands.GeoLocation<String>> radius(String key, String member,
                                                                   double radius, Metric metric,
                                                                   long limit,
                                                                   CoordinateSystem coordinateSystem) {
        Point centerPoint = get(key, member, CoordinateSystem.WGS84);
        if (centerPoint == null) {
            throw new IllegalArgumentException("Member '" + member + "' not found in key '" + key + "'");
        }
        Circle circle = new Circle(centerPoint, new Distance(radius, metric));
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending()
                .limit(limit);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                stringRedisTemplate.opsForGeo().radius(key, circle, args);
        return convertGeoResults(results, coordinateSystem);
    }

    /**
     * 根据坐标搜索附近的位置（使用默认坐标系）
     *
     * @param key       键
     * @param longitude 经度
     * @param latitude  纬度
     * @param radius    半径
     * @param metric    距离单位
     * @param limit     返回的成员数量
     * @return 搜索结果（坐标已转换为默认坐标系）
     */
    public GeoResults<RedisGeoCommands.GeoLocation<String>> radius(String key, double longitude,
                                                                   double latitude, double radius,
                                                                   Metric metric, long limit) {
        return radius(key, longitude, latitude, radius, metric, limit, defaultCoordinateSystem);
    }

    /**
     * 根据坐标搜索附近的位置（指定输入和输出坐标系）
     *
     * @param key              键
     * @param longitude        经度
     * @param latitude         纬度
     * @param radius           半径
     * @param metric           距离单位
     * @param limit            返回的成员数量
     * @param coordinateSystem 输入/输出坐标系
     * @return 搜索结果（坐标已转换为指定坐标系）
     */
    public GeoResults<RedisGeoCommands.GeoLocation<String>> radius(String key, double longitude,
                                                                   double latitude, double radius,
                                                                   Metric metric, long limit,
                                                                   CoordinateSystem coordinateSystem) {
        // 将输入坐标转换为 WGS84 用于 Redis 查询
        double[] wgs84 = CoordinateUtils.toWgs84(coordinateSystem, longitude, latitude);
        Point point = new Point(wgs84[0], wgs84[1]);
        Circle circle = new Circle(point, new Distance(radius, metric));
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending()
                .limit(limit);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                stringRedisTemplate.opsForGeo().radius(key, circle, args);
        return convertGeoResults(results, coordinateSystem);
    }

    /**
     * 获取 GEO Hash 值
     *
     * @param key     键
     * @param members 成员
     * @return GEO Hash 值列表
     */
    public List<String> hash(String key, String... members) {
        return stringRedisTemplate.opsForGeo().hash(key, members);
    }

    /**
     * 删除地理位置成员
     *
     * @param key     键
     * @param members 成员
     * @return 删除成功的成员数量
     */
    public Long remove(String key, String... members) {
        return stringRedisTemplate.opsForGeo().remove(key, members);
    }


    /**
     * 将 GeoResults 中的坐标从 WGS-84 转换为指定坐标系
     */
    private GeoResults<RedisGeoCommands.GeoLocation<String>> convertGeoResults(
            GeoResults<RedisGeoCommands.GeoLocation<String>> results,
            CoordinateSystem coordinateSystem) {
        if (results == null || coordinateSystem == null || coordinateSystem == CoordinateSystem.WGS84) {
            return results;
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> convertedContent = new ArrayList<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult : results) {
            RedisGeoCommands.GeoLocation<String> location = geoResult.getContent();
            Point originalPoint = location.getPoint();
            if (originalPoint != null) {
                double[] converted = CoordinateUtils.fromWgs84(coordinateSystem,
                        originalPoint.getX(), originalPoint.getY());
                RedisGeoCommands.GeoLocation<String> convertedLocation =
                        new RedisGeoCommands.GeoLocation<>(location.getName(),
                                new Point(converted[0], converted[1]));
                convertedContent.add(new GeoResult<>(convertedLocation, geoResult.getDistance()));
            } else {
                convertedContent.add(geoResult);
            }
        }
        return new GeoResults<>(convertedContent, results.getAverageDistance());
    }

    /**
     * 校验地理位置的坐标
     *
     * @param longitude 经度
     * @param latitude  纬度
     */
    private void validateCoordinates(double longitude, double latitude) {
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid longitude value: " + longitude);
        }
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Invalid latitude value: " + latitude);
        }
    }

    /**
     * 获取默认坐标系
     *
     * @return 默认坐标系
     */
    public CoordinateSystem getDefaultCoordinateSystem() {
        return defaultCoordinateSystem;
    }
}
