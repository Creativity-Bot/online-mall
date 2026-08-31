package com.example.onlinemall.workflow.impl;

import com.example.onlinemall.activities.CreateOrderActivities;
import com.example.onlinemall.activities.ReserveStockActivities;
import com.example.onlinemall.dto.*;
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

    private static final Duration PAY_DEADLINE = Duration.ofMinutes(30);

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

    private boolean initDone = false;

    private Exception initError;

    private boolean paymentDone = false;

    private Long orderId;


    @Override
    public ResponseTemplate<OrderCheckoutResult> run(CheckoutRequestDTO request) {
        Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());

        // ---- step A：create order + reserve sku and return order Id  ----
        try {
            orderActivities.createOrder(request);
            saga.addCompensation(orderActivities::markOrderCancelled, request);

            stockActivities.reserveStock(request);
            saga.addCompensation(stockActivities::releaseStock, request);
        } catch (Exception e) {
            initError = e;
            if(e.getCause() instanceof ApplicationFailure
            && "InsufficientStock".equals(((ApplicationFailure)e.getCause()).getType())){
                log.info("compensation starts from step of reservation stock");
                saga.compensate();
            }
            // for other unexpected error, need to reset manually (such as timeout)
        } finally {
            initDone = true;
        }

        if (initError != null) {
            log.error("Order workflow Exception: {}", initError.getMessage());
            throw ApplicationFailure.newNonRetryableFailure(
                    initError.getMessage(),
                    "CheckoutInitFailed");
        }

        // ---- step B：set a timer for 30 mins for customer to pay ----
        boolean await = Workflow.await(PAY_DEADLINE, () -> paymentDone);

        if (!await) {
            log.error("Order {} timeout", request.getOrderId());
            saga.compensate();
        } else {
            // mark order as paid
            orderActivities.markOrderPaid(request);
        }

        stockActivities.confirmStock(request);
        //postPaymentOrderActivities.clearCart(request);
        return ResponseTemplate.success(new OrderCheckoutResult(request.getOrderId(), "PAID", "ok"));
    }

    @Override
    public ResponseTemplate<?> waitUntilPayable() {
        Workflow.await(() -> initDone);
        if (initError != null) {
            if (initError.getCause() instanceof ApplicationFailure
                    && "InsufficientStock".equals(((ApplicationFailure) initError.getCause()).getType())) {
                return ResponseTemplate.fail("insufficient stock");
            } else {
                return ResponseTemplate.fail("checkout meets unexpected issue");
            }
        }
        return ResponseTemplate.success(orderId.toString());
    }

    @Override
    public void paymentCompleted(PaymentCallback callback) {
        this.paymentDone = true;
    }
}
