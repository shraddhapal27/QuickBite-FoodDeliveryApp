package com.quickbite.order.resource;

import com.quickbite.order.dto.OrderRequestDTO;
import com.quickbite.order.entity.Order;
import com.quickbite.order.entity.OrderStatus;
import com.quickbite.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order lifecycle management")
public class OrderResource {

    private final OrderService orderService;

    @Operation(summary = "Place an order")
    @PostMapping("/place/{customerId}")
    public ResponseEntity<?> placeOrder(@PathVariable Long customerId, @RequestBody OrderRequestDTO requestDTO) {
        try {
            return new ResponseEntity<>(orderService.placeOrder(customerId, requestDTO), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Internal error while placing order: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Get order by ID")
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @Operation(summary = "Get orders by customer")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Order>> getOrdersByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
    }

    @Operation(summary = "Get orders by restaurant")
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<Order>> getOrdersByRestaurant(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderService.getOrdersByRestaurant(restaurantId));
    }

    @Operation(summary = "Get active orders for a restaurant")
    @GetMapping("/active/{restaurantId}")
    public ResponseEntity<List<Order>> getActiveOrders(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderService.getActiveOrders(restaurantId));
    }

    @Operation(summary = "Update order status")
    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long orderId, @RequestParam OrderStatus status) {
        try {
            return ResponseEntity.ok(orderService.updateStatus(orderId, status));
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Failed to update status: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Assign delivery agent to order")
    @PutMapping("/{orderId}/assign")
    public ResponseEntity<Order> assignDeliveryAgent(@PathVariable Long orderId, @RequestParam Long agentId) {
        return ResponseEntity.ok(orderService.assignDeliveryAgent(orderId, agentId));
    }

    @Operation(summary = "Cancel an order")
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(orderService.cancelOrder(orderId));
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Reorder from history")
    @PostMapping("/{customerId}/reorder/{orderId}")
    public ResponseEntity<?> reorder(@PathVariable Long customerId, @PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(orderService.reorderFromHistory(customerId, orderId));
        } catch (UnsupportedOperationException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_IMPLEMENTED);
        }
    }

    @Operation(summary = "Get order count for a restaurant")
    @GetMapping("/count/{restaurantId}")
    public ResponseEntity<Long> getOrderCount(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderService.getOrderCount(restaurantId));
    }

    @Operation(summary = "Get orders by delivery agent")
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<List<Order>> getOrdersByAgent(@PathVariable Long agentId) {
        return ResponseEntity.ok(orderService.getOrdersByAgent(agentId));
    }

    @Operation(summary = "Get all orders (Admin)")
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }
}
