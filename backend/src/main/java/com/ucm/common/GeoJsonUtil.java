package com.ucm.common;

import org.locationtech.jts.geom.*;
import java.util.*;

/** JTS 几何对象 <-> GeoJSON Map 互转（仅支持本系统用到的 Point/Polygon） */
public final class GeoJsonUtil {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private GeoJsonUtil() {}

    public static GeometryFactory gf() { return GF; }

    /** JTS Geometry -> GeoJSON geometry Map */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toGeoJson(Geometry geom) {
        if (geom == null) return null;
        if (geom instanceof Polygon p) {
            List<List<double[]>> rings = new ArrayList<>();
            rings.add(ringToList(p.getExteriorRing()));
            for (int i = 0; i < p.getNumInteriorRing(); i++) {
                rings.add(ringToList(p.getInteriorRingN(i)));
            }
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("type", "Polygon");
            g.put("coordinates", rings.stream().map(r -> (Object) r.stream()
                    .map(c -> List.of(c[0], c[1])).toList()).toList());
            return g;
        }
        if (geom instanceof Point pt) {
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("type", "Point");
            g.put("coordinates", List.of(pt.getX(), pt.getY()));
            return g;
        }
        throw new IllegalArgumentException("unsupported geometry: " + geom.getGeometryType());
    }

    private static List<double[]> ringToList(LineString ring) {
        List<double[]> list = new ArrayList<>();
        for (Coordinate c : ring.getCoordinates()) {
            list.add(new double[]{c.x, c.y});
        }
        return list;
    }

    /** GeoJSON geometry Map -> JTS Polygon */
    public static Polygon polygonFromGeoJson(Map<String, Object> geo) {
        String type = (String) geo.get("type");
        if (!"Polygon".equals(type)) {
            throw new IllegalArgumentException("仅支持 Polygon");
        }
        List<List<List<Double>>> coords = (List<List<List<Double>>>) geo.get("coordinates");
        LinearRing shell = toRing(coords.get(0));
        LinearRing[] holes = new LinearRing[coords.size() - 1];
        for (int i = 1; i < coords.size(); i++) {
            holes[i - 1] = toRing(coords.get(i));
        }
        Polygon polygon = GF.createPolygon(shell, holes);
        polygon.setSRID(4326);
        return polygon;
    }

    private static LinearRing toRing(List<List<Double>> ring) {
        Coordinate[] cs = ring.stream()
                .map(p -> new Coordinate(p.get(0), p.get(1)))
                .toArray(Coordinate[]::new);
        return GF.createLinearRing(cs);
    }

    /** 由 bbox 构造矩形 Polygon */
    public static Polygon bboxPolygon(double minx, double miny, double maxx, double maxy) {
        Polygon p = GF.createPolygon(new Coordinate[]{
                new Coordinate(minx, miny),
                new Coordinate(maxx, miny),
                new Coordinate(maxx, maxy),
                new Coordinate(minx, maxy),
                new Coordinate(minx, miny)
        });
        p.setSRID(4326);
        return p;
    }

    /** 包装成 GeoJSON Feature */
    public static Map<String, Object> feature(Geometry geom, Map<String, Object> properties) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("type", "Feature");
        f.put("geometry", toGeoJson(geom));
        f.put("properties", properties);
        return f;
    }

    /** 包装成 GeoJSON FeatureCollection */
    public static Map<String, Object> featureCollection(List<Map<String, Object>> features) {
        Map<String, Object> fc = new LinkedHashMap<>();
        fc.put("type", "FeatureCollection");
        fc.put("features", features);
        return fc;
    }
}
