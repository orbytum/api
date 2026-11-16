package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.CriarSolicitacaoRequest;
import com.orbytum.api.models.dto.request.EditarSolicitacaoRequest;
import com.orbytum.api.models.dto.response.SolicitacaoResponse;
import com.orbytum.api.models.entity.Material;
import com.orbytum.api.models.entity.MaterialEmprestimo;
import com.orbytum.api.models.entity.MaterialEmprestimoSolicitacao;
import com.orbytum.api.models.entity.MaterialEmprestimoSolicitacaoItem;
import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.entity.joinColumns.GrupoXUsuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.MaterialStatus;
import com.orbytum.api.models.enums.SolicitacaoStatus;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.MaterialEmprestimoRepository;
import com.orbytum.api.repository.MaterialRepository;
import com.orbytum.api.repository.ProjetoRepository;
import com.orbytum.api.repository.SolicitacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SolicitacaoService {

    private static final Set<String> TIPOS_VALIDOS = Set.of(
            "uso_material",
            "financiamento",
            "compra_material_catalogado",
            "compra_material"
    );

    private final SolicitacaoRepository solicitacaoRepository;
    private final ProjetoRepository projetoRepository;
    private final UsuarioService usuarioService;
    private final GrupoXUsuarioRepository grupoXUsuarioRepository;
    private final CredenciaisLoginService credenciaisLoginService;
    private final MaterialRepository materialRepository;
    private final MaterialEmprestimoRepository materialEmprestimoRepository;
    private final GrupoAcessoService grupoAcessoService;

    @Transactional
    public SolicitacaoResponse criar(CriarSolicitacaoRequest request, String emailLogado) {
        validarTipo(request.tipo());
        Usuario usuario = buscarUsuario(emailLogado);
        Projeto projeto = buscarProjeto(request.projetoId());

        Long grupoId = projeto.getGrupo().getId();
        GrupoXUsuario vinculo = grupoXUsuarioRepository
                .findByGrupoIdAndUsuarioIdAndIsAtivoTrue(grupoId, usuario.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "Apenas o líder do projeto ou o Líder do grupo podem criar solicitações"));
        boolean liderDoGrupo = grupoAcessoService.isLider(grupoId, usuario.getId());
        if (!liderDoGrupo && !grupoAcessoService.isLiderDoProjeto(projeto.getId(), usuario.getId())) {
            throw new AccessDeniedException("Apenas o líder do projeto ou o Líder do grupo podem criar solicitações");
        }

        boolean isUsoMaterial = "uso_material".equalsIgnoreCase(request.tipo());

        Material material = null;
        if (isUsoMaterial) {
            if (request.materialId() == null) {
                throw new IllegalArgumentException("Para solicitações de uso de material, o material deve ser informado");
            }
            material = buscarMaterial(request.materialId());
            if (material.getStatus() == MaterialStatus.MANUTENCAO || material.getStatus() == MaterialStatus.EM_USO) {
                throw new IllegalArgumentException("Material selecionado não está disponível para uso");
            }
            if (request.quantidade() == null || request.quantidade() <= 0) {
                throw new IllegalArgumentException("A quantidade deve ser informada e maior que zero");
            }
            if (request.quantidade() > material.getQuantidade()) {
                throw new IllegalArgumentException("Quantidade solicitada excede a quantidade disponível em estoque");
            }
        } else if (request.materialId() != null) {
            material = buscarMaterial(request.materialId());
        }

        MaterialEmprestimoSolicitacao solicitacao = MaterialEmprestimoSolicitacao.builder()
                .usuario(vinculo)
                .projeto(projeto)
                .titulo(request.titulo())
                .descricao(request.descricao())
                .justificativa(request.justificativa())
                .isInterna(isUsoMaterial)
                .isAprovada(false)
                .dthSolicitacao(LocalDateTime.now())
                .build();

        // Quando o próprio Líder do grupo solicita, a primeira etapa é dispensada.
        if (liderDoGrupo) {
            registrarAprovacaoDoLider(solicitacao, usuario);
        } else {
            solicitacao.setStatus(SolicitacaoStatus.PENDENTE);
        }

        if (request.valor() != null || request.quantidade() != null || material != null) {
            solicitacao.getItems().add(MaterialEmprestimoSolicitacaoItem.builder()
                    .material(material)
                    .quantidade(request.quantidade())
                    .valor(request.valor())
                    .build());
        }

        return mapearParaResponse(solicitacaoRepository.save(solicitacao));
    }

    /** Todas as solicitações do sistema; exclusivo do administrador, que decide a segunda etapa. */
    @Transactional(readOnly = true)
    public List<SolicitacaoResponse> listarTodas(String emailLogado) {
        Usuario usuario = buscarUsuario(emailLogado);
        if (!isAdmin(usuario)) {
            throw new AccessDeniedException("Apenas o administrador pode listar as solicitações de todos os grupos");
        }
        return solicitacaoRepository.findAllByOrderByDthSolicitacaoDesc().stream()
                .map(this::mapearParaResponse)
                .toList();
    }

    /** Solicitações do grupo: administrador e Líder do grupo veem todas; os demais, as dos seus projetos. */
    @Transactional(readOnly = true)
    public List<SolicitacaoResponse> listarPorGrupo(Long grupoId, String emailLogado) {
        Usuario usuario = buscarUsuario(emailLogado);
        boolean veTodas = isAdmin(usuario) || grupoAcessoService.isLider(grupoId, usuario.getId());
        if (!veTodas) {
            validarPermissaoMembro(grupoId, usuario);
        }
        return solicitacaoRepository.findAllByProjetoGrupoId(grupoId).stream()
                .filter(s -> veTodas || grupoAcessoService.podeAcessarProjeto(s.getProjeto().getId(), usuario.getId()))
                .sorted((a, b) -> b.getDthSolicitacao().compareTo(a.getDthSolicitacao()))
                .map(this::mapearParaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoResponse> listarPorProjeto(Long projetoId, String emailLogado) {
        Usuario usuario = buscarUsuario(emailLogado);
        Projeto projeto = buscarProjeto(projetoId);

        validarPermissaoMembro(projeto.getGrupo().getId(), usuario);
        if (!isAdmin(usuario)) {
            grupoAcessoService.validarAcessoAoProjeto(projetoId, usuario.getId());
        }

        return solicitacaoRepository.findAllByProjetoId(projetoId).stream()
                .map(this::mapearParaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SolicitacaoResponse buscarPorId(Long id, String emailLogado) {
        Usuario usuario = buscarUsuario(emailLogado);
        MaterialEmprestimoSolicitacao solicitacao = buscarSolicitacao(id);

        validarPermissaoMembro(solicitacao.getProjeto().getGrupo().getId(), usuario);
        if (!isAdmin(usuario)) {
            grupoAcessoService.validarAcessoAoProjeto(solicitacao.getProjeto().getId(), usuario.getId());
        }

        return mapearParaResponse(solicitacao);
    }

    @Transactional
    public SolicitacaoResponse atualizar(Long id, EditarSolicitacaoRequest request, String emailLogado) {
        Usuario usuario = buscarUsuario(emailLogado);
        MaterialEmprestimoSolicitacao solicitacao = buscarSolicitacao(id);

        validarPermissaoAlteracao(solicitacao, usuario);

        solicitacao.setTitulo(request.titulo());
        solicitacao.setDescricao(request.descricao());
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

    @Transactional
    public SolicitacaoResponse aprovar(Long id, String emailLogado) {
        Usuario usuario = buscarUsuario(emailLogado);
        MaterialEmprestimoSolicitacao solicitacao = buscarSolicitacao(id);

        Long grupoId = solicitacao.getProjeto().getGrupo().getId();

        // Primeira etapa: o Líder do grupo aprova e encaminha ao administrador.
        if (solicitacao.getStatus() == SolicitacaoStatus.PENDENTE) {
            if (!grupoAcessoService.isLider(grupoId, usuario.getId())) {
                throw new AccessDeniedException("A primeira aprovação da solicitação cabe ao Líder do grupo");
            }
            registrarAprovacaoDoLider(solicitacao, usuario);
            return mapearParaResponse(solicitacaoRepository.save(solicitacao));
        }

        // Segunda etapa: o administrador decide.
        if (solicitacao.getStatus() != SolicitacaoStatus.AGUARDANDO_ADMINISTRADOR) {
            throw new IllegalArgumentException("Esta solicitação não está aguardando aprovação");
        }
        if (!isAdmin(usuario)) {
            throw new AccessDeniedException("Após a aprovação do Líder do grupo, a decisão cabe ao administrador");
        }

        solicitacao.setAprovada(true);
        solicitacao.setDthResposta(LocalDateTime.now());
        if (solicitacao.isInterna()) {
            solicitacao.setStatus(SolicitacaoStatus.EM_ANDAMENTO);

            Material material = solicitacao.getItems().get(0).getMaterial();
            Integer quantidade = solicitacao.getItems().get(0).getQuantidade();

            MaterialEmprestimo emprestimo = MaterialEmprestimo.builder()
                    .material(material)
                    .grupo(solicitacao.getProjeto().getGrupo())
                    .quantidade(quantidade)
                    .isDevolvido(false)
                    .dthInicio(LocalDateTime.now())
                    .build();
            materialEmprestimoRepository.save(emprestimo);

            material.setStatus(MaterialStatus.EM_USO);
            materialRepository.save(material);
        } else {
            solicitacao.setStatus(SolicitacaoStatus.CONCLUIDA);
        }

        return mapearParaResponse(solicitacaoRepository.save(solicitacao));
    }

    @Transactional
    public SolicitacaoResponse rejeitar(Long id, String emailLogado) {
        Usuario usuario = buscarUsuario(emailLogado);
        MaterialEmprestimoSolicitacao solicitacao = buscarSolicitacao(id);

        Long grupoId = solicitacao.getProjeto().getGrupo().getId();

        if (solicitacao.getStatus() == SolicitacaoStatus.PENDENTE) {
            if (!grupoAcessoService.isLider(grupoId, usuario.getId())) {
                throw new AccessDeniedException("A primeira avaliação da solicitação cabe ao Líder do grupo");
            }
        } else if (solicitacao.getStatus() == SolicitacaoStatus.AGUARDANDO_ADMINISTRADOR) {
            if (!isAdmin(usuario)) {
                throw new AccessDeniedException("Após a aprovação do Líder do grupo, a decisão cabe ao administrador");
            }
        } else {
            throw new IllegalArgumentException("Esta solicitação não está aguardando aprovação");
        }

        solicitacao.setAprovada(false);
        solicitacao.setDthResposta(LocalDateTime.now());
        solicitacao.setStatus(SolicitacaoStatus.REJEITADA);

        return mapearParaResponse(solicitacaoRepository.save(solicitacao));
    }

    @Transactional
    public SolicitacaoResponse concluir(Long id, String emailLogado) {
        Usuario usuario = buscarUsuario(emailLogado);
        MaterialEmprestimoSolicitacao solicitacao = buscarSolicitacao(id);

        validarPermissaoConclusao(solicitacao, usuario);

        if (solicitacao.getStatus() != SolicitacaoStatus.EM_ANDAMENTO) {
            throw new IllegalArgumentException("Apenas solicitações em andamento podem ser concluídas");
        }

        solicitacao.setStatus(SolicitacaoStatus.CONCLUIDA);

        devolverMaterial(solicitacao);

        return mapearParaResponse(solicitacaoRepository.save(solicitacao));
    }

    @Transactional
    public SolicitacaoResponse encerrar(Long id, String emailLogado) {
        Usuario usuario = buscarUsuario(emailLogado);
        MaterialEmprestimoSolicitacao solicitacao = buscarSolicitacao(id);

        validarPermissaoLiderOuAdmin(solicitacao.getProjeto().getGrupo().getId(), usuario);

        if (solicitacao.getStatus() != SolicitacaoStatus.EM_ANDAMENTO) {
            throw new IllegalArgumentException("Apenas solicitações em andamento podem ser encerradas");
        }

        solicitacao.setStatus(SolicitacaoStatus.ENCERRADA);

        devolverMaterial(solicitacao);

        return mapearParaResponse(solicitacaoRepository.save(solicitacao));
    }

    private void devolverMaterial(MaterialEmprestimoSolicitacao solicitacao) {
        if (!solicitacao.isInterna() || solicitacao.getItems().isEmpty()) {
            return;
        }

        Material material = solicitacao.getItems().get(0).getMaterial();
        materialEmprestimoRepository
                .findFirstByMaterialIdAndIsDevolvidoFalseOrderByDthInicioDesc(material.getId())
                .ifPresent(emprestimo -> {
                    emprestimo.setDevolvido(true);
                    emprestimo.setDthFim(LocalDateTime.now());
                    materialEmprestimoRepository.save(emprestimo);
                });

        material.setStatus(MaterialStatus.DISPONIVEL);
        materialRepository.save(material);
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

    private void registrarAprovacaoDoLider(MaterialEmprestimoSolicitacao solicitacao, Usuario lider) {
        solicitacao.setStatus(SolicitacaoStatus.AGUARDANDO_ADMINISTRADOR);
        solicitacao.setAprovadorLider(lider);
        solicitacao.setDthAprovacaoLider(LocalDateTime.now());
    }

    /**
     * Pendente: o autor ou o Líder do grupo alteram/cancelam.
     * Aguardando administrador: somente o Líder do grupo, que já a aprovou.
     */
    private void validarPermissaoAlteracao(MaterialEmprestimoSolicitacao solicitacao, Usuario usuario) {
        SolicitacaoStatus status = solicitacao.getStatus();
        if (status != SolicitacaoStatus.PENDENTE && status != SolicitacaoStatus.AGUARDANDO_ADMINISTRADOR) {
            throw new IllegalArgumentException("Solicitações já decididas não podem ser alteradas ou canceladas");
        }

        if (grupoAcessoService.isLider(solicitacao.getProjeto().getGrupo().getId(), usuario.getId())) {
            return;
        }
        if (status == SolicitacaoStatus.PENDENTE && isAutor(solicitacao, usuario)) {
            return;
        }
        throw new AccessDeniedException(status == SolicitacaoStatus.PENDENTE
                ? "Apenas o autor ou o Líder do grupo podem realizar esta ação"
                : "Após a aprovação, apenas o Líder do grupo pode alterar ou cancelar a solicitação");
    }

    private void validarPermissaoConclusao(MaterialEmprestimoSolicitacao solicitacao, Usuario usuario) {
        if (isAdmin(usuario) || isAutor(solicitacao, usuario)
                || grupoAcessoService.isLider(solicitacao.getProjeto().getGrupo().getId(), usuario.getId())) {
            return;
        }
        throw new AccessDeniedException("Apenas o autor, o Líder do grupo ou o administrador podem concluir a solicitação");
    }

    private void validarPermissaoLiderOuAdmin(Long grupoId, Usuario usuario) {
        if (isAdmin(usuario) || grupoAcessoService.isLider(grupoId, usuario.getId())) {
            return;
        }
        throw new AccessDeniedException("Apenas o Líder do grupo ou o administrador podem encerrar a solicitação");
    }

    private boolean isAutor(MaterialEmprestimoSolicitacao solicitacao, Usuario usuario) {
        return solicitacao.getUsuario() != null && solicitacao.getUsuario().getUsuario() != null
                && solicitacao.getUsuario().getUsuario().getId().equals(usuario.getId());
    }

    private void validarTipo(String tipo) {
        if (tipo == null || !TIPOS_VALIDOS.contains(tipo.trim().toLowerCase())) {
            throw new IllegalArgumentException("Tipo de solicitação inválido. Os tipos permitidos são: uso de material, financiamento ou compra de material catalogado");
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

    private Material buscarMaterial(Long id) {
        return materialRepository.findByIdAndIsAtivoTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Material não encontrado"));
    }

    private SolicitacaoResponse mapearParaResponse(MaterialEmprestimoSolicitacao s) {
        BigDecimal valor = !s.getItems().isEmpty() ? s.getItems().get(0).getValor() : null;
        Integer qtd = !s.getItems().isEmpty() ? s.getItems().get(0).getQuantidade() : null;
        Long materialId = !s.getItems().isEmpty() && s.getItems().get(0).getMaterial() != null
                ? s.getItems().get(0).getMaterial().getId() : null;
        String materialNome = !s.getItems().isEmpty() && s.getItems().get(0).getMaterial() != null
                ? s.getItems().get(0).getMaterial().getNome() : null;

        return new SolicitacaoResponse(
                s.getId(),
                s.getTitulo() != null ? s.getTitulo() : s.getJustificativa(),
                s.getDescricao() != null ? s.getDescricao() : s.getJustificativa(),
                s.getJustificativa(),
                s.getStatus(),
                s.isInterna(),
                s.isAprovada(),
                s.getProjeto() != null ? s.getProjeto().getId() : null,
                s.getProjeto() != null ? s.getProjeto().getTitulo() : null,
                s.getProjeto() != null ? s.getProjeto().getGrupo().getId() : null,
                s.getProjeto() != null ? s.getProjeto().getGrupo().getNome() : null,
                s.getUsuario() != null && s.getUsuario().getUsuario() != null ? s.getUsuario().getUsuario().getId() : null,
                s.getUsuario() != null && s.getUsuario().getUsuario() != null ? s.getUsuario().getUsuario().getNome() : null,
                materialId,
                materialNome,
                qtd,
                valor,
                s.getDthSolicitacao(),
                s.getDthResposta(),
                s.getAprovadorLider() != null ? s.getAprovadorLider().getNome() : null,
                s.getDthAprovacaoLider()
        );
    }
}
