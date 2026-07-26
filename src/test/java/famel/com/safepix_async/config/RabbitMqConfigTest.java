package famel.com.safepix_async.config;

import famel.com.safepix_async.domain.dto.PixEvent;
import famel.com.safepix_async.consumer.PixBusinessValidationException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RabbitMqConfigTest {

    private final RabbitMqConfig config = new RabbitMqConfig();

    @Test
    void deveConfigurarFilaPrincipalComDlqPadraoETtl() {
        Queue queue = config.pixQueue(3_600_000);

        assertThat(queue.getName()).isEqualTo(RabbitMqConfig.PIX_QUEUE);
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", "")
                .containsEntry("x-dead-letter-routing-key", RabbitMqConfig.PIX_DLQ)
                .containsEntry("x-message-ttl", 3_600_000);
    }

    @Test
    void deveConfigurarDlqComoQuarentenaComExchangeDeReprocessamento() {
        Queue queue = config.pixDeadLetterQueue();

        assertThat(queue.getName()).isEqualTo(RabbitMqConfig.PIX_DLQ);
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitMqConfig.PIX_REPROCESS_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", RabbitMqConfig.PIX_QUEUE);
    }

    @Test
    void deveSerializarBigDecimalComDuasCasasEVersaoNoPayload() {
        PixEvent event = new PixEvent(
                UUID.randomUUID(),
                "cliente@email.com",
                new BigDecimal("10.1"),
                Instant.parse("2026-05-24T21:00:00Z"),
                null,
                "corr-123",
                "default",
                0,
                RabbitMqConfig.PAYLOAD_VERSION
        );

        String json = new String(config.messageConverter()
                .toMessage(event, new MessageProperties())
                .getBody(), StandardCharsets.UTF_8);

        assertThat(json)
                .contains("\"valor\":10.10")
                .contains("\"payloadVersion\":\"1\"");
    }

    @Test
    void deveCriarContainerFactoryComRetryERecovererParaDlq() {
        assertThat(config.pixRabbitListenerContainerFactory(
                mock(ConnectionFactory.class),
                config.messageConverter(),
                mock(RabbitTemplate.class),
                config.pixConsumerTaskExecutor(1, 1, 1),
                1,
                10,
                1.0,
                10))
                .isNotNull();
    }

    @Test
    void deveDiferenciarRetryEntreErroTransitorioENegocio() {
        var retryPolicy = config.pixRetryPolicy(3, 10, 2.0, 100);

        assertThat(retryPolicy.shouldRetry(new IllegalStateException("rede fora"))).isTrue();
        assertThat(retryPolicy.shouldRetry(new PixBusinessValidationException("valor invalido"))).isFalse();
    }

    @Test
    void deveCriarExecutorCustomizadoParaConsumer() {
        ThreadPoolTaskExecutor executor = config.pixConsumerTaskExecutor(2, 4, 50);

        assertThat(executor.getCorePoolSize()).isEqualTo(2);
        assertThat(executor.getMaxPoolSize()).isEqualTo(4);
        assertThat(executor.getQueueCapacity()).isEqualTo(50);
        assertThat(executor.getThreadNamePrefix()).isEqualTo("pix-consumer-");
    }

    @Test
    void devePropagarMdcERestaurarContextoAnterior() {
        MDC.put("correlationId", "parent-corr");
        Runnable decorated = new MdcTaskDecorator().decorate(() ->
                assertThat(MDC.get("correlationId")).isEqualTo("parent-corr"));

        MDC.put("correlationId", "worker-old");
        decorated.run();

        assertThat(MDC.get("correlationId")).isEqualTo("worker-old");
        MDC.clear();
    }
}
