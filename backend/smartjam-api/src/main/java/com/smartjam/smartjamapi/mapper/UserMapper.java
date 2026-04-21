package com.smartjam.smartjamapi.mapper;

import com.smartjam.api.model.RegisterRequest;
import com.smartjam.smartjamapi.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserEntity toEntity(RegisterRequest request);
}