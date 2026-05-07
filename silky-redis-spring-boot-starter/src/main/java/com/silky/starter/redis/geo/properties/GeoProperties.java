package com.silky.starter.redis.geo.properties;

import com.silky.starter.redis.geo.enums.CoordinateSystem;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis Geo 配置属性
 *
 * @author zy
 * @date 2026-05-07
 **/
@Data
@ConfigurationProperties(prefix = "spring.redis.geo")
public class GeoProperties {

    /**
     * 全局坐标系，默认 WGS84（Redis GEO 底层存储坐标系）
     */
    private CoordinateSystem coordinateSystem = CoordinateSystem.WGS84;
}
