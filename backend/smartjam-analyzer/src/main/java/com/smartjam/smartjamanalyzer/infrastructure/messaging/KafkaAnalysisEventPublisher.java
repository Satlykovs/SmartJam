package com.smartjam.smartjamanalyzer.infrastructure.messaging;

import com.smartjam.common.dto.analysis.AnalysisFinishedEvent;
import com.smartjam.smartjamanalyzer.domain.port.AnalysisEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaAnalysisEventPublisher implements AnalysisEventPublisher {

    private final KafkaTemplate<String, AnalysisFinishedEvent> kafkaTemplate;

    private static final String TOPIC = "analysis-results";

    @Override
    public void publish(AnalysisFinishedEvent event) {
        kafkaTemplate.send(TOPIC, event.targetId().toString(), event);
    }
}
