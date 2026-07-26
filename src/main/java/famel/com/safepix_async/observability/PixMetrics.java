package famel.com.safepix_async.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class PixMetrics {

    public static final String PROCESSED_TOTAL = "pix.processed.total";
    public static final String FAILED_TOTAL = "pix.failed.total";
    public static final String PROCESSING_DURATION = "pix.processing.duration";

    private final Counter processedTotal;
    private final Counter failedTotal;
    private final Timer processingDuration;

    public PixMetrics(MeterRegistry meterRegistry) {
        this.processedTotal = Counter.builder(PROCESSED_TOTAL)
                .description("Total de Pix processados com sucesso")
                .register(meterRegistry);
        this.failedTotal = Counter.builder(FAILED_TOTAL)
                .description("Total de Pix com falha de processamento")
                .register(meterRegistry);
        this.processingDuration = Timer.builder(PROCESSING_DURATION)
                .description("Duracao do processamento Pix")
                .register(meterRegistry);
    }

    public void recordProcessed(Duration duration) {
        processedTotal.increment();
        processingDuration.record(duration);
    }

    public void recordFailed(Duration duration) {
        failedTotal.increment();
        processingDuration.record(duration);
    }
}
