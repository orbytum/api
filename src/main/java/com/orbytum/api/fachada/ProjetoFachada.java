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

        NivelMembro nivelSolicitante = grupoAcessoService.nivelDoUsuario(grupo.getId(), solicitante.getId());
        adicionarParticipante(projetoSalvo, solicitante,
                nivelSolicitante != null ? nivelSolicitante : NivelMembro.LIDER);

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

    public List<ProjetoResponse> listarProjetosPorGrupo(Long grupoId) {
        return projetoService.findAllAtivosByGrupoId(grupoId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjetoResponse buscarProjeto(Long id) {
        return toResponse(buscarProjetoEntidade(id));
    }

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

        ProjetoXUsuario vinculo = projetoXUsuarioRepository
                .findByProjetoIdAndUsuarioIdAndIsAtivoTrue(projetoId, participante.getId())
                .orElse(null);

        if (vinculo != null) {
            vinculo.setNivel(request.nivel() != null ? request.nivel() : NivelMembro.PESQUISADOR);
            vinculo.setAtivo(true);
        } else {
            vinculo = new ProjetoXUsuario(projeto, participante,
                    request.nivel() != null ? request.nivel() : NivelMembro.PESQUISADOR);
        }

        return toParticipanteResponse(projetoXUsuarioRepository.save(vinculo));
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

    private void adicionarParticipante(Projeto projeto, Usuario usuario, NivelMembro nivel) {
        projetoXUsuarioRepository.save(new ProjetoXUsuario(projeto, usuario, nivel));
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
        return new ProjetoResponse(
                projeto.getId(),
                projeto.getGrupo().getId(),
                projeto.getStatus(),
                projeto.getTitulo(),
                projeto.getAssunto(),
                projeto.getDthRegistro(),
                projeto.isAtivo(),
                projeto.isInicial(),
                projeto.isFavorito());
    }
}
