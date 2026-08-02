package com.example.onlinemall.model.checkout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentRequest {
    private String orderId;
    private String customerId;
    private BigDecimal totalAmount;
    private String payMethod;
}
