package com.pi.estoquemadeireira.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

// Configuracao PROVISORIA: libera todas as rotas para a API e os testes
// continuarem funcionando enquanto o login JWT nao existe.
// TODO PIC-14: substituir pelas regras JWT/roles
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)                      // API REST sem sessao, nao precisa de CSRF
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()); // libera tudo por enquanto
        return http.build();
    }
}