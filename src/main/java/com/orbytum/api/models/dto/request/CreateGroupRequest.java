package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateGroupRequest (
    @NotBlank(message = "O nome do grupo é obrigatório") 
    String nome,

    @NotBlank(message = "O e-mail do líder é obrigatório")
    @Email(message = "E-mail do líder em formato inválido")
    String emailLider
){} 
