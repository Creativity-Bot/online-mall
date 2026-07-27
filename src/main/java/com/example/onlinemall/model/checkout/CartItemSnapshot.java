package com.example.onlinemall.model.checkout;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemSnapshot {
    private Long skuId;
    private Long prodId;
    private Integer quantity;
    private BigDecimal unitPrice;
}
