package famel.com.safepix_async.integration;

import famel.com.safepix_async.config.RabbitMqConfig;
import famel.com.safepix_async.consumer.PixConsumer;
import famel.com.safepix_async.domain.dto.PixEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static java.util.concurrent.TimeUnit.SECONDS;

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
        await().atMost(5, SECONDS).untilAsserted(() ->
                verify(pixConsumer).processarPix(argThat(pixEvent ->
                        pixId.equals(pixEvent.id())
                                && "corr-test".equals(pixEvent.correlationId())
                                && "default".equals(pixEvent.tenantId())
                                && RabbitMqConfig.PAYLOAD_VERSION.equals(pixEvent.payloadVersion())), anyMap()));
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

    @Test
    void deveEnviarJsonInvalidoParaDlq() {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        Message message = new Message("{ json-invalido".getBytes(), properties);

        rabbitTemplate.send(RabbitMqConfig.PIX_QUEUE, message);

        Message mensagemNaDlq = receberMensagemDaDlq();

        assertThat(new String(mensagemNaDlq.getBody())).isEqualTo("{ json-invalido");
        assertThat(mensagemNaDlq.getMessageProperties().getHeaders())
                .containsKey("x-exception-message");
    }

    private PixEvent receberPixDaDlq() {
        AtomicReference<PixEvent> pixEvent = new AtomicReference<>();
        await().atMost(10, SECONDS).untilAsserted(() -> {
            Object message = rabbitTemplate.receiveAndConvert(RabbitMqConfig.PIX_DLQ);
            assertThat(message).isInstanceOf(PixEvent.class);
            pixEvent.set((PixEvent) message);
        });
        return pixEvent.get();
    }

    private Message receberMensagemDaDlq() {
        AtomicReference<Message> dlqMessage = new AtomicReference<>();
        await().atMost(10, SECONDS).untilAsserted(() -> {
            Message message = rabbitTemplate.receive(RabbitMqConfig.PIX_DLQ);
            assertThat(message).isNotNull();
            dlqMessage.set(message);
        });
        return dlqMessage.get();
    }

    private PixEvent receberPixDaFilaPrincipal() {
        Object message = rabbitTemplate.receiveAndConvert(RabbitMqConfig.PIX_QUEUE, 500);
        if (message instanceof PixEvent pixEvent) {
            return pixEvent;
        }
        return null;
    }
}
