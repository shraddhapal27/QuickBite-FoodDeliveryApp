package com.quickbite.restaurant.resource;

import com.quickbite.restaurant.entity.MenuCategory;
import com.quickbite.restaurant.entity.MenuItem;
import com.quickbite.restaurant.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
@Tag(name = "Menu", description = "Menu category and item management for restaurants")
public class MenuResource {

    private final MenuService menuService;

    // --- Category Endpoints ---

    @Operation(summary = "Add a category", description = "Creates a new menu category for a restaurant")
    @ApiResponse(responseCode = "201", description = "Category created")
    @PostMapping("/categories")
    public ResponseEntity<MenuCategory> addCategory(@RequestBody MenuCategory category) {
        return new ResponseEntity<>(menuService.addCategory(category), HttpStatus.CREATED);
    }

    @Operation(summary = "Update a category", description = "Updates an existing menu category")
    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<MenuCategory> updateCategory(
            @Parameter(description = "Category ID") @PathVariable Long categoryId,
            @RequestBody MenuCategory category) {
        return ResponseEntity.ok(menuService.updateCategory(categoryId, category));
    }

    @Operation(summary = "Delete a category", description = "Removes a menu category and its items")
    @ApiResponse(responseCode = "204", description = "Category deleted")
    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "Category ID") @PathVariable Long categoryId) {
        menuService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get categories by restaurant", description = "Lists all menu categories for a restaurant")
    @GetMapping("/restaurants/{restaurantId}/categories")
    public ResponseEntity<List<MenuCategory>> getCategoriesByRestaurant(
            @Parameter(description = "Restaurant ID") @PathVariable Long restaurantId) {
        return ResponseEntity.ok(menuService.getCategoriesByRestaurant(restaurantId));
    }

    // --- Item Endpoints ---

    @Operation(summary = "Add a menu item", description = "Creates a new menu item under a category")
    @ApiResponse(responseCode = "201", description = "Menu item created")
    @PostMapping("/items")
    public ResponseEntity<MenuItem> addMenuItem(@RequestBody MenuItem menuItem) {
        return new ResponseEntity<>(menuService.addMenuItem(menuItem), HttpStatus.CREATED);
    }

    @Operation(summary = "Update a menu item", description = "Updates an existing menu item's details")
    @PutMapping("/items/{itemId}")
    public ResponseEntity<MenuItem> updateMenuItem(
            @Parameter(description = "Item ID") @PathVariable Long itemId,
            @RequestBody MenuItem menuItem) {
        return ResponseEntity.ok(menuService.updateMenuItem(itemId, menuItem));
    }

    @Operation(summary = "Delete a menu item", description = "Removes a menu item from the catalog")
    @ApiResponse(responseCode = "204", description = "Menu item deleted")
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteMenuItem(
            @Parameter(description = "Item ID") @PathVariable Long itemId) {
        menuService.deleteMenuItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get item by ID", description = "Fetches a single menu item by its primary key")
    @GetMapping("/items/{itemId}")
    public ResponseEntity<MenuItem> getItemById(
            @Parameter(description = "Item ID") @PathVariable Long itemId) {
        return ResponseEntity.ok(menuService.getItemById(itemId));
    }

    @Operation(summary = "Get menu by restaurant", description = "Lists all menu items for a restaurant")
    @GetMapping("/restaurants/{restaurantId}/items")
    public ResponseEntity<List<MenuItem>> getMenuByRestaurant(
            @Parameter(description = "Restaurant ID") @PathVariable Long restaurantId) {
        return ResponseEntity.ok(menuService.getMenuByRestaurant(restaurantId));
    }

    @Operation(summary = "Get items by category", description = "Lists all items under a specific category")
    @GetMapping("/categories/{categoryId}/items")
    public ResponseEntity<List<MenuItem>> getItemsByCategory(
            @Parameter(description = "Category ID") @PathVariable Long categoryId) {
        return ResponseEntity.ok(menuService.getItemsByCategory(categoryId));
    }

    @Operation(summary = "Toggle item availability", description = "Marks a menu item as available or unavailable")
    @PutMapping("/items/{itemId}/availability")
    public ResponseEntity<MenuItem> toggleAvailability(
            @Parameter(description = "Item ID") @PathVariable Long itemId,
            @Parameter(description = "Availability status") @RequestParam boolean isAvailable) {
        return ResponseEntity.ok(menuService.toggleAvailability(itemId, isAvailable));
    }

    @Operation(summary = "Search menu items", description = "Searches menu items by name within a restaurant")
    @GetMapping("/restaurants/{restaurantId}/search")
    public ResponseEntity<List<MenuItem>> searchMenuItems(
            @Parameter(description = "Restaurant ID") @PathVariable Long restaurantId,
            @Parameter(description = "Search query") @RequestParam String query) {
        return ResponseEntity.ok(menuService.searchMenuItems(query, restaurantId));
    }

    @Operation(summary = "Get vegetarian items", description = "Lists all vegetarian menu items for a restaurant")
    @GetMapping("/restaurants/{restaurantId}/veg")
    public ResponseEntity<List<MenuItem>> getVegItems(
            @Parameter(description = "Restaurant ID") @PathVariable Long restaurantId) {
        return ResponseEntity.ok(menuService.getVegItems(restaurantId));
    }
}
