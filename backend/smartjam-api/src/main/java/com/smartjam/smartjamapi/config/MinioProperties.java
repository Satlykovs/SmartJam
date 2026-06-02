package com.smartjam.smartjamapi.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "minio")
@Getter
@Setter
@ToString(exclude = {"accessKey", "secretKey"})
public class MinioProperties {
    @NotBlank
    private String endpoint;

    @NotBlank
    private String publicEndpoint;

    @NotBlank
    private String accessKey;

    @NotBlank
    private String secretKey;

    @Valid
    @NotNull
    private Buckets buckets;

    @Getter
    @Setter
    public static class Buckets {
        @NotBlank
        private String references;

        @NotBlank
        private String submissions;

        @NotBlank
        private String avatars;

        @NotBlank
        private String tempAvatars;
    }

    @Valid
    @NotNull
    private Webhook webhook;

    @Getter
    @Setter
    public static class Webhook {
        @NotBlank
        private String minioSecret;
    }

    @Valid
    @NotNull
    private FormatAvatar formatAvatar;

    @Getter
    @Setter
    public static class FormatAvatar {
        @NotBlank
        private String jpeg;

        @NotBlank
        private String jpg;

        @NotBlank
        private String png;
    }
}
