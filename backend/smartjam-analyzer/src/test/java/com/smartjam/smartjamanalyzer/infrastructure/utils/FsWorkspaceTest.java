package com.smartjam.smartjamanalyzer.infrastructure.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.smartjam.smartjamanalyzer.domain.port.Workspace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FsWorkspaceTest {
    @Test
    void shouldDeleteRegisteredFilesOnClose() throws IOException {
        Path filePath;
        try (Workspace workspace = new FsWorkspace()) {
            Path file = workspace.allocate("test", ".tmp");
            filePath = file;

            assertTrue(Files.exists(file), "Файл должен существовать внутри try блока");
        }

        assertFalse(Files.exists(filePath), "Файл должен быть удален после выхода из try блока");
    }
}
