package com.pi.estoquemadeireira.service;

import com.pi.estoquemadeireira.dto.LoginRequestDTO;
import com.pi.estoquemadeireira.dto.LoginResponseDTO;
import com.pi.estoquemadeireira.dto.UsuarioLogadoResponseDTO;
import com.pi.estoquemadeireira.entity.Usuario;
import com.pi.estoquemadeireira.repository.UsuarioRepository;
import com.pi.estoquemadeireira.security.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // Mesma mensagem para usuario inexistente e senha errada: nao revela quais usernames existem
    static final String CREDENCIAIS_INVALIDAS = "Usuario ou senha invalidos";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {
        Usuario usuario = usuarioRepository.findByUsername(normalizar(request.username()))
                .orElseThrow(() -> new BadCredentialsException(CREDENCIAIS_INVALIDAS));

        // Compara a senha digitada com o hash BCrypt salvo no banco
        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            log.warn("Tentativa de login com senha errada para o usuario {}", usuario.getUsername());
            throw new BadCredentialsException(CREDENCIAIS_INVALIDAS);
        }

        log.info("Login realizado: {} ({})", usuario.getUsername(), usuario.getRole());
        return new LoginResponseDTO(tokenService.gerarToken(usuario), tokenService.getExpiracaoSegundos(),
                usuario.getUsername(), usuario.getNome(), usuario.getRole().name());
    }

    // O username vem do "sub" do token ja validado
    @Transactional(readOnly = true)
    public UsuarioLogadoResponseDTO me(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Usuario do token nao existe mais"));
        return new UsuarioLogadoResponseDTO(usuario.getUsername(), usuario.getNome(), usuario.getRole().name());
    }

    private static String normalizar(String username) {
        return username.trim().toLowerCase();
    }
}
