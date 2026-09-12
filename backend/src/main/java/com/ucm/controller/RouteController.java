package com.ucm.controller;

import com.ucm.common.ApiResponse;
import com.ucm.common.BizException;
import com.ucm.common.GeoJsonUtil;
import com.ucm.entity.FlightRoute;
import com.ucm.repository.FlightRouteRepository;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 航线管理 */
@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final FlightRouteRepository routeRepo;

    public RouteController(FlightRouteRepository routeRepo) {
        this.routeRepo = routeRepo;
    }

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok(routeRepo.findAll().stream().map(this::toDto).toList());
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody Map<String, Object> body) {
        FlightRoute route = new FlightRoute();
        apply(route, body);
        return ApiResponse.ok(toDto(routeRepo.save(route)));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        FlightRoute route = routeRepo.findById(id).orElseThrow(() -> new BizException("航线不存在"));
        apply(route, body);
        return ApiResponse.ok(toDto(routeRepo.save(route)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        routeRepo.deleteById(id);
        return ApiResponse.ok(null);
    }

    @SuppressWarnings("unchecked")
    private void apply(FlightRoute route, Map<String, Object> body) {
        route.setName((String) body.get("name"));
        route.setDescription((String) body.get("description"));
        if (body.get("altitude") != null) route.setAltitude(((Number) body.get("altitude")).doubleValue());
        if (body.get("overlap") != null) route.setOverlap(((Number) body.get("overlap")).doubleValue());
        if (body.get("waypoints") != null) {
            route.setWaypoints(body.get("waypoints") instanceof String s ? s : toJson(body.get("waypoints")));
        }
        // region: GeoJSON Polygon 或 bbox [minx,miny,maxx,maxy]
        Object region = body.get("region");
        if (region instanceof Map<?, ?> geo) {
            route.setRegion(GeoJsonUtil.polygonFromGeoJson((Map<String, Object>) geo));
        } else if (region instanceof List<?> bbox && bbox.size() == 4) {
            route.setRegion(GeoJsonUtil.bboxPolygon(
                    ((Number) bbox.get(0)).doubleValue(), ((Number) bbox.get(1)).doubleValue(),
                    ((Number) bbox.get(2)).doubleValue(), ((Number) bbox.get(3)).doubleValue()));
        }
    }

    private Map<String, Object> toDto(FlightRoute r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("name", r.getName());
        m.put("description", r.getDescription());
        m.put("altitude", r.getAltitude());
        m.put("overlap", r.getOverlap());
        m.put("waypoints", r.getWaypoints());
        m.put("region", GeoJsonUtil.toGeoJson(r.getRegion()));
        m.put("createdAt", r.getCreatedAt());
        return m;
    }

    private static String toJson(Object o) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(o);
        } catch (Exception e) {
            throw new BizException("JSON 序列化失败");
        }
    }
}
