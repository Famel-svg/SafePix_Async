package famel.com.safepix_async.observability;

import famel.com.safepix_async.config.RabbitMqConfig;
import famel.com.safepix_async.domain.dto.PixEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageProperties;

import static org.assertj.core.api.Assertions.assertThat;

class PixTracingTest {

    private final PixTracing pixTracing = new PixTracing();

    @Test
    void deveInjetarContextoRabbitSemFalhar() {
        MessageProperties properties = new MessageProperties();

        pixTracing.injectRabbitContext(properties);

        assertThat(properties.getHeaders()).isNotNull();
    }

    @Test
    void deveCriarSpanDeProcessamentoPixComHeadersRabbit() {
        PixEvent pixEvent = new PixEvent(
                UUID.randomUUID(),
                "cliente@email.com",
                BigDecimal.TEN,
                Instant.now(),
                null,
                "corr-test",
                "default",
                0,
                RabbitMqConfig.PAYLOAD_VERSION
        );
        Map<String, Object> headers = Map.of("traceparent",
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

        Span span = pixTracing.startPixProcessingSpan(pixEvent, headers);
        try (Scope ignored = span.makeCurrent()) {
            assertThat(Span.current()).isSameAs(span);
        } finally {
            pixTracing.endSpan(span, null);
        }
    }
}
