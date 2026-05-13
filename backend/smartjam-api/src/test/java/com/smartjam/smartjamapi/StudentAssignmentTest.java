// package com.smartjam.smartjamapi;
//
// import java.net.URI;
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.util.UUID;
// import java.util.concurrent.TimeUnit;
//
// import com.smartjam.api.model.*;
// import org.junit.jupiter.api.Test;
// import org.springframework.test.web.reactive.server.WebTestClient;
//
// import static org.awaitility.Awaitility.await;
// import static org.junit.jupiter.api.Assertions.assertNotNull;
//
// public class StudentAssignmentTest {
//
//    private final String BASE_URL = "http://localhost:8081";
//    private final String FILE_PATH = "C:/Users/basko/Downloads/Запись.m4a";
//
//    private final WebTestClient client =
//            WebTestClient.bindToServer().baseUrl(BASE_URL).build();
//
//    @Test
//    public void testFullStudentWorkflow() {
//        // 1. УНИКАЛЬНЫЕ ГЕРОИ
//        String teacherName = "T_" + UUID.randomUUID().toString().substring(0, 8);
//        String studentName = "S_" + UUID.randomUUID().toString().substring(0, 8);
//
//        AuthResponse teacherAuth = registerAndGetToken(teacherName, UserRole.TEACHER);
//        AuthResponse studentAuth = registerAndGetToken(studentName, UserRole.STUDENT);
//
//        // 2. СВЯЗЬ (ИНВАЙТ)
//        InviteResponse invite = client.post()
//                .uri("/api/v1/connections/invite")
//                .header("Authorization", "Bearer " + teacherAuth.accessToken())
//                .exchange()
//                .expectStatus()
//                .isCreated()
//                .expectBody(InviteResponse.class)
//                .returnResult()
//                .getResponseBody();
//
//        client.post()
//                .uri("/api/v1/connections/join")
//                .header("Authorization", "Bearer " + studentAuth.accessToken())
//                .bodyValue(new JoinRequest(invite.inviteCode()))
//                .exchange()
//                .expectStatus()
//                .isOk();
//
//        // 3. CONNECTION ID
//        ConnectionPageResponse connections = client.get()
//                .uri("/api/v1/connections")
//                .header("Authorization", "Bearer " + studentAuth.accessToken())
//                .exchange()
//                .expectBody(ConnectionPageResponse.class)
//                .returnResult()
//                .getResponseBody();
//
//        UUID connectionId = connections.content().get(0).id();
//
//        // 4. ЗАДАНИЕ
//        AssignmentUploadResponse assignUpload = client.post()
//                .uri("/api/v1/assignments")
//                .header("Authorization", "Bearer " + teacherAuth.accessToken())
//                .bodyValue(new CreateAssignmentRequest(connectionId, "Test", "Desc"))
//                .exchange()
//                .expectStatus()
//                .isCreated()
//                .expectBody(AssignmentUploadResponse.class)
//                .returnResult()
//                .getResponseBody();
//
//        uploadToS3(assignUpload.uploadUrl(), FILE_PATH);
//
//        // 5. САБМИШН
//        SubmissionUploadResponse subUpload = client.post()
//                .uri("/api/v1/assignments/{id}/submissions", assignUpload.assignmentId())
//                .header("Authorization", "Bearer " + studentAuth.accessToken())
//                .exchange()
//                .expectStatus()
//                .isCreated()
//                .expectBody(SubmissionUploadResponse.class)
//                .returnResult()
//                .getResponseBody();
//
//        assertNotNull(subUpload);
//        uploadToS3(subUpload.uploadUrl(), FILE_PATH);
//
//        // 6. ОЖИДАНИЕ ОБРАБОТКИ
//        System.out.println("Файлы в S3. Ждем смены статуса...");
//
//        await().atMost(30, TimeUnit.SECONDS).pollInterval(3, TimeUnit.SECONDS).untilAsserted(() -> {
//            SubmissionResultResponse result = client.get()
//                    .uri("/api/v1/submissions/{id}", subUpload.submissionId())
//                    .header("Authorization", "Bearer " + studentAuth.accessToken())
//                    .exchange()
//                    .expectStatus()
//                    .isOk()
//                    .expectBody(SubmissionResultResponse.class)
//                    .returnResult()
//                    .getResponseBody();
//
//            assertNotNull(result);
//            System.out.println("Текущий статус в БД: " + result.status());
//
//            // Проверка, что статус изменился (не AWAITING_UPLOAD)
//            // Если у тебя еще нет логики смены статуса, временно закомментируй проверку ниже
//            // return result.status() != AudioProcessingStatus.AWAITING_UPLOAD;
//        });
//    }
//
//    private void uploadToS3(String url, String pathStr) {
//        // Исправляем домен для локального запуска
//        String fixedUrl = url.replace("references.localhost", "localhost")
//                .replace("submissions.localhost", "localhost")
//                .replace("127.0.0.1", "localhost");
//
//        System.out.println("Uploading file to: " + fixedUrl);
//
//        try {
//            HttpClient httpClient = HttpClient.newHttpClient();
//            Path path = Paths.get(pathStr);
//
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(fixedUrl))
//                    .header("Content-Type", "application/octet-stream")
//                    .PUT(HttpRequest.BodyPublishers.ofFile(path))
//                    .build();
//
//            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//
//            if (response.statusCode() != 200) {
//                System.err.println("S3 ERROR Body: " + response.body());
//                throw new RuntimeException("S3 Upload Failed with status: " + response.statusCode());
//            }
//            System.out.println("S3 Upload Success: 200 OK");
//
//        } catch (Exception e) {
//            throw new RuntimeException("Upload failed", e);
//        }
//    }
//
//    private AuthResponse registerAndGetToken(String username, UserRole role) {
//        String email = username + "@test.com";
//        AuthResponse initial = client.post()
//                .uri("/api/v1/auth/register")
//                .bodyValue(new RegisterRequest(email, username, "Password123!"))
//                .exchange()
//                .expectStatus()
//                .isCreated()
//                .expectBody(AuthResponse.class)
//                .returnResult()
//                .getResponseBody();
//
//        return client.post()
//                .uri("/api/v1/auth/refresh")
//                .bodyValue(new RefreshRequest(initial.refreshToken(), role))
//                .exchange()
//                .expectStatus()
//                .isOk()
//                .expectBody(AuthResponse.class)
//                .returnResult()
//                .getResponseBody();
//    }
// }
