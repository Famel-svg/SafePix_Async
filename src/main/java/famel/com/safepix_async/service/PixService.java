package famel.com.safepix_async.service;

import famel.com.safepix_async.config.RabbitMqConfig;
import famel.com.safepix_async.domain.dto.PixEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class PixService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PixService.class);

    private final RabbitTemplate rabbitTemplate;

    public PixService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enviarPix(PixEvent pixEvent) {
        LOGGER.info("Publicando Pix na fila {}: id={}, correlationId={}",
                RabbitMqConfig.PIX_QUEUE, pixEvent.id(), pixEvent.correlationId());
        rabbitTemplate.convertAndSend(RabbitMqConfig.PIX_QUEUE, pixEvent);
        LOGGER.info("Pix publicado na fila {}: id={}, correlationId={}",
                RabbitMqConfig.PIX_QUEUE, pixEvent.id(), pixEvent.correlationId());
    }
}
