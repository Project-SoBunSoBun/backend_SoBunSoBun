package com.sobunsobun.backend.application.auth;

import com.sobunsobun.backend.domain.Role;
import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.dto.auth.AuthResponse;
import com.sobunsobun.backend.infrastructure.oauth.KakaoOAuthClient;
import com.sobunsobun.backend.repository.UserRepository;
import com.sobunsobun.backend.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoOAuthClient kakao;
    private final UserRepository users;
    private final JwtTokenProvider jwt;

    // 요구하신 정책: 액세스 30분, 리프레시 60일
    private static final long ACCESS_TTL  = 30L * 60 * 1000;           // 30분
    private static final long REFRESH_TTL = 60L * 24 * 60 * 60 * 1000; // 60일

    public AuthResponse loginWithKakaoToken(String kakaoAccessToken) {
        var kakaoUser = kakao.getUserInfo(kakaoAccessToken).block();
        if (kakaoUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "카카오 사용자 조회 실패");
        }

        String oauthId = String.valueOf(kakaoUser.getId());
        String email = kakaoUser.getKakao_account() != null ? kakaoUser.getKakao_account().getEmail() : null;
        String nickname = null;
        String profileImageUrl = null;

        if (kakaoUser.getKakao_account() != null && kakaoUser.getKakao_account().getProfile() != null) {
            nickname = kakaoUser.getKakao_account().getProfile().getNickname();
            profileImageUrl = kakaoUser.getKakao_account().getProfile().getProfile_image_url();
        }

        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "카카오 이메일 동의가 필요합니다.");
        }

        var userOpt = users.findByEmail(email);
        boolean isNew = false;
        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
            if (user.getOauthId() == null || !user.getOauthId().equals(oauthId)) user.setOauthId(oauthId);
            if (nickname != null) user.setNickname(nickname);
            if (profileImageUrl != null) user.setProfileImageUrl(profileImageUrl);
            users.save(user);
        } else {
            isNew = true;
            user = User.builder()
                    .email(email)
                    .nickname(nickname != null ? nickname : "사용자")
                    .oauthId(oauthId)
                    .profileImageUrl(profileImageUrl)
                    .role(Role.USER)
                    .build();
            users.save(user);
        }

        // ✅ 토큰 생성
        String access  = jwt.createAccessToken(String.valueOf(user.getId()), user.getRole().name(), ACCESS_TTL);
        String refresh = jwt.createRefreshToken(String.valueOf(user.getId()), REFRESH_TTL);

        // ✅ 만든 토큰에서 exp(만료시각)를 정확히 추출 (초 단위)
        Claims accessClaims  = jwt.parse(access).getBody();
        Claims refreshClaims = jwt.parse(refresh).getBody();
        long accessExpSec  = accessClaims.getExpiration().getTime()  / 1000L;
        long refreshExpSec = refreshClaims.getExpiration().getTime() / 1000L;

        return AuthResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .user(AuthResponse.UserSummary.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .profileImageUrl(user.getProfileImageUrl())
                        .role(user.getRole())
                        .build())
                .accessTokenExpiresAt(accessExpSec)
                .refreshTokenExpiresAt(refreshExpSec)
                .build();
    }

    public AuthResponse refreshTokens(String refreshToken) {
        try {
            var claims = jwt.parse(refreshToken).getBody();
            Long userId = Long.valueOf(claims.getSubject());

            var user = users.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 없음"));

            String access  = jwt.createAccessToken(String.valueOf(user.getId()), user.getRole().name(), ACCESS_TTL);
            String newRef  = jwt.createRefreshToken(String.valueOf(user.getId()), REFRESH_TTL);

            // ✅ 새 토큰들의 만료시각(초) 읽어서 내려주기
            long accessExpSec  = jwt.parse(access).getBody().getExpiration().getTime() / 1000L;
            long refreshExpSec = jwt.parse(newRef).getBody().getExpiration().getTime() / 1000L;

            return AuthResponse.builder()
                    .accessToken(access)
                    .refreshToken(newRef)
                    .user(AuthResponse.UserSummary.builder()
                            .id(user.getId())
                            .email(user.getEmail())
                            .nickname(user.getNickname())
                            .profileImageUrl(user.getProfileImageUrl())
                            .role(user.getRole())
                            .build())
                    .accessTokenExpiresAt(accessExpSec)
                    .refreshTokenExpiresAt(refreshExpSec)
                    .build();

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다.");
        }
    }
}
