package com.orbytum.api.models.entity;

import com.orbytum.api.models.converter.TipoLembreteAttributeConverter;
import com.orbytum.api.models.converter.TipoRecorrenciaAttributeConverter;
import com.orbytum.api.models.enums.TipoLembrete;
import com.orbytum.api.models.enums.TipoRecorrencia;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nonnull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    @Nonnull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criador_id", nullable = false)
    private Usuario criador;

    @Nonnull
    @Column(nullable = false)
    private String titulo;

    @Column(length = 1000)
    private String descricao;

    @Column(name = "is_online", nullable = false)
    @Builder.Default
    private boolean isOnline = false;

    @Column(length = 500)
    private String url;

    private String localizacao;

    @Nonnull
    @Convert(converter = TipoLembreteAttributeConverter.class)
    @Column(nullable = false, length = 50)
    private TipoLembrete tipo;

    @Nonnull
    @Column(name = "dth_inicio", nullable = false)
    private LocalDateTime dthInicio;

    @Column(name = "dth_fim")
    private LocalDateTime dthFim;

    @Nonnull
    @Convert(converter = TipoRecorrenciaAttributeConverter.class)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private TipoRecorrencia recorrencia = TipoRecorrencia.NENHUMA;

    @Nonnull
    @Column(nullable = false)
    private String organizador;

    @Column(name = "ata_url", length = 500)
    private String ataUrl;

    @Column(name = "ata_s3_key", length = 500)
    private String ataS3Key;

    @Column(name = "ata_nome_arquivo")
    private String ataNomeArquivo;

    @Nonnull
    @Column(name = "dth_registro", nullable = false)
    private LocalDateTime dthRegistro;

    @Nonnull
    @Column(name = "is_ativo", nullable = false)
    @Builder.Default
    private boolean isAtivo = true;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EventoParticipante> participantes = new ArrayList<>();

    public LocalDateTime getDataHora() {
        return dthInicio;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dthInicio = dataHora;
    }

    public String getLink() {
        return url;
    }

    public void setLink(String link) {
        this.url = link;
        if (link != null && !link.isBlank()) {
            this.isOnline = true;
        }
    }

    public String getLocal() {
        return localizacao;
    }

    public void setLocal(String local) {
        this.localizacao = local;
    }

    public boolean isTemAta() {
        return (ataUrl != null && !ataUrl.isBlank()) || (ataS3Key != null && !ataS3Key.isBlank());
    }
}
