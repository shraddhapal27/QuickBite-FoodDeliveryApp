package com.quickbite.restaurant.resource;

import com.quickbite.restaurant.entity.Restaurant;
import com.quickbite.restaurant.service.RestaurantService;
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
@RequestMapping("/restaurants")
@RequiredArgsConstructor
@Tag(name = "Restaurants", description = "Restaurant registration, search, approval, and management")
public class RestaurantResource {

    private final RestaurantService restaurantService;

    @Operation(summary = "Register a restaurant", description = "Creates a new restaurant listing on the platform")
    @ApiResponse(responseCode = "201", description = "Restaurant registered successfully")
    @PostMapping
    public ResponseEntity<Restaurant> registerRestaurant(@RequestBody Restaurant restaurant) {
        return new ResponseEntity<>(restaurantService.registerRestaurant(restaurant), HttpStatus.CREATED);
    }

    @Operation(summary = "Get all restaurants", description = "Returns a list of all registered restaurants")
    @GetMapping
    public ResponseEntity<List<Restaurant>> getAllRestaurants() {
        return ResponseEntity.ok(restaurantService.getAll());
    }

    @Operation(summary = "Get restaurant by ID", description = "Fetches a single restaurant by its primary key")
    @GetMapping("/{id}")
    public ResponseEntity<Restaurant> getById(
            @Parameter(description = "Restaurant ID") @PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getById(id));
    }

    @Operation(summary = "Get restaurants by owner", description = "Lists all restaurants owned by a specific user")
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<Restaurant>> getByOwner(
            @Parameter(description = "Owner user ID") @PathVariable Long ownerId) {
        return ResponseEntity.ok(restaurantService.getByOwner(ownerId));
    }

    @Operation(summary = "Filter by cuisine", description = "Returns restaurants matching the specified cuisine type")
    @GetMapping("/cuisine/{cuisine}")
    public ResponseEntity<List<Restaurant>> getByCuisine(
            @Parameter(description = "Cuisine type (e.g. Indian, Chinese)") @PathVariable String cuisine) {
        return ResponseEntity.ok(restaurantService.getByCuisine(cuisine));
    }

    @Operation(summary = "Filter by city", description = "Returns restaurants located in the specified city")
    @GetMapping("/city/{city}")
    public ResponseEntity<List<Restaurant>> getByCity(
            @Parameter(description = "City name") @PathVariable String city) {
        return ResponseEntity.ok(restaurantService.getByCity(city));
    }

    @Operation(summary = "Find nearby restaurants", description = "Returns restaurants within a given radius from coordinates")
    @GetMapping("/nearby")
    public ResponseEntity<List<Restaurant>> getNearby(
            @Parameter(description = "Latitude") @RequestParam double lat,
            @Parameter(description = "Longitude") @RequestParam double lng,
            @Parameter(description = "Search radius in km") @RequestParam(defaultValue = "10.0") double distanceKm) {
        return ResponseEntity.ok(restaurantService.getNearby(lat, lng, distanceKm));
    }

    @Operation(summary = "Search restaurants", description = "Full-text search across restaurant names and descriptions")
    @GetMapping("/search")
    public ResponseEntity<List<Restaurant>> searchRestaurants(
            @Parameter(description = "Search query") @RequestParam String query) {
        return ResponseEntity.ok(restaurantService.searchRestaurants(query));
    }

    @Operation(summary = "Update restaurant", description = "Updates an existing restaurant's details")
    @PutMapping("/{id}")
    public ResponseEntity<Restaurant> updateRestaurant(
            @Parameter(description = "Restaurant ID") @PathVariable Long id,
            @RequestBody Restaurant restaurant) {
        return ResponseEntity.ok(restaurantService.updateRestaurant(id, restaurant));
    }

    @Operation(summary = "Approve/reject restaurant (Admin)", description = "Admin action to approve or reject a restaurant listing")
    @PutMapping("/{id}/approve")
    public ResponseEntity<Restaurant> approveRestaurant(
            @Parameter(description = "Restaurant ID") @PathVariable Long id,
            @Parameter(description = "Approval status") @RequestParam boolean isApproved) {
        return ResponseEntity.ok(restaurantService.approveRestaurant(id, isApproved));
    }

    @Operation(summary = "Toggle open/closed", description = "Toggles the restaurant's open status for accepting orders")
    @PutMapping("/{id}/toggle-open")
    public ResponseEntity<Restaurant> toggleOpen(
            @Parameter(description = "Restaurant ID") @PathVariable Long id,
            @Parameter(description = "Open status") @RequestParam boolean isOpen) {
        return ResponseEntity.ok(restaurantService.toggleOpen(id, isOpen));
    }

    @Operation(summary = "Update rating", description = "Updates the restaurant's average rating (called by review-service)")
    @PutMapping("/{id}/rating")
    public ResponseEntity<Restaurant> updateRating(
            @Parameter(description = "Restaurant ID") @PathVariable Long id,
            @Parameter(description = "New average rating") @RequestParam double rating) {
        return ResponseEntity.ok(restaurantService.updateRating(id, rating));
    }

    @Operation(summary = "Filter by rating", description = "Returns restaurants with average rating >= minRating")
    @GetMapping("/filter/rating")
    public ResponseEntity<List<Restaurant>> filterByRating(
            @Parameter(description = "Minimum rating (e.g. 4.0)") @RequestParam Double minRating) {
        return ResponseEntity.ok(restaurantService.filterByRating(minRating));
    }

    @Operation(summary = "Filter by delivery time", description = "Returns restaurants with estimated delivery time <= maxMinutes")
    @GetMapping("/filter/delivery-time")
    public ResponseEntity<List<Restaurant>> filterByDeliveryTime(
            @Parameter(description = "Max delivery minutes (e.g. 30)") @RequestParam Integer maxMinutes) {
        return ResponseEntity.ok(restaurantService.filterByDeliveryTime(maxMinutes));
    }

    @Operation(summary = "Filter by price range", description = "Returns restaurants within a specific minimum order price range")
    @GetMapping("/filter/price")
    public ResponseEntity<List<Restaurant>> filterByPriceRange(
            @Parameter(description = "Minimum price") @RequestParam Double minPrice,
            @Parameter(description = "Maximum price") @RequestParam Double maxPrice) {
        return ResponseEntity.ok(restaurantService.filterByPriceRange(minPrice, maxPrice));
    }

    @Operation(summary = "Delete restaurant", description = "Permanently removes a restaurant listing")
    @ApiResponse(responseCode = "204", description = "Restaurant deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRestaurant(
            @Parameter(description = "Restaurant ID") @PathVariable Long id) {
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.noContent().build();
    }
}
