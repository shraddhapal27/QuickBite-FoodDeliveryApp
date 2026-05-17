package com.quickbite.cart.service;

import com.quickbite.cart.entity.Cart;
import com.quickbite.cart.entity.CartItem;

import java.util.List;

public interface CartService {
    Cart getCartByCustomer(Long customerId);
    Cart addItem(Long customerId, CartItem item, Long restaurantId);
    Cart removeItem(Long customerId, Long itemId);
    Cart updateQuantity(Long customerId, Long itemId, Integer quantity);
    void clearCart(Long customerId);
    Double cartTotal(Long customerId);
    Cart applyPromoCode(Long customerId, String code);
    List<Cart> getAllCarts();
}
