package com.example.onlinemall.model.checkout;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 阶段 A 完成后返回：订单已建、库存已预占，等待用户选择支付方式。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutReady {
    private String orderId;
    /** WAIT_SELECT_PAYMENT */
    private String status;
}
