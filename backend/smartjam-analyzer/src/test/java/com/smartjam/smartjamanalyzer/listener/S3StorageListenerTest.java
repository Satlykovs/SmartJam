package com.smartjam.smartjamanalyzer.listener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.smartjam.smartjamanalyzer.dto.S3EventDto;
import com.smartjam.smartjamanalyzer.service.AudioProcessorService;
import com.smartjam.smartjamanalyzer.service.StorageService;
import com.smartjam.smartjamanalyzer.utils.TempWorkspace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class S3StorageListenerTest {
    @Mock
    private StorageService storageService;

    @Mock
    private AudioProcessorService audioProcessorService;

    @InjectMocks
    private S3StorageListener listener;

    private S3EventDto createEvent(String bucket, String key) {
        return S3EventDto.builder()
                .records(List.of(S3EventDto.S3Record.builder()
                        .eventName("s3:ObjectCreated:Put")
                        .s3(S3EventDto.S3Data.builder()
                                .bucket(S3EventDto.Bucket.builder().name(bucket).build())
                                .object(S3EventDto.S3Object.builder().key(key).build())
                                .build())
                        .build()))
                .build();
    }

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Должен обработать файл из references и вызвать сервисы")
    void shouldProcessReferencesBucketCorrectly() throws IOException {
        String bucket = "references";
        String key = "teacher_riff.wav";
        S3EventDto event = createEvent(bucket, key);

        Path mockPath = tempDir.resolve("test.tmp");
        Files.createFile(mockPath);
        Path mockCleanPath = tempDir.resolve("test_clean.wav");
        Files.createFile(mockCleanPath);

        when(storageService.downloadAudioFile(eq(bucket), eq(key), any(TempWorkspace.class)))
                .thenReturn(mockPath);
        when(audioProcessorService.convertToStandardWav(eq(mockPath), any(TempWorkspace.class)))
                .thenReturn(mockCleanPath);

        listener.onFileUploaded(event, null);

        verify(storageService).downloadAudioFile(eq(bucket), eq(key), any(TempWorkspace.class));
        verify(audioProcessorService).convertToStandardWav(eq(mockPath), any(TempWorkspace.class));
    }

    @Test
    @DisplayName("Не должен падать, если StorageService выбросил ошибку")
    void shouldNotCrashWhenStorageServiceFails() {
        S3EventDto event = createEvent("references", "bad_file.wav");

        doThrow(new RuntimeException("S3 Down"))
                .when(storageService)
                .downloadAudioFile(anyString(), anyString(), any(TempWorkspace.class));

        assertThrows(RuntimeException.class, () -> listener.onFileUploaded(event, null));
        verifyNoInteractions(audioProcessorService);
    }

    @Test
    @DisplayName("Должен игнорировать пустые события")
    void shouldIgnoreEmptyEvents() {

        S3EventDto emptyEvent =
                S3EventDto.builder().records(Collections.emptyList()).build();
        S3EventDto nullEvent = S3EventDto.builder().records(null).build();

        listener.onFileUploaded(emptyEvent, null);
        listener.onFileUploaded(nullEvent, null);

        verifyNoInteractions(storageService);
        verifyNoInteractions(audioProcessorService);
    }
}
