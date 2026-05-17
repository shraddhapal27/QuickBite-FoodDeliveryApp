package com.quickbite.restaurant.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.restaurant.entity.Restaurant;
import com.quickbite.restaurant.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RestaurantResource Controller Tests")
class RestaurantResourceTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RestaurantService restaurantService;

    @InjectMocks
    private RestaurantResource restaurantResource;

    private Restaurant sample;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(restaurantResource).build();
        sample = Restaurant.builder()
                .restaurantId(1L).ownerId(10L).name("Test Cafe")
                .cuisine("Indian").city("Mumbai").isOpen(true).isApproved(true)
                .avgRating(4.5).build();
    }

    @Test
    @DisplayName("POST /restaurants – 201 CREATED")
    void register() throws Exception {
        when(restaurantService.registerRestaurant(any())).thenReturn(sample);
        mockMvc.perform(post("/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sample)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Cafe"));
    }

    @Test
    @DisplayName("GET /restaurants – 200 OK list")
    void getAll() throws Exception {
        when(restaurantService.getAll()).thenReturn(List.of(sample));
        mockMvc.perform(get("/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /restaurants/{id} – 200 OK")
    void getById() throws Exception {
        when(restaurantService.getById(1L)).thenReturn(sample);
        mockMvc.perform(get("/restaurants/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantId").value(1));
    }

    @Test
    @DisplayName("GET /restaurants/owner/{ownerId} – 200 OK")
    void getByOwner() throws Exception {
        when(restaurantService.getByOwner(10L)).thenReturn(List.of(sample));
        mockMvc.perform(get("/restaurants/owner/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ownerId").value(10));
    }

    @Test
    @DisplayName("GET /restaurants/cuisine/{cuisine} – 200 OK")
    void getByCuisine() throws Exception {
        when(restaurantService.getByCuisine("Indian")).thenReturn(List.of(sample));
        mockMvc.perform(get("/restaurants/cuisine/Indian"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /restaurants/city/{city} – 200 OK")
    void getByCity() throws Exception {
        when(restaurantService.getByCity("Mumbai")).thenReturn(List.of(sample));
        mockMvc.perform(get("/restaurants/city/Mumbai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /restaurants/nearby – 200 OK")
    void getNearby() throws Exception {
        when(restaurantService.getNearby(19.0, 72.8, 10.0)).thenReturn(List.of(sample));
        mockMvc.perform(get("/restaurants/nearby")
                        .param("lat", "19.0").param("lng", "72.8").param("distanceKm", "10.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /restaurants/search – 200 OK")
    void search() throws Exception {
        when(restaurantService.searchRestaurants("test")).thenReturn(List.of(sample));
        mockMvc.perform(get("/restaurants/search").param("query", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Cafe"));
    }

    @Test
    @DisplayName("PUT /restaurants/{id} – 200 OK")
    void update() throws Exception {
        Restaurant updated = Restaurant.builder().name("Updated").build();
        sample.setName("Updated");
        when(restaurantService.updateRestaurant(eq(1L), any())).thenReturn(sample);

        mockMvc.perform(put("/restaurants/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    @DisplayName("PUT /restaurants/{id}/approve – 200 OK")
    void approve() throws Exception {
        when(restaurantService.approveRestaurant(1L, true)).thenReturn(sample);
        mockMvc.perform(put("/restaurants/1/approve").param("isApproved", "true"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /restaurants/{id}/toggle-open – 200 OK")
    void toggleOpen() throws Exception {
        when(restaurantService.toggleOpen(1L, true)).thenReturn(sample);
        mockMvc.perform(put("/restaurants/1/toggle-open").param("isOpen", "true"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /restaurants/{id}/rating – 200 OK")
    void updateRating() throws Exception {
        sample.setAvgRating(4.8);
        when(restaurantService.updateRating(1L, 4.8)).thenReturn(sample);
        mockMvc.perform(put("/restaurants/1/rating").param("rating", "4.8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avgRating").value(4.8));
    }

    @Test
    @DisplayName("DELETE /restaurants/{id} – 204 NO CONTENT")
    void deleteRestaurant() throws Exception {
        doNothing().when(restaurantService).deleteRestaurant(1L);
        mockMvc.perform(delete("/restaurants/1"))
                .andExpect(status().isNoContent());
    }
}
