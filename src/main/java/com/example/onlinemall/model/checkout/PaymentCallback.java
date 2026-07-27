package com.example.onlinemall.model.checkout;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallback {
    private String orderId;
    private String paymentId;
    private boolean success;
}
