package com.codepalace.project.service;

import com.codepalace.project.model.Kata;
import com.codepalace.project.model.mapper.KataMapper;
import com.codepalace.project.repository.KataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KataServiceTest {

    @Mock KataRepository kataRepository;

    KataService kataService;

    @BeforeEach
    void setUp() {
        kataService = new KataService(kataRepository, new KataMapper());
    }

    @Test
    void onlyPublishedAndPublicKatasAreListed() {
        when(kataRepository.findAllByStatusAndVisibilityOrderByCreatedAtDesc("published", "public"))
                .thenReturn(List.of());

        kataService.list(null, null, null, null);

        verify(kataRepository).findAllByStatusAndVisibilityOrderByCreatedAtDesc("published", "public");
    }

    @Test
    void aSearchOnlyLooksAtPublishedAndPublicKatas() {
        when(kataRepository.searchPublishedPublic("%sum%")).thenReturn(List.of());

        kataService.list("SUM", null, null, null);

        verify(kataRepository).searchPublishedPublic("%sum%");
    }

    @Test
    void theListedKataNeverCarriesTheReferenceSolution() {
        Kata kata = new Kata();
        kata.setId(java.util.UUID.randomUUID());
        kata.setTitle("Sum of Two Numbers");
        kata.setStatementMd("Add two numbers.");
        kata.setDifficulty("easy");
        kata.setProgrammingLanguage("js");
        kata.setStarterCode("export function sum(a, b) {}");
        kata.setReferenceSolution("export function sum(a, b) { return a + b; }");

        when(kataRepository.findAllByStatusAndVisibilityOrderByCreatedAtDesc("published", "public"))
                .thenReturn(List.of(kata));

        var katas = kataService.list(null, null, null, null);

        assertThat(katas).hasSize(1);
        assertThat(katas.getFirst().title()).isEqualTo("Sum of Two Numbers");
        assertThat(katas.getFirst().language()).isEqualTo("js");
        assertThat(katas.getFirst().starterCode()).isEqualTo("export function sum(a, b) {}");
        assertThat(katas.getFirst().toString()).doesNotContain("return a + b");
    }
}
