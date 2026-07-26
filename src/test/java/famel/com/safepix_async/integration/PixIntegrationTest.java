package famel.com.safepix_async.integration;

import famel.com.safepix_async.config.RabbitMqConfig;
import famel.com.safepix_async.consumer.PixConsumer;
import famel.com.safepix_async.domain.dto.PixEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PixIntegrationTest {

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3-management");

    @DynamicPropertySource
    static void rabbitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        registry.add("safepix.consumer.processing-delay-ms", () -> "0");
        registry.add("safepix.consumer.concurrency", () -> "1");
        registry.add("safepix.rabbitmq.listener.retry.max-attempts", () -> "1");
        registry.add("safepix.rabbitmq.listener.retry.initial-interval-ms", () -> "10");
        registry.add("safepix.rabbitmq.listener.retry.max-interval-ms", () -> "10");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockitoSpyBean
    private PixConsumer pixConsumer;

    @BeforeEach
    void purgeQueues() {
        rabbitTemplate.execute(channel -> {
            channel.queuePurge(RabbitMqConfig.PIX_QUEUE);
            channel.queuePurge(RabbitMqConfig.PIX_DLQ);
            return null;
        });
        clearInvocations(pixConsumer);
    }

    @Test
    void deveEnviarPostParaApiEEntregarMensagemAoListener() throws Exception {
        UUID pixId = UUID.randomUUID();
        String body = """
                {
                  "id": "%s",
                  "chavePix": "cliente@email.com",
                  "valor": 150.75,
                  "timestamp": "%s",
                  "metadata": {
                    "origem": "teste"
                  }
                }
                """.formatted(pixId, Instant.now());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d/api/v1/pix".formatted(port)))
                .header("Content-Type", "application/json")
                .header("X-Correlation-Id", "corr-test")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(response.headers().firstValue("Location")).contains("/api/v1/pix/%s/status".formatted(pixId));
        assertThat(response.headers().firstValue("X-Correlation-Id")).contains("corr-test");
        verify(pixConsumer, timeout(5_000)).processarPix(argThat(pixEvent ->
                pixId.equals(pixEvent.id())
                        && "corr-test".equals(pixEvent.correlationId())
                        && "default".equals(pixEvent.tenantId())
                        && RabbitMqConfig.PAYLOAD_VERSION.equals(pixEvent.payloadVersion())));
    }

    @Test
    void deveEnviarMensagemInvalidaParaDlq() {
        PixEvent pixInvalido = new PixEvent(
                UUID.randomUUID(),
                "cliente@email.com",
                BigDecimal.ZERO,
                Instant.now(),
                null,
                "corr-dlq",
                "default",
                0,
                RabbitMqConfig.PAYLOAD_VERSION
        );

        rabbitTemplate.convertAndSend(RabbitMqConfig.PIX_QUEUE, pixInvalido);

        PixEvent pixNaDlq = receberPixDaDlq();

        assertThat(pixNaDlq).isNotNull();
        assertThat(pixNaDlq.id()).isEqualTo(pixInvalido.id());
    }

    @Test
    void deveEnviarMensagemInvalidaParaDlqSemLoopDeRequeue() {
        PixEvent pixInvalido = new PixEvent(
                UUID.randomUUID(),
                "cliente@email.com",
                BigDecimal.ZERO,
                Instant.now(),
                null,
                "corr-dlq-loop",
                "default",
                0,
                RabbitMqConfig.PAYLOAD_VERSION
        );

        rabbitTemplate.convertAndSend(RabbitMqConfig.PIX_QUEUE, pixInvalido);

        PixEvent pixNaDlq = receberPixDaDlq();

        assertThat(pixNaDlq).isNotNull();
        assertThat(pixNaDlq.id()).isEqualTo(pixInvalido.id());
        assertThat(receberPixDaFilaPrincipal()).isNull();
    }

    private PixEvent receberPixDaDlq() {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            Object message = rabbitTemplate.receiveAndConvert(RabbitMqConfig.PIX_DLQ, 1_000);
            if (message instanceof PixEvent pixEvent) {
                return pixEvent;
            }
        }
        return null;
    }

    private PixEvent receberPixDaFilaPrincipal() {
        Object message = rabbitTemplate.receiveAndConvert(RabbitMqConfig.PIX_QUEUE, 500);
        if (message instanceof PixEvent pixEvent) {
            return pixEvent;
        }
        return null;
    }
}
