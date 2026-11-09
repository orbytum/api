package com.orbytum.api.service;

import com.orbytum.api.models.enums.StorageProvider;
import org.springframework.web.multipart.MultipartFile;

public interface ArquivoStorage {

    ArquivoArmazenado salvar(String chave, MultipartFile arquivo);

    byte[] baixar(String chave, StorageProvider provedor);

    void excluir(String chave, StorageProvider provedor);

    boolean disponivel(StorageProvider provedor);
}
