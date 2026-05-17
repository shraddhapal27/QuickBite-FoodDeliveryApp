package com.quickbite.delivery.resource;

import com.quickbite.delivery.dto.AgentRegistrationDTO;
import com.quickbite.delivery.dto.LocationUpdateDTO;
import com.quickbite.delivery.dto.RatingUpdateDTO;
import com.quickbite.delivery.entity.DeliveryAgent;
import com.quickbite.delivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Resource for the Delivery-Agent-Service.
 * Base path: /agents
 */
@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
@Tag(name = "Delivery Agents", description = "Agent registration, verification, availability, location, and delivery lifecycle")
public class DeliveryResource {

    private final DeliveryService deliveryService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Operation(summary = "Register a new delivery agent")
    @PostMapping("/register")
    public ResponseEntity<?> registerAgent(@RequestBody AgentRegistrationDTO dto) {
        try {
            return new ResponseEntity<>(deliveryService.registerAgent(dto), HttpStatus.CREATED);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get agent by ID")
    @GetMapping("/{agentId}")
    public ResponseEntity<?> getAgentById(@PathVariable Long agentId) {
        try {
            return ResponseEntity.ok(deliveryService.getAgentById(agentId));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Get agent by auth user ID")
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getAgentByUserId(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(deliveryService.getAgentByUserId(userId));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Get all agents (Admin)")
    @GetMapping
    public ResponseEntity<List<DeliveryAgent>> getAllAgents() {
        return ResponseEntity.ok(deliveryService.getAllAgents());
    }

    @Operation(summary = "Verify agent (Admin)")
    @PutMapping("/{agentId}/verify")
    public ResponseEntity<?> verifyAgent(@PathVariable Long agentId) {
        try {
            return ResponseEntity.ok(deliveryService.verifyAgent(agentId));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Delete agent (Admin)")
    @DeleteMapping("/{agentId}")
    public ResponseEntity<String> deleteAgent(@PathVariable Long agentId) {
        try {
            deliveryService.deleteAgent(agentId);
            return ResponseEntity.ok("Agent deleted successfully.");
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Set agent availability")
    @PutMapping("/{agentId}/availability")
    public ResponseEntity<?> setAvailability(@PathVariable Long agentId, @RequestParam Boolean available) {
        try {
            return ResponseEntity.ok(deliveryService.setAvailability(agentId, available));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Update agent location (GPS)")
    @PutMapping("/{agentId}/location")
    public ResponseEntity<?> updateLocation(@PathVariable Long agentId, @RequestBody LocationUpdateDTO location) {
        try {
            DeliveryAgent agent = deliveryService.updateLocation(agentId, location);
            
            // Broadcast live location to any listening customers for this order via STOMP WebSockets
            if (agent.getCurrentOrderId() != null) {
                messagingTemplate.convertAndSend("/topic/delivery/" + agent.getCurrentOrderId(), location);
            }
            
            return ResponseEntity.ok(agent);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Find nearby available agents")
    @GetMapping("/nearby")
    public ResponseEntity<List<DeliveryAgent>> getNearbyAgents(
            @RequestParam double lat, @RequestParam double lng,
            @RequestParam(defaultValue = "5.0") double radius) {
        return ResponseEntity.ok(deliveryService.getNearbyAgents(lat, lng, radius));
    }

    @Operation(summary = "Assign order to agent")
    @PostMapping("/{agentId}/assign")
    public ResponseEntity<?> assignOrder(@PathVariable Long agentId, @RequestParam Long orderId) {
        try {
            return ResponseEntity.ok(deliveryService.assignOrder(agentId, orderId));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Mark delivery as complete")
    @PostMapping("/{agentId}/complete")
    public ResponseEntity<?> completeDelivery(@PathVariable Long agentId) {
        try {
            return ResponseEntity.ok(deliveryService.completeDelivery(agentId));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get active deliveries")
    @GetMapping("/active")
    public ResponseEntity<List<DeliveryAgent>> getActiveDeliveries() {
        return ResponseEntity.ok(deliveryService.getActiveDeliveries());
    }

    @Operation(summary = "Update agent rating")
    @PutMapping("/{agentId}/rating")
    public ResponseEntity<?> updateRating(@PathVariable Long agentId, @RequestBody RatingUpdateDTO rating) {
        try {
            return ResponseEntity.ok(deliveryService.updateRating(agentId, rating));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
