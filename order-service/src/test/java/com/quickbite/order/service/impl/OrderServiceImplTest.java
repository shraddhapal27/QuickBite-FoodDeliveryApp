package com.quickbite.order.service.impl;

import com.quickbite.order.dto.CartDTO;
import com.quickbite.order.dto.CartItemDTO;
import com.quickbite.order.dto.OrderRequestDTO;
import com.quickbite.order.entity.Order;
import com.quickbite.order.entity.OrderStatus;
import com.quickbite.order.entity.PaymentMode;
import com.quickbite.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Unit Tests")
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks private OrderServiceImpl orderService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
                .orderId(100L).customerId(1L).restaurantId(10L)
                .totalAmount(500.0).discount(50.0).finalAmount(515.5)
                .modeOfPayment(PaymentMode.COD).orderStatus(OrderStatus.PLACED)
                .orderDate(LocalDateTime.now()).estimatedDeliveryMin(45)
                .deliveryAddress("123 Main St").items(new ArrayList<>())
                .build();
    }

    // ── Place Order ──

    @Nested
    @DisplayName("Place Order")
    class PlaceOrderTests {

        @Test
        @DisplayName("placeOrder – success with valid cart")
        void placeOrder_success() {
            CartItemDTO item = new CartItemDTO();
            item.setMenuItemId(1L); item.setName("Burger"); item.setPrice(200.0);
            item.setQuantity(2); item.setCustomization("No onions");

            CartDTO cart = new CartDTO();
            cart.setCustomerId(1L); cart.setRestaurantId(10L);
            cart.setTotalPrice(400.0); cart.setItems(List.of(item));

            OrderRequestDTO req = new OrderRequestDTO();
            req.setDeliveryAddress("123 Main St");
            req.setPaymentMode(PaymentMode.COD);

            when(restTemplate.getForObject(contains("/cart/1"), eq(CartDTO.class))).thenReturn(cart);
            when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
                Order o = i.getArgument(0); o.setOrderId(100L); return o;
            });

            Order result = orderService.placeOrder(1L, req);

            assertThat(result.getOrderId()).isEqualTo(100L);
            assertThat(result.getCustomerId()).isEqualTo(1L);
            assertThat(result.getRestaurantId()).isEqualTo(10L);
            assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PLACED);
            assertThat(result.getDeliveryAddress()).isEqualTo("123 Main St");
            assertThat(result.getItems()).hasSize(1);
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("placeOrder – empty cart throws")
        void placeOrder_emptyCart() {
            CartDTO cart = new CartDTO();
            cart.setItems(new ArrayList<>());
            when(restTemplate.getForObject(anyString(), eq(CartDTO.class))).thenReturn(cart);

            OrderRequestDTO req = new OrderRequestDTO();
            req.setDeliveryAddress("addr"); req.setPaymentMode(PaymentMode.COD);

            assertThatThrownBy(() -> orderService.placeOrder(1L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cart is empty");
        }

        @Test
        @DisplayName("placeOrder – null cart throws")
        void placeOrder_nullCart() {
            when(restTemplate.getForObject(anyString(), eq(CartDTO.class))).thenReturn(null);
            OrderRequestDTO req = new OrderRequestDTO();
            req.setDeliveryAddress("addr"); req.setPaymentMode(PaymentMode.COD);
            assertThatThrownBy(() -> orderService.placeOrder(1L, req))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("placeOrder – missing restaurantId throws")
        void placeOrder_noRestaurant() {
            CartItemDTO item = new CartItemDTO();
            item.setMenuItemId(1L); item.setName("X"); item.setPrice(100.0); item.setQuantity(1);
            CartDTO cart = new CartDTO();
            cart.setItems(List.of(item)); cart.setRestaurantId(null); cart.setTotalPrice(100.0);
            when(restTemplate.getForObject(anyString(), eq(CartDTO.class))).thenReturn(cart);
            OrderRequestDTO req = new OrderRequestDTO();
            req.setDeliveryAddress("addr"); req.setPaymentMode(PaymentMode.COD);
            assertThatThrownBy(() -> orderService.placeOrder(1L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Restaurant ID is missing");
        }
    }

    // ── Get Orders ──

    @Nested
    @DisplayName("Get Orders")
    class GetOrderTests {

        @Test
        @DisplayName("getOrderById – success")
        void getOrderById_success() {
            when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
            Order result = orderService.getOrderById(100L);
            assertThat(result.getOrderId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("getOrderById – not found throws")
        void getOrderById_notFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> orderService.getOrderById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Order not found");
        }

        @Test
        @DisplayName("getOrdersByCustomer – returns list")
        void getOrdersByCustomer() {
            when(orderRepository.findByCustomerId(1L)).thenReturn(List.of(sampleOrder));
            List<Order> list = orderService.getOrdersByCustomer(1L);
            assertThat(list).hasSize(1);
        }

        @Test
        @DisplayName("getOrdersByRestaurant – returns list")
        void getOrdersByRestaurant() {
            when(orderRepository.findByRestaurantId(10L)).thenReturn(List.of(sampleOrder));
            List<Order> list = orderService.getOrdersByRestaurant(10L);
            assertThat(list).hasSize(1);
        }

        @Test
        @DisplayName("getAllOrders – delegates to repository")
        void getAllOrders() {
            when(orderRepository.findAll()).thenReturn(List.of(sampleOrder));
            assertThat(orderService.getAllOrders()).hasSize(1);
        }

        @Test
        @DisplayName("getOrderCount – returns count")
        void getOrderCount() {
            when(orderRepository.countByRestaurantId(10L)).thenReturn(5L);
            assertThat(orderService.getOrderCount(10L)).isEqualTo(5L);
        }
    }

    // ── Active Orders ──

    @Test
    @DisplayName("getActiveOrders – filters non-terminal statuses")
    void getActiveOrders_filters() {
        Order delivered = Order.builder().orderId(101L).restaurantId(10L)
                .orderStatus(OrderStatus.DELIVERED).items(new ArrayList<>()).build();
        Order cancelled = Order.builder().orderId(102L).restaurantId(10L)
                .orderStatus(OrderStatus.CANCELLED).items(new ArrayList<>()).build();
        Order preparing = Order.builder().orderId(103L).restaurantId(10L)
                .orderStatus(OrderStatus.PREPARING).items(new ArrayList<>()).build();
        Order confirmed = Order.builder().orderId(104L).restaurantId(10L)
                .orderStatus(OrderStatus.CONFIRMED).items(new ArrayList<>()).build();

        when(orderRepository.findByRestaurantId(10L))
                .thenReturn(List.of(sampleOrder, delivered, cancelled, preparing, confirmed));

        List<Order> active = orderService.getActiveOrders(10L);
        // PLACED + PREPARING + CONFIRMED = 3 active orders (DELIVERED and CANCELLED excluded)
        assertThat(active).hasSize(3);
        assertThat(active).extracting(Order::getOrderStatus)
                .containsExactlyInAnyOrder(OrderStatus.PLACED, OrderStatus.PREPARING, OrderStatus.CONFIRMED);
    }

    // ── Update Status ──

    @Test
    @DisplayName("updateStatus – changes status and saves")
    void updateStatus_success() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.updateStatus(100L, OrderStatus.CONFIRMED);
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(sampleOrder);
    }

    // ── Assign Delivery Agent ──

    @Test
    @DisplayName("assignDeliveryAgent – sets agentId")
    void assignAgent() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Order result = orderService.assignDeliveryAgent(100L, 55L);
        assertThat(result.getDeliveryAgentId()).isEqualTo(55L);
    }

    // ── Cancel Order ──

    @Nested
    @DisplayName("Cancel Order")
    class CancelOrderTests {

        @Test
        @DisplayName("cancelOrder – PLACED order can be cancelled")
        void cancelOrder_placed() {
            sampleOrder.setOrderStatus(OrderStatus.PLACED);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            Order result = orderService.cancelOrder(100L);
            assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("cancelOrder – CONFIRMED order can be cancelled")
        void cancelOrder_confirmed() {
            sampleOrder.setOrderStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
            when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            Order result = orderService.cancelOrder(100L);
            assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("cancelOrder – PREPARING order cannot be cancelled")
        void cancelOrder_preparing_throws() {
            sampleOrder.setOrderStatus(OrderStatus.PREPARING);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
            assertThatThrownBy(() -> orderService.cancelOrder(100L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel");
        }

        @Test
        @DisplayName("cancelOrder – DELIVERED order cannot be cancelled")
        void cancelOrder_delivered_throws() {
            sampleOrder.setOrderStatus(OrderStatus.DELIVERED);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
            assertThatThrownBy(() -> orderService.cancelOrder(100L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ── Reorder ──

    @Test
    @DisplayName("reorderFromHistory – throws UnsupportedOperationException")
    void reorder_notImplemented() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(sampleOrder));
        assertThatThrownBy(() -> orderService.reorderFromHistory(1L, 100L))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
