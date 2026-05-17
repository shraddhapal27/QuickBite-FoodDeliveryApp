package com.quickbite.order.service.impl;

import com.quickbite.order.dto.CartDTO;
import com.quickbite.order.dto.OrderRequestDTO;
import com.quickbite.order.entity.Order;
import com.quickbite.order.entity.OrderItem;
import com.quickbite.order.entity.OrderStatus;
import com.quickbite.order.repository.OrderRepository;
import com.quickbite.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.quickbite.order.config.RabbitMQConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;

    private static final String CART_SERVICE_URL = "http://QUICKBITE-CART-SERVICE/cart";
    private static final String NOTIFICATION_SERVICE_URL = "http://QUICKBITE-NOTIFICATION-SERVICE/notifications/send";

    @Override
    @Transactional
    public Order placeOrder(Long customerId, OrderRequestDTO request) {
        log.info("Placing order for customer: {}", customerId);
        
        // 1. Fetch Cart from Cart Service
        CartDTO cart = restTemplate.getForObject(CART_SERVICE_URL + "/" + customerId, CartDTO.class);
        
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty. Cannot place order.");
        }
        
        if (cart.getRestaurantId() == null) {
            throw new IllegalArgumentException("Restaurant ID is missing in the cart.");
        }

        // 2. Map Cart Items to Order Items
        List<OrderItem> orderItems = cart.getItems().stream().map(cartItem -> 
            OrderItem.builder()
                .menuItemId(cartItem.getMenuItemId())
                .name(cartItem.getName())
                .price(cartItem.getPrice())
                .quantity(cartItem.getQuantity())
                .customization(cartItem.getCustomization())
                .build()
        ).collect(Collectors.toList());

        // 3. Build Order
        double totalItemAmount = orderItems.stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();
                
        // Cart total already includes promo code deductions if applied
        double discount = totalItemAmount - cart.getTotalPrice();
        if (discount < 0) discount = 0; // fallback just in case
        
        // Add fixed delivery fee and taxes for demo
        double deliveryFee = 40.00;
        double taxes = 25.50;
        double finalAmount = cart.getTotalPrice() + deliveryFee + taxes;

        Order order = Order.builder()
                .customerId(customerId)
                .restaurantId(cart.getRestaurantId())
                .items(orderItems)
                .totalAmount(totalItemAmount)
                .discount(discount)
                .finalAmount(finalAmount)
                .modeOfPayment(request.getPaymentMode())
                .orderStatus(OrderStatus.PLACED)
                .orderDate(LocalDateTime.now())
                .estimatedDeliveryMin(45) // Default 45 mins
                .deliveryAddress(request.getDeliveryAddress())
                .specialInstructions(request.getSpecialInstructions())
                .build();

        // Save order
        Order savedOrder = orderRepository.save(order);
        
        // 4. Clear Cart via API call
        try {
            restTemplate.delete(CART_SERVICE_URL + "/" + customerId + "/clear");
        } catch (Exception e) {
            log.error("Failed to clear cart after placing order. Cart Service may be down or rejecting.", e);
            // We can decide to rollback or proceed. Usually we proceed and have a retry mechanism.
        }
        
        // Payment orchestration is handled by the frontend (or API Gateway).
        // The frontend will call Payment Service after the order is placed.

        // 6. Auto-Assign Delivery Agent
        try {
            // First, fetch the restaurant to get its coordinates
            com.quickbite.order.dto.RestaurantDTO restaurant = restTemplate.getForObject(
                "http://QUICKBITE-RESTAURANT-SERVICE/restaurants/" + cart.getRestaurantId(), 
                com.quickbite.order.dto.RestaurantDTO.class
            );
            
            if (restaurant != null && restaurant.getLatitude() != null && restaurant.getLongitude() != null) {
                // Find nearby agents within 10km
                String nearbyUrl = String.format("http://QUICKBITE-DELIVERY-SERVICE/agents/nearby?lat=%s&lng=%s&radius=10.0", 
                    restaurant.getLatitude(), restaurant.getLongitude());
                
                com.quickbite.order.dto.DeliveryAgentDTO[] agents = restTemplate.getForObject(nearbyUrl, com.quickbite.order.dto.DeliveryAgentDTO[].class);
                
                if (agents != null && agents.length > 0) {
                    // Find first available agent (who doesn't have an active order)
                    for (com.quickbite.order.dto.DeliveryAgentDTO agent : agents) {
                        if (agent.getIsAvailable() && agent.getCurrentOrderId() == null) {
                            restTemplate.postForObject(
                                "http://QUICKBITE-DELIVERY-SERVICE/agents/" + agent.getAgentId() + "/assign?orderId=" + savedOrder.getOrderId(), 
                                null, Object.class
                            );
                            
                            savedOrder.setDeliveryAgentId(agent.getAgentId());
                            orderRepository.save(savedOrder);
                            log.info("Auto-assigned agent {} to order {}", agent.getAgentId(), savedOrder.getOrderId());
                            break;
                        }
                    }
                } else {
                    log.warn("No nearby agents found for order {}", savedOrder.getOrderId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to auto-assign delivery agent for order {}", savedOrder.getOrderId(), e);
        }

        // 7. Send Notification
        try {
            java.util.Map<String, Object> notificationRequest = new java.util.HashMap<>();
            notificationRequest.put("recipientId", customerId);
            notificationRequest.put("type", "ORDER");
            notificationRequest.put("title", "Order Placed!");
            notificationRequest.put("message", "Your order has been placed successfully.");
            notificationRequest.put("channel", "APP");
            notificationRequest.put("relatedId", savedOrder.getOrderId());
            notificationRequest.put("relatedType", "Order");
            notificationRequest.put("deepLinkUrl", "/orders");
            rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_EXCHANGE, RabbitMQConfig.NOTIFICATION_ROUTING_KEY, notificationRequest);
        } catch (Exception e) {
            log.error("Failed to send order placement notification", e);
        }
        
        return savedOrder;
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
    }

    @Override
    public List<Order> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Override
    public List<Order> getOrdersByRestaurant(Long restaurantId) {
        return orderRepository.findByRestaurantId(restaurantId);
    }

    @Override
    public List<Order> getActiveOrders(Long restaurantId) {
        return orderRepository.findByRestaurantId(restaurantId).stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.PLACED || 
                             o.getOrderStatus() == OrderStatus.CONFIRMED ||
                             o.getOrderStatus() == OrderStatus.PREPARING ||
                             o.getOrderStatus() == OrderStatus.READY_FOR_PICKUP ||
                             o.getOrderStatus() == OrderStatus.PICKED_UP ||
                             o.getOrderStatus() == OrderStatus.OUT_FOR_DELIVERY)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Order updateStatus(Long orderId, OrderStatus status) {
        Order order = getOrderById(orderId);
        
        // Prevent duplicate status updates and notifications
        if (order.getOrderStatus() == status) {
            log.info("Order {} is already in status {}. Skipping update.", orderId, status);
            return order;
        }

        order.setOrderStatus(status);
        Order savedOrder = orderRepository.save(order);
        log.info("Order {} status updated to {}", orderId, status);

        // Fire-and-forget side effects AFTER the DB save succeeds
        performPostStatusUpdateSideEffects(savedOrder, status);

        return savedOrder;
    }

    /**
     * Non-transactional helper: sends notifications and completes delivery agent.
     * Failures here are logged but never propagate to the caller.
     */
    private void performPostStatusUpdateSideEffects(Order order, OrderStatus status) {
        // When the order is delivered, try to complete the delivery agent assignment
        if (status == OrderStatus.DELIVERED && order.getDeliveryAgentId() != null) {
            try {
                restTemplate.postForObject(
                    "http://QUICKBITE-DELIVERY-SERVICE/agents/" + order.getDeliveryAgentId() + "/complete",
                    null, Object.class);
                log.info("Auto-completed delivery agent {} for order {}", order.getDeliveryAgentId(), order.getOrderId());
            } catch (Exception e) {
                log.warn("Failed to auto-complete delivery agent for order {}: {}", order.getOrderId(), e.getMessage());
            }
        }
        
        // Send notification to customer
        try {
            String friendlyStatus = status.name().replace("_", " ");
            String message;
            switch (status) {
                case CONFIRMED:        message = "Your order has been accepted by the restaurant!"; break;
                case PREPARING:        message = "The restaurant is preparing your order. Hang tight!"; break;
                case READY_FOR_PICKUP: message = "Your order is ready! A delivery agent will pick it up soon."; break;
                case PICKED_UP:        message = "A delivery agent has picked up your order!"; break;
                case OUT_FOR_DELIVERY: message = "Your order is on the way to you!"; break;
                case DELIVERED:        message = "Your order has been delivered. Enjoy your meal!"; break;
                case CANCELLED:        message = "Your order has been cancelled."; break;
                default:               message = "Your order is now " + friendlyStatus + "."; break;
            }

            java.util.Map<String, Object> notificationRequest = new java.util.HashMap<>();
            notificationRequest.put("recipientId", order.getCustomerId());
            notificationRequest.put("type", "ORDER");
            notificationRequest.put("title", "Order Update: " + friendlyStatus);
            notificationRequest.put("message", message);
            notificationRequest.put("channel", "APP");
            notificationRequest.put("relatedId", order.getOrderId());
            notificationRequest.put("relatedType", "Order");
            notificationRequest.put("deepLinkUrl", "/orders");
            rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_EXCHANGE, RabbitMQConfig.NOTIFICATION_ROUTING_KEY, notificationRequest);
        } catch (Exception e) {
            log.error("Failed to send order status update notification for order {}: {}", order.getOrderId(), e.getMessage());
        }
    }

    @Override
    @Transactional
    public Order assignDeliveryAgent(Long orderId, Long agentId) {
        Order order = getOrderById(orderId);
        order.setDeliveryAgentId(agentId);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = getOrderById(orderId);
        
        if (order.getOrderStatus() == OrderStatus.PLACED || order.getOrderStatus() == OrderStatus.CONFIRMED) {
            order.setOrderStatus(OrderStatus.CANCELLED);
            // FUTURE: Trigger refund if payment was made online
            return orderRepository.save(order);
        }
        
        throw new IllegalStateException("Cannot cancel order once preparation has started.");
    }

    @Override
    @Transactional
    public Order reorderFromHistory(Long customerId, Long orderId) {
        // Find past order
        Order pastOrder = getOrderById(orderId);
        
        // Add items to cart (we simulate calling Cart Service to add items one by one)
        // A better approach for full reorder is to just use a custom endpoint in Cart Service,
        // but for now we'll throw an UnsupportedOperationException until that endpoint exists.
        throw new UnsupportedOperationException("Reorder functionality coming soon in Cart Service integration.");
    }

    @Override
    public long getOrderCount(Long restaurantId) {
        return orderRepository.countByRestaurantId(restaurantId);
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public List<Order> getOrdersByAgent(Long agentId) {
        return orderRepository.findByDeliveryAgentId(agentId);
    }
}
