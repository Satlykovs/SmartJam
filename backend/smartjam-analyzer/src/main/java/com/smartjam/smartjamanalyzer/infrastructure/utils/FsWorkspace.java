package com.smartjam.smartjamanalyzer.infrastructure.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.smartjam.smartjamanalyzer.domain.port.Workspace;
import lombok.extern.slf4j.Slf4j;

/**
 * A short-lived temporary workspace that tracks files created via {@link #allocate(String, String)} and deletes them
 * automatically when the workspace is closed.
 *
 * <p><b>Thread-safety:</b> This class is <em>not</em> thread-safe. Instances must not be shared across threads without
 * external synchronisation.
 *
 * <p><b>Close behaviour:</b> {@link #close()} performs best-effort cleanup. Deletion failures are logged as warnings
 * and swallowed; no exception is thrown from {@code close()}.
 */
@Slf4j
public class FsWorkspace implements Workspace {
    private final List<Path> filesToClean = new ArrayList<>();

    /**
     * Creates a temporary file in the workspace and registers it for automatic deletion when the workspace is closed.
     *
     * @param prefix The prefix for the file path.
     * @param suffix The suffix for the file path.
     * @return The path to the registered temporary file.
     */
    public Path allocate(String prefix, String suffix) throws IOException {
        Path path = Files.createTempFile(prefix, suffix);
        filesToClean.add(path);
        return path;
    }

    /**
     * Deletes all temporary files registered in this workspace.
     *
     * <p>Cleanup is best-effort: if a file cannot be deleted, the failure is logged at {@code WARN} level and
     * processing continues with the remaining files. No exception is thrown. Calling {@code close()} on an empty
     * workspace is a no-op.
     */
    @Override
    public void close() {
        if (filesToClean.isEmpty()) return;

        log.debug("Очистка временного пространства. Файлов к удалению: {}", filesToClean.size());

        for (Path path : filesToClean) {
            try {
                if (Files.deleteIfExists(path)) {
                    log.debug("Удален временный файл: {}", path.getFileName());
                }
            } catch (Exception e) {
                log.warn("Не удалось удалить файл {}: {}", path.getFileName(), e.getMessage());
            }
        }
    }
}
