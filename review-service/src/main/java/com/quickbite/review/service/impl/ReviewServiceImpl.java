package com.quickbite.review.service.impl;

import com.quickbite.review.dto.ReviewRequestDTO;
import com.quickbite.review.dto.ReviewUpdateDTO;
import com.quickbite.review.entity.Review;
import com.quickbite.review.repository.ReviewRepository;
import com.quickbite.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    // ── Submission ──────────────────────────────────────────────────────────

    @Override
    public Review addReview(ReviewRequestDTO dto) {
        // Enforce one-review-per-order constraint
        if (reviewRepository.existsByOrderId(dto.getOrderId())) {
            throw new IllegalStateException("A review already exists for orderId: " + dto.getOrderId());
        }

        validateFoodRating(dto.getFoodRating());
        if (dto.getDeliveryRating() != null) {
            validateDeliveryRating(dto.getDeliveryRating());
        }

        Review review = Review.builder()
                .orderId(dto.getOrderId())
                .customerId(dto.getCustomerId())
                .restaurantId(dto.getRestaurantId())
                .agentId(dto.getAgentId())
                .foodRating(dto.getFoodRating())
                .deliveryRating(dto.getDeliveryRating())
                .comment(dto.getComment())
                .isVerified(false)
                .build();

        return reviewRepository.save(review);
    }

    // ── Retrieval ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Review getByReviewId(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with id: " + reviewId));
    }

    @Override
    @Transactional(readOnly = true)
    public Review getByOrderId(Long orderId) {
        return reviewRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("No review found for orderId: " + orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getByRestaurantId(Long restaurantId) {
        return reviewRepository.findByRestaurantId(restaurantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getByCustomerId(Long customerId) {
        return reviewRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getByAgentId(Long agentId) {
        return reviewRepository.findByAgentId(agentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    // ── Update & Delete ─────────────────────────────────────────────────────

    @Override
    public Review updateReview(Long reviewId, ReviewUpdateDTO dto) {
        Review review = getByReviewId(reviewId);

        if (dto.getFoodRating() != null) {
            validateFoodRating(dto.getFoodRating());
            review.setFoodRating(dto.getFoodRating());
        }
        if (dto.getDeliveryRating() != null) {
            validateDeliveryRating(dto.getDeliveryRating());
            review.setDeliveryRating(dto.getDeliveryRating());
        }
        if (dto.getComment() != null) {
            review.setComment(dto.getComment());
        }

        return reviewRepository.save(review);
    }

    @Override
    public void deleteReview(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new IllegalArgumentException("Review not found with id: " + reviewId);
        }
        reviewRepository.deleteById(reviewId);
    }

    // ── Average Computation ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Double getAvgFoodRating(Long restaurantId) {
        Double avg = reviewRepository.avgFoodRatingByRestaurantId(restaurantId);
        return avg != null ? Math.round(avg * 100.0) / 100.0 : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAvgDeliveryRating(Long agentId) {
        Double avg = reviewRepository.avgDeliveryRatingByAgentId(agentId);
        return avg != null ? Math.round(avg * 100.0) / 100.0 : 0.0;
    }

    // ── Admin Moderation ────────────────────────────────────────────────────

    @Override
    public Review verifyReview(Long reviewId) {
        Review review = getByReviewId(reviewId);
        review.setIsVerified(true);
        return reviewRepository.save(review);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getUnverifiedReviews() {
        return reviewRepository.findByIsVerified(false);
    }

    // ── Private Validators ──────────────────────────────────────────────────

    private void validateFoodRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Food rating must be between 1 and 5.");
        }
    }

    private void validateDeliveryRating(Integer rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Delivery rating must be between 1 and 5.");
        }
    }
}
