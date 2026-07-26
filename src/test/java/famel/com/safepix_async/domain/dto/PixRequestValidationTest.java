package famel.com.safepix_async.domain.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PixRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void deveAceitarPixValido() {
        PixRequest pixRequest = new PixRequest(
                UUID.randomUUID(),
                "cliente@email.com",
                BigDecimal.TEN,
                Instant.now(),
                Map.of("origem", "api")
        );

        assertThat(validator.validate(pixRequest)).isEmpty();
    }

    @Test
    void deveRejeitarChavePixEmBranco() {
        PixRequest pixRequest = new PixRequest(UUID.randomUUID(), " ", BigDecimal.TEN, Instant.now(), null);

        assertThat(validator.validate(pixRequest))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("chavePix"));
    }

    @Test
    void deveRejeitarValorNaoPositivo() {
        PixRequest pixRequest = new PixRequest(UUID.randomUUID(), "cliente@email.com", BigDecimal.ZERO, Instant.now(), null);

        assertThat(validator.validate(pixRequest))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("valor"));
    }
}
