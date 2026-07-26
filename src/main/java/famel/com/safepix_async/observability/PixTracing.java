package famel.com.safepix_async.observability;

import famel.com.safepix_async.domain.dto.PixEvent;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Map;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Component;

@Component
public class PixTracing {

    private static final TextMapSetter<MessageProperties> RABBIT_HEADER_SETTER =
            (carrier, key, value) -> carrier.setHeader(key, value);

    private static final TextMapGetter<Map<String, Object>> RABBIT_HEADER_GETTER =
            new TextMapGetter<>() {
                @Override
                public Iterable<String> keys(Map<String, Object> carrier) {
                    return carrier.keySet();
                }

                @Override
                public String get(Map<String, Object> carrier, String key) {
                    Object value = carrier.get(key);
                    return value == null ? null : value.toString();
                }
            };

    private final Tracer tracer = GlobalOpenTelemetry.getTracer("famel.com.safepix_async");

    public void injectRabbitContext(MessageProperties messageProperties) {
        GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(Context.current(), messageProperties, RABBIT_HEADER_SETTER);
    }

    public Span startPixProcessingSpan(PixEvent pixEvent, Map<String, Object> headers) {
        Context parentContext = GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), headers, RABBIT_HEADER_GETTER);
        return tracer.spanBuilder("processamento PIX")
                .setParent(parentContext)
                .setSpanKind(SpanKind.CONSUMER)
                .setAttribute("pix.value", pixEvent.valor() == null ? 0.0 : pixEvent.valor().doubleValue())
                .setAttribute("pix.key_type", keyType(pixEvent.chavePix()))
                .startSpan();
    }

    public void endSpan(Span span, Throwable exception) {
        if (exception != null) {
            span.recordException(exception);
        }
        span.end();
    }

    private String keyType(String pixKey) {
        if (pixKey == null) {
            return "unknown";
        }
        if (pixKey.contains("@")) {
            return "email";
        }
        if (pixKey.matches("\\+?\\d{10,13}")) {
            return "phone";
        }
        return "random";
    }
}
