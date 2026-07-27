package com.example.onlinemall.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderSettlement {
    private Long settlementId;
    private String orderNumber;
    private String merchantTradeNo;
    private String bizPayNo;
    private Integer payType;
    private String channel;
    private BigDecimal payAmount;
    private Integer payStatus;
    private Integer isClearing;
    private LocalDateTime paidAt;
    private LocalDateTime clearingTime;
    private String rawNotify;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
