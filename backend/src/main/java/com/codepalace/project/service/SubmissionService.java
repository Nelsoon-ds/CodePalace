package com.codepalace.project.service;

import com.codepalace.project.model.*;
import com.codepalace.project.model.dto.client.CodeSubmission;
import com.codepalace.project.model.dto.client.RunResponseDto;
import com.codepalace.project.model.mapper.RunMapper;
import com.codepalace.project.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final JobNotificationService jobNotificationService;
    private final TestSuiteRepository testSuiteRepository;
    private final RunRepository runRepository;
    private final SessionRepository sessionRepository;
    private final JobRepository jobRepository;
    private final CurrentUserService currentUserService;
    private final SessionService sessionService;
    private final RunMapper runMapper;

    public SubmissionService(SubmissionRepository submissionRepository,
                             JobNotificationService jobNotificationService,
                             TestSuiteRepository testSuiteRepository,
                             RunRepository runRepository,
                             SessionRepository sessionRepository,
                             JobRepository jobRepository,
                             CurrentUserService currentUserService,
                             SessionService sessionService,
                             RunMapper runMapper) {
        this.submissionRepository = submissionRepository;
        this.jobNotificationService = jobNotificationService;
        this.testSuiteRepository = testSuiteRepository;
        this.runRepository = runRepository;
        this.sessionRepository = sessionRepository;
        this.jobRepository = jobRepository;
        this.currentUserService = currentUserService;
        this.sessionService = sessionService;
        this.runMapper = runMapper;
    }

    /**
     * Stores a submission and queues it for execution.
     *
     * <p>The whole write is one transaction: the submission, the run and the job row either all
     * exist or none of them do, and the queue notification is delivered by Postgres when the
     * transaction commits.
     */
    @Transactional
    public RunResponseDto createSubmission(UUID sessionId, CodeSubmission codeSubmission) {
        Session session = sessionService.requireOwned(sessionId);

        if (session.getKataId() == null) {
            throw new EntityNotFoundException("This session has no kata attached, so there is nothing to run.");
        }

        TestSuite testsuite = testSuiteRepository
                .findFirstByKataIdAndProgrammingLanguageOrderByCreatedAtDesc(
                        session.getKataId(),
                        session.getProgrammingLanguage())
                .orElseThrow(() -> new EntityNotFoundException("No test suite is available for this kata yet."));

        Submission submission = new Submission();
        submission.setSessionId(sessionId);
        submission.setCode(codeSubmission.code());
        submission.setCreatedAt(Instant.now());
        submission = submissionRepository.save(submission);

        Run run = new Run();
        run.setStatus("QUEUED");
        run.setSubmissionId(submission.getId());
        run.setSuiteId(testsuite.getId());
        runRepository.save(run);

        Job job = new Job();
        job.setRunId(run.getId());
        job.setStatus("QUEUED");
        job.setWorkType("RUN_SUBMISSION");
        job.setCreatedAt(Instant.now());
        jobRepository.save(job);

        jobNotificationService.notifyJobsChannel("New job added!");

        return runMapper.toResponse(run);
    }
}
