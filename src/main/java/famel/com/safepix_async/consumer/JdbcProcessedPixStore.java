package famel.com.safepix_async.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Store de idempotencia persistida no PostgreSQL (tabela {@code pix_processing}).
 *
 * <p>Uma reserva em {@code PROCESSING} mais antiga que {@code processingTimeout} e considerada
 * abandonada (ex.: instancia caiu no meio do processamento) e pode ser assumida novamente.
 */
@Repository
public class JdbcProcessedPixStore implements ProcessedPixStore {

    static final String PROCESSING = "PROCESSING";
    static final String PROCESSED = "PROCESSED";

    private final JdbcClient jdbcClient;
    private final Duration processingTimeout;
    private final Clock clock;

    @Autowired
    public JdbcProcessedPixStore(JdbcClient jdbcClient,
            @Value("${safepix.idempotency.processing-timeout:PT5M}") Duration processingTimeout) {
        this(jdbcClient, processingTimeout, Clock.systemUTC());
    }

    JdbcProcessedPixStore(JdbcClient jdbcClient, Duration processingTimeout, Clock clock) {
        this.jdbcClient = jdbcClient;
        this.processingTimeout = processingTimeout;
        this.clock = clock;
    }

    @Override
    public boolean tryStart(UUID pixId) {
        Instant now = clock.instant();
        int affected = jdbcClient.sql("""
                        INSERT INTO pix_processing (pix_id, status, created_at, updated_at)
                        VALUES (:pixId, :processing, :now, :now)
                        ON CONFLICT (pix_id) DO UPDATE
                            SET updated_at = EXCLUDED.updated_at
                            WHERE pix_processing.status = :processing
                              AND pix_processing.updated_at < :staleBefore
                        """)
                .param("pixId", pixId)
                .param("processing", PROCESSING)
                .param("now", Timestamp.from(now))
                .param("staleBefore", Timestamp.from(now.minus(processingTimeout)))
                .update();
        return affected == 1;
    }

    @Override
    public void markProcessed(UUID pixId) {
        jdbcClient.sql("""
                        UPDATE pix_processing
                           SET status = :processed, updated_at = :now
                         WHERE pix_id = :pixId
                        """)
                .param("processed", PROCESSED)
                .param("now", Timestamp.from(clock.instant()))
                .param("pixId", pixId)
                .update();
    }

    @Override
    public void release(UUID pixId) {
        jdbcClient.sql("DELETE FROM pix_processing WHERE pix_id = :pixId AND status = :processing")
                .param("pixId", pixId)
                .param("processing", PROCESSING)
                .update();
    }

    @Override
    public boolean isProcessed(UUID pixId) {
        return jdbcClient.sql("SELECT COUNT(*) FROM pix_processing WHERE pix_id = :pixId AND status = :processed")
                .param("pixId", pixId)
                .param("processed", PROCESSED)
                .query(Long.class)
                .single() > 0;
    }

    @Override
    public long processedCount() {
        return jdbcClient.sql("SELECT COUNT(*) FROM pix_processing WHERE status = :processed")
                .param("processed", PROCESSED)
                .query(Long.class)
                .single();
    }
}
