package com.example.onlinemall.activities;

import com.example.onlinemall.dto.CheckoutRequestDTO;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface CreateOrderActivities {

    @ActivityMethod
    void createOrder(CheckoutRequestDTO request);

    @ActivityMethod
    void markOrderCancelled(CheckoutRequestDTO request);

    @ActivityMethod
    void markOrderPaid(CheckoutRequestDTO request);

}
