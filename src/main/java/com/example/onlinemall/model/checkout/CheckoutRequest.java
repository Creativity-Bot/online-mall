package com.example.onlinemall.model.checkout;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    /** 业务侧预先生成，同时作为 Temporal WorkflowId，保证幂等 */
    private String orderId;
    private String customerId;
    /** 下单瞬间的购物车快照，避免流程中途购物车被改 */
    private List<CartItemSnapshot> items = new ArrayList<>();
    private BigDecimal totalAmount;
}
