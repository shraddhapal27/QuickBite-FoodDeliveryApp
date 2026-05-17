package com.quickbite.delivery.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a delivery agent registered on the QuickBite platform.
 * Agents require admin verification before they can receive order assignments.
 */
@Entity
@Table(name = "delivery_agents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long agentId;

    /** FK to the auth-service User record */
    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    @Column(nullable = false)
    private String vehicleNumber;

    /** Current GPS latitude (updated periodically by agent app) */
    private Double currentLatitude;

    /** Current GPS longitude (updated periodically by agent app) */
    private Double currentLongitude;

    /** True when the agent is online and willing to receive orders */
    @Builder.Default
    @JsonProperty("isAvailable")
    private Boolean isAvailable = false;

    /** True once admin has verified identity and vehicle documents */
    @Builder.Default
    @JsonProperty("isVerified")
    private Boolean isVerified = false;

    /** Running average of customer delivery ratings (1-5) */
    @Builder.Default
    private Double avgRating = 0.0;

    @Builder.Default
    private Integer totalDeliveries = 0;

    @Builder.Default
    private Integer totalRatings = 0;

    /** ID of the order currently being delivered (null if idle) */
    private Long currentOrderId;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
