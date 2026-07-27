package com.example.onlinemall.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Basket {
    private Long basketId;
    private Long prodId;
    private Long shopId;
    private Long skuId;
    private String customerId;
    private Integer prodCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
