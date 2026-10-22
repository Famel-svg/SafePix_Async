package famel.com.safepix_async.controller;

import famel.com.safepix_async.config.SecurityConfig;
import famel.com.safepix_async.service.DlqAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DlqAdminController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.security.user.name=" + DlqAdminControllerTest.ADMIN_USER,
        "spring.security.user.password=" + DlqAdminControllerTest.ADMIN_PASSWORD
})
class DlqAdminControllerTest {

    static final String ADMIN_USER = "admin-test";
    static final String ADMIN_PASSWORD = "senha-de-teste";

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

    @Test
    void deveRejeitarSenhaIncorretaComHttpBasic() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dlq/pix").with(httpBasic(ADMIN_USER, "senha-errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"));
    }

    @Test
    void deveRejeitarUsuarioInexistenteComHttpBasic() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dlq/pix").with(httpBasic("intruso", ADMIN_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRejeitarReprocessamentoComSenhaIncorreta() throws Exception {
        mockMvc.perform(post("/api/v1/admin/dlq/pix/reprocess").with(httpBasic(ADMIN_USER, "senha-errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveAceitarCredencialCorretaComHttpBasic() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dlq/pix").with(httpBasic(ADMIN_USER, ADMIN_PASSWORD)))
                .andExpect(status().isOk());
    }
}
