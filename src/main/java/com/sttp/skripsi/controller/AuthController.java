package com.sttp.skripsi.controller;

import com.sttp.skripsi.constant.ErrorMessage;
import com.sttp.skripsi.dto.AuthRequest;
import com.sttp.skripsi.dto.AuthResponse;
import com.sttp.skripsi.dto.RegisterRequest;
import com.sttp.skripsi.exception.AppException;
import com.sttp.skripsi.model.User;
import com.sttp.skripsi.security.JwtService;
import com.sttp.skripsi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request);
            String token = jwtService.generateToken(user);
            AuthResponse response = new AuthResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getFullName()
            );
            return ResponseEntity.ok(response);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Registration failed: ", e);
            throw AppException.internalServerError(ErrorMessage.USER_CREATION_FAILED);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );

            User user = (User) authentication.getPrincipal();
            String token = jwtService.generateToken(user);

            AuthResponse response = new AuthResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getFullName()
            );

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Login failed: ", e);
            throw AppException.unauthorized(ErrorMessage.AUTH_INVALID_CREDENTIALS);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        try {
            SecurityContextHolder.clearContext();
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logout berhasil");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Logout failed: ", e);
            throw AppException.internalServerError(ErrorMessage.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/debug/admin-password")
    public ResponseEntity<Map<String, String>> debugAdminPassword() {
        logger.info("Debug endpoint called to check admin password");
        User admin = userService.getUserByUsername("admin");
        String rawPassword = "12345678";
        boolean matches = userService.checkPassword(admin.getPassword(), rawPassword);

        logger.info("Admin password check - Stored hash: {}, Raw: {}, Matches: {}",
            admin.getPassword(), rawPassword, matches);

        Map<String, String> response = new HashMap<>();
        response.put("storedPassword", admin.getPassword());
        response.put("rawPassword", rawPassword);
        response.put("matches", String.valueOf(matches));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-admin-password")
    public ResponseEntity<Map<String, String>> resetAdminPassword() {
        logger.info("Reset admin password endpoint called");
        userService.resetAdminPassword();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Admin password has been reset to: 12345678");
        logger.info("Admin password reset successful");
        return ResponseEntity.ok(response);
    }
}