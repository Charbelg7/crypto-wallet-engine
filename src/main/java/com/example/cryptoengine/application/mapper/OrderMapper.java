package com.example.cryptoengine.application.mapper;

import com.example.cryptoengine.application.dto.OrderResponse;
import com.example.cryptoengine.domain.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    
    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);
    
    @Mapping(target = "remainingQuantity", expression = "java(order.getRemainingQuantity())")
    @Mapping(target = "symbol", expression = "java(order.getSymbol())")
    OrderResponse toResponse(Order order);
}
