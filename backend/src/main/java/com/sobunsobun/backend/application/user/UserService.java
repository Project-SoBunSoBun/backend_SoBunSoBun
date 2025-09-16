package com.sobunsobun.backend.application.user;

import com.sobunsobun.backend.domain.User;
import com.sobunsobun.backend.repository.user.UserRepository;
import com.sobunsobun.backend.support.util.NicknameNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final NicknameNormalizer normalizer;

    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String rawNickname) {
        String nickname = normalizer.normalize(rawNickname);
        validate(nickname);

        return !userRepository.existsByNickname(nickname); // 존재하지 않음 = 사용 가능
    }

    @Transactional
    public void setNickname(Long userId, String rawNickname) {
        String nickname = normalizer.normalize(rawNickname);
        validate(nickname);

        if (userRepository.existsByNicknameAndIdNot(nickname, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        user.setNickname(nickname);
        try {
            userRepository.saveAndFlush(user); // DB 유니크 위반 즉시 감지
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");
        }
    }

    private void validate(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "닉네임은 비어 있을 수 없습니다.");
        }
        if (nickname.length() > 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "닉네임은 최대 8자입니다.");
        }
        if (!nickname.matches("^[가-힣a-zA-Z0-9]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "닉네임은 한글/영문/숫자만 가능합니다.");
        }
    }
}
