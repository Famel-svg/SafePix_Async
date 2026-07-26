package famel.com.safepix_async.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PixMetricsTest {

    @Test
    void deveRegistrarMetricasDeNegocio() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PixMetrics metrics = new PixMetrics(registry);

        metrics.recordProcessed(Duration.ofMillis(5));
        metrics.recordFailed(Duration.ofMillis(7));

        assertThat(registry.get(PixMetrics.PROCESSED_TOTAL).counter().count()).isEqualTo(1);
        assertThat(registry.get(PixMetrics.FAILED_TOTAL).counter().count()).isEqualTo(1);
        assertThat(registry.get(PixMetrics.PROCESSING_DURATION).timer().count()).isEqualTo(2);
    }
}
