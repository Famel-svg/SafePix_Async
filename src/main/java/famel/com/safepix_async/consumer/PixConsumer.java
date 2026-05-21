package famel.com.safepix_async.consumer;

import famel.com.safepix_async.config.RabbitMqConfig;
import famel.com.safepix_async.domain.dto.PixDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PixConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PixConsumer.class);

    @RabbitListener(queues = RabbitMqConfig.PIX_QUEUE)
    public void processarPix(PixDTO pixDTO) {
        LOGGER.info("Pix recebido para processamento: id={}, chavePix={}, valor={}",
                pixDTO.id(), pixDTO.chavePix(), pixDTO.valor());

        if (pixDTO.valor() == null || pixDTO.valor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AmqpRejectAndDontRequeueException("Pix com valor invalido: " + pixDTO.valor());
        }

        try {
            Thread.sleep(2_000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AmqpRejectAndDontRequeueException("Processamento do Pix interrompido", exception);
        }

        LOGGER.info("Pix processado com sucesso: id={}", pixDTO.id());
    }
}
