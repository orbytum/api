package com.orbytum.api.service;

import com.orbytum.api.models.dto.request.CreatePublicacaoRequest;
import com.orbytum.api.models.dto.response.PublicacaoPaginadaResponse;
import com.orbytum.api.models.dto.response.PublicacaoResponse;
import com.orbytum.api.models.entity.Projeto;
import com.orbytum.api.models.entity.Publicacao;
import com.orbytum.api.models.exceptions.ProjetoNaoEncontradoErro;
import com.orbytum.api.models.exceptions.PublicacaoNaoEncontradaErro;
import com.orbytum.api.repository.ProjetoRepository;
import com.orbytum.api.repository.PublicacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final S3StorageService s3StorageService;

    public PublicacaoService(
            PublicacaoRepository publicacaoRepository,
            ProjetoRepository projetoRepository,
            S3StorageService s3StorageService
    ) {
        this.publicacaoRepository = publicacaoRepository;
        this.projetoRepository = projetoRepository;
        this.s3StorageService = s3StorageService;
    }

    @Transactional
    public PublicacaoResponse criarPublicacao(CreatePublicacaoRequest request) {
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

        Projeto projeto = projetoRepository.findById(request.projetoId())
                .orElseThrow(() -> new ProjetoNaoEncontradoErro("Projeto não encontrado com o ID: " + request.projetoId()));

        UUID uuid = UUID.randomUUID();
        String s3Key = "publicacoes/" + uuid + ".pdf";

        logger.info("Iniciando upload da publicação para o S3 com a chave '{}'.", s3Key);
        String s3Url = s3StorageService.uploadFile(s3Key, request.arquivo());

        Publicacao publicacao = Publicacao.builder()
                .titulo(request.titulo().trim())
                .descricao(request.descricao().trim())
                .projeto(projeto)
                .uuid(uuid)
                .url(s3Url)
                .dthRegistro(LocalDateTime.now())
                .isAtivo(true)
                .build();

        Publicacao salva = publicacaoRepository.save(publicacao);
        logger.info("Publicação salva com sucesso com ID {} e UUID {}.", salva.getId(), salva.getUuid());

        return PublicacaoResponse.fromEntity(salva);
    }

    @Transactional(readOnly = true)
    public PublicacaoPaginadaResponse listarPublicacoes(
            String titulo,
            LocalDate dataInicio,
            LocalDate dataFim,
            Long projetoId,
            int page,
            int size
    ) {
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
    public PublicacaoResponse buscarPorId(Long id) {
        return publicacaoRepository.findByIdAndIsAtivoTrue(id)
                .map(PublicacaoResponse::fromEntity)
                .orElseThrow(() -> new PublicacaoNaoEncontradaErro("Publicação não encontrada com o ID: " + id));
    }

    @Transactional(readOnly = true)
    public PublicacaoResponse buscarPorUuid(UUID uuid) {
        return publicacaoRepository.findByUuidAndIsAtivoTrue(uuid)
                .map(PublicacaoResponse::fromEntity)
                .orElseThrow(() -> new PublicacaoNaoEncontradaErro("Publicação não encontrada com o UUID: " + uuid));
    }

    @Transactional
    public void inativarPublicacao(Long id) {
        Publicacao publicacao = publicacaoRepository.findByIdAndIsAtivoTrue(id)
                .orElseThrow(() -> new PublicacaoNaoEncontradaErro("Publicação ativa não encontrada com o ID: " + id));

        publicacao.setAtivo(false);
        publicacaoRepository.save(publicacao);
        logger.info("Publicação com ID {} foi inativada com sucesso.", id);
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
}
