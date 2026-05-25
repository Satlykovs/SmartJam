package com.smartjam.notification.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import com.smartjam.common.dto.analysis.AnalysisType;
import com.smartjam.notification.domain.port.RecipientResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter implementing {@link RecipientResolver} using high-performance JDBC queries. Bypasses full ORM
 * overhead for lightweight data retrieval.
 */
@Component
@RequiredArgsConstructor
public class RecipientPersistenceAdapter implements RecipientResolver {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public UUID findOwnerId(UUID targetId, AnalysisType type) {
        String query = (type == AnalysisType.SUBMISSION)
                ? "SELECT student_id FROM submissions " + "WHERE id = ?"
                : "SELECT c.teacher_id FROM connections c JOIN assignments a ON a.connection_id ="
                        + " c.id WHERE a.id = ?";

        return jdbcTemplate.queryForObject(query, UUID.class, targetId);
    }

    @Override
    public List<String> findFcmTokens(UUID userId) {
        return jdbcTemplate.queryForList("SELECT fcm_token FROM user_devices WHERE user_id = ?", String.class, userId);
    }
}
