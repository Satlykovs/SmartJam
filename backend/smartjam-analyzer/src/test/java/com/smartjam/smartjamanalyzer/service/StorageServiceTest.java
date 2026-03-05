package com.smartjam.smartjamanalyzer.service;

import java.nio.file.Path;

import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StorageServiceTest {
    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private StorageService storageService;

    @Test
    void shouldPrepareCorrectPathAndCallMinio() throws Exception {
        String bucket = "submissions";
        String fileKey = "user1/my_audio.mp3";

        Path resultPath = storageService.downloadAudioFile(bucket, fileKey);

        String fileName = resultPath.getFileName().toString();

        assertTrue(fileName.contains("user1_my_audio.mp3"), "Имя файла должно содержать ключ с замененными слэшами");

        ArgumentCaptor<DownloadObjectArgs> captor = ArgumentCaptor.forClass(DownloadObjectArgs.class);
        verify(minioClient, times(1)).downloadObject(captor.capture());

        DownloadObjectArgs capturedArgs = captor.getValue();
        assertEquals("submissions", capturedArgs.bucket());
        assertEquals("user1/my_audio.mp3", capturedArgs.object());
    }

    @Test
    void shouldThrowRuntimeExceptionWhenMinioFails() throws Exception {

        doThrow(new RuntimeException("Connection error"))
                .when(minioClient)
                .downloadObject(any(DownloadObjectArgs.class));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            storageService.downloadAudioFile("any-bucket", "any-file");
        });

        assertTrue(exception.getMessage().contains("Failed to download file"));
    }
}
