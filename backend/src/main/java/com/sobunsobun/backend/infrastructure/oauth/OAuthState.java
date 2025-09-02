package com.sobunsobun.backend.infrastructure.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public record OAuthState(String returnTo, String platform, long ts){
    public static String encode(String returnTo, String platform, ObjectMapper om){
        try{
            String json = om.writeValueAsString(Map.of(
                    "returnTo", returnTo,
                    "platform", platform,
                    "ts", System.currentTimeMillis()
            ));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        }catch (Exception e){ throw new IllegalStateException("state encode 실패", e); }
    }
    public static OAuthState decode(String b64, ObjectMapper om){
        try{
            byte[] raw = Base64.getUrlDecoder().decode(b64);
            @SuppressWarnings("unchecked")
            Map<String,Object> m = om.readValue(raw, Map.class);
            return new OAuthState(
                    (String)m.getOrDefault("returnTo","/"),
                    (String)m.getOrDefault("platform","web"),
                    ((Number)m.getOrDefault("ts",0)).longValue()
            );
        }catch (Exception e){ throw new IllegalArgumentException("state decode 실패", e); }
    }
}
