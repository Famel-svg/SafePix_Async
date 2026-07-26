package famel.com.safepix_async.consumer;

import famel.com.safepix_async.config.RabbitMqConfig;
import famel.com.safepix_async.domain.dto.PixEvent;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PixConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PixConsumer.class);

    private final ProcessedPixStore processedPixStore;
    private final long processingDelayMs;

    public PixConsumer(ProcessedPixStore processedPixStore,
            @Value("${safepix.consumer.processing-delay-ms:2000}") long processingDelayMs) {
        this.processedPixStore = processedPixStore;
        this.processingDelayMs = processingDelayMs;
    }

    @RabbitListener(
            queues = RabbitMqConfig.PIX_QUEUE,
            concurrency = "${safepix.consumer.concurrency:1}",
            containerFactory = "pixRabbitListenerContainerFactory")
    public void processarPix(PixEvent pixEvent) {
        if (!processedPixStore.tryStart(pixEvent.id())) {
            LOGGER.info("Pix duplicado ignorado: id={}, correlationId={}",
                    pixEvent.id(), pixEvent.correlationId());
            return;
        }

        LOGGER.info("Pix recebido para processamento: id={}, chavePix={}, valor={}, correlationId={}",
                pixEvent.id(), pixEvent.chavePix(), pixEvent.valor(), pixEvent.correlationId());

        try {
            if (pixEvent.valor() == null || pixEvent.valor().compareTo(BigDecimal.ZERO) <= 0) {
                throw new AmqpRejectAndDontRequeueException("Pix com valor invalido: " + pixEvent.valor());
            }

            Thread.sleep(processingDelayMs);
            processedPixStore.markProcessed(pixEvent.id());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            processedPixStore.release(pixEvent.id());
            throw new AmqpRejectAndDontRequeueException("Processamento do Pix interrompido", exception);
        } catch (RuntimeException exception) {
            processedPixStore.release(pixEvent.id());
            throw exception;
        }

        LOGGER.info("Pix processado com sucesso: id={}, correlationId={}",
                pixEvent.id(), pixEvent.correlationId());
    }
}
