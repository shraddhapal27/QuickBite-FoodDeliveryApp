package com.quickbite.payment.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.payment.dto.*;
import com.quickbite.payment.entity.*;
import com.quickbite.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentResource Controller Tests")
class PaymentResourceTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentResource paymentResource;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentResource).build();
        objectMapper.findAndRegisterModules(); // for LocalDateTime serialization
    }

    private Payment samplePayment() {
        return Payment.builder().paymentId(1L).orderId(100L).customerId(10L)
                .amount(500.0).status(PaymentStatus.PAID).mode(PaymentMode.CARD)
                .transactionId("TXN-ABC123").currency("INR")
                .paidAt(LocalDateTime.now()).build();
    }

    // ── Razorpay Endpoints ──

    @Test
    @DisplayName("POST /payments/razorpay/create-order – 200 OK")
    void createRazorpayOrder() throws Exception {
        RazorpayOrderRequestDTO req = new RazorpayOrderRequestDTO();
        req.setOrderId(1L); req.setCustomerId(10L); req.setAmount(500.0);

        RazorpayOrderResponseDTO res = new RazorpayOrderResponseDTO(
                "order_mock_1", 1L, 10L, 50000, "INR", "key_test");
        when(paymentService.createRazorpayOrder(any())).thenReturn(res);

        mockMvc.perform(post("/payments/razorpay/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razorpayOrderId").value("order_mock_1"));
    }

    @Test
    @DisplayName("POST /payments/razorpay/verify – 200 OK")
    void verifyPayment() throws Exception {
        RazorpayVerifyDTO dto = new RazorpayVerifyDTO();
        dto.setRazorpayPaymentId("pay_mock_1"); dto.setRazorpayOrderId("order_mock_1");
        dto.setRazorpaySignature("sig"); dto.setQuickbiteOrderId(1L);
        dto.setCustomerId(10L); dto.setAmount(500.0);

        when(paymentService.verifyAndCapturePayment(any())).thenReturn(samplePayment());

        mockMvc.perform(post("/payments/razorpay/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    // ── Process Payment ──

    @Test
    @DisplayName("POST /payments/process – 201 CREATED")
    void processPayment() throws Exception {
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setOrderId(1L); req.setCustomerId(10L); req.setAmount(300.0);
        req.setMode(PaymentMode.COD);

        Payment p = samplePayment(); p.setStatus(PaymentStatus.PENDING);
        when(paymentService.processPayment(any())).thenReturn(p);

        mockMvc.perform(post("/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /payments/process – 400 BAD REQUEST")
    void processPayment_error() throws Exception {
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setOrderId(1L); req.setCustomerId(10L);
        req.setAmount(1000.0); req.setMode(PaymentMode.WALLET);

        when(paymentService.processPayment(any()))
                .thenThrow(new IllegalArgumentException("Insufficient balance"));

        mockMvc.perform(post("/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── Query Endpoints ──

    @Test
    @DisplayName("GET /payments/order/{orderId} – 200 OK")
    void getByOrder() throws Exception {
        when(paymentService.getByOrderId(100L)).thenReturn(samplePayment());
        mockMvc.perform(get("/payments/order/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(100));
    }

    @Test
    @DisplayName("GET /payments/customer/{customerId} – 200 OK")
    void getByCustomer() throws Exception {
        when(paymentService.getByCustomerId(10L)).thenReturn(List.of(samplePayment()));
        mockMvc.perform(get("/payments/customer/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /payments/total/{customerId} – 200 OK")
    void getTotalSpend() throws Exception {
        when(paymentService.getTotalSpend(10L)).thenReturn(1500.0);
        mockMvc.perform(get("/payments/total/10"))
                .andExpect(status().isOk())
                .andExpect(content().string("1500.0"));
    }

    // ── Refund ──

    @Test
    @DisplayName("POST /payments/{paymentId}/refund – 200 OK")
    void refund_success() throws Exception {
        Payment refunded = samplePayment(); refunded.setStatus(PaymentStatus.REFUNDED);
        when(paymentService.refundPayment(1L)).thenReturn(refunded);

        mockMvc.perform(post("/payments/1/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    @DisplayName("POST /payments/{paymentId}/refund – 400 BAD REQUEST")
    void refund_error() throws Exception {
        when(paymentService.refundPayment(1L))
                .thenThrow(new IllegalStateException("Only PAID"));
        mockMvc.perform(post("/payments/1/refund"))
                .andExpect(status().isBadRequest());
    }

    // ── Update Status ──

    @Test
    @DisplayName("PUT /payments/{paymentId}/status – 200 OK")
    void updateStatus() throws Exception {
        Payment p = samplePayment();
        when(paymentService.updatePaymentStatus(1L, PaymentStatus.REFUNDED)).thenReturn(p);
        mockMvc.perform(put("/payments/1/status").param("status", "REFUNDED"))
                .andExpect(status().isOk());
    }

    // ── Wallet Endpoints ──

    @Test
    @DisplayName("GET /wallet/{customerId} – 200 OK")
    void getWallet() throws Exception {
        Wallet w = Wallet.builder().walletId(1L).customerId(10L).balance(500.0)
                .statements(new ArrayList<>()).build();
        when(paymentService.getOrCreateWallet(10L)).thenReturn(w);

        mockMvc.perform(get("/wallet/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.0));
    }

    @Test
    @DisplayName("GET /wallet/{customerId}/balance – 200 OK")
    void getBalance() throws Exception {
        when(paymentService.getWalletBalance(10L)).thenReturn(750.0);
        mockMvc.perform(get("/wallet/10/balance"))
                .andExpect(status().isOk())
                .andExpect(content().string("750.0"));
    }

    @Test
    @DisplayName("POST /wallet/add – 200 OK")
    void addToWallet() throws Exception {
        WalletTopUpDTO req = new WalletTopUpDTO();
        req.setCustomerId(10L); req.setAmount(500.0);

        Wallet w = Wallet.builder().walletId(1L).customerId(10L).balance(500.0)
                .statements(new ArrayList<>()).build();
        when(paymentService.addToWallet(any())).thenReturn(w);

        mockMvc.perform(post("/wallet/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.0));
    }

    @Test
    @DisplayName("POST /wallet/pay – 200 OK")
    void payFromWallet() throws Exception {
        WalletPayRequestDTO req = new WalletPayRequestDTO();
        req.setCustomerId(10L); req.setOrderId(1L); req.setAmount(200.0);

        when(paymentService.payFromWallet(10L, 1L, 200.0)).thenReturn(samplePayment());

        mockMvc.perform(post("/wallet/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /wallet/pay – 400 insufficient balance")
    void payFromWallet_error() throws Exception {
        WalletPayRequestDTO req = new WalletPayRequestDTO();
        req.setCustomerId(10L); req.setOrderId(1L); req.setAmount(9999.0);

        when(paymentService.payFromWallet(10L, 1L, 9999.0))
                .thenThrow(new IllegalArgumentException("Insufficient"));

        mockMvc.perform(post("/wallet/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /wallet/{customerId}/statements – 200 OK")
    void getStatements() throws Exception {
        WalletStatement stmt = WalletStatement.builder()
                .statementId(1L).amount(100.0).type(TransactionType.CREDIT)
                .description("Top-up").createdAt(LocalDateTime.now()).build();
        when(paymentService.getWalletStatements(10L)).thenReturn(List.of(stmt));

        mockMvc.perform(get("/wallet/10/statements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
