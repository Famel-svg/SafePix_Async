package famel.com.safepix_async.consumer;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProcessedPixStore {

    private final Set<UUID> processing = ConcurrentHashMap.newKeySet();
    private final Set<UUID> processed = ConcurrentHashMap.newKeySet();

    public boolean tryStart(UUID pixId) {
        return !processed.contains(pixId) && processing.add(pixId);
    }

    public void markProcessed(UUID pixId) {
        processing.remove(pixId);
        processed.add(pixId);
    }

    public void release(UUID pixId) {
        processing.remove(pixId);
    }

    public boolean isProcessed(UUID pixId) {
        return processed.contains(pixId);
    }

    public int processedCount() {
        return processed.size();
    }
}
