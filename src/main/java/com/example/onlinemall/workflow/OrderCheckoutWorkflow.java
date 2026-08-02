package com.example.onlinemall.workflow;

import com.example.onlinemall.model.checkout.CheckoutReady;
import com.example.onlinemall.model.checkout.CheckoutRequest;
import com.example.onlinemall.model.checkout.OrderCheckoutResult;
import com.example.onlinemall.model.checkout.PayInfo;
import com.example.onlinemall.model.checkout.PaymentCallback;
import com.example.onlinemall.model.checkout.SelectPaymentRequest;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderCheckoutWorkflow {

    @WorkflowMethod
    OrderCheckoutResult run(CheckoutRequest request);

    /**
     * Early Return Update：阶段 A（建单/预占）完成后返回，前端进入选支付方式页。
     * 与 Client 的 Update-with-Start 配合使用。
     */
    @UpdateMethod(name = "waitUntilPayable")
    CheckoutReady waitUntilPayable();

    /**
     * 用户选择支付方式（Update，非 Signal）：唤醒主流程创建支付单，并同步返回 PayInfo。
     */
    @UpdateMethod(name = "selectPayment")
    PayInfo selectPayment(SelectPaymentRequest request);

    /** 支付渠道 webhook 回调：Signal 唤醒主流程，校验 orderId/paymentId 后继续确认付款 */
    @SignalMethod
    void paymentCompleted(PaymentCallback callback);
}
