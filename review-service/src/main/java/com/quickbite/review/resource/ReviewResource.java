package com.quickbite.review.resource;

import com.quickbite.review.dto.ReviewRequestDTO;
import com.quickbite.review.dto.ReviewUpdateDTO;
import com.quickbite.review.entity.Review;
import com.quickbite.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Resource for the Review/Rating-Service.
 * Base path: /reviews
 */
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Food and delivery review management, ratings, and moderation")
public class ReviewResource {

    private final ReviewService reviewService;

    @Operation(summary = "Submit a review", description = "Submit a new dual-rating review after order delivery")
    @PostMapping
    public ResponseEntity<?> addReview(@RequestBody ReviewRequestDTO dto) {
        try {
            return new ResponseEntity<>(reviewService.addReview(dto), HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Failed to save review: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Get review by ID")
    @GetMapping("/{reviewId}")
    public ResponseEntity<?> getByReviewId(@PathVariable Long reviewId) {
        try {
            return ResponseEntity.ok(reviewService.getByReviewId(reviewId));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Get review by order ID")
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getByOrderId(@PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(reviewService.getByOrderId(orderId));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Get reviews by restaurant")
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<Review>> getByRestaurantId(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(reviewService.getByRestaurantId(restaurantId));
    }

    @Operation(summary = "Get reviews by customer")
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Review>> getByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(reviewService.getByCustomerId(customerId));
    }

    @Operation(summary = "Get reviews by delivery agent")
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<List<Review>> getByAgentId(@PathVariable Long agentId) {
        return ResponseEntity.ok(reviewService.getByAgentId(agentId));
    }

    @Operation(summary = "Get all reviews (Admin)")
    @GetMapping
    public ResponseEntity<List<Review>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @Operation(summary = "Update a review")
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(@PathVariable Long reviewId, @RequestBody ReviewUpdateDTO dto) {
        try {
            return ResponseEntity.ok(reviewService.updateReview(reviewId, dto));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Delete a review (Admin)")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<String> deleteReview(@PathVariable Long reviewId) {
        try {
            reviewService.deleteReview(reviewId);
            return ResponseEntity.ok("Review deleted successfully.");
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Get avg food rating for restaurant")
    @GetMapping("/avg/food/{restaurantId}")
    public ResponseEntity<Double> getAvgFoodRating(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(reviewService.getAvgFoodRating(restaurantId));
    }

    @Operation(summary = "Get avg delivery rating for agent")
    @GetMapping("/avg/delivery/{agentId}")
    public ResponseEntity<Double> getAvgDeliveryRating(@PathVariable Long agentId) {
        return ResponseEntity.ok(reviewService.getAvgDeliveryRating(agentId));
    }

    @Operation(summary = "Verify a review (Admin)")
    @PutMapping("/{reviewId}/verify")
    public ResponseEntity<?> verifyReview(@PathVariable Long reviewId) {
        try {
            return ResponseEntity.ok(reviewService.verifyReview(reviewId));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Get unverified reviews (Admin)")
    @GetMapping("/unverified")
    public ResponseEntity<List<Review>> getUnverifiedReviews() {
        return ResponseEntity.ok(reviewService.getUnverifiedReviews());
    }
}
