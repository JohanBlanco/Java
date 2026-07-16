package com.gymplatform.dto;

public record UserCreateResponse(
        UserResponse user,
        String registrationFormWhatsappUrl
) {}
