package com.quickbite.restaurant.repository;

import com.quickbite.restaurant.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    List<MenuCategory> findByRestaurantIdOrderByDisplayOrderAsc(Long restaurantId);

    long countByRestaurantId(Long restaurantId);
}
