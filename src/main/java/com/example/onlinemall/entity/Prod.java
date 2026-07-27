package com.example.onlinemall.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Prod {
    private Long prodId;
    private String prodName;
    private Long shopId;
    private BigDecimal originalPrice;
    private BigDecimal currentPrice;
    private String summary;
    private String description;
    private String pic;
    private String imgs;
    private Integer status;
    private Long categoryId;
    private Integer soldNum;
    private Integer totalStocks;
    private String deliveryMode;
    private Long deliveryTemplateId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime putawayTime;
}
