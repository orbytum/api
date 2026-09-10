package com.orbytum.api.fachada;

import com.orbytum.api.models.dto.request.CreateProjetoRequest;
import com.orbytum.api.models.dto.request.EditProjetoRequest;
import com.orbytum.api.models.dto.response.ProjetoResponse;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.exceptions.GrupoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.ProjetoNaoEncontradoErro;
import com.orbytum.api.service.GrupoService;
import com.orbytum.api.service.ProjetoService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProjetoFachada {

    private final ProjetoService projetoService;
    private final GrupoService grupoService;

    @Transactional
    public ProjetoResponse criarProjeto(CreateProjetoRequest request) {
        Grupo grupo = grupoService.findById(request.grupoId())
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo de pesquisa não encontrado com ID: " + request.grupoId()));

        Projeto projeto = new Projeto(grupo, request.status(), request.titulo(), request.assunto());
        Projeto projetoSalvo = projetoService.save(projeto);

        return toResponse(projetoSalvo);
    }

    @Transactional
    public ProjetoResponse atualizarProjeto(Long id, EditProjetoRequest request) {
        Projeto projeto = projetoService.findById(id)
                .orElseThrow(() -> new ProjetoNaoEncontradoErro("Projeto não encontrado com ID: " + id));

        projeto.setStatus(request.status());
        projeto.setTitulo(request.titulo());
        projeto.setAssunto(request.assunto());
        if (request.isAtivo() != null) {
            projeto.setAtivo(request.isAtivo());
        }

        Projeto projetoAtualizado = projetoService.save(projeto);

        return toResponse(projetoAtualizado);
    }

    @Transactional
    public void deletarProjeto(Long id) {
        Projeto projeto = projetoService.findById(id)
                .orElseThrow(() -> new ProjetoNaoEncontradoErro("Projeto não encontrado com ID: " + id));

        projeto.setAtivo(false);
        projetoService.save(projeto);
    }

    public List<ProjetoResponse> listarProjetos() {
        return projetoService.findAllAtivos()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjetoResponse buscarProjeto(Long id) {
        Projeto projeto = projetoService.findById(id)
                .orElseThrow(() -> new ProjetoNaoEncontradoErro("Projeto não encontrado com ID: " + id));

        return toResponse(projeto);
    }

    private ProjetoResponse toResponse(Projeto projeto) {
        return new ProjetoResponse(
                projeto.getId(),
                projeto.getGrupo().getId(),
                projeto.getStatus(),
                projeto.getTitulo(),
                projeto.getAssunto(),
                projeto.getDthRegistro(),
                projeto.isAtivo());
    }
}
