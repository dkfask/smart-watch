package com.example.demo.util;

import com.example.demo.model.GeoFence;

import java.math.BigDecimal;

/**
 * 地理位置计算工具类，提供距离计算和围栏判断等通用方法
 */
public final class GeoUtils {

    private GeoUtils() {}

    /**
     * 使用Haversine公式计算两点之间的距离（米）
     * @param lat1 第一个点的纬度
     * @param lon1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lon2 第二个点的经度
     * @return 距离（米）
     */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * 判断BigDecimal坐标位置是否在围栏内
     * @param lat 纬度
     * @param lng 经度
     * @param f 围栏
     * @return 是否在围栏内
     */
    public static boolean inside(BigDecimal lat, BigDecimal lng, GeoFence f) {
        if (lat == null || lng == null || f.getCenterLat() == null || f.getCenterLng() == null || f.getRadius() == null)
            return false;
        double d = distanceMeters(lat.doubleValue(), lng.doubleValue(), f.getCenterLat(), f.getCenterLng());
        return d <= f.getRadius();
    }

    /**
     * 判断Double坐标位置是否在围栏内
     * @param lat 纬度
     * @param lng 经度
     * @param f 围栏
     * @return 是否在围栏内
     */
    public static boolean inside(Double lat, Double lng, GeoFence f) {
        if (lat == null || lng == null || f.getCenterLat() == null || f.getCenterLng() == null || f.getRadius() == null)
            return false;
        double d = distanceMeters(lat, lng, f.getCenterLat(), f.getCenterLng());
        return d <= f.getRadius();
    }
}
