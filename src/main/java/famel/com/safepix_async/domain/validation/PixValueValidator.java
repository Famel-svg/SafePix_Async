package famel.com.safepix_async.domain.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class PixValueValidator implements ConstraintValidator<ValidPixValue, BigDecimal> {

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        return value == null || value.compareTo(BigDecimal.ZERO) > 0;
    }
}
