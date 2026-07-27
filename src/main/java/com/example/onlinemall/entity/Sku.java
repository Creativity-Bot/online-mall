package com.example.onlinemall.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Sku {
    private Long skuId;
    private Long prodId;
    private String skuCode;
    private String properties;
    private BigDecimal originalPrice;
    private BigDecimal currentPrice;
    private String modelId;
    private String pic;
    private String skuName;
    private Double weight;
    private Double volume;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
