package com.sttp.skripsi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
public class GoogleAuthController {
    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthController.class);

    @GetMapping("/")
    public ResponseEntity<String> handleGoogleCallback(
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "scope", required = false) String scope) {
        
        // If no OAuth parameters, return a simple welcome message
        if (code == null) {
            return ResponseEntity.ok("Welcome to the application!");
        }
        
        logger.info("Received OAuth callback with code: {}", code);
        
        try {
            // Create a token structure that matches what the Go code expects
            Map<String, Object> token = new HashMap<>();
            token.put("code", code);
            token.put("state", state);
            token.put("scope", scope);
            token.put("token_type", "Bearer");
            token.put("access_token", code); // The code will be exchanged for a token by Go
            token.put("expires_in", 3600);
            token.put("refresh_token", ""); // Will be populated by Go

            // Convert to JSON
            String tokenJson = new com.google.gson.Gson().toJson(token);

            // Ensure the golang directory exists
            Files.createDirectories(Paths.get("golang"));

            // Save to token.json
            String tokenPath = "golang/token.json";
            try (FileWriter file = new FileWriter(tokenPath)) {
                file.write(tokenJson);
                file.flush();
                logger.info("Successfully saved token to {}", tokenPath);
            }

            // Verify the file was written
            if (Files.exists(Paths.get(tokenPath))) {
                String content = new String(Files.readAllBytes(Paths.get(tokenPath)));
                logger.info("Verified token file contents: {}", content);
                return ResponseEntity.ok("Authorization successful! You can now close this window and return to the application.");
            } else {
                logger.error("Token file was not created at {}", tokenPath);
                return ResponseEntity.internalServerError().body("Error: Token file was not created");
            }
        } catch (IOException e) {
            logger.error("Error saving token: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error saving token: " + e.getMessage());
        }
    }
} 