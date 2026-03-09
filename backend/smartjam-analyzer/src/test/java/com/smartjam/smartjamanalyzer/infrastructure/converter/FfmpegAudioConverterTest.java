package com.smartjam.smartjamanalyzer.infrastructure.converter;

import java.io.IOException;
import java.nio.file.Path;

import com.smartjam.smartjamanalyzer.domain.port.Workspace;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// NOTE: добавить прокидывание фильтра сюда
@ExtendWith(MockitoExtension.class)
class FfmpegAudioConverterTest {

    @Mock
    private FFmpeg ffmpeg;

    @Mock
    private FFprobe ffprobe;

    @Mock
    private Workspace workspace;

    @InjectMocks
    private FfmpegAudioConverter converter;

    @Test
    @DisplayName("Должен упасть, если воркспейс не может выделить файл")
    void shouldFailWhenWorkspaceAllocationFails() throws IOException {
        Path inputPath = Path.of("src/test/resources/test.mp3");
        when(workspace.allocate(anyString(), anyString())).thenThrow(new IOException("Disk full"));

        RuntimeException ex =
                assertThrows(RuntimeException.class, () -> converter.convertToStandardWav(inputPath, workspace));

        assertTrue(ex.getMessage().contains("Conversion failed"));
        verify(workspace).allocate(anyString(), anyString());
    }
}
