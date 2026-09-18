package com.orbytum.api.controller;

import com.orbytum.api.models.dto.response.RoleResponse;
import com.orbytum.api.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<List<RoleResponse>> listarRoles() {
        return ResponseEntity.ok(roleService.listarRoles());
    }
}
