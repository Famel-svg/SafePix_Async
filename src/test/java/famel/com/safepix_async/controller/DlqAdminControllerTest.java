package famel.com.safepix_async.controller;

import famel.com.safepix_async.config.SecurityConfig;
import famel.com.safepix_async.service.DlqAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DlqAdminController.class)
@Import(SecurityConfig.class)
class DlqAdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private DlqAdminService dlqAdminService;

    @Test
    @WithAnonymousUser
    void deveExigirAutenticacaoParaAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dlq/pix"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveListarQuandoAutenticado() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dlq/pix").with(user("admin")))
                .andExpect(status().isOk());
    }
}
