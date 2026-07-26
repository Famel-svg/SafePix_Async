package famel.com.safepix_async.service;

import famel.com.safepix_async.config.RabbitMqConfig;
import famel.com.safepix_async.domain.dto.PixEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PixAntifraudClientTest {

    private final PixAntifraudClient client = new PixAntifraudClient();

    @Test
    void deveTerCircuitBreakerPixAntifraud() throws NoSuchMethodException {
        CircuitBreaker circuitBreaker = PixAntifraudClient.class
                .getMethod("approve", PixEvent.class)
                .getAnnotation(CircuitBreaker.class);

        assertThat(circuitBreaker).isNotNull();
        assertThat(circuitBreaker.name()).isEqualTo("pixAntifraud");
        assertThat(circuitBreaker.fallbackMethod()).isEqualTo("fallback");
    }

    @Test
    void deveAprovarChaveNormalENegarChaveSuspeita() {
        assertThat(client.approve(pixEvent("cliente@email.com", null))).isTrue();
        assertThat(client.approve(pixEvent("fraude@email.com", null))).isFalse();
        assertThat(client.approve(pixEvent("deny@email.com", null))).isFalse();
    }

    @Test
    void deveNegarNoFallbackQuandoServicoFalhar() {
        PixEvent event = pixEvent("cliente@email.com", Map.of("simulateAntifraudFailure", true));

        assertThat(client.fallback(event, new IllegalStateException("indisponivel"))).isFalse();
    }

    private PixEvent pixEvent(String chavePix, Map<String, Object> metadata) {
        return new PixEvent(
                UUID.randomUUID(),
                chavePix,
                BigDecimal.TEN,
                Instant.now(),
                metadata,
                "corr-test",
                "default",
                0,
                RabbitMqConfig.PAYLOAD_VERSION
        );
    }
}
