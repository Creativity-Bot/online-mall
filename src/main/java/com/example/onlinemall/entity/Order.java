package com.example.onlinemall.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    private Long orderId;
    private String customerId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private BigDecimal actualPaid;
    private String remarks;
    private Integer status;
    private String dvyType;
    private Long dvyId;
    private String dvyFlowId;
    private BigDecimal freightAmount;
    private Long addrOrderId;
    private Integer productNums;
    private LocalDateTime payDeadline;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime payTime;
    private LocalDateTime dvyTime;
    private LocalDateTime finishedTime;
    private LocalDateTime cancelTime;
    private Integer isPayed;
    private Integer deleteStatus;
    private Integer refundSts;
    private BigDecimal reduceAmount;
    private Integer closeType;
}
