package com.silky.starter.redis.geo.enums;

/**
 * 坐标转换工具类
 * <p>
 * 支持以下坐标系之间的相互转换：
 * <ul>
 *   <li>WGS84 - WGS-84坐标系（GPS标准坐标系）</li>
 *   <li>GCJ02 - GCJ-02坐标系（国测局坐标系，高德/腾讯地图）</li>
 *   <li>BD09  - BD-09坐标系（百度地图）</li>
 * </ul>
 * <p>
 * Redis GEO 底层使用 WGS-84 坐标系存储，因此：
 * <ul>
 *   <li>使用 GCJ02/BD09 添加位置时，会先转换为 WGS-84 再存储</li>
 *   <li>查询位置时，会从 WGS-84 转换为配置的坐标系再返回</li>
 * </ul>
 *
 * @author zy
 * @date 2026-05-07
 **/
public class CoordinateUtils {

    private static final double A = 6378245.0;
    private static final double EE = 0.00669342162296594323;
    private static final double PI = Math.PI;
    private static final double X_PI = PI * 3000.0 / 180.0;

    private CoordinateUtils() {
    }

    /**
     * WGS84 转 GCJ02
     *
     * @param longitude WGS84经度
     * @param latitude  WGS84纬度
     * @return double[0]=经度, double[1]=纬度
     */
    public static double[] wgs84ToGcj02(double longitude, double latitude) {
        if (outOfChina(longitude, latitude)) {
            return new double[]{longitude, latitude};
        }
        double dLat = transformLat(longitude - 105.0, latitude - 35.0);
        double dLon = transformLon(longitude - 105.0, latitude - 35.0);
        double radLat = latitude / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLon = (dLon * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
        return new double[]{longitude + dLon, latitude + dLat};
    }

    /**
     * GCJ02 转 WGS84
     *
     * @param longitude GCJ02经度
     * @param latitude  GCJ02纬度
     * @return double[0]=经度, double[1]=纬度
     */
    public static double[] gcj02ToWgs84(double longitude, double latitude) {
        if (outOfChina(longitude, latitude)) {
            return new double[]{longitude, latitude};
        }
        double[] gcj = wgs84ToGcj02(longitude, latitude);
        double dx = gcj[0] - longitude;
        double dy = gcj[1] - latitude;
        return new double[]{longitude - dx, latitude - dy};
    }

    /**
     * GCJ02 转 BD09
     *
     * @param longitude GCJ02经度
     * @param latitude  GCJ02纬度
     * @return double[0]=经度, double[1]=纬度
     */
    public static double[] gcj02ToBd09(double longitude, double latitude) {
        double z = Math.sqrt(longitude * longitude + latitude * latitude)
                + 0.00002 * Math.sin(latitude * X_PI);
        double theta = Math.atan2(latitude, longitude) + 0.000003 * Math.cos(longitude * X_PI);
        return new double[]{z * Math.cos(theta) + 0.0065, z * Math.sin(theta) + 0.006};
    }

    /**
     * BD09 转 GCJ02
     *
     * @param longitude BD09经度
     * @param latitude  BD09纬度
     * @return double[0]=经度, double[1]=纬度
     */
    public static double[] bd09ToGcj02(double longitude, double latitude) {
        double x = longitude - 0.0065;
        double y = latitude - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * X_PI);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * X_PI);
        return new double[]{z * Math.cos(theta), z * Math.sin(theta)};
    }

    /**
     * WGS84 转 BD09
     *
     * @param longitude WGS84经度
     * @param latitude  WGS84纬度
     * @return double[0]=经度, double[1]=纬度
     */
    public static double[] wgs84ToBd09(double longitude, double latitude) {
        double[] gcj = wgs84ToGcj02(longitude, latitude);
        return gcj02ToBd09(gcj[0], gcj[1]);
    }

    /**
     * BD09 转 WGS84
     *
     * @param longitude BD09经度
     * @param latitude  BD09纬度
     * @return double[0]=经度, double[1]=纬度
     */
    public static double[] bd09ToWgs84(double longitude, double latitude) {
        double[] gcj = bd09ToGcj02(longitude, latitude);
        return gcj02ToWgs84(gcj[0], gcj[1]);
    }

    /**
     * 将指定坐标系的经纬度转换为 WGS84 坐标系（Redis GEO 内部存储格式）
     *
     * @param sourceCoordinateSystem 源坐标系
     * @param longitude              源经度
     * @param latitude               源纬度
     * @return double[0]=WGS84经度, double[1]=WGS84纬度
     */
    public static double[] toWgs84(CoordinateSystem sourceCoordinateSystem, double longitude, double latitude) {
        if (sourceCoordinateSystem == null || sourceCoordinateSystem == CoordinateSystem.WGS84) {
            return new double[]{longitude, latitude};
        }
        switch (sourceCoordinateSystem) {
            case GCJ02:
                return gcj02ToWgs84(longitude, latitude);
            case BD09:
                return bd09ToWgs84(longitude, latitude);
            default:
                return new double[]{longitude, latitude};
        }
    }

    /**
     * 将 WGS84 坐标系（Redis GEO 内部存储格式）转换为指定坐标系
     *
     * @param targetCoordinateSystem 目标坐标系
     * @param longitude              WGS84经度
     * @param latitude               WGS84纬度
     * @return double[0]=目标经度, double[1]=目标纬度
     */
    public static double[] fromWgs84(CoordinateSystem targetCoordinateSystem, double longitude, double latitude) {
        if (targetCoordinateSystem == null || targetCoordinateSystem == CoordinateSystem.WGS84) {
            return new double[]{longitude, latitude};
        }
        switch (targetCoordinateSystem) {
            case GCJ02:
                return wgs84ToGcj02(longitude, latitude);
            case BD09:
                return wgs84ToBd09(longitude, latitude);
            default:
                return new double[]{longitude, latitude};
        }
    }

    /**
     * 在两个指定坐标系之间转换坐标
     *
     * @param from      源坐标系
     * @param to        目标坐标系
     * @param longitude 源经度
     * @param latitude  源纬度
     * @return double[0]=目标经度, double[1]=目标纬度
     */
    public static double[] convert(CoordinateSystem from, CoordinateSystem to, double longitude, double latitude) {
        if (from == to || from == null || to == null) {
            return new double[]{longitude, latitude};
        }
        // 先转为 WGS84，再转为目标坐标系
        double[] wgs84 = toWgs84(from, longitude, latitude);
        return fromWgs84(to, wgs84[0], wgs84[1]);
    }

    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y
                + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320.0 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x
                + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }

    private static boolean outOfChina(double longitude, double latitude) {
        return longitude < 72.004 || longitude > 137.8347 || latitude < 0.8293 || latitude > 55.8271;
    }
}
