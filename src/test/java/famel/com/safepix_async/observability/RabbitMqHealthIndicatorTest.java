package famel.com.safepix_async.observability;

import famel.com.safepix_async.config.RabbitMqConfig;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RabbitMqHealthIndicatorTest {

    @Test
    void deveRetornarUpQuandoFilaPrincipalExiste() {
        RabbitAdmin rabbitAdmin = mock(RabbitAdmin.class);
        Properties properties = new Properties();
        properties.put(RabbitAdmin.QUEUE_MESSAGE_COUNT, 0);
        properties.put(RabbitAdmin.QUEUE_CONSUMER_COUNT, 1);
        when(rabbitAdmin.getQueueProperties(RabbitMqConfig.PIX_QUEUE)).thenReturn(properties);

        assertThat(new RabbitMqHealthIndicator(rabbitAdmin).health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void deveRetornarDownQuandoRabbitFalha() {
        RabbitAdmin rabbitAdmin = mock(RabbitAdmin.class);
        when(rabbitAdmin.getQueueProperties(RabbitMqConfig.PIX_QUEUE))
                .thenThrow(new AmqpConnectException(new RuntimeException("offline")));

        assertThat(new RabbitMqHealthIndicator(rabbitAdmin).health().getStatus()).isEqualTo(Status.DOWN);
    }
}
