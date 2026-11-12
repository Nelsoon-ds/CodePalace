package com.codepalace.project.service;


import com.codepalace.project.model.User;
import com.codepalace.project.repository.UserRepository;
import org.springframework.security.crypto.password4j.Argon2Password4jPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final Argon2Password4jPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, Argon2Password4jPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(String email, String rawPassword) {
        String encoded = passwordEncoder.encode(rawPassword);
        return userRepository.save(new User(email, encoded));
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
