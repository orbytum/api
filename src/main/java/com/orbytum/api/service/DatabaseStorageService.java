package com.orbytum.api.service;

import com.orbytum.api.configuration.StorageProperties;
import com.orbytum.api.models.entity.Documento;
import com.orbytum.api.models.enums.StorageProvider;
import com.orbytum.api.models.exceptions.ArquivoStorageException;
import com.orbytum.api.repository.DocumentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class DatabaseStorageService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseStorageService.class);

    private final DocumentoRepository documentoRepository;
    private final StorageProperties properties;

    public DatabaseStorageService(DocumentoRepository documentoRepository, StorageProperties properties) {
        this.documentoRepository = documentoRepository;
        this.properties = properties;
    }

    @Transactional
    public ArquivoArmazenado salvar(String chave, MultipartFile arquivo) {
        if (chave == null || chave.isBlank()) {
            throw new IllegalArgumentException("A chave do arquivo não pode ser nula ou vazia.");
        }
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("O arquivo não pode ser nulo ou vazio.");
        }
        if (arquivo.getSize() > properties.getMaxFileSizeBytes()) {
            throw new IllegalArgumentException(
                    "O arquivo excede o tamanho máximo permitido de "
                            + properties.getMaxFileSizeBytes() + " bytes.");
        }

        try {
            byte[] bytes = arquivo.getBytes();
            if (bytes.length > properties.getMaxFileSizeBytes()) {
                throw new IllegalArgumentException(
                        "O arquivo excede o tamanho máximo permitido de "
                                + properties.getMaxFileSizeBytes() + " bytes.");
            }
            String nome = resolverNome(arquivo);
            String contentType = resolverContentType(arquivo);

            Documento documento = Documento.builder()
                    .nome(nome)
                    .extensao(resolverExtensao(nome))
                    .contentType(contentType)
                    .tamanho((long) bytes.length)
                    .chave(chave)
                    .dthCriacao(LocalDateTime.now())
                    .documento(bytes)
                    .build();

            Documento salvo = documentoRepository.save(documento);
            logger.info("Arquivo '{}' salvo no PostgreSQL (documento id={}).", chave, salvo.getId());

            return new ArquivoArmazenado(
                    StorageProvider.DATABASE,
                    chave,
                    null,
                    nome,
                    contentType,
                    bytes.length,
                    salvo
            );
        } catch (IOException e) {
            logger.error("Falha ao ler o arquivo '{}' para salvar no PostgreSQL: {}", chave, e.getMessage(), e);
            throw new ArquivoStorageException("Falha ao ler o arquivo enviado: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] baixar(String chave) {
        Documento documento = buscarPorChave(chave);
        return documento.getDocumento();
    }

    @Transactional
    public void excluir(String chave) {
        if (chave == null || chave.isBlank()) {
            return;
        }
        documentoRepository.findByChave(chave).ifPresent(documentoRepository::delete);
    }

    private Documento buscarPorChave(String chave) {
        if (chave == null || chave.isBlank()) {
            throw new IllegalArgumentException("A chave do arquivo não pode ser nula ou vazia.");
        }
        return documentoRepository.findByChave(chave)
                .orElseThrow(() -> new ArquivoStorageException(
                        "Arquivo não encontrado no PostgreSQL para a chave: " + chave));
    }

    private String resolverNome(MultipartFile arquivo) {
        String original = arquivo.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "arquivo";
        }
        // Evita caminhos enviados pelo cliente.
        return original.replace("\\", "/").replaceAll(".*/", "");
    }

    private String resolverContentType(MultipartFile arquivo) {
        String contentType = arquivo.getContentType();
        return (contentType != null && !contentType.isBlank())
                ? contentType
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private String resolverExtensao(String nome) {
        int ponto = nome.lastIndexOf('.');
        if (ponto < 0 || ponto == nome.length() - 1) {
            return "";
        }
        return nome.substring(ponto + 1);
    }
}
