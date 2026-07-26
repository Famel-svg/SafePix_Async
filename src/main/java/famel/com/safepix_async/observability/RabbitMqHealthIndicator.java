package famel.com.safepix_async.observability;

import famel.com.safepix_async.config.RabbitMqConfig;
import java.util.Properties;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class RabbitMqHealthIndicator implements HealthIndicator {

    private final RabbitAdmin rabbitAdmin;

    public RabbitMqHealthIndicator(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    @Override
    public Health health() {
        try {
            Properties properties = rabbitAdmin.getQueueProperties(RabbitMqConfig.PIX_QUEUE);
            if (properties == null) {
                return Health.down()
                        .withDetail("queue", RabbitMqConfig.PIX_QUEUE)
                        .withDetail("reason", "queue not found")
                        .build();
            }
            return Health.up()
                    .withDetail("queue", RabbitMqConfig.PIX_QUEUE)
                    .withDetail("messageCount", properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT))
                    .withDetail("consumerCount", properties.get(RabbitAdmin.QUEUE_CONSUMER_COUNT))
                    .build();
        } catch (AmqpException exception) {
            return Health.down(exception)
                    .withDetail("queue", RabbitMqConfig.PIX_QUEUE)
                    .build();
        }
    }
}
