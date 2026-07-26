package famel.com.safepix_async.domain.dto;

import famel.com.safepix_async.domain.validation.ValidPixValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PixRequest(
        @NotNull
        UUID id,

        @NotBlank
        String chavePix,

        @NotNull
        @ValidPixValue
        BigDecimal valor,

        @NotNull
        Instant timestamp,

        Map<String, Object> metadata
) {

    public PixEvent toEvent(String correlationId, String tenantId) {
        return new PixEvent(id, chavePix, valor, timestamp, metadata, correlationId, tenantId, 0);
    }
}
