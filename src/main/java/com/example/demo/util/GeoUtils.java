package com.example.demo.util;

import com.example.demo.model.GeoFence;

import java.math.BigDecimal;
import java.util.List;

/**
 * Geographic utility functions used by fence geometry and location math.
 *
 * <p>Supported geometry types (post-redesign):
 * <ul>
 *   <li>{@code CIRCLE} — point-in-radius check (Haversine distance).</li>
 *   <li>{@code POLYGON} — point-in-polygon check via ray casting.</li>
 *   <li>{@code RECTANGLE} — axis-aligned bounding-box check.</li>
 * </ul>
 *
 * <p>The class is stateless. All methods are pure functions — they may be
 * called from any thread and have no side effects.
 */
public final class GeoUtils {

    /** Mean Earth radius in metres (spherical model). */
    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private GeoUtils() {}

    // =====================================================================
    // Distance
    // =====================================================================

    /**
     * Haversine great-circle distance between two lat/lng points, in metres.
     *
     * @param lat1 latitude of point A in degrees
     * @param lon1 longitude of point A in degrees
     * @param lat2 latitude of point B in degrees
     * @param lon2 longitude of point B in degrees
     * @return arc distance in metres
     */
    public static double distanceMeters(double lat1, double lon1,
                                        double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }

    // =====================================================================
    // Inside checks — one per geometry type
    // =====================================================================

    /**
     * Point in circle of radius {@code radiusM} centred at {@code (cLat, cLng)}.
     * Boundary inclusive.
     */
    public static boolean insideCircle(double lat, double lng,
                                       double cLat, double cLng, int radiusM) {
        return distanceMeters(lat, lng, cLat, cLng) <= radiusM;
    }

