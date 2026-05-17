package com.quickbite.delivery.service;

import com.quickbite.delivery.dto.AgentRegistrationDTO;
import com.quickbite.delivery.dto.LocationUpdateDTO;
import com.quickbite.delivery.dto.RatingUpdateDTO;
import com.quickbite.delivery.entity.DeliveryAgent;

import java.util.List;

/**
 * Service contract for the Delivery-Agent-Service.
 * All business operations for agent lifecycle, geo-proximity, and delivery tracking are declared here.
 */
public interface DeliveryService {

    // ── Registration & Profile ──────────────────────────────────────────────

    /** Register a new delivery agent (initially unverified). */
    DeliveryAgent registerAgent(AgentRegistrationDTO dto);

    /** Fetch a single agent by their internal agentId. */
    DeliveryAgent getAgentById(Long agentId);

    /** Fetch a single agent by the linked auth-service userId. */
    DeliveryAgent getAgentByUserId(Long userId);

    /** Return all registered agents (admin use). */
    List<DeliveryAgent> getAllAgents();

    // ── Admin Operations ────────────────────────────────────────────────────

    /** Admin: verify an agent after document review. */
    DeliveryAgent verifyAgent(Long agentId);

    /** Delete an agent record. */
    void deleteAgent(Long agentId);

    // ── Availability & Location ─────────────────────────────────────────────

    /** Toggle an agent's online/offline availability state. */
    DeliveryAgent setAvailability(Long agentId, Boolean available);

    /** Push an updated GPS position for real-time customer tracking. */
    DeliveryAgent updateLocation(Long agentId, LocationUpdateDTO location);

    // ── Geo-Proximity Discovery ─────────────────────────────────────────────

    /** Find available verified agents within {@code radiusKm} of the given coordinates. */
    List<DeliveryAgent> getNearbyAgents(double lat, double lng, double radiusKm);

    // ── Order Assignment ────────────────────────────────────────────────────

    /** Assign an order to an agent (called by order-service on placement). */
    DeliveryAgent assignOrder(Long agentId, Long orderId);

    /** Mark the current delivery complete and free the agent slot. */
    DeliveryAgent completeDelivery(Long agentId);

    /** Return all agents who are currently assigned to an active order. */
    List<DeliveryAgent> getActiveDeliveries();

    // ── Ratings ─────────────────────────────────────────────────────────────

    /** Update the agent's running average rating with a new delivery score. */
    DeliveryAgent updateRating(Long agentId, RatingUpdateDTO rating);
}
