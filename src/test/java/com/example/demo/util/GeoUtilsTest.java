package com.example.demo.util;

import com.example.demo.model.GeoFence;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GeoUtils geographic utility tests — covers Haversine distance, circle
 * point-in-radius, polygon ray-casting, rectangle axis-aligned bbox,
 * and the legacy BigDecimal/Double dispatch through the old GeoFence
 * entity.
 */
class GeoUtilsTest {

    private static final double EPSILON_M = 1.0;        // ±1 m acceptable
    private static final double EPSILON_DEG = 1e-9;

    // =====================================================================
    // Distance — Haversine
    // =====================================================================

    @Test
    void distanceMeters_betweenTwoPoints_returnsCorrectDistance() {
        // Tiananmen to the Forbidden City — about 850 m apart.
        double d = GeoUtils.distanceMeters(39.9087, 116.3975,
                                           39.9163, 116.3972);
        assertTrue(d > 800 && d < 900, "expected ~850 m, got " + d);
    }

    @Test
    void distanceMeters_samePoint_returnsZero() {
        double d = GeoUtils.distanceMeters(39.9087, 116.3975,
                                           39.9087, 116.3975);
        assertEquals(0.0, d, 0.01);
    }

    @Test
    void distanceMeters_antipodes_returnsHalfEarthCircumference() {
        // 0,0 to 0,180 should be pi * R ~= 20015 km.
        double d = GeoUtils.distanceMeters(0, 0, 0, 180);
        assertEquals(20_015_087.0, d, 50_000.0);
    }

    @Test
    void distanceMeters_oneDegreeOfLatitude_isAbout111km() {
        // 1 degree of latitude at the equator ≈ 111.32 km.
        double d = GeoUtils.distanceMeters(0, 0, 1, 0);
        assertEquals(111_195.0, d, 1_000.0);
    }

    // =====================================================================
    // insideCircle
    // =====================================================================

    @Test
    void insideCircle_atCentre_returnsTrue() {
        assertTrue(GeoUtils.insideCircle(39.9087, 116.3975,
                                        39.9087, 116.3975, 500));
    }

    @Test
    void insideCircle_insideRadius_returnsTrue() {
        assertTrue(GeoUtils.insideCircle(39.9090, 116.3980,
                                        39.9087, 116.3975, 500));
    }

    @Test
    void insideCircle_outsideRadius_returnsFalse() {
        assertFalse(GeoUtils.insideCircle(40.0000, 116.5000,
                                         39.9087, 116.3975, 500));
    }

    @Test
    void insideCircle_boundaryInclusive_returnsTrue() {
        // Exactly on the radius circle is included.
        // ~1.1 km north of (39.9087, 116.3975) == exactly 1100 m.
        double cLat = 39.9087;
        double cLng = 116.3975;
        // 1 degree of latitude at ~40° N ~= 111 km; 0.01° ≈ 1110 m.
        double edgeLat = cLat + 0.01;
        assertTrue(GeoUtils.insideCircle(edgeLat, cLng, cLat, cLng, 1110));
        assertFalse(GeoUtils.insideCircle(edgeLat + 0.001, cLng, cLat, cLng, 1110));
    }

    @Test
    void insideCircle_zeroRadius_onlyCentreMatches() {
        assertTrue(GeoUtils.insideCircle(0, 0, 0, 0, 0));
        assertFalse(GeoUtils.insideCircle(0.0001, 0, 0, 0, 0));
    }

    @Test
    void insideCircle_negativeRadius_treatedAsZero() {
        // Negative radius is nonsensical; the code uses <= which makes any
        // non-centre point "inside" only if radius is exactly 0. Negative
        // radius makes the predicate always false.
        assertFalse(GeoUtils.insideCircle(0.0001, 0.0001, 0, 0, -10));
    }

    @Test
    void insideCircle_atPoles_works() {
        // North Pole circle of 500 m around (90, 0).
        assertTrue(GeoUtils.insideCircle(90.0, 0.0, 90.0, 0.0, 500));
    }

    @Test
    void insideCircle_acrossAntimeridian() {
        // Fence centred near +180°; point near -180° should still be inside.
        // Centre: (0, 179.9); point: (0, -179.9); radius 30 km.
        // Great-circle distance ≈ 22 km — inside.
        assertTrue(GeoUtils.insideCircle(0, -179.9, 0, 179.9, 30_000));
    }

    // =====================================================================
    // insidePolygon (ray casting)
    // =====================================================================

    @Test
    void insidePolygon_convexSquare_insideReturnsTrue() {
        List<double[]> square = box(0, 0, 10, 10);
        assertTrue(GeoUtils.insidePolygon(5, 5, square));
    }

