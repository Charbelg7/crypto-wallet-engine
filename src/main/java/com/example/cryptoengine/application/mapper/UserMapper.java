package com.example.cryptoengine.application.mapper;

import com.example.cryptoengine.application.dto.UserResponse;
import com.example.cryptoengine.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {
    
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
    
    UserResponse toResponse(User user);
}
