package com.example.onlinemall.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLedger {
    private Long id;
    private String orderNumber;
    private Long skuId;
    private Integer qty;
    private Integer type;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
