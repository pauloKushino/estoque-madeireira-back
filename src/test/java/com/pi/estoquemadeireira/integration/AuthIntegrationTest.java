package com.pi.estoquemadeireira.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Login de ponta a ponta com os usuarios reais da migration V5 (admin/admin123 e user/user123) no H2.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String loginJson(String username, String senha) {
        return """
                {"username": "%s", "senha": "%s"}
                """.formatted(username, senha);
    }

    private String fazerLogin(String username, String senha) throws Exception {
        String resposta = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(username, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.token");
    }

    @Test
    @DisplayName("POST /auth/login com admin/admin123 devolve 200 com token e dados do usuario")
    void loginAdminValido() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(not(emptyString())))
                .andExpect(jsonPath("$.expiraEm").value(7200))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.nome").value("Administrador"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    @DisplayName("POST /auth/login com senha errada devolve 401 no formato ApiError")
    void loginSenhaErrada() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("admin", "senhaErrada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Usuario ou senha invalidos"))
                .andExpect(jsonPath("$.path").value("/auth/login"));
    }

    @Test
    @DisplayName("POST /auth/login com usuario inexistente devolve 401")
    void loginUsuarioInexistente() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("fantasma", "qualquer")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login com campos em branco devolve 400")
    void loginCamposEmBranco() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /auth/me sem token devolve 401")
    void meSemToken() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /auth/me com o token do admin devolve os dados do admin")
    void meComTokenAdmin() throws Exception {
        String token = fazerLogin("admin", "admin123");

        mockMvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.nome").value("Administrador"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    @DisplayName("GET /auth/me com o token do user devolve role USER")
    void meComTokenUser() throws Exception {
        String token = fazerLogin("user", "user123");

        mockMvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("Token adulterado e recusado com 401")
    void tokenAdulterado() throws Exception {
        String token = fazerLogin("admin", "admin123");
        String adulterado = token.substring(0, token.length() - 4) + "AAAA";

        mockMvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Rotas de negocio continuam abertas ate o PIC-15 (GET /produtos sem token = 200)")
    void rotasDeNegocioAindaAbertas() throws Exception {
        mockMvc.perform(get("/produtos"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Preflight CORS do front (localhost:4200) para /auth/me nao exige token")
    void preflightCorsLiberado() throws Exception {
        mockMvc.perform(options("/auth/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }
}
