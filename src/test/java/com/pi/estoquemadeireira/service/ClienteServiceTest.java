package com.pi.estoquemadeireira.service;

import com.pi.estoquemadeireira.dto.ClienteRequestDTO;
import com.pi.estoquemadeireira.dto.ClienteResponseDTO;
import com.pi.estoquemadeireira.entity.Cliente;
import com.pi.estoquemadeireira.repository.ClienteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    private static Cliente cliente(Long id, String nome) {
        Cliente c = new Cliente(nome, "529.982.247-25", "11987654321", "a@b.com",
                "01001-000", "Praca da Se", "Se", "Sao Paulo", "SP");
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private static ClienteRequestDTO request(String nome) {
        return new ClienteRequestDTO(nome, "529.982.247-25", "11987654321", "a@b.com",
                "01001-000", "Praca da Se", "Se", "Sao Paulo", "SP");
    }

    @Test
    @DisplayName("listar sem filtro (null) busca todos os clientes")
    void listarSemFiltro() {
        when(clienteRepository.findAll()).thenReturn(List.of(cliente(1L, "Ana"), cliente(2L, "Bia")));

        List<ClienteResponseDTO> resultado = clienteService.listar(null);

        assertThat(resultado).extracting(ClienteResponseDTO::nome).containsExactly("Ana", "Bia");
        verify(clienteRepository, never()).findByNomeContainingIgnoreCase(any());
    }

    @Test
    @DisplayName("listar com filtro em branco tambem busca todos")
    void listarComFiltroEmBranco() {
        when(clienteRepository.findAll()).thenReturn(List.of());

        assertThat(clienteService.listar("   ")).isEmpty();

        verify(clienteRepository).findAll();
        verify(clienteRepository, never()).findByNomeContainingIgnoreCase(any());
    }

    @Test
    @DisplayName("listar com nome filtra pelo repositorio")
    void listarComFiltro() {
        when(clienteRepository.findByNomeContainingIgnoreCase("an")).thenReturn(List.of(cliente(1L, "Ana")));

        List<ClienteResponseDTO> resultado = clienteService.listar("an");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo(1L);
        verify(clienteRepository, never()).findAll();
    }

    @Test
    @DisplayName("buscarPorId retorna o cliente existente")
    void buscarPorIdSucesso() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente(1L, "Ana")));

        ClienteResponseDTO resultado = clienteService.buscarPorId(1L);

        assertThat(resultado.nome()).isEqualTo("Ana");
        assertThat(resultado.cpfCnpj()).isEqualTo("529.982.247-25");
    }

    @Test
    @DisplayName("buscarPorId lanca EntityNotFoundException quando nao existe")
    void buscarPorIdNaoEncontrado() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.buscarPorId(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Cliente nao encontrado: id 99");
    }

    @Test
    @DisplayName("criar persiste o cliente e devolve o DTO com id gerado")
    void criarSucesso() {
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 10L);
            return c;
        });

        ClienteResponseDTO resultado = clienteService.criar(request("Carlos"));

        assertThat(resultado.id()).isEqualTo(10L);
        assertThat(resultado.nome()).isEqualTo("Carlos");
        assertThat(resultado.uf()).isEqualTo("SP");
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    @DisplayName("atualizar altera os campos e salva")
    void atualizarSucesso() {
        Cliente existente = cliente(1L, "Ana");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.save(existente)).thenReturn(existente);

        ClienteResponseDTO resultado = clienteService.atualizar(1L, request("Ana Maria"));

        assertThat(resultado.nome()).isEqualTo("Ana Maria");
        assertThat(existente.getNome()).isEqualTo("Ana Maria");
        verify(clienteRepository).save(existente);
    }

    @Test
    @DisplayName("atualizar de cliente inexistente lanca excecao e nao salva")
    void atualizarNaoEncontrado() {
        when(clienteRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.atualizar(5L, request("X")))
                .isInstanceOf(EntityNotFoundException.class);

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("excluir remove o cliente existente")
    void excluirSucesso() {
        Cliente existente = cliente(1L, "Ana");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));

        clienteService.excluir(1L);

        verify(clienteRepository).delete(existente);
    }

    @Test
    @DisplayName("excluir de cliente inexistente lanca excecao e nao remove")
    void excluirNaoEncontrado() {
        when(clienteRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.excluir(7L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("id 7");

        verify(clienteRepository, never()).delete(any());
    }
}
