package com.smartjam.common;

import java.nio.file.Paths;
import java.util.List;

import com.redis.testcontainers.RedisContainer;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketNotificationArgs;
import io.minio.messages.EventType;
import io.minio.messages.NotificationConfiguration;
import io.minio.messages.QueueConfiguration;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ImportTestcontainers
@Import(BaseIntegrationTest.TestInfrastructureConfig.class)
public abstract class BaseIntegrationTest {

    protected static final Network network = Network.newNetwork();

    @ServiceConnection
    protected static final PostgreSQLContainer postgres = new PostgreSQLContainer(
                    DockerImageName.parse("postgres:latest"))
            .withNetwork(network)
            .withNetworkAliases("postgres")
            .withDatabaseName("smartjam")
            .withUsername("admin")
            .withPassword("admin");

    @ServiceConnection
    protected static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka"))
            .withNetwork(network)
            .withNetworkAliases("kafka");

    @ServiceConnection
    protected static final RedisContainer redis =
            new RedisContainer(DockerImageName.parse("redis:latest")).withNetwork(network);

    protected static final MinIOContainer minio = new MinIOContainer(
                    DockerImageName.parse("minio/minio:RELEASE.2023-09-04T19-57-37Z"))
            .withEnv("MINIO_NOTIFY_KAFKA_ENABLE_primary", "on")
            .withEnv("MINIO_NOTIFY_KAFKA_BROKERS_primary", "kafka:9092")
            .withEnv("MINIO_NOTIFY_KAFKA_TOPIC_primary", "s3-events")
            .withNetwork(network);

    protected static final GenericContainer<?> dbInit = new GenericContainer<>(
                    new ImageFromDockerfile("smartjam-db-init", false)
                            .withFileFromPath(
                                    "Dockerfile", Paths.get(System.getProperty("user.dir") + "/../db-init.Dockerfile")))
            .withNetwork(network)
            .dependsOn(postgres)
            .withFileSystemBind(
                    System.getProperty("user.dir") + "/../smartjam-common/src/main/resources/db",
                    "/liquibase/changelog/db")
            .withCommand(
                    "--url=jdbc:postgresql://postgres:5432/smartjam",
                    "--changelog-file=db/changelog/db.changelog-master.yaml",
                    "--username=admin",
                    "--password=admin",
                    "update")
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy());

    static {
        postgres.start();
        kafka.start();
        redis.start();
        minio.start();

        dbInit.start();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void overrideMinioProperties(DynamicPropertyRegistry registry) {
        registry.add("minio.url", minio::getS3URL);
        registry.add("minio.access-key", minio::getUserName);
        registry.add("minio.secret-key", minio::getPassword);
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class TestInfrastructureConfig {

        @Bean
        public NewTopic s3EventsTopic() {
            return TopicBuilder.name("s3-events").partitions(3).replicas(1).build();
        }

        @Bean
        public NewTopic analysisResultsTopic() {
            return TopicBuilder.name("analysis-results")
                    .partitions(3)
                    .replicas(1)
                    .build();
        }

        @Bean
        public MinioClient testMinioClient() throws Exception {
            MinioClient client = MinioClient.builder()
                    .endpoint(minio.getS3URL())
                    .credentials(minio.getUserName(), minio.getPassword())
                    .build();

            setupBucket(client, "references");
            setupBucket(client, "submissions");

            return client;
        }

        private void setupBucket(MinioClient client, String bucketName) throws Exception {
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());

                QueueConfiguration queueConfig = new QueueConfiguration();
                queueConfig.setQueue("arn:minio:sqs::primary:kafka");
                queueConfig.setEvents(List.of(EventType.OBJECT_CREATED_PUT));

                NotificationConfiguration notificationConfig = new NotificationConfiguration();
                notificationConfig.setQueueConfigurationList(List.of(queueConfig));

                client.setBucketNotification(SetBucketNotificationArgs.builder()
                        .bucket(bucketName)
                        .config(notificationConfig)
                        .build());
            }
        }
    }
}
