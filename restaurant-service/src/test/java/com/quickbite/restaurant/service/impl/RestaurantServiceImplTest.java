package com.quickbite.restaurant.service.impl;

import com.quickbite.restaurant.entity.Restaurant;
import com.quickbite.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RestaurantServiceImpl Unit Tests")
class RestaurantServiceImplTest {

    @Mock private RestaurantRepository restaurantRepository;
    @InjectMocks private RestaurantServiceImpl restaurantService;

    private Restaurant sampleRestaurant;

    @BeforeEach
    void setUp() {
        sampleRestaurant = Restaurant.builder()
                .restaurantId(1L).ownerId(10L).name("QuickBite Cafe")
                .description("Best cafe in town").cuisine("Indian")
                .address("456 Park Ave").city("Mumbai")
                .latitude(19.076).longitude(72.877)
                .phone("022-12345678").avgRating(4.5)
                .isOpen(true).isApproved(true)
                .deliveryRadius(5.0).minOrderAmount(100.0)
                .estimatedDeliveryMin(30)
                .build();
    }

    // ── Get All ──

    @Test
    @DisplayName("getAll – returns all restaurants")
    void getAll() {
        when(restaurantRepository.findAll()).thenReturn(List.of(sampleRestaurant));
        assertThat(restaurantService.getAll()).hasSize(1);
    }

    // ── Register ──

    @Nested
    @DisplayName("Register Restaurant")
    class RegisterTests {

        @Test
        @DisplayName("registerRestaurant – sets approved and closed by default")
        void register_success() {
            when(restaurantRepository.save(any(Restaurant.class)))
                    .thenAnswer(i -> { Restaurant r = i.getArgument(0); r.setRestaurantId(2L); return r; });

            Restaurant input = Restaurant.builder().name("New Place").ownerId(10L).build();
            Restaurant result = restaurantService.registerRestaurant(input);

            assertThat(result.getIsApproved()).isTrue();
            assertThat(result.getIsOpen()).isFalse();
            verify(restaurantRepository).save(input);
        }
    }

    // ── Get By ID ──

    @Nested
    @DisplayName("Get By ID")
    class GetByIdTests {

        @Test
        @DisplayName("getById – success")
        void getById_success() {
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(sampleRestaurant));
            Restaurant r = restaurantService.getById(1L);
            assertThat(r.getName()).isEqualTo("QuickBite Cafe");
        }

        @Test
        @DisplayName("getById – not found throws")
        void getById_notFound() {
            when(restaurantRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> restaurantService.getById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ── Queries ──

    @Nested
    @DisplayName("Query Operations")
    class QueryTests {

        @Test
        @DisplayName("getByOwner – returns owner's restaurants")
        void getByOwner() {
            when(restaurantRepository.findByOwnerId(10L)).thenReturn(List.of(sampleRestaurant));
            assertThat(restaurantService.getByOwner(10L)).hasSize(1);
        }

        @Test
        @DisplayName("getByCuisine – returns matching cuisine")
        void getByCuisine() {
            when(restaurantRepository.findByCuisine("Indian")).thenReturn(List.of(sampleRestaurant));
            assertThat(restaurantService.getByCuisine("Indian")).hasSize(1);
        }

        @Test
        @DisplayName("getByCity – returns matching city")
        void getByCity() {
            when(restaurantRepository.findByCity("Mumbai")).thenReturn(List.of(sampleRestaurant));
            assertThat(restaurantService.getByCity("Mumbai")).hasSize(1);
        }

        @Test
        @DisplayName("getNearby – delegates to repository")
        void getNearby() {
            when(restaurantRepository.findNearby(19.0, 72.8, 5.0))
                    .thenReturn(List.of(sampleRestaurant));
            assertThat(restaurantService.getNearby(19.0, 72.8, 5.0)).hasSize(1);
        }

        @Test
        @DisplayName("searchRestaurants – searches by name")
        void search() {
            when(restaurantRepository.findByNameContainingIgnoreCase("quick"))
                    .thenReturn(List.of(sampleRestaurant));
            assertThat(restaurantService.searchRestaurants("quick")).hasSize(1);
        }
    }

    // ── Update ──

    @Test
    @DisplayName("updateRestaurant – updates all fields")
    void update_success() {
        Restaurant updated = Restaurant.builder()
                .name("Updated Cafe").description("New desc").cuisine("Chinese")
                .address("789 New St").city("Delhi").latitude(28.6).longitude(77.2)
                .phone("011-999").deliveryRadius(10.0).minOrderAmount(200.0)
                .estimatedDeliveryMin(40).build();

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(sampleRestaurant));
        when(restaurantRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Restaurant result = restaurantService.updateRestaurant(1L, updated);
        assertThat(result.getName()).isEqualTo("Updated Cafe");
        assertThat(result.getCuisine()).isEqualTo("Chinese");
        assertThat(result.getCity()).isEqualTo("Delhi");
    }

    // ── Approve ──

    @Nested
    @DisplayName("Approve Restaurant")
    class ApproveTests {

        @Test
        @DisplayName("approveRestaurant – sets approved flag")
        void approve_true() {
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(sampleRestaurant));
            when(restaurantRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            Restaurant r = restaurantService.approveRestaurant(1L, true);
            assertThat(r.getIsApproved()).isTrue();
        }

        @Test
        @DisplayName("approveRestaurant – can revoke approval")
        void approve_false() {
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(sampleRestaurant));
            when(restaurantRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            Restaurant r = restaurantService.approveRestaurant(1L, false);
            assertThat(r.getIsApproved()).isFalse();
        }
    }

    // ── Toggle Open ──

    @Nested
    @DisplayName("Toggle Open")
    class ToggleOpenTests {

        @Test
        @DisplayName("toggleOpen – opens approved restaurant")
        void toggleOpen_approved() {
            sampleRestaurant.setIsApproved(true);
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(sampleRestaurant));
            when(restaurantRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            Restaurant r = restaurantService.toggleOpen(1L, true);
            assertThat(r.getIsOpen()).isTrue();
        }

        @Test
        @DisplayName("toggleOpen – unapproved restaurant cannot open")
        void toggleOpen_unapproved() {
            sampleRestaurant.setIsApproved(false);
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(sampleRestaurant));
            assertThatThrownBy(() -> restaurantService.toggleOpen(1L, true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("unapproved");
        }

        @Test
        @DisplayName("toggleOpen – can close any restaurant")
        void toggleOpen_close() {
            when(restaurantRepository.findById(1L)).thenReturn(Optional.of(sampleRestaurant));
            when(restaurantRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            Restaurant r = restaurantService.toggleOpen(1L, false);
            assertThat(r.getIsOpen()).isFalse();
        }
    }

    // ── Delete ──

    @Test
    @DisplayName("deleteRestaurant – deletes existing restaurant")
    void delete_success() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(sampleRestaurant));
        restaurantService.deleteRestaurant(1L);
        verify(restaurantRepository).delete(sampleRestaurant);
    }

    @Test
    @DisplayName("deleteRestaurant – not found throws")
    void delete_notFound() {
        when(restaurantRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> restaurantService.deleteRestaurant(999L))
                .isInstanceOf(RuntimeException.class);
    }

    // ── Update Rating ──

    @Test
    @DisplayName("updateRating – updates average rating")
    void updateRating() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(sampleRestaurant));
        when(restaurantRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Restaurant r = restaurantService.updateRating(1L, 4.8);
        assertThat(r.getAvgRating()).isEqualTo(4.8);
    }
}
