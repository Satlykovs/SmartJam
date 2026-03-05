package com.smartjam.smartjamanalyzer.service;

import java.nio.file.Path;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioProcessorService {
    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;

    @Value("${analyzer.audio-filter}")
    private String audioFilter;

    public Path convertToStandardWav(Path inputFile) {
        String inputPathStr = inputFile.toAbsolutePath().toString();

        String outputPathStr = inputPathStr + "_clean.wav";

        log.info("Конвертация с фильтрами: {}", audioFilter);

        try {
            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(inputPathStr)
                    .overrideOutputFiles(true)
                    .addOutput(outputPathStr)
                    .setFormat("wav")
                    .setAudioChannels(1)
                    .setAudioSampleRate(44100)
                    .setAudioFilter(audioFilter)
                    .done();

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

            executor.createJob(builder).run();

            return Path.of(outputPathStr);
        } catch (Exception e) {
            log.error("Ошибка при конвертации в FFmpeg: {}", e.getMessage());
            throw new RuntimeException("FFmpeg conversion failed", e);
        }
    }
}
