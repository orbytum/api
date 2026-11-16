package com.orbytum.api.repository;

import com.orbytum.api.models.entity.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    Optional<Documento> findByChave(String chave);

    @Query("SELECT COUNT(vinculo) FROM EditalXDocumento vinculo WHERE vinculo.documento.id = :documentoId")
    long countVinculosEdital(@Param("documentoId") Long documentoId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Publicacao p WHERE p.documento.id = :documentoId")
    boolean existsPublicacaoByDocumentoId(@Param("documentoId") Long documentoId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Evento e WHERE e.ataDocumento.id = :documentoId")
    boolean existsEventoByDocumentoId(@Param("documentoId") Long documentoId);
}
