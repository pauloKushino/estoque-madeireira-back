package com.pi.estoquemadeireira.integration;

import com.jayway.jsonpath.JsonPath;
import com.pi.estoquemadeireira.entity.Produto;
import com.pi.estoquemadeireira.entity.UnidadeMedida;
import com.pi.estoquemadeireira.repository.ProdutoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sobe a aplicacao inteira (controller -> service -> repository -> H2 com as migrations do Flyway)
// e exercita os endpoints via HTTP simulado. Cada requisicao roda na propria transacao, como em producao.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProdutoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProdutoRepository produtoRepository;

    private Produto salvarProduto(String nome, String estoque, String preco) {
        return produtoRepository.save(new Produto(nome, UnidadeMedida.M3, new BigDecimal(estoque),
                new BigDecimal(preco), BigDecimal.ONE));
    }

    @Test
    @DisplayName("POST /produtos persiste o produto no banco e retorna 201")
    void criarProduto() throws Exception {
        String json = """
                {"nome": "Caibro de Peroba", "unidadeMedida": "METRO_LINEAR",
                 "quantidadeEstoque": 80.5, "precoUnitario": 22.90, "estoqueMinimo": 10}
                """;

        MvcResult result = mockMvc.perform(post("/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Caibro de Peroba"))
                .andExpect(jsonPath("$.unidadeMedida").value("METRO_LINEAR"))
                .andReturn();

        long id = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
        Produto salvo = produtoRepository.findById(id).orElseThrow();
        assertThat(salvo.getNome()).isEqualTo("Caibro de Peroba");
        assertThat(salvo.getQuantidadeEstoque()).isEqualByComparingTo("80.5");
        assertThat(salvo.getPrecoUnitario()).isEqualByComparingTo("22.90");
    }

    @Test
    @DisplayName("POST /produtos com dados invalidos retorna 400 e nao persiste nada")
    void criarProdutoInvalido() throws Exception {
        long totalAntes = produtoRepository.count();
        String json = """
                {"nome": "", "quantidadeEstoque": -1, "precoUnitario": 0}
                """;

        mockMvc.perform(post("/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/produtos"))
                .andExpect(jsonPath("$.message", containsString("nome")))
                .andExpect(jsonPath("$.message", containsString("unidadeMedida")))
                .andExpect(jsonPath("$.message", containsString("quantidadeEstoque")))
                .andExpect(jsonPath("$.message", containsString("precoUnitario")));

        assertThat(produtoRepository.count()).isEqualTo(totalAntes);
    }

    @Test
    @DisplayName("GET /produtos/{id} retorna o produto salvo no banco")
    void buscarPorId() throws Exception {
        Produto produto = salvarProduto("Prancha de Ipe", "12", "1500.00");

        mockMvc.perform(get("/produtos/{id}", produto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(produto.getId()))
                .andExpect(jsonPath("$.nome").value("Prancha de Ipe"))
                .andExpect(jsonPath("$.precoUnitario").value(1500.00));
    }

    @Test
    @DisplayName("GET /produtos/{id} inexistente retorna 404 no formato ApiError")
    void buscarPorIdInexistente() throws Exception {
        mockMvc.perform(get("/produtos/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Produto nao encontrado: id 999999"))
                .andExpect(jsonPath("$.path").value("/produtos/999999"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /produtos?nome= filtra pelo nome ignorando maiusculas")
    void listarComFiltro() throws Exception {
        salvarProduto("Sarrafo Angelim Filtro", "5", "9.90");
        salvarProduto("Sarrafo Angelim Filtro Largo", "5", "11.90");

        mockMvc.perform(get("/produtos").param("nome", "angelim filtro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nome", containsString("Angelim Filtro")));
    }

    @Test
    @DisplayName("GET /produtos sem filtro inclui os dados de exemplo da migration V4")
    void listarTodos() throws Exception {
        mockMvc.perform(get("/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nome == 'Tabua de Pinus 25cm')]").exists());
    }

    @Test
    @DisplayName("GET /produtos/{id}/estoque retorna a quantidade disponivel")
    void consultarEstoque() throws Exception {
        Produto produto = salvarProduto("Viga de Macaranduba", "42.250", "300.00");

        mockMvc.perform(get("/produtos/{id}/estoque", produto.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produtoId").value(produto.getId()))
                .andExpect(jsonPath("$.nomeProduto").value("Viga de Macaranduba"))
                .andExpect(jsonPath("$.quantidadeDisponivel").value(42.25))
                .andExpect(jsonPath("$.unidadeMedida").value("M3"));
    }

    @Test
    @DisplayName("PUT /produtos/{id} atualiza o registro no banco")
    void atualizarProduto() throws Exception {
        Produto produto = salvarProduto("Tabua Antiga", "10", "50.00");
        String json = """
                {"nome": "Tabua Reformada", "unidadeMedida": "UNIDADE",
                 "quantidadeEstoque": 15, "precoUnitario": 55.00, "estoqueMinimo": 2}
                """;

        mockMvc.perform(put("/produtos/{id}", produto.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Tabua Reformada"));

        Produto atualizado = produtoRepository.findById(produto.getId()).orElseThrow();
        assertThat(atualizado.getNome()).isEqualTo("Tabua Reformada");
        assertThat(atualizado.getUnidadeMedida()).isEqualTo(UnidadeMedida.UNIDADE);
        assertThat(atualizado.getQuantidadeEstoque()).isEqualByComparingTo("15");
        assertThat(atualizado.getPrecoUnitario()).isEqualByComparingTo("55.00");
    }

    @Test
    @DisplayName("PUT /produtos/{id} inexistente retorna 404")
    void atualizarProdutoInexistente() throws Exception {
        String json = """
                {"nome": "Qualquer", "unidadeMedida": "UNIDADE",
                 "quantidadeEstoque": 1, "precoUnitario": 1.00}
                """;

        mockMvc.perform(put("/produtos/{id}", 999_999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /produtos/{id} remove do banco e retorna 204")
    void excluirProduto() throws Exception {
        Produto produto = salvarProduto("Produto Descartavel", "1", "1.00");

        mockMvc.perform(delete("/produtos/{id}", produto.getId()))
                .andExpect(status().isNoContent());

        assertThat(produtoRepository.existsById(produto.getId())).isFalse();
        mockMvc.perform(get("/produtos/{id}", produto.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /produtos/{id} inexistente retorna 404")
    void excluirProdutoInexistente() throws Exception {
        mockMvc.perform(delete("/produtos/{id}", 999_999))
                .andExpect(status().isNotFound());
    }
}
