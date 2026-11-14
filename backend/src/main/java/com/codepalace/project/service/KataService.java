package com.codepalace.project.service;

import com.codepalace.project.model.Kata;
import com.codepalace.project.model.dto.client.KataSummaryDto;
import com.codepalace.project.model.mapper.KataMapper;
import com.codepalace.project.repository.KataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class KataService {

    private static final String PUBLISHED = "published";
    private static final String PUBLIC = "public";

    private final KataRepository kataRepository;
    private final KataMapper kataMapper;

    public KataService(KataRepository kataRepository, KataMapper kataMapper) {
        this.kataRepository = kataRepository;
        this.kataMapper = kataMapper;
    }

    /**
     * Lists the katas a user is allowed to see.
     *
     * <p>Only published, public katas are returned, and the reference solution is never part of
     * the response. The {@code tag} and {@code category} filters are accepted for API compatibility
     * but have no backing columns yet - the taxonomy table is a later phase.
     */
    public List<KataSummaryDto> list(String query, String tag, String category, String difficulty) {
        List<Kata> katas;

        if (query != null && !query.isBlank()) {
            katas = kataRepository.searchPublishedPublic("%" + query.trim().toLowerCase(Locale.ROOT) + "%");
        } else if (difficulty != null && !difficulty.isBlank()) {
            katas = kataRepository.findAllByStatusAndVisibilityAndDifficultyOrderByCreatedAtDesc(
                    PUBLISHED, PUBLIC, difficulty);
        } else {
            katas = kataRepository.findAllByStatusAndVisibilityOrderByCreatedAtDesc(PUBLISHED, PUBLIC);
        }

        return kataMapper.toSummaries(katas);
    }
}
