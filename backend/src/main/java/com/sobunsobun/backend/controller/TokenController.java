package com.sobunsobun.backend.controller;

import com.sobunsobun.backend.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.sobunsobun.backend.controller.dto.TokenPairResponse;
// (선택) import com.sobunsobun.backend.controller.dto.RefreshRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Validated
public class TokenController {

    private final JwtService jwt;

    public TokenController(JwtService jwt) {
        this.jwt = jwt;
    }

    @Operation(summary = "refresh 토큰으로 access 토큰 재발급(리프레시 회전)")
    @ApiResponse(responseCode = "200",
            description = "새 access/refresh 토큰 페어",
            content = @Content(schema = @Schema(implementation = TokenPairResponse.class)))
    @PostMapping(value = "/token/refresh", consumes = "application/json", produces = "application/json")
    public ResponseEntity<TokenPairResponse> refresh(@RequestBody @Valid RefreshReq req) {
        try {
            Jws<io.jsonwebtoken.Claims> jws = jwt.parse(req.getRefreshToken());
            Claims c = jws.getBody();
            if (!jwt.isRefreshToken(c)) {
                return ResponseEntity.badRequest().build();
            }
            long userId = Long.parseLong(c.getSubject());
            String role = (String) c.get("role");
            String provider = (String) c.get("provider");
            String nickname = (String) c.get("nickname");

            String newAccess = jwt.issueAccess(userId, role, provider, nickname);
            String newRefresh = jwt.issueRefresh(userId, role, provider, nickname);

            return ResponseEntity.ok(TokenPairResponse.builder()
                    .accessToken(newAccess)
                    .refreshToken(newRefresh)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }

    @Data
    public static class RefreshReq {
        @NotBlank
        private String refreshToken;
    }
}

