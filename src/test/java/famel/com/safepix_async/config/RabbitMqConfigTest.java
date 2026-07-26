package famel.com.safepix_async.config;

import famel.com.safepix_async.domain.dto.PixEvent;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;

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
}
