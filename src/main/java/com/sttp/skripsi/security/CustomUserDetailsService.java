package com.sttp.skripsi.security;

import com.sttp.skripsi.constant.ErrorMessage;
import com.sttp.skripsi.exception.AppException;
import com.sttp.skripsi.model.User;
import com.sttp.skripsi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Attempting to load user by username: {}", username);
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        logger.error("User not found with username: {}", username);
                        throw AppException.notFound(ErrorMessage.AUTH_USER_NOT_FOUND);
                    });

            logger.debug("Found user: id={}, email={}, active={}", user.getId(), user.getEmail(), user.isActive());

            if (!user.isActive()) { 
                logger.error("User account is disabled: {}", username);
                throw AppException.unauthorized(ErrorMessage.AUTH_ACCOUNT_DISABLED);
            }

            logger.info("Successfully loaded user: {}", username);
            return user;

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error loading user: {}", e.getMessage(), e);
            throw AppException.internalServerError(ErrorMessage.INTERNAL_SERVER_ERROR);
        }
    }
}