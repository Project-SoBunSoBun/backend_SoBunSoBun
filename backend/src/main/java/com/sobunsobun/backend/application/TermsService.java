package com.sobunsobun.backend.application;

import com.sobunsobun.backend.domain.Terms;
import com.sobunsobun.backend.dto.terms.TermsResponse;
import com.sobunsobun.backend.repository.TermsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsService {

    private static final Set<String> REQUIRED_TYPES = Set.of("SERVICE", "PRIVACY", "LOCATION");

    private final TermsRepository termsRepository;

    public TermsResponse getLatestTerms(String type) {
        Terms terms = termsRepository.findTopByTypeOrderByEffectiveDateDesc(type.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "약관을 찾을 수 없습니다."));
        return toResponse(terms);
    }

    public List<TermsResponse> getTermsVersions(String type) {
        return termsRepository.findByTypeOrderByEffectiveDateDesc(type.toUpperCase())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TermsResponse getTermsByVersion(String type, String version) {
        Terms terms = termsRepository.findByTypeAndVersion(type.toUpperCase(), version)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 버전의 약관을 찾을 수 없습니다."));
        return toResponse(terms);
    }

    private TermsResponse toResponse(Terms terms) {
        return TermsResponse.builder()
                .id(terms.getId())
                .type(terms.getType())
                .version(terms.getVersion())
                .title(terms.getTitle())
                .content(terms.getContent())
                .isRequired(REQUIRED_TYPES.contains(terms.getType()))
                .effectiveDate(terms.getEffectiveDate().atStartOfDay())
                .createdAt(terms.getCreatedAt())
                .build();
    }
}
