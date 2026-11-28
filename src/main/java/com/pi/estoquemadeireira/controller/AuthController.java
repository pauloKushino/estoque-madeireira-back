package com.pi.estoquemadeireira.controller;

import com.pi.estoquemadeireira.dto.LoginRequestDTO;
import com.pi.estoquemadeireira.dto.LoginResponseDTO;
import com.pi.estoquemadeireira.dto.UsuarioLogadoResponseDTO;
import com.pi.estoquemadeireira.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticacao", description = "Login com JWT e dados do usuario logado")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Publico: recebe usuario e senha e devolve o token JWT
    @PostMapping("/login")
    @Operation(summary = "Faz login e devolve o token JWT")
    @ApiResponse(responseCode = "200", description = "Login realizado")
    @ApiResponse(responseCode = "400", description = "username ou senha em branco")
    @ApiResponse(responseCode = "401", description = "Usuario ou senha invalidos")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO dto) {
        return authService.login(dto);
    }

    // Exige token: o username vem do "sub" do JWT ja validado pelo Spring Security
    @GetMapping("/me")
    @Operation(summary = "Mostra o usuario dono do token (header Authorization: Bearer ...)")
    @ApiResponse(responseCode = "200", description = "Usuario logado")
    @ApiResponse(responseCode = "401", description = "Sem token, token invalido ou expirado")
    public UsuarioLogadoResponseDTO me(Authentication authentication) {
        return authService.me(authentication.getName());
    }
}
