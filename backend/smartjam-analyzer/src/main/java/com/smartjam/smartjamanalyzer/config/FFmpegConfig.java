package com.smartjam.smartjamanalyzer.config;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FFmpegConfig {
    @Bean
    public FFmpeg fFmpeg() {
        try {
            return new FFmpeg("ffmpeg");
        } catch (IOException e) {
            log.error("FFmpeg не найден в PATH");
            throw new RuntimeException("FFmpeg init failed", e);
        }
    }

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
