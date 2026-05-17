package com.quickbite.cart.service.impl;

import com.quickbite.cart.entity.Cart;
import com.quickbite.cart.entity.CartItem;
import com.quickbite.cart.repository.CartRepository;
import com.quickbite.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;

    @Override
    @Cacheable(value = "carts", key = "#customerId")
    public Cart getCartByCustomer(Long customerId) {
        log.info("Cache MISS — loading cart from DB for customerId={}", customerId);
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> createNewCart(customerId));
    }

    private Cart createNewCart(Long customerId) {
        Cart cart = Cart.builder()
                .customerId(customerId)
                .totalPrice(0.0)
                .build();
        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = "carts", key = "#customerId")
    public Cart addItem(Long customerId, CartItem item, Long restaurantId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> createNewCart(customerId));

        if (cart.getRestaurantId() != null && !cart.getRestaurantId().equals(restaurantId)) {
            // Case study requires prompting the user to clear cart when switching restaurants.
            // Returning an exception so the controller can send 400 Bad Request to frontend.
            throw new IllegalArgumentException("Cart already contains items from a different restaurant. Clear cart first.");
        }

        if (cart.getRestaurantId() == null) {
            cart.setRestaurantId(restaurantId);
        }

        // Check if item already exists in cart
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(i -> i.getMenuItemId().equals(item.getMenuItemId()))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(existingItem.get().getQuantity() + item.getQuantity());
        } else {
            cart.getItems().add(item);
        }

        recalculateTotal(cart);
        log.info("Item added to cart: customerId={}, menuItemId={}, qty={}", customerId, item.getMenuItemId(), item.getQuantity());
        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = "carts", key = "#customerId")
    public Cart removeItem(Long customerId, Long itemId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> createNewCart(customerId));
        cart.getItems().removeIf(i -> i.getItemId().equals(itemId));
        
        if (cart.getItems().isEmpty()) {
            cart.setRestaurantId(null);
        }

        recalculateTotal(cart);
        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = "carts", key = "#customerId")
    public Cart updateQuantity(Long customerId, Long itemId, Integer quantity) {
        if (quantity <= 0) {
            return removeItem(customerId, itemId);
        }

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> createNewCart(customerId));
        cart.getItems().stream()
                .filter(i -> i.getItemId().equals(itemId))
                .findFirst()
                .ifPresent(i -> i.setQuantity(quantity));

        recalculateTotal(cart);
        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    @CacheEvict(value = "carts", key = "#customerId")
    public void clearCart(Long customerId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> createNewCart(customerId));
        cart.getItems().clear();
        cart.setRestaurantId(null);
        cart.setTotalPrice(0.0);
        log.info("Cart cleared for customerId={}", customerId);
        cartRepository.save(cart);
    }

    @Override
    public Double cartTotal(Long customerId) {
        return getCartByCustomer(customerId).getTotalPrice();
    }

    @Override
    @Transactional
    @CacheEvict(value = "carts", key = "#customerId")
    public Cart applyPromoCode(Long customerId, String code) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> createNewCart(customerId));
        // Dummy promo code implementation
        if ("QUICK10".equalsIgnoreCase(code)) {
            double currentTotal = cart.getItems().stream()
                    .mapToDouble(i -> i.getPrice() * i.getQuantity())
                    .sum();
            cart.setTotalPrice(currentTotal * 0.9); // 10% discount
            return cartRepository.save(cart);
        }
        throw new IllegalArgumentException("Invalid promo code");
    }

    @Override
    public List<Cart> getAllCarts() {
        return cartRepository.findAll();
    }

    private void recalculateTotal(Cart cart) {
        double total = cart.getItems().stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity())
                .sum();
        cart.setTotalPrice(total);
    }
}
