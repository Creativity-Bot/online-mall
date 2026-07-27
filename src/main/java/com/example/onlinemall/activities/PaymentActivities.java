package com.example.onlinemall.activities;

import com.example.onlinemall.model.checkout.CheckoutRequest;
import com.example.onlinemall.model.checkout.PayInfo;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface PaymentActivities {

    @ActivityMethod
    PayInfo createPayment(CheckoutRequest request);

    /** 关闭渠道待支付单 + 本地支付记录，需幂等 */
    @ActivityMethod
    void cancelPayment(String orderId);
}
