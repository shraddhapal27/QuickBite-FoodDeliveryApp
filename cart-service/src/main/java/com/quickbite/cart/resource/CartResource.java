package com.quickbite.cart.resource;

import com.quickbite.cart.entity.Cart;
import com.quickbite.cart.entity.CartItem;
import com.quickbite.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart operations")
public class CartResource {

    private final CartService cartService;

    @Operation(summary = "Get cart")
    @GetMapping("/{customerId}")
    public ResponseEntity<Cart> getCartByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(cartService.getCartByCustomer(customerId));
    }

    @Operation(summary = "Add item to cart")
    @PostMapping("/{customerId}/add")
    public ResponseEntity<?> addItemToCart(@PathVariable Long customerId,
                                           @RequestParam Long restaurantId,
                                           @RequestBody CartItem item) {
        try {
            return new ResponseEntity<>(cartService.addItem(customerId, item, restaurantId), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Remove item from cart")
    @DeleteMapping("/{customerId}/remove/{itemId}")
    public ResponseEntity<Cart> removeItemFromCart(@PathVariable Long customerId, @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(customerId, itemId));
    }

    @Operation(summary = "Update item quantity")
    @PutMapping("/{customerId}/update/{itemId}")
    public ResponseEntity<Cart> updateItemQuantity(@PathVariable Long customerId, @PathVariable Long itemId, @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartService.updateQuantity(customerId, itemId, quantity));
    }

    @Operation(summary = "Clear cart")
    @DeleteMapping("/{customerId}/clear")
    public ResponseEntity<Void> clearCart(@PathVariable Long customerId) {
        cartService.clearCart(customerId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Apply promo code")
    @PostMapping("/{customerId}/promo")
    public ResponseEntity<?> applyPromoCode(@PathVariable Long customerId, @RequestParam String code) {
        try {
            return ResponseEntity.ok(cartService.applyPromoCode(customerId, code));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(summary = "Get all carts (Admin)")
    @GetMapping("/all")
    public ResponseEntity<List<Cart>> getAllCarts() {
        return ResponseEntity.ok(cartService.getAllCarts());
    }
}
