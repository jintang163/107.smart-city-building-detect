package com.ucm.analysis.service;

/**
 * 0.01°（约 1km）网格编码工具：热力图聚合、热点判定、案例区域统计共用。
 */
public final class GridUtil {

    /** 网格边长（度），0.01° 约合 1km */
    public static final double GRID_SIZE = 0.01;

    private GridUtil() {}

    /** 经纬度 -> 网格编码，如 "11430_3055" */
    public static String gridCode(double lng, double lat) {
        return gridX(lng) + "_" + gridY(lat);
    }

    public static int gridX(double lng) {
        return (int) Math.floor(lng / GRID_SIZE);
    }

    public static int gridY(double lat) {
        return (int) Math.floor(lat / GRID_SIZE);
    }

    /** 网格编码 -> 西南角经纬度（用于绘制网格多边形） */
    public static double[] swCorner(String gridCode) {
        String[] parts = gridCode.split("_");
        double lng = Integer.parseInt(parts[0]) * GRID_SIZE;
        double lat = Integer.parseInt(parts[1]) * GRID_SIZE;
        return new double[]{lng, lat};
    }
}
