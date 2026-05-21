package famel.com.safepix_async.domain.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PixDTOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void deveAceitarPixValido() {
        PixDTO pixDTO = new PixDTO(UUID.randomUUID(), "cliente@email.com", BigDecimal.TEN, Instant.now());

        assertThat(validator.validate(pixDTO)).isEmpty();
    }

    @Test
    void deveRejeitarChavePixEmBranco() {
        PixDTO pixDTO = new PixDTO(UUID.randomUUID(), " ", BigDecimal.TEN, Instant.now());

        assertThat(validator.validate(pixDTO))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("chavePix"));
    }

    @Test
    void deveRejeitarValorNaoPositivo() {
        PixDTO pixDTO = new PixDTO(UUID.randomUUID(), "cliente@email.com", BigDecimal.ZERO, Instant.now());

        assertThat(validator.validate(pixDTO))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("valor"));
    }
}
