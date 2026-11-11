package com.orbytum.api.service;

import com.orbytum.api.configuration.StorageProperties;
import com.orbytum.api.models.enums.StorageProvider;
import com.orbytum.api.models.exceptions.S3StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FallbackStorageService implements ArquivoStorage {

    private static final Logger logger = LoggerFactory.getLogger(FallbackStorageService.class);

    private final StorageProperties properties;
    private final ObjectProvider<S3StorageService> s3StorageProvider;
    private final DatabaseStorageService databaseStorage;

    public FallbackStorageService(
            StorageProperties properties,
            ObjectProvider<S3StorageService> s3StorageProvider,
            DatabaseStorageService databaseStorage
    ) {
        this.properties = properties;
        this.s3StorageProvider = s3StorageProvider;
        this.databaseStorage = databaseStorage;
    }

    @Override
    public ArquivoArmazenado salvar(String chave, MultipartFile arquivo) {
        StorageProvider provider = properties.getProvider();

        if (provider == StorageProvider.DATABASE) {
            return databaseStorage.salvar(chave, arquivo);
        }

        if (provider == StorageProvider.AUTO && !s3Disponivel()) {
            logger.info("S3 não configurado. Salvando arquivo '{}' no PostgreSQL.", chave);
            return databaseStorage.salvar(chave, arquivo);
        }

        try {
            return salvarNoS3(chave, arquivo);
        } catch (S3StorageException ex) {
            if (!properties.isFallbackEnabled()) {
                throw ex;
            }
            logger.warn("Falha no S3 ao salvar '{}'. Usando fallback no PostgreSQL: {}",
                    chave, ex.getMessage());
            return databaseStorage.salvar(chave, arquivo);
        }
    }

    @Override
    public byte[] baixar(String chave, StorageProvider provedor) {
        if (provedor == StorageProvider.DATABASE) {
            return databaseStorage.baixar(chave);
        }
        return s3Storage().downloadFile(chave);
    }

    @Override
    public void excluir(String chave, StorageProvider provedor) {
        if (chave == null || chave.isBlank()) {
            return;
        }
        if (provedor == StorageProvider.DATABASE) {
            databaseStorage.excluir(chave);
            return;
        }
        S3StorageService s3 = s3StorageProvider.getIfAvailable();
        if (s3 == null) {
            logger.warn("S3 não configurado. Ignorando exclusão do arquivo '{}'.", chave);
            return;
        }
        s3.deleteFile(chave);
    }

    @Override
    public boolean disponivel(StorageProvider provedor) {
        if (provedor == StorageProvider.DATABASE) {
            return true;
        }
        return s3Disponivel();
    }

    private ArquivoArmazenado salvarNoS3(String chave, MultipartFile arquivo) {
        S3StorageService s3 = s3Storage();
        String url = s3.uploadFile(chave, arquivo);

        String nome = resolverNome(arquivo);
        String contentType = (arquivo.getContentType() != null && !arquivo.getContentType().isBlank())
                ? arquivo.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return new ArquivoArmazenado(
                StorageProvider.S3,
                chave,
                url,
                nome,
                contentType,
                arquivo.getSize(),
                null
        );
    }

    private S3StorageService s3Storage() {
        S3StorageService s3 = s3StorageProvider.getIfAvailable();
        if (s3 == null || !s3.isConfigured()) {
            throw new S3StorageException("O armazenamento S3 não está configurado.");
        }
        return s3;
    }

    private boolean s3Disponivel() {
        S3StorageService s3 = s3StorageProvider.getIfAvailable();
        return s3 != null && s3.isConfigured();
    }

    private String resolverNome(MultipartFile arquivo) {
        String original = arquivo.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "arquivo";
        }
        return original.replace("\\", "/").replaceAll(".*/", "");
    }
}
