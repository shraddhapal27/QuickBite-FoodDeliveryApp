package com.quickbite.delivery.dto;

import com.quickbite.delivery.entity.VehicleType;
import lombok.Data;

/**
 * Request body for registering a new delivery agent.
 */
@Data
public class AgentRegistrationDTO {
    private Long userId;
    private String fullName;
    private String phone;
    private VehicleType vehicleType;
    private String vehicleNumber;
}
