package com.quickbite.order.dto;

import com.quickbite.order.entity.PaymentMode;
import lombok.Data;

@Data
public class OrderRequestDTO {
    private String deliveryAddress;
    private String specialInstructions;
    private PaymentMode paymentMode;
    private String promoCode; // Optional promo code to be validated or just taken as is for now
}
