package famel.com.safepix_async.domain.dto;

import famel.com.safepix_async.config.RabbitMqConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PixRequestTest {

    @Test
    void deveConverterRequestEmEventoComMetadadosDeRastreio() {
        UUID id = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-01-01T10:00:00Z");
        PixRequest request = new PixRequest(id, "cliente@email.com", new BigDecimal("150.75"), timestamp,
                Map.of("origem", "teste"));

        PixEvent event = request.toEvent("corr-123", "tenant-a");

        assertThat(event.id()).isEqualTo(id);
        assertThat(event.chavePix()).isEqualTo("cliente@email.com");
        assertThat(event.valor()).isEqualByComparingTo("150.75");
        assertThat(event.timestamp()).isEqualTo(timestamp);
        assertThat(event.metadata()).containsEntry("origem", "teste");
        assertThat(event.correlationId()).isEqualTo("corr-123");
        assertThat(event.tenantId()).isEqualTo("tenant-a");
        assertThat(event.retryCount()).isZero();
        assertThat(event.payloadVersion()).isEqualTo(RabbitMqConfig.PAYLOAD_VERSION);
    }
}
