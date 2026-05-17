package com.quickbite.payment.dto;

import lombok.Data;

@Data
public class WalletPayRequestDTO {
    private Long customerId;
    private Long orderId;
    private Double amount;
}
