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
    /**
 * Maps registration input into a new UserEntity.
 *
 * @param request the registration data to map into the entity
 * @return a UserEntity populated from the provided registration data
 */
public abstract UserEntity toEntity(RegisterRequest request);

    /**
     * Map a UserEntity to a UserResponse DTO.
     *
     * The resulting UserResponse contains the user's fields and a list of role names;
     * if the source entity has no roles, the roles list will be empty.
     *
     * @param userEntity the source entity to convert
     * @return the mapped UserResponse with roles represented as a list of role names
     */
    @Mapping(target = "roles", expression = "java(mapRoles(userEntity.getRoles()))")
    public abstract UserResponse toUserResponse(UserEntity userEntity);

    /**
     * Convert a set of UserRole enums into a list of their name strings.
     *
     * @param roles the set of roles to convert; may be null
     * @return a list of role name strings; an empty list if roles is null
     */
    protected List<String> mapRoles(Set<UserRole> roles) {
        return roles == null ? List.of() : roles.stream().map(Enum::name).toList();
    }
}
