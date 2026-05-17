package com.quickbite.review.service.impl;

import com.quickbite.review.dto.ReviewRequestDTO;
import com.quickbite.review.dto.ReviewUpdateDTO;
import com.quickbite.review.entity.Review;
import com.quickbite.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewServiceImpl Unit Tests")
class ReviewServiceImplTest {

    @Mock private ReviewRepository reviewRepository;
    @InjectMocks private ReviewServiceImpl reviewService;

    private Review sampleReview;

    @BeforeEach
    void setUp() {
        sampleReview = Review.builder()
                .reviewId(1L).orderId(100L).customerId(10L)
                .restaurantId(5L).agentId(20L)
                .foodRating(4).deliveryRating(5)
                .comment("Great food and fast delivery!")
                .isVerified(false).reviewDate(LocalDateTime.now())
                .build();
    }

    // ── Submission ──

    @Nested
    @DisplayName("Add Review")
    class AddReviewTests {

        @Test
        @DisplayName("addReview – success")
        void addReview_success() {
            ReviewRequestDTO dto = new ReviewRequestDTO();
            dto.setOrderId(200L); dto.setCustomerId(10L);
            dto.setRestaurantId(5L); dto.setAgentId(20L);
            dto.setFoodRating(5); dto.setDeliveryRating(4);
            dto.setComment("Excellent!");

            when(reviewRepository.existsByOrderId(200L)).thenReturn(false);
            when(reviewRepository.save(any(Review.class))).thenAnswer(i -> {
                Review r = i.getArgument(0); r.setReviewId(2L); return r;
            });

            Review result = reviewService.addReview(dto);
            assertThat(result.getFoodRating()).isEqualTo(5);
            assertThat(result.getIsVerified()).isFalse();
            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        @DisplayName("addReview – duplicate order throws")
        void addReview_duplicateOrder() {
            ReviewRequestDTO dto = new ReviewRequestDTO();
            dto.setOrderId(100L); dto.setFoodRating(4);
            when(reviewRepository.existsByOrderId(100L)).thenReturn(true);

            assertThatThrownBy(() -> reviewService.addReview(dto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("addReview – invalid food rating throws")
        void addReview_invalidFoodRating() {
            ReviewRequestDTO dto = new ReviewRequestDTO();
            dto.setOrderId(300L); dto.setFoodRating(0);
            when(reviewRepository.existsByOrderId(300L)).thenReturn(false);

            assertThatThrownBy(() -> reviewService.addReview(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 1 and 5");
        }

        @Test
        @DisplayName("addReview – food rating above 5 throws")
        void addReview_foodRatingTooHigh() {
            ReviewRequestDTO dto = new ReviewRequestDTO();
            dto.setOrderId(300L); dto.setFoodRating(6);
            when(reviewRepository.existsByOrderId(300L)).thenReturn(false);

            assertThatThrownBy(() -> reviewService.addReview(dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("addReview – invalid delivery rating throws")
        void addReview_invalidDeliveryRating() {
            ReviewRequestDTO dto = new ReviewRequestDTO();
            dto.setOrderId(300L); dto.setFoodRating(4); dto.setDeliveryRating(0);
            when(reviewRepository.existsByOrderId(300L)).thenReturn(false);

            assertThatThrownBy(() -> reviewService.addReview(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Delivery rating");
        }

        @Test
        @DisplayName("addReview – null delivery rating is allowed")
        void addReview_nullDeliveryRating() {
            ReviewRequestDTO dto = new ReviewRequestDTO();
            dto.setOrderId(300L); dto.setCustomerId(10L);
            dto.setRestaurantId(5L); dto.setFoodRating(4);
            dto.setDeliveryRating(null);

            when(reviewRepository.existsByOrderId(300L)).thenReturn(false);
            when(reviewRepository.save(any(Review.class))).thenAnswer(i -> {
                Review r = i.getArgument(0); r.setReviewId(3L); return r;
            });

            Review result = reviewService.addReview(dto);
            assertThat(result.getDeliveryRating()).isNull();
        }
    }

    // ── Retrieval ──

    @Nested
    @DisplayName("Retrieval")
    class RetrievalTests {

        @Test
        @DisplayName("getByReviewId – success")
        void getByReviewId() {
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(sampleReview));
            assertThat(reviewService.getByReviewId(1L).getComment()).isEqualTo("Great food and fast delivery!");
        }

        @Test
        @DisplayName("getByReviewId – not found throws")
        void getByReviewId_notFound() {
            when(reviewRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> reviewService.getByReviewId(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("getByOrderId – success")
        void getByOrderId() {
            when(reviewRepository.findByOrderId(100L)).thenReturn(Optional.of(sampleReview));
            assertThat(reviewService.getByOrderId(100L).getFoodRating()).isEqualTo(4);
        }

        @Test
        @DisplayName("getByOrderId – not found throws")
        void getByOrderId_notFound() {
            when(reviewRepository.findByOrderId(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> reviewService.getByOrderId(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("getByRestaurantId – returns list")
        void getByRestaurantId() {
            when(reviewRepository.findByRestaurantId(5L)).thenReturn(List.of(sampleReview));
            assertThat(reviewService.getByRestaurantId(5L)).hasSize(1);
        }

        @Test
        @DisplayName("getByCustomerId – returns list")
        void getByCustomerId() {
            when(reviewRepository.findByCustomerId(10L)).thenReturn(List.of(sampleReview));
            assertThat(reviewService.getByCustomerId(10L)).hasSize(1);
        }

        @Test
        @DisplayName("getByAgentId – returns list")
        void getByAgentId() {
            when(reviewRepository.findByAgentId(20L)).thenReturn(List.of(sampleReview));
            assertThat(reviewService.getByAgentId(20L)).hasSize(1);
        }

        @Test
        @DisplayName("getAllReviews – returns all")
        void getAllReviews() {
            when(reviewRepository.findAll()).thenReturn(List.of(sampleReview));
            assertThat(reviewService.getAllReviews()).hasSize(1);
        }
    }

    // ── Update & Delete ──

    @Nested
    @DisplayName("Update & Delete")
    class UpdateDeleteTests {

        @Test
        @DisplayName("updateReview – updates food rating")
        void updateReview_foodRating() {
            ReviewUpdateDTO dto = new ReviewUpdateDTO();
            dto.setFoodRating(5);
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(sampleReview));
            when(reviewRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Review result = reviewService.updateReview(1L, dto);
            assertThat(result.getFoodRating()).isEqualTo(5);
        }

        @Test
        @DisplayName("updateReview – updates delivery rating")
        void updateReview_deliveryRating() {
            ReviewUpdateDTO dto = new ReviewUpdateDTO();
            dto.setDeliveryRating(3);
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(sampleReview));
            when(reviewRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Review result = reviewService.updateReview(1L, dto);
            assertThat(result.getDeliveryRating()).isEqualTo(3);
        }

        @Test
        @DisplayName("updateReview – updates comment")
        void updateReview_comment() {
            ReviewUpdateDTO dto = new ReviewUpdateDTO();
            dto.setComment("Updated comment");
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(sampleReview));
            when(reviewRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Review result = reviewService.updateReview(1L, dto);
            assertThat(result.getComment()).isEqualTo("Updated comment");
        }

        @Test
        @DisplayName("updateReview – invalid food rating throws")
        void updateReview_invalidRating() {
            ReviewUpdateDTO dto = new ReviewUpdateDTO();
            dto.setFoodRating(0);
            when(reviewRepository.findById(1L)).thenReturn(Optional.of(sampleReview));

            assertThatThrownBy(() -> reviewService.updateReview(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("deleteReview – success")
        void deleteReview() {
            when(reviewRepository.existsById(1L)).thenReturn(true);
            reviewService.deleteReview(1L);
            verify(reviewRepository).deleteById(1L);
        }

        @Test
        @DisplayName("deleteReview – not found throws")
        void deleteReview_notFound() {
            when(reviewRepository.existsById(999L)).thenReturn(false);
            assertThatThrownBy(() -> reviewService.deleteReview(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── Average Computation ──

    @Nested
    @DisplayName("Average Ratings")
    class AverageTests {

        @Test
        @DisplayName("getAvgFoodRating – returns rounded average")
        void getAvgFoodRating() {
            when(reviewRepository.avgFoodRatingByRestaurantId(5L)).thenReturn(4.333);
            Double avg = reviewService.getAvgFoodRating(5L);
            assertThat(avg).isEqualTo(4.33);
        }

        @Test
        @DisplayName("getAvgFoodRating – null returns 0.0")
        void getAvgFoodRating_null() {
            when(reviewRepository.avgFoodRatingByRestaurantId(99L)).thenReturn(null);
            assertThat(reviewService.getAvgFoodRating(99L)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("getAvgDeliveryRating – returns rounded average")
        void getAvgDeliveryRating() {
            when(reviewRepository.avgDeliveryRatingByAgentId(20L)).thenReturn(4.667);
            Double avg = reviewService.getAvgDeliveryRating(20L);
            assertThat(avg).isEqualTo(4.67);
        }

        @Test
        @DisplayName("getAvgDeliveryRating – null returns 0.0")
        void getAvgDeliveryRating_null() {
            when(reviewRepository.avgDeliveryRatingByAgentId(99L)).thenReturn(null);
            assertThat(reviewService.getAvgDeliveryRating(99L)).isEqualTo(0.0);
        }
    }

    // ── Admin Moderation ──

    @Test
    @DisplayName("verifyReview – sets isVerified to true")
    void verifyReview() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(sampleReview));
        when(reviewRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Review result = reviewService.verifyReview(1L);
        assertThat(result.getIsVerified()).isTrue();
    }

    @Test
    @DisplayName("getUnverifiedReviews – returns unverified list")
    void getUnverifiedReviews() {
        when(reviewRepository.findByIsVerified(false)).thenReturn(List.of(sampleReview));
        assertThat(reviewService.getUnverifiedReviews()).hasSize(1);
    }
}
