package com.ucm.analysis.service;

import com.ucm.common.GeoJsonUtil;
import com.ucm.entity.ChangeSpot;
import com.ucm.entity.WorkOrder;
import com.ucm.repository.WorkOrderRepository;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 执法资源热力图：把未办结工单按 0.01° 网格聚合，
 * 超期工单加权，输出 GeoJSON 网格热力与航线优先级建议。
 * 纯统计聚合，不依赖 GeoServer。
 */
@Service
public class HeatmapService {

    /** 未办结（待处置）状态集合 */
    public static final List<WorkOrder.Status> OPEN_STATUSES = List.of(
            WorkOrder.Status.PENDING, WorkOrder.Status.INSPECTING,
            WorkOrder.Status.CONFIRMED, WorkOrder.Status.RECTIFYING);

    /** 超期工单在热力权重中的加倍系数 */
    private static final int OVERDUE_WEIGHT = 2;

    private final WorkOrderRepository orderRepo;
    private final DurationPredictService predictService;

    public HeatmapService(WorkOrderRepository orderRepo, DurationPredictService predictService) {
        this.orderRepo = orderRepo;
        this.predictService = predictService;
    }

    /** 某网格的未办结工单数（供规则引擎热点判定） */
    public long openOrderCount(String gridCode) {
        return aggregate().stream()
                .filter(g -> g.gridCode.equals(gridCode))
                .mapToLong(g -> g.count).findFirst().orElse(0);
    }

    /** 网格聚合结果 */
    public List<GridStat> aggregate() {
        Map<String, GridStat> byGrid = new HashMap<>();
        for (WorkOrder order : orderRepo.findByStatusIn(OPEN_STATUSES)) {
            Point centroid = centroidOf(order);
            if (centroid == null) continue;
            String gridCode = GridUtil.gridCode(centroid.getX(), centroid.getY());
            GridStat stat = byGrid.computeIfAbsent(gridCode, GridStat::new);
            stat.count++;
            boolean overdue = Boolean.TRUE.equals(
                    predictService.predictForOrder(order).get("overdue"));
            if (overdue) stat.overdueCount++;
            stat.weight += overdue ? OVERDUE_WEIGHT : 1;
        }
        return byGrid.values().stream()
                .sorted((a, b) -> Integer.compare(b.weight, a.weight))
                .toList();
    }

    /** 热力图 GeoJSON + 汇总统计 + 巡查/航线优先级建议 */
    public Map<String, Object> heatmap() {
        List<GridStat> grids = aggregate();

        List<Map<String, Object>> features = new ArrayList<>();
        for (GridStat g : grids) {
            double[] sw = GridUtil.swCorner(g.gridCode);
            double s = GridUtil.GRID_SIZE;
            Polygon cell = GeoJsonUtil.bboxPolygon(sw[0], sw[1], sw[0] + s, sw[1] + s);
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("gridCode", g.gridCode);
            props.put("count", g.count);
            props.put("overdueCount", g.overdueCount);
            props.put("weight", g.weight);
            props.put("level", g.level());
            features.add(GeoJsonUtil.feature(cell, props));
        }

        // 巡查/无人机航线优先级：按权重取 Top5 网格中心
        List<Map<String, Object>> priorities = grids.stream().limit(5).map(g -> {
            double[] sw = GridUtil.swCorner(g.gridCode);
            double s = GridUtil.GRID_SIZE;
            Map<String, Object> p = new LinkedHashMap<String, Object>();
            p.put("gridCode", g.gridCode);
            p.put("centerLng", Math.round((sw[0] + s / 2) * 1e6) / 1e6);
            p.put("centerLat", Math.round((sw[1] + s / 2) * 1e6) / 1e6);
            p.put("count", g.count);
            p.put("overdueCount", g.overdueCount);
            p.put("weight", g.weight);
            return p;
        }).toList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("openOrders", grids.stream().mapToInt(g -> g.count).sum());
        summary.put("overdueOrders", grids.stream().mapToInt(g -> g.overdueCount).sum());
        summary.put("hotspotGrids", grids.stream().filter(g -> g.count >= RecommendService.HOTSPOT_THRESHOLD).count());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("geojson", GeoJsonUtil.featureCollection(features));
        result.put("summary", summary);
        result.put("priorities", priorities);
        return result;
    }

    private static Point centroidOf(WorkOrder order) {
        ChangeSpot spot = order.getSpot();
        if (spot == null || spot.getGeom() == null) return null;
        return spot.getGeom().getCentroid();
    }

    /** 单个网格的聚合统计 */
    public static final class GridStat {
        final String gridCode;
        int count = 0;
        int overdueCount = 0;
        int weight = 0;

        GridStat(String gridCode) { this.gridCode = gridCode; }

        /** 热力等级 1~5，前端色带分级 */
        int level() {
            if (weight >= 8) return 5;
            if (weight >= 5) return 4;
            if (weight >= 3) return 3;
            if (weight >= 2) return 2;
            return 1;
        }
    }
}
