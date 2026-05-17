package com.quickbite.restaurant.repository;

import com.quickbite.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    List<Restaurant> findByOwnerId(Long ownerId);

    List<Restaurant> findByCuisine(String cuisine);

    List<Restaurant> findByCity(String city);

    List<Restaurant> findByIsOpenTrueAndIsApprovedTrue();

    List<Restaurant> findByNameContainingIgnoreCase(String name);

    long countByCity(String city);

    // ── Filter Queries ──────────────────────────────────────────────────────
    List<Restaurant> findByAvgRatingGreaterThanEqual(Double minRating);

    List<Restaurant> findByEstimatedDeliveryMinLessThanEqual(Integer maxMinutes);

    List<Restaurant> findByMinOrderAmountBetween(Double minPrice, Double maxPrice);

    List<Restaurant> findByAvgRatingGreaterThanEqualAndEstimatedDeliveryMinLessThanEqual(
            Double minRating, Integer maxDeliveryMin);

    // Haversine formula to find restaurants within a certain distance (in km)
    // 6371 is the Earth's radius in kilometers
    @Query(value = "SELECT * FROM restaurants r " +
            "WHERE (6371 * acos(cos(radians(:lat)) * cos(radians(r.latitude)) * " +
            "cos(radians(r.longitude) - radians(:lng)) + " +
            "sin(radians(:lat)) * sin(radians(r.latitude)))) <= :distance", 
            nativeQuery = true)
    List<Restaurant> findNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("distance") double distance);
}
