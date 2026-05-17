package com.quickbite.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderResponseDTO {
    private String razorpayOrderId;   // e.g. order_XXXXXXXXXXXXXXXXXX
    private Long   quickbiteOrderId;  // Internal order reference
    private Long   customerId;
    private Integer amountInPaise;    // Amount in paise (amount * 100)
    private String currency;
    private String keyId;             // Public key sent to frontend to open checkout
}
