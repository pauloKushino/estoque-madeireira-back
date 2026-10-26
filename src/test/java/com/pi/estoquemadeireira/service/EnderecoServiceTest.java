package com.pi.estoquemadeireira.service;

import com.pi.estoquemadeireira.client.ViaCepClient;
import com.pi.estoquemadeireira.client.ViaCepResponse;
import com.pi.estoquemadeireira.dto.EnderecoResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnderecoServiceTest {

    @Mock
    private ViaCepClient viaCepClient;

    @InjectMocks
    private EnderecoService enderecoService;

    private static ViaCepResponse enderecoSe() {
        return new ViaCepResponse("01001-000", "Praca da Se", "Se", "Sao Paulo", "SP", null);
    }

    @Test
    @DisplayName("consulta CEP sem mascara e mapeia a resposta do ViaCEP")
    void consultarCepSemMascara() {
        when(viaCepClient.consultarCep("01001000")).thenReturn(enderecoSe());

        EnderecoResponseDTO resultado = enderecoService.consultarCep("01001000");

        assertThat(resultado.cep()).isEqualTo("01001-000");
        assertThat(resultado.logradouro()).isEqualTo("Praca da Se");
        assertThat(resultado.bairro()).isEqualTo("Se");
        assertThat(resultado.cidade()).isEqualTo("Sao Paulo");
        assertThat(resultado.uf()).isEqualTo("SP");
    }

    @Test
    @DisplayName("CEP com mascara e limpo antes de chamar o ViaCEP")
    void consultarCepComMascara() {
        when(viaCepClient.consultarCep("01001000")).thenReturn(enderecoSe());

        enderecoService.consultarCep("01001-000");

        verify(viaCepClient).consultarCep("01001000");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "abcdefgh", "0100100", "010010001", ""})
    @DisplayName("CEP fora do formato de 8 digitos lanca IllegalArgumentException sem chamar a API")
    void consultarCepInvalido(String cep) {
        assertThatThrownBy(() -> enderecoService.consultarCep(cep))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CEP invalido");

        verify(viaCepClient, never()).consultarCep(anyString());
    }

    @Test
    @DisplayName("CEP marcado com erro pelo ViaCEP vira EntityNotFoundException")
    void consultarCepInexistente() {
        when(viaCepClient.consultarCep("99999999"))
                .thenReturn(new ViaCepResponse(null, null, null, null, null, true));

        assertThatThrownBy(() -> enderecoService.consultarCep("99999-999"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99999999");
    }

    @Test
    @DisplayName("resposta nula do ViaCEP tambem vira EntityNotFoundException")
    void consultarCepRespostaNula() {
        when(viaCepClient.consultarCep("12345678")).thenReturn(null);

        assertThatThrownBy(() -> enderecoService.consultarCep("12345678"))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
