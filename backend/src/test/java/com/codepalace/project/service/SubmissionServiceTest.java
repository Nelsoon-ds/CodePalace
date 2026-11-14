package com.codepalace.project.service;

import com.codepalace.project.model.Job;
import com.codepalace.project.model.Run;
import com.codepalace.project.model.Session;
import com.codepalace.project.model.Submission;
import com.codepalace.project.model.TestSuite;
import com.codepalace.project.model.dto.client.CodeSubmission;
import com.codepalace.project.model.mapper.RunMapper;
import com.codepalace.project.repository.JobRepository;
import com.codepalace.project.repository.RunRepository;
import com.codepalace.project.repository.SessionRepository;
import com.codepalace.project.repository.SubmissionRepository;
import com.codepalace.project.repository.TestSuiteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock SubmissionRepository submissionRepository;
    @Mock JobNotificationService jobNotificationService;
    @Mock TestSuiteRepository testSuiteRepository;
    @Mock RunRepository runRepository;
    @Mock SessionRepository sessionRepository;
    @Mock JobRepository jobRepository;
    @Mock CurrentUserService currentUserService;
    @Mock SessionService sessionService;

    SubmissionService submissionService;

    @BeforeEach
    void setUp() {
        submissionService = new SubmissionService(submissionRepository, jobNotificationService, testSuiteRepository,
                runRepository, sessionRepository, jobRepository, currentUserService, sessionService, new RunMapper());
    }

    @Test
    void submissionForAnotherUsersSessionIsRejected() {
        UUID sessionId = UUID.randomUUID();

        when(sessionService.requireOwned(sessionId)).thenThrow(new EntityNotFoundException("Session not found."));

        assertThatThrownBy(() -> submissionService.createSubmission(sessionId, new CodeSubmission("code")))
                .isInstanceOf(EntityNotFoundException.class);

        verify(submissionRepository, never()).save(any());
        verify(runRepository, never()).save(any());
        verify(jobRepository, never()).save(any());
        verify(jobNotificationService, never()).notifyJobsChannel(any());
    }

    @Test
    void submissionCreatesSubmissionRunAndJob() {
        UUID sessionId = UUID.randomUUID();
        UUID kataId = UUID.randomUUID();

        Session session = new Session();
        session.setId(sessionId);
        session.setKataId(kataId);
        session.setProgrammingLanguage("js");

        TestSuite suite = new TestSuite();
        suite.setId(UUID.randomUUID());

        when(sessionService.requireOwned(sessionId)).thenReturn(session);
        when(testSuiteRepository.findFirstByKataIdAndProgrammingLanguageOrderByCreatedAtDesc(kataId, "js"))
                .thenReturn(Optional.of(suite));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setId(UUID.randomUUID());
            return submission;
        });
        when(runRepository.save(any(Run.class))).thenAnswer(invocation -> {
            Run run = invocation.getArgument(0);
            run.setId(UUID.randomUUID());
            return run;
        });

        var response = submissionService.createSubmission(sessionId, new CodeSubmission("export const sum = 1;"));

        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo("QUEUED");
        verify(submissionRepository).save(any(Submission.class));
        verify(runRepository).save(any(Run.class));
        verify(jobRepository).save(any(Job.class));
        verify(jobNotificationService).notifyJobsChannel("New job added!");
    }

    @Test
    void sessionWithoutAKataCannotBeRun() {
        UUID sessionId = UUID.randomUUID();

        Session session = new Session();
        session.setId(sessionId);
        session.setProgrammingLanguage("js");

        when(sessionService.requireOwned(sessionId)).thenReturn(session);

        assertThatThrownBy(() -> submissionService.createSubmission(sessionId, new CodeSubmission("code")))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("no kata");

        verify(submissionRepository, never()).save(any());
    }
}
