package com.quickbite.review.repository;

import com.quickbite.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByRestaurantId(Long restaurantId);

    List<Review> findByCustomerId(Long customerId);

    Optional<Review> findByOrderId(Long orderId);

    List<Review> findByAgentId(Long agentId);

    boolean existsByOrderId(Long orderId);

    long countByRestaurantId(Long restaurantId);

    /** Average food rating for a restaurant (used to update restaurant's avgRating). */
    @Query("SELECT AVG(r.foodRating) FROM Review r WHERE r.restaurantId = :restaurantId")
    Double avgFoodRatingByRestaurantId(@Param("restaurantId") Long restaurantId);

    /** Average delivery rating for an agent (used to update agent's avgRating). */
    @Query("SELECT AVG(r.deliveryRating) FROM Review r WHERE r.agentId = :agentId AND r.deliveryRating IS NOT NULL")
    Double avgDeliveryRatingByAgentId(@Param("agentId") Long agentId);

    /** All unverified reviews — for admin moderation queue. */
    List<Review> findByIsVerified(Boolean isVerified);
}
