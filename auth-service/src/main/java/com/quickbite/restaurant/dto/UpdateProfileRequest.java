package com.quickbite.restaurant.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String phone;
    private String profilePicUrl;
}
