package com.pi.estoquemadeireira.dto;

// Resposta do login. Mesmo contrato do repositorio da aula; expiraEm = validade do token em segundos.
public record LoginResponseDTO(
        String token,
        long expiraEm,
        String username,
        String nome,
        String role
) {
}
