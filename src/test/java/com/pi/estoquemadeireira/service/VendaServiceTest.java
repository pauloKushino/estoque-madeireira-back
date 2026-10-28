package com.pi.estoquemadeireira.service;

import com.pi.estoquemadeireira.dto.ItemVendaRequestDTO;
import com.pi.estoquemadeireira.dto.VendaRequestDTO;
import com.pi.estoquemadeireira.dto.VendaResponseDTO;
import com.pi.estoquemadeireira.entity.ItemVenda;
import com.pi.estoquemadeireira.entity.Produto;
import com.pi.estoquemadeireira.entity.UnidadeMedida;
import com.pi.estoquemadeireira.entity.Venda;
import com.pi.estoquemadeireira.exception.EstoqueInsuficienteException;
import com.pi.estoquemadeireira.repository.ClienteRepository;
import com.pi.estoquemadeireira.repository.ProdutoRepository;
import com.pi.estoquemadeireira.repository.VendaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendaServiceTest {

    @Mock
    private VendaRepository vendaRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private VendaService vendaService;

    private static Produto produto(Long id, String nome, String estoque, String preco) {
        Produto p = new Produto(nome, UnidadeMedida.M3, new BigDecimal(estoque),
                new BigDecimal(preco), BigDecimal.ZERO);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    private static Venda vendaComItem(Long id, Produto produto) {
        Venda v = new Venda();
        ReflectionTestUtils.setField(v, "id", id);
        v.setClienteId(1L);
        ItemVenda item = new ItemVenda();
        item.setProduto(produto);
        item.setQuantidade(new BigDecimal("2"));
        item.setPrecoUnitarioNoMomento(produto.getPrecoUnitario());
        item.calcularSubtotal();
        v.adicionarItem(item);
        v.calcularValorTotal();
        return v;
    }

    @Test
    @DisplayName("listar devolve todas as vendas mapeadas")
    void listar() {
        Produto tabua = produto(1L, "Tabua", "10", "100.00");
        when(vendaRepository.findAll()).thenReturn(List.of(vendaComItem(1L, tabua), vendaComItem(2L, tabua)));

        List<VendaResponseDTO> resultado = vendaService.listar();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).itens()).hasSize(1);
        assertThat(resultado.get(0).valorTotal()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("buscarPorId retorna a venda com seus itens")
    void buscarPorIdSucesso() {
        Produto tabua = produto(1L, "Tabua", "10", "100.00");
        when(vendaRepository.findById(5L)).thenReturn(Optional.of(vendaComItem(5L, tabua)));

        VendaResponseDTO resultado = vendaService.buscarPorId(5L);

        assertThat(resultado.id()).isEqualTo(5L);
        assertThat(resultado.itens().get(0).nomeProduto()).isEqualTo("Tabua");
    }

    @Test
    @DisplayName("buscarPorId lanca excecao quando a venda nao existe")
    void buscarPorIdNaoEncontrado() {
        when(vendaRepository.findById(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendaService.buscarPorId(8L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Venda nao encontrada: id 8");
    }

    @Test
    @DisplayName("criar debita o estoque, congela o preco e soma o valor total")
    void criarSucesso() {
        Produto tabua = produto(1L, "Tabua", "10", "100.00");
        Produto viga = produto(2L, "Viga", "5.5", "45.90");
        when(clienteRepository.existsById(1L)).thenReturn(true);
        when(produtoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tabua));
        when(produtoRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(viga));
        when(vendaRepository.save(any(Venda.class))).thenAnswer(inv -> inv.getArgument(0));

        VendaRequestDTO dto = new VendaRequestDTO(1L, List.of(
                new ItemVendaRequestDTO(1L, new BigDecimal("3")),
                new ItemVendaRequestDTO(2L, new BigDecimal("2.5"))));

        VendaResponseDTO resultado = vendaService.criar(dto);

        // 3 * 100.00 + 2.5 * 45.90 = 300.00 + 114.75
        assertThat(resultado.valorTotal()).isEqualByComparingTo("414.75");
        assertThat(resultado.clienteId()).isEqualTo(1L);
        assertThat(resultado.dataVenda()).isNotNull();
        assertThat(resultado.itens()).hasSize(2);
        assertThat(resultado.itens().get(1).subtotal()).isEqualByComparingTo("114.75");
        assertThat(resultado.itens().get(0).precoUnitarioNoMomento()).isEqualByComparingTo("100.00");
        assertThat(tabua.getQuantidadeEstoque()).isEqualByComparingTo("7");
        assertThat(viga.getQuantidadeEstoque()).isEqualByComparingTo("3.0");
        verify(vendaRepository).save(any(Venda.class));
    }

    @Test
    @DisplayName("criar com cliente inexistente lanca EntityNotFoundException e nao mexe em estoque")
    void criarClienteInexistente() {
        when(clienteRepository.existsById(99L)).thenReturn(false);
        VendaRequestDTO dto = new VendaRequestDTO(99L, List.of(new ItemVendaRequestDTO(1L, BigDecimal.ONE)));

        assertThatThrownBy(() -> vendaService.criar(dto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Cliente nao encontrado: id 99");

        verify(produtoRepository, never()).findByIdForUpdate(any());
        verify(vendaRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar com produto inexistente lanca EntityNotFoundException")
    void criarProdutoInexistente() {
        when(clienteRepository.existsById(1L)).thenReturn(true);
        when(produtoRepository.findByIdForUpdate(50L)).thenReturn(Optional.empty());
        VendaRequestDTO dto = new VendaRequestDTO(1L, List.of(new ItemVendaRequestDTO(50L, BigDecimal.ONE)));

        assertThatThrownBy(() -> vendaService.criar(dto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Produto nao encontrado: id 50");

        verify(vendaRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar com estoque insuficiente lanca EstoqueInsuficienteException e nao salva a venda")
    void criarEstoqueInsuficiente() {
        Produto tabua = produto(1L, "Tabua", "2", "100.00");
        when(clienteRepository.existsById(1L)).thenReturn(true);
        when(produtoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tabua));
        VendaRequestDTO dto = new VendaRequestDTO(1L, List.of(new ItemVendaRequestDTO(1L, new BigDecimal("2.001"))));

        assertThatThrownBy(() -> vendaService.criar(dto))
                .isInstanceOf(EstoqueInsuficienteException.class)
                .hasMessageContaining("Estoque insuficiente para o produto 'Tabua'")
                .hasMessageContaining("solicitado 2.001");

        assertThat(tabua.getQuantidadeEstoque()).isEqualByComparingTo("2");
        verify(vendaRepository, never()).save(any());
    }

    @Test
    @DisplayName("quantidade exatamente igual ao estoque e permitida e zera o saldo")
    void criarQuantidadeIgualAoEstoque() {
        Produto tabua = produto(1L, "Tabua", "4", "10.00");
        when(clienteRepository.existsById(1L)).thenReturn(true);
        when(produtoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tabua));
        when(vendaRepository.save(any(Venda.class))).thenAnswer(inv -> inv.getArgument(0));

        vendaService.criar(new VendaRequestDTO(1L, List.of(new ItemVendaRequestDTO(1L, new BigDecimal("4")))));

        assertThat(tabua.getQuantidadeEstoque()).isEqualByComparingTo("0");
    }
}
