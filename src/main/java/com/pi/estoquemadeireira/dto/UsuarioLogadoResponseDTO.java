package com.pi.estoquemadeireira.dto;

// Resposta do GET /auth/me. Nunca devolve a senha (nem o hash).
public record UsuarioLogadoResponseDTO(
        String username,
        String nome,
        String role
) {
}
