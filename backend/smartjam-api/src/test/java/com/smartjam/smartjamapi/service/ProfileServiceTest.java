package com.smartjam.smartjamapi.service;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.smartjam.api.model.UserAvatarResponse;
import com.smartjam.api.model.UserProfileUpdateRequest;
import com.smartjam.api.model.UserResponse;
import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.exception.UserAlreadyExistsException;
import com.smartjam.smartjamapi.mapper.UserMapper;
import com.smartjam.smartjamapi.repository.UserRepository;
import com.smartjam.smartjamapi.security.IdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService")
class ProfileServiceTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private UserRepository repository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private ProfileService profileService;

    private UUID userId;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = new UserEntity();
        user.setId(userId);
        user.setUsername("john_doe");
        user.setFirstName("John");
        user.setLastName("Doe");
    }

    // ============================================================
    // getCurrentUserProfile
    // ============================================================

    @Test
    @DisplayName("getCurrentUserProfile: пользователь найден → возвращает UserResponse")
    void getCurrentUserProfile_whenUserExists_returnsUserResponse() {
        UserResponse expectedResponse = mock(UserResponse.class);

        when(identityService.getCurrentUserId()).thenReturn(userId);
        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(expectedResponse);

        UserResponse actual = profileService.getCurrentUserProfile();

        assertThat(actual).isSameAs(expectedResponse);

        verify(identityService).getCurrentUserId();
        verify(repository).findById(userId);
        verify(userMapper).toUserResponse(user);
        verifyNoMoreInteractions(repository, userMapper, s3Service);
    }

    @Test
    @DisplayName("getCurrentUserProfile: пользователь не найден → IllegalStateException")
    void getCurrentUserProfile_whenUserNotFound_throwsIllegalState() {
        when(identityService.getCurrentUserId()).thenReturn(userId);
        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getCurrentUserProfile())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Authenticated user not found");

        verifyNoInteractions(userMapper, s3Service);
    }

    // ============================================================
    // updateCurrentUserProfile
    // ============================================================

    @Test
    @DisplayName("updateCurrentUserProfile: пользователь не найден → IllegalStateException")
    void updateCurrentUserProfile_whenUserNotFound_throwsIllegalState() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("new_username", "New", "Name", false);

        when(identityService.getCurrentUserId()).thenReturn(userId);
        when(repository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.updateCurrentUserProfile(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Authenticated user not found");

        verifyNoInteractions(userMapper, s3Service);
    }

    @Test
    @DisplayName("updateCurrentUserProfile: новый username свободен → username обновляется")
    void updateCurrentUserProfile_whenNewUsernameIsAvailable_updatesUsername() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("new_username", null, null, false);

        when(identityService.getCurrentUserId()).thenReturn(userId);
        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.existsByUsername("new_username")).thenReturn(false);

        UserAvatarResponse result = profileService.updateCurrentUserProfile(request);

        assertThat(result.avatarUrl()).isNull();
        assertThat(user.getUsername()).isEqualTo("new_username");

        verify(repository).existsByUsername("new_username");
        verifyNoInteractions(s3Service);
    }

    @Test
    @DisplayName("updateCurrentUserProfile: username совпадает с текущим → existsByUsername не вызывается")
    void updateCurrentUserProfile_whenUsernameSameAsCurrent_doesNotCheckExistence() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("john_doe", null, null, false);

        when(identityService.getCurrentUserId()).thenReturn(userId);
        when(repository.findById(userId)).thenReturn(Optional.of(user));

        UserAvatarResponse result = profileService.updateCurrentUserProfile(request);

        assertThat(result.avatarUrl()).isNull();
        assertThat(user.getUsername()).isEqualTo("john_doe");

        verify(repository, never()).existsByUsername(any());
        verifyNoInteractions(s3Service);
    }

    @Test
    @DisplayName("updateCurrentUserProfile: username уже занят → UserAlreadyExistsException")
    void updateCurrentUserProfile_whenUsernameAlreadyExists_throwsUserAlreadyExists() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("taken_username", null, null, false);

        when(identityService.getCurrentUserId()).thenReturn(userId);
        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.existsByUsername("taken_username")).thenReturn(true);

        assertThatThrownBy(() -> profileService.updateCurrentUserProfile(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Username already exists");

        assertThat(user.getUsername()).isEqualTo("john_doe"); // не изменился
        verifyNoInteractions(s3Service);
    }

    static Stream<Arguments> blankOrNullStringsProvider() {
        return Stream.of(arguments((String) null), arguments(""), arguments("   "), arguments("\t\n"));
    }

    @ParameterizedTest(name = "updateCurrentUserProfile: username = [{0}] не обновляется")
    @MethodSource("blankOrNullStringsProvider")
    void updateCurrentUserProfile_whenUsernameIsBlankOrNull_doesNotUpdate(String username) {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(username, null, null, false);

        when(identityService.getCurrentUserId()).thenReturn(userId);
        when(repository.findById(userId)).thenReturn(Optional.of(user));

        UserAvatarResponse result = profileService.updateCurrentUserProfile(request);

        assertThat(result.avatarUrl()).isNull();
        assertThat(user.getUsername()).isEqualTo("john_doe");

        verify(repository, never()).existsByUsername(any());
        verifyNoInteractions(s3Service);
    }

    @Test
    @DisplayName("updateCurrentUserProfile: новое firstName → обновляется")
    void updateCurrentUserProfile_whenFirstNameProvided_updatesFirstName() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(null, "Jonathan", null, false);

        when(identityService.getCurrentUserId()).thenReturn(userId);
        when(repository.findById(userId)).thenReturn(Optional.of(user));

        UserAvatarResponse result = profileService.updateCurrentUserProfile(request);

        assertThat(result.avatarUrl()).isNull();
        assertThat(user.getFirstName()).isEqualTo("Jonathan");
        assertThat(user.getLastName()).isEqualTo("Doe"); // не тронуто

        verifyNoInteractions(s3Service);
    }

    @ParameterizedTest(name = "updateCurrentUserProfile: firstName = [{0}] не обновляется")
    @MethodSource("blankOrNullStringsProvider")
    void updateCurrentUserProfile_whenFirstNameIsBlankOrNull_doesNotUpdate(String firstName) {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(null, firstName, null, false);

        when(identityService.getCurrentUserId()).thenReturn(userId);
        when(repository.findById(userId)).thenReturn(Optional.of(user));

        profileService.updateCurrentUserProfile(request);

        assertThat(user.getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("updateCurrentUserProfile: новое lastName → обновляется")
    void updateCurrentUserProfile_whenLastNameProvided_updatesLastName() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(null, null, "Smith", false);

        when(identityService.getCurrentUserId()).thenReturn(userId);
        when(repository.findById(userId)).thenReturn(Optional.of(user));

        UserAvatarResponse result = profileService.updateCurrentUserProfile(request);

        assertThat(result.avatarUrl()).isNull();
        assertThat(user.getLastName()).isEqualTo("Smith");
        assertThat(user.getFirstName()).isEqualTo("John"); // не тронуто
    }

    @ParameterizedTest(name = "updateCurrentUserProfile: lastName = [{0}] не обновляется")
    @MethodSource("blankOrNullStringsProvider")
    void updateCurrentUserProfile_whenLastNameIsBlankOrNull_doesNotUpdate(String lastName) {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(null, null, lastName, false);

        when(identityService.getCurrentUserId()).thenReturn(userId);
        when(repository.findById(userId)).thenReturn(Optional.of(user));

        profileService.updateCurrentUserProfile(request);

        assertThat(user.getLastName()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("updateCurrentUserProfile: avatarUpdated=true → генерирует presigned URL")
    void updateCurrentUserProfile_whenAvatarUpdatedTrue_generatesPresignedUrl() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(null, null, null, true);
        String expectedKey = "temp-avatars/" + userId;
        String expectedUrl = "http://minio:9000/temp-avatars/upload?signature=abc";

        when(identityService.getCurrentUserId()).thenReturn(userId);
        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(s3Service.getTempAvatarsKey(userId)).thenReturn(expectedKey);
        when(s3Service.generatePresignedUrlForUserAvatar(expectedKey)).thenReturn(expectedUrl);

        UserAvatarResponse result = profileService.updateCurrentUserProfile(request);

        assertThat(result.avatarUrl()).isEqualTo(expectedUrl);

        verify(s3Service).getTempAvatarsKey(userId);
        verify(s3Service).generatePresignedUrlForUserAvatar(expectedKey);
    }

    @Test
    @DisplayName("updateCurrentUserProfile: avatarUpdated=false → presigned URL не генерируется")
    void updateCurrentUserProfile_whenAvatarUpdatedFalse_doesNotGenerateUrl() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(null, null, null, false);

        when(identityService.getCurrentUserId()).thenReturn(userId);
        when(repository.findById(userId)).thenReturn(Optional.of(user));

        UserAvatarResponse result = profileService.updateCurrentUserProfile(request);

        assertThat(result.avatarUrl()).isNull();
        verifyNoInteractions(s3Service);
    }

    @Test
    @DisplayName("updateCurrentUserProfile: avatarUpdated=null → presigned URL не генерируется")
    void updateCurrentUserProfile_whenAvatarUpdatedNull_doesNotGenerateUrl() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(null, null, null, null);

        when(identityService.getCurrentUserId()).thenReturn(userId);
        when(repository.findById(userId)).thenReturn(Optional.of(user));

        UserAvatarResponse result = profileService.updateCurrentUserProfile(request);

        assertThat(result.avatarUrl()).isNull();
        verifyNoInteractions(s3Service);
    }

    @Test
    @DisplayName("updateCurrentUserProfile: все поля сразу → обновляет всё + генерирует URL")
    void updateCurrentUserProfile_whenAllFieldsProvided_updatesAllAndGeneratesUrl() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("new_user", "Alex", "Brown", true);
        String expectedKey = "temp-avatars/" + userId;
        String expectedUrl = "http://minio:9000/upload";

        when(identityService.getCurrentUserId()).thenReturn(userId);
        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.existsByUsername("new_user")).thenReturn(false);
        when(s3Service.getTempAvatarsKey(userId)).thenReturn(expectedKey);
        when(s3Service.generatePresignedUrlForUserAvatar(expectedKey)).thenReturn(expectedUrl);

        UserAvatarResponse result = profileService.updateCurrentUserProfile(request);

        assertThat(result.avatarUrl()).isEqualTo(expectedUrl);
        assertThat(user.getUsername()).isEqualTo("new_user");
        assertThat(user.getFirstName()).isEqualTo("Alex");
        assertThat(user.getLastName()).isEqualTo("Brown");

        verify(repository).existsByUsername("new_user");
        verify(s3Service).getTempAvatarsKey(userId);
        verify(s3Service).generatePresignedUrlForUserAvatar(expectedKey);
    }

    @Test
    @DisplayName("updateCurrentUserProfile: запрос со всеми null → ничего не меняется, URL=null")
    void updateCurrentUserProfile_whenAllFieldsNull_changesNothingAndReturnsNullUrl() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(null, null, null, null);

        when(identityService.getCurrentUserId()).thenReturn(userId);
        when(repository.findById(userId)).thenReturn(Optional.of(user));

        UserAvatarResponse result = profileService.updateCurrentUserProfile(request);

        assertThat(result.avatarUrl()).isNull();
        assertThat(user.getUsername()).isEqualTo("john_doe");
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");

        verify(repository, never()).existsByUsername(any());
        verifyNoInteractions(s3Service);
    }
}
