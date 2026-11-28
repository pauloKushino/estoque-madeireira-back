package com.pi.estoquemadeireira.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

// Configuracao do Spring Security com JWT (PIC-14), no modelo do repositorio da aula.
@Configuration
public class SecurityConfig {

    // Tamanho minimo da chave para o HS256 (256 bits = 32 bytes)
    static final int TAMANHO_MINIMO_SEGREDO = 32;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        http
                // API REST stateless com token no header -> CSRF nao se aplica
                .csrf(AbstractHttpConfigurer::disable)
                // Usa a configuracao de CORS existente (CorsConfig). TODO PIC-16: CORS no SecurityConfig
                .cors(Customizer.withDefaults())
                // Sem sessao no servidor: cada requisicao se identifica pelo proprio token
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ---- publicas (PIC-14; demais rotas publicas no PIC-16) ----
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // preflight do navegador

                        // ---- exigem token (PIC-14) ----
                        .requestMatchers("/auth/me").authenticated()

                        // ---- por perfil ADMIN/USER: PIC-15 ----
                        // TODO PIC-15: trocar por authenticated() + regras por perfil
                        // (junto com o PIC-18, que adapta os testes de integracao existentes)
                        .anyRequest().permitAll()
                )
                // Valida o "Authorization: Bearer <token>" em toda requisicao que o enviar
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }

    // Chave secreta usada para assinar e validar o token (vem de JWT_SECRET)
    @Bean
    public SecretKey jwtSecretKey(@Value("${app.jwt.secret}") String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException(
                    "app.jwt.secret (JWT_SECRET) precisa ter pelo menos 32 caracteres.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    // Gera (assina) o token no login
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    // Confere a assinatura e a validade do token que chega no header Authorization
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    // Transforma a claim "roles" do token em ROLE_ADMIN / ROLE_USER
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    // Senhas sempre comparadas com o hash BCrypt guardado no banco (migration V5)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
