package com.example.onlinemall.activities.impl;

import com.example.onlinemall.activities.PaymentActivities;
import com.example.onlinemall.model.checkout.CheckoutRequest;
import com.example.onlinemall.model.checkout.PayInfo;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(workers = "order-service-worker")
public class PaymentActivitiesImpl implements PaymentActivities {

    private static final Logger log = LoggerFactory.getLogger(PaymentActivitiesImpl.class);

    @Override
    public PayInfo createPayment(CheckoutRequest request) {
        log.info("createPayment orderId={} amount={}", request.getOrderId(), request.getTotalAmount());
        // TODO: call payment provider, persist payment record
        String paymentId = "pay-" + request.getOrderId();
        return new PayInfo(
                request.getOrderId(),
                "WAIT_PAY",
                "https://pay.example.com/checkout?orderId=" + request.getOrderId(),
                paymentId
        );
    }

    @Override
    public void cancelPayment(String orderId) {
        log.info("cancelPayment orderId={} paymentId={}", orderId, "pay-" + orderId);
        // TODO: close provider precreate order + mark local payment CLOSED (idempotent)
    }
}
