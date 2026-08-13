package com.orbytum.api.controller;

import com.orbytum.api.fachada.AuthFachada;
import com.orbytum.api.models.dto.request.RegisterAdminRequest;
import com.orbytum.api.models.dto.response.AuthResponse;
import com.orbytum.api.models.dto.request.LoginRequest;
import com.orbytum.api.models.dto.request.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthFachada authFachada;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authFachada.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authFachada.register(request));
    }

    @PostMapping("/register-admin")
    public ResponseEntity<AuthResponse> registerAdmin(
            @Valid @RequestBody RegisterAdminRequest request,
            Authentication authentication
    ) {
        String emailUsuarioLogado = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(authFachada.registerAdmin(request, emailUsuarioLogado));
    }
}
