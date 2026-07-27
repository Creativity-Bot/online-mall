package com.example.onlinemall.activities;

import com.example.onlinemall.model.checkout.CheckoutRequest;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ReserveStockActivities {

    @ActivityMethod
    void reserveStock(CheckoutRequest request);

    @ActivityMethod
    void confirmStock(String orderId);

    @ActivityMethod
    void releaseStock(String orderId);
}
