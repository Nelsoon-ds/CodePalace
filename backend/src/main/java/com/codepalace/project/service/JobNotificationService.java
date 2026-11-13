package com.codepalace.project.service;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobNotificationService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void notifyJobsChannel(String jsonPayload) {
        // pg_notify takes the channel name and the payload as arguments
        String sql = "SELECT pg_notify('jobs_channel', :payload)";

        entityManager.createNativeQuery(sql)
                .setParameter("payload", jsonPayload)
                .getSingleResult(); // Executes the function
    }
}