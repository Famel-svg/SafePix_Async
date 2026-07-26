package famel.com.safepix_async.consumer;

import famel.com.safepix_async.config.RabbitMqConfig;
import famel.com.safepix_async.domain.dto.PixEvent;
import famel.com.safepix_async.observability.PixMetrics;
import famel.com.safepix_async.observability.SensitiveDataMasker;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

@Component
public class PixConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PixConsumer.class);

    private final ProcessedPixStore processedPixStore;
    private final long processingDelayMs;
    private final PixMetrics pixMetrics;

    public PixConsumer(ProcessedPixStore processedPixStore,
            @Value("${safepix.consumer.processing-delay-ms:2000}") long processingDelayMs,
            PixMetrics pixMetrics) {
        this.processedPixStore = processedPixStore;
        this.processingDelayMs = processingDelayMs;
        this.pixMetrics = pixMetrics;
    }

    @RabbitListener(
            queues = RabbitMqConfig.PIX_QUEUE,
            concurrency = "${safepix.consumer.concurrency:1}",
            containerFactory = "pixRabbitListenerContainerFactory")
    public void processarPix(PixEvent pixEvent) {
        String previousCorrelationId = MDC.get("correlationId");
        String previousPixId = MDC.get("pixId");
        MDC.put("correlationId", pixEvent.correlationId());
        MDC.put("pixId", pixEvent.id().toString());
        long startNanos = System.nanoTime();
        try {
            if (!processedPixStore.tryStart(pixEvent.id())) {
                LOGGER.info("PIX duplicado ignorado: id={}", pixEvent.id());
                return;
            }

            LOGGER.info("PIX recebido: id={}, chavePix={}, valor={}",
                    pixEvent.id(), SensitiveDataMasker.maskPixKey(pixEvent.chavePix()), pixEvent.valor());
            LOGGER.info("PIX processamento iniciado: id={}", pixEvent.id());

            if (pixEvent.valor() == null || pixEvent.valor().compareTo(BigDecimal.ZERO) <= 0) {
                throw new AmqpRejectAndDontRequeueException("Pix com valor invalido: " + pixEvent.valor());
            }

            Thread.sleep(processingDelayMs);
            processedPixStore.markProcessed(pixEvent.id());
            pixMetrics.recordProcessed(durationSince(startNanos));
            LOGGER.info("PIX processado com sucesso: id={}", pixEvent.id());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            processedPixStore.release(pixEvent.id());
            pixMetrics.recordFailed(durationSince(startNanos));
            LOGGER.error("PIX falhou e sera enviado para DLQ: id={}, erro={}: {}",
                    pixEvent.id(), exception.getClass().getSimpleName(), exception.getMessage());
            throw new AmqpRejectAndDontRequeueException("Processamento do Pix interrompido", exception);
        } catch (RuntimeException exception) {
            processedPixStore.release(pixEvent.id());
            pixMetrics.recordFailed(durationSince(startNanos));
            LOGGER.error("PIX falhou e sera enviado para DLQ: id={}, erro={}: {}",
                    pixEvent.id(), exception.getClass().getSimpleName(), exception.getMessage());
            throw exception;
        } finally {
            restoreMdc("pixId", previousPixId);
            restoreMdc("correlationId", previousCorrelationId);
        }
    }

    private Duration durationSince(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos);
    }

    private void restoreMdc(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }
}
