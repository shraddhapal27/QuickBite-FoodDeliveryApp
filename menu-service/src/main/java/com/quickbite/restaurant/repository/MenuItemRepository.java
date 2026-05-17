package com.quickbite.restaurant.repository;

import com.quickbite.restaurant.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByRestaurantId(Long restaurantId);

    List<MenuItem> findByCategoryId(Long categoryId);

    List<MenuItem> findByRestaurantIdAndIsVegTrue(Long restaurantId);

    List<MenuItem> findByRestaurantIdAndIsAvailableTrue(Long restaurantId);

    List<MenuItem> findByNameContainingIgnoreCaseAndRestaurantId(String name, Long restaurantId);

    List<MenuItem> findByRestaurantIdAndPriceLessThanEqual(Long restaurantId, Double maxPrice);

    long countByRestaurantId(Long restaurantId);
}
