package com.ucm.analysis;

import com.ucm.analysis.entity.ViolationCase;
import com.ucm.analysis.repository.ViolationCaseRepository;
import com.ucm.analysis.service.GridUtil;
import com.ucm.controller.AuthController;
import com.ucm.entity.SysUser;
import com.ucm.repository.SysUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 智能分析模块演示数据：补充执法队员账号，并生成一批历史处置案例
 * （分布在演示影像 bbox 附近的若干网格内，队员效率有差异），
 * 使整改推荐 / 工期预测 / 队员匹配开箱即可演示。仅在案例库为空时执行。
 */
@Component
@Order(2) // 在 DataInitializer 之后执行
public class AnalysisDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AnalysisDataInitializer.class);

    private final SysUserRepository userRepo;
    private final ViolationCaseRepository caseRepo;

    public AnalysisDataInitializer(SysUserRepository userRepo, ViolationCaseRepository caseRepo) {
        this.userRepo = userRepo;
        this.caseRepo = caseRepo;
    }

    @Override
    public void run(String... args) {
        List<SysUser> operators = ensureOperators();
        if (caseRepo.count() > 0 || operators.isEmpty()) return;

        Random rnd = new Random(42); // 固定种子，保证每次全新部署生成一致
        List<ViolationCase> cases = new ArrayList<>();

        // 四个聚集网格中心（落在 0.01° 网格内，演示重复违建高发区）+ 散布点
        double[][] clusters = {
                {114.305, 30.555, 8}, {114.315, 30.565, 6},
                {114.325, 30.575, 5}, {114.335, 30.585, 4},
        };
        for (double[] c : clusters) {
            for (int i = 0; i < (int) c[2]; i++) {
                cases.add(buildCase(rnd, operators,
                        c[0] + (rnd.nextDouble() - 0.5) * 0.006,
                        c[1] + (rnd.nextDouble() - 0.5) * 0.006));
            }
        }
        for (int i = 0; i < 25; i++) {
            cases.add(buildCase(rnd, operators,
                    114.30 + rnd.nextDouble() * 0.06,
                    30.55 + rnd.nextDouble() * 0.05));
        }

        caseRepo.saveAll(cases);
        log.info("智能分析演示数据初始化完成: {} 条历史案例, {} 名队员", cases.size(), operators.size());
    }

    /** 确保演示队员账号存在（幂等） */
    private List<SysUser> ensureOperators() {
        List<SysUser> operators = new ArrayList<>();
        String[][] seeds = {
                {"zhangsan", "张三"}, {"lisi", "李四"}, {"wangwu", "王五"}, {"zhaoliu", "赵六"},
        };
        for (String[] s : seeds) {
            SysUser u = userRepo.findByUsername(s[0]).orElseGet(() -> {
                SysUser nu = new SysUser();
                nu.setUsername(s[0]);
                nu.setPassword(AuthController.sha256("123456"));
                nu.setRealName(s[1]);
                nu.setRole(SysUser.Role.OPERATOR);
                return userRepo.save(nu);
            });
            operators.add(u);
        }
        return operators;
    }

    private ViolationCase buildCase(Random rnd, List<SysUser> operators, double lng, double lat) {
        ViolationCase c = new ViolationCase();

        // 违建类型分布：屋顶加盖/违法扩建各 30%，占地/临时各 20%
        double t = rnd.nextDouble();
        ViolationCase.ViolationType type = t < 0.3 ? ViolationCase.ViolationType.ROOFTOP_ADDITION
                : t < 0.6 ? ViolationCase.ViolationType.ILLEGAL_EXPANSION
                : t < 0.8 ? ViolationCase.ViolationType.OCCUPY_LAND
                : ViolationCase.ViolationType.TEMP_STRUCTURE;
        c.setViolationType(type);

        // 面积按类型区间生成
        double area = switch (type) {
            case ROOFTOP_ADDITION -> 20 + rnd.nextDouble() * 240;
            case ILLEGAL_EXPANSION -> 20 + rnd.nextDouble() * 330;
            case OCCUPY_LAND -> 30 + rnd.nextDouble() * 570;
            case TEMP_STRUCTURE -> 10 + rnd.nextDouble() * 110;
        };
        area = Math.round(area * 10) / 10.0;
        c.setAreaM2(area);

        c.setLongitude(Math.round(lng * 1e6) / 1e6);
        c.setLatitude(Math.round(lat * 1e6) / 1e6);
        c.setGridCode(GridUtil.gridCode(lng, lat));

        // 整改方式：85% 与规则口径一致，15% 噪声（模拟人工裁量）
        ViolationCase.RectifyMethod method = ruleLikeMethod(type, area);
        if (rnd.nextDouble() < 0.15) {
            ViolationCase.RectifyMethod[] all = ViolationCase.RectifyMethod.values();
            method = all[rnd.nextInt(all.length)];
        }
        c.setRectifyMethod(method);

        // 队员：张三 30% / 李四 25% / 王五 25% / 赵六 20%，效率因子有差异
        double m = rnd.nextDouble();
        int idx = m < 0.3 ? 0 : m < 0.55 ? 1 : m < 0.8 ? 2 : 3;
        SysUser operator = operators.get(Math.min(idx, operators.size() - 1));
        double memberFactor = switch (idx) {
            case 0 -> 0.85;  // 张三效率高
            case 1 -> 1.25;  // 李四偏慢
            case 2 -> 1.0;   // 王五平均
            default -> 0.95; // 赵六略快
        };
        c.setOperatorId(operator.getId());
        c.setOperatorName(operator.getRealName());

        // 处理天数：按整改方式基线 × 队员效率 × 随机抖动
        double baseDays = switch (method) {
            case SELF_DEMOLITION -> 2 + rnd.nextDouble() * 4;
            case ASSISTED_DEMOLITION -> 6 + rnd.nextDouble() * 8;
            case FORCED_DEMOLITION -> 12 + rnd.nextDouble() * 16;
        };
        c.setDurationDays(Math.round(baseDays * memberFactor * 10) / 10.0);

        // 实际工时：规则工时（基础 + 每平米 × 面积）× 0.85~1.2 抖动
        double baseHours = switch (method) {
            case SELF_DEMOLITION -> 4 + 0.05 * area;
            case ASSISTED_DEMOLITION -> 8 + 0.10 * area;
            case FORCED_DEMOLITION -> 16 + 0.15 * area;
        };
        c.setWorkHours(Math.round(baseHours * (0.85 + rnd.nextDouble() * 0.35) * 10) / 10.0);

        // 办结时间：最近 120 天内
        c.setFinishedAt(LocalDateTime.now().minusDays(1 + rnd.nextInt(120)));
        return c;
    }

    /** 与 rectification.drl 口径一致的整改方式（用于生成高一致性训练数据） */
    private static ViolationCase.RectifyMethod ruleLikeMethod(ViolationCase.ViolationType type, double area) {
        if (area >= 500) return ViolationCase.RectifyMethod.FORCED_DEMOLITION;
        return switch (type) {
            case ROOFTOP_ADDITION -> area >= 200 ? ViolationCase.RectifyMethod.FORCED_DEMOLITION
                    : area >= 80 ? ViolationCase.RectifyMethod.ASSISTED_DEMOLITION
                    : ViolationCase.RectifyMethod.SELF_DEMOLITION;
            case ILLEGAL_EXPANSION -> area >= 300 ? ViolationCase.RectifyMethod.FORCED_DEMOLITION
                    : area >= 50 ? ViolationCase.RectifyMethod.ASSISTED_DEMOLITION
                    : ViolationCase.RectifyMethod.SELF_DEMOLITION;
            case OCCUPY_LAND -> area >= 100 ? ViolationCase.RectifyMethod.FORCED_DEMOLITION
                    : ViolationCase.RectifyMethod.ASSISTED_DEMOLITION;
            case TEMP_STRUCTURE -> area >= 50 ? ViolationCase.RectifyMethod.ASSISTED_DEMOLITION
                    : ViolationCase.RectifyMethod.SELF_DEMOLITION;
        };
    }
}
