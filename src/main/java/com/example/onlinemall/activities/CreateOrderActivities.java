package com.example.onlinemall.activities;

import com.example.onlinemall.model.checkout.CheckoutRequest;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface CreateOrderActivities {

    @ActivityMethod
    void createOrder(CheckoutRequest request);

    @ActivityMethod
    void clearCart(String customerId);

    @ActivityMethod
    void markOrderPaid(String orderId);

    @ActivityMethod
    void markOrderCancelled(String orderId);
}
