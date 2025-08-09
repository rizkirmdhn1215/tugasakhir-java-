package com.sttp.skripsi.service;

import com.sttp.skripsi.constant.ErrorMessage;
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
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        try {
            return userRepository.findAll();
        } catch (Exception e) {
            throw AppException.internalServerError(ErrorMessage.DB_QUERY_ERROR);
        }
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound(ErrorMessage.USER_NOT_FOUND));
    }

    @Transactional
    public User createUser(String username, String email, String password, String fullName) {
        if (userRepository.existsByUsername(username)) {
            throw AppException.badRequest(ErrorMessage.USERNAME_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(email)) {
            throw AppException.badRequest(ErrorMessage.EMAIL_ALREADY_EXISTS);
        }

        if (password.length() < 8) {
            throw AppException.badRequest(ErrorMessage.PASSWORD_TOO_WEAK);
        }

        try {
            User user = User.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .fullName(fullName)
                    .active(true) // FIXED
                    .build();

            return userRepository.save(user);
        } catch (Exception e) {
            throw AppException.internalServerError(ErrorMessage.USER_CREATION_FAILED);
        }
    }

    @Transactional
    public User updateUser(Long id, String email, String fullName, Boolean isActive) {
        User user = getUserById(id);

        if (email != null && !email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            throw AppException.badRequest(ErrorMessage.EMAIL_ALREADY_EXISTS);
        }

        try {
            if (email != null) user.setEmail(email);
            if (fullName != null) user.setFullName(fullName);
            if (isActive != null) user.setActive(isActive); // FIXED

            return userRepository.save(user);
        } catch (Exception e) {
            throw AppException.internalServerError(ErrorMessage.USER_UPDATE_FAILED);
        }
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        try {
            userRepository.delete(user);
        } catch (Exception e) {
            throw AppException.internalServerError(ErrorMessage.USER_DELETE_FAILED);
        }
    }

    @Transactional
    public void changePassword(Long id, String newPassword) {
        User user = getUserById(id);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}