package com.quickbite.delivery.service.impl;

import com.quickbite.delivery.dto.AgentRegistrationDTO;
import com.quickbite.delivery.dto.LocationUpdateDTO;
import com.quickbite.delivery.dto.RatingUpdateDTO;
import com.quickbite.delivery.entity.DeliveryAgent;
import com.quickbite.delivery.entity.VehicleType;
import com.quickbite.delivery.repository.DeliveryRepository;
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
@DisplayName("DeliveryServiceImpl Unit Tests")
class DeliveryServiceImplTest {

    @Mock private DeliveryRepository deliveryRepository;
    @InjectMocks private DeliveryServiceImpl deliveryService;

    private DeliveryAgent sampleAgent;

    @BeforeEach
    void setUp() {
        sampleAgent = DeliveryAgent.builder()
                .agentId(1L).userId(100L).fullName("Ravi Kumar")
                .phone("9876543210").vehicleType(VehicleType.BIKE)
                .vehicleNumber("KA-01-1234")
                .isAvailable(true).isVerified(true)
                .avgRating(4.5).totalDeliveries(50).totalRatings(40)
                .currentLatitude(12.97).currentLongitude(77.59)
                .currentOrderId(null)
                .build();
    }

    // ── Registration ──

    @Nested
    @DisplayName("Register Agent")
    class RegisterTests {

        @Test
        @DisplayName("registerAgent – success")
        void register_success() {
            AgentRegistrationDTO dto = new AgentRegistrationDTO();
            dto.setUserId(200L); dto.setFullName("New Agent");
            dto.setPhone("1111111111"); dto.setVehicleType(VehicleType.SCOOTER);
            dto.setVehicleNumber("MH-02-5678");

            when(deliveryRepository.findByUserId(200L)).thenReturn(Optional.empty());
            when(deliveryRepository.findByPhone("1111111111")).thenReturn(Optional.empty());
            when(deliveryRepository.save(any(DeliveryAgent.class))).thenAnswer(i -> {
                DeliveryAgent a = i.getArgument(0); a.setAgentId(2L); return a;
            });

            DeliveryAgent result = deliveryService.registerAgent(dto);
            assertThat(result.getFullName()).isEqualTo("New Agent");
            assertThat(result.getIsVerified()).isFalse();
            assertThat(result.getIsAvailable()).isFalse();
        }

