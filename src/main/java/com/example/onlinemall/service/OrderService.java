package com.example.onlinemall.service;

import com.example.onlinemall.dto.*;
import com.example.onlinemall.entity.Order;

import java.util.List;

public interface OrderService {
    void createOrder(Order order);

    void updateOrder(Order order);

    void deleteOrder(Order order);

    Order getOrderById(Long id);

    List<Order> getAllOrders();

    /**
     * Update-with-Start：启动 Workflow，并通过 waitUntilPayable Update 同步等到建单+预占完成。
     * 返回可进入选支付页的状态；主流程继续挂起等待选支付。
     */
    ResponseTemplate<?> checkout(CheckoutRequestDTO request);

    /**
     * 选择支付方式 → Workflow Update，阻塞直到 createPayment 完成并返回渠道 PayInfo。
     */
    PayInfo pay(SelectPaymentRequest request);

    /** 支付渠道 webhook → Signal paymentCompleted，唤醒 Workflow */
    void handlePaymentCallback(PaymentCallback callback);
}
