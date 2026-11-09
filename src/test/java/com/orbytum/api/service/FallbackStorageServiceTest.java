package com.orbytum.api.service;

import com.orbytum.api.configuration.StorageProperties;
import com.orbytum.api.models.entity.Documento;
import com.orbytum.api.models.enums.StorageProvider;
import com.orbytum.api.models.exceptions.S3StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FallbackStorageServiceTest {

    @Mock
    private ObjectProvider<S3StorageService> s3StorageProvider;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private DatabaseStorageService databaseStorageService;

    private StorageProperties properties;
    private FallbackStorageService fallbackStorageService;

    private final MockMultipartFile arquivo = new MockMultipartFile(
            "arquivo",
            "documento.pdf",
            "application/pdf",
            "%PDF-1.4".getBytes(StandardCharsets.UTF_8)
    );

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        fallbackStorageService = new FallbackStorageService(
                properties, s3StorageProvider, databaseStorageService);
    }

    private ArquivoArmazenado resultado(StorageProvider provedor) {
        return new ArquivoArmazenado(
                provedor,
                "chave/teste.pdf",
                provedor == StorageProvider.S3 ? "https://s3/chave/teste.pdf" : null,
                "documento.pdf",
                "application/pdf",
                arquivo.getSize(),
                provedor == StorageProvider.DATABASE
                        ? Documento.builder()
                                .id(1L)
                                .nome("documento.pdf")
                                .extensao("pdf")
                                .documento(new byte[0])
                                .build()
                        : null
        );
    }

    @Test
    void providerDatabaseNuncaChamaS3() {
        properties.setProvider(StorageProvider.DATABASE);
        when(databaseStorageService.salvar(anyString(), any())).thenReturn(resultado(StorageProvider.DATABASE));

        ArquivoArmazenado resultado = fallbackStorageService.salvar("chave/teste.pdf", arquivo);

        assertEquals(StorageProvider.DATABASE, resultado.provedor());
        verifyNoInteractions(s3StorageProvider, s3StorageService);
    }

    @Test
    void providerAutoSemS3SalvaNoBanco() {
        properties.setProvider(StorageProvider.AUTO);
        when(s3StorageProvider.getIfAvailable()).thenReturn(null);
        when(databaseStorageService.salvar(anyString(), any())).thenReturn(resultado(StorageProvider.DATABASE));

        ArquivoArmazenado resultado = fallbackStorageService.salvar("chave/teste.pdf", arquivo);

        assertEquals(StorageProvider.DATABASE, resultado.provedor());
        verifyNoInteractions(s3StorageService);
    }

    @Test
    void providerAutoComS3ConfiguradoUsaS3() {
        properties.setProvider(StorageProvider.AUTO);
        when(s3StorageProvider.getIfAvailable()).thenReturn(s3StorageService);
        when(s3StorageService.isConfigured()).thenReturn(true);
        when(s3StorageService.uploadFile(anyString(), any(MockMultipartFile.class)))
                .thenReturn("https://s3/chave/teste.pdf");

        ArquivoArmazenado resultado = fallbackStorageService.salvar("chave/teste.pdf", arquivo);

        assertEquals(StorageProvider.S3, resultado.provedor());
        verifyNoInteractions(databaseStorageService);
    }

    @Test
    void erroNoS3AcionaFallbackParaBanco() {
        properties.setProvider(StorageProvider.AUTO);
        when(s3StorageProvider.getIfAvailable()).thenReturn(s3StorageService);
        when(s3StorageService.isConfigured()).thenReturn(true);
        when(s3StorageService.uploadFile(anyString(), any(MockMultipartFile.class)))
                .thenThrow(new S3StorageException("S3 indisponível"));
        when(databaseStorageService.salvar(anyString(), any())).thenReturn(resultado(StorageProvider.DATABASE));

        ArquivoArmazenado resultado = fallbackStorageService.salvar("chave/teste.pdf", arquivo);

        assertEquals(StorageProvider.DATABASE, resultado.provedor());
    }

    @Test
    void fallbackDesabilitadoPropagaErroDoS3() {
        properties.setProvider(StorageProvider.S3);
        properties.setFallbackEnabled(false);
        when(s3StorageProvider.getIfAvailable()).thenReturn(s3StorageService);
        when(s3StorageService.isConfigured()).thenReturn(true);
        when(s3StorageService.uploadFile(anyString(), any(MockMultipartFile.class)))
                .thenThrow(new S3StorageException("S3 indisponível"));

        assertThrows(S3StorageException.class,
                () -> fallbackStorageService.salvar("chave/teste.pdf", arquivo));
        verifyNoInteractions(databaseStorageService);
    }
}
