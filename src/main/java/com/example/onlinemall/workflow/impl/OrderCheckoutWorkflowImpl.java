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
import java.util.Objects;

@WorkflowImpl(workers = "order-service-worker")
public class OrderCheckoutWorkflowImpl implements OrderCheckoutWorkflow {

    private static final Logger log = Workflow.getLogger(OrderCheckoutWorkflowImpl.class);

    private static final Duration PAY_DEADLINE = Duration.ofMinutes(30);

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

    private String orderId;

    private boolean initDone = false;
    private Exception initError;

    private boolean payMethodSelected = false;
    private String selectedPayMethod;
    private boolean payInfoReady = false;
    private Exception createPaymentError;
    private PayInfo payInfo;

    private boolean paymentDone = false;
    private PaymentCallback paymentCallback;

    /** 主流程已进入取消/补偿，供 Update handler 快速失败 */
    private boolean cancelled = false;
    private String cancelReason;

    @Override
    public OrderCheckoutResult run(CheckoutRequest request) {
        this.orderId = request.getOrderId();
        Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());
        StockChange stockChange = buildStockChange(request);
        long deadlineMs = Workflow.currentTimeMillis() + PAY_DEADLINE.toMillis();

        // ---- 阶段 A：建单 + 预占，尽快返回选支付页 ----
        try {
            orderActivities.createOrder(request);
            saga.addCompensation(orderActivities::markOrderCancelled, orderId);

            stockActivities.reserveStock(stockChange);
            saga.addCompensation(stockActivities::releaseStock, stockChange);
        } catch (Exception e) {
            initError = e;
        } finally {
            initDone = true;
        }

        if (initError != null) {
            markCancelled(initError.getMessage() == null ? "checkout init failed" : initError.getMessage());
            saga.compensate();
            return new OrderCheckoutResult(orderId, "CANCELLED", cancelReason);
        }

        // ---- 等待 1：用户选择支付方式 ----
        Duration remainToSelect = remaining(deadlineMs);
        if (remainToSelect.isZero() || remainToSelect.isNegative()
                || !Workflow.await(remainToSelect, () -> payMethodSelected || cancelled)) {
            markCancelled("select payment timeout");
            saga.compensate();
            return new OrderCheckoutResult(orderId, "CANCELLED", cancelReason);
        }
        if (cancelled) {
            saga.compensate();
            return new OrderCheckoutResult(orderId, "CANCELLED", cancelReason);
        }

        try {
            payInfo = paymentActivities.createPayment(CreatePaymentRequest.builder()
                    .orderId(orderId)
                    .customerId(request.getCustomerId())
                    .totalAmount(request.getTotalAmount())
                    .payMethod(selectedPayMethod)
                    .build());
            saga.addCompensation(paymentActivities::cancelPayment, orderId);
        } catch (Exception e) {
            createPaymentError = e;
            markCancelled(e.getMessage() == null ? "create payment failed" : e.getMessage());
            saga.compensate();
            return new OrderCheckoutResult(orderId, "CANCELLED", cancelReason);
        } finally {
            // 唤醒 selectPayment Update（成功或失败）
            payInfoReady = true;
        }

        // ---- 等待 2：支付渠道回调 ----
        Duration remainToPay = remaining(deadlineMs);
        boolean paidInTime = !remainToPay.isZero() && !remainToPay.isNegative()
                && Workflow.await(remainToPay, () -> paymentDone || cancelled);
        if (cancelled) {
            saga.compensate();
            return new OrderCheckoutResult(orderId, "CANCELLED", cancelReason);
        }
        if (!paidInTime || paymentCallback == null || !paymentCallback.isSuccess()) {
            markCancelled("payment timeout or failed");
            saga.compensate();
            return new OrderCheckoutResult(orderId, "CANCELLED", cancelReason);
        }

