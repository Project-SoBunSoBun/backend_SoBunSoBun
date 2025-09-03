package com.sobunsobun.backend.infrastructure.oauth;

import lombok.Data;
import java.util.Map;

@Data
public class KakaoUserResponse {
    private Long id;
    private Map<String, Object> kakao_account; // email 등
    private Map<String, Object> properties;    // nickname, profile_image 등
}
