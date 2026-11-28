package com.pi.estoquemadeireira.dto;

import jakarta.validation.constraints.NotBlank;

// Corpo do POST /auth/login
public record LoginRequestDTO(

        @NotBlank(message = "username e obrigatorio")
        String username,

        @NotBlank(message = "senha e obrigatoria")
        String senha
) {
}
