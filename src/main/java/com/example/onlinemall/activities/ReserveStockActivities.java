package com.example.onlinemall.activities;

import com.example.onlinemall.model.checkout.StockChange;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ReserveStockActivities {

    @ActivityMethod
    void reserveStock(StockChange stockChange);

    @ActivityMethod
    void confirmStock(StockChange stockChange);

    @ActivityMethod
    void releaseStock(StockChange stockChange);
}
