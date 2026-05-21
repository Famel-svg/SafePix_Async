package famel.com.safepix_async.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PixDTO(
        @NotNull
        UUID id,

        @NotBlank
        String chavePix,

        @NotNull
        @Positive
        BigDecimal valor,

        @NotNull
        Instant timestamp
) {
}
