package famel.com.safepix_async.service;

import famel.com.safepix_async.config.RabbitMqConfig;
import famel.com.safepix_async.domain.dto.PixEvent;
import famel.com.safepix_async.observability.PixTracing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;

@ExtendWith(MockitoExtension.class)
class PixServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private PixTracing pixTracing;

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
                0,
                RabbitMqConfig.PAYLOAD_VERSION
        );

        pixService.enviarPix(pixEvent);

        ArgumentCaptor<MessagePostProcessor> processorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitMqConfig.PIX_QUEUE), same(pixEvent),
                processorCaptor.capture());

        Message message = new Message(new byte[0], new MessageProperties());
        Message processed = processorCaptor.getValue().postProcessMessage(message);

        assertThat(processed.getMessageProperties().getHeaders())
                .containsEntry(RabbitMqConfig.HEADER_CORRELATION_ID, "corr-123")
                .containsEntry(RabbitMqConfig.HEADER_TENANT_ID, "default")
                .containsEntry(RabbitMqConfig.HEADER_RETRY_COUNT, 0)
                .containsEntry(RabbitMqConfig.HEADER_PAYLOAD_VERSION, RabbitMqConfig.PAYLOAD_VERSION);
        verify(pixTracing).injectRabbitContext(any(MessageProperties.class));
        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("pixId")).isNull();
    }

    @Test
    void deveRestaurarMdcAnteriorAposEnviarPix() {
        PixEvent pixEvent = new PixEvent(
                UUID.randomUUID(),
                "cliente@email.com",
                BigDecimal.TEN,
                Instant.now(),
                null,
                "corr-service",
                "default",
                0,
                RabbitMqConfig.PAYLOAD_VERSION
        );
        MDC.put("correlationId", "parent-corr");

        pixService.enviarPix(pixEvent);

        assertThat(MDC.get("correlationId")).isEqualTo("parent-corr");
        assertThat(MDC.get("pixId")).isNull();
        MDC.clear();
    }
}
