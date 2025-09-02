package com.sobunsobun.backend.infrastructure.oauth;

import com.sobunsobun.backend.dto.auth.KakaoTokenResponse;
import com.sobunsobun.backend.dto.auth.KakaoUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClientImpl implements OAuthClient {

    @Value("${oauth2.kakao.client-id}")     private String clientId;
    @Value("${oauth2.kakao.client-secret:}")private String clientSecret;
    @Value("${oauth2.kakao.authorize-uri:https://kauth.kakao.com/oauth/authorize}")
    private String authorizeUri;
    @Value("${oauth2.kakao.token-uri:https://kauth.kakao.com/oauth/token}")
    private String tokenUri;
    @Value("${oauth2.kakao.userinfo-uri:https://kapi.kakao.com/v2/user/me}")
    private String userInfoUri;

    private final RestTemplate rest = new RestTemplate();

    @Override public OAuthProvider provider() { return OAuthProvider.KAKAO; }

    @Override
    public String buildAuthorizeUrl(String redirectUri, String stateB64) {
        return authorizeUri
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&state=" + stateB64;
    }

    @Override
    public String getAccessToken(String code, String redirectUri) {
        var body = new LinkedMultiValueMap<String, String>();
        body.add("grant_type","authorization_code");
        body.add("client_id", clientId);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        if(clientSecret!=null && !clientSecret.isBlank()) body.add("client_secret", clientSecret);

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        KakaoTokenResponse rsp = rest.postForEntity(tokenUri, new HttpEntity<>(body, h), KakaoTokenResponse.class).getBody();
        if (rsp==null || rsp.getAccessToken()==null) throw new IllegalStateException("kakao token null");
        return rsp.getAccessToken();
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<KakaoUserResponse> resp = new RestTemplate().exchange(
                userInfoUri,
                HttpMethod.GET,
                new HttpEntity<>(headers),   // 바디 없음, 헤더만
                KakaoUserResponse.class
        );
        KakaoUserResponse u = resp.getBody();
        if (u == null) throw new IllegalStateException("kakao user null");

        String email = (u.getKakao_account()!=null)? u.getKakao_account().getEmail(): null;
        String nickname = (u.getKakao_account()!=null && u.getKakao_account().getProfile()!=null)
                ? u.getKakao_account().getProfile().getNickname() : null;
        String img = (u.getKakao_account()!=null && u.getKakao_account().getProfile()!=null)
                ? u.getKakao_account().getProfile().getProfileImageUrl() : null;

        return new OAuthUserInfo("kakao:"+u.getId(), email, nickname, img);
    }

}
