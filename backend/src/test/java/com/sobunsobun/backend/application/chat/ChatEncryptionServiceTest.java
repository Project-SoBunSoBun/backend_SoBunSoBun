package com.sobunsobun.backend.application.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ChatEncryptionServiceTest {

    private ChatEncryptionService chatEncryptionService;

    @BeforeEach
    void setUp() {
        chatEncryptionService = new ChatEncryptionService();
        // 테스트용 256-bit AES 키 (64자 hex = 32 bytes)
        String testSecretHex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        ReflectionTestUtils.setField(chatEncryptionService, "secretHex", testSecretHex);
        chatEncryptionService.initKey();
    }

    @Test
    @DisplayName("encrypt → decrypt 하면 원문이 그대로 복구된다 (round-trip)")
    void encryptAndDecrypt_roundTrip() {
        // given
        String plain = "안녕하세요! 소분소분 채팅 암호화 테스트 😊 1234 !@#";

        // when
        String encrypted = chatEncryptionService.encrypt(plain);
        String decrypted = chatEncryptionService.decrypt(encrypted);

        // then
        assertNotNull(encrypted, "암호문은 null 이면 안 된다");
        assertNotEquals(plain, encrypted, "암호문은 평문과 달라야 한다");
        assertEquals(plain, decrypted, "복호화 결과는 원문과 같아야 한다");
    }

    @Test
    @DisplayName("서로 다른 평문은 서로 다른 암호문을 생성한다 (동일 키 기준)")
    void encrypt_differentPlainTexts_produceDifferentCipherTexts() {
        // given
        String plain1 = "첫 번째 메시지";
        String plain2 = "두 번째 메시지";

        // when
        String enc1 = chatEncryptionService.encrypt(plain1);
        String enc2 = chatEncryptionService.encrypt(plain2);

        // then
        assertNotEquals(plain1, enc1);
        assertNotEquals(plain2, enc2);
        assertNotEquals(enc1, enc2, "서로 다른 평문은 다른 암호문을 가져야 한다");
    }

    @Test
    @DisplayName("null, 빈 문자열 처리")
    void encryptDecrypt_nullAndEmpty() {
        // null 은 그대로 null 처리된다고 가정
        assertNull(chatEncryptionService.encrypt(null));
        assertNull(chatEncryptionService.decrypt(null));

        // 빈 문자열은 허용 / round-trip
        String empty = "";
        String encrypted = chatEncryptionService.encrypt(empty);
        String decrypted = chatEncryptionService.decrypt(encrypted);

        assertEquals(empty, decrypted);
    }

    @Test
    @DisplayName("isEncrypted: 암호문은 true, 평문은 false 를 반환한다")
    void isEncrypted_cipherVsPlain() {
        // given
        String plain = "암호화 전 평문";
        String cipher = chatEncryptionService.encrypt(plain);

        // when
        boolean plainCheck = chatEncryptionService.isEncrypted(plain);
        boolean cipherCheck = chatEncryptionService.isEncrypted(cipher);

        // then
        assertFalse(plainCheck, "평문은 isEncrypted() == false 여야 한다");
        assertTrue(cipherCheck, "encrypt() 결과는 isEncrypted() == true 여야 한다");
    }

    @Test
    @DisplayName("isEncrypted: null, 빈 문자열, 이상한 문자열은 false")
    void isEncrypted_nullEmptyRandom() {
        assertFalse(chatEncryptionService.isEncrypted(null));
        assertFalse(chatEncryptionService.isEncrypted(""));
        assertFalse(chatEncryptionService.isEncrypted("????"));
        assertFalse(chatEncryptionService.isEncrypted("not-base64@@@"));
    }
}