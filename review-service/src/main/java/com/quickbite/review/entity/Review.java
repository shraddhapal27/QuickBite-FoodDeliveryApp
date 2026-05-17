package com.quickbite.review.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a customer's dual-rating review after an order is delivered.
 * One review is allowed per order (enforced by unique constraint on orderId).
 */
@Entity
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_order", columnNames = "orderId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    /** Unique — one review per completed order */
    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Long restaurantId;

    /** May be null if no delivery agent was involved (e.g. self-pickup) */
    private Long agentId;

    /** Food quality rating 1–5 */
    @Column(nullable = false)
    private Integer foodRating;

    /** Delivery experience rating 1–5 (nullable if no agent) */
    private Integer deliveryRating;

    @Column(length = 1000)
    private String comment;

    @Builder.Default
    private LocalDateTime reviewDate = LocalDateTime.now();

    /**
     * False until admin has verified the review is genuine.
     * Unverified reviews are still visible but flagged differently in the UI.
     */
    @Builder.Default
    private Boolean isVerified = false;

    @PrePersist
    protected void onCreate() {
        if (reviewDate == null) reviewDate = LocalDateTime.now();
    }
}
