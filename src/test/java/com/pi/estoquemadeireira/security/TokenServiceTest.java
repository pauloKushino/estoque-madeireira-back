package com.pi.estoquemadeireira.security;

import com.pi.estoquemadeireira.entity.Role;
import com.pi.estoquemadeireira.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Usa os beans reais do SecurityConfig (sem subir o Spring) para gerar e depois ler o token.
class TokenServiceTest {

    private final SecurityConfig config = new SecurityConfig();
    private final SecretKey chave = config.jwtSecretKey("segredo-de-teste-com-mais-de-32-caracteres");
    private final JwtDecoder decoder = config.jwtDecoder(chave);
    private final TokenService tokenService = new TokenService(config.jwtEncoder(chave), 120);

    private final Usuario admin = new Usuario("admin", "Administrador", "hash", Role.ADMIN);

    @Test
    @DisplayName("Token gerado carrega username, nome, role e emissor")
    void tokenTemAsClaims() {
        Jwt jwt = decoder.decode(tokenService.gerarToken(admin));

        assertThat(jwt.getSubject()).isEqualTo("admin");
        assertThat(jwt.getClaimAsString("nome")).isEqualTo("Administrador");
        assertThat(jwt.getClaimAsStringList("roles")).isEqualTo(List.of("ADMIN"));
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(TokenService.EMISSOR);
    }

    @Test
    @DisplayName("Token expira no tempo configurado (120 minutos)")
    void tokenExpiraNoTempoConfigurado() {
        Jwt jwt = decoder.decode(tokenService.gerarToken(admin));

        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(120));
        assertThat(tokenService.getExpiracaoSegundos()).isEqualTo(7200);
    }

    @Test
    @DisplayName("Token assinado com outra chave e recusado")
    void tokenDeOutraChaveERecusado() {
        SecretKey outraChave = config.jwtSecretKey("outra-chave-qualquer-com-mais-de-32-caracteres");
        String tokenFalso = new TokenService(config.jwtEncoder(outraChave), 120).gerarToken(admin);

        assertThatThrownBy(() -> decoder.decode(tokenFalso)).isInstanceOf(JwtException.class);
    }
}
