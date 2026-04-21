package com.example.demo.util;

import com.example.demo.model.GeoFence;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GeoUtils地理位置计算工具类单元测试
 */
class GeoUtilsTest {

    /**
     * 测试两点间距离计算精度（北京天安门到故宫约1km）
     */
    @Test
    void distanceMeters_betweenTwoPoints_returnsCorrectDistance() {
        double lat1 = 39.9087;
        double lng1 = 116.3975;
        double lat2 = 39.9163;
        double lng2 = 116.3972;
        double distance = GeoUtils.distanceMeters(lat1, lng1, lat2, lng2);
        assertTrue(distance > 800 && distance < 900, "Distance should be around 850m, got: " + distance);
    }

    /**
     * 测试同一点距离为0
     */
    @Test
    void distanceMeters_samePoint_returnsZero() {
        double distance = GeoUtils.distanceMeters(39.9087, 116.3975, 39.9087, 116.3975);
        assertEquals(0.0, distance, 0.01);
    }

    /**
     * 测试BigDecimal坐标在围栏内
     */
    @Test
    void inside_bigDecimalInsideFence_returnsTrue() {
        GeoFence fence = createCircleFence(39.9087, 116.3975, 500);
        assertTrue(GeoUtils.inside(new BigDecimal("39.9087"), new BigDecimal("116.3975"), fence));
    }

    /**
     * 测试BigDecimal坐标在围栏外
     */
    @Test
    void inside_bigDecimalOutsideFence_returnsFalse() {
        GeoFence fence = createCircleFence(39.9087, 116.3975, 100);
        assertFalse(GeoUtils.inside(new BigDecimal("39.9200"), new BigDecimal("116.4100"), fence));
    }

    /**
     * 测试Double坐标在围栏内
     */
    @Test
    void inside_doubleInsideFence_returnsTrue() {
        GeoFence fence = createCircleFence(39.9087, 116.3975, 500);
        assertTrue(GeoUtils.inside(39.9087, 116.3975, fence));
    }

    /**
     * 测试Double坐标在围栏外
     */
    @Test
    void inside_doubleOutsideFence_returnsFalse() {
        GeoFence fence = createCircleFence(39.9087, 116.3975, 100);
        assertFalse(GeoUtils.inside(39.9200, 116.4100, fence));
    }

    /**
     * 测试null纬度返回false
     */
    @Test
    void inside_nullLat_returnsFalse() {
        GeoFence fence = createCircleFence(39.9087, 116.3975, 500);
        assertFalse(GeoUtils.inside((Double) null, 116.3975, fence));
    }

    /**
     * 测试null经度返回false
     */
    @Test
    void inside_nullLng_returnsFalse() {
        GeoFence fence = createCircleFence(39.9087, 116.3975, 500);
        assertFalse(GeoUtils.inside(39.9087, (Double) null, fence));
    }

    /**
     * 测试围栏缺少radius返回false
     */
    @Test
    void inside_fenceWithoutRadius_returnsFalse() {
        GeoFence fence = new GeoFence();
        fence.setCenterLat(39.9087);
        fence.setCenterLng(116.3975);
        assertFalse(GeoUtils.inside(39.9087, 116.3975, fence));
    }

    /**
     * 测试围栏缺少中心点返回false
     */
    @Test
    void inside_fenceWithoutCenter_returnsFalse() {
        GeoFence fence = new GeoFence();
        fence.setRadius(500);
        assertFalse(GeoUtils.inside(39.9087, 116.3975, fence));
    }

    /**
     * 测试BigDecimal坐标null返回false
     */
    @Test
    void inside_bigDecimalNull_returnsFalse() {
        GeoFence fence = createCircleFence(39.9087, 116.3975, 500);
        assertFalse(GeoUtils.inside((BigDecimal) null, new BigDecimal("116.3975"), fence));
    }

    /**
     * 创建圆形围栏辅助方法
     */
    private GeoFence createCircleFence(double centerLat, double centerLng, int radius) {
        GeoFence fence = new GeoFence();
        fence.setCenterLat(centerLat);
        fence.setCenterLng(centerLng);
        fence.setRadius(radius);
        fence.setType("circle");
        fence.setStatus("active");
        return fence;
    }
}
