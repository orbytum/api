package com.orbytum.api.models.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateGroupRequest (
    @NotBlank(message = "O nome do grupo é obrigatório") 
    String nome
){}
