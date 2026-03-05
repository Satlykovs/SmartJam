package com.smartjam.smartjamanalyzer.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TempWorkspace implements AutoCloseable {
    private final List<Path> filesToClean = new ArrayList<>();

    /**
     * Регистрирует файл в рабочем пространстве.
     *
     * @param path Путь к файлу для регистрации.
     * @return Возвращает тот же путь (для удобства)
     */
    public Path register(Path path) {
        if (path != null) {
            filesToClean.add(path);
        }
        return path;
    }

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
