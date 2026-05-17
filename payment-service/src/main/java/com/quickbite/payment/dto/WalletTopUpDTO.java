package com.quickbite.payment.dto;

import lombok.Data;

@Data
public class WalletTopUpDTO {
    private Long customerId;
    private Double amount;
    private String description;
}
