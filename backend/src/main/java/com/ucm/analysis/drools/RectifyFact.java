package com.ucm.analysis.drools;

/**
 * 整改推荐规则 Fact：输入违建特征，输出推荐整改方式与工时参数。
 * 输入：违建类型 / 面积 / 是否执法热点区域 / 是否重复违建高发区（后两项由统计模型计算后注入）
 * 输出：整改方式、风险等级、基础工时、每平米工时、命中规则说明
 */
public class RectifyFact {

    // ---------- 输入 ----------
    private String violationType;
    private double areaM2;
    /** 所在网格未办结工单密度达到热点阈值 */
    private boolean hotspot;
    /** 所在网格历史案例数达到重复违建高发阈值 */
    private boolean repeatArea;

    // ---------- 输出 ----------
    /** SELF_DEMOLITION 自拆 / ASSISTED_DEMOLITION 助拆 / FORCED_DEMOLITION 强拆 */
    private String rectifyMethod;
    /** 风险等级：高 / 中 / 低 */
    private String riskLevel;
    /** 基础工时（小时） */
    private double baseHours;
    /** 每平米工时（小时/㎡） */
    private double perM2Hours;
    /** 命中规则说明（可多条拼接） */
    private StringBuilder reason = new StringBuilder();

    public RectifyFact() {}

    public RectifyFact(String violationType, double areaM2, boolean hotspot, boolean repeatArea) {
        this.violationType = violationType;
        this.areaM2 = areaM2;
        this.hotspot = hotspot;
        this.repeatArea = repeatArea;
    }

    public void appendReason(String r) {
        if (reason.length() > 0) reason.append("；");
        reason.append(r);
    }

    public String getViolationType() { return violationType; }
    public void setViolationType(String violationType) { this.violationType = violationType; }
    public double getAreaM2() { return areaM2; }
    public void setAreaM2(double areaM2) { this.areaM2 = areaM2; }
    public boolean isHotspot() { return hotspot; }
    public void setHotspot(boolean hotspot) { this.hotspot = hotspot; }
    public boolean isRepeatArea() { return repeatArea; }
    public void setRepeatArea(boolean repeatArea) { this.repeatArea = repeatArea; }
    public String getRectifyMethod() { return rectifyMethod; }
    public void setRectifyMethod(String rectifyMethod) { this.rectifyMethod = rectifyMethod; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public double getBaseHours() { return baseHours; }
    public void setBaseHours(double baseHours) { this.baseHours = baseHours; }
    public double getPerM2Hours() { return perM2Hours; }
    public void setPerM2Hours(double perM2Hours) { this.perM2Hours = perM2Hours; }
    public String getReason() { return reason.toString(); }
    public void setReason(String r) { this.reason = new StringBuilder(r); }
}
