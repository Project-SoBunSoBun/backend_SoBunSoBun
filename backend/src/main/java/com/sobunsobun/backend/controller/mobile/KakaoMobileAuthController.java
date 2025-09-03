package com.sobunsobun.backend.controller.mobile;

import com.sobunsobun.backend.infrastructure.oauth.KakaoOAuthClient;
import com.sobunsobun.backend.infrastructure.oauth.KakaoTokenResponse;
import com.sobunsobun.backend.infrastructure.oauth.KakaoUserResponse;
import com.sobunsobun.backend.security.jwt.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.sobunsobun.backend.controller.dto.AuthResponse;
import com.sobunsobun.backend.controller.dto.AuthResponse.KakaoTokenDto;
import com.sobunsobun.backend.controller.dto.AuthResponse.UserDto;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@RestController("mobileKakaoAuthController")
@RequestMapping("/auth")
@Validated
public class KakaoMobileAuthController {

    private final KakaoOAuthClient kakao;
    private final JwtService jwt;

    public KakaoMobileAuthController(KakaoOAuthClient kakao, JwtService jwt) {
        this.kakao = kakao;
        this.jwt = jwt;
    }

    @Value("${oauth.kakao.redirect-uri}")
    private String defaultRedirectUri;

    @Operation(summary = "모바일 전용: 카카오 authorization_code → 우리 JWT(JSON) 교환")
    @ApiResponse(responseCode = "200",
            description = "토큰 페어 및 사용자 정보",
            content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @PostMapping(value = "/kakao/token", consumes = "application/json", produces = "application/json")
    public ResponseEntity<AuthResponse> exchange(@RequestBody @Valid ExchangeReq req) {

        String redirectUri = (req.getRedirectUri() != null && !req.getRedirectUri().isBlank())
                ? req.getRedirectUri() : defaultRedirectUri;

        KakaoTokenResponse token = kakao.exchangeCodeForToken(req.getCode(), redirectUri);
        KakaoUserResponse userinfo = kakao.fetchUser(token.getAccessToken());

        long userId = userinfo.getId(); // (임시) 운영에서는 DB 매핑 권장
        String nickname = userinfo.getProperties() != null
                ? (String) userinfo.getProperties().getOrDefault("nickname", "사용자")
                : "사용자";

        String accessJwt = jwt.issueAccess(userId, "USER", "KAKAO", nickname);
        String refreshJwt = jwt.issueRefresh(userId, "USER", "KAKAO", nickname);

        AuthResponse body = AuthResponse.builder()
                .accessToken(accessJwt)
                .refreshToken(refreshJwt)
                .newUser(false)
                .user(UserDto.builder()
                        .id(userId)
                        .nickname(nickname)
                        .profileImageUrl(userinfo.getProperties() != null
                                ? (String) userinfo.getProperties().get("profile_image") : null)
                        .provider("KAKAO")
                        .build())
                .kakao(KakaoTokenDto.builder()
                        .accessToken(token.getAccessToken())
                        .refreshToken(token.getRefreshToken())
                        .expiresIn(token.getExpiresIn())
                        .build())
                .build();

        return ResponseEntity.ok(body);
    }



    @Data
    public static class ExchangeReq {
        @NotBlank
        private String code;
        private String state;
        private String redirectUri;
    }
}
