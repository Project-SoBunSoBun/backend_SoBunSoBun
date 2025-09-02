package com.sobunsobun.backend.application.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sobunsobun.backend.application.user.UserService;
import com.sobunsobun.backend.dto.auth.AuthResponse;
import com.sobunsobun.backend.infrastructure.oauth.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ObjectMapper om;
    private final TempAuthCodeStore authCodes;
    private final UserService users;      // 네 레포에 이미 있음 (upsertFromOAuth 같은 메서드 있으면 사용)
    private final TokenService tokens;    // 네 레포에 이미 있음 (JWT 발급)
    private final Map<OAuthProvider, OAuthClient> clients; // 구성은 아래 @Configuration 참고

    @Value("${oauth2.redirect.kakao}") private String kakaoRedirectUri;
    @Value("${oauth2.redirect.google:}") private String googleRedirectUri;
    @Value("${oauth2.redirect.apple:}")  private String appleRedirectUri;

    private String redirectUriOf(OAuthProvider p){
        return switch (p){
            case KAKAO -> kakaoRedirectUri;
            case GOOGLE -> googleRedirectUri;
            case APPLE -> appleRedirectUri;
        };
    }

    /** 1) 로그인 시작: authorize URL 생성 */
    public String createAuthorizeUrl(OAuthProvider provider, String returnTo, String platform){
        if(!StringUtils.hasText(returnTo)) throw new IllegalArgumentException("returnTo required");
        String state = OAuthState.encode(returnTo, platform, om);
        return clients.get(provider).buildAuthorizeUrl(redirectUriOf(provider), state);
    }

    /** 2) 콜백 성공: provider토큰/유저 → 우리 JWT → authCode 저장 후 반환 */
    public String onCallbackSuccess(OAuthProvider provider, String code){
        OAuthClient c = clients.get(provider);
        String accessToken = c.getAccessToken(code, redirectUriOf(provider));
        OAuthUserInfo info = c.getUserInfo(accessToken);

        var upsert = users.upsertFromOAuth(info.providerId(), info.email(), info.nickname(), info.profileImage());
        String at = tokens.issueAccess(upsert.user());
        String rt = tokens.issueRefresh(upsert.user());

        String authCode = UUID.randomUUID().toString();
        try{
            String json = om.writeValueAsString(AuthResponse.builder()
                    .accessToken(at).refreshToken(rt).isNewUser(upsert.isNew()).build());
            authCodes.save(authCode, json, Duration.ofMinutes(5));
            return authCode;
        }catch (Exception e){ throw new IllegalStateException("authCode 저장 실패", e); }
    }

    /** 3) 1회용 코드 → JWT 교환 */
    public AuthResponse exchangeAuthCode(String code){
        try{
            String json = authCodes.take(code);
            if (json == null) throw new IllegalArgumentException("잘못되었거나 만료된 코드");
            return om.readValue(json, AuthResponse.class);
        }catch (IllegalArgumentException e){ throw e;
        }catch (Exception e){ throw new IllegalStateException("authCode 파싱 실패", e); }
    }

    /** 4) 리다이렉트 URL 생성 */
    public String buildSuccessRedirect(String returnTo, String authCode){
        String sep = returnTo.contains("?") ? "&" : "?";
        return returnTo + sep + "authCode=" + enc(authCode);
    }
    public String buildFailureRedirect(String loginPage, String returnTo, String reason){
        return loginPage + "?reason=" + enc(reason) + "&redirect=" + enc(returnTo);
    }
    private String enc(String s){ return URLEncoder.encode(s, StandardCharsets.UTF_8); }


}
