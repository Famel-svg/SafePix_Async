package famel.com.safepix_async.service;

import famel.com.safepix_async.config.RabbitMqConfig;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DlqAdminServiceTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final DlqAdminService service = new DlqAdminService(rabbitTemplate);

    @Test
    void deveListarMensagensDaDlqEDevolverParaFila() {
        Message message = message("payload", "message-1");
        when(rabbitTemplate.receive(RabbitMqConfig.PIX_DLQ)).thenReturn(message, null);

        List<DlqMessageView> messages = service.listMessages(10);

        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst().messageId()).isEqualTo("message-1");
        assertThat(messages.getFirst().body()).isEqualTo("payload");
        verify(rabbitTemplate).send(RabbitMqConfig.PIX_DLQ, message);
    }

    @Test
    void deveReprocessarMensagensDaDlqParaFilaPrincipal() {
        Message message = message("payload", "message-2");
        when(rabbitTemplate.receive(RabbitMqConfig.PIX_DLQ)).thenReturn(message, null);

        DlqReprocessResult result = service.reprocessMessages(10);

        assertThat(result.reprocessed()).isOne();
        assertThat(result.targetQueue()).isEqualTo(RabbitMqConfig.PIX_QUEUE);
        assertThat(message.getMessageProperties().getHeaders())
                .containsEntry("x-reprocessed-from-dlq", true);
        verify(rabbitTemplate).send(RabbitMqConfig.PIX_REPROCESS_EXCHANGE, RabbitMqConfig.PIX_QUEUE, message);
    }

    private Message message(String body, String messageId) {
        MessageProperties properties = new MessageProperties();
        properties.setMessageId(messageId);
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setHeader("x-exception-message", "erro");
        return new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }
}
