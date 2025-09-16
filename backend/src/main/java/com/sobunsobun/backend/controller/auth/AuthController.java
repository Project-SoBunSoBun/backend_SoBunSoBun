package com.sobunsobun.backend.controller.auth;

import com.sobunsobun.backend.application.auth.AuthService;
import com.sobunsobun.backend.dto.auth.AuthResponse;
import com.sobunsobun.backend.dto.auth.KakaoTokenLoginRequest;
import com.sobunsobun.backend.dto.auth.RefreshTokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    @Operation(summary = "iOS: 카카오 토큰 로그인/회원가입 처리")
    @PostMapping("/login/kakao-token")
    public ResponseEntity<AuthResponse> loginByKakaoToken(@RequestBody @Validated KakaoTokenLoginRequest req) {
        return ResponseEntity.ok(auth.loginWithKakaoToken(req.getAccessToken()));
    }

    @Operation(summary = "리프레시 토큰으로 액세스/리프레시 재발급")
    @PostMapping("/token/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody @Validated RefreshTokenRequest req) {
        return ResponseEntity.ok(auth.refreshTokens(req.getRefreshToken()));
    }
}
