package com.pi.estoquemadeireira.service;

import com.pi.estoquemadeireira.dto.LoginRequestDTO;
import com.pi.estoquemadeireira.dto.LoginResponseDTO;
import com.pi.estoquemadeireira.dto.UsuarioLogadoResponseDTO;
import com.pi.estoquemadeireira.entity.Role;
import com.pi.estoquemadeireira.entity.Usuario;
import com.pi.estoquemadeireira.repository.UsuarioRepository;
import com.pi.estoquemadeireira.security.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    private final Usuario admin = new Usuario("admin", "Administrador", "hash-bcrypt", Role.ADMIN);

    @Test
    @DisplayName("Login com senha certa devolve token e dados do usuario")
    void loginValido() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("admin123", "hash-bcrypt")).thenReturn(true);
        when(tokenService.gerarToken(admin)).thenReturn("token-gerado");
        when(tokenService.getExpiracaoSegundos()).thenReturn(7200L);

        LoginResponseDTO resposta = authService.login(new LoginRequestDTO("admin", "admin123"));

        assertThat(resposta).isEqualTo(
                new LoginResponseDTO("token-gerado", 7200L, "admin", "Administrador", "ADMIN"));
    }

    @Test
    @DisplayName("Username com espacos e maiusculas e normalizado antes da busca")
    void loginNormalizaUsername() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("admin123", "hash-bcrypt")).thenReturn(true);

        authService.login(new LoginRequestDTO("  ADMIN ", "admin123"));

        verify(usuarioRepository).findByUsername("admin");
    }

    @Test
    @DisplayName("Senha errada gera BadCredentialsException e nao gera token")
    void loginSenhaErrada() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("errada", "hash-bcrypt")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("admin", "errada")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage(AuthService.CREDENCIAIS_INVALIDAS);
        verify(tokenService, never()).gerarToken(any());
    }

    @Test
    @DisplayName("Usuario inexistente gera a mesma mensagem da senha errada")
    void loginUsuarioInexistente() {
        when(usuarioRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("fantasma", "qualquer")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage(AuthService.CREDENCIAIS_INVALIDAS);
    }

    @Test
    @DisplayName("me devolve username, nome e role, sem a senha")
    void meDevolveUsuarioLogado() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThat(authService.me("admin"))
                .isEqualTo(new UsuarioLogadoResponseDTO("admin", "Administrador", "ADMIN"));
    }

    @Test
    @DisplayName("me com usuario que nao existe mais gera BadCredentialsException")
    void meUsuarioRemovido() {
        when(usuarioRepository.findByUsername("removido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me("removido")).isInstanceOf(BadCredentialsException.class);
    }
}
