package famel.com.safepix_async.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PixEvent(
        UUID id,
        String chavePix,
        BigDecimal valor,
        Instant timestamp,
        Map<String, Object> metadata,
        String correlationId,
        String tenantId,
        int retryCount
) {
}
