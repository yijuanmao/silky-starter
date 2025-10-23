package com.silky.starter.redis.test.geo;

import com.silky.starter.redis.test.RedisApplicationTest;
import com.silky.starter.redis.test.geo.service.BusinessGeoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 地理位置服务测试类
 *
 * @author zy
 * @date 2025-10-23 17:22
 **/
public class RedisGeoServiceTest extends RedisApplicationTest {
    @Autowired
    private BusinessGeoService businessGeoService;

    /**
     * 查找附近门店
     */
    @Test
    public void findNearbyStoresTest() {
        double userLongitude = 116.4000;
        double userLatitude = 39.9100;
        double radiusKm = 5.0;
        int limit = 3;

        List<BusinessGeoService.StoreVO> nearbyStores = businessGeoService.findNearbyStores(userLongitude, userLatitude, radiusKm, limit);

        log.info("附近门店列表:");
        for (BusinessGeoService.StoreVO store : nearbyStores) {
            log.info("门店ID: {}, 距离: {} km", store.getStoreId(), store.getDistance());
        }
    }

    /**
     * 计算配送距离
     */
    @Test
    public void testCalculateDeliveryDistance() {
        BusinessGeoService.DeliveryDistance deliveryDistance = businessGeoService.calculateDeliveryDistance("store_001", 116.4100, 39.9200);
        log.info("门店ID: {}, 配送距离: {} km", deliveryDistance.getStoreId(), deliveryDistance.getDistanceKm());
    }
}
