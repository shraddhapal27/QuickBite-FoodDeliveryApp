package com.quickbite.payment.service.impl;

import com.quickbite.payment.dto.*;
import com.quickbite.payment.entity.*;
import com.quickbite.payment.repository.PaymentRepository;
import com.quickbite.payment.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl Unit Tests")
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private WalletRepository walletRepository;

    @InjectMocks private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "mock_key");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "mock_secret");
    }

    // ── Razorpay Order Creation ──

    @Nested
    @DisplayName("Create Razorpay Order")
    class RazorpayOrderTests {

        @Test
        @DisplayName("createRazorpayOrder – returns mock order with mock_key")
        void createRazorpayOrder_mockKey() {
            RazorpayOrderRequestDTO req = new RazorpayOrderRequestDTO();
            req.setOrderId(1L); req.setCustomerId(10L);
            req.setAmount(500.0); req.setCurrency("INR");

            RazorpayOrderResponseDTO res = paymentService.createRazorpayOrder(req);

            assertThat(res.getRazorpayOrderId()).startsWith("order_mock_");
            assertThat(res.getQuickbiteOrderId()).isEqualTo(1L);
            assertThat(res.getCustomerId()).isEqualTo(10L);
            assertThat(res.getAmountInPaise()).isEqualTo(50000);
            assertThat(res.getCurrency()).isEqualTo("INR");
        }

        @Test
        @DisplayName("createRazorpayOrder – defaults to INR currency")
        void createRazorpayOrder_defaultCurrency() {
            RazorpayOrderRequestDTO req = new RazorpayOrderRequestDTO();
            req.setOrderId(1L); req.setCustomerId(10L);
            req.setAmount(100.0); req.setCurrency(null);

            RazorpayOrderResponseDTO res = paymentService.createRazorpayOrder(req);
            assertThat(res.getCurrency()).isEqualTo("INR");
        }
    }

    // ── Verify & Capture Payment ──

    @Nested
    @DisplayName("Verify & Capture Payment")
    class VerifyCaptureTests {

        @Test
        @DisplayName("verifyAndCapturePayment – mock payment bypasses signature")
        void verify_mockPayment() {
            RazorpayVerifyDTO dto = new RazorpayVerifyDTO();
            dto.setRazorpayPaymentId("pay_mock_123");
            dto.setRazorpayOrderId("order_mock_456");
            dto.setRazorpaySignature("any");
            dto.setQuickbiteOrderId(1L); dto.setCustomerId(10L); dto.setAmount(500.0);

            when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
                Payment p = i.getArgument(0); p.setPaymentId(1L); return p;
            });

            Payment result = paymentService.verifyAndCapturePayment(dto);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(result.getMode()).isEqualTo(PaymentMode.RAZORPAY);
            assertThat(result.getTransactionId()).isEqualTo("pay_mock_123");
        }

        @Test
        @DisplayName("verifyAndCapturePayment – invalid signature throws")
        void verify_invalidSignature() {
            RazorpayVerifyDTO dto = new RazorpayVerifyDTO();
            dto.setRazorpayPaymentId("pay_real_123"); // not mock
            dto.setRazorpayOrderId("order_123");
            dto.setRazorpaySignature("wrong_signature");
            dto.setQuickbiteOrderId(1L); dto.setCustomerId(10L); dto.setAmount(500.0);

            assertThatThrownBy(() -> paymentService.verifyAndCapturePayment(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("invalid signature");
        }
    }

    // ── Process Payment ──

    @Nested
    @DisplayName("Process Payment")
    class ProcessPaymentTests {

        @Test
        @DisplayName("processPayment – COD sets PENDING status")
        void processPayment_cod() {
            PaymentRequestDTO req = new PaymentRequestDTO();
            req.setOrderId(1L); req.setCustomerId(10L);
            req.setAmount(300.0); req.setMode(PaymentMode.COD);

            when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
                Payment p = i.getArgument(0); p.setPaymentId(1L); return p;
            });

            Payment result = paymentService.processPayment(req);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(result.getMode()).isEqualTo(PaymentMode.COD);
            assertThat(result.getPaidAt()).isNull();
        }

        @Test
        @DisplayName("processPayment – CARD sets PAID status")
        void processPayment_card() {
            PaymentRequestDTO req = new PaymentRequestDTO();
            req.setOrderId(2L); req.setCustomerId(10L);
            req.setAmount(500.0); req.setMode(PaymentMode.CARD);

            when(paymentRepository.save(any())).thenAnswer(i -> {
                Payment p = i.getArgument(0); p.setPaymentId(2L); return p;
            });

            Payment result = paymentService.processPayment(req);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(result.getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("processPayment – WALLET delegates to payFromWallet")
        void processPayment_wallet() {
            PaymentRequestDTO req = new PaymentRequestDTO();
            req.setOrderId(3L); req.setCustomerId(10L);
            req.setAmount(200.0); req.setMode(PaymentMode.WALLET);

            Wallet wallet = Wallet.builder().walletId(1L).customerId(10L)
                    .balance(500.0).statements(new ArrayList<>()).build();
            when(walletRepository.findByCustomerId(10L)).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(paymentRepository.save(any())).thenAnswer(i -> {
                Payment p = i.getArgument(0); p.setPaymentId(3L); return p;
            });

            Payment result = paymentService.processPayment(req);
            assertThat(result.getMode()).isEqualTo(PaymentMode.WALLET);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(wallet.getBalance()).isEqualTo(300.0);
        }
    }

    // ── Get Payment ──

    @Nested
    @DisplayName("Get Payment Queries")
    class GetPaymentTests {

        @Test
        @DisplayName("getByOrderId – success")
        void getByOrderId() {
            Payment p = Payment.builder().paymentId(1L).orderId(10L).build();
            when(paymentRepository.findByOrderId(10L)).thenReturn(Optional.of(p));
            assertThat(paymentService.getByOrderId(10L).getPaymentId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("getByOrderId – not found throws")
        void getByOrderId_notFound() {
            when(paymentRepository.findByOrderId(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> paymentService.getByOrderId(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("getByCustomerId – returns list")
        void getByCustomerId() {
            when(paymentRepository.findByCustomerId(10L)).thenReturn(List.of(
                    Payment.builder().paymentId(1L).build()));
            assertThat(paymentService.getByCustomerId(10L)).hasSize(1);
        }

        @Test
        @DisplayName("getTotalSpend – returns sum")
        void getTotalSpend() {
            when(paymentRepository.sumAmountByCustomerId(10L)).thenReturn(1500.0);
            assertThat(paymentService.getTotalSpend(10L)).isEqualTo(1500.0);
        }

        @Test
        @DisplayName("getTotalSpend – null defaults to 0")
        void getTotalSpend_null() {
            when(paymentRepository.sumAmountByCustomerId(10L)).thenReturn(null);
            assertThat(paymentService.getTotalSpend(10L)).isEqualTo(0.0);
        }
    }

    // ── Refund ──

    @Nested
    @DisplayName("Refund Payment")
    class RefundTests {

        @Test
        @DisplayName("refundPayment – success for PAID payment")
        void refund_success() {
            Payment p = Payment.builder().paymentId(1L).orderId(10L).customerId(10L)
                    .amount(300.0).status(PaymentStatus.PAID).mode(PaymentMode.CARD).build();
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Payment result = paymentService.refundPayment(1L);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(result.getRefundedAt()).isNotNull();
        }

        @Test
        @DisplayName("refundPayment – WALLET refund credits back wallet")
        void refund_wallet() {
            Payment p = Payment.builder().paymentId(2L).orderId(10L).customerId(10L)
                    .amount(200.0).status(PaymentStatus.PAID).mode(PaymentMode.WALLET).build();
            Wallet w = Wallet.builder().walletId(1L).customerId(10L)
                    .balance(100.0).statements(new ArrayList<>()).build();

            when(paymentRepository.findById(2L)).thenReturn(Optional.of(p));
            when(walletRepository.findByCustomerId(10L)).thenReturn(Optional.of(w));
            when(walletRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            paymentService.refundPayment(2L);
            assertThat(w.getBalance()).isEqualTo(300.0); // 100 + 200 refund
        }

        @Test
        @DisplayName("refundPayment – non-PAID throws")
        void refund_nonPaid() {
            Payment p = Payment.builder().paymentId(3L).status(PaymentStatus.PENDING).build();
            when(paymentRepository.findById(3L)).thenReturn(Optional.of(p));
            assertThatThrownBy(() -> paymentService.refundPayment(3L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only PAID");
        }

        @Test
        @DisplayName("refundPayment – not found throws")
        void refund_notFound() {
            when(paymentRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> paymentService.refundPayment(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── Update Payment Status ──

    @Test
    @DisplayName("updatePaymentStatus – sets paidAt when transitioning to PAID")
    void updateStatus_toPaid() {
        Payment p = Payment.builder().paymentId(1L).status(PaymentStatus.PENDING).build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Payment result = paymentService.updatePaymentStatus(1L, PaymentStatus.PAID);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(result.getPaidAt()).isNotNull();
    }

    // ── Wallet Operations ──

    @Nested
    @DisplayName("Wallet Operations")
    class WalletTests {

        @Test
        @DisplayName("getOrCreateWallet – returns existing wallet")
        void getOrCreate_existing() {
            Wallet w = Wallet.builder().walletId(1L).customerId(10L).balance(500.0).build();
            when(walletRepository.findByCustomerId(10L)).thenReturn(Optional.of(w));
            assertThat(paymentService.getOrCreateWallet(10L).getBalance()).isEqualTo(500.0);
        }

        @Test
        @DisplayName("getOrCreateWallet – creates new wallet if not found")
        void getOrCreate_new() {
            when(walletRepository.findByCustomerId(99L)).thenReturn(Optional.empty());
            when(walletRepository.save(any())).thenAnswer(i -> {
                Wallet w = i.getArgument(0); w.setWalletId(2L); return w;
            });
            Wallet w = paymentService.getOrCreateWallet(99L);
            assertThat(w.getCustomerId()).isEqualTo(99L);
            assertThat(w.getBalance()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("getWalletBalance – returns balance")
        void getWalletBalance() {
            Wallet w = Wallet.builder().walletId(1L).customerId(10L).balance(750.0)
                    .statements(new ArrayList<>()).build();
            when(walletRepository.findByCustomerId(10L)).thenReturn(Optional.of(w));
            assertThat(paymentService.getWalletBalance(10L)).isEqualTo(750.0);
        }

        @Test
        @DisplayName("addToWallet – credits balance and adds statement")
        void addToWallet_success() {
            Wallet w = Wallet.builder().walletId(1L).customerId(10L).balance(100.0)
                    .statements(new ArrayList<>()).build();
            WalletTopUpDTO req = new WalletTopUpDTO();
            req.setCustomerId(10L); req.setAmount(500.0); req.setDescription("Top-up");

            when(walletRepository.findByCustomerId(10L)).thenReturn(Optional.of(w));
            when(walletRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Wallet result = paymentService.addToWallet(req);
            assertThat(result.getBalance()).isEqualTo(600.0);
            assertThat(result.getStatements()).hasSize(1);
            assertThat(result.getStatements().get(0).getType()).isEqualTo(TransactionType.CREDIT);
        }

        @Test
        @DisplayName("addToWallet – zero or negative amount throws")
        void addToWallet_negativeAmount() {
            WalletTopUpDTO req = new WalletTopUpDTO();
            req.setCustomerId(10L); req.setAmount(-100.0);
            assertThatThrownBy(() -> paymentService.addToWallet(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("payFromWallet – success debits wallet")
        void payFromWallet_success() {
            Wallet w = Wallet.builder().walletId(1L).customerId(10L).balance(500.0)
                    .statements(new ArrayList<>()).build();
            when(walletRepository.findByCustomerId(10L)).thenReturn(Optional.of(w));
            when(walletRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(paymentRepository.save(any())).thenAnswer(i -> {
                Payment p = i.getArgument(0); p.setPaymentId(1L); return p;
            });

            Payment result = paymentService.payFromWallet(10L, 1L, 200.0);
            assertThat(result.getMode()).isEqualTo(PaymentMode.WALLET);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(w.getBalance()).isEqualTo(300.0);
            assertThat(w.getStatements()).hasSize(1);
            assertThat(w.getStatements().get(0).getType()).isEqualTo(TransactionType.DEBIT);
        }

        @Test
        @DisplayName("payFromWallet – insufficient balance throws")
        void payFromWallet_insufficientBalance() {
            Wallet w = Wallet.builder().walletId(1L).customerId(10L).balance(50.0)
                    .statements(new ArrayList<>()).build();
            when(walletRepository.findByCustomerId(10L)).thenReturn(Optional.of(w));
            assertThatThrownBy(() -> paymentService.payFromWallet(10L, 1L, 200.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient");
        }

        @Test
        @DisplayName("getWalletStatements – returns statements list")
        void getStatements() {
            WalletStatement stmt = WalletStatement.builder()
                    .statementId(1L).amount(100.0).type(TransactionType.CREDIT)
                    .description("Test").createdAt(LocalDateTime.now()).build();
            Wallet w = Wallet.builder().walletId(1L).customerId(10L).balance(100.0)
                    .statements(new ArrayList<>(List.of(stmt))).build();
            when(walletRepository.findByCustomerId(10L)).thenReturn(Optional.of(w));

            List<WalletStatement> stmts = paymentService.getWalletStatements(10L);
            assertThat(stmts).hasSize(1);
        }
    }
}
