package com.example.onlinemall.model.checkout;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayInfo {
    private String orderId;
    /** WAIT_PAY */
    private String status;
    /** 跳转支付页 / 调起 SDK 用 */
    private String payUrl;
    private String paymentId;
}
