package com.shareway.application.usecase;

import com.shareway.application.dto.response.UserResponse;
import com.shareway.infrastructure.adapter.audit.domain.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
