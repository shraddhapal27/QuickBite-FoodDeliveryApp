package com.quickbite.payment.dto;

import com.quickbite.payment.entity.PaymentMode;
import lombok.Data;

@Data
public class PaymentRequestDTO {
    private Long orderId;
    private Long customerId;
    private Double amount;
    private PaymentMode mode;
}
