package com.pi.estoquemadeireira.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CpfCnpjValidatorTest {

    private final CpfCnpjValidator validator = new CpfCnpjValidator();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("valor nulo, vazio ou em branco e considerado valido")
    void valoresVaziosSaoValidos(String valor) {
        assertThat(validator.isValid(valor, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-25", "52998224725", "111.444.777-35"})
    @DisplayName("CPFs validos, com ou sem mascara")
    void cpfValido(String cpf) {
        assertThat(validator.isValid(cpf, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-24", "52998224700", "123.456.789-00"})
    @DisplayName("CPFs com digito verificador errado")
    void cpfComDigitoErrado(String cpf) {
        assertThat(validator.isValid(cpf, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11.222.333/0001-81", "11222333000181", "07.526.557/0001-00"})
    @DisplayName("CNPJs validos, com ou sem mascara")
    void cnpjValido(String cnpj) {
        assertThat(validator.isValid(cnpj, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"11.222.333/0001-80", "11222333000182", "12.345.678/0001-00"})
    @DisplayName("CNPJs com digito verificador errado")
    void cnpjComDigitoErrado(String cnpj) {
        assertThat(validator.isValid(cnpj, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"111.111.111-11", "00000000000", "11.111.111/1111-11", "99999999999999"})
    @DisplayName("sequencias de digitos repetidos sao rejeitadas")
    void digitosRepetidos(String valor) {
        assertThat(validator.isValid(valor, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "1234567890", "123456789012", "123456789012345", "abc"})
    @DisplayName("quantidade de digitos diferente de 11 ou 14 e invalida")
    void tamanhoInvalido(String valor) {
        assertThat(validator.isValid(valor, null)).isFalse();
    }

    @Test
    @DisplayName("digito verificador resultando em resto menor que 2 vira zero")
    void restoMenorQueDois() {
        // 11.444.777/0001-61 e 000.000.001-91 exercitam o ramo soma % 11 < 2
        assertThat(validator.isValid("11.444.777/0001-61", null)).isTrue();
        assertThat(validator.isValid("000.000.001-91", null)).isTrue();
    }
}
