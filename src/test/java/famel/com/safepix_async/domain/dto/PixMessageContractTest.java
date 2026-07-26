package famel.com.safepix_async.domain.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import famel.com.safepix_async.config.RabbitMqConfig;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.cloud.contract.spec.Contract;

import static org.assertj.core.api.Assertions.assertThat;

class PixMessageContractTest {

    @Test
    void deveValidarSchemaDaMensagemPixComSpringCloudContract() throws Exception {
        Contract contract = pixMessageContract();
        Contract.assertContract(contract);

        PixEvent event = new PixEvent(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "cliente@email.com",
                new BigDecimal("42.5"),
                Instant.parse("2026-05-24T21:00:00Z"),
                null,
                "corr-contract",
                "default",
                0,
                RabbitMqConfig.PAYLOAD_VERSION
        );

        String json = new String(new RabbitMqConfig().messageConverter()
                .toMessage(event, new MessageProperties())
                .getBody(), StandardCharsets.UTF_8);
        JsonNode root = new ObjectMapper().readTree(json);

        assertThat(root.get("id").asText()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(root.get("chavePix").asText()).isEqualTo("cliente@email.com");
        assertThat(root.get("valor").decimalValue()).isEqualByComparingTo("42.50");
        assertThat(root.get("timestamp").isNumber()).isTrue();
        assertThat(root.get("correlationId").asText()).isEqualTo("corr-contract");
        assertThat(root.get("tenantId").asText()).isEqualTo("default");
        assertThat(root.get("retryCount").asInt()).isZero();
        assertThat(root.get("payloadVersion").asText()).isEqualTo(RabbitMqConfig.PAYLOAD_VERSION);
        assertThat(contract.getOutputMessage().getSentTo().getServerValue()).isEqualTo(RabbitMqConfig.PIX_QUEUE);
        assertThat(contract.getMetadata()).containsEntry("schema", "PixEvent.v1");
    }

    private Contract pixMessageContract() {
        return Contract.make(contract -> {
            contract.name("pix_event_v1");
            contract.label("pix-event-produced");
            contract.description("Schema de mensagem PixEvent enviada para RabbitMQ");
            contract.metadata(Map.of("schema", "PixEvent.v1"));
            contract.outputMessage(message -> {
                message.sentTo(RabbitMqConfig.PIX_QUEUE);
                message.body(Map.of(
                        "id", "11111111-1111-1111-1111-111111111111",
                        "chavePix", "cliente@email.com",
                        "valor", 42.50,
                        "timestamp", 1785099600,
                        "correlationId", "corr-contract",
                        "tenantId", "default",
                        "retryCount", 0,
                        "payloadVersion", RabbitMqConfig.PAYLOAD_VERSION
                ));
            });
        });
    }
}
