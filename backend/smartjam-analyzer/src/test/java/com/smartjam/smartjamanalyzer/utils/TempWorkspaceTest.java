package com.smartjam.smartjamanalyzer.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TempWorkspaceTest {
    @Test
    void shouldDeleteRegistredFilesOnClose() throws IOException {
        Path file = Files.createTempFile("test", ".tmp");
        assertTrue(Files.exists(file), "Файл должен быть создан перед тестом");

        try (TempWorkspace workspace = new TempWorkspace()) {
            workspace.register(file);

            assertTrue(Files.exists(file), "Файл должен существовать внутри try блока");
        }

        assertFalse(Files.exists(file), "Файл должен быть удален после выхода из try блока");
    }
}
