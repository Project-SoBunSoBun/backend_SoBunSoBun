package com.sobunsobun.backend.controller;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.auth.AuthResponse;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.security.JwtUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class MeController {

    private final UserRepository users;

    @Operation(summary = "내 정보 조회(보호됨)")
    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserSummary> me(Authentication authentication) {
        JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
        User user = users.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "사용자를 찾을 수 없습니다"));
        return ResponseEntity.ok(
                AuthResponse.UserSummary.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .profileImageUrl(user.getProfileImageUrl())
                        .address(user.getAddress())
                        .role(user.getRole())
                        .build()
        );
    }
}
