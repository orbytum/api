package com.orbytum.api.configuration;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * O perfil Coordenador foi removido (os perfis passam a ser Líder do grupo, Líder de projeto,
 * Pesquisador e Colaborador). Como o Flyway está desativado e o esquema é mantido pelo Hibernate,
 * esta rotina converte os registros antigos na inicialização, antes de qualquer acesso, e recria as
 * restrições CHECK das colunas de nível sem o valor COORDENADOR. É idempotente; espelha a migração V12.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RemocaoCoordenadorInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(RemocaoCoordenadorInitializer.class);
    private static final String[] TABELAS = {"grupoxusuario", "convite_grupo", "projetoxusuario"};

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        for (String tabela : TABELAS) {
            int convertidos = jdbcTemplate.update(
                    "UPDATE " + tabela + " SET nivel = 'PESQUISADOR' WHERE nivel = 'COORDENADOR'");
            if (convertidos > 0) {
                logger.info("Perfil Coordenador removido: {} registro(s) de {} convertido(s) para PESQUISADOR.",
                        convertidos, tabela);
            }
            jdbcTemplate.execute("ALTER TABLE " + tabela + " DROP CONSTRAINT IF EXISTS " + tabela + "_nivel_check");
            jdbcTemplate.execute("ALTER TABLE " + tabela + " ADD CONSTRAINT " + tabela
                    + "_nivel_check CHECK (nivel IN ('LIDER', 'PESQUISADOR', 'COLABORADOR'))");
        }
    }
}
