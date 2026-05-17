package com.quickbite.review.dto;

import lombok.Data;

/**
 * Request body for updating an existing review's ratings and comment.
 */
@Data
public class ReviewUpdateDTO {
    private Integer foodRating;
    private Integer deliveryRating;
    private String comment;
}
