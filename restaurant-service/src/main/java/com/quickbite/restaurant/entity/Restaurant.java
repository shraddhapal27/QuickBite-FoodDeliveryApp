package com.quickbite.restaurant.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "restaurants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long restaurantId;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String cuisine;

    private String address;

    private String city;

    private Double latitude;

    private Double longitude;

    private String phone;

    @Builder.Default
    private Double avgRating = 0.0;

    @Builder.Default
    @JsonProperty("isOpen")
    private Boolean isOpen = false;

    @Builder.Default
    @JsonProperty("isApproved")
    private Boolean isApproved = false;

    private Double deliveryRadius; // in kilometers

    private Double minOrderAmount;

    private Integer estimatedDeliveryMin;

    @Column(length = 1000)
    private String imageUrl;
}

