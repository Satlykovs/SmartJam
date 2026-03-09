package com.smartjam.smartjamanalyzer.infrastructure.converter;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for initializing FFmpeg and FFprobe binaries. Provides {@link FFmpeg} and {@link FFprobe} beans
 * used for audio processing tasks.
 */
@Slf4j
@Configuration
public class FFmpegConfig {

    /**
     * Creates and initializes the FFmpeg instance.
     *
     * @return a configured {@link FFmpeg} instance.
     * @throws RuntimeException if the FFmpeg binary is not found in the system PATH.
     */
    @Bean
    public FFmpeg ffmpeg() {
        try {
            return new FFmpeg("ffmpeg");
        } catch (IOException e) {
            log.error("FFmpeg не найден в PATH");
            throw new RuntimeException("FFmpeg init failed", e);
        }
    }

    /**
     * Creates and initializes the FFprobe instance.
     *
     * @return a configured {@link FFprobe} instance.
     * @throws RuntimeException if the FFprobe binary is not found in the system PATH.
     */
    @Bean
    public FFprobe ffprobe() {
        try {
            return new FFprobe("ffprobe");
        } catch (IOException e) {
            log.error("FFprobe не найден в PATH");
            throw new RuntimeException("FFprobe init failed", e);
        }
    }
}
