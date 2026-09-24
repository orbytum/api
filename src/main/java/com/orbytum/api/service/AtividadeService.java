package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.CreateAtividadeRequest;
import com.orbytum.api.models.dto.request.EditAtividadeRequest;
import com.orbytum.api.models.dto.response.AtividadeResponse;
import com.orbytum.api.models.entity.Atividade;
import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.AtividadeStatus;
import com.orbytum.api.models.exceptions.AtividadeNaoEncontradaErro;
import com.orbytum.api.models.exceptions.AtividadeNaoPertenceAoProjetoErro;
import com.orbytum.api.models.exceptions.ProjetoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.TransicaoAtividadeInvalidaErro;
import com.orbytum.api.models.exceptions.UsuarioNaoEncontradoErro;
import com.orbytum.api.repository.AtividadeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
            grupoAcessoService.validarMembroDoProjeto(projeto.getId(), responsavel.getId());
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
                grupoAcessoService.validarMembroDoProjeto(atividade.getProjeto().getId(), responsavel.getId());
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

    public AtividadeResponse buscar(Long id, String emailLogado) {
        Atividade atividade = buscarEntidade(id);
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarAcessoAoGrupo(atividade.getProjeto().getGrupo().getId(), solicitante);
        return toResponse(atividade);
    }

    public List<AtividadeResponse> listarPorProjeto(Long projetoId, String emailLogado) {
        Projeto projeto = projetoService.findById(projetoId)
                .orElseThrow(() -> new ProjetoNaoEncontradoErro("Projeto não encontrado com ID: " + projetoId));
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarAcessoAoGrupo(projeto.getGrupo().getId(), solicitante);

        return atividadeRepository.findAllByProjetoIdAndIsAtivoTrue(projetoId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AtividadeResponse> listarAtrasadasPorProjeto(Long projetoId, String emailLogado) {
        return listarPorProjeto(projetoId, emailLogado).stream()
                .filter(AtividadeResponse::isAtrasada)
                .toList();
    }

    private Atividade buscarEntidade(Long id) {
        return atividadeRepository.findById(id)
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
        boolean atrasada = atividade.getStatus().isAberta()
                && atividade.getDthPrazo() != null
                && atividade.getDthPrazo().isBefore(LocalDateTime.now());

        boolean projetoPodeSerFinalizado = !existeAtividadeAberta(atividade.getProjeto().getId());

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
