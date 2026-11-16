package com.orbytum.api.configuration;

import com.orbytum.api.models.enums.SolicitacaoStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A aprovação das solicitações passou a ter duas etapas (status AGUARDANDO_ADMINISTRADOR). Como o
 * esquema é mantido pelo Hibernate em modo update, a restrição CHECK criada para a coluna de status
 * não é atualizada sozinha; esta rotina a recria a partir dos valores do enum. É idempotente.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class SolicitacaoStatusInitializer implements CommandLineRunner {

    private static final String TABELA = "material_emprestimo_solicitacao";

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        String valores = Arrays.stream(SolicitacaoStatus.values())
                .map(s -> String.valueOf(s.getId()))
                .collect(Collectors.joining(", "));
        jdbcTemplate.execute("ALTER TABLE " + TABELA + " DROP CONSTRAINT IF EXISTS " + TABELA + "_status_check");
        jdbcTemplate.execute("ALTER TABLE " + TABELA + " ADD CONSTRAINT " + TABELA
                + "_status_check CHECK (status IN (" + valores + "))");
    }
}
