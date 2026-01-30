package com.example.cryptoengine.application.dto;

import java.time.LocalDateTime;

/**
 * DTO for user response.
 */
public record UserResponse(
    Long id,
    String email,
    String name,
    LocalDateTime createdAt
) {}
