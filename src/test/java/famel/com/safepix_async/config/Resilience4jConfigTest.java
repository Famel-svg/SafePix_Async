package famel.com.safepix_async.config;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class Resilience4jConfigTest {

    @Test
    void deveConfigurarCircuitBreakerPixAntifraud() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));

        Properties properties = yaml.getObject();

        assertThat(properties)
                .containsEntry("resilience4j.circuitbreaker.instances.pixAntifraud.failure-rate-threshold", 50)
                .containsEntry("resilience4j.circuitbreaker.instances.pixAntifraud.wait-duration-in-open-state",
                        "30s");
    }
}
