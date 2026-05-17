package com.quickbite.delivery.dto;

import lombok.Data;

/**
 * Request body for updating an agent's delivery rating after a completed delivery.
 */
@Data
public class RatingUpdateDTO {
    /** New delivery rating (1–5) for this delivery */
    private Double newRating;
}
