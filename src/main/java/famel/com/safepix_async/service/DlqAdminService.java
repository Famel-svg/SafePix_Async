package famel.com.safepix_async.service;

import famel.com.safepix_async.config.RabbitMqConfig;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class DlqAdminService {

    private static final int MAX_LIMIT = 100;

    private final RabbitTemplate rabbitTemplate;

    public DlqAdminService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public List<DlqMessageView> listMessages(int limit) {
        List<Message> messages = drain(limit);
        messages.forEach(message -> rabbitTemplate.send(RabbitMqConfig.PIX_DLQ, message));
        return messages.stream()
                .map(this::toView)
                .toList();
    }

    public DlqReprocessResult reprocessMessages(int limit) {
        List<Message> messages = drain(limit);
        messages.forEach(message -> {
            message.getMessageProperties().setHeader("x-reprocessed-from-dlq", true);
            rabbitTemplate.send(RabbitMqConfig.PIX_REPROCESS_EXCHANGE, RabbitMqConfig.PIX_QUEUE, message);
        });
        return new DlqReprocessResult(messages.size(), RabbitMqConfig.PIX_QUEUE);
    }

    private List<Message> drain(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        List<Message> messages = new ArrayList<>();
        for (int index = 0; index < safeLimit; index++) {
            Message message = rabbitTemplate.receive(RabbitMqConfig.PIX_DLQ);
            if (message == null) {
                break;
            }
            messages.add(message);
        }
        return messages;
    }

    private DlqMessageView toView(Message message) {
        return new DlqMessageView(
                message.getMessageProperties().getMessageId(),
                message.getMessageProperties().getContentType(),
                message.getBody().length,
                new String(message.getBody(), StandardCharsets.UTF_8),
                message.getMessageProperties().getHeaders());
    }
}
