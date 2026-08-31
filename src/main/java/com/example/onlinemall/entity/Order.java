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
    /** 业务侧手动赋值的主键 */
    private Long orderId;
    private String customerId;
    private BigDecimal totalAmount;
    private BigDecimal actualPaid;
    private String remarks;
    private Integer status;
    /** 库存生命周期：见 {@link com.example.onlinemall.constant.StockStatus} */
    private Integer stockStatus;
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
