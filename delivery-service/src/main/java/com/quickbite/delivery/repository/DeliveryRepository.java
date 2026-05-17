package com.quickbite.delivery.repository;

import com.quickbite.delivery.entity.DeliveryAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<DeliveryAgent, Long> {

    Optional<DeliveryAgent> findByUserId(Long userId);

    Optional<DeliveryAgent> findByAgentId(Long agentId);

    Optional<DeliveryAgent> findByPhone(String phone);

    List<DeliveryAgent> findByIsAvailable(Boolean isAvailable);

    List<DeliveryAgent> findByIsVerified(Boolean isVerified);

    List<DeliveryAgent> findByIsAvailableAndIsVerified(Boolean isAvailable, Boolean isVerified);

    long countByIsAvailable(Boolean isAvailable);

    void deleteByAgentId(Long agentId);

    /**
     * Haversine-based geo-proximity query to find available verified agents
     * within {@code radiusKm} kilometres of a given coordinate.
     */
    @Query(value = """
            SELECT * FROM delivery_agents a
            WHERE a.is_available = true
              AND a.is_verified  = true
              AND a.current_order_id IS NULL
              AND (
                6371 * ACOS(
                    COS(RADIANS(:lat)) * COS(RADIANS(a.current_latitude))
                    * COS(RADIANS(a.current_longitude) - RADIANS(:lng))
                    + SIN(RADIANS(:lat)) * SIN(RADIANS(a.current_latitude))
                )
              ) <= :radiusKm
            ORDER BY (
                6371 * ACOS(
                    COS(RADIANS(:lat)) * COS(RADIANS(a.current_latitude))
                    * COS(RADIANS(a.current_longitude) - RADIANS(:lng))
                    + SIN(RADIANS(:lat)) * SIN(RADIANS(a.current_latitude))
                )
            ) ASC
            """, nativeQuery = true)
    List<DeliveryAgent> findNearbyAgents(@Param("lat") double lat,
                                         @Param("lng") double lng,
                                         @Param("radiusKm") double radiusKm);

    /** Agents currently assigned to an order */
    List<DeliveryAgent> findByCurrentOrderIdIsNotNull();
}
