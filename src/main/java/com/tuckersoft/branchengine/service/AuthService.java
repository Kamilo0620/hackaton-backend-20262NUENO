package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.config.JwtService;
import com.tuckersoft.branchengine.dto.Dtos.*;
import com.tuckersoft.branchengine.exception.ConflictException;
import com.tuckersoft.branchengine.model.User;
import com.tuckersoft.branchengine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException("El correo ya se encuentra registrado");
        }

        User user = User.builder()
                .email(req.email())
                .displayName(req.displayName())
                .password(passwordEncoder.encode(req.password()))
                .role("ROLE_USER")
                .createdAt(Instant.now())
                .build();
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, "Bearer", user.getEmail(), user.getDisplayName(), user.getRole());
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        User user = userRepository.findByEmail(req.email()).orElseThrow();
        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, "Bearer", user.getEmail(), user.getDisplayName(), user.getRole());
    }
}
