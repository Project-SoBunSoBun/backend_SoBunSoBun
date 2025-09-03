package com.sobunsobun.backend.controller;

import com.sobunsobun.backend.security.jwt.JwtAuthFilter;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;


import java.util.Map;

@RestController
@SecurityRequirement(name = "BearerAuth") // ✅ /me에만 자물쇠
public class MeController {

    @Operation(summary = "현재 로그인 사용자 정보")
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtAuthFilter.UserPrincipal p)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(Map.of(
                "id", p.userId,
                "provider", p.provider,
                "nickname", p.nickname,
                "role", p.role
        ));
    }
}
