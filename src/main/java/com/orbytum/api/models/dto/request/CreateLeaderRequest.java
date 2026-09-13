package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.Email;                                                                                                                                                                 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;                                                                                                                                                              

public record CreateLeaderRequest(
    @NotBlank(message = "O nome é obrigatório")
    String nome,

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "Formato de e-mail inválido")
    String email,

    @NotBlank(message = "O telefone é obrigatório")
    String telefone,

    @NotBlank(message = "O título é obrigatório")
    String titulo,

    @NotBlank(message = "A senha é obrigatória")
    String senha
) {}
