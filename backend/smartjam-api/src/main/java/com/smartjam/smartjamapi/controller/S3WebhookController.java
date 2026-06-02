package com.smartjam.smartjamapi.controller;

import com.smartjam.smartjamapi.config.MinioProperties;
import com.smartjam.smartjamapi.record.S3WebhookPayload;
import com.smartjam.smartjamapi.service.S3WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/s3-avatar-webhook")
@Slf4j
@RequiredArgsConstructor
public class S3WebhookController {

    private final MinioProperties minioProperties;
    private final S3WebhookService s3WebhookService;

    @PostMapping
    public ResponseEntity<Void> validateUserAvatar(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody S3WebhookPayload payload) {

        log.info("Calling validateUserAvatar");
        String expectedHeader = "Bearer " + minioProperties.getWebhook().getMinioSecret();
        if (authHeader == null || !authHeader.equals(expectedHeader)) {
            log.warn("Попытка несанкционированного доступа к вебхуку! Переданный токен: {}", authHeader);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        s3WebhookService.validateUserAvatar(payload);

        return ResponseEntity.ok().build();
    }
}
