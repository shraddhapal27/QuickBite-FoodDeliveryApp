package com.quickbite.restaurant.service;

import com.quickbite.restaurant.entity.Restaurant;
import java.util.List;

public interface RestaurantService {

    List<Restaurant> getAll();

    Restaurant registerRestaurant(Restaurant restaurant);

    Restaurant getById(Long id);

    List<Restaurant> getByOwner(Long ownerId);

    List<Restaurant> getByCuisine(String cuisine);

    List<Restaurant> getByCity(String city);

    List<Restaurant> getNearby(double lat, double lng, double distanceKm);

    List<Restaurant> searchRestaurants(String query);

    Restaurant updateRestaurant(Long id, Restaurant updatedData);

    Restaurant approveRestaurant(Long id, boolean isApproved);

    Restaurant toggleOpen(Long id, boolean isOpen);

    void deleteRestaurant(Long id);

    Restaurant updateRating(Long id, double newRating);

    // ── Filter Methods ──────────────────────────────────────────────────────
    List<Restaurant> filterByRating(Double minRating);

    List<Restaurant> filterByDeliveryTime(Integer maxMinutes);

    List<Restaurant> filterByPriceRange(Double minPrice, Double maxPrice);
}
