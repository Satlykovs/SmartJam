package com.smartjam.notification.application;

import java.util.List;
import java.util.UUID;

import com.smartjam.common.dto.analysis.AnalysisFinishedEvent;
import com.smartjam.common.dto.analysis.AnalysisType;
import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.notification.domain.port.PushPublisher;
import com.smartjam.notification.domain.port.RecipientResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessAnalysisResultUseCaseTest {

    @Mock
    private RecipientResolver recipientResolver;

    @Mock
    private PushPublisher pushPublisher;

    @InjectMocks
    private ProcessAnalysisResultUseCase useCase;

    @ParameterizedTest(name = "Тип: {1}, Статус: {0} -> Ожидаем в пуше: {2}")
    @CsvSource({
        "COMPLETED, SUBMISSION, проанализирована",
        "COMPLETED, REFERENCE, обработана",
        "FAILED,    SUBMISSION, SKIP",
        "FAILED,    REFERENCE,  SKIP"
    })
    @DisplayName("Базовая проверка логики уведомлений")
    void testNotificationMatrix(AudioProcessingStatus status, AnalysisType type, String expectedMessagePart) {
        UUID targetId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String token = "token-123";

        AnalysisFinishedEvent event = AnalysisFinishedEvent.builder()
                .targetId(targetId)
                .type(type)
                .status(status)
                .totalScore(90.0)
                .build();

        if (status == AudioProcessingStatus.COMPLETED) {
            when(recipientResolver.findOwnerId(targetId, type)).thenReturn(userId);
            when(recipientResolver.findFcmTokens(userId)).thenReturn(List.of(token));
        }

        useCase.execute(event);

        if ("SKIP".equals(expectedMessagePart)) {
            verifyNoInteractions(pushPublisher);
        } else {
            verify(pushPublisher).sendPush(anyList(), contains(expectedMessagePart));
        }
    }
}
