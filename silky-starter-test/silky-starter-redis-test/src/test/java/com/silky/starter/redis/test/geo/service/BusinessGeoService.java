package com.silky.starter.redis.test.geo.service;

import com.silky.starter.redis.geo.template.RedisGeoTemplate;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 地理位置业务服务
 *
 * @author zy
 * @date 2025-10-23 17:20
 **/
@Slf4j
@Service
public class BusinessGeoService {

    private static final String STORE_KEY = "silky:stores";
    private static final String DELIVERY_KEY = "silky:delivery";

    @Autowired
    private RedisGeoTemplate redisGeoTemplate;

    /**
     * 初始化门店位置数据
     */
    @PostConstruct
    public void initStoreLocations() {
        Map<String, Point> stores = new HashMap<>(6);
        stores.put("store_001", new Point(116.3974, 39.9093));  // 王府井店
        stores.put("store_002", new Point(116.3408, 39.9318));  // 西单店
        stores.put("store_003", new Point(116.4164, 39.9284));  // 朝阳门店
        stores.put("store_004", new Point(116.4864, 39.9933));  // 望京店

        redisGeoTemplate.addAll(STORE_KEY, stores);
        log.info("门店位置数据初始化完成");
    }

    /**
     * 查找附近门店
     */
    public List<StoreVO> findNearbyStores(double longitude, double latitude,
                                          double radiusKm, int limit) {
        GeoResults<RedisGeoCommands.GeoLocation<Object>> results =
                redisGeoTemplate.radius(STORE_KEY, longitude, latitude,
                        radiusKm, Metrics.KILOMETERS, limit);

        return results.getContent().stream()
                .map(geoResult -> {
                    RedisGeoCommands.GeoLocation<Object> location = geoResult.getContent();
                    Distance distance = geoResult.getDistance();

                    return StoreVO.builder()
                            .storeId((String) location.getName())
                            .distance(distance.getValue())
                            .distanceUnit("km")
                            .longitude(location.getPoint().getX())
                            .latitude(location.getPoint().getY())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 计算配送距离
     */
    public DeliveryDistance calculateDeliveryDistance(String storeId,
                                                      double userLongitude,
                                                      double userLatitude) {
        // 获取门店位置
        Point storePoint = redisGeoTemplate.get(STORE_KEY, storeId);
        if (storePoint == null) {
            throw new RuntimeException("门店不存在");
        }

        // 计算距离
        Distance distance = redisGeoTemplate.distance(STORE_KEY, storeId,
                String.valueOf(userLongitude + ":" + userLatitude), Metrics.KILOMETERS);

        return DeliveryDistance.builder()
                .storeId(storeId)
                .userLongitude(userLongitude)
                .userLatitude(userLatitude)
                .distanceKm(distance.getValue())
                .estimatedTime(calculateEstimatedTime(distance.getValue()))
                .build();
    }

    /**
     * 更新配送员实时位置
     */
    public void updateDeliveryLocation(String deliveryId,
                                       double longitude, double latitude) {
        redisGeoTemplate.add(DELIVERY_KEY, longitude, latitude, deliveryId);
        log.info("更新配送员位置: {}, ({}, {})", deliveryId, longitude, latitude);
    }

    /**
     * 查找最近的可分配配送员
     */
    public List<DeliveryPerson> findNearbyDeliveryPersons(double storeLongitude,
                                                          double storeLatitude,
                                                          double radiusKm) {
        GeoResults<RedisGeoCommands.GeoLocation<Object>> results =
                redisGeoTemplate.radius(DELIVERY_KEY, storeLongitude, storeLatitude,
                        radiusKm, Metrics.KILOMETERS, 10);

        return results.getContent().stream()
                .map(geoResult -> {
                    RedisGeoCommands.GeoLocation<Object> location = geoResult.getContent();
                    Distance distance = geoResult.getDistance();

                    return DeliveryPerson.builder()
                            .deliveryId((String) location.getName())
                            .distanceKm(distance.getValue())
                            .longitude(location.getPoint().getX())
                            .latitude(location.getPoint().getY())
                            .build();
                })
                .sorted(Comparator.comparing(DeliveryPerson::getDistanceKm))
                .collect(Collectors.toList());
    }

    private int calculateEstimatedTime(double distanceKm) {
        // 简单估算配送时间：2公里内30分钟，每增加1公里增加10分钟
        return (int) (30 + Math.max(0, distanceKm - 2) * 10);
    }

    @Data
    @Builder
    public static class StoreVO {
        private String storeId;
        private Double distance;
        private String distanceUnit;
        private Double longitude;
        private Double latitude;
    }

    @Data
    @Builder
    public static class DeliveryDistance {
        private String storeId;
        private Double userLongitude;
        private Double userLatitude;
        private Double distanceKm;
        private Integer estimatedTime;
    }

    @Data
    @Builder
    public static class DeliveryPerson {
        private String deliveryId;
        private Double distanceKm;
        private Double longitude;
        private Double latitude;
    }
}
