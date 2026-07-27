package com.example.onlinemall.service;

import com.example.onlinemall.entity.Order;
import com.example.onlinemall.model.checkout.CheckoutRequest;
import com.example.onlinemall.model.checkout.PayInfo;
import com.example.onlinemall.model.checkout.PaymentCallback;

import java.util.List;

public interface OrderService {
    void createOrder(Order order);

    void updateOrder(Order order);

    void deleteOrder(Order order);

    Order getOrderById(Long id);

    List<Order> getAllOrders();

    /**
     * Update-with-Start：同步等到可支付，返回 payInfo；Workflow 继续等支付。
     */
    PayInfo checkout(CheckoutRequest request);

    /** 支付回调 → Signal Workflow */
    void handlePaymentCallback(PaymentCallback callback);
}
