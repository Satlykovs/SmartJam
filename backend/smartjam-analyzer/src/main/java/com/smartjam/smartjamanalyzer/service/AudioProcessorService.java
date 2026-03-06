package com.smartjam.smartjamanalyzer.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.smartjam.smartjamanalyzer.utils.TempWorkspace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.ProcessFunction;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service responsible for orchestrating audio file processing using FFmpeg. It standardizes incoming audio to a unified
 * format (WAV, Mono, 44100Hz) and ensures safe OS-level process management to prevent resource leaks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AudioProcessorService {

    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;

    @Value("${analyzer.audio-filter}")
    private String audioFilter;

    /**
     * Converts an audio file to standardized mono WAV format (44100Hz) using FFmpeg. Applies noise reduction,
     * normalization, and frequency filtering.
     *
     * <p>Execution is performed in a separate thread with a strict 3-minute timeout.
     *
     * @param inputFile The path to the source audio file.
     * @param workspace The workspace responsible for the lifecycle of temporary files.
     * @return A {@link Path} to the successfully processed WAV file.
     * @throws RuntimeException if the process times out, is interrupted, or fails during conversion.
     */
    public Path convertToStandardWav(Path inputFile, TempWorkspace workspace) {
        String inputPathStr = inputFile.toAbsolutePath().toString();
        log.info("Конвертация с фильтрами: {}", audioFilter);

        try {
            Path outputPath = workspace.createTempFile("smartjam_clean_", ".wav");
            String outputPathStr = outputPath.toAbsolutePath().toString();

            CapturingProcessFunction ffmpegFn = new CapturingProcessFunction();
            CapturingProcessFunction ffprobeFn = new CapturingProcessFunction();

            FFmpeg boundFfmpeg = new FFmpeg(ffmpeg.getPath(), ffmpegFn);
            FFprobe boundFfprobe = new FFprobe(ffprobe.getPath(), ffprobeFn);

            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(inputPathStr)
                    .overrideOutputFiles(true)
                    .addOutput(outputPathStr)
                    .setFormat("wav")
                    .setAudioChannels(1)
                    .setAudioSampleRate(44100)
                    .setAudioFilter(audioFilter)
                    .done();

            FFmpegExecutor executor = new FFmpegExecutor(boundFfmpeg, boundFfprobe);
            CompletableFuture<Void> future =
                    CompletableFuture.runAsync(() -> executor.createJob(builder).run());

            try {
                future.get(3, TimeUnit.MINUTES);
            } catch (java.util.concurrent.TimeoutException e) {
                ffmpegFn.stop();
                ffprobeFn.stop();
                future.cancel(true);

                log.error("FFmpeg завис (таймаут 3 минуты) для файла: {}", inputPathStr);
                throw new RuntimeException("FFmpeg timeout exceeded", e);
            }

            return outputPath;

        } catch (Exception e) {
            if (e instanceof InterruptedException || e.getCause() instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            log.error("Ошибка при конвертации в FFmpeg: {}", e.getMessage(), e);
            throw new RuntimeException("FFmpeg conversion failed", e);
        }
    }

    /**
     * An internal implementation of {@link ProcessFunction} that captures the native OS process handle to allow
     * forceful termination.
     */
    private static class CapturingProcessFunction implements ProcessFunction {
        private final AtomicReference<Process> current = new AtomicReference<>();

        @Override
        public Process run(List<String> args) throws IOException {
            Process p = new ProcessBuilder(args).start();
            current.set(p);
            return p;
        }

        public void stop() {
            Process p = current.getAndSet(null);
            if (p != null && p.isAlive()) {
                p.destroyForcibly();
            }
        }
    }
}
