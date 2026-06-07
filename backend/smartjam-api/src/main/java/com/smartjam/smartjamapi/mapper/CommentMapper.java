package com.smartjam.smartjamapi.mapper;

import java.util.List;

import com.smartjam.api.model.CommentResponse;
import com.smartjam.smartjamapi.entity.CommentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    @Mapping(target = "commentId", source = "id")
    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "assignmentId", source = "assignment.id")
    CommentResponse toResponse(CommentEntity entity);

    List<CommentResponse> toResponseList(List<CommentEntity> entities);
}
