package com.sttp.skripsi.service;

import com.sttp.skripsi.constant.ErrorMessage;
import com.sttp.skripsi.dto.RegisterRequest;
import com.sttp.skripsi.exception.AppException;
import com.sttp.skripsi.model.User;
import com.sttp.skripsi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw AppException.badRequest(ErrorMessage.USERNAME_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw AppException.badRequest(ErrorMessage.EMAIL_ALREADY_EXISTS);
        }

        if (request.getPassword().length() < 8) {
            throw AppException.badRequest(ErrorMessage.PASSWORD_TOO_WEAK);
        }

        try {
            User user = User.builder()
                    .username(request.getUsername())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .email(request.getEmail())
                    .fullName(request.getFullName())
                    .active(true) // FIXED
                    .build();

            User savedUser = userRepository.save(user);
            return savedUser; // <--- Make sure to return this!
        } catch (Exception e) {
            throw AppException.internalServerError(ErrorMessage.USER_CREATION_FAILED);
        }
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> AppException.notFound(ErrorMessage.USER_NOT_FOUND));
    }

    public boolean checkPassword(String encodedPassword, String rawPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Transactional
    public void resetAdminPassword() {
        try {
            User admin = getUserByUsername("admin");
            admin.setPassword(passwordEncoder.encode("12345678"));
            userRepository.save(admin);
        } catch (Exception e) {
            throw AppException.internalServerError(ErrorMessage.USER_UPDATE_FAILED);
        }
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(String username, String email, String fullName, String password) {
        if (userRepository.existsByUsername(username)) {
            throw AppException.badRequest(ErrorMessage.USERNAME_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(email)) {
            throw AppException.badRequest(ErrorMessage.EMAIL_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword(passwordEncoder.encode(password));
        user.setActive(true);

        return userRepository.save(user);
    }

    public User updateUser(Long id, String email, String fullName, Boolean isActive) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound(ErrorMessage.USER_NOT_FOUND));

        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw AppException.badRequest(ErrorMessage.EMAIL_ALREADY_EXISTS);
        }

        user.setEmail(email);
        user.setFullName(fullName);
        if (isActive != null) {
            user.setActive(isActive);
        }

        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public void changePassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound(ErrorMessage.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}