package com.codepalace.project.service;


import com.codepalace.project.exception.EmailAlreadyExistsException;
import com.codepalace.project.model.User;
import com.codepalace.project.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        String encoded = passwordEncoder.encode(rawPassword);
        return userRepository.save(new User(normalizedEmail, encoded));
    }

    /**
     * Emails are stored and looked up in a single canonical form, so {@code Test@x.com} and
     * {@code test@x.com} can never become two accounts.
     */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
