package com.attendance.service;

import com.attendance.domain.Role;
import com.attendance.entity.User;
import com.attendance.exception.ConflictException;
import com.attendance.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates and maintains login accounts. Passwords are always stored as BCrypt hashes. */
@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User create(String username, String rawPassword, Role role) {
        String name = username.trim();
        if (userRepository.existsByUsername(name)) {
            throw new ConflictException("Username '" + name + "' is already taken");
        }
        User user = new User();
        user.setUsername(name);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    @Transactional
    public void update(User user, boolean enabled, String newRawPassword) {
        user.setEnabled(enabled);
        if (newRawPassword != null && !newRawPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        }
        userRepository.save(user);
    }
}
