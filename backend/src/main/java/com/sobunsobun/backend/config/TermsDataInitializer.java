package com.sobunsobun.backend.config;

import com.sobunsobun.backend.domain.Terms;
import com.sobunsobun.backend.repository.TermsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 애플리케이션 시작 시 약관 초기 데이터를 DB에 저장합니다.
 * 이미 존재하는 type+version 조합은 건너뜁니다.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class TermsDataInitializer implements ApplicationRunner {

    private final TermsRepository termsRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initTerms("SERVICE",  "1.0.0", "서비스 이용약관",          "static/서비스 이용약관.txt");
        initTerms("PRIVACY",  "1.0.0", "개인정보처리방침",          "static/개인정보처리방침.txt");
        initTerms("LOCATION", "1.0.0", "위치기반서비스 이용약관",    "static/위치기반서비스 이용약관.txt");
    }

    private void initTerms(String type, String version, String title, String resourcePath) {
        if (termsRepository.existsByTypeAndVersion(type, version)) {
            log.debug("약관 데이터 이미 존재 — type={}, version={}", type, version);
            return;
        }

        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            String content = resource.getContentAsString(StandardCharsets.UTF_8);

            Terms terms = Terms.builder()
                    .type(type)
                    .version(version)
                    .title(title)
                    .content(content)
                    .effectiveDate(LocalDate.of(2025, 1, 1))
                    .build();

            termsRepository.save(terms);
            log.info("약관 초기 데이터 저장 완료 — type={}, version={}", type, version);
        } catch (IOException e) {
            log.error("약관 파일 읽기 실패 — path={}", resourcePath, e);
        }
    }
}
