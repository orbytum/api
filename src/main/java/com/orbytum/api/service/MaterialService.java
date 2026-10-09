package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.CriarMaterialRequest;
import com.orbytum.api.models.dto.request.EditarMaterialRequest;
import com.orbytum.api.models.dto.response.MaterialResponse;
import com.orbytum.api.models.entity.Material;
import com.orbytum.api.models.entity.MaterialEmprestimo;
import com.orbytum.api.models.entity.MaterialEmprestimoSolicitacao;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.MaterialStatus;
import com.orbytum.api.models.enums.NivelMembro;
import com.orbytum.api.models.enums.SolicitacaoStatus;
import com.orbytum.api.repository.GrupoXUsuarioRepository;
import com.orbytum.api.repository.MaterialEmprestimoRepository;
import com.orbytum.api.repository.MaterialRepository;
import com.orbytum.api.repository.SolicitacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialEmprestimoRepository materialEmprestimoRepository;
    private final SolicitacaoRepository solicitacaoRepository;
    private final GrupoXUsuarioRepository grupoXUsuarioRepository;
    private final CredenciaisLoginService credenciaisLoginService;

    public List<MaterialResponse> listar(String emailLogado) {
        validarPermissao(emailLogado);
        return materialRepository.findAllByIsAtivoTrue().stream()
                .map(this::mapearParaResponse)
                .toList();
    }

    public MaterialResponse buscarPorId(Long id, String emailLogado) {
        validarPermissao(emailLogado);
        return mapearParaResponse(buscarMaterial(id));
    }

    @Transactional
    public MaterialResponse criar(CriarMaterialRequest request, String emailLogado) {
        validarPermissao(emailLogado);

        Material material = Material.builder()
                .nome(request.nome())
                .descricao(request.descricao())
                .identificador(request.identificador())
                .quantidade(request.quantidade())
                .localizacao(request.localizacao())
                .status(request.status() != null ? request.status() : MaterialStatus.DISPONIVEL)
                .dthCompra(request.dthCompra())
                .dthRegistro(LocalDateTime.now())
                .isAtivo(true)
                .build();

        return mapearParaResponse(materialRepository.save(material));
    }

    @Transactional
    public MaterialResponse atualizar(Long id, EditarMaterialRequest request, String emailLogado) {
        validarPermissao(emailLogado);

        Material material = buscarMaterial(id);
        material.setNome(request.nome());
        material.setDescricao(request.descricao());
        material.setIdentificador(request.identificador());
        material.setQuantidade(request.quantidade());
        material.setLocalizacao(request.localizacao());
        material.setDthCompra(request.dthCompra());
        material.setStatus(request.status());
        material.setAtivo(request.isAtivo());

        return mapearParaResponse(materialRepository.save(material));
    }

    @Transactional
    public void remover(Long id, String emailLogado) {
        validarPermissao(emailLogado);

        Material material = buscarMaterial(id);
        material.setAtivo(false);
        materialRepository.save(material);
    }

    private void validarPermissao(String emailLogado) {
        boolean isAdmin = credenciaisLoginService.findByEmail(emailLogado)
                .map(c -> c.getAccessLevel() == AccessLevel.ADMIN)
                .orElse(false);

        if (isAdmin) {
            return;
        }

        boolean isLiderOuCoordenador = grupoXUsuarioRepository
                .findAllByUsuarioEmailAndIsAtivoTrueAndGrupoIsAtivoTrue(emailLogado)
                .stream()
                .anyMatch(v -> v.getNivel() != null && v.getNivel().isPeloMenos(NivelMembro.COORDENADOR));

        if (!isLiderOuCoordenador) {
            throw new AccessDeniedException("Apenas Líderes ou Coordenadores podem gerenciar materiais");
        }
    }

    private Material buscarMaterial(Long id) {
        return materialRepository.findByIdAndIsAtivoTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Material não encontrado"));
    }

    private MaterialResponse mapearParaResponse(Material material) {
        Integer quantidadeEmprestada = materialEmprestimoRepository.somarQuantidadeEmprestada(material.getId());
        if (quantidadeEmprestada == null) {
            quantidadeEmprestada = 0;
        }
        int quantidadeDisponivel = material.getQuantidade() - quantidadeEmprestada;

        MaterialStatus statusEfetivo = material.getStatus();
        if (material.getStatus() != MaterialStatus.INDISPONIVEL) {
            statusEfetivo = quantidadeDisponivel <= 0 ? MaterialStatus.EMPRESTADO : MaterialStatus.DISPONIVEL;
        }

        List<MaterialEmprestimoSolicitacao> solicitacoesAtivas =
                solicitacaoRepository.findByMaterialIdAndStatus(material.getId(), SolicitacaoStatus.EM_ANDAMENTO);

        String usuarioEmUsoNome = null;
        if (!solicitacoesAtivas.isEmpty()
                && solicitacoesAtivas.get(0).getUsuario() != null
                && solicitacoesAtivas.get(0).getUsuario().getUsuario() != null) {
            usuarioEmUsoNome = solicitacoesAtivas.get(0).getUsuario().getUsuario().getNome();
        }

        LocalDateTime dthDevolucao = materialEmprestimoRepository
                .findFirstByMaterialIdAndIsDevolvidoFalseOrderByDthInicioDesc(material.getId())
                .map(MaterialEmprestimo::getDthFim)
                .orElse(null);

        return new MaterialResponse(
                material.getId(),
                material.getNome(),
                material.getDescricao(),
                material.getIdentificador(),
                material.getQuantidade(),
                material.getLocalizacao(),
                statusEfetivo,
                material.getDthCompra(),
                material.getDthRegistro(),
                material.isAtivo(),
                quantidadeEmprestada,
                quantidadeDisponivel,
                usuarioEmUsoNome,
                dthDevolucao
        );
    }
}
