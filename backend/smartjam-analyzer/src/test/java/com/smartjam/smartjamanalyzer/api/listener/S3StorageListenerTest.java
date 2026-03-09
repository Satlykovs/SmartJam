package com.smartjam.smartjamanalyzer.api.listener;

import java.util.Collections;
import java.util.List;

import com.smartjam.smartjamanalyzer.api.kafka.S3StorageListener;
import com.smartjam.smartjamanalyzer.api.kafka.dto.S3EventDto;
import com.smartjam.smartjamanalyzer.application.AudioAnalysisUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3StorageListenerTest {

    @Mock
    private AudioAnalysisUseCase analysisUseCase;

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
    @DisplayName("Должен вызвать UseCase при получении события")
    void shouldCallUseCaseOnEvent() {
        String bucket = "references";
        String key = "teacher_riff.wav";
        S3EventDto event = createEvent(bucket, key);
        Acknowledgment ack = mock(Acknowledgment.class);

        listener.onFileUploaded(event, ack);

        verify(analysisUseCase).execute(eq(bucket), eq(key));
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("Должен подтвердить (ack) оффсет, если событие пустое или null")
    void shouldAckOnEmptyEvents() {
        Acknowledgment ack = mock(Acknowledgment.class);

        listener.onFileUploaded(S3EventDto.builder().records(null).build(), ack);
        listener.onFileUploaded(
                S3EventDto.builder().records(Collections.emptyList()).build(), ack);

        verify(ack, times(2)).acknowledge();
        verifyNoInteractions(analysisUseCase);
    }

    @Test
    @DisplayName("Должен пропускать некорректные записи (skip) и не вызывать UseCase")
    void shouldSkipInvalidRecords() {
        S3EventDto event = S3EventDto.builder()
                .records(List.of(S3EventDto.S3Record.builder().s3(null).build()))
                .build();
        Acknowledgment ack = mock(Acknowledgment.class);

        listener.onFileUploaded(event, ack);

        verifyNoInteractions(analysisUseCase);
        verify(ack).acknowledge();
    }

    @Test
    @DisplayName("Должен выбрасывать RuntimeException, если UseCase упал (для ретрая)")
    void shouldThrowExceptionWhenUseCaseFails() {
        String bucket = "references";
        String key = "fail.wav";
        S3EventDto event = createEvent(bucket, key);
        Acknowledgment ack = mock(Acknowledgment.class);

        doThrow(new RuntimeException("Math failed")).when(analysisUseCase).execute(anyString(), anyString());

        assertThrows(RuntimeException.class, () -> listener.onFileUploaded(event, ack));

        verify(ack, never()).acknowledge();
    }
}
