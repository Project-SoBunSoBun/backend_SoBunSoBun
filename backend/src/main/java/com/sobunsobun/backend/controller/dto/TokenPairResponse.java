package com.sobunsobun.backend.controller.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class TokenPairResponse {
    private String accessToken;
    private String refreshToken;
}
