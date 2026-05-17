package com.quickbite.order.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.order.dto.OrderRequestDTO;
import com.quickbite.order.entity.Order;
import com.quickbite.order.entity.OrderStatus;
import com.quickbite.order.entity.PaymentMode;
import com.quickbite.order.service.OrderService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderResource Controller Tests")
class OrderResourceTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderResource orderResource;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderResource).build();
        objectMapper.findAndRegisterModules(); // for LocalDateTime serialization
        sampleOrder = Order.builder()
                .orderId(100L).customerId(1L).restaurantId(10L)
                .totalAmount(500.0).discount(0.0).finalAmount(565.5)
                .modeOfPayment(PaymentMode.COD).orderStatus(OrderStatus.PLACED)
                .orderDate(LocalDateTime.now()).estimatedDeliveryMin(45)
                .deliveryAddress("123 Main St").items(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("POST /orders/place/{customerId} – 201 CREATED")
    void placeOrder_success() throws Exception {
        OrderRequestDTO req = new OrderRequestDTO();
        req.setDeliveryAddress("123 Main St"); req.setPaymentMode(PaymentMode.COD);
        when(orderService.placeOrder(eq(1L), any(OrderRequestDTO.class))).thenReturn(sampleOrder);

        mockMvc.perform(post("/orders/place/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(100));
    }

    @Test
    @DisplayName("POST /orders/place/{customerId} – 400 when cart empty")
    void placeOrder_badRequest() throws Exception {
        OrderRequestDTO req = new OrderRequestDTO();
        req.setDeliveryAddress("addr"); req.setPaymentMode(PaymentMode.COD);
        when(orderService.placeOrder(eq(1L), any())).thenThrow(new IllegalArgumentException("Cart is empty"));

        mockMvc.perform(post("/orders/place/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /orders/{orderId} – 200 OK")
    void getOrderById() throws Exception {
        when(orderService.getOrderById(100L)).thenReturn(sampleOrder);
        mockMvc.perform(get("/orders/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(100));
    }

    @Test
    @DisplayName("GET /orders/customer/{customerId} – 200 OK")
    void getOrdersByCustomer() throws Exception {
        when(orderService.getOrdersByCustomer(1L)).thenReturn(List.of(sampleOrder));
        mockMvc.perform(get("/orders/customer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /orders/restaurant/{restaurantId} – 200 OK")
    void getOrdersByRestaurant() throws Exception {
        when(orderService.getOrdersByRestaurant(10L)).thenReturn(List.of(sampleOrder));
        mockMvc.perform(get("/orders/restaurant/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restaurantId").value(10));
    }

    @Test
    @DisplayName("GET /orders/active/{restaurantId} – 200 OK")
    void getActiveOrders() throws Exception {
        when(orderService.getActiveOrders(10L)).thenReturn(List.of(sampleOrder));
        mockMvc.perform(get("/orders/active/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("PUT /orders/{orderId}/status – 200 OK")
    void updateStatus() throws Exception {
        sampleOrder.setOrderStatus(OrderStatus.CONFIRMED);
        when(orderService.updateStatus(100L, OrderStatus.CONFIRMED)).thenReturn(sampleOrder);
        mockMvc.perform(put("/orders/100/status").param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("CONFIRMED"));
    }

    @Test
    @DisplayName("PUT /orders/{orderId}/assign – 200 OK")
    void assignAgent() throws Exception {
        sampleOrder.setDeliveryAgentId(55L);
        when(orderService.assignDeliveryAgent(100L, 55L)).thenReturn(sampleOrder);
        mockMvc.perform(put("/orders/100/assign").param("agentId", "55"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryAgentId").value(55));
    }

    @Test
    @DisplayName("PUT /orders/{orderId}/cancel – 200 OK")
    void cancelOrder_success() throws Exception {
        sampleOrder.setOrderStatus(OrderStatus.CANCELLED);
        when(orderService.cancelOrder(100L)).thenReturn(sampleOrder);
        mockMvc.perform(put("/orders/100/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("CANCELLED"));
    }

    @Test
    @DisplayName("PUT /orders/{orderId}/cancel – 400 BAD REQUEST")
    void cancelOrder_badRequest() throws Exception {
        when(orderService.cancelOrder(100L)).thenThrow(new IllegalStateException("Cannot cancel"));
        mockMvc.perform(put("/orders/100/cancel"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /orders/count/{restaurantId} – 200 OK")
    void getOrderCount() throws Exception {
        when(orderService.getOrderCount(10L)).thenReturn(42L);
        mockMvc.perform(get("/orders/count/10"))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    @Test
    @DisplayName("GET /orders – 200 OK all orders")
    void getAllOrders() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(sampleOrder));
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
