package com.quickbite.payment.dto;

import lombok.Data;

@Data
public class RazorpayOrderRequestDTO {
    private Long orderId;       // Your internal QuickBite order ID
    private Long customerId;
    private Double amount;      // In rupees (will be converted to paise internally)
    private String currency;    // e.g. "INR"
    private String receipt;     // Optional receipt note
}
