package com.sobunsobun.backend.application.chat;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
public class ChatEncryptionService {

    // application.yml 에 넣을 값 (hex)
    @Value("${app.chat.secret}")
    private String secretHex;

    private SecretKey secretKey;
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;     // bits
    private static final int IV_LENGTH_BYTES = 12;     // 96 bits (GCM 권장)

    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    void initKey() {
        try {
            byte[] keyBytes = Hex.decodeHex(secretHex.toCharArray());
            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                throw new IllegalStateException("AES 키는 16/24/32바이트(128/192/256비트)여야 합니다.");
            }
            this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            log.info("ChatEncryptionService 초기화 완료 (key length = {} bytes)", keyBytes.length);
        } catch (Exception e) {
            log.error("ChatEncryptionService 초기화 실패", e);
            throw new IllegalStateException("암호화 키 초기화 실패: " + e.getMessage(), e);
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // 저장은 iv + cipher 를 합쳐서 Base64로
            byte[] combined = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("채팅 메시지 암호화 실패", e);
            throw new ChatEncryptionException("채팅 메시지 암호화 실패", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null) return null;
        if (!isEncrypted(cipherText)) return cipherText;

        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);

            if (combined.length < IV_LENGTH_BYTES + 16) { // 최소 길이 체크
                throw new IllegalArgumentException("암호문 형식이 올바르지 않습니다.");
            }

            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] cipherBytes = new byte[combined.length - IV_LENGTH_BYTES];

            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(combined, IV_LENGTH_BYTES, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("채팅 메시지 복호화 실패", e);
            throw new ChatEncryptionException("채팅 메시지 복호화 실패", e);
        }
    }

    public boolean isEncrypted(String content) {
        if (content == null || content.isEmpty()) return false;

        // Base64인지 검증
        try {
            byte[] decoded = Base64.getDecoder().decode(content);
            // 최소 길이 체크: IV(12 bytes) + 최소 암호문(16 bytes)
            return decoded.length >= (IV_LENGTH_BYTES + 16);
        } catch (IllegalArgumentException e) {
            return false; // Base64 decode 실패 → 평문
        }
    }

    public static class ChatEncryptionException extends RuntimeException {
        public ChatEncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
