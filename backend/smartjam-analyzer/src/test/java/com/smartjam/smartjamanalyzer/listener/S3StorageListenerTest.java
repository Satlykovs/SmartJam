package com.smartjam.smartjamanalyzer.listener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.smartjam.smartjamanalyzer.dto.S3EventDto;
import com.smartjam.smartjamanalyzer.service.AudioProcessorService;
import com.smartjam.smartjamanalyzer.service.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    @DisplayName("Должен обработать файл из references и вызвать сервисы")
    void shouldProcessReferencesBucketCorrectly() throws IOException {
        String bucket = "references";
        String key = "teacher_riff.wav";
        S3EventDto event = createEvent(bucket, key);

        Path mockPath = Files.createTempFile("test", ".tmp");
        Path mockCleanPath = Files.createTempFile("test_clean", ".wav");

        when(storageService.downloadAudioFile(bucket, key)).thenReturn(mockPath);
        when(audioProcessorService.convertToStandardWav(mockPath)).thenReturn(mockCleanPath);

        listener.onFileUploaded(event);

        verify(storageService).downloadAudioFile(bucket, key);
        verify(audioProcessorService).convertToStandardWav(mockPath);
    }

    @Test
    @DisplayName("Не должен падать, если StorageService выбросил ошибку")
    void shouldNotCrashWhenStorageServiceFails() {
        S3EventDto event = createEvent("references", "bad_file.wav");

        doThrow(new RuntimeException("S3 Down")).when(storageService).downloadAudioFile(any(), any());

        listener.onFileUploaded(event);

        verifyNoInteractions(audioProcessorService);
    }

    @Test
    @DisplayName("Должен игнорировать пустые события")
    void shouldIgnoreEmptyEvents() {

        S3EventDto emptyEvent =
                S3EventDto.builder().records(Collections.emptyList()).build();
        S3EventDto nullEvent = S3EventDto.builder().records(null).build();

        listener.onFileUploaded(emptyEvent);
        listener.onFileUploaded(nullEvent);

        verifyNoInteractions(storageService);
        verifyNoInteractions(audioProcessorService);
    }
}
