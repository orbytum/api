package com.orbytum.api.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.models.entity.Grupo;
import com.orbytum.api.models.entity.Usuario;
import com.orbytum.api.repository.GrupoRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class GrupoService {

    private final GrupoRepository grupoRepository;
    private final GrupoXUsuarioService grupoXUsuarioService;

    public boolean existsByNome(String nome) {
        return grupoRepository.existsByNome(nome);
    }

    @Transactional
    public Grupo save(Grupo grupo) {
        return grupoRepository.save(grupo);
    }

    public Optional<Grupo> findById(Long id) {
        return grupoRepository.findById(id);
    }

    public List<Grupo> findAllAtivos() {
        return grupoRepository.findAllByIsAtivoTrue();
    }

    public List<Grupo> findAllByCriador(Usuario criador) {
        return grupoRepository.findAllByCriadorAndIsAtivoTrue(criador);
    }

    public Page<Grupo> listarGrupos(CredenciaisLogin adminLogado, int page, int size, String nome, String usuario) {
        Usuario criador = adminLogado.getUsuario();

        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.ASC, "id"));

        String normalizedNome = (nome != null && !nome.isBlank()) ? nome.trim() : "";
        String normalizedUsuario = (usuario != null && !usuario.isBlank()) ? usuario.trim() : "";

        return grupoRepository.filtrarGrupos(criador, normalizedNome, normalizedUsuario, pageable);
    }
}
