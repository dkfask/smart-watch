package com.smartwatch.monitor.utils;

import android.graphics.Color;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.CircleOptions;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.LatLngBounds;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.PolygonOptions;
import java.util.List;

/**
 * 高德地图工具类，封装地图常用操作
 * 包括标记添加、圆形围栏绘制、多边形绘制、相机移动等功能
 */
public class MapHelper {

    /**
     * 初始化地图设置
     * @param aMap 高德地图实例
     */
    public static void setupMap(AMap aMap) {
        if (aMap == null) return;
        aMap.getUiSettings().setZoomControlsEnabled(true);
        aMap.getUiSettings().setCompassEnabled(true);
        aMap.getUiSettings().setScaleControlsEnabled(true);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.setMyLocationEnabled(true);
    }

    /**
     * 在地图上添加标记点
     * @param aMap 高德地图实例
     * @param lat 纬度
     * @param lng 经度
     * @param title 标记标题
     */
    public static void addMarker(AMap aMap, double lat, double lng, String title) {
        if (aMap == null) return;
        LatLng latLng = new LatLng(lat, lng);
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        aMap.addMarker(markerOptions);
    }

    /**
     * 在地图上添加圆形覆盖物（用于圆形围栏显示）
     * @param aMap 高德地图实例
     * @param center 圆心坐标
     * @param radius 半径（米）
     * @param color 填充颜色
     */
    public static void addCircle(AMap aMap, LatLng center, double radius, int color) {
        if (aMap == null || center == null) return;
        CircleOptions circleOptions = new CircleOptions()
                .center(center)
                .radius(radius)
                .fillColor(color)
                .strokeColor(getDarkerColor(color))
                .strokeWidth(2);
        aMap.addCircle(circleOptions);
    }

    /**
     * 在地图上添加多边形覆盖物（用于多边形围栏显示）
     * @param aMap 高德地图实例
     * @param points 多边形顶点列表
     * @param color 填充颜色
     */
    public static void addPolygon(AMap aMap, List<LatLng> points, int color) {
        if (aMap == null || points == null || points.size() < 3) return;
        PolygonOptions polygonOptions = new PolygonOptions()
                .addAll(points)
                .fillColor(color)
                .strokeColor(getDarkerColor(color))
                .strokeWidth(2);
        aMap.addPolygon(polygonOptions);
    }

    /**
     * 移动相机到指定坐标和缩放级别
     * @param aMap 高德地图实例
     * @param lat 纬度
     * @param lng 经度
     * @param zoom 缩放级别（3-20）
     */
    public static void moveCamera(AMap aMap, double lat, double lng, float zoom) {
        if (aMap == null) return;
        LatLng latLng = new LatLng(lat, lng);
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    /**
     * 移动相机到包含所有指定坐标的范围
     * @param aMap 高德地图实例
     * @param points 坐标点列表
     */
    public static void moveCameraToBounds(AMap aMap, List<LatLng> points) {
        if (aMap == null || points == null || points.isEmpty()) return;
        if (points.size() == 1) {
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 15));
            return;
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : points) {
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();
        aMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    /**
     * 获取更深的颜色用于描边
     * @param color 原始颜色
     * @return 更深的颜色值
     */
    public static int getDarkerColor(int color) {
        int alpha = Color.alpha(color);
        int red = Math.max((int) (Color.red(color) * 0.7), 0);
        int green = Math.max((int) (Color.green(color) * 0.7), 0);
        int blue = Math.max((int) (Color.blue(color) * 0.7), 0);
        return Color.argb(alpha, red, green, blue);
    }

    /**
     * 根据围栏状态获取对应的填充颜色
     * @param status 围栏状态（active/inactive）
     * @param isSelected 是否被选中
     * @return 颜色值
     */
    public static int getFenceColor(String status, boolean isSelected) {
        if (isSelected) {
            return Color.argb(80, 239, 68, 68);
        }
        if ("active".equalsIgnoreCase(status)) {
            return Color.argb(80, 59, 130, 246);
        }
        return Color.argb(80, 107, 114, 128);
    }
}
