package famel.com.safepix_async.observability;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdInterceptorTest {

    private final CorrelationIdInterceptor interceptor = new CorrelationIdInterceptor();

    @Test
    void deveUsarCorrelationIdDoHeaderELimparMdc() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationIdInterceptor.HEADER, "corr-123");

        interceptor.preHandle(request, response, new Object());

        assertThat(MDC.get(CorrelationIdInterceptor.MDC_KEY)).isEqualTo("corr-123");
        assertThat(response.getHeader(CorrelationIdInterceptor.HEADER)).isEqualTo("corr-123");

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(MDC.get(CorrelationIdInterceptor.MDC_KEY)).isNull();
    }

    @Test
    void deveGerarCorrelationIdQuandoHeaderAusente() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());

        assertThat(MDC.get(CorrelationIdInterceptor.MDC_KEY)).isNotBlank();
        assertThat(response.getHeader(CorrelationIdInterceptor.HEADER)).isEqualTo(MDC.get(CorrelationIdInterceptor.MDC_KEY));

        interceptor.afterCompletion(request, response, new Object(), null);
    }
}
