package com.sobunsobun.backend.application.auth;

import com.sobunsobun.backend.application.user.UserService;
import com.sobunsobun.backend.dto.auth.*;
import com.sobunsobun.backend.infrastructure.oauth.KakaoOAuthClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final KakaoOAuthClient kakao;
    private final UserService users;
    private final TokenService tokens;

    public AuthService(KakaoOAuthClient kakao, UserService users, TokenService tokens) {
        this.kakao = kakao; this.users = users; this.tokens = tokens;
    }

    /** 카카오 code -> kakaoAccessToken -> 프로필 -> 가입/로그인 -> JWT */
    @Transactional
    public AuthResponse loginWithKakaoCode(String code){
        KakaoTokenResponse tk = kakao.exchangeCodeForToken(code);
        KakaoUserResponse profile = kakao.getUser(tk.getAccessToken());

        var upsert = users.upsertByKakaoProfile(
                profile.getId(),
                profile.getKakao_account()!=null ? profile.getKakao_account().getEmail() : null,
                profile.getKakao_account()!=null && profile.getKakao_account().getProfile()!=null
                        ? profile.getKakao_account().getProfile().getNickname() : null,
                profile.getKakao_account()!=null && profile.getKakao_account().getProfile()!=null
                        ? profile.getKakao_account().getProfile().getProfileImageUrl() : null
        );

        String access  = tokens.issueAccess(upsert.user());
        String refresh = tokens.issueRefresh(upsert.user());

        return AuthResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .isNewUser(upsert.isNew())
                .build();
    }
}
