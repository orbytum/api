package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.CreateNoticiaRequest;
import com.orbytum.api.models.dto.request.EditNoticiaRequest;
import com.orbytum.api.models.dto.request.RejeitarNoticiaRequest;
import com.orbytum.api.models.dto.response.NoticiaPaginadaResponse;
import com.orbytum.api.models.dto.response.NoticiaResponse;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Noticia;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.NoticiaStatus;
import com.orbytum.api.models.exceptions.GrupoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.NoticiaNaoEncontradaErro;
import com.orbytum.api.models.exceptions.NoticiaNaoPendenteErro;
import com.orbytum.api.models.exceptions.SemPermissaoNoGrupoErro;
import com.orbytum.api.models.exceptions.UsuarioNaoEncontradoErro;
import com.orbytum.api.repository.NoticiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticiaService {

    private final NoticiaRepository noticiaRepository;
    private final GrupoService grupoService;
    private final UsuarioService usuarioService;
    private final GrupoAcessoService grupoAcessoService;

    @Value("${orbytum.noticias.prazo-exibicao-dias:30}")
    private int prazoExibicaoDias;

    @Transactional
    public NoticiaResponse criar(CreateNoticiaRequest request, String emailLogado) {
        if (request == null) {
            throw new IllegalArgumentException("A requisição não pode ser nula.");
        }

        Grupo grupo = buscarGrupo(request.grupoId());
        Usuario autor = usuarioAutenticado(emailLogado);
        validarAcessoAoGrupo(grupo.getId(), autor);
        validarPeriodoExibicao(request.dataInicioExibicao(), request.dataFimExibicao());

        Noticia noticia = new Noticia(
                grupo,
                autor,
                request.titulo().trim(),
                request.conteudo().trim(),
                normalizar(request.referencia()),
                request.assunto().trim(),
                request.dataInicioExibicao(),
                request.dataFimExibicao());

        Noticia salva = noticiaRepository.save(noticia);
        return toResponse(salva);
    }

    @Transactional
    public NoticiaResponse atualizar(Long id, EditNoticiaRequest request, String emailLogado) {
        Noticia noticia = buscarEntidade(id);
        Usuario solicitante = usuarioAutenticado(emailLogado);
        Long grupoId = noticia.getGrupo().getId();

        validarAcessoAoGrupo(grupoId, solicitante);
        validarPermissaoEdicao(noticia, solicitante, grupoId);
        validarPeriodoExibicao(request.dataInicioExibicao(), request.dataFimExibicao());

        noticia.setTitulo(request.titulo().trim());
        noticia.setConteudo(request.conteudo().trim());
        noticia.setReferencia(normalizar(request.referencia()));
        noticia.setAssunto(request.assunto().trim());
        noticia.setDataInicioExibicao(request.dataInicioExibicao());
        noticia.setDataFimExibicao(request.dataFimExibicao());

        if (noticia.getStatus() == NoticiaStatus.REJEITADA) {
            noticia.setStatus(NoticiaStatus.PENDENTE);
            noticia.setMotivoRejeicao(null);
            noticia.setAprovador(null);
            noticia.setDthAprovacao(null);
        }

        return toResponse(noticiaRepository.save(noticia));
    }

    @Transactional
    public NoticiaResponse aprovar(Long id, String emailLogado) {
        Noticia noticia = buscarEntidade(id);
        Usuario solicitante = usuarioAutenticado(emailLogado);
        Long grupoId = noticia.getGrupo().getId();

        validarCoordenadorOuAdmin(grupoId, solicitante);

        if (noticia.getStatus() != NoticiaStatus.PENDENTE) {
            throw new NoticiaNaoPendenteErro("Somente notícias pendentes podem ser aprovadas.");
        }

        LocalDateTime agora = LocalDateTime.now();
        if (noticia.getDataFimExibicao() == null) {
            noticia.setDataFimExibicao(agora.plusDays(prazoExibicaoDias));
        }
        validarPeriodoExibicao(noticia.getDataInicioExibicao(), noticia.getDataFimExibicao());

        noticia.setStatus(NoticiaStatus.APROVADA);
        noticia.setAprovador(solicitante);
        noticia.setDthAprovacao(agora);
        noticia.setMotivoRejeicao(null);

        return toResponse(noticiaRepository.save(noticia));
    }

    @Transactional
    public NoticiaResponse rejeitar(Long id, RejeitarNoticiaRequest request, String emailLogado) {
        Noticia noticia = buscarEntidade(id);
        Usuario solicitante = usuarioAutenticado(emailLogado);
        Long grupoId = noticia.getGrupo().getId();

        validarCoordenadorOuAdmin(grupoId, solicitante);

        if (noticia.getStatus() != NoticiaStatus.PENDENTE) {
            throw new NoticiaNaoPendenteErro("Somente notícias pendentes podem ser rejeitadas.");
        }

        noticia.setStatus(NoticiaStatus.REJEITADA);
        noticia.setAprovador(solicitante);
        noticia.setDthAprovacao(LocalDateTime.now());
        noticia.setMotivoRejeicao(request != null ? normalizar(request.motivo()) : null);

        return toResponse(noticiaRepository.save(noticia));
    }

    @Transactional
    public void deletar(Long id, String emailLogado) {
        Noticia noticia = buscarEntidade(id);
        Usuario solicitante = usuarioAutenticado(emailLogado);
        Long grupoId = noticia.getGrupo().getId();

        validarAcessoAoGrupo(grupoId, solicitante);
        validarPermissaoEdicao(noticia, solicitante, grupoId);

        noticia.setAtivo(false);
        noticiaRepository.save(noticia);
    }

    @Transactional(readOnly = true)
    public NoticiaResponse buscar(Long id, String emailLogado) {
        Noticia noticia = buscarEntidade(id);
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarAcessoAoGrupo(noticia.getGrupo().getId(), solicitante);
        return toResponse(noticia);
    }

    @Transactional(readOnly = true)
    public NoticiaPaginadaResponse listar(
            Long grupoId,
            String assunto,
            LocalDate dataInicio,
            LocalDate dataFim,
            int page,
            int size,
            String emailLogado
    ) {
        Grupo grupo = buscarGrupo(grupoId);
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarAcessoAoGrupo(grupo.getId(), solicitante);

        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "dthRegistro"));

        LocalDateTime inicio = (dataInicio != null) ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = (dataFim != null) ? dataFim.atTime(LocalTime.MAX) : null;
        String normalizedAssunto = normalizar(assunto);

        Page<Noticia> resultado = noticiaRepository.filtrarVigentes(
                grupo.getId(),
                NoticiaStatus.APROVADA,
                LocalDateTime.now(),
                normalizedAssunto,
                inicio,
                fim,
                pageable);

        List<NoticiaResponse> items = resultado.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new NoticiaPaginadaResponse(
                items,
                resultado.getTotalElements(),
                resultado.getTotalPages(),
                page,
                pageSize);
    }

    @Transactional(readOnly = true)
    public NoticiaPaginadaResponse listarPendentes(Long grupoId, int page, int size, String emailLogado) {
        Grupo grupo = buscarGrupo(grupoId);
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarCoordenadorOuAdmin(grupo.getId(), solicitante);

        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.ASC, "dthRegistro"));

        Page<Noticia> resultado = noticiaRepository.findByGrupoIdAndStatusAndIsAtivoTrue(
                grupo.getId(), NoticiaStatus.PENDENTE, pageable);

        List<NoticiaResponse> items = resultado.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new NoticiaPaginadaResponse(
                items,
                resultado.getTotalElements(),
                resultado.getTotalPages(),
                page,
                pageSize);
    }

    private Noticia buscarEntidade(Long id) {
        return noticiaRepository.findByIdAndIsAtivoTrue(id)
                .orElseThrow(() -> new NoticiaNaoEncontradaErro("Notícia não encontrada com ID: " + id));
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

    private void validarPermissaoEdicao(Noticia noticia, Usuario solicitante, Long grupoId) {
        if (isAdmin(solicitante)) {
            return;
        }

        boolean isAutor = noticia.getAutor() != null
                && noticia.getAutor().getId().equals(solicitante.getId());

        if (grupoAcessoService.isCoordenadorOuAcima(grupoId, solicitante.getId())) {
            return;
        }

        if (!isAutor) {
            throw new SemPermissaoNoGrupoErro("Apenas o autor ou Líder/Coordenador podem alterar esta notícia");
        }

        if (noticia.getStatus() == NoticiaStatus.APROVADA) {
            throw new NoticiaNaoPendenteErro("Notícia publicada só pode ser alterada por Líder ou Coordenador do grupo");
        }
    }

    private void validarAcessoAoGrupo(Long grupoId, Usuario solicitante) {
        if (isAdmin(solicitante)) {
            return;
        }
        grupoAcessoService.validarMembro(grupoId, solicitante.getId());
    }

    private void validarCoordenadorOuAdmin(Long grupoId, Usuario solicitante) {
        if (isAdmin(solicitante)) {
            return;
        }
        grupoAcessoService.validarCoordenadorOuAcima(grupoId, solicitante.getId());
    }

    private boolean isAdmin(Usuario usuario) {
        return usuario.getCredenciaisLogin() != null
                && usuario.getCredenciaisLogin().getAccessLevel() == AccessLevel.ADMIN;
    }

    private void validarPeriodoExibicao(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio != null && fim != null && !fim.isAfter(inicio)) {
            throw new IllegalArgumentException("A data final de exibição deve ser posterior à data inicial.");
        }
    }

    private String normalizar(String valor) {
        return (valor != null && !valor.isBlank()) ? valor.trim() : null;
    }

    private NoticiaResponse toResponse(Noticia noticia) {
        return NoticiaResponse.fromEntity(noticia, isExpirada(noticia));
    }

    boolean isExpirada(Noticia noticia) {
        LocalDateTime agora = LocalDateTime.now();
        if (noticia.getDataFimExibicao() != null) {
            return agora.isAfter(noticia.getDataFimExibicao());
        }
        LocalDateTime base = noticia.getDthAprovacao() != null
                ? noticia.getDthAprovacao()
                : noticia.getDthRegistro();
        return base != null && agora.isAfter(base.plusDays(prazoExibicaoDias));
    }
}
