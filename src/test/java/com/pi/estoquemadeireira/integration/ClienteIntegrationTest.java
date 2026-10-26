package com.pi.estoquemadeireira.integration;

import com.jayway.jsonpath.JsonPath;
import com.pi.estoquemadeireira.entity.Cliente;
import com.pi.estoquemadeireira.repository.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClienteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    private Cliente salvarCliente(String nome) {
        return clienteRepository.save(new Cliente(nome, "529.982.247-25", "11999990000",
                "cliente@email.com", "01001-000", "Praca da Se", "Se", "Sao Paulo", "SP"));
    }

    @Test
    @DisplayName("POST /clientes com CNPJ valido persiste no banco e retorna 201")
    void criarCliente() throws Exception {
        String json = """
                {"nome": "Serraria Boa Madeira LTDA", "cpfCnpj": "11.444.777/0001-61",
                 "telefone": "4730301010", "email": "compras@boamadeira.com",
                 "cep": "89010-000", "logradouro": "Rua XV de Novembro", "bairro": "Centro",
                 "cidade": "Blumenau", "uf": "SC"}
                """;

        MvcResult result = mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Serraria Boa Madeira LTDA"))
                .andExpect(jsonPath("$.cidade").value("Blumenau"))
                .andReturn();

        long id = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
        Cliente salvo = clienteRepository.findById(id).orElseThrow();
        assertThat(salvo.getCpfCnpj()).isEqualTo("11.444.777/0001-61");
        assertThat(salvo.getEmail()).isEqualTo("compras@boamadeira.com");
        assertThat(salvo.getUf()).isEqualTo("SC");
    }

    @Test
    @DisplayName("POST /clientes com CPF, email, CEP e UF invalidos retorna 400 e nao persiste")
    void criarClienteInvalido() throws Exception {
        long totalAntes = clienteRepository.count();
        String json = """
                {"nome": "Cliente Invalido", "cpfCnpj": "123.456.789-00",
                 "email": "nao-e-email", "cep": "123", "uf": "SPX"}
                """;

        mockMvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cpfCnpj")))
                .andExpect(jsonPath("$.message", containsString("email")))
                .andExpect(jsonPath("$.message", containsString("cep")))
                .andExpect(jsonPath("$.message", containsString("uf")));

        assertThat(clienteRepository.count()).isEqualTo(totalAntes);
    }

    @Test
    @DisplayName("GET /clientes/{id} retorna o cliente salvo")
    void buscarPorId() throws Exception {
        Cliente cliente = salvarCliente("Carpintaria Busca Id");

        mockMvc.perform(get("/clientes/{id}", cliente.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cliente.getId()))
                .andExpect(jsonPath("$.nome").value("Carpintaria Busca Id"))
                .andExpect(jsonPath("$.cpfCnpj").value("529.982.247-25"));
    }

    @Test
    @DisplayName("GET /clientes/{id} inexistente retorna 404")
    void buscarPorIdInexistente() throws Exception {
        mockMvc.perform(get("/clientes/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cliente nao encontrado: id 999999"));
    }

    @Test
    @DisplayName("GET /clientes?nome= filtra pelo nome ignorando maiusculas")
    void listarComFiltro() throws Exception {
        salvarCliente("Moveis Rusticos Filtro");

        mockMvc.perform(get("/clientes").param("nome", "RUSTICOS FILTRO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Moveis Rusticos Filtro"));
    }

    @Test
    @DisplayName("GET /clientes sem filtro inclui os dados de exemplo da migration V4")
    void listarTodos() throws Exception {
        mockMvc.perform(get("/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nome == 'Construtora Horizonte LTDA')]").exists());
    }

    @Test
    @DisplayName("PUT /clientes/{id} atualiza o registro no banco")
    void atualizarCliente() throws Exception {
        Cliente cliente = salvarCliente("Nome Antigo");
        String json = """
                {"nome": "Nome Novo", "cpfCnpj": "529.982.247-25", "telefone": "4133224455",
                 "email": "novo@email.com", "cep": "80010000", "logradouro": "Rua Nova",
                 "bairro": "Centro", "cidade": "Curitiba", "uf": "PR"}
                """;

        mockMvc.perform(put("/clientes/{id}", cliente.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Novo"));

        Cliente atualizado = clienteRepository.findById(cliente.getId()).orElseThrow();
        assertThat(atualizado.getNome()).isEqualTo("Nome Novo");
        assertThat(atualizado.getEmail()).isEqualTo("novo@email.com");
        assertThat(atualizado.getCidade()).isEqualTo("Curitiba");
    }

    @Test
    @DisplayName("PUT /clientes/{id} inexistente retorna 404")
    void atualizarClienteInexistente() throws Exception {
        mockMvc.perform(put("/clientes/{id}", 999_999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Qualquer\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /clientes/{id} remove do banco e retorna 204")
    void excluirCliente() throws Exception {
        Cliente cliente = salvarCliente("Cliente Descartavel");

        mockMvc.perform(delete("/clientes/{id}", cliente.getId()))
                .andExpect(status().isNoContent());

        assertThat(clienteRepository.existsById(cliente.getId())).isFalse();
    }

    @Test
    @DisplayName("DELETE /clientes/{id} inexistente retorna 404")
    void excluirClienteInexistente() throws Exception {
        mockMvc.perform(delete("/clientes/{id}", 999_999))
                .andExpect(status().isNotFound());
    }
}
