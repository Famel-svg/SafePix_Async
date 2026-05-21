package famel.com.safepix_async.service;

import famel.com.safepix_async.config.RabbitMqConfig;
import famel.com.safepix_async.domain.dto.PixDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class PixService {

    private final RabbitTemplate rabbitTemplate;

    public PixService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void enviarPix(PixDTO pixDTO) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.PIX_QUEUE, pixDTO);
    }
}
