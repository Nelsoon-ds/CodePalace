package com.codepalace.project.service;

import com.codepalace.project.model.*;
import com.codepalace.project.model.dto.client.CodeSubmission;
import com.codepalace.project.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Service
public class SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private JobNotificationService jobNotificationService;

    @Autowired
    private TestSuiteRepository testSuiteRepository;

    @Autowired
    private RunRepository runRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private JobRepository jobRepository;

    public void createSubmission(UUID sessionId, CodeSubmission codeSubmission) {
        Submission submission = new Submission();
        Session session = sessionRepository.findById(sessionId).orElseThrow(EntityNotFoundException::new);

        TestSuite testsuite = testSuiteRepository.findFirstByKataIdAndProgrammingLanguageOrderByCreatedAtDesc(
                session.getKataId(),
                session.getProgrammingLanguage()).orElseThrow(EntityNotFoundException::new);

        submission.setSessionId(sessionId);
        submission.setCode(codeSubmission.code());
        submission.setCreatedAt(Timestamp.from(Instant.now()));
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
        job.setCreatedAt(Timestamp.from(Instant.now()));
        jobRepository.save(job);
        jobNotificationService.notifyJobsChannel("New job added!");

    }
}
