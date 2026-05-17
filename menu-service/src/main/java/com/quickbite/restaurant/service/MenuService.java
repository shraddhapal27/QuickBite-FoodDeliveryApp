package com.quickbite.restaurant.service;

import com.quickbite.restaurant.entity.MenuCategory;
import com.quickbite.restaurant.entity.MenuItem;

import java.util.List;

public interface MenuService {

    // Category Methods
    MenuCategory addCategory(MenuCategory category);

    MenuCategory updateCategory(Long categoryId, MenuCategory categoryDetails);

    void deleteCategory(Long categoryId);

    List<MenuCategory> getCategoriesByRestaurant(Long restaurantId);

    // Item Methods
    MenuItem addMenuItem(MenuItem menuItem);

    MenuItem updateMenuItem(Long itemId, MenuItem itemDetails);

    void deleteMenuItem(Long itemId);

    MenuItem getItemById(Long itemId);

    List<MenuItem> getMenuByRestaurant(Long restaurantId);

    List<MenuItem> getItemsByCategory(Long categoryId);

    MenuItem toggleAvailability(Long itemId, boolean isAvailable);

    List<MenuItem> searchMenuItems(String query, Long restaurantId);

    List<MenuItem> getVegItems(Long restaurantId);
}
