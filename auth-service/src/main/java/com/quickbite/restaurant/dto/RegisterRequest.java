package com.quickbite.restaurant.dto;

import com.quickbite.restaurant.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    // Optional — defaults to CUSTOMER in AuthServiceImpl
    private Role role;

    private String phone;
}
