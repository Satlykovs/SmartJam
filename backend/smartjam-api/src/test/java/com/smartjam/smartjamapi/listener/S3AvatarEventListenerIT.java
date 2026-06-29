package com.smartjam.smartjamapi.listener;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

import com.smartjam.common.BaseIntegrationTest;
import com.smartjam.smartjamapi.entity.UserEntity;
import com.smartjam.smartjamapi.repository.UserRepository;
import com.smartjam.smartjamapi.service.S3Service;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
        properties = {
            "minio.buckets.temp-avatars=temp-avatars",
            "minio.buckets.avatars=avatars",
            "minio.format-avatar.jpeg=jpeg",
            "minio.format-avatar.jpg=jpg",
            "minio.format-avatar.png=png"
        })
@Slf4j
class S3AvatarEventListenerIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private MinioClient minioClient;

    @Test
    void shouldSuccessfullyProcessAvatarLifecycle() throws Exception {

        UserEntity user = new UserEntity();
        String unique = UUID.randomUUID().toString().substring(0, 8);

        user.setUsername("tester_" + unique);
        user.setEmail("tester_" + unique + "@smartjam.com");
        user.setPasswordHash("hash");

        userRepository.saveAndFlush(user);
        UUID userId = user.getId();

        String fullTempKey = s3Service.getTempAvatarsKey(userId);
        String relativeKey = fullTempKey.substring(fullTempKey.indexOf("/") + 1);

        byte[] validImage;

        try (InputStream is = getClass().getResourceAsStream("/sigma.jpeg")) {
            if (is == null) {
                throw new IllegalStateException("sigma.jpeg not found in test resources");
            }
            validImage = is.readAllBytes();
        }

        minioClient.putObject(PutObjectArgs.builder().bucket("temp-avatars").object(relativeKey).stream(
                        new ByteArrayInputStream(validImage), validImage.length, -1)
                .contentType("image/jpeg")
                .build());

        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    UserEntity updated = userRepository.findById(userId).orElseThrow();
                    assertThat(updated.getAvatarUrl()).isNotNull();
                });
    }
}
