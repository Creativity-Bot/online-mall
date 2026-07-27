package com.example.onlinemall.workflow.impl;

import com.example.onlinemall.activities.CreateOrderActivities;
import com.example.onlinemall.activities.PaymentActivities;
import com.example.onlinemall.activities.ReserveStockActivities;
import com.example.onlinemall.model.checkout.CheckoutRequest;
import com.example.onlinemall.model.checkout.OrderCheckoutResult;
import com.example.onlinemall.model.checkout.PayInfo;
import com.example.onlinemall.model.checkout.PaymentCallback;
import com.example.onlinemall.workflow.OrderCheckoutWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

@WorkflowImpl(workers = "order-service-worker")
public class OrderCheckoutWorkflowImpl implements OrderCheckoutWorkflow {

    private static final Logger log = Workflow.getLogger(OrderCheckoutWorkflowImpl.class);

    private static final ActivityOptions DEFAULT_OPTIONS =
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(15))
                    .setRetryOptions(
                            RetryOptions.newBuilder()
                                    .setMaximumAttempts(3)
                                    .build())
                    .build();

    private final CreateOrderActivities orderActivities =
            Workflow.newActivityStub(CreateOrderActivities.class, DEFAULT_OPTIONS);

    private final ReserveStockActivities stockActivities =
            Workflow.newActivityStub(ReserveStockActivities.class, DEFAULT_OPTIONS);

    private final PaymentActivities paymentActivities =
            Workflow.newActivityStub(PaymentActivities.class, DEFAULT_OPTIONS);

    private boolean initDone = false;
    private Exception initError;
    private PayInfo payInfo;

    private boolean paymentDone = false;
    private PaymentCallback paymentCallback;

    @Override
    public OrderCheckoutResult run(CheckoutRequest request) {
        Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());

        // ---- 阶段 A：尽快让前端可支付 ----
        try {
            orderActivities.createOrder(request);
            saga.addCompensation(orderActivities::markOrderCancelled, request.getOrderId());

            stockActivities.reserveStock(request);
            saga.addCompensation(stockActivities::releaseStock, request.getOrderId());

            payInfo = paymentActivities.createPayment(request);
            saga.addCompensation(paymentActivities::cancelPayment, request.getOrderId());
        } catch (Exception e) {
            initError = e;
        } finally {
            // 唤醒 waitUntilPayable Update（无论成功失败）
            initDone = true;
        }

        if (initError != null) {
            saga.compensate();
            return new OrderCheckoutResult(request.getOrderId(), "CANCELLED", initError.getMessage());
        }

        // ---- 阶段 B：等支付（HTTP 已提前返回）----
        boolean paidInTime = Workflow.await(Duration.ofMinutes(30), () -> paymentDone);
        if (!paidInTime || paymentCallback == null || !paymentCallback.isSuccess()) {
            saga.compensate();
            return new OrderCheckoutResult(request.getOrderId(), "CANCELLED", "payment timeout or failed");
        }

        orderActivities.markOrderPaid(request.getOrderId());
        stockActivities.confirmStock(request.getOrderId());
        orderActivities.clearCart(request.getCustomerId());
        return new OrderCheckoutResult(request.getOrderId(), "PAID", "ok");
    }

    @Override
    public PayInfo waitUntilPayable() {
        Workflow.await(() -> initDone);
        if (initError != null) {
            throw ApplicationFailure.newNonRetryableFailure(
                    initError.getMessage() == null ? "checkout init failed" : initError.getMessage(),
                    "CheckoutInitFailed");
        }
        return payInfo;
    }

    @Override
    public void paymentCompleted(PaymentCallback callback) {
        this.paymentCallback = callback;
        this.paymentDone = true;
    }
}