    @Test
    void insidePolygon_convexSquare_outsideReturnsFalse() {
        List<double[]> square = box(0, 0, 10, 10);
        assertFalse(GeoUtils.insidePolygon(20, 20, square));
        assertFalse(GeoUtils.insidePolygon(-5, 5, square));
        assertFalse(GeoUtils.insidePolygon(5, 15, square));
    }

    @Test
    void insidePolygon_concaveStar_insideAndOutsideHandled() {
        // Concave star (clockwise). Centre (5,5) is INSIDE.
        // (3,0) is OUTSIDE (in the concavity).
        List<double[]> star = List.of(
            new double[]{5,  0},
            new double[]{6,  4},
            new double[]{10, 4},
            new double[]{7,  6},
            new double[]{9, 10},
            new double[]{5,  7},
            new double[]{1, 10},
            new double[]{3,  6},
            new double[]{0,  4},
            new double[]{4,  4}
        );
        assertTrue(GeoUtils.insidePolygon(5, 5, star));
        assertFalse(GeoUtils.insidePolygon(3, 0, star));
    }

    @Test
    void insidePolygon_edgePoints_returnTrue() {
        // Boundary is inclusive.
        List<double[]> square = box(0, 0, 10, 10);
        assertTrue(GeoUtils.insidePolygon(0, 5, square));
        assertTrue(GeoUtils.insidePolygon(5, 0, square));
        assertTrue(GeoUtils.insidePolygon(5, 10, square));
        assertTrue(GeoUtils.insidePolygon(10, 5, square));
        assertTrue(GeoUtils.insidePolygon(0, 0, square));
        assertTrue(GeoUtils.insidePolygon(10, 10, square));
    }

    @Test
    void insidePolygon_nullOrDegenerate_returnsFalse() {
        assertFalse(GeoUtils.insidePolygon(0, 0, null));
        assertFalse(GeoUtils.insidePolygon(0, 0, List.of()));
        assertFalse(GeoUtils.insidePolygon(0, 0, List.of(new double[]{0, 0})));
        assertFalse(GeoUtils.insidePolygon(0, 0, List.of(
            new double[]{0, 0}, new double[]{1, 1})));
    }

    @Test
    void insidePolygon_acrossAntimeridian() {
        // Polygon spans 179° to -179° (crosses the antimeridian).
        // Point at lng=-179.9 is inside.
        List<double[]> wrap = List.of(
            new double[]{0,  179},
            new double[]{10, 179},
            new double[]{10, -179},
            new double[]{0,  -179}
        );
        assertTrue(GeoUtils.insidePolygon(5, -179.9, wrap));
    }

    // =====================================================================
    // insideRectangle
    // =====================================================================

    @Test
    void insideRectangle_basicWorks() {
        assertTrue(GeoUtils.insideRectangle(5, 5, 0, 0, 10, 10));
        assertTrue(GeoUtils.insideRectangle(0, 0, 0, 0, 10, 10));
        assertTrue(GeoUtils.insideRectangle(10, 10, 0, 0, 10, 10));
        assertFalse(GeoUtils.insideRectangle(11, 5, 0, 0, 10, 10));
        assertFalse(GeoUtils.insideRectangle(5, 11, 0, 0, 10, 10));
        assertFalse(GeoUtils.insideRectangle(-1, 5, 0, 0, 10, 10));
    }

    @Test
    void insideRectangle_acrossEquatorAndAntimeridian() {
        assertTrue(GeoUtils.insideRectangle(0, 0,    -10, -180, 10, 180));
        assertTrue(GeoUtils.insideRectangle(0, -179,  -10, -180, 10, 180));
        assertTrue(GeoUtils.insideRectangle(0,  179,  -10, -180, 10, 180));
    }

    // =====================================================================
    // Polygon area
    // =====================================================================

    @Test
    void polygonAreaSqMeters_unitSquare_isAbout111kmSquared() {
        // 1° lat x 1° lng near equator ≈ 111 km × 111 km ≈ 1.23e10 m².
        // (Equirectangular approx — within ~1% of true geodesic at this size.)
        List<double[]> oneByOne = box(0, 0, 1, 1);
        double a = GeoUtils.polygonAreaSqMeters(oneByOne);
        assertTrue(a > 1.2e10 && a < 1.25e10, "expected ~1.23e10 m², got " + a);
    }

    @Test
    void polygonAreaSqMeters_degenerateIsZero() {
        assertEquals(0.0, GeoUtils.polygonAreaSqMeters(null));
        assertEquals(0.0, GeoUtils.polygonAreaSqMeters(List.of()));
    }

    // =====================================================================
    // Dispatch through legacy GeoFence (backwards-compat)
    // =====================================================================

