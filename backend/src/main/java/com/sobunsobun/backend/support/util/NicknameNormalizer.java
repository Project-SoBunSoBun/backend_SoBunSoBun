package com.sobunsobun.backend.support.util;

import org.springframework.stereotype.Component;

//정규화- 공백 제거 등

@Component
public class NicknameNormalizer {
    public String normalize(String raw) {
        if (raw == null) return null;
        return raw.trim();
    }
}
