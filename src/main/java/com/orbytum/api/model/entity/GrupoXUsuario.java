package com.orbytum.api.model.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Entity
public class GrupoXUsuario {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @ManyToOne
    private Grupo grupo;

    @ManyToOne
    private Usuario usuario;



}
