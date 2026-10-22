package famel.com.safepix_async.consumer;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class JdbcProcessedPixStoreIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    private static DriverManagerDataSource dataSource;
    private JdbcClient jdbcClient;

    @BeforeAll
    static void migrate() {
        dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
    }

    @BeforeEach
    void cleanTable() {
        jdbcClient = JdbcClient.create(dataSource);
        jdbcClient.sql("DELETE FROM pix_processing").update();
    }

    @Test
    void deveReservarPixApenasUmaVez() {
        JdbcProcessedPixStore store = store(Clock.systemUTC());
        UUID pixId = UUID.randomUUID();

        assertThat(store.tryStart(pixId)).isTrue();
        assertThat(store.tryStart(pixId)).isFalse();
    }

    @Test
    void deveMarcarComoProcessadoEImpedirNovoProcessamento() {
        JdbcProcessedPixStore store = store(Clock.systemUTC());
        UUID pixId = UUID.randomUUID();

        store.tryStart(pixId);
        store.markProcessed(pixId);

        assertThat(store.isProcessed(pixId)).isTrue();
        assertThat(store.processedCount()).isOne();
        assertThat(store.tryStart(pixId)).isFalse();
    }

    @Test
    void deveLiberarReservaParaNovaTentativa() {
        JdbcProcessedPixStore store = store(Clock.systemUTC());
        UUID pixId = UUID.randomUUID();

        store.tryStart(pixId);
        store.release(pixId);

        assertThat(store.isProcessed(pixId)).isFalse();
        assertThat(store.tryStart(pixId)).isTrue();
    }

    @Test
    void naoDeveLiberarPixJaProcessado() {
        JdbcProcessedPixStore store = store(Clock.systemUTC());
        UUID pixId = UUID.randomUUID();

        store.tryStart(pixId);
        store.markProcessed(pixId);
        store.release(pixId);

        assertThat(store.isProcessed(pixId)).isTrue();
    }

    @Test
    void deveAssumirReservaAbandonadaAposTimeout() {
        Instant inicio = Instant.parse("2026-01-01T10:00:00Z");
        UUID pixId = UUID.randomUUID();

        assertThat(store(Clock.fixed(inicio, ZoneOffset.UTC)).tryStart(pixId)).isTrue();
        assertThat(store(Clock.fixed(inicio.plusSeconds(60), ZoneOffset.UTC)).tryStart(pixId)).isFalse();
        assertThat(store(Clock.fixed(inicio.plus(Duration.ofMinutes(6)), ZoneOffset.UTC)).tryStart(pixId)).isTrue();
    }

    private JdbcProcessedPixStore store(Clock clock) {
        return new JdbcProcessedPixStore(jdbcClient, Duration.ofMinutes(5), clock);
    }
}
