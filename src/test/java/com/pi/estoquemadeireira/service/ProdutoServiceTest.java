package com.pi.estoquemadeireira.service;

import com.pi.estoquemadeireira.dto.EstoqueResponseDTO;
import com.pi.estoquemadeireira.dto.ProdutoRequestDTO;
import com.pi.estoquemadeireira.dto.ProdutoResponseDTO;
import com.pi.estoquemadeireira.entity.Produto;
import com.pi.estoquemadeireira.entity.UnidadeMedida;
import com.pi.estoquemadeireira.repository.ProdutoRepository;
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
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private ProdutoService produtoService;

    private static Produto produto(Long id, String nome, String estoque) {
        Produto p = new Produto(nome, UnidadeMedida.M3, new BigDecimal(estoque),
                new BigDecimal("850.00"), new BigDecimal("10"));
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    private static ProdutoRequestDTO request(String nome) {
        return new ProdutoRequestDTO(nome, UnidadeMedida.UNIDADE, new BigDecimal("20"),
                new BigDecimal("12.50"), new BigDecimal("5"));
    }

    @Test
    @DisplayName("listar sem filtro busca todos os produtos")
    void listarSemFiltro() {
        when(produtoRepository.findAll()).thenReturn(List.of(produto(1L, "Tabua", "10"), produto(2L, "Viga", "5")));

        List<ProdutoResponseDTO> resultado = produtoService.listar(null);

        assertThat(resultado).extracting(ProdutoResponseDTO::nome).containsExactly("Tabua", "Viga");
        verify(produtoRepository, never()).findByNomeContainingIgnoreCase(any());
    }

    @Test
    @DisplayName("listar com filtro em branco busca todos")
    void listarComFiltroEmBranco() {
        when(produtoRepository.findAll()).thenReturn(List.of());

        assertThat(produtoService.listar("")).isEmpty();

        verify(produtoRepository).findAll();
    }

    @Test
    @DisplayName("listar com nome usa a busca por nome")
    void listarComFiltro() {
        when(produtoRepository.findByNomeContainingIgnoreCase("tab")).thenReturn(List.of(produto(1L, "Tabua", "10")));

        List<ProdutoResponseDTO> resultado = produtoService.listar("tab");

        assertThat(resultado).hasSize(1);
        verify(produtoRepository, never()).findAll();
    }

    @Test
    @DisplayName("buscarPorId retorna o produto")
    void buscarPorIdSucesso() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto(1L, "Tabua", "10")));

        ProdutoResponseDTO resultado = produtoService.buscarPorId(1L);

        assertThat(resultado.id()).isEqualTo(1L);
        assertThat(resultado.unidadeMedida()).isEqualTo(UnidadeMedida.M3);
    }

    @Test
    @DisplayName("buscarPorId lanca excecao quando o produto nao existe")
    void buscarPorIdNaoEncontrado() {
        when(produtoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.buscarPorId(9L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Produto nao encontrado: id 9");
    }

    @Test
    @DisplayName("consultarEstoque devolve a quantidade disponivel")
    void consultarEstoqueSucesso() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto(1L, "Tabua", "45.5")));

        EstoqueResponseDTO resultado = produtoService.consultarEstoque(1L);

        assertThat(resultado.produtoId()).isEqualTo(1L);
        assertThat(resultado.nomeProduto()).isEqualTo("Tabua");
        assertThat(resultado.quantidadeDisponivel()).isEqualByComparingTo("45.5");
        assertThat(resultado.unidadeMedida()).isEqualTo(UnidadeMedida.M3);
    }

    @Test
    @DisplayName("consultarEstoque lanca excecao quando o produto nao existe")
    void consultarEstoqueNaoEncontrado() {
        when(produtoRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.consultarEstoque(3L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("criar persiste e devolve o produto com id")
    void criarSucesso() {
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> {
            Produto p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 4L);
            return p;
        });

        ProdutoResponseDTO resultado = produtoService.criar(request("Ripao"));

        assertThat(resultado.id()).isEqualTo(4L);
        assertThat(resultado.nome()).isEqualTo("Ripao");
        assertThat(resultado.quantidadeEstoque()).isEqualByComparingTo("20");
        verify(produtoRepository).save(any(Produto.class));
    }

    @Test
    @DisplayName("atualizar altera os campos do produto existente")
    void atualizarSucesso() {
        Produto existente = produto(1L, "Tabua", "10");
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(produtoRepository.save(existente)).thenReturn(existente);

        ProdutoResponseDTO resultado = produtoService.atualizar(1L, request("Tabua Nova"));

        assertThat(resultado.nome()).isEqualTo("Tabua Nova");
        assertThat(resultado.unidadeMedida()).isEqualTo(UnidadeMedida.UNIDADE);
        assertThat(existente.getPrecoUnitario()).isEqualByComparingTo("12.50");
    }

    @Test
    @DisplayName("atualizar produto inexistente lanca excecao e nao salva")
    void atualizarNaoEncontrado() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.atualizar(1L, request("X")))
                .isInstanceOf(EntityNotFoundException.class);

        verify(produtoRepository, never()).save(any());
    }

    @Test
    @DisplayName("excluir remove o produto existente")
    void excluirSucesso() {
        Produto existente = produto(1L, "Tabua", "10");
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(existente));

        produtoService.excluir(1L);

        verify(produtoRepository).delete(existente);
    }

    @Test
    @DisplayName("excluir produto inexistente lanca excecao e nao remove")
    void excluirNaoEncontrado() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.excluir(1L))
                .isInstanceOf(EntityNotFoundException.class);

        verify(produtoRepository, never()).delete(any());
    }
}
