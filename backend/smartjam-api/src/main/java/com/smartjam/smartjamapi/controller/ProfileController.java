package com.smartjam.smartjamapi.controller;

import com.smartjam.api.api.ProfileApi;
import com.smartjam.api.model.UserAvatarResponse;
import com.smartjam.api.model.UserProfileUpdateRequest;
import com.smartjam.api.model.UserResponse;
import com.smartjam.smartjamapi.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProfileController implements ProfileApi {

    private final ProfileService profileService;

    @Override
    public ResponseEntity<UserResponse> getCurrentUserProfile() {
        log.info("Calling getCurrentUserProfile");
        return ResponseEntity.ok(profileService.getCurrentUserProfile());
    }

    @Override
    public ResponseEntity<UserAvatarResponse> updateCurrentUserProfile(UserProfileUpdateRequest body) {
        log.info("Calling updateCurrentUserProfile");
        return ResponseEntity.ok(profileService.updateCurrentUserProfile(body));
    }
}
