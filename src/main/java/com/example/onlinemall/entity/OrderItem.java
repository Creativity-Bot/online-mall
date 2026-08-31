package com.example.onlinemall.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderItem {
    private Long orderItemId;
    private Long shopId;
    private Long orderId;
    private Long prodId;
    private Long skuId;
    private Integer prodCount;
    private String prodName;
    private String skuName;
    private String pic;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
}
