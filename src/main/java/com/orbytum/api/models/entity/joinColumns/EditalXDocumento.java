package com.orbytum.api.models.entity.joinColumns;

import com.orbytum.api.models.entity.Documento;
import com.orbytum.api.models.entity.Edital;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "edital_x_documento",
        uniqueConstraints = @UniqueConstraint(
                name = "uc_edital_x_documento",
                columnNames = {"edital_id", "documento_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditalXDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nonnull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "edital_id", nullable = false)
    private Edital edital;

    @Nonnull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    public EditalXDocumento(@Nonnull Edital edital, @Nonnull Documento documento) {
        this.edital = edital;
        this.documento = documento;
    }
}
