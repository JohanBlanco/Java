package com.gymplatform.controller;

import com.gymplatform.dto.LoginRequest;
import com.gymplatform.dto.AuthResponse;
import com.gymplatform.dto.UserCreateRequest;
import com.gymplatform.dto.UserResponse;
import com.gymplatform.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticación", description = "Login y registro de miembros")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register/{organizationId}")
    public UserResponse register(@PathVariable Long organizationId, @Valid @RequestBody UserCreateRequest request) {
        return authService.registerMember(request, organizationId);
    }
}
