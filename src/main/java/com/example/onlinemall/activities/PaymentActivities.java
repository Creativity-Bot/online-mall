package com.example.onlinemall.activities;

import com.example.onlinemall.model.checkout.CreatePaymentRequest;
import com.example.onlinemall.model.checkout.PayInfo;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface PaymentActivities {

    /** 用户选定支付方式后创建渠道支付单 */
    @ActivityMethod
    PayInfo createPayment(CreatePaymentRequest request);

    /** 关闭渠道待支付单 + 本地支付记录，需幂等 */
    @ActivityMethod
    void cancelPayment(String orderId);
}
