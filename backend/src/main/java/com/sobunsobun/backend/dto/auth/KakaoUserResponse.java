package com.sobunsobun.backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KakaoUserResponse {
    private Long id;
    private KakaoAccount kakao_account;
    private Properties properties;

    @Data public static class KakaoAccount {
        private String email;
        private Profile profile;
        @Data public static class Profile {
            private String nickname;
            @JsonProperty("profile_image_url") private String profileImageUrl;
        }
    }
    @Data public static class Properties {
        private String nickname;
        @JsonProperty("profile_image") private String profileImage;
    }
}
