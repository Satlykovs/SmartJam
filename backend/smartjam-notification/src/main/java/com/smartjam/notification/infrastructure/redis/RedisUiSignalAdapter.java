package com.smartjam.notification.infrastructure.redis;

import java.util.UUID;

import com.smartjam.common.dto.analysis.AnalysisType;
import com.smartjam.notification.domain.port.UiSignalPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUiSignalAdapter implements UiSignalPublisher {

    private final StringRedisTemplate redisTemplate;

    private static final String CHANNEL = "ui-updates";

    @Override
    public void sendRefreshSignal(UUID targetId, AnalysisType type) {
        String payload = type.name() + ':' + targetId.toString();

        try {
            redisTemplate.convertAndSend(CHANNEL, payload);
            log.info("Signal sent to Redis channel [{}]: {}", CHANNEL, payload);
        } catch (Exception e) {
            log.error("Redis signal failed for {}: {}", payload, e.getMessage());
            throw new RuntimeException("Redis error", e);
        }
    }
}
