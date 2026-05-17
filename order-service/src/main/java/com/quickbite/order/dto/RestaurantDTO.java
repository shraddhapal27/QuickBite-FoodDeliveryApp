package com.quickbite.order.dto;

import lombok.Data;

@Data
public class RestaurantDTO {
    private Long restaurantId;
    private String name;
    private Double latitude;
    private Double longitude;
}
