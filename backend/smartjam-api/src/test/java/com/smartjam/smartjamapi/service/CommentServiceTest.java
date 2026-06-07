package com.smartjam.smartjamapi.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.smartjam.api.model.CommentPageResponse;
import com.smartjam.api.model.CommentRequest;
import com.smartjam.api.model.CommentResponse;
import com.smartjam.smartjamapi.entity.AssignmentEntity;
import com.smartjam.smartjamapi.entity.CommentEntity;
import com.smartjam.smartjamapi.entity.ConnectionsEntity;
import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.mapper.CommentMapper;
import com.smartjam.smartjamapi.repository.CommentsRepository;
import com.smartjam.smartjamapi.security.IdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService")
class CommentServiceTest {

    @Mock
    private AssignmentsService assignmentsService;

    @Mock
    private IdentityService identityService;

    @Mock
    private CommentsRepository repository;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentService commentService;

    private UUID assignmentId;
    private UUID teacherId;
    private UUID studentId;
    private UserEntity teacher;
    private UserEntity student;
    private AssignmentEntity assignment;

    @BeforeEach
    void setUp() {
        assignmentId = UUID.randomUUID();
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        teacher = new UserEntity();
        teacher.setId(teacherId);

        student = new UserEntity();
        student.setId(studentId);

        ConnectionsEntity connection = new ConnectionsEntity();
        connection.setId(UUID.randomUUID());
        connection.setTeacher(teacher);
        connection.setStudent(student);

        assignment = new AssignmentEntity();
        assignment.setConnection(connection);
    }

    static Stream<Arguments> connectionMembersProvider() {
        return Stream.of(arguments("teacher"), arguments("student"));
    }

