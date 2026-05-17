package com.quickbite.review.dto;

import lombok.Data;

/**
 * Request body for submitting a new review after order delivery.
 */
@Data
public class ReviewRequestDTO {
    private Long orderId;
    private Long customerId;
    private Long restaurantId;
    /** Nullable — agent may not exist for all order types */
    private Long agentId;
    /** Food quality rating 1–5 (required) */
    private Integer foodRating;
    /** Delivery experience rating 1–5 (optional) */
    private Integer deliveryRating;
    private String comment;
}
