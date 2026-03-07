package com.smartjam.smartjamanalyzer.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TempWorkspaceTest {
    @Test
    void shouldDeleteRegisteredFilesOnClose() throws IOException {
        Path filePath;
        try (TempWorkspace workspace = new TempWorkspace()) {
            Path file = workspace.createTempFile("test", ".tmp");
            filePath = file;

            assertTrue(Files.exists(file), "Файл должен существовать внутри try блока");
        }

        assertFalse(Files.exists(filePath), "Файл должен быть удален после выхода из try блока");
    }
}
