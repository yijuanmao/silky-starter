package com.silky.starter.redis.geo.enums;

/**
 * 坐标系枚举
 *
 * @author zy
 * @date 2026-05-07
 **/
public enum CoordinateSystem {

    /**
     * WGS-84坐标系（GPS标准坐标系，国际通用）
     */
    WGS84,

    /**
     * GCJ-02坐标系（国测局坐标系，高德地图/腾讯地图使用）
     */
    GCJ02,

    /**
     * BD-09坐标系（百度坐标系，百度地图使用）
     */
    BD09
}
