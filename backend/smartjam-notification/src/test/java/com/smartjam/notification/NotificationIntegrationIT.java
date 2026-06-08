package com.smartjam.notification;

import java.util.List;
import java.util.UUID;

import com.smartjam.common.BaseIntegrationTest;
import com.smartjam.common.dto.analysis.AnalysisFinishedEvent;
import com.smartjam.common.dto.analysis.AnalysisType;
import com.smartjam.common.model.AudioProcessingStatus;
import com.smartjam.notification.domain.port.PushPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(
        properties = {
            "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
            "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer"
        })
@ActiveProfiles("test")
public class NotificationIntegrationIT extends BaseIntegrationTest {

    private final String TEST_FCM_TOKEN = "test-token-device-1";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PushPublisher pushPublisher;

    private UUID submissionId;
    private UUID assignmentId;

    @BeforeEach
    void setUpDatabase() {
        UUID studentId = UUID.randomUUID();
        submissionId = UUID.randomUUID();
        assignmentId = UUID.randomUUID();

        UUID teacherId = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();

        jdbcTemplate.execute("TRUNCATE TABLE user_devices CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE submissions CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE assignments CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE connections CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");

        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, password_hash) VALUES (?, 'student', 's@s.com', 'pwd')",
                studentId);
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, password_hash) VALUES (?, 'teacher', 't@t.com', 'pwd')",
                teacherId);
        jdbcTemplate.update(
                "INSERT INTO connections (id, teacher_id, student_id, status) VALUES (?, ?, ?, 'ACTIVE')",
                connectionId,
                teacherId,
                studentId);

        // Используем наш assignmentId
        jdbcTemplate.update(
                "INSERT INTO assignments (id, connection_id, title) VALUES (?, ?, 'Task 1')",
                assignmentId,
                connectionId);

        jdbcTemplate.update(
                "INSERT INTO submissions (id, assignment_id, student_id, status) VALUES (?, ?, ?, 'AWAITING_UPLOAD')",
                submissionId,
                assignmentId,
                studentId);
        jdbcTemplate.update(
                "INSERT INTO user_devices (fcm_token, user_id, device_type) VALUES (?, ?, 'ANDROID')",
                TEST_FCM_TOKEN,
                studentId);
    }

    @Test
    @DisplayName("End-to-End: Кафка -> Нотификатор -> БД -> Отправка Пуша")
    void shouldProcessKafkaEventAndSendPush() {
        AnalysisFinishedEvent event = AnalysisFinishedEvent.builder()
                .targetId(submissionId)
                .type(AnalysisType.SUBMISSION)
                .status(AudioProcessingStatus.COMPLETED)
                .totalScore(88.5)
                .build();

        kafkaTemplate.send("analysis-results", submissionId.toString(), event);

        verify(pushPublisher, timeout(5000)).sendPush(eq(List.of(TEST_FCM_TOKEN)), contains("88.5"));
    }

    @Test
    @DisplayName("Должен корректно проигнорировать пуш, если у юзера нет девайсов")
    void shouldNotFailIfUserHasNoDevices() {
        UUID lonelyStudentId = UUID.randomUUID();
        UUID lonelySubmissionId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, password_hash) VALUES (?, 'lonely', 'l@l.com', 'pwd')",
                lonelyStudentId);

        jdbcTemplate.update(
                "INSERT INTO submissions (id, assignment_id, student_id, status) VALUES (?, ?, ?, 'UPLOADED')",
                lonelySubmissionId,
                this.assignmentId,
                lonelyStudentId);

        AnalysisFinishedEvent event = AnalysisFinishedEvent.builder()
                .targetId(lonelySubmissionId)
                .type(AnalysisType.SUBMISSION)
                .status(AudioProcessingStatus.COMPLETED)
                .build();

        kafkaTemplate.send("analysis-results", lonelySubmissionId.toString(), event);

        verify(pushPublisher, after(2000).never()).sendPush(any(), anyString());
    }
}
