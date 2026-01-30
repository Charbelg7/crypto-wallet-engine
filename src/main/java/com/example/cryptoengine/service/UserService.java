package com.example.cryptoengine.service;

import com.example.cryptoengine.application.dto.CreateUserRequest;
import com.example.cryptoengine.application.dto.UserResponse;
import com.example.cryptoengine.application.mapper.UserMapper;
import com.example.cryptoengine.domain.entity.User;
import com.example.cryptoengine.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user management operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("User with email already exists: " + request.email());
        }

        User user = User.builder()
            .email(request.email())
            .name(request.name())
            .build();

        user = userRepository.save(user);
        log.info("Created user: {} ({})", user.getId(), user.getEmail());
        
        return userMapper.toResponse(user);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        return userMapper.toResponse(user);
    }
}
