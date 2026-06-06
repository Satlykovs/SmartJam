package com.smartjam.notification.api.kafka;

import com.smartjam.common.dto.analysis.AnalysisFinishedEvent;
import com.smartjam.notification.application.ProcessAnalysisResultUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisResultListenerTest {

    @Mock
    private ProcessAnalysisResultUseCase useCase;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private AnalysisResultListener listener;

    @Test
    void shouldExecuteUseCaseAndAcknowledge() {
        AnalysisFinishedEvent event = AnalysisFinishedEvent.builder().build();

        listener.onAnalysisFinished(event, ack);

        verify(useCase).execute(event);
        verify(ack).acknowledge();
    }

    @Test
    void shouldNotAcknowledgeIfUseCaseFails() {
        AnalysisFinishedEvent event = AnalysisFinishedEvent.builder().build();
        doThrow(new RuntimeException("Error")).when(useCase).execute(event);

        try {
            listener.onAnalysisFinished(event, ack);
        } catch (Exception _) {
        }

        verify(ack, never()).acknowledge();
    }
}
