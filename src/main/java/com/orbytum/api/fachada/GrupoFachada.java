package com.orbytum.api.fachada;

import com.orbytum.api.models.dto.request.CreateGroupRequest;
import com.orbytum.api.models.dto.response.GrupoResponse;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.service.GrupoService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GrupoFachada {
    private final GrupoService grupoService;

    @Transactional
    public GrupoResponse criarGrupo(CreateGroupRequest request) {
        if (grupoService.existsByNome(request.nome())) {
            throw new IllegalArgumentException("Já existe um grupo de pesquisa cadastrado com este nome");
        }

        Grupo novoGrupo = new Grupo(request.nome());
        Grupo grupoSalvo = grupoService.save(novoGrupo);

        return new GrupoResponse(grupoSalvo.getId(), grupoSalvo.getNome(), grupoSalvo.isAtivo());
    }

    public List<GrupoResponse> listarGrupos() {
        return grupoService.findAllAtivos()
                .stream()
                .map(grupo -> new GrupoResponse(
                        grupo.getId(),
                        grupo.getNome(),
                        grupo.isAtivo()))
                .toList();
    }
}
