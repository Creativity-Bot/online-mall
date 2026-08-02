package com.example.onlinemall.activities.impl;

import com.example.onlinemall.activities.PaymentActivities;
import com.example.onlinemall.model.checkout.CreatePaymentRequest;
import com.example.onlinemall.model.checkout.PayInfo;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ActivityImpl(workers = "order-service-worker")
public class PaymentActivitiesImpl implements PaymentActivities {

    private static final Logger log = LoggerFactory.getLogger(PaymentActivitiesImpl.class);

    @Override
    public PayInfo createPayment(CreatePaymentRequest request) {
        if (request == null || !StringUtils.hasText(request.getOrderId())) {
            throw ApplicationFailure.newNonRetryableFailure("orderId is required", "InvalidPaymentRequest");
        }
        if (!StringUtils.hasText(request.getPayMethod())) {
            throw ApplicationFailure.newNonRetryableFailure("payMethod is required", "InvalidPaymentRequest");
        }

        log.info("createPayment orderId={} amount={} payMethod={}",
                request.getOrderId(), request.getTotalAmount(), request.getPayMethod());
        // TODO: call payment provider by payMethod, persist payment record
        String paymentId = "pay-" + request.getPayMethod().toLowerCase() + "-" + request.getOrderId();
        String payUrl = "https://pay.example.com/checkout?orderId=" + request.getOrderId()
                + "&method=" + request.getPayMethod();
        return new PayInfo(request.getOrderId(), "WAIT_PAY", payUrl, paymentId);
    }

    @Override
    public void cancelPayment(String orderId) {
        log.info("cancelPayment orderId={}", orderId);
        // TODO: close provider precreate order + mark local payment CLOSED (idempotent)
    }
}
