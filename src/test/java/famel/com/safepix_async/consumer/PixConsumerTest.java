package famel.com.safepix_async.consumer;

import famel.com.safepix_async.config.RabbitMqConfig;
import famel.com.safepix_async.domain.dto.PixEvent;
import famel.com.safepix_async.observability.PixMetrics;
import famel.com.safepix_async.service.PixAntifraudClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PixConsumerTest {

    @Test
    void deveIgnorarPixDuplicadoSemProcessarDuasVezes() {
        ProcessedPixStore store = new ProcessedPixStore();
        PixAntifraudClient antifraudClient = approvingAntifraud();
        PixConsumer consumer = new PixConsumer(store, 0, pixMetrics(), antifraudClient);
        PixEvent event = pixEvent(BigDecimal.TEN);

        consumer.processarPix(event);
        consumer.processarPix(event);

        verify(antifraudClient).approve(event);
        assertThat(store.isProcessed(event.id())).isTrue();
        assertThat(store.processedCount()).isOne();
    }

    @Test
    void deveLiberarIdempotenciaQuandoValorForInvalido() {
        ProcessedPixStore store = new ProcessedPixStore();
        PixConsumer consumer = new PixConsumer(store, 0, pixMetrics(), approvingAntifraud());
        PixEvent event = pixEvent(BigDecimal.ZERO);

        assertThatThrownBy(() -> consumer.processarPix(event))
                .isInstanceOf(PixBusinessValidationException.class)
                .hasMessageContaining("valor invalido");

        assertThat(store.isProcessed(event.id())).isFalse();
        assertThat(store.tryStart(event.id())).isTrue();
    }

    @Test
    void deveNegarPixQuandoAntifraudeReprovar() {
        ProcessedPixStore store = new ProcessedPixStore();
        PixAntifraudClient antifraudClient = mock(PixAntifraudClient.class);
        PixConsumer consumer = new PixConsumer(store, 0, pixMetrics(), antifraudClient);
        PixEvent event = pixEvent(BigDecimal.TEN);
        when(antifraudClient.approve(event)).thenReturn(false);

        assertThatThrownBy(() -> consumer.processarPix(event))
                .isInstanceOf(PixBusinessValidationException.class)
                .hasMessageContaining("negado pelo antifraude");

        assertThat(store.isProcessed(event.id())).isFalse();
        assertThat(store.tryStart(event.id())).isTrue();
    }

    @Test
    void deveConfigurarRabbitListenerComConcorrenciaEFactoryCustomizada() throws NoSuchMethodException {
        RabbitListener listener = PixConsumer.class.getMethod("processarPix", PixEvent.class)
                .getAnnotation(RabbitListener.class);

        assertThat(listener.queues()).containsExactly(RabbitMqConfig.PIX_QUEUE);
        assertThat(listener.concurrency()).isEqualTo("${safepix.consumer.concurrency:1}");
        assertThat(listener.containerFactory()).isEqualTo("pixRabbitListenerContainerFactory");
    }

    private PixEvent pixEvent(BigDecimal valor) {
        return new PixEvent(
                UUID.randomUUID(),
                "cliente@email.com",
                valor,
                Instant.now(),
                null,
                "corr-test",
                "default",
                0,
                RabbitMqConfig.PAYLOAD_VERSION
        );
    }

    private PixMetrics pixMetrics() {
        return new PixMetrics(new SimpleMeterRegistry());
    }

    private PixAntifraudClient approvingAntifraud() {
        PixAntifraudClient antifraudClient = mock(PixAntifraudClient.class);
        when(antifraudClient.approve(org.mockito.ArgumentMatchers.any(PixEvent.class))).thenReturn(true);
        return antifraudClient;
    }
}
