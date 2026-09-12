package com.ucm.analysis.event;

import org.springframework.context.ApplicationEvent;

/**
 * 工单归档事件：携带整改方式标签，供智能分析模块同步训练案例。
 */
public class OrderArchivedEvent extends ApplicationEvent {

    private final Long orderId;
    /** 实际整改方式（RectifyMethod 枚举名，可空） */
    private final String rectifyMethod;

    public OrderArchivedEvent(Object source, Long orderId, String rectifyMethod) {
        super(source);
        this.orderId = orderId;
        this.rectifyMethod = rectifyMethod;
    }

    public Long getOrderId() { return orderId; }
    public String getRectifyMethod() { return rectifyMethod; }
}
