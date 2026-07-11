package com.gymplatform.dto;

import com.gymplatform.domain.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UserCreateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @Schema(description = "Opcional. Si se omite o está vacío, se usa 12345678")
        String password,
        @NotEmpty @Schema(description = "Roles del usuario: GYM_OWNER (Admin), RECEPTIONIST, INSTRUCTOR, MEMBER")
        List<Role> roles,
        Integer birthYear,
        Integer age,
        String goals,
        String phone,
        Long membershipPackageId
) {}
