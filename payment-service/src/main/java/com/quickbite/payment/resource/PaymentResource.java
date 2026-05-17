package com.quickbite.payment.resource;

import com.quickbite.payment.dto.*;
import com.quickbite.payment.entity.Payment;
import com.quickbite.payment.entity.PaymentStatus;
import com.quickbite.payment.entity.Wallet;
import com.quickbite.payment.entity.WalletStatement;
import com.quickbite.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Payments & Wallet", description = "Payment processing, Razorpay integration, and wallet management")
public class PaymentResource {

    private final PaymentService paymentService;

    // ── Razorpay Endpoints ──

    @Operation(summary = "Create Razorpay order", description = "Step 1: Get a Razorpay Order ID before opening checkout")
    @PostMapping("/payments/razorpay/create-order")
    public ResponseEntity<?> createRazorpayOrder(@RequestBody RazorpayOrderRequestDTO request) {
        try {
            return ResponseEntity.ok(paymentService.createRazorpayOrder(request));
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Verify Razorpay payment", description = "Step 2: Verify payment after Razorpay checkout succeeds")
    @PostMapping("/payments/razorpay/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody RazorpayVerifyDTO verifyDTO) {
        try {
            return ResponseEntity.ok(paymentService.verifyAndCapturePayment(verifyDTO));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Verification error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Process payment")
    @PostMapping("/payments/process")
    public ResponseEntity<?> processPayment(@RequestBody PaymentRequestDTO request) {
        try {
            return new ResponseEntity<>(paymentService.processPayment(request), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Error processing payment: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Get payment by order ID")
    @GetMapping("/payments/order/{orderId}")
    public ResponseEntity<Payment> getByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getByOrderId(orderId));
    }

    @Operation(summary = "Get payments by customer")
    @GetMapping("/payments/customer/{customerId}")
    public ResponseEntity<List<Payment>> getByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(paymentService.getByCustomerId(customerId));
    }

    @Operation(summary = "Refund payment")
    @PostMapping("/payments/{paymentId}/refund")
    public ResponseEntity<?> refundPayment(@PathVariable Long paymentId) {
        try {
            return ResponseEntity.ok(paymentService.refundPayment(paymentId));
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Update payment status")
    @PutMapping("/payments/{paymentId}/status")
    public ResponseEntity<Payment> updateStatus(@PathVariable Long paymentId, @RequestParam PaymentStatus status) {
        return ResponseEntity.ok(paymentService.updatePaymentStatus(paymentId, status));
    }

    @Operation(summary = "Get payments by date range")
    @GetMapping("/payments/range")
    public ResponseEntity<List<Payment>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(paymentService.getByDateRange(start, end));
    }

    @Operation(summary = "Get total spend by customer")
    @GetMapping("/payments/total/{customerId}")
    public ResponseEntity<Double> getTotalSpend(@PathVariable Long customerId) {
        return ResponseEntity.ok(paymentService.getTotalSpend(customerId));
    }

    // ── Wallet Endpoints ──

    @Operation(summary = "Get or create wallet")
    @GetMapping("/wallet/{customerId}")
    public ResponseEntity<Wallet> getWallet(@PathVariable Long customerId) {
        return ResponseEntity.ok(paymentService.getOrCreateWallet(customerId));
    }

    @Operation(summary = "Get wallet balance")
    @GetMapping("/wallet/{customerId}/balance")
    public ResponseEntity<Double> getBalance(@PathVariable Long customerId) {
        return ResponseEntity.ok(paymentService.getWalletBalance(customerId));
    }

    @Operation(summary = "Add money to wallet")
    @PostMapping("/wallet/add")
    public ResponseEntity<?> addToWallet(@RequestBody WalletTopUpDTO request) {
        try {
            return ResponseEntity.ok(paymentService.addToWallet(request));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Pay from wallet")
    @PostMapping("/wallet/pay")
    public ResponseEntity<?> payFromWallet(@RequestBody WalletPayRequestDTO request) {
        try {
            return ResponseEntity.ok(paymentService.payFromWallet(request.getCustomerId(), request.getOrderId(), request.getAmount()));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get wallet statements")
    @GetMapping("/wallet/{customerId}/statements")
    public ResponseEntity<List<WalletStatement>> getStatements(@PathVariable Long customerId) {
        return ResponseEntity.ok(paymentService.getWalletStatements(customerId));
    }
}
