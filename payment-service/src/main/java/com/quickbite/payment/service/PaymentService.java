package com.quickbite.payment.service;

import com.quickbite.payment.dto.*;
import com.quickbite.payment.entity.Payment;
import com.quickbite.payment.entity.PaymentStatus;
import com.quickbite.payment.entity.Wallet;
import com.quickbite.payment.entity.WalletStatement;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {

    // ── Razorpay Online Payment ──
    RazorpayOrderResponseDTO createRazorpayOrder(RazorpayOrderRequestDTO request);

    Payment verifyAndCapturePayment(RazorpayVerifyDTO verifyDTO);

    // ── Generic Payment Operations ──
    Payment processPayment(PaymentRequestDTO request);

    Payment getByOrderId(Long orderId);

    List<Payment> getByCustomerId(Long customerId);

    Payment refundPayment(Long paymentId);

    Payment updatePaymentStatus(Long paymentId, PaymentStatus status);

    List<Payment> getByDateRange(LocalDateTime start, LocalDateTime end);

    Double getTotalSpend(Long customerId);

    // ── Wallet Operations ──
    Wallet getOrCreateWallet(Long customerId);

    Double getWalletBalance(Long customerId);

    Wallet addToWallet(WalletTopUpDTO request);

    Payment payFromWallet(Long customerId, Long orderId, Double amount);

    List<WalletStatement> getWalletStatements(Long customerId);
}