    @ParameterizedTest(name = "createComment: {0} успешно создаёт комментарий")
    @MethodSource("connectionMembersProvider")
    void createComment_whenUserIsConnectionMember_savesCommentAndReturnsResponse(String userKey) {
        UUID currentUserId = userKey.equals("teacher") ? teacherId : studentId;
        UserEntity expectedAuthor = userKey.equals("teacher") ? teacher : student;

        CommentRequest request = new CommentRequest("Отличная работа!");
        CommentEntity savedEntity = new CommentEntity();
        CommentResponse expectedResponse = mock(CommentResponse.class);

        when(identityService.getCurrentUserId()).thenReturn(currentUserId);
        when(assignmentsService.getAssignmentEntityById(assignmentId)).thenReturn(assignment);
        when(repository.save(any(CommentEntity.class))).thenReturn(savedEntity);
        when(commentMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        CommentResponse actual = commentService.createComment(assignmentId, request);

        assertThat(actual).isSameAs(expectedResponse);

        ArgumentCaptor<CommentEntity> captor = ArgumentCaptor.forClass(CommentEntity.class);
        verify(repository).save(captor.capture());

        CommentEntity passedToRepo = captor.getValue();
        assertThat(passedToRepo.getText()).isEqualTo("Отличная работа!");
        assertThat(passedToRepo.getAuthor()).isEqualTo(expectedAuthor);
        assertThat(passedToRepo.getAssignment()).isEqualTo(assignment);

        verify(commentMapper).toResponse(savedEntity);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("createComment: посторонний пользователь получает AccessDeniedException")
    void createComment_whenUserNotInConnection_throwsAccessDenied() {
        UUID strangerId = UUID.randomUUID();
        CommentRequest request = new CommentRequest("hi");

        when(identityService.getCurrentUserId()).thenReturn(strangerId);
        when(assignmentsService.getAssignmentEntityById(assignmentId)).thenReturn(assignment);

        assertThatThrownBy(() -> commentService.createComment(assignmentId, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only the connection member");

        verifyNoInteractions(repository, commentMapper);
    }

    @ParameterizedTest(name = "getComments: {0} успешно читает комментарии")
    @MethodSource("connectionMembersProvider")
    void getComments_whenUserIsConnectionMember_returnsFirstPageWithoutNext(String userKey) {
        UUID currentUserId = userKey.equals("teacher") ? teacherId : studentId;
        int limit = 5;

        CommentEntity c1 = CommentEntity.builder()
                .id(UUID.randomUUID())
                .text("a")
                .createdAt(Instant.parse("2025-01-01T10:00:00Z"))
                .build();
        CommentEntity c2 = CommentEntity.builder()
                .id(UUID.randomUUID())
                .text("b")
                .createdAt(Instant.parse("2025-01-01T11:00:00Z"))
                .build();

        List<CommentResponse> mappedResponses = List.of(mock(CommentResponse.class), mock(CommentResponse.class));

        when(identityService.getCurrentUserId()).thenReturn(currentUserId);
        when(assignmentsService.getAssignmentEntityById(assignmentId)).thenReturn(assignment);
        when(repository.getFirstPage(eq(assignmentId), any(Pageable.class)))
                .thenReturn(new ArrayList<>(List.of(c1, c2)));
        when(commentMapper.toResponseList(anyList())).thenReturn(mappedResponses);

        CommentPageResponse result = commentService.getComments(assignmentId, limit, null);

        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.items()).isEqualTo(mappedResponses);

        verify(repository).getFirstPage(eq(assignmentId), any(Pageable.class));
        verify(repository, never()).getNextPage(any(), any(), any(), any());
    }

    @Test
    @DisplayName("getComments: первая страница, есть следующая → hasNext=true и nextCursor сформирован")
    void getComments_whenFirstPageHasMore_returnsNextCursor() {
        int limit = 2;

        CommentEntity c1 = CommentEntity.builder()
                .id(UUID.randomUUID())
                .text("first")
                .createdAt(Instant.parse("2025-01-01T10:00:00Z"))
                .build();
        CommentEntity c2 = CommentEntity.builder()
                .id(UUID.randomUUID())
                .text("second")
                .createdAt(Instant.parse("2025-01-01T11:00:00Z"))
                .build();
        CommentEntity c3 = CommentEntity.builder()
                .id(UUID.randomUUID())
                .text("third")
                .createdAt(Instant.parse("2025-01-01T12:00:00Z"))
                .build();

        List<CommentResponse> mappedResponses = List.of(mock(CommentResponse.class), mock(CommentResponse.class));

        when(identityService.getCurrentUserId()).thenReturn(teacherId);
        when(assignmentsService.getAssignmentEntityById(assignmentId)).thenReturn(assignment);
        when(repository.getFirstPage(eq(assignmentId), any(Pageable.class)))
                .thenReturn(new ArrayList<>(List.of(c1, c2, c3)));
        when(commentMapper.toResponseList(anyList())).thenReturn(mappedResponses);

        CommentPageResponse result = commentService.getComments(assignmentId, limit, null);

        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(c2.getCreatedAt() + "," + c2.getId());
        assertThat(result.items()).isEqualTo(mappedResponses);

        ArgumentCaptor<List<CommentEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(commentMapper).toResponseList(captor.capture());

        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).containsExactly(c1, c2);

        verify(repository, never()).getNextPage(any(), any(), any(), any());
    }

    @Test
    @DisplayName("getComments: первая страница, следующей нет → hasNext=false, nextCursor=null")
    void getComments_whenRepositoryReturnsExactlyLimit_returnsHasNextFalseAndNullCursor() {
        int limit = 7;

        List<CommentEntity> comments = new ArrayList<>();

        for (int i = 0; i < limit; i++) {
            CommentEntity x = CommentEntity.builder()
                    .id(UUID.randomUUID())
                    .author(teacher)
                    .assignment(assignment)
                    .text(Integer.toString(i))
                    .createdAt(Instant.parse(String.format("2025-01-01T1%d:00:00Z", i)))
                    .build();
            comments.add(x);
        }

        List<CommentResponse> mappedResponses = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            mappedResponses.add(mock(CommentResponse.class));
        }

        Instant cursorTime = Instant.parse("2025-01-01T09:00:00Z");
        UUID cursorId = UUID.randomUUID();
        String cursor = cursorTime + "," + cursorId;

        when(identityService.getCurrentUserId()).thenReturn(teacherId);
        when(assignmentsService.getAssignmentEntityById(assignmentId)).thenReturn(assignment);
        when(repository.getNextPage(eq(assignmentId), eq(cursorTime), eq(cursorId), any(Pageable.class)))
                .thenReturn(new ArrayList<>(comments));
        when(commentMapper.toResponseList(anyList())).thenReturn(mappedResponses);

        CommentPageResponse result = commentService.getComments(assignmentId, limit, cursor);

        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.items()).isEqualTo(mappedResponses);

        ArgumentCaptor<List<CommentEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(commentMapper).toResponseList(captor.capture());

        assertThat(captor.getValue()).hasSize(limit);
        assertThat(captor.getValue()).containsAll(comments);

        verify(repository, never()).getFirstPage(any(), any());
    }

