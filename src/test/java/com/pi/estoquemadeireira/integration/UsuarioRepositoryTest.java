package com.pi.estoquemadeireira.integration;

import com.pi.estoquemadeireira.entity.Role;
import com.pi.estoquemadeireira.entity.Usuario;
import com.pi.estoquemadeireira.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

// Confere os usuarios criados pela migration V5 (admin e user) usando o H2 de teste.
@SpringBootTest
@ActiveProfiles("test")
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("Usuario admin existe, e ADMIN e a senha admin123 confere com o hash")
    void deveEncontrarAdmin() {
        Usuario admin = usuarioRepository.findByUsername("admin").orElseThrow();

        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(encoder.matches("admin123", admin.getSenha())).isTrue();
        assertThat(encoder.matches("senhaErrada", admin.getSenha())).isFalse();
    }

    @Test
    @DisplayName("Usuario user existe, e USER e a senha user123 confere com o hash")
    void deveEncontrarUser() {
        Usuario user = usuarioRepository.findByUsername("user").orElseThrow();

        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(encoder.matches("user123", user.getSenha())).isTrue();
    }

    @Test
    @DisplayName("Username que nao existe volta vazio")
    void naoDeveEncontrarUsuarioInexistente() {
        assertThat(usuarioRepository.findByUsername("fantasma")).isEmpty();
    }
}