        // 已付款：只向前恢复，失败则一直重试，不做 Saga 补偿
        postPaymentOrderActivities.markOrderPaid(orderId);
        postPaymentStockActivities.confirmStock(stockChange);
        postPaymentOrderActivities.clearCart(request.getCustomerId());
        return new OrderCheckoutResult(orderId, "PAID", "ok");
    }

    private void markCancelled(String reason) {
        this.cancelled = true;
        this.cancelReason = reason;
    }

    private static Duration remaining(long deadlineMs) {
        long left = deadlineMs - Workflow.currentTimeMillis();
        return left <= 0 ? Duration.ZERO : Duration.ofMillis(left);
    }

    private StockChange buildStockChange(CheckoutRequest request) {
        StockChange stockChange = new StockChange();
        stockChange.setOrderId(request.getOrderId());

        if (request.getItems() == null) {
            return stockChange;
        }
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
    public CheckoutReady waitUntilPayable() {
        Workflow.await(() -> initDone || cancelled);
        if (initError != null) {
            throw ApplicationFailure.newNonRetryableFailure(
                    initError.getMessage() == null ? "checkout init failed" : initError.getMessage(),
                    "CheckoutInitFailed");
        }
        rejectIfCancelled();
        return new CheckoutReady(orderId, "WAIT_SELECT_PAYMENT");
    }

    @Override
    public PayInfo selectPayment(SelectPaymentRequest request) {
        if (request == null || request.getPayMethod() == null || request.getPayMethod().isBlank()) {
            throw ApplicationFailure.newNonRetryableFailure("payMethod is required", "InvalidSelectPayment");
        }
        Workflow.await(() -> initDone || cancelled);
        if (initError != null) {
            throw ApplicationFailure.newNonRetryableFailure(
                    initError.getMessage() == null ? "checkout init failed" : initError.getMessage(),
                    "CheckoutInitFailed");
        }
        rejectIfCancelled();
        if (paymentDone) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "payment already completed",
                    "PaymentAlreadyCompleted");
        }

        // 幂等：已创建过支付单则直接返回
        if (payInfo != null && payInfoReady) {
            return payInfo;
        }

        this.selectedPayMethod = request.getPayMethod();
        this.payMethodSelected = true;

        Workflow.await(() -> payInfoReady || cancelled);
        if (createPaymentError != null) {
            throw ApplicationFailure.newNonRetryableFailure(
                    createPaymentError.getMessage() == null
                            ? "create payment failed"
                            : createPaymentError.getMessage(),
                    "CreatePaymentFailed");
        }
        rejectIfCancelled();
        return payInfo;
    }

    private void rejectIfCancelled() {
        if (cancelled) {
            throw ApplicationFailure.newNonRetryableFailure(
                    cancelReason == null ? "checkout cancelled" : cancelReason,
                    "CheckoutCancelled");
        }
    }

    @Override
    public void paymentCompleted(PaymentCallback callback) {
        if (callback == null) {
            log.warn("ignore paymentCompleted: null callback");
            return;
        }
        // 支付单尚未创建前忽略回调，避免伪造提前 Signal
        if (payInfo == null) {
            log.warn("ignore paymentCompleted before payInfo ready, orderId={}", callback.getOrderId());
            return;
        }
        if (cancelled) {
            log.warn("ignore paymentCompleted after cancelled, orderId={}", orderId);
            return;
        }
        if (!Objects.equals(orderId, callback.getOrderId())) {
            log.warn("ignore paymentCompleted orderId mismatch, expected={}, actual={}",
                    orderId, callback.getOrderId());
            return;
        }
        if (!Objects.equals(payInfo.getPaymentId(), callback.getPaymentId())) {
            log.warn("ignore paymentCompleted paymentId mismatch, expected={}, actual={}, orderId={}",
                    payInfo.getPaymentId(), callback.getPaymentId(), orderId);
            return;
        }
        this.paymentCallback = callback;
        this.paymentDone = true;
    }
}