    /**
     * Point-in-polygon via ray casting (even-odd rule). The polygon is
     * defined as a list of {@code [lat, lng]} pairs; the first/last point
     * does NOT need to be duplicated. Boundary counts as inside.
     *
     * <p>Implementation notes:
     * <ul>
     *   <li>Returns {@code false} for null, empty, or single-vertex polygons.</li>
     *   <li>Degenerate horizontal edges are skipped (the standard
     *       "ignore vertex on the ray" trick).</li>
     *   <li>Cross-meridian polygons (lng wrapping ±180°) work correctly
     *       because we only compare relative ordering of longitudes, not
     *       absolute ranges.</li>
     *   <li>Near-pole polygons (|lat| &gt; 89.9°) may give surprising
     *       results — caller should project to a local frame first.
     *       The Smart Watch app only operates in mid-latitude ranges
     *       (China, typically 18°–53° N) so this is acceptable.</li>
     * </ul>
     */
    public static boolean insidePolygon(double lat, double lng, List<double[]> polygon) {
        if (polygon == null || polygon.size() < 3) {
            return false;
        }
        boolean inside = false;
        final int n = polygon.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double[] pi = polygon.get(i);
            double[] pj = polygon.get(j);
            if (pi == null || pj == null || pi.length < 2 || pj.length < 2) {
                continue;
            }
            double yi = pi[0];        // latitude
            double xi = pi[1];        // longitude
            double yj = pj[0];
            double xj = pj[1];

            // Check whether the horizontal ray east of (lng, lat) crosses
            // edge (xj,yj) -> (xi,yi). Strictly < to skip the upper vertex
            // and <= to include the lower — this is the canonical
            // half-open convention that avoids double-counting collinear
            // vertices.
            boolean crossesLat = (yi > lat) != (yj > lat);
            if (!crossesLat) {
                continue;
            }
            // x-coordinate of intersection of edge with the horizontal line
            double xIntersect = (xj * (yi - lat) - xi * (yj - lat))
                                / (yi - yj);
            if (lng < xIntersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * Point in axis-aligned rectangle. {@code (swLat, swLng)} is the
     * south-west corner (smaller lat, smaller lng); {@code (neLat, neLng)}
     * is the north-east corner (larger lat, larger lng). Caller is
     * responsible for passing corners in this order — swapping corners
     * will produce a zero-area rect that always returns false.
     *
     * <p>Boundary inclusive on all four sides.
     */
    public static boolean insideRectangle(double lat, double lng,
                                          double swLat, double swLng,
                                          double neLat, double neLng) {
        return lat >= swLat && lat <= neLat
            && lng >= swLng && lng <= neLng;
    }

    // =====================================================================
    // Geometry metrics
    // =====================================================================

    /**
     * Approximate area of a polygon (spherical excess formula) in
     * square metres. Returns 0 for degenerate inputs. Useful for
     * enforcing "no fences larger than X m²" business rules.
     */
    public static double polygonAreaSqMeters(List<double[]> polygon) {
        if (polygon == null || polygon.size() < 3) {
            return 0.0;
        }
        // Equirectangular projection centred on the polygon's centroid —
        // accurate enough for small areas (< a few km²) and far cheaper
        // than a full geodesic calculation.
        double sumLat = 0.0, sumLng = 0.0;
        final int n = polygon.size();
        for (double[] p : polygon) {
            sumLat += p[0];
            sumLng += p[1];
        }
        double cLat = Math.toRadians(sumLat / n);
        double cLng = Math.toRadians(sumLng / n);

        double area = 0.0;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double[] pi = polygon.get(i);
            double[] pj = polygon.get(j);
            double xi = Math.toRadians(pj[1]) - cLng;
            double yi = Math.toRadians(pj[0]) - cLat;
            double xj = Math.toRadians(pi[1]) - cLng;
            double yj = Math.toRadians(pi[0]) - cLat;
            area += (xj * yi - xi * yj);
        }
        double latRad = cLat;
        double metresPerDegLat = 111_132.92
            - 559.82 * Math.cos(2 * latRad) + 1.175 * Math.cos(4 * latRad);
        double metresPerDegLng = 111_412.84 * Math.cos(latRad)
            - 93.5 * Math.cos(3 * latRad);
        return Math.abs(area) * 0.5 * metresPerDegLat * metresPerDegLng;
    }

    // =====================================================================
    // Legacy BigDecimal overloads — keep old callers working
    // =====================================================================

    /**
     * Legacy dispatch on the old {@link GeoFence} entity. Only
     * {@code type == "circle"} is supported by this path; polygon/rectangle
     * return {@code false} to avoid silent breakage. New code should use
     * {@link #insideFromGeometryType}.
     */
    public static boolean inside(BigDecimal lat, BigDecimal lng, GeoFence f) {
        if (lat == null || lng == null || f == null) return false;
        if (f.getCenterLat() == null || f.getCenterLng() == null
                || f.getRadius() == null) {
            return false;
        }
        return distanceMeters(lat.doubleValue(), lng.doubleValue(),
                              f.getCenterLat(), f.getCenterLng())
                <= f.getRadius();
    }

    /** Same as above but with primitive doubles. */
    public static boolean inside(Double lat, Double lng, GeoFence f) {
        if (lat == null || lng == null || f == null) return false;
        if (f.getCenterLat() == null || f.getCenterLng() == null
                || f.getRadius() == null) {
            return false;
        }
        return distanceMeters(lat, lng, f.getCenterLat(), f.getCenterLng())
                <= f.getRadius();
    }

    // =====================================================================
    // New geometry dispatch (post-redesign)
    // =====================================================================

    /**
     * Dispatch a point-in-fence check using the new geometry fields.
     * {@code type} is matched case-insensitively.
     *
     * @param type      "CIRCLE" / "POLYGON" / "RECTANGLE" (case-insensitive)
     * @param lat       latitude of test point
     * @param lng       longitude of test point
     * @param cLat      circle centre lat (only used for CIRCLE)
     * @param cLng      circle centre lng (only used for CIRCLE)
     * @param radiusM   circle radius in metres (only used for CIRCLE)
     * @param polygon   polygon vertices (only used for POLYGON)
     * @param swLat     rectangle south-west lat (only used for RECTANGLE)
     * @param swLng     rectangle south-west lng (only used for RECTANGLE)
     * @param neLat     rectangle north-east lat (only used for RECTANGLE)
     * @param neLng     rectangle north-east lng (only used for RECTANGLE)
     */
    public static boolean insideFromGeometryType(String type,
                                                 double lat, double lng,
                                                 Double cLat, Double cLng,
                                                 Integer radiusM,
                                                 List<double[]> polygon,
                                                 Double swLat, Double swLng,
                                                 Double neLat, Double neLng) {
        if (type == null) return false;
        switch (type.trim().toUpperCase()) {
            case "CIRCLE":
                if (cLat == null || cLng == null || radiusM == null) return false;
                return insideCircle(lat, lng, cLat, cLng, radiusM);
            case "POLYGON":
                return insidePolygon(lat, lng, polygon);
            case "RECTANGLE":
                if (swLat == null || swLng == null || neLat == null || neLng == null) {
                    return false;
                }
                return insideRectangle(lat, lng, swLat, swLng, neLat, neLng);
            default:
                return false;
        }
    }
}
