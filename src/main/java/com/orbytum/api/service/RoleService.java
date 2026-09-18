package com.orbytum.api.service;

import com.orbytum.api.models.dto.response.RoleResponse;
import com.orbytum.api.models.entity.Role;
import com.orbytum.api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    public List<RoleResponse> listarRoles() {
        return roleRepository.findAll().stream()
                .map(r -> new RoleResponse(r.getId(), r.getNome(), r.isLider()))
                .toList();
    }

    public Optional<Role> findById(Long id) {
        return roleRepository.findById(id);
    }

    public Role obterOuCriarRolePadrao() {
        return roleRepository.findByNomeIgnoreCase("Membro")
                .orElseGet(() -> roleRepository.save(new Role("Membro", List.of(), false)));
    }

    public Role obterOuCriarRoleLider() {
        return roleRepository.findFirstByIsLiderTrue()
                .orElseGet(() -> roleRepository.save(new Role("Líder", List.of(), true)));
    }
}
