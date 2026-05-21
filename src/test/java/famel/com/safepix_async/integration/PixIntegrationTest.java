package famel.com.safepix_async.integration;

import famel.com.safepix_async.config.RabbitMqConfig;
import famel.com.safepix_async.consumer.PixConsumer;
import famel.com.safepix_async.domain.dto.PixDTO;
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
                  "timestamp": "%s"
                }
                """.formatted(pixId, Instant.now());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:%d/pix".formatted(port)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(202);
        verify(pixConsumer, timeout(5_000)).processarPix(argThat(pixDTO -> pixId.equals(pixDTO.id())));
    }

    @Test
    void deveEnviarMensagemInvalidaParaDlq() {
        PixDTO pixInvalido = new PixDTO(UUID.randomUUID(), "cliente@email.com", BigDecimal.ZERO, Instant.now());

        rabbitTemplate.convertAndSend(RabbitMqConfig.PIX_QUEUE, pixInvalido);

        PixDTO pixNaDlq = receberPixDaDlq();

        assertThat(pixNaDlq).isNotNull();
        assertThat(pixNaDlq.id()).isEqualTo(pixInvalido.id());
    }

    private PixDTO receberPixDaDlq() {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            Object message = rabbitTemplate.receiveAndConvert(RabbitMqConfig.PIX_DLQ, 1_000);
            if (message instanceof PixDTO pixDTO) {
                return pixDTO;
            }
        }
        return null;
    }
}
