package com.smartjam.smartjamanalyzer.application;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.smartjamanalyzer.domain.model.FeatureSequence;
import com.smartjam.smartjamanalyzer.domain.port.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AudioAnalysisUseCaseTest {
    private final String VALID_UUID_STR = "11111111-1111-1111-1111-111111111111";
    private final UUID VALID_UUID = UUID.fromString(VALID_UUID_STR);

    @Mock
    private AudioStorage storage;

    @Mock
    private AudioConverter converter;

    @Mock
    private WorkspaceFactory workspaceFactory;

    @Mock
    private Workspace workspace;

    @Mock
    private FeatureExtractor featureExtractor;

    @Mock
    private PerformanceEvaluator performanceEvaluator;

    @Mock
    private ReferenceRepository referenceRepository;

    @Mock
    private ResultRepository resultRepository;

    @Mock
    private DebugVisualizer debugVisualizer;

    @InjectMocks
    private AudioAnalysisUseCase useCase;

    @Test
    @DisplayName("UseCase должен сначала скачать, потом конвертировать")
    void shouldProcessInOrder() {
        String bucket = "references";
        String fileKey = VALID_UUID_STR + ".m4a";
        Path mockPath = Path.of("input");
        Path mockWav = Path.of("output");
        FeatureSequence mockSeq = new FeatureSequence(List.of(new float[84]), 20f);

        when(workspaceFactory.create()).thenReturn(workspace);
        when(storage.downloadAudioFile(eq(bucket), eq(fileKey), any())).thenReturn(mockPath);
        when(converter.convertToStandardWav(eq(mockPath), any())).thenReturn(mockWav);
        when(featureExtractor.extract(mockWav)).thenReturn(mockSeq);

        useCase.execute(bucket, fileKey);

        InOrder inOrder = inOrder(referenceRepository, storage, converter, featureExtractor);

        inOrder.verify(referenceRepository).updateStatus(VALID_UUID, AudioProcessingStatus.ANALYZING, null);

        inOrder.verify(storage).downloadAudioFile(eq(bucket), eq(fileKey), any());
        inOrder.verify(converter).convertToStandardWav(eq(mockPath), any());

        inOrder.verify(featureExtractor).extract(mockWav);
        inOrder.verify(referenceRepository).save(VALID_UUID, mockSeq);
    }

    @Test
    @DisplayName("UseCase должен бросать ошибку, если конвертация зависла и писать FAILED в БД")
    void shouldThrowExceptionWhenConverterTimesOut() {
        when(workspaceFactory.create()).thenReturn(workspace);
        when(storage.downloadAudioFile(any(), any(), any())).thenReturn(Path.of("input"));
        when(converter.convertToStandardWav(any(), any())).thenThrow(new RuntimeException("FFmpeg timeout exceeded"));

        assertThrows(RuntimeException.class, () -> useCase.execute("submissions", VALID_UUID_STR));

        verify(resultRepository).updateStatus(VALID_UUID, AudioProcessingStatus.FAILED, "FFmpeg timeout exceeded");
    }

    @Test
    @DisplayName("UseCase должен оборачивать ошибку скачивания в свою ошибку")
    void shouldWrapStorageException() {
        String errorMessage = "MinIO is down";

        when(workspaceFactory.create()).thenReturn(workspace);
        when(storage.downloadAudioFile(any(), any(), any())).thenThrow(new RuntimeException(errorMessage));

        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> useCase.execute("references", VALID_UUID_STR));

        assertTrue(exception.getMessage().contains("Business logic failed"));
        assertEquals(errorMessage, exception.getCause().getMessage());

        verify(referenceRepository).updateStatus(VALID_UUID, AudioProcessingStatus.FAILED, errorMessage);
    }

    @Test
    @DisplayName("UseCase должен выкинуть исключение, если ключ не UUID")
    void shouldThrowOnInvalidUuidKey() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute("submissions", "not-a-uuid.mp3"));
    }
}
