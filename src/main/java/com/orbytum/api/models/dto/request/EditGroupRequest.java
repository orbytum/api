package com.orbytum.api.models.dto.request;
  
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EditGroupRequest (
    @NotBlank(message = "O nome do grupo é obrigatório")
    String nome,

    @NotNull(message = "O status de ativo é obrigatório")
    Boolean isAtivo
) {}
