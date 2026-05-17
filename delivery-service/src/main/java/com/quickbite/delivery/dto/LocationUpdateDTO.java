package com.quickbite.delivery.dto;

import lombok.Data;

/**
 * Request body for updating an agent's live GPS coordinates.
 */
@Data
public class LocationUpdateDTO {
    private Double latitude;
    private Double longitude;
}
