package com.quickbite.order.dto;

import lombok.Data;
import java.util.List;

@Data
public class CartDTO {
    private Long cartId;
    private Long customerId;
    private Long restaurantId;
    private Double totalPrice;
    private List<CartItemDTO> items;
}
