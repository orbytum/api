package com.orbytum.api.fachada;

import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.exceptions.UsuarioNaoEncontradoErro;
import com.orbytum.api.service.GrupoService;
import com.orbytum.api.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GrupoFachada {

    private final GrupoService grupoService;
    private final UsuarioService usuarioService;

}
