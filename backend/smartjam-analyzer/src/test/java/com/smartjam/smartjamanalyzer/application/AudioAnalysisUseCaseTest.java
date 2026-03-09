package com.smartjam.smartjamanalyzer.application;

import com.smartjam.smartjamanalyzer.domain.port.AudioConverter;
import com.smartjam.smartjamanalyzer.domain.port.AudioStorage;
import com.smartjam.smartjamanalyzer.domain.port.Workspace;
import com.smartjam.smartjamanalyzer.domain.port.WorkspaceFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioAnalysisUseCaseTest
{
    @Mock
    private AudioStorage storage;

    @Mock
    private AudioConverter converter;

    @Mock
    private WorkspaceFactory workspaceFactory;

    @Mock
    private Workspace workspace;

    @InjectMocks
    private AudioAnalysisUseCase useCase;

    @Test
    @DisplayName("UseCase должен сначала скачать, потом конвертировать")
    void shouldProcessInOrder() throws Exception
    {
        String bucket = "sub";
        String key = "test.mp3";
        Path mockPath = Path.of("input");
        Path mockClean = Path.of("output");

        when(storage.downloadAudioFile(eq(bucket), eq(key), any())).thenReturn(mockPath);
        when(converter.convertToStandardWav(eq(mockPath), any())).thenReturn(mockClean);
        when(workspaceFactory.create()).thenReturn(workspace);

        useCase.execute(bucket, key);

        InOrder inOrder = inOrder(storage, converter);
        inOrder.verify(storage).downloadAudioFile(eq(bucket), eq(key), any());
        inOrder.verify(converter).convertToStandardWav(eq(mockPath), any());
    }

    @Test
    @DisplayName("UseCase должен бросать ошибку, если конвертация зависла")
    void shouldThrowExceptionWhenConverterTimesOut()
    {
        when(workspaceFactory.create()).thenReturn(workspace);
        when(storage.downloadAudioFile(any(), any(), any())).thenReturn(Path.of("input"));
        when(converter.convertToStandardWav(any(), any())).thenThrow(
                new RuntimeException("FFmpeg timeout exceeded"));

        assertThrows(RuntimeException.class, () -> useCase.execute("sub", "key.mp3"));
    }

    @Test
    @DisplayName("UseCase должен оборачивать ошибку скачивания в свою ошибку")
    void shouldWrapStorageException()
    {
        String errorMessage = "MinIO is down";

        when(workspaceFactory.create()).thenReturn(workspace);
        when(storage.downloadAudioFile(any(), any(), any())).thenThrow(
                new RuntimeException(errorMessage));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> useCase.execute("sub", "key.mp3"));

        assertTrue(exception.getMessage().contains("Business logic failed"));
        assertEquals(errorMessage, exception.getCause().getMessage());
    }
}
