package com.quickbite.order.repository;

import com.quickbite.order.entity.Order;
import com.quickbite.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);
    List<Order> findByRestaurantId(Long restaurantId);
    List<Order> findByOrderStatus(OrderStatus orderStatus);
    List<Order> findByDeliveryAgentId(Long deliveryAgentId);
    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);
    long countByRestaurantId(Long restaurantId);
}
