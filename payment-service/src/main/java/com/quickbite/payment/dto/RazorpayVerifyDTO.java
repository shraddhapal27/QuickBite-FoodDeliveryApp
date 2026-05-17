package com.quickbite.payment.dto;

import lombok.Data;

@Data
public class RazorpayVerifyDTO {
    private String razorpayPaymentId;   // razorpay_payment_id from frontend callback
    private String razorpayOrderId;     // razorpay_order_id from frontend callback
    private String razorpaySignature;   // razorpay_signature from frontend callback
    private Long   quickbiteOrderId;    // Your internal order ID
    private Long   customerId;
    private Double amount;
}
