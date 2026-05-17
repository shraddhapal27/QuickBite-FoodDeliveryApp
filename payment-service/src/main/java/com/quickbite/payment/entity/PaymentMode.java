package com.quickbite.payment.entity;

public enum PaymentMode {
    COD,          // Cash on Delivery
    CARD,         // Debit / Credit Card (via Razorpay gateway)
    UPI,          // UPI (via Razorpay gateway)
    RAZORPAY,     // Generic Razorpay (legacy)
    WALLET        // QuickBite internal wallet
}
