package com.smartjam.smartjamapi.mapper;

import java.util.List;
import java.util.Set;

import com.smartjam.api.model.RegisterRequest;
import com.smartjam.api.model.UserResponse;
import com.smartjam.api.model.UserRole;
import com.smartjam.smartjamapi.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class UserMapper {
    public abstract UserEntity toEntity(RegisterRequest request);

    @Mapping(target = "roles", expression = "java(mapRoles(userEntity.getRoles()))")
    public abstract UserResponse toUserResponse(UserEntity userEntity);

    protected List<String> mapRoles(Set<UserRole> roles) {
        return roles == null ? List.of() : roles.stream().map(Enum::name).toList();
    }
}
