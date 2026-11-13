package com.codepalace.project.service;

import com.codepalace.project.model.Run;
import com.codepalace.project.model.Submission;
import com.codepalace.project.model.dto.client.CodeSubmission;
import com.codepalace.project.repository.RunRepository;
import com.codepalace.project.repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private JobNotificationService jobNotificationService;

    @Autowired
    private RunRepository runRepository;

    public void createSubmission(UUID id, CodeSubmission codeSubmission) {
        Submission submission = new Submission();

        submission.setSessionId(id);
        submission.setCode(codeSubmission.code());
        submission = submissionRepository.save(submission);

        Run run = new Run();

        run.setSubmissionId(submission.getId());
        run.setSuiteId(submission);


        jobNotificationService.notifyJobsChannel("New job added!");

    }
}
