package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.CreateAtividadeRequest;
import com.orbytum.api.models.dto.request.EditAtividadeRequest;
import com.orbytum.api.models.dto.response.AtividadePaginadoResponse;
import com.orbytum.api.models.dto.response.AtividadeResponse;
import com.orbytum.api.models.entity.Atividade;
import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.AtividadeStatus;
import com.orbytum.api.models.exceptions.AtividadeNaoEncontradaErro;
import com.orbytum.api.models.exceptions.AtividadeNaoPertenceAoProjetoErro;
import com.orbytum.api.models.exceptions.ProjetoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.ResponsavelNaoPertenceAoProjetoErro;
import com.orbytum.api.models.exceptions.TransicaoAtividadeInvalidaErro;
import com.orbytum.api.models.exceptions.UsuarioNaoEncontradoErro;
import com.orbytum.api.repository.AtividadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AtividadeService {

    private static final List<AtividadeStatus> STATUS_ABERTOS = List.of(
            AtividadeStatus.PENDENTE,
            AtividadeStatus.EM_ANDAMENTO,
            AtividadeStatus.AGUARDANDO_CONFIRMACAO);

    private final AtividadeRepository atividadeRepository;
    private final ProjetoService projetoService;
    private final UsuarioService usuarioService;
    private final GrupoAcessoService grupoAcessoService;

    @Transactional
    public AtividadeResponse criar(CreateAtividadeRequest request, String emailLogado) {
        Usuario solicitante = usuarioAutenticado(emailLogado);
        Projeto projeto = projetoService.findById(request.projetoId())
                .orElseThrow(() -> new ProjetoNaoEncontradoErro("Projeto não encontrado com ID: " + request.projetoId()));

        Long grupoId = projeto.getGrupo().getId();
        validarAcessoAoGrupo(grupoId, solicitante);

        Usuario responsavel = usuarioService.findById(request.responsavelId())
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário não encontrado com ID: " + request.responsavelId()));

        Atividade atividadePai = null;
        if (request.atividadePaiId() != null) {
            atividadePai = buscarEntidade(request.atividadePaiId());

            if (!atividadePai.getProjeto().getId().equals(projeto.getId())) {
                throw new AtividadeNaoPertenceAoProjetoErro("A atividade pai pertence a outro projeto");
            }
            if (atividadePai.getStatus() != AtividadeStatus.EM_ANDAMENTO) {
                throw new TransicaoAtividadeInvalidaErro("Impedimentos só podem ser registrados em atividades Em Andamento");
            }
            grupoAcessoService.validarMembro(grupoId, responsavel.getId());
        } else {
            if (!isAdmin(solicitante)) {
                grupoAcessoService.validarCoordenadorOuAcima(grupoId, solicitante.getId());
            }
            validarResponsavelNoProjeto(projeto.getId(), responsavel);
        }

        Atividade atividade = new Atividade(
                projeto,
                responsavel,
                atividadePai,
                request.titulo(),
                request.descricao(),
                request.dthPrazo());

        return toResponse(atividadeRepository.save(atividade));
    }

    @Transactional
    public AtividadeResponse atualizar(Long id, EditAtividadeRequest request, String emailLogado) {
        Atividade atividade = buscarEntidade(id);
        Usuario solicitante = usuarioAutenticado(emailLogado);
        Long grupoId = atividade.getProjeto().getGrupo().getId();

        validarAcessoAoGrupo(grupoId, solicitante);
        if (!isAdmin(solicitante)) {
            grupoAcessoService.validarCoordenadorOuAcima(grupoId, solicitante.getId());
        }

        atividade.setTitulo(request.titulo());
        atividade.setDescricao(request.descricao());

        if (request.dthPrazo() != null) {
            atividade.setDthPrazo(request.dthPrazo());
        }

        if (request.responsavelId() != null) {
            Usuario responsavel = usuarioService.findById(request.responsavelId())
                    .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário não encontrado com ID: " + request.responsavelId()));

            if (atividade.getAtividadePai() == null) {
                validarResponsavelNoProjeto(atividade.getProjeto().getId(), responsavel);
            } else {
                grupoAcessoService.validarMembro(grupoId, responsavel.getId());
            }
            atividade.setResponsavel(responsavel);
        }

        return toResponse(atividadeRepository.save(atividade));
    }

    @Transactional
    public AtividadeResponse atualizarStatus(Long id, AtividadeStatus novoStatus, String emailLogado) {
        Atividade atividade = buscarEntidade(id);
        Usuario solicitante = usuarioAutenticado(emailLogado);
        Long grupoId = atividade.getProjeto().getGrupo().getId();

        validarAcessoAoGrupo(grupoId, solicitante);

        if (!atividade.getStatus().podeTransicionarPara(novoStatus)) {
            throw new TransicaoAtividadeInvalidaErro(
                    "Transição de status inválida: " + atividade.getStatus() + " -> " + novoStatus);
        }

        if ((novoStatus == AtividadeStatus.CONCLUIDA || novoStatus == AtividadeStatus.ENCERRADA) && !isAdmin(solicitante)) {
            grupoAcessoService.validarCoordenadorOuAcima(grupoId, solicitante.getId());
        }

        atividade.setStatus(novoStatus);
        if (novoStatus == AtividadeStatus.CONCLUIDA) {
            atividade.setDthConclusao(LocalDateTime.now());
        }

        return toResponse(atividadeRepository.save(atividade));
    }

    @Transactional
    public void deletar(Long id, String emailLogado) {
        Atividade atividade = buscarEntidade(id);
        Usuario solicitante = usuarioAutenticado(emailLogado);
        Long grupoId = atividade.getProjeto().getGrupo().getId();

        validarAcessoAoGrupo(grupoId, solicitante);
        if (!isAdmin(solicitante)) {
            grupoAcessoService.validarCoordenadorOuAcima(grupoId, solicitante.getId());
        }

        atividade.setAtivo(false);
        atividadeRepository.save(atividade);
    }

    @Transactional(readOnly = true)
    public AtividadeResponse buscar(Long id, String emailLogado) {
        Atividade atividade = buscarEntidade(id);
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarAcessoAoGrupo(atividade.getProjeto().getGrupo().getId(), solicitante);
        return toResponse(atividade);
    }

    @Transactional(readOnly = true)
    public AtividadePaginadoResponse listarPorProjeto(Long projetoId, int page, int size, String emailLogado) {
        Projeto projeto = projetoService.findById(projetoId)
                .orElseThrow(() -> new ProjetoNaoEncontradoErro("Projeto não encontrado com ID: " + projetoId));
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarAcessoAoGrupo(projeto.getGrupo().getId(), solicitante);

        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.ASC, "id"));

        Page<Atividade> atividades = atividadeRepository.pageAtivasPorProjeto(projetoId, pageable);
        boolean projetoPodeSerFinalizado = !existeAtividadeAberta(projetoId);

        List<AtividadeResponse> items = atividades.getContent().stream()
                .map(atividade -> toResponse(atividade, projetoPodeSerFinalizado))
                .toList();

        return new AtividadePaginadoResponse(
                items,
                atividades.getTotalElements(),
                atividades.getTotalPages(),
                page,
                pageSize
        );
    }

    @Transactional(readOnly = true)
    public List<AtividadeResponse> listarAtrasadasPorProjeto(Long projetoId, String emailLogado) {
        Projeto projeto = projetoService.findById(projetoId)
                .orElseThrow(() -> new ProjetoNaoEncontradoErro("Projeto não encontrado com ID: " + projetoId));
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarAcessoAoGrupo(projeto.getGrupo().getId(), solicitante);

        List<Atividade> atrasadas = atividadeRepository.findAllAtrasadasPorProjeto(
                projetoId,
                STATUS_ABERTOS,
                LocalDateTime.now());
        boolean projetoPodeSerFinalizado = !existeAtividadeAberta(projetoId);

        return atrasadas.stream()
                .map(atividade -> toResponse(atividade, projetoPodeSerFinalizado))
                .toList();
    }

    private void validarResponsavelNoProjeto(Long projetoId, Usuario responsavel) {
        if (!grupoAcessoService.isMembroDoProjeto(projetoId, responsavel.getId())) {
            throw new ResponsavelNaoPertenceAoProjetoErro("O responsável informado não participa deste projeto");
        }
    }

    private Atividade buscarEntidade(Long id) {
        return atividadeRepository.findByIdComRelacionamentos(id)
                .orElseThrow(() -> new AtividadeNaoEncontradaErro("Atividade não encontrada com ID: " + id));
    }

    private Usuario usuarioAutenticado(String emailLogado) {
        return usuarioService.findByEmail(emailLogado)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário autenticado não encontrado"));
    }

    private boolean isAdmin(Usuario usuario) {
        return usuario.getCredenciaisLogin() != null
                && usuario.getCredenciaisLogin().getAccessLevel() == AccessLevel.ADMIN;
    }

    private void validarAcessoAoGrupo(Long grupoId, Usuario solicitante) {
        if (isAdmin(solicitante)) {
            return;
        }
        grupoAcessoService.validarMembro(grupoId, solicitante.getId());
    }

    private boolean existeAtividadeAberta(Long projetoId) {
        return atividadeRepository.countByProjetoIdAndStatusInAndIsAtivoTrue(projetoId, STATUS_ABERTOS) > 0;
    }

    private AtividadeResponse toResponse(Atividade atividade) {
        return toResponse(atividade, !existeAtividadeAberta(atividade.getProjeto().getId()));
    }

    private AtividadeResponse toResponse(Atividade atividade, boolean projetoPodeSerFinalizado) {
        boolean atrasada = atividade.getStatus().isAberta()
                && atividade.getDthPrazo() != null
                && atividade.getDthPrazo().isBefore(LocalDateTime.now());

        return new AtividadeResponse(
                atividade.getId(),
                atividade.getProjeto().getId(),
                atividade.getResponsavel() != null ? atividade.getResponsavel().getId() : null,
                atividade.getResponsavel() != null ? atividade.getResponsavel().getNome() : null,
                atividade.getAtividadePai() != null ? atividade.getAtividadePai().getId() : null,
                atividade.getTitulo(),
                atividade.getDescricao(),
                atividade.getStatus(),
                atividade.getDthRegistro(),
                atividade.getDthPrazo(),
                atividade.getDthConclusao(),
                atrasada,
                atividade.isAtivo(),
                projetoPodeSerFinalizado);
    }
}
