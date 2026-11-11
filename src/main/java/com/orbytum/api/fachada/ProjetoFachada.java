package com.orbytum.api.fachada;

import com.orbytum.api.models.dto.request.AdicionarParticipanteRequest;
import com.orbytum.api.models.dto.request.CreateProjetoRequest;
import com.orbytum.api.models.dto.request.EditProjetoRequest;
import com.orbytum.api.models.dto.response.ParticipanteResponse;
import com.orbytum.api.models.dto.response.ProjetoResponse;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.ProjetoXUsuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.NivelMembro;
import com.orbytum.api.models.exceptions.GrupoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.ProjetoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.SemPermissaoNoGrupoErro;
import com.orbytum.api.models.exceptions.UsuarioNaoEncontradoErro;
import com.orbytum.api.repository.ProjetoXUsuarioRepository;
import com.orbytum.api.service.GrupoAcessoService;
import com.orbytum.api.service.GrupoService;
import com.orbytum.api.service.ProjetoService;
import com.orbytum.api.service.UsuarioService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProjetoFachada {

    private final ProjetoService projetoService;
    private final GrupoService grupoService;
    private final UsuarioService usuarioService;
    private final GrupoAcessoService grupoAcessoService;
    private final ProjetoXUsuarioRepository projetoXUsuarioRepository;

    @Transactional
    public ProjetoResponse criarProjeto(CreateProjetoRequest request) {
        Grupo grupo = grupoService.findById(request.grupoId())
                .orElseThrow(() -> new GrupoNaoEncontradoErro("Grupo de pesquisa não encontrado com ID: " + request.grupoId()));

        Usuario solicitante = usuarioAutenticado();
        validarPodeGerenciarProjetos(grupo.getId(), solicitante);

        Projeto projeto = new Projeto(grupo, request.status(), request.titulo(), request.assunto());
        Projeto projetoSalvo = projetoService.save(projeto);

        Usuario liderProjeto = solicitante;
        if (request.liderProjetoId() != null) {
            if (!isAdmin(solicitante)) {
                grupoAcessoService.validarLider(grupo.getId(), solicitante.getId());
            }
            liderProjeto = usuarioService.findById(request.liderProjetoId())
                    .orElseThrow(() -> new UsuarioNaoEncontradoErro(
                            "Usuário não encontrado com ID: " + request.liderProjetoId()));
            grupoAcessoService.validarMembro(grupo.getId(), liderProjeto.getId());
            if (grupoAcessoService.isColaborador(grupo.getId(), liderProjeto.getId())) {
                throw new SemPermissaoNoGrupoErro("Um colaborador não pode ser definido como líder do projeto");
            }
        }

        if (liderProjeto.getId().equals(solicitante.getId())) {
            salvarParticipante(projetoSalvo, solicitante, NivelMembro.LIDER);
        } else {
            NivelMembro nivelSolicitante = grupoAcessoService.nivelDoUsuario(grupo.getId(), solicitante.getId());
            if (nivelSolicitante == NivelMembro.LIDER || nivelSolicitante == null) {
                nivelSolicitante = NivelMembro.PESQUISADOR;
            }
            salvarParticipante(projetoSalvo, solicitante, nivelSolicitante);
            salvarParticipante(projetoSalvo, liderProjeto, NivelMembro.LIDER);
        }

        return toResponse(projetoSalvo);
    }

    @Transactional
    public ProjetoResponse atualizarProjeto(Long id, EditProjetoRequest request) {
        Projeto projeto = buscarProjetoEntidade(id);

        Usuario solicitante = usuarioAutenticado();
        validarPodeGerenciarProjetos(projeto.getGrupo().getId(), solicitante);

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
        Projeto projeto = buscarProjetoEntidade(id);

        Usuario solicitante = usuarioAutenticado();
        validarPodeGerenciarProjetos(projeto.getGrupo().getId(), solicitante);

        projetoService.delete(projeto);
    }

    @Transactional
    public ProjetoResponse alternarFavorito(Long id) {
        Projeto projeto = buscarProjetoEntidade(id);

        Usuario solicitante = usuarioAutenticado();
        if (!isAdmin(solicitante)) {
            grupoAcessoService.validarMembro(projeto.getGrupo().getId(), solicitante.getId());
        }

        projeto.setFavorito(!projeto.isFavorito());
        return toResponse(projetoService.save(projeto));
    }

    @Transactional
    public ProjetoResponse finalizarProjeto(Long id) {
        Projeto projeto = buscarProjetoEntidade(id);

        Usuario solicitante = usuarioAutenticado();
        validarPodeGerenciarProjetos(projeto.getGrupo().getId(), solicitante);

        return toResponse(projetoService.finalizarProjeto(projeto));
    }

    @Transactional
    public List<ProjetoResponse> listarProjetosPorGrupo(Long grupoId) {
        return projetoService.findAllAtivosByGrupoId(grupoId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProjetoResponse buscarProjeto(Long id) {
        return toResponse(buscarProjetoEntidade(id));
    }

    @Transactional
    public List<ParticipanteResponse> listarParticipantes(Long projetoId) {
        buscarProjetoEntidade(projetoId);
        return projetoXUsuarioRepository.findAllByProjetoIdAndIsAtivoTrue(projetoId).stream()
                .map(this::toParticipanteResponse)
                .toList();
    }

    @Transactional
    public ParticipanteResponse adicionarParticipante(Long projetoId, AdicionarParticipanteRequest request) {
        Projeto projeto = buscarProjetoEntidade(projetoId);

        Usuario solicitante = usuarioAutenticado();
        validarPodeGerenciarProjetos(projeto.getGrupo().getId(), solicitante);

        Usuario participante = usuarioService.findById(request.usuarioId())
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário não encontrado com ID: " + request.usuarioId()));

        grupoAcessoService.validarMembro(projeto.getGrupo().getId(), participante.getId());

        NivelMembro nivelSolicitado = request.nivel() != null ? request.nivel() : NivelMembro.PESQUISADOR;
        NivelMembro nivelGrupo = grupoAcessoService.nivelDoUsuario(projeto.getGrupo().getId(), participante.getId());

        if (nivelGrupo == NivelMembro.COLABORADOR) {
            nivelSolicitado = NivelMembro.COLABORADOR;
        }

        if (nivelSolicitado == NivelMembro.LIDER) {
            if (nivelGrupo == NivelMembro.COLABORADOR) {
                throw new SemPermissaoNoGrupoErro("Um colaborador não pode ser definido como líder do projeto");
            }
            if (!isAdmin(solicitante)) {
                grupoAcessoService.validarLider(projeto.getGrupo().getId(), solicitante.getId());
            }
            rebaixarLideresAtuais(projetoId, participante.getId());
        }

        return toParticipanteResponse(salvarParticipante(projeto, participante, nivelSolicitado));
    }

    @Transactional
    public void removerParticipante(Long projetoId, Long usuarioId) {
        Projeto projeto = buscarProjetoEntidade(projetoId);

        Usuario solicitante = usuarioAutenticado();
        validarPodeGerenciarProjetos(projeto.getGrupo().getId(), solicitante);

        ProjetoXUsuario vinculo = projetoXUsuarioRepository
                .findByProjetoIdAndUsuarioIdAndIsAtivoTrue(projetoId, usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Este usuário não participa deste projeto"));

        vinculo.setAtivo(false);
        projetoXUsuarioRepository.save(vinculo);
    }

    private ProjetoXUsuario salvarParticipante(Projeto projeto, Usuario usuario, NivelMembro nivel) {
        ProjetoXUsuario vinculo = projetoXUsuarioRepository
                .findByProjetoIdAndUsuarioIdAndIsAtivoTrue(projeto.getId(), usuario.getId())
                .orElse(null);

        if (vinculo != null) {
            vinculo.setNivel(nivel != null ? nivel : NivelMembro.PESQUISADOR);
            vinculo.setAtivo(true);
        } else {
            vinculo = new ProjetoXUsuario(projeto, usuario, nivel != null ? nivel : NivelMembro.PESQUISADOR);
        }

        return projetoXUsuarioRepository.save(vinculo);
    }

    private void rebaixarLideresAtuais(Long projetoId, Long novoLiderId) {
        projetoXUsuarioRepository.findAllByProjetoIdAndNivelAndIsAtivoTrue(projetoId, NivelMembro.LIDER)
                .stream()
                .filter(vinculo -> !vinculo.getUsuario().getId().equals(novoLiderId))
                .forEach(vinculo -> {
                    vinculo.setNivel(NivelMembro.PESQUISADOR);
                    projetoXUsuarioRepository.save(vinculo);
                });
    }

    private Projeto buscarProjetoEntidade(Long id) {
        return projetoService.findById(id)
                .orElseThrow(() -> new ProjetoNaoEncontradoErro("Projeto não encontrado com ID: " + id));
    }

    private Usuario usuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioService.findByEmail(email)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário autenticado não encontrado"));
    }

    private void validarPodeGerenciarProjetos(Long grupoId, Usuario solicitante) {
        if (isAdmin(solicitante)) {
            return;
        }
        grupoAcessoService.validarCoordenadorOuAcima(grupoId, solicitante.getId());
    }

    private boolean isAdmin(Usuario solicitante) {
        return solicitante.getCredenciaisLogin() != null
                && solicitante.getCredenciaisLogin().getAccessLevel() == AccessLevel.ADMIN;
    }

    private ParticipanteResponse toParticipanteResponse(ProjetoXUsuario vinculo) {
        return new ParticipanteResponse(
                vinculo.getUsuario().getId(),
                vinculo.getUsuario().getNome(),
                vinculo.getUsuario().getEmail(),
                vinculo.getNivel(),
                vinculo.isAtivo());
    }

    private ProjetoResponse toResponse(Projeto projeto) {
        ProjetoXUsuario lider = projetoXUsuarioRepository
                .findFirstByProjetoIdAndNivelAndIsAtivoTrue(projeto.getId(), NivelMembro.LIDER)
                .orElse(null);

        Long liderProjetoId = lider != null && lider.getUsuario() != null ? lider.getUsuario().getId() : null;
        String liderProjetoNome = lider != null && lider.getUsuario() != null ? lider.getUsuario().getNome() : null;

        return new ProjetoResponse(
                projeto.getId(),
                projeto.getGrupo().getId(),
                projeto.getStatus(),
                projeto.getTitulo(),
                projeto.getAssunto(),
                projeto.getDthRegistro(),
                projeto.isAtivo(),
                projeto.isInicial(),
                projeto.isFavorito(),
                liderProjetoId,
                liderProjetoNome);
    }
}
