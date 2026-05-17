package com.quickbite.review.service;

import com.quickbite.review.dto.ReviewRequestDTO;
import com.quickbite.review.dto.ReviewUpdateDTO;
import com.quickbite.review.entity.Review;

import java.util.List;

/**
 * Service contract for the Review/Rating-Service.
 * Handles dual-rating submission, retrieval by entity, average computation, and moderation.
 */
public interface ReviewService {

    // ── Submission ──────────────────────────────────────────────────────────

    /**
     * Submit a new review. Enforces one-review-per-order constraint.
     * After saving, triggers average re-computation for the restaurant and agent.
     */
    Review addReview(ReviewRequestDTO dto);

    // ── Retrieval ───────────────────────────────────────────────────────────

    Review getByReviewId(Long reviewId);

    Review getByOrderId(Long orderId);

    List<Review> getByRestaurantId(Long restaurantId);

    List<Review> getByCustomerId(Long customerId);

    List<Review> getByAgentId(Long agentId);

    /** Admin: get all reviews across the platform. */
    List<Review> getAllReviews();

    // ── Update & Delete ─────────────────────────────────────────────────────

    Review updateReview(Long reviewId, ReviewUpdateDTO dto);

    /** Admin: remove a fraudulent or inappropriate review. */
    void deleteReview(Long reviewId);

    // ── Average Computation ─────────────────────────────────────────────────

    /** Average food rating for a restaurant (1–5 scale). */
    Double getAvgFoodRating(Long restaurantId);

    /** Average delivery rating for an agent (1–5 scale). */
    Double getAvgDeliveryRating(Long agentId);

    // ── Admin Moderation ────────────────────────────────────────────────────

    /** Mark a review as verified (genuine) by admin. */
    Review verifyReview(Long reviewId);

    /** Return all reviews pending verification. */
    List<Review> getUnverifiedReviews();
}
