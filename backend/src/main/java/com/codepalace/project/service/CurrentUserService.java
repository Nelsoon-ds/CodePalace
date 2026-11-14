package com.codepalace.project.service;

import com.codepalace.project.exception.UnauthenticatedException;
import com.codepalace.project.model.User;
import com.codepalace.project.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Resolves the caller behind the current request. Every service that touches user-owned data
 * goes through this, so a request can never act on data it does not own.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthenticatedException("Authentication is required to access this resource.");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("The authenticated account no longer exists."));
    }

    public UUID requireId() {
        return require().getUuid();
    }
}
