package com.quickbite.payment.service.impl;

import com.quickbite.payment.dto.*;
import com.quickbite.payment.entity.*;
import com.quickbite.payment.repository.PaymentRepository;
import com.quickbite.payment.repository.WalletRepository;
import com.quickbite.payment.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RazorpayClient razorpayClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    // ─────────────────────────────────────────────
    // Razorpay Online Payment
    // ─────────────────────────────────────────────

    @Override
    public RazorpayOrderResponseDTO createRazorpayOrder(RazorpayOrderRequestDTO request) {
        try {
            // Razorpay requires amount in paise (1 INR = 100 paise)
            int amountInPaise = (int) (request.getAmount() * 100);
            String currency   = request.getCurrency() != null ? request.getCurrency() : "INR";
            String receipt    = request.getReceipt()  != null ? request.getReceipt()
                                        : "QB-" + request.getOrderId();

            JSONObject orderParams = new JSONObject();
            orderParams.put("amount",   amountInPaise);
            orderParams.put("currency", currency);
            orderParams.put("receipt",  receipt);

            if (razorpayKeyId.startsWith("${") || razorpayKeyId.equals("mock_key")) {
                log.info("Using mock Razorpay order for testing without real keys.");
                return new RazorpayOrderResponseDTO(
                        "order_mock_" + System.currentTimeMillis(),
                        request.getOrderId(),
                        request.getCustomerId(),
                        amountInPaise,
                        currency,
                        "rzp_test_mock123"
                );
            }

            if (razorpayClient == null) {
                throw new RuntimeException("Razorpay client is not initialized. Please check your credentials.");
            }
            Order razorpayOrder = razorpayClient.orders.create(orderParams);
            log.info("Razorpay Order created: {} for QuickBite order {}",
                    razorpayOrder.get("id"), request.getOrderId());

            return new RazorpayOrderResponseDTO(
                    razorpayOrder.get("id"),
                    request.getOrderId(),
                    request.getCustomerId(),
                    amountInPaise,
                    currency,
                    razorpayKeyId
            );
        } catch (Exception e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage());
            throw new RuntimeException("Razorpay order creation failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Payment verifyAndCapturePayment(RazorpayVerifyDTO verifyDTO) {
        if (verifyDTO.getRazorpayPaymentId().startsWith("pay_mock_")) {
            log.info("Bypassing signature verification for mock payment.");
        } else {
            // Verify HMAC-SHA256 signature
            String expectedSignature = generateHmacSha256(
                    verifyDTO.getRazorpayOrderId() + "|" + verifyDTO.getRazorpayPaymentId(),
                    razorpayKeySecret
            );

            if (!expectedSignature.equals(verifyDTO.getRazorpaySignature())) {
                log.error("Razorpay signature verification FAILED for order {}", verifyDTO.getRazorpayOrderId());
                throw new IllegalArgumentException("Payment verification failed: invalid signature.");
            }
        }

        log.info("Razorpay payment {} verified successfully for QuickBite order {}",
                verifyDTO.getRazorpayPaymentId(), verifyDTO.getQuickbiteOrderId());

        Payment payment = Payment.builder()
                .orderId(verifyDTO.getQuickbiteOrderId())
                .customerId(verifyDTO.getCustomerId())
                .amount(verifyDTO.getAmount())
                .mode(PaymentMode.RAZORPAY)     // Razorpay stored as RAZORPAY
                .currency("INR")
                .transactionId(verifyDTO.getRazorpayPaymentId())   // Razorpay payment ID as txn ref
                .status(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build();

        return paymentRepository.save(payment);
    }

    // ─────────────────────────────────────────────
    // Generic Payment Operations
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public Payment processPayment(PaymentRequestDTO request) {
        log.info("Processing {} payment of ₹{} for order {}", request.getMode(), request.getAmount(), request.getOrderId());

        if (request.getMode() == PaymentMode.WALLET) {
            return payFromWallet(request.getCustomerId(), request.getOrderId(), request.getAmount());
        }

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .mode(request.getMode())
                .currency("INR")
                .transactionId(generateTransactionId())
                .status(request.getMode() == PaymentMode.COD ? PaymentStatus.PENDING : PaymentStatus.PAID)
                .paidAt(request.getMode() != PaymentMode.COD ? LocalDateTime.now() : null)
                .build();

        return paymentRepository.save(payment);
    }

    @Override
    public Payment getByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("No payment found for order ID: " + orderId));
    }

    @Override
    public List<Payment> getByCustomerId(Long customerId) {
        return paymentRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional
    public Payment refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Only PAID payments can be refunded. Current status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());

        // Refund back to wallet if original mode was WALLET
        if (payment.getMode() == PaymentMode.WALLET) {
            Wallet wallet = getOrCreateWallet(payment.getCustomerId());
            wallet.setBalance(wallet.getBalance() + payment.getAmount());
            WalletStatement refundStatement = WalletStatement.builder()
                    .wallet(wallet)
                    .amount(payment.getAmount())
                    .type(TransactionType.CREDIT)
    .description("Refund for your order")
                    .createdAt(LocalDateTime.now())
                    .build();
            wallet.getStatements().add(refundStatement);
            walletRepository.save(wallet);
        }

        log.info("Refunded payment {} for order {}", paymentId, payment.getOrderId());
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public Payment updatePaymentStatus(Long paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        payment.setStatus(status);
        if (status == PaymentStatus.PAID && payment.getPaidAt() == null) {
            payment.setPaidAt(LocalDateTime.now());
        }
        return paymentRepository.save(payment);
    }

    @Override
    public List<Payment> getByDateRange(LocalDateTime start, LocalDateTime end) {
        return paymentRepository.findByPaidAtBetween(start, end);
    }

    @Override
    public Double getTotalSpend(Long customerId) {
        Double total = paymentRepository.sumAmountByCustomerId(customerId);
        return total != null ? total : 0.0;
    }



    // ─────────────────────────────────────────────
    // Wallet Operations
    // ─────────────────────────────────────────────

    @Override
    public Wallet getOrCreateWallet(Long customerId) {
        return walletRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    log.info("Creating new wallet for customer {}", customerId);
                    return walletRepository.save(Wallet.builder()
                            .customerId(customerId)
                            .balance(0.0)
                            .build());
                });
    }

    @Override
    public Double getWalletBalance(Long customerId) {
        return getOrCreateWallet(customerId).getBalance();
    }

    @Override
    @Transactional
    public Wallet addToWallet(WalletTopUpDTO request) {
        if (request.getAmount() <= 0) throw new IllegalArgumentException("Top-up amount must be positive.");

        Wallet wallet = getOrCreateWallet(request.getCustomerId());
        wallet.setBalance(wallet.getBalance() + request.getAmount());

        WalletStatement statement = WalletStatement.builder()
                .wallet(wallet)
                .amount(request.getAmount())
                .type(TransactionType.CREDIT)
                .description(request.getDescription() != null ? request.getDescription() : "Wallet Top-Up")
                .createdAt(LocalDateTime.now())
                .build();
        wallet.getStatements().add(statement);

        log.info("Credited ₹{} to wallet of customer {}", request.getAmount(), request.getCustomerId());
        return walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public Payment payFromWallet(Long customerId, Long orderId, Double amount) {
        Wallet wallet = getOrCreateWallet(customerId);

        if (wallet.getBalance() < amount) {
            throw new IllegalArgumentException(
                    String.format("Insufficient wallet balance. Available: ₹%.2f, Required: ₹%.2f",
                            wallet.getBalance(), amount));
        }

        wallet.setBalance(wallet.getBalance() - amount);
        WalletStatement debit = WalletStatement.builder()
                .wallet(wallet)
                .amount(amount)
                .type(TransactionType.DEBIT)
                .description("Payment for your order")
                .createdAt(LocalDateTime.now())
                .build();
        wallet.getStatements().add(debit);
        walletRepository.save(wallet);

        Payment payment = Payment.builder()
                .orderId(orderId)
                .customerId(customerId)
                .amount(amount)
                .mode(PaymentMode.WALLET)
                .currency("INR")
                .transactionId(generateTransactionId())
                .status(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build();

        log.info("Wallet payment of ₹{} for order {} by customer {}", amount, orderId, customerId);
        return paymentRepository.save(payment);
    }

    @Override
    public List<WalletStatement> getWalletStatements(Long customerId) {
        return getOrCreateWallet(customerId).getStatements();
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 16);
    }

    private String generateHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }
}
