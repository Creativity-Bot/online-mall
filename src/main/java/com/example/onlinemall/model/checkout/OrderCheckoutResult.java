package com.example.onlinemall.model.checkout;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCheckoutResult {
    private String orderId;
    private String status;
    private String message;
}
