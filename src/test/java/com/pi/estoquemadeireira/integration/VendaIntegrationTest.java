package com.pi.estoquemadeireira.integration;

import com.jayway.jsonpath.JsonPath;
import com.pi.estoquemadeireira.entity.Cliente;
import com.pi.estoquemadeireira.entity.Produto;
import com.pi.estoquemadeireira.entity.UnidadeMedida;
import com.pi.estoquemadeireira.entity.Venda;
import com.pi.estoquemadeireira.repository.ClienteRepository;
import com.pi.estoquemadeireira.repository.ProdutoRepository;
import com.pi.estoquemadeireira.repository.VendaRepository;
import com.pi.estoquemadeireira.dto.ItemVendaRequestDTO;
import com.pi.estoquemadeireira.dto.VendaRequestDTO;
import com.pi.estoquemadeireira.service.VendaService;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Testes de ponta a ponta da regra principal do sistema: a venda debita o estoque
// na mesma transacao e, se qualquer item falhar, nada e gravado.
// Sem @Transactional na classe de proposito: cada requisicao precisa commitar ou
// fazer rollback de verdade para que a atomicidade seja comprovada no banco.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VendaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private VendaService vendaService;

    private Cliente cliente;
    private Produto tabua;
    private Produto ripa;

    @BeforeEach
    void setUp() {
        cliente = clienteRepository.save(new Cliente("Cliente Venda Teste", "529.982.247-25", null,
                null, null, null, null, null, null));
        tabua = produtoRepository.save(new Produto("Tabua Venda Teste", UnidadeMedida.M3,
                new BigDecimal("10.000"), new BigDecimal("100.00"), BigDecimal.ONE));
        ripa = produtoRepository.save(new Produto("Ripa Venda Teste", UnidadeMedida.UNIDADE,
                new BigDecimal("50"), new BigDecimal("10.50"), BigDecimal.ONE));
    }

    private BigDecimal estoqueNoBanco(Produto produto) {
        return produtoRepository.findById(produto.getId()).orElseThrow().getQuantidadeEstoque();
    }

    private String vendaJson(Long clienteId, Long produto1, String qtd1, Long produto2, String qtd2) {
        return """
                {"clienteId": %d, "itens": [
                    {"produtoId": %d, "quantidade": %s},
                    {"produtoId": %d, "quantidade": %s}
                ]}
                """.formatted(clienteId, produto1, qtd1, produto2, qtd2);
    }

    @Test
    @DisplayName("POST /vendas grava venda e itens, calcula total e debita o estoque no banco")
    void criarVendaDebitaEstoque() throws Exception {
        long vendasAntes = vendaRepository.count();

        MvcResult result = mockMvc.perform(post("/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vendaJson(cliente.getId(), tabua.getId(), "2.5", ripa.getId(), "3")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.clienteId").value(cliente.getId()))
                .andExpect(jsonPath("$.dataVenda").exists())
                // 2.5 x 100.00 + 3 x 10.50 = 250.00 + 31.50
                .andExpect(jsonPath("$.valorTotal").value(281.50))
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.itens[0].nomeProduto").value("Tabua Venda Teste"))
                .andExpect(jsonPath("$.itens[0].subtotal").value(250.00))
                .andExpect(jsonPath("$.itens[1].precoUnitarioNoMomento").value(10.50))
                .andExpect(jsonPath("$.itens[1].subtotal").value(31.50))
                .andReturn();

        long vendaId = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
        Venda venda = vendaRepository.findById(vendaId).orElseThrow();
        assertThat(venda.getValorTotal()).isEqualByComparingTo("281.50");
        assertThat(vendaRepository.count()).isEqualTo(vendasAntes + 1);
        assertThat(estoqueNoBanco(tabua)).isEqualByComparingTo("7.5");
        assertThat(estoqueNoBanco(ripa)).isEqualByComparingTo("47");

        mockMvc.perform(get("/produtos/{id}/estoque", tabua.getId()))
                .andExpect(jsonPath("$.quantidadeDisponivel").value(7.5));
    }

    @Test
    @DisplayName("Preco do item fica congelado: alterar o produto depois nao muda a venda")
    void precoCongeladoNoMomentoDaVenda() throws Exception {
        MvcResult result = mockMvc.perform(post("/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vendaJson(cliente.getId(), tabua.getId(), "1", ripa.getId(), "1")))
                .andExpect(status().isCreated())
                .andReturn();
        long vendaId = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();

        tabua.setPrecoUnitario(new BigDecimal("999.00"));
        tabua.setQuantidadeEstoque(estoqueNoBanco(tabua));
        produtoRepository.save(tabua);

        mockMvc.perform(get("/vendas/{id}", vendaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].precoUnitarioNoMomento").value(100.00))
                .andExpect(jsonPath("$.valorTotal").value(110.50));
    }

    @Test
    @DisplayName("Estoque insuficiente em um item rejeita a venda inteira com 422 e nao debita nada")
    void estoqueInsuficienteRejeitaVendaInteira() throws Exception {
        long vendasAntes = vendaRepository.count();

        // O 1o item tem estoque; o 2o pede mais do que existe
        mockMvc.perform(post("/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vendaJson(cliente.getId(), tabua.getId(), "2", ripa.getId(), "51")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message", containsString("Estoque insuficiente")))
                .andExpect(jsonPath("$.message", containsString("Ripa Venda Teste")));

        assertThat(vendaRepository.count()).isEqualTo(vendasAntes);
        assertThat(estoqueNoBanco(tabua)).isEqualByComparingTo("10");
        assertThat(estoqueNoBanco(ripa)).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("Vender exatamente o saldo disponivel zera o estoque sem erro")
    void venderTodoOEstoque() throws Exception {
        mockMvc.perform(post("/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vendaJson(cliente.getId(), tabua.getId(), "10", ripa.getId(), "50")))
                .andExpect(status().isCreated());

        assertThat(estoqueNoBanco(tabua)).isEqualByComparingTo("0");
        assertThat(estoqueNoBanco(ripa)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("POST /vendas com cliente inexistente retorna 404 e nao debita estoque")
    void clienteInexistente() throws Exception {
        mockMvc.perform(post("/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vendaJson(999_999L, tabua.getId(), "1", ripa.getId(), "1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cliente nao encontrado: id 999999"));

        assertThat(estoqueNoBanco(tabua)).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("POST /vendas com produto inexistente retorna 404 e desfaz o debito dos itens anteriores")
    void produtoInexistente() throws Exception {
        mockMvc.perform(post("/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vendaJson(cliente.getId(), tabua.getId(), "1", 999_999L, "1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Produto nao encontrado: id 999999"));

        assertThat(estoqueNoBanco(tabua)).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("POST /vendas sem itens ou com quantidade invalida retorna 400")
    void vendaInvalida() throws Exception {
        mockMvc.perform(post("/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\": %d, \"itens\": []}".formatted(cliente.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("ao menos um item")));

        mockMvc.perform(post("/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vendaJson(cliente.getId(), tabua.getId(), "0", ripa.getId(), "-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("quantidade deve ser maior que zero")));

        assertThat(estoqueNoBanco(tabua)).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("GET /vendas lista as vendas gravadas e GET /vendas/{id} inexistente retorna 404")
    void listarEBuscarVendas() throws Exception {
        mockMvc.perform(post("/vendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vendaJson(cliente.getId(), tabua.getId(), "1", ripa.getId(), "1")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/vendas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.clienteId == %d)]".formatted(cliente.getId())).exists());

        mockMvc.perform(get("/vendas/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Venda nao encontrada: id 999999"));
    }

    @Test
    @DisplayName("Vendas simultaneas dos mesmos produtos debitam todo o estoque, sem baixa perdida nem deadlock")
    void vendasConcorrentes() throws Exception {
        int vendas = 8;
        // Metade das vendas lista os itens em ordem invertida: sem trava ordenada isso gera deadlock
        List<Callable<Object>> tarefas = IntStream.range(0, vendas)
                .<Callable<Object>>mapToObj(n -> () -> vendaService.criar(new VendaRequestDTO(cliente.getId(),
                        n % 2 == 0
                                ? List.of(new ItemVendaRequestDTO(tabua.getId(), BigDecimal.ONE),
                                          new ItemVendaRequestDTO(ripa.getId(), BigDecimal.ONE))
                                : List.of(new ItemVendaRequestDTO(ripa.getId(), BigDecimal.ONE),
                                          new ItemVendaRequestDTO(tabua.getId(), BigDecimal.ONE)))))
                .toList();

        ExecutorService pool = Executors.newFixedThreadPool(vendas);
        try {
            for (Future<Object> f : pool.invokeAll(tarefas)) {
                f.get(); // relanca a excecao se alguma venda falhou
            }
        } finally {
            pool.shutdown();
        }

        assertThat(estoqueNoBanco(tabua)).isEqualByComparingTo("2");
        assertThat(estoqueNoBanco(ripa)).isEqualByComparingTo("42");
    }
}
