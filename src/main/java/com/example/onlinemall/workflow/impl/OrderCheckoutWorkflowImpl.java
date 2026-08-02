package com.example.onlinemall.workflow.impl;

import com.example.onlinemall.activities.CreateOrderActivities;
import com.example.onlinemall.activities.PaymentActivities;
import com.example.onlinemall.activities.ReserveStockActivities;
import com.example.onlinemall.model.checkout.*;
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

    /** 支付成功后：只向前重试，不回滚；maximumAttempts=0 表示无限重试 */
    private static final ActivityOptions POST_PAYMENT_OPTIONS =
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(15))
                    .setRetryOptions(
                            RetryOptions.newBuilder()
                                    .setInitialInterval(Duration.ofSeconds(1))
                                    .setBackoffCoefficient(2.0)
                                    .setMaximumInterval(Duration.ofMinutes(1))
                                    .setMaximumAttempts(0)
                                    .build())
                    .build();

    private final CreateOrderActivities orderActivities =
            Workflow.newActivityStub(CreateOrderActivities.class, DEFAULT_OPTIONS);

    private final ReserveStockActivities stockActivities =
            Workflow.newActivityStub(ReserveStockActivities.class, DEFAULT_OPTIONS);

    private final PaymentActivities paymentActivities =
            Workflow.newActivityStub(PaymentActivities.class, DEFAULT_OPTIONS);

    private final CreateOrderActivities postPaymentOrderActivities =
            Workflow.newActivityStub(CreateOrderActivities.class, POST_PAYMENT_OPTIONS);

    private final ReserveStockActivities postPaymentStockActivities =
            Workflow.newActivityStub(ReserveStockActivities.class, POST_PAYMENT_OPTIONS);

    private boolean initDone = false;
    private Exception initError;
    private PayInfo payInfo;

    private boolean paymentDone = false;
    private PaymentCallback paymentCallback;

    @Override
    public OrderCheckoutResult run(CheckoutRequest request) {
        Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());
        StockChange stockChange = buildStockChange(request);

        // ---- 阶段 A：尽快让前端可支付 ----
        try {
            // 因为用了redis，所以可以先建单
            orderActivities.createOrder(request);
            saga.addCompensation(orderActivities::markOrderCancelled, request.getOrderId());

            stockActivities.reserveStock(stockChange);
            saga.addCompensation(stockActivities::releaseStock, stockChange);

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

        // 已付款：只向前恢复，失败则一直重试，不做 Saga 补偿
        postPaymentOrderActivities.markOrderPaid(request.getOrderId());
        postPaymentStockActivities.confirmStock(stockChange);
        postPaymentOrderActivities.clearCart(request.getCustomerId());
        return new OrderCheckoutResult(request.getOrderId(), "PAID", "ok");
    }

    private StockChange buildStockChange(CheckoutRequest request) {
        StockChange stockChange = new StockChange();
        stockChange.setOrderId(request.getOrderId());

        request.getItems().forEach(shop -> {
            if (shop.getProductItemList() == null) {
                return;
            }
            shop.getProductItemList().forEach(productItem -> {
                stockChange.addSku(Sku.builder()
                        .skuId(productItem.getSkuId())
                        .quantity(productItem.getQuantity())
                        .build());

                stockChange.addOrMergeSpu(Spu.builder()
                        .prodId(productItem.getProdId())
                        .quantity(productItem.getQuantity())
                        .build());
            });
        });

        stockChange.sort();
        return stockChange;
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
