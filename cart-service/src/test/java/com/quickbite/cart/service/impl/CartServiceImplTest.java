package com.quickbite.cart.service.impl;

import com.quickbite.cart.entity.Cart;
import com.quickbite.cart.entity.CartItem;
import com.quickbite.cart.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartServiceImpl Unit Tests")
class CartServiceImplTest {

    @Mock private CartRepository cartRepository;
    @InjectMocks private CartServiceImpl cartService;

    private Cart sampleCart;
    private CartItem sampleItem;

    @BeforeEach
    void setUp() {
        sampleItem = CartItem.builder()
                .itemId(1L).menuItemId(100L).name("Burger")
                .price(200.0).quantity(2).customization("No onions")
                .build();

        sampleCart = Cart.builder()
                .cartId(1L).customerId(10L).restaurantId(5L)
                .totalPrice(400.0)
                .items(new ArrayList<>(List.of(sampleItem)))
                .build();
    }

    // ── Get Cart ──

    @Test
    @DisplayName("getCartByCustomer – returns existing cart")
    void getCartByCustomer_existing() {
        when(cartRepository.findByCustomerId(10L)).thenReturn(Optional.of(sampleCart));
        Cart result = cartService.getCartByCustomer(10L);
        assertThat(result.getCustomerId()).isEqualTo(10L);
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("getCartByCustomer – creates new cart when none exists")
    void getCartByCustomer_createsNew() {
        when(cartRepository.findByCustomerId(99L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> {
            Cart c = i.getArgument(0); c.setCartId(2L); return c;
        });
        Cart result = cartService.getCartByCustomer(99L);
        assertThat(result.getCustomerId()).isEqualTo(99L);
        assertThat(result.getTotalPrice()).isEqualTo(0.0);
        verify(cartRepository).save(any(Cart.class));
    }

    // ── Add Item ──

    @Nested
    @DisplayName("Add Item")
    class AddItemTests {

        @Test
        @DisplayName("addItem – adds new item to cart")
        void addItem_newItem() {
            CartItem newItem = CartItem.builder()
                    .menuItemId(200L).name("Pizza").price(300.0).quantity(1).build();
            when(cartRepository.findByCustomerId(10L)).thenReturn(Optional.of(sampleCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

            Cart result = cartService.addItem(10L, newItem, 5L);
            assertThat(result.getItems()).hasSize(2);
            assertThat(result.getTotalPrice()).isEqualTo(700.0); // 200*2 + 300*1
        }

        @Test
        @DisplayName("addItem – increments quantity for existing item")
        void addItem_existingItem() {
            CartItem duplicate = CartItem.builder()
                    .menuItemId(100L).name("Burger").price(200.0).quantity(1).build();
            when(cartRepository.findByCustomerId(10L)).thenReturn(Optional.of(sampleCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

            Cart result = cartService.addItem(10L, duplicate, 5L);
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getQuantity()).isEqualTo(3); // 2 + 1
        }

        @Test
        @DisplayName("addItem – throws when switching restaurants")
        void addItem_differentRestaurant() {
            when(cartRepository.findByCustomerId(10L)).thenReturn(Optional.of(sampleCart));
            CartItem item = CartItem.builder()
                    .menuItemId(300L).name("Sushi").price(500.0).quantity(1).build();

            assertThatThrownBy(() -> cartService.addItem(10L, item, 99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("different restaurant");
        }

        @Test
        @DisplayName("addItem – sets restaurantId on empty cart")
        void addItem_setsRestaurantId() {
            Cart emptyCart = Cart.builder()
                    .cartId(2L).customerId(20L).restaurantId(null)
                    .totalPrice(0.0).items(new ArrayList<>())
                    .build();
            CartItem item = CartItem.builder()
                    .menuItemId(100L).name("Burger").price(200.0).quantity(1).build();
            when(cartRepository.findByCustomerId(20L)).thenReturn(Optional.of(emptyCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

            Cart result = cartService.addItem(20L, item, 5L);
            assertThat(result.getRestaurantId()).isEqualTo(5L);
        }
    }

    // ── Remove Item ──

    @Test
    @DisplayName("removeItem – removes item and recalculates total")
    void removeItem_success() {
        when(cartRepository.findByCustomerId(10L)).thenReturn(Optional.of(sampleCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        Cart result = cartService.removeItem(10L, 1L);
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalPrice()).isEqualTo(0.0);
        assertThat(result.getRestaurantId()).isNull(); // cleared when last item removed
    }

    // ── Update Quantity ──

    @Nested
    @DisplayName("Update Quantity")
    class UpdateQuantityTests {

        @Test
        @DisplayName("updateQuantity – updates quantity and recalculates")
        void updateQuantity_positive() {
            when(cartRepository.findByCustomerId(10L)).thenReturn(Optional.of(sampleCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

            Cart result = cartService.updateQuantity(10L, 1L, 5);
            assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5);
            assertThat(result.getTotalPrice()).isEqualTo(1000.0); // 200 * 5
        }

        @Test
        @DisplayName("updateQuantity – zero quantity removes item")
        void updateQuantity_zero() {
            when(cartRepository.findByCustomerId(10L)).thenReturn(Optional.of(sampleCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

            Cart result = cartService.updateQuantity(10L, 1L, 0);
            assertThat(result.getItems()).isEmpty();
        }
    }

    // ── Clear Cart ──

    @Test
    @DisplayName("clearCart – empties cart and resets totals")
    void clearCart() {
        when(cartRepository.findByCustomerId(10L)).thenReturn(Optional.of(sampleCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

        cartService.clearCart(10L);
        assertThat(sampleCart.getItems()).isEmpty();
        assertThat(sampleCart.getRestaurantId()).isNull();
        assertThat(sampleCart.getTotalPrice()).isEqualTo(0.0);
    }

    // ── Cart Total ──

    @Test
    @DisplayName("cartTotal – returns total price")
    void cartTotal() {
        when(cartRepository.findByCustomerId(10L)).thenReturn(Optional.of(sampleCart));
        Double total = cartService.cartTotal(10L);
        assertThat(total).isEqualTo(400.0);
    }

    // ── Promo Code ──

    @Nested
    @DisplayName("Promo Code")
    class PromoCodeTests {

        @Test
        @DisplayName("applyPromoCode – QUICK10 gives 10% discount")
        void applyPromo_valid() {
            when(cartRepository.findByCustomerId(10L)).thenReturn(Optional.of(sampleCart));
            when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

            Cart result = cartService.applyPromoCode(10L, "QUICK10");
            assertThat(result.getTotalPrice()).isEqualTo(360.0); // 400 * 0.9
        }

        @Test
        @DisplayName("applyPromoCode – invalid code throws")
        void applyPromo_invalid() {
            when(cartRepository.findByCustomerId(10L)).thenReturn(Optional.of(sampleCart));
            assertThatThrownBy(() -> cartService.applyPromoCode(10L, "INVALID"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid promo code");
        }
    }

    // ── Get All Carts ──

    @Test
    @DisplayName("getAllCarts – returns all carts")
    void getAllCarts() {
        when(cartRepository.findAll()).thenReturn(List.of(sampleCart));
        assertThat(cartService.getAllCarts()).hasSize(1);
    }
}
