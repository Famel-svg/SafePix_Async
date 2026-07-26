package famel.com.safepix_async.service;

import famel.com.safepix_async.domain.dto.PixEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PixAntifraudClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(PixAntifraudClient.class);

    @CircuitBreaker(name = "pixAntifraud", fallbackMethod = "fallback")
    public boolean approve(PixEvent pixEvent) {
        if (Boolean.TRUE.equals(pixEvent.metadata() == null ? null : pixEvent.metadata().get("simulateAntifraudFailure"))) {
            throw new IllegalStateException("Antifraud indisponivel");
        }

        String pixKey = pixEvent.chavePix();
        if (pixKey == null) {
            return false;
        }

        String normalizedKey = pixKey.toLowerCase(Locale.ROOT);
        return !normalizedKey.contains("fraude") && !normalizedKey.contains("deny");
    }

    boolean fallback(PixEvent pixEvent, Throwable exception) {
        LOGGER.warn("Antifraud fallback acionado: pixId={}, erro={}",
                pixEvent.id(), exception.getClass().getSimpleName());
        return false;
    }
}