        @Test
        @DisplayName("registerAgent – duplicate userId throws")
        void register_duplicateUserId() {
            AgentRegistrationDTO dto = new AgentRegistrationDTO();
            dto.setUserId(100L); dto.setPhone("9999999999");
            when(deliveryRepository.findByUserId(100L)).thenReturn(Optional.of(sampleAgent));

            assertThatThrownBy(() -> deliveryService.registerAgent(dto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already registered");
        }

        @Test
        @DisplayName("registerAgent – duplicate phone throws")
        void register_duplicatePhone() {
            AgentRegistrationDTO dto = new AgentRegistrationDTO();
            dto.setUserId(200L); dto.setPhone("9876543210");
            when(deliveryRepository.findByUserId(200L)).thenReturn(Optional.empty());
            when(deliveryRepository.findByPhone("9876543210")).thenReturn(Optional.of(sampleAgent));

            assertThatThrownBy(() -> deliveryService.registerAgent(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Phone number already registered");
        }
    }

    // ── Get Agent ──

    @Nested
    @DisplayName("Get Agent")
    class GetAgentTests {

        @Test
        @DisplayName("getAgentById – success")
        void getById_success() {
            when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(sampleAgent));
            assertThat(deliveryService.getAgentById(1L).getFullName()).isEqualTo("Ravi Kumar");
        }

        @Test
        @DisplayName("getAgentById – not found throws")
        void getById_notFound() {
            when(deliveryRepository.findByAgentId(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> deliveryService.getAgentById(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Agent not found");
        }

        @Test
        @DisplayName("getAgentByUserId – success")
        void getByUserId() {
            when(deliveryRepository.findByUserId(100L)).thenReturn(Optional.of(sampleAgent));
            assertThat(deliveryService.getAgentByUserId(100L).getAgentId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("getAllAgents – returns all")
        void getAllAgents() {
            when(deliveryRepository.findAll()).thenReturn(List.of(sampleAgent));
            assertThat(deliveryService.getAllAgents()).hasSize(1);
        }
    }

    // ── Admin Operations ──

    @Test
    @DisplayName("verifyAgent – sets isVerified to true")
    void verifyAgent() {
        sampleAgent.setIsVerified(false);
        when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(sampleAgent));
        when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DeliveryAgent result = deliveryService.verifyAgent(1L);
        assertThat(result.getIsVerified()).isTrue();
    }

    @Test
    @DisplayName("deleteAgent – deletes existing agent")
    void deleteAgent_success() {
        when(deliveryRepository.existsById(1L)).thenReturn(true);
        deliveryService.deleteAgent(1L);
        verify(deliveryRepository).deleteByAgentId(1L);
    }

    @Test
    @DisplayName("deleteAgent – not found throws")
    void deleteAgent_notFound() {
        when(deliveryRepository.existsById(999L)).thenReturn(false);
        assertThatThrownBy(() -> deliveryService.deleteAgent(999L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Availability & Location ──

    @Nested
    @DisplayName("Availability & Location")
    class AvailabilityTests {

        @Test
        @DisplayName("setAvailability – verified agent can go online")
        void setAvailability_verified() {
            when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(sampleAgent));
            when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            DeliveryAgent result = deliveryService.setAvailability(1L, true);
            assertThat(result.getIsAvailable()).isTrue();
        }

        @Test
        @DisplayName("setAvailability – unverified agent cannot go online")
        void setAvailability_unverified() {
            sampleAgent.setIsVerified(false);
            when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(sampleAgent));

            assertThatThrownBy(() -> deliveryService.setAvailability(1L, true))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("verified");
        }

        @Test
        @DisplayName("updateLocation – updates GPS coordinates")
        void updateLocation() {
            LocationUpdateDTO loc = new LocationUpdateDTO();
            loc.setLatitude(13.0); loc.setLongitude(77.6);
            when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(sampleAgent));
            when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            DeliveryAgent result = deliveryService.updateLocation(1L, loc);
            assertThat(result.getCurrentLatitude()).isEqualTo(13.0);
            assertThat(result.getCurrentLongitude()).isEqualTo(77.6);
        }

        @Test
        @DisplayName("updateLocation – null coordinates throw")
        void updateLocation_nullCoords() {
            LocationUpdateDTO loc = new LocationUpdateDTO();
            loc.setLatitude(null); loc.setLongitude(null);
            assertThatThrownBy(() -> deliveryService.updateLocation(1L, loc))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null");
        }
    }

    // ── Geo-Proximity ──

    @Test
    @DisplayName("getNearbyAgents – delegates to repository")
    void getNearbyAgents() {
        when(deliveryRepository.findNearbyAgents(12.97, 77.59, 5.0))
                .thenReturn(List.of(sampleAgent));
        assertThat(deliveryService.getNearbyAgents(12.97, 77.59, 5.0)).hasSize(1);
    }

    // ── Order Assignment ──

    @Nested
    @DisplayName("Order Assignment")
    class OrderAssignmentTests {

        @Test
        @DisplayName("assignOrder – success for available verified agent")
        void assignOrder_success() {
            when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(sampleAgent));
            when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            DeliveryAgent result = deliveryService.assignOrder(1L, 500L);
            assertThat(result.getCurrentOrderId()).isEqualTo(500L);
            assertThat(result.getIsAvailable()).isFalse();
        }

        @Test
        @DisplayName("assignOrder – unverified agent throws")
        void assignOrder_unverified() {
            sampleAgent.setIsVerified(false);
            when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(sampleAgent));

            assertThatThrownBy(() -> deliveryService.assignOrder(1L, 500L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not verified");
        }

        @Test
        @DisplayName("assignOrder – unavailable agent throws")
        void assignOrder_unavailable() {
            sampleAgent.setIsAvailable(false);
            when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(sampleAgent));

            assertThatThrownBy(() -> deliveryService.assignOrder(1L, 500L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not available");
        }

        @Test
        @DisplayName("assignOrder – agent already busy throws")
        void assignOrder_alreadyBusy() {
            sampleAgent.setCurrentOrderId(400L);
            when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(sampleAgent));

            assertThatThrownBy(() -> deliveryService.assignOrder(1L, 500L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already handling");
        }

        @Test
        @DisplayName("completeDelivery – clears order and increments counter")
        void completeDelivery() {
            sampleAgent.setCurrentOrderId(500L);
            when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(sampleAgent));
            when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            DeliveryAgent result = deliveryService.completeDelivery(1L);
            assertThat(result.getCurrentOrderId()).isNull();
            assertThat(result.getTotalDeliveries()).isEqualTo(51);
            assertThat(result.getIsAvailable()).isTrue();
        }

        @Test
        @DisplayName("completeDelivery – no active delivery throws")
        void completeDelivery_noOrder() {
            sampleAgent.setCurrentOrderId(null);
            when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(sampleAgent));

            assertThatThrownBy(() -> deliveryService.completeDelivery(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no active delivery");
        }

        @Test
        @DisplayName("getActiveDeliveries – returns agents with current orders")
        void getActiveDeliveries() {
            sampleAgent.setCurrentOrderId(500L);
            when(deliveryRepository.findByCurrentOrderIdIsNotNull()).thenReturn(List.of(sampleAgent));
            assertThat(deliveryService.getActiveDeliveries()).hasSize(1);
        }
    }

    // ── Ratings ──

    @Nested
    @DisplayName("Ratings")
    class RatingTests {

        @Test
        @DisplayName("updateRating – computes running average")
        void updateRating_success() {
            // Current: avgRating=4.5, totalRatings=40
            // New rating: 5 -> newAvg = (4.5*40 + 5) / 41 = 185/41 ≈ 4.51
            RatingUpdateDTO dto = new RatingUpdateDTO();
            dto.setNewRating(5.0);
            when(deliveryRepository.findByAgentId(1L)).thenReturn(Optional.of(sampleAgent));
            when(deliveryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            DeliveryAgent result = deliveryService.updateRating(1L, dto);
            assertThat(result.getAvgRating()).isEqualTo(4.51);
            assertThat(result.getTotalRatings()).isEqualTo(41);
        }

        @Test
        @DisplayName("updateRating – rating below 1 throws")
        void updateRating_tooLow() {
            RatingUpdateDTO dto = new RatingUpdateDTO();
            dto.setNewRating(0.0);
            assertThatThrownBy(() -> deliveryService.updateRating(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("between 1 and 5");
        }

        @Test
        @DisplayName("updateRating – rating above 5 throws")
        void updateRating_tooHigh() {
            RatingUpdateDTO dto = new RatingUpdateDTO();
            dto.setNewRating(6.0);
            assertThatThrownBy(() -> deliveryService.updateRating(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("updateRating – null rating throws")
        void updateRating_null() {
            RatingUpdateDTO dto = new RatingUpdateDTO();
            dto.setNewRating(null);
            assertThatThrownBy(() -> deliveryService.updateRating(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
