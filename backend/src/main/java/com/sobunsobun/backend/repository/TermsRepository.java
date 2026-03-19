package com.sobunsobun.backend.repository;

import com.sobunsobun.backend.domain.Terms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TermsRepository extends JpaRepository<Terms, Long> {

    boolean existsByTypeAndVersion(String type, String version);

    Optional<Terms> findTopByTypeOrderByEffectiveDateDesc(String type);

    List<Terms> findByTypeOrderByEffectiveDateDesc(String type);

    Optional<Terms> findByTypeAndVersion(String type, String version);
}
