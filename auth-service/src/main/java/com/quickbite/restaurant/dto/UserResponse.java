package com.quickbite.restaurant.dto;

import com.quickbite.restaurant.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID userId;
    private String fullName;
    private String email;
    private Role role;
    private String phone;
    private String profilePicUrl;
    private boolean isActive;
    private LocalDateTime createdAt;
}
