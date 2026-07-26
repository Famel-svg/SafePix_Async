package famel.com.safepix_async.service;

import famel.com.safepix_async.config.RabbitMqConfig;
import famel.com.safepix_async.domain.dto.PixEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PixServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PixService pixService;

    @Test
    void deveEnviarPixParaFilaPrincipal() {
        PixEvent pixEvent = new PixEvent(
                UUID.randomUUID(),
                "cliente@email.com",
                BigDecimal.TEN,
                Instant.now(),
                null,
                "corr-123",
                "default",
                0
        );

        pixService.enviarPix(pixEvent);

        verify(rabbitTemplate).convertAndSend(RabbitMqConfig.PIX_QUEUE, pixEvent);
    }
}
