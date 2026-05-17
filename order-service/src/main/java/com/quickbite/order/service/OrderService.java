package com.quickbite.order.service;

import com.quickbite.order.dto.OrderRequestDTO;
import com.quickbite.order.entity.Order;
import com.quickbite.order.entity.OrderStatus;

import java.util.List;

public interface OrderService {
    Order placeOrder(Long customerId, OrderRequestDTO orderRequestDTO);
    Order getOrderById(Long orderId);
    List<Order> getOrdersByCustomer(Long customerId);
    List<Order> getOrdersByRestaurant(Long restaurantId);
    List<Order> getActiveOrders(Long restaurantId);
    Order updateStatus(Long orderId, OrderStatus status);
    Order assignDeliveryAgent(Long orderId, Long agentId);
    Order cancelOrder(Long orderId);
    Order reorderFromHistory(Long customerId, Long orderId);
    long getOrderCount(Long restaurantId);
    List<Order> getAllOrders();
    List<Order> getOrdersByAgent(Long agentId);
}
