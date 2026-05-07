package com.silky.starter.redis.geo.properties;

import com.silky.starter.redis.geo.enums.CoordinateSystem;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis Geo 配置属性
 * <p>
 * 在 application.yml 中配置示例：
 * <pre>
 * silky:
 *   redis:
 *     geo:
 *       coordinate-system: GCJ02
 * </pre>
 *
 * @author zy
 * @date 2026-05-07
 **/
@Data
@ConfigurationProperties(prefix = "silky.redis.geo")
public class GeoProperties {

    /**
     * 全局坐标系，默认 WGS84（Redis GEO 底层存储坐标系）
     * <p>
     * 可选值：WGS84、GCJ02、BD09
     * <ul>
     *   <li>WGS84 - GPS标准坐标系，国际地图使用</li>
     *   <li>GCJ02 - 国测局坐标系，高德地图/腾讯地图使用</li>
     *   <li>BD09  - 百度坐标系，百度地图使用</li>
     * </ul>
     */
    private CoordinateSystem coordinateSystem = CoordinateSystem.WGS84;
}
