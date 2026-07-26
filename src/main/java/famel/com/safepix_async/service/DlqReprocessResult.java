package famel.com.safepix_async.service;

public record DlqReprocessResult(int reprocessed, String targetQueue) {
}
