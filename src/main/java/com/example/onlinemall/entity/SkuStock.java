package com.example.onlinemall.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SkuStock {
    private Long id;
    private Long skuId;
    private Integer reservedStocks;
    private Integer availableStocks;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
