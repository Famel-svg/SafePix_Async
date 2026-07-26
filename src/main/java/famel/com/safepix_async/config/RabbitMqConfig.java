package famel.com.safepix_async.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String PIX_QUEUE = "pix.recebido.v1";
    public static final String PIX_DLQ = "pix.recebido.v1.dlq";
    public static final String PIX_REPROCESS_EXCHANGE = "pix.recebido.v1.reprocess";
    public static final String HEADER_CORRELATION_ID = "x-correlation-id";
    public static final String HEADER_TENANT_ID = "x-tenant-id";
    public static final String HEADER_RETRY_COUNT = "x-retry-count";
    public static final String HEADER_PAYLOAD_VERSION = "x-payload-version";
    public static final String PAYLOAD_VERSION = "1";

    @Bean
    public Queue pixQueue(@Value("${safepix.rabbitmq.message-ttl-ms:3600000}") int messageTtlMs) {
        return QueueBuilder.durable(PIX_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(PIX_DLQ)
                .ttl(messageTtlMs)
                .build();
    }

    @Bean
    public Queue pixDeadLetterQueue() {
        return QueueBuilder.durable(PIX_DLQ)
                .deadLetterExchange(PIX_REPROCESS_EXCHANGE)
                .deadLetterRoutingKey(PIX_QUEUE)
                .build();
    }

    @Bean
    public DirectExchange pixReprocessExchange() {
        return new DirectExchange(PIX_REPROCESS_EXCHANGE);
    }

    @Bean
    public Binding pixReprocessBinding(Queue pixQueue, DirectExchange pixReprocessExchange) {
        return BindingBuilder.bind(pixQueue)
                .to(pixReprocessExchange)
                .with(PIX_QUEUE);
    }

    @Bean
    @SuppressWarnings("removal")
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter(rabbitObjectMapper());
    }

    @Bean
    public SimpleRabbitListenerContainerFactory pixRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            RabbitTemplate rabbitTemplate,
            @Value("${safepix.rabbitmq.listener.retry.max-attempts:3}") int maxAttempts,
            @Value("${safepix.rabbitmq.listener.retry.initial-interval-ms:500}") long initialIntervalMs,
            @Value("${safepix.rabbitmq.listener.retry.multiplier:2.0}") double multiplier,
            @Value("${safepix.rabbitmq.listener.retry.max-interval-ms:5000}") long maxIntervalMs) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setDefaultRequeueRejected(false);
        factory.setErrorHandler(new ConditionalRejectingErrorHandler());
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxRetries(Math.max(0, maxAttempts - 1))
                .backOffOptions(initialIntervalMs, multiplier, maxIntervalMs)
                .recoverer(new RepublishMessageRecoverer(rabbitTemplate, "", PIX_DLQ))
                .build());
        return factory;
    }

    private ObjectMapper rabbitObjectMapper() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(BigDecimal.class, new com.fasterxml.jackson.databind.JsonSerializer<>() {
            @Override
            public void serialize(BigDecimal value, JsonGenerator generator,
                    com.fasterxml.jackson.databind.SerializerProvider serializers) throws IOException {
                generator.writeNumber(value.setScale(2, RoundingMode.HALF_UP));
            }
        });

        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(module);
    }
}
