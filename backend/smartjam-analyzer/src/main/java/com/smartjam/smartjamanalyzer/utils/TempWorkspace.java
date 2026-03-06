package com.smartjam.smartjamanalyzer.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

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
