package com.example.onlinemall.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ProductItem {
    private Long skuId;
    private Long prodId;
    private Integer quantity;
    private BigDecimal unitPrice;
}
