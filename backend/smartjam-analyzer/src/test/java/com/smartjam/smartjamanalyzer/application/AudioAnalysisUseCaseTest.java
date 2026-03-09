package com.smartjam.smartjamanalyzer.application;

import java.nio.file.Path;

import com.smartjam.smartjamanalyzer.domain.port.AudioConverter;
import com.smartjam.smartjamanalyzer.domain.port.AudioStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioAnalysisUseCaseTest {
    @Mock
    private AudioStorage storage;

    @Mock
    private AudioConverter converter;

    @InjectMocks
    private AudioAnalysisUseCase useCase;

    @Test
    @DisplayName("UseCase должен сначала скачать, потом конвертировать")
    void shouldProcessInOrder() throws Exception {
        String bucket = "sub";
        String key = "test.mp3";
        Path mockPath = Path.of("input");
        Path mockClean = Path.of("output");

        when(storage.downloadAudioFile(eq(bucket), eq(key), any())).thenReturn(mockPath);
        when(converter.convertToStandardWav(eq(mockPath), any())).thenReturn(mockClean);

        useCase.execute(bucket, key);

        InOrder inOrder = inOrder(storage, converter);
        inOrder.verify(storage).downloadAudioFile(eq(bucket), eq(key), any());
        inOrder.verify(converter).convertToStandardWav(eq(mockPath), any());
    }

    @Test
    @DisplayName("UseCase должен бросать ошибку, если конвертация зависла")
    void shouldThrowExceptionWhenConverterTimesOut() {
        Path mockPath = Path.of("input");
        when(storage.downloadAudioFile(any(), any(), any())).thenReturn(mockPath);
        when(converter.convertToStandardWav(any(), any()))
                .thenThrow(new RuntimeException("FFmpeg timeout exceeded"));

        assertThrows(RuntimeException.class, () -> useCase.execute("sub", "key.mp3"));
    }

    @Test
    @DisplayName("UseCase должен оборачивать ошибку скачивания в свою бизнес-ошибку")
    void shouldWrapStorageException() {
        when(storage.downloadAudioFile(any(), any(), any()))
                .thenThrow(new RuntimeException("MinIO is down"));

        assertThrows(RuntimeException.class, () -> useCase.execute("sub", "key.mp3"));
    }
}
