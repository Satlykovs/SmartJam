package com.smartjam.smartjamapi.controller;

import jakarta.validation.Valid;

import com.smartjam.smartjamapi.dto.UploadRequest;
import com.smartjam.smartjamapi.dto.UploadUrlResponse;
import com.smartjam.smartjamapi.service.UploadService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/upload-url")
    public ResponseEntity<UploadUrlResponse> getUploadUrl(@RequestBody @Valid UploadRequest request) {
        return ResponseEntity.ok(uploadService.generateUploadUrl(request.fileName()));
    }
}
