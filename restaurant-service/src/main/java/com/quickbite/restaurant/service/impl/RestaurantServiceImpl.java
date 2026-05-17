package com.quickbite.restaurant.service.impl;

import com.quickbite.restaurant.entity.Restaurant;
import com.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;

    @Override
    @Cacheable(value = "restaurants")
    public List<Restaurant> getAll() {
        log.info("Cache MISS — loading all restaurants from DB");
        return restaurantRepository.findAll();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "restaurants", allEntries = true),
            @CacheEvict(value = "restaurantsByOwner", key = "#restaurant.ownerId"),
            @CacheEvict(value = "restaurantsByCuisine", allEntries = true),
            @CacheEvict(value = "restaurantsByCity", allEntries = true)
    })
    public Restaurant registerRestaurant(Restaurant restaurant) {
        restaurant.setIsApproved(false);
        restaurant.setIsOpen(false);
        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("Restaurant registered: '{}' (id={}, owner={})", saved.getName(), saved.getRestaurantId(), saved.getOwnerId());
        return saved;
    }

    @Override
    @Cacheable(value = "restaurantById", key = "#id")
    public Restaurant getById(Long id) {
        log.info("Cache MISS — loading restaurant from DB for id={}", id);
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found with id: " + id));
    }

    @Override
    @Cacheable(value = "restaurantsByOwner", key = "#ownerId")
    public List<Restaurant> getByOwner(Long ownerId) {
        log.info("Cache MISS — loading restaurants from DB for ownerId={}", ownerId);
        return restaurantRepository.findByOwnerId(ownerId);
    }

    @Override
    @Cacheable(value = "restaurantsByCuisine", key = "#cuisine")
    public List<Restaurant> getByCuisine(String cuisine) {
        log.info("Cache MISS — loading restaurants from DB for cuisine={}", cuisine);
        return restaurantRepository.findByCuisine(cuisine);
    }

    @Override
    @Cacheable(value = "restaurantsByCity", key = "#city")
    public List<Restaurant> getByCity(String city) {
        log.info("Cache MISS — loading restaurants from DB for city={}", city);
        return restaurantRepository.findByCity(city);
    }

    @Override
    public List<Restaurant> getNearby(double lat, double lng, double distanceKm) {
        return restaurantRepository.findNearby(lat, lng, distanceKm);
    }

    @Override
    public List<Restaurant> searchRestaurants(String query) {
        return restaurantRepository.findByNameContainingIgnoreCase(query);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "restaurantById", key = "#id"),
            @CacheEvict(value = "restaurants", allEntries = true),
            @CacheEvict(value = "restaurantsByOwner", allEntries = true),
            @CacheEvict(value = "restaurantsByCuisine", allEntries = true),
            @CacheEvict(value = "restaurantsByCity", allEntries = true)
    })
    public Restaurant updateRestaurant(Long id, Restaurant updatedData) {
        Restaurant existing = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found with id: " + id));
        
        if (updatedData.getName() != null) existing.setName(updatedData.getName());
        if (updatedData.getDescription() != null) existing.setDescription(updatedData.getDescription());
        if (updatedData.getCuisine() != null) existing.setCuisine(updatedData.getCuisine());
        if (updatedData.getAddress() != null) existing.setAddress(updatedData.getAddress());
        if (updatedData.getCity() != null) existing.setCity(updatedData.getCity());
        if (updatedData.getLatitude() != null) existing.setLatitude(updatedData.getLatitude());
        if (updatedData.getLongitude() != null) existing.setLongitude(updatedData.getLongitude());
        if (updatedData.getPhone() != null) existing.setPhone(updatedData.getPhone());
        if (updatedData.getDeliveryRadius() != null) existing.setDeliveryRadius(updatedData.getDeliveryRadius());
        if (updatedData.getMinOrderAmount() != null) existing.setMinOrderAmount(updatedData.getMinOrderAmount());
        if (updatedData.getEstimatedDeliveryMin() != null) existing.setEstimatedDeliveryMin(updatedData.getEstimatedDeliveryMin());
        if (updatedData.getImageUrl() != null) existing.setImageUrl(updatedData.getImageUrl());
        
        return restaurantRepository.save(existing);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "restaurantById", key = "#id"),
            @CacheEvict(value = "restaurants", allEntries = true)
    })
    public Restaurant approveRestaurant(Long id, boolean isApproved) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found with id: " + id));
        restaurant.setIsApproved(isApproved);
        return restaurantRepository.save(restaurant);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "restaurantById", key = "#id"),
            @CacheEvict(value = "restaurants", allEntries = true)
    })
    public Restaurant toggleOpen(Long id, boolean isOpen) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found with id: " + id));
        if (isOpen && !restaurant.getIsApproved()) {
            log.warn("Cannot open unapproved restaurant id={}", id);
            throw new RuntimeException("Cannot open an unapproved restaurant.");
        }
        restaurant.setIsOpen(isOpen);
        log.info("Restaurant '{}' (id={}) toggled to {}", restaurant.getName(), id, isOpen ? "OPEN" : "CLOSED");
        return restaurantRepository.save(restaurant);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "restaurantById", key = "#id"),
            @CacheEvict(value = "restaurants", allEntries = true),
            @CacheEvict(value = "restaurantsByOwner", allEntries = true),
            @CacheEvict(value = "restaurantsByCuisine", allEntries = true),
            @CacheEvict(value = "restaurantsByCity", allEntries = true)
    })
    public void deleteRestaurant(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found with id: " + id));
        log.warn("Deleting restaurant '{}' (id={})", restaurant.getName(), id);
        restaurantRepository.delete(restaurant);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "restaurantById", key = "#id"),
            @CacheEvict(value = "restaurants", allEntries = true)
    })
    public Restaurant updateRating(Long id, double newRating) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found with id: " + id));
        restaurant.setAvgRating(newRating);
        return restaurantRepository.save(restaurant);
    }

    // ── Filter Methods ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Restaurant> filterByRating(Double minRating) {
        log.info("Filtering restaurants with minRating >= {}", minRating);
        return restaurantRepository.findByAvgRatingGreaterThanEqual(minRating);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Restaurant> filterByDeliveryTime(Integer maxMinutes) {
        log.info("Filtering restaurants with deliveryTime <= {} min", maxMinutes);
        return restaurantRepository.findByEstimatedDeliveryMinLessThanEqual(maxMinutes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Restaurant> filterByPriceRange(Double minPrice, Double maxPrice) {
        log.info("Filtering restaurants with price range [{}, {}]", minPrice, maxPrice);
        return restaurantRepository.findByMinOrderAmountBetween(minPrice, maxPrice);
    }
}

