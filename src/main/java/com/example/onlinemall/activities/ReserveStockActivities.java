package com.example.onlinemall.activities;

import com.example.onlinemall.dto.CheckoutRequestDTO;
import com.example.onlinemall.dto.StockChange;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ReserveStockActivities {

    @ActivityMethod
    void reserveStock(CheckoutRequestDTO stockChange);

    @ActivityMethod
    void releaseStock(CheckoutRequestDTO stockChange);

    @ActivityMethod
    void confirmStock(CheckoutRequestDTO stockChange);
}
