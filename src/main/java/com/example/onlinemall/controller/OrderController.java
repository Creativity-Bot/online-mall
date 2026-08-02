package com.example.onlinemall.controller;

import com.example.onlinemall.entity.Order;
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
     * 确认下单：Update-with-Start，HTTP 同步等到 WAIT_PAY + payInfo。
     * todo get the customer id from JWT
     */
    @PostMapping("/checkout")
    public ResponseTemplate checkout(@RequestBody CheckoutRequest request) {
        PayInfo payInfo = orderService.checkout(request);
        return new ResponseTemplate(200, "WAIT_PAY", payInfo);
    }

    /**
     * 支付渠道回调：Signal 进行中的 Workflow。
     */
    @PostMapping("/payment/callback")
    public ResponseTemplate paymentCallback(@RequestBody PaymentCallback callback) {
        orderService.handlePaymentCallback(callback);
        return new ResponseTemplate(200, "SIGNALED", null);
    }
}
