package com.orbytum.api.service;

import com.orbytum.api.configuration.StorageProperties;
import com.orbytum.api.models.dto.request.CreatePublicacaoRequest;
import com.orbytum.api.models.dto.request.EditarPublicacaoRequest;
import com.orbytum.api.models.dto.response.PublicacaoPaginadaResponse;
import com.orbytum.api.models.dto.response.PublicacaoResponse;
import com.orbytum.api.models.entity.Documento;
import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.entity.Publicacao;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.StorageProvider;
import com.orbytum.api.models.exceptions.ArquivoStorageException;
import com.orbytum.api.models.exceptions.ProjetoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.PublicacaoNaoEncontradaErro;
import com.orbytum.api.models.exceptions.SemPermissaoNoGrupoErro;
import com.orbytum.api.models.exceptions.UsuarioNaoEncontradoErro;
import com.orbytum.api.repository.ProjetoRepository;
import com.orbytum.api.repository.PublicacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class PublicacaoService {

    private static final Logger logger = LoggerFactory.getLogger(PublicacaoService.class);

    private final PublicacaoRepository publicacaoRepository;
    private final ProjetoRepository projetoRepository;
    private final ArquivoStorage arquivoStorage;
    private final StorageProperties storageProperties;
    private final UsuarioService usuarioService;
    private final GrupoAcessoService grupoAcessoService;

    public PublicacaoService(
            PublicacaoRepository publicacaoRepository,
            ProjetoRepository projetoRepository,
            ArquivoStorage arquivoStorage,
            StorageProperties storageProperties,
            UsuarioService usuarioService,
            GrupoAcessoService grupoAcessoService
    ) {
        this.publicacaoRepository = publicacaoRepository;
        this.projetoRepository = projetoRepository;
        this.arquivoStorage = arquivoStorage;
        this.storageProperties = storageProperties;
        this.usuarioService = usuarioService;
        this.grupoAcessoService = grupoAcessoService;
    }

    @Transactional
    public PublicacaoResponse criarPublicacao(CreatePublicacaoRequest request, String emailLogado) {
        if (request == null) {
            throw new IllegalArgumentException("A requisição não pode ser nula.");
        }
        if (request.titulo() == null || request.titulo().isBlank()) {
            throw new IllegalArgumentException("O título da publicação é obrigatório.");
        }
        if (request.descricao() == null || request.descricao().isBlank()) {
            throw new IllegalArgumentException("A descrição da publicação é obrigatória.");
        }
        if (request.projetoId() == null) {
            throw new IllegalArgumentException("O ID do projeto vinculado é obrigatório.");
        }

        validarArquivoPdf(request.arquivo());

        Projeto projeto = buscarProjeto(request.projetoId());
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarGestorDoProjeto(projeto, solicitante);

        UUID uuid = UUID.randomUUID();
        String chave = "publicacoes/" + uuid + ".pdf";

        logger.info("Iniciando armazenamento da publicação com a chave '{}'.", chave);
        ArquivoArmazenado arquivo = arquivoStorage.salvar(chave, request.arquivo());
        agendarExclusaoS3EmRollback(arquivo);

        Publicacao publicacao = Publicacao.builder()
                .titulo(request.titulo().trim())
                .descricao(request.descricao().trim())
                .projeto(projeto)
                .uuid(uuid)
                .url(resolverUrl(arquivo, uuid))
                .storageProvider(arquivo.provedor())
                .storageKey(arquivo.chave())
                .documento(arquivo.documento())
                .dthRegistro(LocalDateTime.now())
                .isAtivo(true)
                .build();

        Publicacao salva = publicacaoRepository.save(publicacao);
        logger.info("Publicação salva com sucesso com ID {} e UUID {}.", salva.getId(), salva.getUuid());

        return PublicacaoResponse.fromEntity(salva);
    }

    @Transactional
    public PublicacaoResponse atualizarPublicacao(Long id, EditarPublicacaoRequest request, String emailLogado) {
        if (request == null) {
            throw new IllegalArgumentException("A requisição não pode ser nula.");
        }
        if (request.titulo() == null || request.titulo().isBlank()) {
            throw new IllegalArgumentException("O título da publicação é obrigatório.");
        }
        if (request.descricao() == null || request.descricao().isBlank()) {
            throw new IllegalArgumentException("A descrição da publicação é obrigatória.");
        }

        Publicacao publicacao = buscarPublicacao(id);
        Usuario solicitante = usuarioAutenticado(emailLogado);

        validarGestorDoProjeto(publicacao.getProjeto(), solicitante);

        Projeto projetoDestino = publicacao.getProjeto();
        if (request.projetoId() != null && !request.projetoId().equals(projetoDestino.getId())) {
            projetoDestino = buscarProjeto(request.projetoId());
            validarGestorDoProjeto(projetoDestino, solicitante);
            publicacao.setProjeto(projetoDestino);
        }

        publicacao.setTitulo(request.titulo().trim());
        publicacao.setDescricao(request.descricao().trim());

        if (request.arquivo() != null && !request.arquivo().isEmpty()) {
            validarArquivoPdf(request.arquivo());

            StorageProvider provedorAnterior = publicacao.getStorageProvider();
            String chaveAnterior = publicacao.getStorageKey();

            String chave = "publicacoes/" + publicacao.getUuid() + "-" + UUID.randomUUID() + ".pdf";
            ArquivoArmazenado arquivo = arquivoStorage.salvar(chave, request.arquivo());
            agendarExclusaoS3EmRollback(arquivo);

            publicacao.setUrl(resolverUrl(arquivo, publicacao.getUuid()));
            publicacao.setStorageProvider(arquivo.provedor());
            publicacao.setStorageKey(arquivo.chave());
            publicacao.setDocumento(arquivo.documento());

            agendarExclusaoArquivoAnterior(provedorAnterior, chaveAnterior);
        }

        return PublicacaoResponse.fromEntity(publicacaoRepository.save(publicacao));
    }

    @Transactional(readOnly = true)
    public PublicacaoPaginadaResponse listarPublicacoes(
            String titulo,
            LocalDate dataInicio,
            LocalDate dataFim,
            Long projetoId,
            Long grupoId,
            int page,
            int size,
            String emailLogado
    ) {
        Usuario solicitante = usuarioAutenticado(emailLogado);

        if (projetoId != null) {
            Projeto projeto = buscarProjeto(projetoId);
            validarLeituraDoProjeto(projeto, solicitante);
        } else if (grupoId != null) {
            if (!isAdmin(solicitante)) {
                grupoAcessoService.validarMembro(grupoId, solicitante.getId());
            }
        } else {
            throw new IllegalArgumentException("Informe o grupoId ou o projetoId para listar publicações.");
        }

        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "dthRegistro"));

        LocalDateTime inicio = (dataInicio != null) ? dataInicio.atStartOfDay() : null;
        LocalDateTime fim = (dataFim != null) ? dataFim.atTime(LocalTime.MAX) : null;
        String normalizedTitulo = (titulo != null && !titulo.isBlank()) ? titulo.trim() : null;

        Page<Publicacao> publicacoesPage = publicacaoRepository.filtrarPublicacoes(
                normalizedTitulo,
                inicio,
                fim,
                projetoId,
                grupoId,
                pageable
        );

        List<PublicacaoResponse> items = publicacoesPage.getContent().stream()
                .map(PublicacaoResponse::fromEntity)
                .toList();

        return new PublicacaoPaginadaResponse(
                items,
                publicacoesPage.getTotalElements(),
                publicacoesPage.getTotalPages(),
                page,
                pageSize
        );
    }

    @Transactional(readOnly = true)
    public PublicacaoResponse buscarPorId(Long id, String emailLogado) {
        Publicacao publicacao = buscarPublicacao(id);
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarLeituraDoProjeto(publicacao.getProjeto(), solicitante);
        return PublicacaoResponse.fromEntity(publicacao);
    }

    @Transactional(readOnly = true)
    public PublicacaoResponse buscarPorUuid(UUID uuid, String emailLogado) {
        Publicacao publicacao = buscarPublicacaoPorUuid(uuid);
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarLeituraDoProjeto(publicacao.getProjeto(), solicitante);
        return PublicacaoResponse.fromEntity(publicacao);
    }

    @Transactional(readOnly = true)
    public ArquivoDownload baixarArquivo(UUID uuid, String emailLogado) {
        Publicacao publicacao = buscarPublicacaoPorUuid(uuid);
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarLeituraDoProjeto(publicacao.getProjeto(), solicitante);

        StorageProvider provedor = publicacao.getStorageProvider();
        if (provedor == null) {
            provedor = publicacao.getDocumento() != null
                    ? StorageProvider.DATABASE
                    : StorageProvider.S3;
        }

        if (provedor == StorageProvider.DATABASE) {
            Documento documento = publicacao.getDocumento();
            if (documento == null || documento.getDocumento() == null) {
                throw new ArquivoStorageException("Arquivo da publicação não encontrado no PostgreSQL.");
            }
            return new ArquivoDownload(
                    documento.getDocumento(),
                    documento.getNome() != null && !documento.getNome().isBlank()
                            ? documento.getNome()
                            : "publicacao.pdf",
                    documento.getContentType() != null && !documento.getContentType().isBlank()
                            ? documento.getContentType()
                            : MediaType.APPLICATION_PDF_VALUE
            );
        }

        String chave = publicacao.getStorageKey();
        if (chave == null || chave.isBlank()) {
            chave = extrairChaveS3(publicacao.getUrl());
        }
        if (chave == null || chave.isBlank()) {
            throw new ArquivoStorageException("Arquivo da publicação não encontrado.");
        }

        byte[] bytes = arquivoStorage.baixar(chave, StorageProvider.S3);
        return new ArquivoDownload(bytes, "publicacao.pdf", MediaType.APPLICATION_PDF_VALUE);
    }

    @Transactional
    public void inativarPublicacao(Long id, String emailLogado) {
        Publicacao publicacao = buscarPublicacao(id);
        Usuario solicitante = usuarioAutenticado(emailLogado);
        validarGestorDoProjeto(publicacao.getProjeto(), solicitante);

        publicacao.setAtivo(false);
        publicacaoRepository.save(publicacao);
        logger.info("Publicação com ID {} foi inativada com sucesso.", id);
    }

    private Publicacao buscarPublicacao(Long id) {
        return publicacaoRepository.findByIdAndIsAtivoTrue(id)
                .orElseThrow(() -> new PublicacaoNaoEncontradaErro("Publicação ativa não encontrada com o ID: " + id));
    }

    private Publicacao buscarPublicacaoPorUuid(UUID uuid) {
        return publicacaoRepository.findByUuidAndIsAtivoTrue(uuid)
                .orElseThrow(() -> new PublicacaoNaoEncontradaErro("Publicação não encontrada com o UUID: " + uuid));
    }

    private Projeto buscarProjeto(Long projetoId) {
        return projetoRepository.findById(projetoId)
                .orElseThrow(() -> new ProjetoNaoEncontradoErro("Projeto não encontrado com o ID: " + projetoId));
    }

    private Usuario usuarioAutenticado(String emailLogado) {
        return usuarioService.findByEmail(emailLogado)
                .orElseThrow(() -> new UsuarioNaoEncontradoErro("Usuário autenticado não encontrado"));
    }

    private boolean isAdmin(Usuario usuario) {
        return usuario.getCredenciaisLogin() != null
                && usuario.getCredenciaisLogin().getAccessLevel() == AccessLevel.ADMIN;
    }

    private void validarLeituraDoProjeto(Projeto projeto, Usuario solicitante) {
        if (isAdmin(solicitante)) {
            return;
        }
        if (projeto == null || projeto.getGrupo() == null) {
            throw new SemPermissaoNoGrupoErro("Você não possui acesso às publicações deste projeto");
        }
        grupoAcessoService.validarMembro(projeto.getGrupo().getId(), solicitante.getId());
    }

    private void validarGestorDoProjeto(Projeto projeto, Usuario solicitante) {
        if (isAdmin(solicitante)) {
            return;
        }
        if (projeto == null || !grupoAcessoService.podeGerenciarProjeto(projeto.getId(), solicitante.getId())) {
            throw new SemPermissaoNoGrupoErro(
                    "Esta operação exige permissão de Líder do grupo ou do projeto");
        }
    }

    private void validarArquivoPdf(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("O arquivo PDF da publicação é obrigatório.");
        }

        String originalFilename = arquivo.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("A publicação deve ser exclusivamente no formato PDF.");
        }

        String contentType = arquivo.getContentType();
        if (contentType != null && !contentType.equalsIgnoreCase("application/pdf")
                && !contentType.equalsIgnoreCase("application/x-pdf")) {
            throw new IllegalArgumentException("A publicação deve ser exclusivamente no formato PDF.");
        }
    }

    private String resolverUrl(ArquivoArmazenado arquivo, UUID uuid) {
        if (arquivo.provedor() == StorageProvider.S3) {
            return arquivo.url();
        }
        return baseUrl() + "/publicacoes/uuid/" + uuid + "/arquivo";
    }

    private String baseUrl() {
        String base = storageProperties.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            return "http://localhost:8080";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private void agendarExclusaoS3EmRollback(ArquivoArmazenado arquivo) {
        if (arquivo == null || arquivo.provedor() != StorageProvider.S3) {
            return;
        }
        String chave = arquivo.chave();
        if (chave == null || chave.isBlank()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    return;
                }
                try {
                    arquivoStorage.excluir(chave, StorageProvider.S3);
                } catch (RuntimeException ex) {
                    logger.warn("Falha ao remover do S3 o arquivo '{}' após rollback: {}",
                            chave, ex.getMessage());
                }
            }
        });
    }

    private void agendarExclusaoArquivoAnterior(StorageProvider provedor, String chave) {
        if (provedor != StorageProvider.S3 || chave == null || chave.isBlank()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            arquivoStorage.excluir(chave, StorageProvider.S3);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    arquivoStorage.excluir(chave, StorageProvider.S3);
                } catch (RuntimeException ex) {
                    logger.warn("Falha ao excluir o arquivo antigo '{}' do S3: {}", chave, ex.getMessage());
                }
            }
        });
    }

    private String extrairChaveS3(String url) {
        if (url == null || url.isBlank() || !url.startsWith("http")) {
            return null;
        }
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (path == null || path.isBlank() || "/".equals(path)) {
                return null;
            }
            String chave = path.startsWith("/") ? path.substring(1) : path;
            String host = uri.getHost();
            if (host != null && (host.startsWith("s3.") || host.startsWith("s3-"))) {
                int barra = chave.indexOf('/');
                if (barra >= 0 && barra < chave.length() - 1) {
                    chave = chave.substring(barra + 1);
                }
            }
            return URLDecoder.decode(chave, StandardCharsets.UTF_8);
        } catch (RuntimeException ex) {
            logger.warn("Não foi possível extrair a chave S3 da URL '{}': {}", url, ex.getMessage());
            return null;
        }
    }
}
