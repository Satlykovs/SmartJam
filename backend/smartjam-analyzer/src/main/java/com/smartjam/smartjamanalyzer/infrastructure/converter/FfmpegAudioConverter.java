package com.smartjam.smartjamanalyzer.infrastructure.converter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.smartjam.smartjamanalyzer.domain.port.AudioConverter;
import com.smartjam.smartjamanalyzer.domain.port.Workspace;
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
 * FFmpeg-based implementation of {@link AudioConverter}. Ensures safe OS-level process management to prevent resource
 * leaks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class FfmpegAudioConverter implements AudioConverter {

    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;

    @Value("${converter.audio-filter}")
    private String audioFilter;

    @Override
    public Path convertToStandardWav(Path inputFile, Workspace workspace) {
        String inputPathStr = inputFile.toAbsolutePath().toString();
        log.info("Starting conversion with filters: {}", audioFilter);

        CompletableFuture<Void> future = null;
        CapturingProcessFunction ffmpegFn = new CapturingProcessFunction();
        CapturingProcessFunction ffprobeFn = new CapturingProcessFunction();
        Path outputPath = null;

        try {
            outputPath = workspace.allocate("smartjam_clean_", ".wav");
            String outputPathStr = outputPath.toAbsolutePath().toString();

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
            future =
                    CompletableFuture.runAsync(() -> executor.createJob(builder).run());

            future.get(3, TimeUnit.MINUTES);

            return outputPath;

        } catch (Exception e) {
            if (future != null) {
                future.cancel(true);
            }
            ffmpegFn.stop();
            ffprobeFn.stop();

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                log.error("Conversion interrupted: {}", inputPathStr);
            } else if (e instanceof TimeoutException) {
                log.error("FFmpeg timeout (3 min) for file: {}", inputPathStr);
            } else {
                log.error("FFmpeg error: {}", e.getMessage());
            }

            throw new RuntimeException("Conversion failed", e instanceof ExecutionException ? e.getCause() : e);
        }
    }

    /**
     * An internal implementation of {@link ProcessFunction} that captures the native OS process handle to allow
     * forceful termination.
     */
    private static class CapturingProcessFunction implements ProcessFunction {
        private final AtomicReference<Process> current = new AtomicReference<>();

        /**
         * Starts the process and captures its handle.
         *
         * @param args Command line arguments.
         * @return The started process.
         * @throws IOException if startup fails.
         */
        @Override
        public Process run(List<String> args) throws IOException {
            Process p = new ProcessBuilder(args).start();
            current.set(p);
            return p;
        }

        /** Forcibly destroys the captured process if it is still alive. */
        public void stop() {
            Process p = current.getAndSet(null);
            if (p != null && p.isAlive()) {
                p.destroyForcibly();
            }
        }
    }
}
