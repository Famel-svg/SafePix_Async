package famel.com.safepix_async.consumer;

import java.util.UUID;

/**
 * Controle de idempotencia do processamento de Pix.
 */
public interface ProcessedPixStore {

    /**
     * Tenta reservar o Pix para processamento.
     *
     * @return {@code true} se o Pix ainda nao foi processado nem esta em processamento
     */
    boolean tryStart(UUID pixId);

    void markProcessed(UUID pixId);

    void release(UUID pixId);

    boolean isProcessed(UUID pixId);

    long processedCount();
}
