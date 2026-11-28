package com.pi.estoquemadeireira.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    @DisplayName("Segredo com menos de 32 caracteres impede a aplicacao de subir")
    void segredoCurtoDeveFalhar() {
        assertThatThrownBy(() -> config.jwtSecretKey("curto-demais"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 caracteres");
    }

    @Test
    @DisplayName("Segredo com 32 caracteres ou mais gera uma chave HmacSHA256")
    void segredoValidoGeraChave() {
        SecretKey chave = config.jwtSecretKey("a".repeat(32));

        assertThat(chave.getAlgorithm()).isEqualTo("HmacSHA256");
    }

    @Test
    @DisplayName("PasswordEncoder reconhece a senha certa e recusa a errada")
    void passwordEncoderUsaBcrypt() {
        var encoder = config.passwordEncoder();
        String hash = encoder.encode("admin123");

        assertThat(encoder.matches("admin123", hash)).isTrue();
        assertThat(encoder.matches("outra", hash)).isFalse();
    }
}
