package com.quickbite.order.dto;

import lombok.Data;

@Data
public class DeliveryAgentDTO {
    private Long agentId;
    private Long userId;
    private String fullName;
    private Boolean isAvailable;
    private Long currentOrderId;
}
