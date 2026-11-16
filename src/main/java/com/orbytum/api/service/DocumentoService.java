package com.orbytum.api.service;

import com.orbytum.api.models.entity.Documento;
import com.orbytum.api.models.exceptions.ArquivoStorageException;
import com.orbytum.api.models.exceptions.DocumentoNaoEncontradoErro;
import com.orbytum.api.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final DatabaseStorageService databaseStorageService;

    @Transactional
    public Documento salvar(MultipartFile arquivo, Long editalId) {
        String chave = gerarChave(editalId, arquivo != null ? arquivo.getOriginalFilename() : null);
        ArquivoArmazenado armazenado = databaseStorageService.salvar(chave, arquivo);
        if (armazenado == null || armazenado.documento() == null) {
            throw new ArquivoStorageException("Não foi possível armazenar o documento do edital.");
        }
        return armazenado.documento();
    }

    @Transactional(readOnly = true)
    public Documento buscar(Long documentoId) {
        return documentoRepository.findById(documentoId)
                .orElseThrow(() -> new DocumentoNaoEncontradoErro(
                        "Documento não encontrado com ID: " + documentoId));
    }

    @Transactional(readOnly = true)
    public ArquivoDownload baixar(Documento documento) {
        if (documento == null || documento.getDocumento() == null) {
            throw new ArquivoStorageException("Arquivo do documento não encontrado.");
        }

        String nome = documento.getNome() != null && !documento.getNome().isBlank()
                ? documento.getNome()
                : "documento";
        String contentType = documento.getContentType() != null && !documento.getContentType().isBlank()
                ? documento.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return new ArquivoDownload(documento.getDocumento(), nome, contentType);
    }

    @Transactional
    public void excluirSeOrfao(Documento documento) {
        if (documento == null || documento.getId() == null) {
            return;
        }
        boolean vinculadoAEdital = documentoRepository.countVinculosEdital(documento.getId()) > 0;
        boolean vinculadoAPublicacao = documentoRepository.existsPublicacaoByDocumentoId(documento.getId());
        boolean vinculadoAEvento = documentoRepository.existsEventoByDocumentoId(documento.getId());

        if (!vinculadoAEdital && !vinculadoAPublicacao && !vinculadoAEvento) {
            documentoRepository.delete(documento);
        }
    }

    private String gerarChave(Long editalId, String nomeOriginal) {
        String nome = nomeOriginal != null && !nomeOriginal.isBlank()
                ? nomeOriginal.replace("\\", "/").replaceAll(".*/", "")
                : "documento";
        nome = nome.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (nome.length() > 120) {
            nome = nome.substring(nome.length() - 120);
        }
        return "editais/" + editalId + "/" + UUID.randomUUID() + "-" + nome;
    }
}
