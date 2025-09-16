package com.sobunsobun.backend.controller;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.auth.AuthResponse;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MeController {

    private final UserRepository users;

    @Operation(summary = "내 정보 조회(보호됨)")
    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserSummary> me(Authentication authentication) {
        JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
        User user = users.findById(principal.id()).orElseThrow();
        return ResponseEntity.ok(
                AuthResponse.UserSummary.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .profileImageUrl(user.getProfileImageUrl())
                        .role(user.getRole())
                        .build()
        );
    }
}
