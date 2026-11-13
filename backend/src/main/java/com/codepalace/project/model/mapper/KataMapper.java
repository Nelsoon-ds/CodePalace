package com.codepalace.project.model.mapper;

import com.codepalace.project.model.Kata;
import com.codepalace.project.model.dto.client.KataSummaryDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KataMapper {

    public KataSummaryDto toSummary(Kata kata) {
        if (kata == null) {
            return null;
        }
        return new KataSummaryDto(
                kata.getId(),
                kata.getTitle(),
                kata.getStatementMd(),
                kata.getDifficulty(),
                kata.getProgrammingLanguage(),
                kata.getStarterCode(),
                kata.getCreatedAt()
        );
    }

    public List<KataSummaryDto> toSummaries(List<Kata> katas) {
        return katas.stream().map(this::toSummary).toList();
    }
}
