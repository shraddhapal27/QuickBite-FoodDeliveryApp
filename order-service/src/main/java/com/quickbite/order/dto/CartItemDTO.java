package com.quickbite.order.dto;

import lombok.Data;

@Data
public class CartItemDTO {
    private Long itemId;
    private Long menuItemId;
    private String name;
    private Double price;
    private Integer quantity;
    private String customization;
}
