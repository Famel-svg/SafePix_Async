package famel.com.safepix_async.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String PIX_QUEUE = "pix.recebido.v1";
    public static final String PIX_DLQ = "pix.recebido.v1.dlq";
    public static final String PIX_DLX = "pix.recebido.v1.dlx";

    @Bean
    public Queue pixQueue() {
        return QueueBuilder.durable(PIX_QUEUE)
                .deadLetterExchange(PIX_DLX)
                .deadLetterRoutingKey(PIX_DLQ)
                .build();
    }

    @Bean
    public Queue pixDeadLetterQueue() {
        return QueueBuilder.durable(PIX_DLQ).build();
    }

    @Bean
    public DirectExchange pixDeadLetterExchange() {
        return new DirectExchange(PIX_DLX);
    }

    @Bean
    public Binding pixDeadLetterBinding() {
        return BindingBuilder.bind(pixDeadLetterQueue())
                .to(pixDeadLetterExchange())
                .with(PIX_DLQ);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
