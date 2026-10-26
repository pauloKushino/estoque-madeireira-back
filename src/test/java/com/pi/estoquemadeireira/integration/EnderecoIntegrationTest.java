package com.pi.estoquemadeireira.integration;

import com.pi.estoquemadeireira.client.ViaCepClient;
import com.pi.estoquemadeireira.client.ViaCepResponse;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A API externa (ViaCEP) e substituida por um mock: o teste nao pode depender de internet,
// mas todo o resto (controller, service, tratamento de erros) roda de verdade.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnderecoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ViaCepClient viaCepClient;

    @Test
    @DisplayName("GET /enderecos/{cep} com mascara consulta o ViaCEP sem mascara e retorna o endereco")
    void consultarCep() throws Exception {
        when(viaCepClient.consultarCep("01001000")).thenReturn(new ViaCepResponse(
                "01001-000", "Praca da Se", "Se", "Sao Paulo", "SP", null));

        mockMvc.perform(get("/enderecos/{cep}", "01001-000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cep").value("01001-000"))
                .andExpect(jsonPath("$.logradouro").value("Praca da Se"))
                .andExpect(jsonPath("$.cidade").value("Sao Paulo"))
                .andExpect(jsonPath("$.uf").value("SP"));

        verify(viaCepClient).consultarCep("01001000");
    }

    @Test
    @DisplayName("CEP com formato invalido retorna 400 sem chamar a API externa")
    void cepInvalido() throws Exception {
        mockMvc.perform(get("/enderecos/{cep}", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CEP invalido: informe 8 digitos numericos"));

        verify(viaCepClient, never()).consultarCep(anyString());
    }

    @Test
    @DisplayName("CEP inexistente na base do ViaCEP retorna 404")
    void cepNaoEncontrado() throws Exception {
        when(viaCepClient.consultarCep("99999999")).thenReturn(new ViaCepResponse(
                null, null, null, null, null, true));

        mockMvc.perform(get("/enderecos/{cep}", "99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("CEP nao encontrado: 99999999"));
    }

    @Test
    @DisplayName("Falha de comunicacao com o ViaCEP retorna 502")
    void viaCepIndisponivel() throws Exception {
        when(viaCepClient.consultarCep(anyString())).thenThrow(FeignException.class);

        mockMvc.perform(get("/enderecos/{cep}", "01001000"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("Servico externo indisponivel no momento. Tente novamente."));
    }

    @Test
    @DisplayName("Erro inesperado retorna 500 com mensagem generica, sem expor detalhes internos")
    void erroInesperado() throws Exception {
        when(viaCepClient.consultarCep(anyString())).thenThrow(new IllegalStateException("detalhe interno"));

        mockMvc.perform(get("/enderecos/{cep}", "01001000"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Erro interno inesperado. Contate o suporte."));
    }

    @Test
    @DisplayName("Rota inexistente retorna 404 no formato ApiError")
    void rotaInexistente() throws Exception {
        mockMvc.perform(get("/rota-que-nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Recurso nao encontrado: /rota-que-nao-existe"));
    }
}
