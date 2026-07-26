package famel.com.safepix_async.observability;

public final class SensitiveDataMasker {

    private SensitiveDataMasker() {
    }

    public static String maskPixKey(String chavePix) {
        if (chavePix == null || chavePix.length() <= 4) {
            return "***";
        }
        return "***" + chavePix.substring(chavePix.length() - 4);
    }
}