    @Test
    void inside_bigDecimalInsideFence_returnsTrue() {
        GeoFence fence = createCircleFence(39.9087, 116.3975, 500);
        assertTrue(GeoUtils.inside(new BigDecimal("39.9087"),
                                   new BigDecimal("116.3975"), fence));
    }

    @Test
    void inside_bigDecimalOutsideFence_returnsFalse() {
        GeoFence fence = createCircleFence(39.9087, 116.3975, 100);
        assertFalse(GeoUtils.inside(new BigDecimal("39.9200"),
                                    new BigDecimal("116.4100"), fence));
    }

    @Test
    void inside_doubleInsideFence_returnsTrue() {
        GeoFence fence = createCircleFence(39.9087, 116.3975, 500);
        assertTrue(GeoUtils.inside(39.9087, 116.3975, fence));
    }

    @Test
    void inside_doubleOutsideFence_returnsFalse() {
        GeoFence fence = createCircleFence(39.9087, 116.3975, 100);
        assertFalse(GeoUtils.inside(39.9200, 116.4100, fence));
    }

    @Test
    void inside_nullLat_returnsFalse() {
        GeoFence fence = createCircleFence(39.9087, 116.3975, 500);
        assertFalse(GeoUtils.inside((Double) null, 116.3975, fence));
    }

    @Test
    void inside_nullLng_returnsFalse() {
        GeoFence fence = createCircleFence(39.9087, 116.3975, 500);
        assertFalse(GeoUtils.inside(39.9087, (Double) null, fence));
    }

    @Test
    void inside_fenceWithoutRadius_returnsFalse() {
        GeoFence fence = new GeoFence();
        fence.setCenterLat(39.9087);
        fence.setCenterLng(116.3975);
        assertFalse(GeoUtils.inside(39.9087, 116.3975, fence));
    }

    @Test
    void inside_fenceWithoutCenter_returnsFalse() {
        GeoFence fence = new GeoFence();
        fence.setRadius(500);
        assertFalse(GeoUtils.inside(39.9087, 116.3975, fence));
    }

    @Test
    void inside_bigDecimalNull_returnsFalse() {
        GeoFence fence = createCircleFence(39.9087, 116.3975, 500);
        assertFalse(GeoUtils.inside((BigDecimal) null,
                                    new BigDecimal("116.3975"), fence));
    }

    // =====================================================================
    // Dispatch through new geometry params
    // =====================================================================

    @Test
    void insideFromGeometryType_circleDispatch() {
        boolean in = GeoUtils.insideFromGeometryType(
            "CIRCLE", 39.9090, 116.3980,
            39.9087, 116.3975, 500,
            null, null, null, null, null);
        assertTrue(in);
    }

    @Test
    void insideFromGeometryType_circleCaseInsensitive() {
        boolean in = GeoUtils.insideFromGeometryType(
            "circle", 39.9090, 116.3980,
            39.9087, 116.3975, 500,
            null, null, null, null, null);
        assertTrue(in);
    }

    @Test
    void insideFromGeometryType_polygonDispatch() {
        List<double[]> square = box(0, 0, 10, 10);
        boolean in = GeoUtils.insideFromGeometryType(
            "POLYGON", 5, 5,
            null, null, null, square,
            null, null, null, null);
        assertTrue(in);
    }

    @Test
    void insideFromGeometryType_rectangleDispatch() {
        boolean in = GeoUtils.insideFromGeometryType(
            "RECTANGLE", 5, 5,
            null, null, null, null,
            0.0, 0.0, 10.0, 10.0);
        assertTrue(in);
    }

    @Test
    void insideFromGeometryType_unknownType_returnsFalse() {
        boolean in = GeoUtils.insideFromGeometryType(
            "TRIANGLE", 0, 0,
            null, null, null, null,
            null, null, null, null);
        assertFalse(in);
    }

    @Test
    void insideFromGeometryType_circleMissingFields_returnsFalse() {
        boolean in = GeoUtils.insideFromGeometryType(
            "CIRCLE", 0, 0,
            null, null, null,
            null, null, null, null, null);
        assertFalse(in);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private GeoFence createCircleFence(double cLat, double cLng, int radius) {
        GeoFence f = new GeoFence();
        f.setCenterLat(cLat);
        f.setCenterLng(cLng);
        f.setRadius(radius);
        f.setType("circle");
        f.setStatus("active");
        return f;
    }

    /** Axis-aligned rectangle as a 4-vertex polygon (counter-clockwise). */
    private static List<double[]> box(double minLat, double minLng,
                                     double maxLat, double maxLng) {
        List<double[]> verts = new ArrayList<>(4);
        verts.add(new double[]{minLat, minLng});
        verts.add(new double[]{minLat, maxLng});
        verts.add(new double[]{maxLat, maxLng});
        verts.add(new double[]{maxLat, minLng});
        return verts;
    }
}
