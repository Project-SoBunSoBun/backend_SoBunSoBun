package com.sobunsobun.backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KakaoTokenResponse {
    @JsonProperty("access_token")  private String accessToken;
    @JsonProperty("token_type")    private String tokenType;
    @JsonProperty("refresh_token") private String refreshToken;
    @JsonProperty("expires_in")    private Long expiresIn;
    @JsonProperty("scope")         private String scope;
}
