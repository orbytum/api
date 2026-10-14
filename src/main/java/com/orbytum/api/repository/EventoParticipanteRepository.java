package com.orbytum.api.repository;

import com.orbytum.api.models.entity.EventoParticipante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventoParticipanteRepository extends JpaRepository<EventoParticipante, Long> {

    List<EventoParticipante> findByEventoId(Long eventoId);

    Optional<EventoParticipante> findByEventoIdAndUsuarioId(Long eventoId, Long usuarioId);

    void deleteAllByEventoId(Long eventoId);
}
