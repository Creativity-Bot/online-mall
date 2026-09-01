package com.example.onlinemall.workflow;

import com.example.onlinemall.dto.*;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderCheckoutWorkflow {

    @WorkflowMethod
    ResponseTemplate<OrderCheckoutResult> run(CheckoutRequestDTO request);

    /**
     * Early Return Update：阶段 A（建单/预占）完成后返回，前端进入选支付方式页。
     * 与 Client 的 Update-with-Start 配合使用。
     */
    @UpdateMethod(name = "waitUntilPayable")
    ResponseTemplate<?> waitUntilPayable(CheckoutRequestDTO request);


    /** 支付渠道 webhook 回调：Signal 唤醒主流程，校验 orderId/paymentId 后继续确认付款 */
    @SignalMethod
    void paymentCompleted(PaymentCallback callback);
}
