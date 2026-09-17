package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.CriarSolicitacaoRequest;
import com.orbytum.api.models.dto.request.EditarSolicitacaoRequest;
import com.orbytum.api.models.dto.response.SolicitacaoResponse;
import com.orbytum.api.models.entity.MaterialEmprestimoSolicitacao;
import com.orbytum.api.models.entity.MaterialEmprestimoSolicitacaoItem;
import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.ProjetoRepository;
import com.orbytum.api.repository.SolicitacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SolicitacaoService {

    private final SolicitacaoRepository solicitacaoRepository;
    private final ProjetoRepository projetoRepository;
    private final UsuarioService usuarioService;
    private final GrupoXUsuarioRepository grupoXUsuarioRepository;
    private final CredenciaisLoginService credenciaisLoginService;

    @Transactional
    public SolicitacaoResponse criar(CriarSolicitacaoRequest request, String emailLogado) {
        Usuario usuario = buscarUsuario(emailLogado);
        Projeto projeto = buscarProjeto(request.projetoId());

        GrupoXUsuario vinculo = validarPermissaoMembro(projeto.getGrupo().getId(), usuario);

        MaterialEmprestimoSolicitacao solicitacao = MaterialEmprestimoSolicitacao.builder()
                .usuario(vinculo)
                .projeto(projeto)
                .justificativa(request.justificativa())
                .isInterna("uso_material".equalsIgnoreCase(request.tipo()))
                .isAprovada(false)
                .dthSolicitacao(LocalDateTime.now())
                .build();

        if (request.valor() != null || request.quantidade() != null) {
            solicitacao.getItems().add(MaterialEmprestimoSolicitacaoItem.builder()
                    .quantidade(request.quantidade())
                    .valor(request.valor())
                    .build());
        }

        return mapearParaResponse(solicitacaoRepository.save(solicitacao));
    }

    public List<SolicitacaoResponse> listarPorProjeto(Long projetoId, String emailLogado) {
        Usuario usuario = buscarUsuario(emailLogado);
        Projeto projeto = buscarProjeto(projetoId);

        validarPermissaoMembro(projeto.getGrupo().getId(), usuario);

        return solicitacaoRepository.findAllByProjetoId(projetoId).stream()
                .map(this::mapearParaResponse)
                .toList();
    }

    public SolicitacaoResponse buscarPorId(Long id, String emailLogado) {
        Usuario usuario = buscarUsuario(emailLogado);
        MaterialEmprestimoSolicitacao solicitacao = buscarSolicitacao(id);

        validarPermissaoMembro(solicitacao.getProjeto().getGrupo().getId(), usuario);

        return mapearParaResponse(solicitacao);
    }

    @Transactional
    public SolicitacaoResponse atualizar(Long id, EditarSolicitacaoRequest request, String emailLogado) {
        Usuario usuario = buscarUsuario(emailLogado);
        MaterialEmprestimoSolicitacao solicitacao = buscarSolicitacao(id);

        validarPermissaoAlteracao(solicitacao, usuario);

        solicitacao.setJustificativa(request.justificativa());
        if (!solicitacao.getItems().isEmpty()) {
            if (request.valor() != null) solicitacao.getItems().get(0).setValor(request.valor());
            if (request.quantidade() != null) solicitacao.getItems().get(0).setQuantidade(request.quantidade());
        }

        return mapearParaResponse(solicitacaoRepository.save(solicitacao));
    }

    @Transactional
    public void remover(Long id, String emailLogado) {
        Usuario usuario = buscarUsuario(emailLogado);
        MaterialEmprestimoSolicitacao solicitacao = buscarSolicitacao(id);

        validarPermissaoAlteracao(solicitacao, usuario);

        solicitacaoRepository.delete(solicitacao);
    }

    private GrupoXUsuario validarPermissaoMembro(Long grupoId, Usuario usuario) {
        if (isAdmin(usuario)) {
            return grupoXUsuarioRepository.findAllByGrupoIdAndIsAtivoTrue(grupoId).stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Grupo sem membros ativos"));
        }

        return grupoXUsuarioRepository.findByGrupoIdAndUsuarioIdAndIsAtivoTrue(grupoId, usuario.getId())
                .orElseThrow(() -> new AccessDeniedException("Você não possui permissão neste grupo de pesquisa"));
    }

    private void validarPermissaoAlteracao(MaterialEmprestimoSolicitacao solicitacao, Usuario usuario) {
        if (solicitacao.isAprovada()) {
            throw new IllegalArgumentException("Solicitações já aprovadas não podem ser alteradas ou canceladas");
        }

        if (isAdmin(usuario)) {
            return;
        }

        boolean isAutor = solicitacao.getUsuario() != null &&
                solicitacao.getUsuario().getUsuario().getId().equals(usuario.getId());

        if (!isAutor) {
            GrupoXUsuario vinculo = grupoXUsuarioRepository
                    .findByGrupoIdAndUsuarioIdAndIsAtivoTrue(solicitacao.getProjeto().getGrupo().getId(), usuario.getId())
                    .orElseThrow(() -> new AccessDeniedException("Acesso negado"));

            if (vinculo.getRole() == null || !vinculo.getRole().isLider()) {
                throw new AccessDeniedException("Apenas o autor ou o líder do grupo podem realizar esta ação");
            }
        }
    }

    private boolean isAdmin(Usuario usuario) {
        return credenciaisLoginService.findByEmail(usuario.getEmail())
                .map(c -> c.getAccessLevel() == AccessLevel.ADMIN)
                .orElse(false);
    }

    private Usuario buscarUsuario(String email) {
        return usuarioService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    }

    private Projeto buscarProjeto(Long id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Projeto não encontrado"));
    }

    private MaterialEmprestimoSolicitacao buscarSolicitacao(Long id) {
        return solicitacaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada"));
    }

    private SolicitacaoResponse mapearParaResponse(MaterialEmprestimoSolicitacao s) {
        BigDecimal valor = !s.getItems().isEmpty() ? s.getItems().get(0).getValor() : null;
        Integer qtd = !s.getItems().isEmpty() ? s.getItems().get(0).getQuantidade() : null;

        return new SolicitacaoResponse(
                s.getId(),
                s.getJustificativa(),
                s.getJustificativa(),
                s.getJustificativa(),
                s.isInterna(),
                s.isAprovada(),
                s.getProjeto() != null ? s.getProjeto().getId() : null,
                s.getProjeto() != null ? s.getProjeto().getTitulo() : null,
                s.getUsuario() != null && s.getUsuario().getUsuario() != null ? s.getUsuario().getUsuario().getId() : null,
                s.getUsuario() != null && s.getUsuario().getUsuario() != null ? s.getUsuario().getUsuario().getNome() : null,
                qtd,
                valor,
                s.getDthSolicitacao(),
                s.getDthResposta()
        );
    }
}
