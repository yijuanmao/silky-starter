package com.silky.starter.redis.test.geo.service;

import com.silky.starter.redis.geo.enums.CoordinateSystem;
import com.silky.starter.redis.geo.template.RedisGeoTemplate;
import com.silky.starter.redis.test.RedisApplicationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 地理位置服务测试类
 *
 * @author zy
 * @date 2025-10-23 17:17
 **/
public class LocationService extends RedisApplicationTest {

    private static final String LOCATION_KEY = "silky:locations";

    @Autowired
    private RedisGeoTemplate redisGeoTemplate;

    /**
     * 添加地理位置（WGS84坐标系，默认）
     */
    @Test
    public void addLocationTest() {
        // 添加单个位置
        Long result = redisGeoTemplate.add(LOCATION_KEY, 116.405285, 39.904989, "北京");
        log.info("添加位置结果: {}", result);

        // 添加多个位置
        Map<String, Point> locations = new HashMap<>(5);
        locations.put("上海", new Point(121.472644, 31.231706));
        locations.put("广州", new Point(113.280637, 23.125178));
        locations.put("深圳", new Point(114.057868, 22.543099));

        Long batchResult = redisGeoTemplate.addAll(LOCATION_KEY, locations);
        log.info("批量添加位置结果: {}", batchResult);
    }

    /**
     * 添加地理位置（GCJ02坐标系 - 高德地图坐标）
     */
    @Test
    public void addLocationGcj02Test() {
        // 使用高德地图坐标添加（会自动转换为WGS84存储）
        Long result = redisGeoTemplate.add(LOCATION_KEY, 116.397428, 39.90923, "北京-GCJ02",
                CoordinateSystem.GCJ02);
        log.info("GCJ02坐标系添加位置结果: {}", result);

        Map<String, Point> locations = new HashMap<>(3);
        locations.put("上海-GCJ02", new Point(121.473701, 31.230416));
        locations.put("广州-GCJ02", new Point(113.264385, 23.129112));

        Long batchResult = redisGeoTemplate.addAll(LOCATION_KEY, locations, CoordinateSystem.GCJ02);
        log.info("GCJ02坐标系批量添加位置结果: {}", batchResult);
    }

    /**
     * 获取地理位置
     */
    @Test
    public void getLocationTest() {
        Point point = redisGeoTemplate.get(LOCATION_KEY, "北京");
        if (point != null) {
            log.info("北京坐标(WGS84): 经度={}, 纬度={}", point.getX(), point.getY());
        }
    }

    /**
     * 获取地理位置（指定坐标系）
     */
    @Test
    public void getLocationWithCoordinateSystemTest() {
        Point wgs84 = redisGeoTemplate.get(LOCATION_KEY, "北京", CoordinateSystem.WGS84);
        log.info("北京坐标(WGS84): ({}, {})", wgs84.getX(), wgs84.getY());

        Point gcj02 = redisGeoTemplate.get(LOCATION_KEY, "北京", CoordinateSystem.GCJ02);
        log.info("北京坐标(GCJ02): ({}, {})", gcj02.getX(), gcj02.getY());

        Point bd09 = redisGeoTemplate.get(LOCATION_KEY, "北京", CoordinateSystem.BD09);
        log.info("北京坐标(BD09): ({}, {})", bd09.getX(), bd09.getY());
    }

    /**
     * 计算距离
     */
    @Test
    public void calculateDistanceTest() {
        Distance distance = redisGeoTemplate.distance(
                LOCATION_KEY, "北京", "上海", Metrics.KILOMETERS
        );
        log.info("北京到上海距离: {} 公里", distance.getValue());
    }

    /**
     * 搜索附近位置（验证Distance不再是0）
     */
    @Test
    public void searchNearbyTest() {
        // 基于成员搜索
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisGeoTemplate.radius(LOCATION_KEY, "北京", 1000, Metrics.KILOMETERS, 10);

        log.info("北京1000公里内的城市:");
        results.forEach(geoResult -> {
            RedisGeoCommands.GeoLocation<String> location = geoResult.getContent();
            Distance distance = geoResult.getDistance();
            log.info("城市: {}, 距离: {} 公里", location.getName(), distance.getValue());
        });
    }

    /**
     * 搜索附近位置（使用GCJ02坐标系 - 高德地图场景）
     */
    @Test
    public void searchNearbyGcj02Test() {
        // 基于坐标搜索，输入/输出都使用GCJ02坐标系
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisGeoTemplate.radius(LOCATION_KEY, 116.397428, 39.90923,
                        500, Metrics.KILOMETERS, 10, CoordinateSystem.GCJ02);

        log.info("GCJ02坐标系搜索结果（高德坐标116.397428,39.90923附近500公里）:");
        results.forEach(geoResult -> {
            RedisGeoCommands.GeoLocation<String> location = geoResult.getContent();
            Distance distance = geoResult.getDistance();
            Point point = location.getPoint();
            log.info("城市: {}, 距离: {} 公里, 坐标(GCJ02): ({}, {})",
                    location.getName(), distance.getValue(), point.getX(), point.getY());
        });
    }

    /**
     * 基于坐标搜索附近位置
     */
    @Test
    public void searchNearbyByCoordinateTest() {
        // 基于坐标搜索
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisGeoTemplate.radius(LOCATION_KEY, 116.405285, 39.904989,
                        500, Metrics.KILOMETERS, 10);

        log.info("坐标(116.405285, 39.904989) 500公里内的城市:");
        results.forEach(geoResult -> {
            RedisGeoCommands.GeoLocation<String> location = geoResult.getContent();
            Distance distance = geoResult.getDistance();
            Point point = location.getPoint();
            log.info("城市: {}, 距离: {} 公里, 坐标: ({}, {})",
                    location.getName(), distance.getValue(), point.getX(), point.getY());
        });
    }

    /**
     * 获取GeoHash
     */
    @Test
    public void getGeoHashTest() {
        List<String> hashes = redisGeoTemplate.hash(LOCATION_KEY, "北京", "上海");
        log.info("GeoHash值 - 北京: {}, 上海: {}",
                hashes.get(0), hashes.get(1));
    }

    /**
     * 删除地理位置
     */
    @Test
    public void removeLocationTest() {
        Long result = redisGeoTemplate.remove(LOCATION_KEY, "北京-GCJ02", "上海-GCJ02", "广州-GCJ02");
        log.info("删除位置结果: {}", result);
    }
}
