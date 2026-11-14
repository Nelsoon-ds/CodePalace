package com.codepalace.project.model.mapper;

import com.codepalace.project.model.Session;
import com.codepalace.project.model.Workspace;
import com.codepalace.project.model.dto.client.SessionSummaryDto;
import com.codepalace.project.model.dto.client.WorkspaceSummaryDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class WorkspaceMapper {

    public WorkspaceSummaryDto toSummary(Workspace workspace, Map<UUID, String> kataTitles) {
        if (workspace == null) {
            return null;
        }
        List<SessionSummaryDto> sessions = workspace.getSessions().stream()
                .map(session -> toSessionSummary(session, kataTitles))
                .toList();

        return new WorkspaceSummaryDto(
                workspace.getId(),
                workspace.getWorkspaceName(),
                workspace.getCreatedAt(),
                sessions
        );
    }

    private SessionSummaryDto toSessionSummary(Session session, Map<UUID, String> kataTitles) {
        String title = session.getKataId() != null ? kataTitles.get(session.getKataId()) : null;
        return new SessionSummaryDto(
                session.getId(),
                session.getWorkspaceId(),
                session.getKataId(),
                title,
                session.getProgrammingLanguage(),
                session.getStatus(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
