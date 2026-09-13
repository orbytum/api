package com.orbytum.api.models.entity.joinColumns;


import com.orbytum.api.models.entity.Edital;
import com.orbytum.api.models.entity.Projeto;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

@Entity
public class EditalXProjeto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @Nonnull
    private Edital edital;

    @ManyToOne(fetch = FetchType.LAZY)
    @Nonnull
    private Projeto projeto;

    @Nonnull
    private boolean isAtivo;
}
