package com.example.onlinemall.model.checkout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

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
