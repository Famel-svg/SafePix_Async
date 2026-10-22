package famel.com.safepix_async.consumer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementacao em memoria usada apenas em testes unitarios.
 */
class InMemoryProcessedPixStore implements ProcessedPixStore {

    private final Set<UUID> processing = ConcurrentHashMap.newKeySet();
    private final Set<UUID> processed = ConcurrentHashMap.newKeySet();

    @Override
    public boolean tryStart(UUID pixId) {
        return !processed.contains(pixId) && processing.add(pixId);
    }

    @Override
    public void markProcessed(UUID pixId) {
        processing.remove(pixId);
        processed.add(pixId);
    }

    @Override
    public void release(UUID pixId) {
        processing.remove(pixId);
    }

    @Override
    public boolean isProcessed(UUID pixId) {
        return processed.contains(pixId);
    }

    @Override
    public long processedCount() {
        return processed.size();
    }
}
