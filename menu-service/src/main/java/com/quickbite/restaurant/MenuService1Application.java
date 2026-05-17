package com.quickbite.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import com.quickbite.restaurant.repository.MenuCategoryRepository;
import com.quickbite.restaurant.repository.MenuItemRepository;
import com.quickbite.restaurant.entity.MenuCategory;
import com.quickbite.restaurant.entity.MenuItem;

@SpringBootApplication
public class MenuService1Application {

	public static void main(String[] args) {
		SpringApplication.run(MenuService1Application.class, args);
	}

	@Bean
	public CommandLineRunner seedData(MenuCategoryRepository categoryRepo, MenuItemRepository itemRepo) {
		return args -> {
			if (itemRepo.count() == 0) {
				for (long restId = 1; restId <= 5; restId++) {
					// Seed Categories
					MenuCategory cat1 = new MenuCategory();
					cat1.setRestaurantId(restId);
					cat1.setName("Popular");
					cat1.setDisplayOrder(1);
					cat1 = categoryRepo.save(cat1);

					MenuCategory cat2 = new MenuCategory();
					cat2.setRestaurantId(restId);
					cat2.setName("Main Course");
					cat2.setDisplayOrder(2);
					cat2 = categoryRepo.save(cat2);

					// Seed Items
					MenuItem item1 = new MenuItem();
					item1.setRestaurantId(restId);
					item1.setCategoryId(cat1.getCategoryId());
					item1.setName("Special Burger");
					item1.setDescription("Our signature burger with double patty");
					item1.setPrice(199.0);
					item1.setDiscountedPrice(149.0);
					item1.setImageUrl("https://images.unsplash.com/photo-1568901346375-23c9450c58cd");
					item1.setIsVeg(false);
					item1.setIsAvailable(true);
					itemRepo.save(item1);

					MenuItem item2 = new MenuItem();
					item2.setRestaurantId(restId);
					item2.setCategoryId(cat2.getCategoryId());
					item2.setName("Paneer Butter Masala");
					item2.setDescription("Rich and creamy paneer curry");
					item2.setPrice(250.0);
					item2.setImageUrl("https://images.unsplash.com/photo-1555396273-367ea4eb4db5");
					item2.setIsVeg(true);
					item2.setIsAvailable(true);
					itemRepo.save(item2);
				}
			}
		};
	}
}
