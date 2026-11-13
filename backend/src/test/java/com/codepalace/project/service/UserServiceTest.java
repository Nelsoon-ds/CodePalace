package com.codepalace.project.service;

import com.codepalace.project.exception.EmailAlreadyExistsException;
import com.codepalace.project.model.User;
import com.codepalace.project.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password4j.Argon2Password4jPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;

    PasswordEncoder passwordEncoder;
    UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new Argon2Password4jPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void emailIsStoredInACanonicalForm() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUuid(UUID.randomUUID());
            return user;
        });

        User created = userService.createUser("  Test@Example.COM ", "correct-horse-battery");

        assertThat(created.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void passwordIsStoredHashed() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.createUser("test@example.com", "correct-horse-battery");

        assertThat(created.getPasswordHash()).isNotEqualTo("correct-horse-battery");
        assertThat(passwordEncoder.matches("correct-horse-battery", created.getPasswordHash())).isTrue();
    }

    @Test
    void duplicateEmailIsRejected() {
        User existing = new User("test@example.com", "hash");
        existing.setUuid(UUID.randomUUID());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.createUser("TEST@example.com", "correct-horse-battery"))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, org.mockito.Mockito.never()).save(any(User.class));
    }
}
