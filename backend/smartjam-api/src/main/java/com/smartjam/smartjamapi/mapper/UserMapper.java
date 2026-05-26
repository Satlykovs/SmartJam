package com.smartjam.smartjamapi.mapper;

import java.util.List;
import java.util.Set;

import com.smartjam.api.model.RegisterRequest;
import com.smartjam.api.model.UserResponse;
import com.smartjam.api.model.UserRole;
import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.service.S3Service;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class UserMapper {

    @Autowired
    protected S3Service s3Service;

    public abstract UserEntity toEntity(RegisterRequest request);

    @Mapping(target = "roles", expression = "java(mapRoles(userEntity.getRoles()))")
    @Mapping(target = "avatarUrl", expression = "java(buildAvatarUrl(userEntity.getAvatarUrl()))")
    public abstract UserResponse toUserResponse(UserEntity userEntity);

    protected List<String> mapRoles(Set<UserRole> roles) {
        return roles == null ? List.of() : roles.stream().map(Enum::name).toList();
    }

    protected String buildAvatarUrl(String s3Key) {
        if (s3Key == null) {
            return null;
        }

        return s3Service.generatePresignedUrlForDownload(s3Key);
    }
}
