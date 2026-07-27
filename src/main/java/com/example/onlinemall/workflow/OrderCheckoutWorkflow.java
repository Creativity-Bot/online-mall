package com.example.onlinemall.workflow;

import com.example.onlinemall.model.checkout.CheckoutRequest;
import com.example.onlinemall.model.checkout.OrderCheckoutResult;
import com.example.onlinemall.model.checkout.PayInfo;
import com.example.onlinemall.model.checkout.PaymentCallback;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderCheckoutWorkflow {

    @WorkflowMethod
    OrderCheckoutResult run(CheckoutRequest request);

    /**
     * Early Return：等阶段 A（建单/预留/创建支付）完成后，把 payInfo 同步返回给 HTTP 调用方。
     * Workflow 主流程继续跑阶段 B（等支付）。
     */
    @UpdateMethod(name = "waitUntilPayable")
    PayInfo waitUntilPayable();

    /** 支付渠道回调后，由后端 Signal 进来 */
    @SignalMethod
    void paymentCompleted(PaymentCallback callback);
}
