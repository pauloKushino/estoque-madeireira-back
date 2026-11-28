package com.pi.estoquemadeireira.security;

import com.pi.estoquemadeireira.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

// Gera o JWT assinado (HS256) depois que o login da certo.
@Service
public class TokenService {

    static final String EMISSOR = "estoque-madeireira-api";

    private final JwtEncoder jwtEncoder;
    private final long expiracaoMinutos;

    public TokenService(JwtEncoder jwtEncoder,
                        @Value("${app.jwt.expiracao-minutos}") long expiracaoMinutos) {
        this.jwtEncoder = jwtEncoder;
        this.expiracaoMinutos = expiracaoMinutos;
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .subject(usuario.getUsername())                         // "sub": quem e o dono do token
                .issuedAt(agora)
                .expiresAt(agora.plus(expiracaoMinutos, ChronoUnit.MINUTES))
                .claim("nome", usuario.getNome())
                .claim("roles", List.of(usuario.getRole().name()))     // lida pelo JwtAuthenticationConverter
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    // Quanto tempo o token vale, em segundos (vai no campo "expiraEm" da resposta do login)
    public long getExpiracaoSegundos() {
        return expiracaoMinutos * 60;
    }
}
