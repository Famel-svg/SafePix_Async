package famel.com.safepix_async.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    @Test
    void deveMascararChavePixMantendoApenasQuatroUltimosCaracteres() {
        assertThat(SensitiveDataMasker.maskPixKey("cliente@email.com")).isEqualTo("***.com");
        assertThat(SensitiveDataMasker.maskPixKey("1234")).isEqualTo("***");
        assertThat(SensitiveDataMasker.maskPixKey(null)).isEqualTo("***");
    }
}
