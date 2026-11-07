package com.orbytum.api.models.entity.joinColumns;

import com.orbytum.api.models.entity.Edital;
import com.orbytum.api.models.entity.Grupo;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "edital_x_grupo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EditalXGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edital_id", nullable = false)
    @Nonnull
    private Edital edital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    @Nonnull
    private Grupo grupo;

    public EditalXGrupo(@Nonnull Edital edital, @Nonnull Grupo grupo) {
        this.edital = edital;
        this.grupo = grupo;
    }
}
