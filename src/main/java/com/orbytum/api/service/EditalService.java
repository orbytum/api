package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.CriarEditalRequest;
import com.orbytum.api.models.dto.request.EditarEditalRequest;
import com.orbytum.api.models.dto.response.EditalPaginadoResponse;
import com.orbytum.api.models.dto.response.EditalResponse;
import com.orbytum.api.models.entity.Edital;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.EditalXGrupo;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.exceptions.EditalJaVinculadoErro;
import com.orbytum.api.models.exceptions.EditalNaoEncontradoErro;
import com.orbytum.api.models.exceptions.GrupoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.UsuarioNaoEncontradoErro;
import com.orbytum.api.repository.EditalRepository;
import com.orbytum.api.repository.EditalXGrupoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class EditalService {

    private final EditalRepository editalRepository;
    private final EditalXGrupoRepository editalXGrupoRepository;
    private final GrupoService grupoService;
    private final UsuarioService usuarioService;
    private final GrupoAcessoService grupoAcessoService;

    @Transactional
    public EditalResponse criar(Long grupoId, CriarEditalRequest request, String emailLogado) {
        Grupo grupo = buscarGrupo(grupoId);
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarPodeGerenciar(grupo.getId(), solicitante);
        validarPeriodo(request.dthAbertura(), request.dthEncerramento());

        String uri = request.uri().trim();
        Edital edital = editalRepository.findByUri(uri)
                .orElseGet(() -> editalRepository.save(Edital.builder()
                        .organizador(request.organizador().trim())
                        .descricao(request.descricao().trim())
                        .valor(request.valor())
                        .uri(uri)
                        .dthAbertura(request.dthAbertura())
                        .dthEncerramento(request.dthEncerramento())
                        .build()));

        if (editalXGrupoRepository.existsByEditalIdAndGrupoId(edital.getId(), grupo.getId())) {
            throw new EditalJaVinculadoErro("Este edital já está vinculado ao grupo.");
        }

        editalXGrupoRepository.save(new EditalXGrupo(edital, grupo));
        return EditalResponse.fromEntity(edital);
    }

    @Transactional
    public EditalResponse atualizar(Long editalId, Long grupoId, EditarEditalRequest request, String emailLogado) {
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarPodeGerenciar(grupoId, solicitante);
        Edital edital = buscarEditalDoGrupo(editalId, grupoId);
        validarPeriodo(request.dthAbertura(), request.dthEncerramento());

        edital.setOrganizador(request.organizador().trim());
        edital.setDescricao(request.descricao().trim());
        edital.setValor(request.valor());
        edital.setUri(request.uri().trim());
        edital.setDthAbertura(request.dthAbertura());
        edital.setDthEncerramento(request.dthEncerramento());

        return EditalResponse.fromEntity(editalRepository.save(edital));
    }

    @Transactional(readOnly = true)
    public EditalResponse buscar(Long editalId, Long grupoId, String emailLogado) {
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarMembro(grupoId, solicitante);
        return EditalResponse.fromEntity(buscarEditalDoGrupo(editalId, grupoId));
    }

    @Transactional(readOnly = true)
    public EditalPaginadoResponse listar(
            Long grupoId,
            String palavraChave,
            String organizacao,
            LocalDate dataInicio,
            LocalDate dataFim,
            int page,
            int size,
            String emailLogado) {

        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarMembro(grupoId, solicitante);

        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        LocalDateTime inicio = dataInicio != null ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = dataFim != null ? dataFim.atTime(LocalTime.MAX) : null;

        Page<Edital> resultado = editalRepository.filtrarPorGrupo(
                grupoId, normalizar(palavraChave), normalizar(organizacao), inicio, fim, pageable);

        return new EditalPaginadoResponse(
                resultado.getContent().stream().map(EditalResponse::fromEntity).toList(),
                resultado.getTotalElements(),
                resultado.getTotalPages(),
                page,
                pageSize);
    }

    @Transactional
    public void removerDoGrupo(Long editalId, Long grupoId, String emailLogado) {
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarPodeGerenciar(grupoId, solicitante);
        Edital edital = buscarEditalDoGrupo(editalId, grupoId);

        EditalXGrupo vinculo = editalXGrupoRepository
                .findByEditalIdAndGrupoId(editalId, grupoId)
                .orElseThrow(() -> new EditalNaoEncontradoErro("Edital não vinculado a este grupo."));

        long totalVinculos = editalXGrupoRepository.countByEditalId(editalId);
        editalXGrupoRepository.delete(vinculo);
        if (totalVinculos <= 1) {
            editalRepository.delete(edital);
        }
    }

    private Edital buscarEditalDoGrupo(Long editalId, Long grupoId) {
        Edital edital = editalRepository.findById(editalId)
                .orElseThrow(() -> new EditalNaoEncontradoErro("Edital não encontrado com ID: " + editalId));
        if (!editalXGrupoRepository.existsByEditalIdAndGrupoId(editalId, grupoId)) {
            throw new EditalNaoEncontradoErro("Edital não encontrado para este grupo.");
        }
        return edital;
    }

    private void validarMembro(Long grupoId, Usuario solicitante) {
        if (isAdmin(solicitante)) {
            return;
        }
        grupoAcessoService.validarMembro(grupoId, solicitante.getId());
    }

    private void validarPodeGerenciar(Long grupoId, Usuario solicitante) {
        if (isAdmin(solicitante)) {
            return;
        }
        grupoAcessoService.validarCoordenadorOuAcima(grupoId, solicitante.getId());
    }

    private void validarPeriodo(LocalDateTime abertura, LocalDateTime encerramento) {
        if (abertura == null || encerramento == null || !encerramento.isAfter(abertura)) {
            throw new IllegalArgumentException("A data de encerramento deve ser posterior à data de abertura.");
        }
    }

    private Grupo buscarGrupo(Long grupoId) {
        if (grupoId == null) {
            throw new IllegalArgumentException("O ID do grupo é obrigatório.");
        }
        return grupoService.findById(grupoId)
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo de pesquisa não encontrado com ID: " + grupoId));
    }

    private Usuario usuarioAutenticado(String emailLogado) {
        return usuarioService.findByEmail(emailLogado)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário autenticado não encontrado"));
    }

    private boolean isAdmin(Usuario usuario) {
        return usuario.getCredenciaisLogin() != null
                && usuario.getCredenciaisLogin().getAccessLevel() == AccessLevel.ADMIN;
    }

    private String normalizar(String valor) {
        return (valor != null && !valor.isBlank()) ? valor.trim() : null;
    }
}
