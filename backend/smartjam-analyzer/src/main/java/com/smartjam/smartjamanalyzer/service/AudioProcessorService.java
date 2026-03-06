package com.smartjam.smartjamanalyzer.service;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.smartjam.smartjamanalyzer.utils.TempWorkspace;
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

    /**
     * Метод для конвертации скачанного аудиофайла. Конвертирует его в стандартизированный тип MONO WAV, применяет
     * фильтры, обрезая лишние частоты и нормализуя громкость.
     *
     * @param inputFile Путь к файлу для обработки
     * @param workspace Временное рабочее пространство, которое создает и очищает временные файлы
     * @return Path к обработанному файлу
     */
    public Path convertToStandardWav(Path inputFile, TempWorkspace workspace) {
        String inputPathStr = inputFile.toAbsolutePath().toString();

        log.info("Конвертация с фильтрами: {}", audioFilter);

        try {

            Path outputPath = workspace.createTempFile("smartjam_clean_", ".wav");

            String outputPathStr = outputPath.toAbsolutePath().toString();

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

            CompletableFuture.runAsync(() -> executor.createJob(builder).run()).get(3, TimeUnit.MINUTES);

            return outputPath;
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("FFmpeg завис (таймаут 3 минуты) для файла: {}", inputPathStr);
            throw new RuntimeException("FFmpeg timeout exceeded", e);
        } catch (Exception e) {
            log.error("Ошибка при конвертации в FFmpeg: {}", e.getMessage(), e);
            throw new RuntimeException("FFmpeg conversion failed", e);
        }
    }
}
