package com.smartjam.smartjamanalyzer.domain.port;

import java.nio.file.Path;

/**
 * Service responsible for orchestrating audio file processing using FFmpeg. It standardizes incoming audio to a unified
 * format (WAV, Mono, 44100Hz) and ensures safe OS-level process management to prevent resource leaks.
 */
public interface AudioConverter {
    /**
     * Converts an audio file to a standardized mono WAV format (44100Hz).
     *
     * @param inputFile The path to the source audio file.
     * @param workspace The workspace responsible for the lifecycle of temporary files.
     * @return A {@link Path} to the successfully processed WAV file.
     * @throws RuntimeException if the process fails or times out.
     */
    Path convertToStandardWav(Path inputFile, Workspace workspace);
}
