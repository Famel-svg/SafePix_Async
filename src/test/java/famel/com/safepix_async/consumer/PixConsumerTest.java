package famel.com.safepix_async.consumer;

import famel.com.safepix_async.config.RabbitMqConfig;
import famel.com.safepix_async.domain.dto.PixEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PixConsumerTest {

    @Test
    void deveIgnorarPixDuplicadoSemProcessarDuasVezes() {
        ProcessedPixStore store = new ProcessedPixStore();
        PixConsumer consumer = new PixConsumer(store, 0);
        PixEvent event = pixEvent(BigDecimal.TEN);

        consumer.processarPix(event);
        consumer.processarPix(event);

        assertThat(store.isProcessed(event.id())).isTrue();
        assertThat(store.processedCount()).isOne();
    }

    @Test
    void deveLiberarIdempotenciaQuandoValorForInvalido() {
        ProcessedPixStore store = new ProcessedPixStore();
        PixConsumer consumer = new PixConsumer(store, 0);
        PixEvent event = pixEvent(BigDecimal.ZERO);

        assertThatThrownBy(() -> consumer.processarPix(event))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

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
}
