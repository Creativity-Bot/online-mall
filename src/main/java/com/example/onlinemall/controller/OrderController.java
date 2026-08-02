package com.example.onlinemall.controller;

import com.example.onlinemall.entity.Order;
import com.example.onlinemall.exception.CheckoutWorkflowException;
import com.example.onlinemall.model.ResponseTemplate;
import com.example.onlinemall.model.checkout.*;
import com.example.onlinemall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/get")
    public ResponseTemplate retrieveOrderDetail(@RequestParam Long id) {
        Order order = orderService.getOrderById(id);
        return new ResponseTemplate(200, "SUCCESS", order);
    }

    /**
     * 确认下单：Update-with-Start。
     * 启动 OrderCheckoutWorkflow，并通过 waitUntilPayable Update 同步等到建单 + 预占完成，
     * 返回后进入选支付方式页；主流程继续挂起。
     * todo get the customer id from JWT
     */
    @PostMapping("/checkout")
    public ResponseTemplate checkout(@RequestBody CheckoutRequest request) {
        try {
            CheckoutReady ready = orderService.checkout(request);
            return new ResponseTemplate(200, "WAIT_SELECT_PAYMENT", ready);
        } catch (CheckoutWorkflowException e) {
            return new ResponseTemplate(400, e.getErrorType(), e.getMessage());
        } catch (IllegalArgumentException e) {
            return new ResponseTemplate(400, "BAD_REQUEST", e.getMessage());
        }
    }

    /**
     * 选择支付方式：Workflow Update（非 Signal）。
     * 唤醒挂起的 Workflow 创建渠道支付单，阻塞直到返回 PayInfo（payUrl 等）。
     */
    @PostMapping("/pay")
    public ResponseTemplate selectPayment(@RequestBody SelectPaymentRequest request) {
        try {
            PayInfo payInfo = orderService.selectPayment(request);
            return new ResponseTemplate(200, "WAIT_PAY", payInfo);
        } catch (CheckoutWorkflowException e) {
            return new ResponseTemplate(400, e.getErrorType(), e.getMessage());
        } catch (IllegalArgumentException e) {
            return new ResponseTemplate(400, "BAD_REQUEST", e.getMessage());
        }
    }

    /**
     * 支付渠道 webhook：Signal paymentCompleted，唤醒 Workflow 完成后续确认。
     */
    @PostMapping("/payment/callback")
    public ResponseTemplate paymentCallback(@RequestBody PaymentCallback callback) {
        try {
            orderService.handlePaymentCallback(callback);
            return new ResponseTemplate(200, "SIGNALED", null);
        } catch (IllegalArgumentException e) {
            return new ResponseTemplate(400, "BAD_REQUEST", e.getMessage());
        }
    }
}
