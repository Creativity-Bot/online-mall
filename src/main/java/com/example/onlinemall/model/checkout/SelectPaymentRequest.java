package com.example.onlinemall.model.checkout;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelectPaymentRequest {
    private String orderId;
    /** 如 WECHAT / ALIPAY */
    private String payMethod;
}
