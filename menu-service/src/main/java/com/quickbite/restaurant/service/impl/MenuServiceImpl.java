package com.quickbite.restaurant.service.impl;

import com.quickbite.restaurant.entity.MenuCategory;
import com.quickbite.restaurant.entity.MenuItem;
import com.quickbite.restaurant.repository.MenuCategoryRepository;
import com.quickbite.restaurant.repository.MenuItemRepository;
import com.quickbite.restaurant.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;

    @Override
    @Caching(evict = {
            @CacheEvict(value = "menuCategories", key = "#category.restaurantId")
    })
    public MenuCategory addCategory(MenuCategory category) {
        MenuCategory saved = categoryRepository.save(category);
        log.info("Category added: '{}' (id={}, restaurantId={})", saved.getName(), saved.getCategoryId(), saved.getRestaurantId());
        return saved;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "menuCategories", allEntries = true)
    })
    public MenuCategory updateCategory(Long categoryId, MenuCategory categoryDetails) {
        MenuCategory existingCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("MenuCategory not found with id: " + categoryId));

        existingCategory.setName(categoryDetails.getName());
        existingCategory.setDescription(categoryDetails.getDescription());
        existingCategory.setImageUrl(categoryDetails.getImageUrl());
        existingCategory.setDisplayOrder(categoryDetails.getDisplayOrder());

        return categoryRepository.save(existingCategory);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "menuCategories", allEntries = true),
            @CacheEvict(value = "itemsByCategory", key = "#categoryId")
    })
    public void deleteCategory(Long categoryId) {
        categoryRepository.deleteById(categoryId);
    }

    @Override
    @Cacheable(value = "menuCategories", key = "#restaurantId")
    public List<MenuCategory> getCategoriesByRestaurant(Long restaurantId) {
        log.info("Cache MISS — loading categories from DB for restaurantId={}", restaurantId);
        return categoryRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "menuByRestaurant", key = "#menuItem.restaurantId"),
            @CacheEvict(value = "itemsByCategory", key = "#menuItem.categoryId"),
            @CacheEvict(value = "vegItems", key = "#menuItem.restaurantId"),
            @CacheEvict(value = "menuItems", key = "#menuItem.itemId", condition = "#menuItem.itemId != null")
    })
    public MenuItem addMenuItem(MenuItem menuItem) {
        MenuItem saved = itemRepository.save(menuItem);
        log.info("Menu item added: '{}' (id={}, price={})", saved.getName(), saved.getItemId(), saved.getPrice());
        return saved;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "menuItems", key = "#itemId"),
            @CacheEvict(value = "menuByRestaurant", allEntries = true),
            @CacheEvict(value = "itemsByCategory", allEntries = true),
            @CacheEvict(value = "vegItems", allEntries = true)
    })
    public MenuItem updateMenuItem(Long itemId, MenuItem itemDetails) {
        MenuItem existingItem = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("MenuItem not found with id: " + itemId));

        existingItem.setName(itemDetails.getName());
        existingItem.setDescription(itemDetails.getDescription());
        existingItem.setPrice(itemDetails.getPrice());
        existingItem.setDiscountedPrice(itemDetails.getDiscountedPrice());
        existingItem.setImageUrl(itemDetails.getImageUrl());
        existingItem.setIsVeg(itemDetails.getIsVeg());
        existingItem.setCalories(itemDetails.getCalories());
        existingItem.setTags(itemDetails.getTags());

        return itemRepository.save(existingItem);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "menuItems", key = "#itemId"),
            @CacheEvict(value = "menuByRestaurant", allEntries = true),
            @CacheEvict(value = "itemsByCategory", allEntries = true),
            @CacheEvict(value = "vegItems", allEntries = true)
    })
    public void deleteMenuItem(Long itemId) {
        log.warn("Deleting menu item id={}", itemId);
        itemRepository.deleteById(itemId);
    }

    @Override
    @Cacheable(value = "menuItems", key = "#itemId")
    public MenuItem getItemById(Long itemId) {
        log.info("Cache MISS — loading menu item from DB for itemId={}", itemId);
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("MenuItem not found with id: " + itemId));
    }

    @Override
    @Cacheable(value = "menuByRestaurant", key = "#restaurantId")
    public List<MenuItem> getMenuByRestaurant(Long restaurantId) {
        log.info("Cache MISS — loading menu from DB for restaurantId={}", restaurantId);
        return itemRepository.findByRestaurantId(restaurantId);
    }

    @Override
    @Cacheable(value = "itemsByCategory", key = "#categoryId")
    public List<MenuItem> getItemsByCategory(Long categoryId) {
        log.info("Cache MISS — loading items from DB for categoryId={}", categoryId);
        return itemRepository.findByCategoryId(categoryId);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "menuItems", key = "#itemId"),
            @CacheEvict(value = "menuByRestaurant", allEntries = true),
            @CacheEvict(value = "itemsByCategory", allEntries = true),
            @CacheEvict(value = "vegItems", allEntries = true)
    })
    public MenuItem toggleAvailability(Long itemId, boolean isAvailable) {
        MenuItem existingItem = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("MenuItem not found with id: " + itemId));
        existingItem.setIsAvailable(isAvailable);
        return itemRepository.save(existingItem);
    }

    @Override
    public List<MenuItem> searchMenuItems(String query, Long restaurantId) {
        return itemRepository.findByNameContainingIgnoreCaseAndRestaurantId(query, restaurantId);
    }

    @Override
    @Cacheable(value = "vegItems", key = "#restaurantId")
    public List<MenuItem> getVegItems(Long restaurantId) {
        log.info("Cache MISS — loading veg items from DB for restaurantId={}", restaurantId);
        return itemRepository.findByRestaurantIdAndIsVegTrue(restaurantId);
    }
}
