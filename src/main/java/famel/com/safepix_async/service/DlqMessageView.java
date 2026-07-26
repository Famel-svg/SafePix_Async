package famel.com.safepix_async.service;

import java.util.Map;

public record DlqMessageView(
        String messageId,
        String contentType,
        int bodySize,
        String body,
        Map<String, Object> headers) {
}
