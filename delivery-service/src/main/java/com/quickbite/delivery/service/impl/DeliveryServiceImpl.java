package com.quickbite.delivery.service.impl;

import com.quickbite.delivery.dto.AgentRegistrationDTO;
import com.quickbite.delivery.dto.LocationUpdateDTO;
import com.quickbite.delivery.dto.RatingUpdateDTO;
import com.quickbite.delivery.entity.DeliveryAgent;
import com.quickbite.delivery.repository.DeliveryRepository;
import com.quickbite.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;

    // ── Registration & Profile ──────────────────────────────────────────────

    @Override
    public DeliveryAgent registerAgent(AgentRegistrationDTO dto) {
        if (deliveryRepository.findByUserId(dto.getUserId()).isPresent()) {
            throw new IllegalStateException("A delivery agent is already registered for userId: " + dto.getUserId());
        }
        if (deliveryRepository.findByPhone(dto.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Phone number already registered: " + dto.getPhone());
        }
        DeliveryAgent agent = DeliveryAgent.builder()
                .userId(dto.getUserId())
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .vehicleType(dto.getVehicleType())
                .vehicleNumber(dto.getVehicleNumber())
                .isAvailable(false)
                .isVerified(false)
                .avgRating(0.0)
                .totalDeliveries(0)
                .totalRatings(0)
                .build();
        DeliveryAgent saved = deliveryRepository.save(agent);
        log.info("Delivery agent registered: {} (agentId={}, userId={})", saved.getFullName(), saved.getAgentId(), saved.getUserId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAgent getAgentById(Long agentId) {
        return deliveryRepository.findByAgentId(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found with id: " + agentId));
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAgent getAgentByUserId(Long userId) {
        return deliveryRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found for userId: " + userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAgent> getAllAgents() {
        return deliveryRepository.findAll();
    }

    // ── Admin Operations ────────────────────────────────────────────────────

    @Override
    public DeliveryAgent verifyAgent(Long agentId) {
        DeliveryAgent agent = getAgentById(agentId);
        agent.setIsVerified(true);
        return deliveryRepository.save(agent);
    }

    @Override
    public void deleteAgent(Long agentId) {
        if (!deliveryRepository.existsById(agentId)) {
            throw new IllegalArgumentException("Agent not found with id: " + agentId);
        }
        deliveryRepository.deleteByAgentId(agentId);
    }

    // ── Availability & Location ─────────────────────────────────────────────

    @Override
    public DeliveryAgent setAvailability(Long agentId, Boolean available) {
        DeliveryAgent agent = getAgentById(agentId);
        if (!agent.getIsVerified()) {
            throw new IllegalStateException("Agent must be verified by admin before toggling availability.");
        }
        agent.setIsAvailable(available);
        return deliveryRepository.save(agent);
    }

    @Override
    public DeliveryAgent updateLocation(Long agentId, LocationUpdateDTO location) {
        if (location.getLatitude() == null || location.getLongitude() == null) {
            throw new IllegalArgumentException("Latitude and longitude must not be null.");
        }
        DeliveryAgent agent = getAgentById(agentId);
        agent.setCurrentLatitude(location.getLatitude());
        agent.setCurrentLongitude(location.getLongitude());
        return deliveryRepository.save(agent);
    }

    // ── Geo-Proximity Discovery ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAgent> getNearbyAgents(double lat, double lng, double radiusKm) {
        return deliveryRepository.findNearbyAgents(lat, lng, radiusKm);
    }

    // ── Order Assignment ────────────────────────────────────────────────────

    @Override
    public DeliveryAgent assignOrder(Long agentId, Long orderId) {
        DeliveryAgent agent = getAgentById(agentId);
        if (!agent.getIsVerified()) {
            throw new IllegalStateException("Cannot assign order: agent is not verified.");
        }
        if (!agent.getIsAvailable()) {
            throw new IllegalStateException("Cannot assign order: agent is not available.");
        }
        if (agent.getCurrentOrderId() != null) {
            throw new IllegalStateException("Agent is already handling order: " + agent.getCurrentOrderId());
        }
        agent.setCurrentOrderId(orderId);
        agent.setIsAvailable(false);
        log.info("Order {} assigned to agent {} (agentId={})", orderId, agent.getFullName(), agentId);
        return deliveryRepository.save(agent);
    }

    @Override
    public DeliveryAgent completeDelivery(Long agentId) {
        DeliveryAgent agent = getAgentById(agentId);
        if (agent.getCurrentOrderId() == null) {
            throw new IllegalStateException("Agent has no active delivery to complete.");
        }
        agent.setCurrentOrderId(null);
        agent.setTotalDeliveries(agent.getTotalDeliveries() + 1);
        agent.setIsAvailable(true);
        log.info("Delivery completed by agent {} (agentId={}, totalDeliveries={})", agent.getFullName(), agentId, agent.getTotalDeliveries());
        return deliveryRepository.save(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAgent> getActiveDeliveries() {
        return deliveryRepository.findByCurrentOrderIdIsNotNull();
    }

    // ── Ratings ─────────────────────────────────────────────────────────────

    @Override
    public DeliveryAgent updateRating(Long agentId, RatingUpdateDTO rating) {
        if (rating.getNewRating() == null || rating.getNewRating() < 1 || rating.getNewRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        DeliveryAgent agent = getAgentById(agentId);
        int totalRatings = agent.getTotalRatings();
        double currentAvg = agent.getAvgRating();

        // Compute incremental running average
        double newAvg = ((currentAvg * totalRatings) + rating.getNewRating()) / (totalRatings + 1);
        agent.setAvgRating(Math.round(newAvg * 100.0) / 100.0); // round to 2 decimal places
        agent.setTotalRatings(totalRatings + 1);
        return deliveryRepository.save(agent);
    }
}
