package com.smartjam.smartjamanalyzer.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * A short-lived temporary workspace that tracks files created via {@link #createTempFile(String, String)} and deletes
 * them automatically when the workspace is closed.
 *
 * <p>Typical usage with try-with-resources:
 *
 * <pre>{@code
 * try (TempWorkspace workspace = new TempWorkspace()) {
 *     Path file = workspace.createTempFile("prefix_", ".wav");
 *     // use file ...
 * } // all registered files are deleted here
 * }</pre>
 *
 * <p><b>Thread-safety:</b> This class is <em>not</em> thread-safe. Instances must not be shared across threads without
 * external synchronisation.
 *
 * <p><b>Close behaviour:</b> {@link #close()} performs best-effort cleanup. Deletion failures are logged as warnings
 * and swallowed; no exception is thrown from {@code close()}.
 */
@Slf4j
public class TempWorkspace implements AutoCloseable {
    private final List<Path> filesToClean = new ArrayList<>();

    /**
     * Создаёт временный файл в рабочем пространстве, регистрирует его на автоматическое удаление при закрытии
     * workspace-а.
     *
     * @param prefix Префикс пути к файлу.
     * @param suffix Суффикс пути к файлу.
     * @return Возвращает зарегистрированный путь.
     */
    public Path createTempFile(String prefix, String suffix) throws IOException {
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