    @Test
    @DisplayName("getComments: валидный курсор → вызывает getNextPage с правильными аргументами")
    void getComments_whenCursorIsValid_callsGetNextPageWithParsedArgs() {
        int limit = 10;
        Instant cursorTime = Instant.parse("2025-01-01T10:00:00Z");
        UUID cursorId = UUID.randomUUID();
        String cursor = cursorTime + "," + cursorId;

        when(identityService.getCurrentUserId()).thenReturn(teacherId);
        when(assignmentsService.getAssignmentEntityById(assignmentId)).thenReturn(assignment);
        when(repository.getNextPage(eq(assignmentId), eq(cursorTime), eq(cursorId), any(Pageable.class)))
                .thenReturn(new ArrayList<>());
        when(commentMapper.toResponseList(anyList())).thenReturn(List.of());

        CommentPageResponse result = commentService.getComments(assignmentId, limit, cursor);

        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.items()).isEmpty();

        verify(repository).getNextPage(eq(assignmentId), eq(cursorTime), eq(cursorId), any(Pageable.class));
        verify(repository, never()).getFirstPage(any(), any());
    }

    @Test
    @DisplayName("getComments: битый курсор → IllegalArgumentException")
    void getComments_whenCursorIsInvalid_throwsIllegalArgument() {
        when(identityService.getCurrentUserId()).thenReturn(teacherId);
        when(assignmentsService.getAssignmentEntityById(assignmentId)).thenReturn(assignment);

        assertThatThrownBy(() -> commentService.getComments(assignmentId, 10, "broken-cursor"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid cursor format");

        verify(repository, never()).getFirstPage(any(), any());
        verifyNoInteractions(commentMapper);
    }

    @Test
    @DisplayName("getComments: посторонний пользователь получает AccessDeniedException")
    void getComments_whenUserNotInConnection_throwsAccessDenied() {
        UUID strangerId = UUID.randomUUID();

        when(identityService.getCurrentUserId()).thenReturn(strangerId);
        when(assignmentsService.getAssignmentEntityById(assignmentId)).thenReturn(assignment);

        assertThatThrownBy(() -> commentService.getComments(assignmentId, 10, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only the connection member");

        verifyNoInteractions(repository, commentMapper);
    }

    @Test
    @DisplayName("getComments: пустой результат → hasNext=false, nextCursor=null")
    void getComments_whenRepositoryReturnsEmpty_returnsEmptyResponse() {
        when(identityService.getCurrentUserId()).thenReturn(teacherId);
        when(assignmentsService.getAssignmentEntityById(assignmentId)).thenReturn(assignment);
        when(repository.getFirstPage(eq(assignmentId), any(Pageable.class))).thenReturn(new ArrayList<>());
        when(commentMapper.toResponseList(anyList())).thenReturn(List.of());

        CommentPageResponse result = commentService.getComments(assignmentId, 5, null);

        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("getComments: невалидный курсор '67.67.6767,67' → IllegalArgumentException")
    void getComments_whenCursorHasInvalidFormat_throwsIllegalArgument() {
        String invalidCursor = "67.67.6767,67";

        when(identityService.getCurrentUserId()).thenReturn(teacherId);
        when(assignmentsService.getAssignmentEntityById(assignmentId)).thenReturn(assignment);

        assertThatThrownBy(() -> commentService.getComments(assignmentId, 10, invalidCursor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid cursor format");

        verify(repository, never()).getFirstPage(any(), any());
        verify(repository, never()).getNextPage(any(), any(), any(), any());
        verifyNoInteractions(commentMapper);
    }
}
