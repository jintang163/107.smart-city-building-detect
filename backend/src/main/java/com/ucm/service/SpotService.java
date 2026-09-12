package com.ucm.service;

import com.ucm.common.BizException;
import com.ucm.common.UserContext;
import com.ucm.entity.ChangeSpot;
import com.ucm.entity.WorkOrder;
import com.ucm.repository.ChangeSpotRepository;
import com.ucm.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 图斑审核：确认 -> 自动生成核查工单；排除 -> 标记 REJECTED */
@Service
public class SpotService {

    private final ChangeSpotRepository spotRepo;
    private final WorkOrderRepository orderRepo;

    public SpotService(ChangeSpotRepository spotRepo, WorkOrderRepository orderRepo) {
        this.spotRepo = spotRepo;
        this.orderRepo = orderRepo;
    }

    public List<ChangeSpot> list(ChangeSpot.Status status, Long compareTaskId) {
        if (compareTaskId != null) return spotRepo.findByCompareTaskId(compareTaskId);
        if (status != null) return spotRepo.findByStatus(status);
        return spotRepo.findAll();
    }

    @Transactional
    public WorkOrder review(Long spotId, boolean approved, String comment) {
        ChangeSpot spot = spotRepo.findById(spotId)
                .orElseThrow(() -> new BizException("图斑不存在: " + spotId));
        if (spot.getStatus() != ChangeSpot.Status.PENDING) {
            throw new BizException("图斑已审核，请勿重复操作");
        }
        spot.setReviewBy(UserContext.username());
        spot.setReviewAt(LocalDateTime.now());
        spot.setReviewComment(comment);
        if (!approved) {
            spot.setStatus(ChangeSpot.Status.REJECTED);
            spotRepo.save(spot);
            return null;
        }
        spot.setStatus(ChangeSpot.Status.CONFIRMED);
        spotRepo.save(spot);

        // 自动生成核查工单
        WorkOrder order = new WorkOrder();
        order.setCode(genCode());
        order.setSpot(spot);
        order.setTitle("疑似违建核查-" + spot.getId());
        order.setDescription(String.format("AI 检测新增构筑物，面积约 %.1f ㎡，置信度 %.0f%%，请现场核查。",
                spot.getAreaM2() == null ? 0 : spot.getAreaM2(),
                spot.getConfidence() == null ? 0 : spot.getConfidence() * 100));
        return orderRepo.save(order);
    }

    private String genCode() {
        return "WO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%03d", (int) (Math.random() * 1000));
    }
}
