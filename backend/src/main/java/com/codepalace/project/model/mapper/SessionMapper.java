package com.codepalace.project.model.mapper;

import com.codepalace.project.model.Session;
import com.codepalace.project.model.dto.client.SessionDetailDto;
import com.codepalace.project.model.dto.client.SessionSummaryDto;
import org.springframework.stereotype.Component;

@Component
public class SessionMapper {

    public SessionSummaryDto toSummary(Session session, String kataTitle) {
        if (session == null) {
            return null;
        }
        return new SessionSummaryDto(
                session.getId(),
                session.getWorkspaceId(),
                session.getKataId(),
                kataTitle,
                session.getProgrammingLanguage(),
                session.getStatus(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    public SessionDetailDto toDetail(Session session, String kataTitle) {
        if (session == null) {
            return null;
        }
        return new SessionDetailDto(
                session.getId(),
                session.getWorkspaceId(),
                session.getKataId(),
                kataTitle,
                session.getProgrammingLanguage(),
                session.getStatus(),
                session.getDraftCode(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
