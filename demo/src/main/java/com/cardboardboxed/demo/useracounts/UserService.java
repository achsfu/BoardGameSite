package com.cardboardboxed.demo.useracounts;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UserService {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }

        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        if (userRepository.findByUsername(username) != null) {
            throw new IllegalArgumentException("Username already exists");
        }

        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(user -> "ADMIN".equalsIgnoreCase(user.getRole()));

        String role = (!adminExists && username.equalsIgnoreCase("admin"))
                ? "ADMIN"
                : "PLAYER";

        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(username, encodedPassword, role);

        return userRepository.save(user);
    }
}