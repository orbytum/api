# Fase 4 — Migrações e Testes

**Pré-requisito:** Fases 1, 2 e 3.
**Requisitos:** transversais (REQ-083, REQ-084) e validação de todas as fases.

---

## T4.1 — Migrações Flyway (`V3+`)

**Contexto:** `src/main/resources/application.yml` tem `spring.flyway.enabled=false` e `spring.jpa.hibernate.ddl-auto=update`. Os scripts são necessários para ambientes com Flyway habilitado e para documentar a evolução.

**Arquivos:**
- Criar: `src/main/resources/db/migration/V3__grupos_niveis_e_funcao.sql`
- Criar: `src/main/resources/db/migration/V4__projeto_participantes_e_flags.sql`
- Criar: `src/main/resources/db/migration/V5__atividades_rework.sql`

**Passos:**
1. **V3** — grupos:
   - `ALTER TABLE grupoxusuario ADD COLUMN IF NOT EXISTS nivel VARCHAR(20);`
   - `ALTER TABLE grupoxusuario ADD COLUMN IF NOT EXISTS funcao VARCHAR(20);`
   - `ALTER TABLE convite_grupo ADD COLUMN IF NOT EXISTS nivel VARCHAR(20);`
   - Backfill: vínculos de `Role.is_lider = true` → `nivel = 'LIDER'`; demais → `'PESQUISADOR'`.
2. **V4** — projetos:
   - Criar tabela `projeto_x_usuario` (id, projeto_id, usuario_id, nivel, is_ativo) + FKs.
   - `ALTER TABLE projeto ADD COLUMN IF NOT EXISTS is_inicial BOOLEAN NOT NULL DEFAULT false;`
   - `ALTER TABLE projeto ADD COLUMN IF NOT EXISTS is_favorito BOOLEAN NOT NULL DEFAULT false;`
3. **V5** — atividades:
   - `ALTER TABLE atividade ADD COLUMN IF NOT EXISTS responsavel_id BIGINT;`
   - `ALTER TABLE atividade ADD COLUMN IF NOT EXISTS atividade_pai_id BIGINT;`
   - `ALTER TABLE atividade ALTER COLUMN dth_conclusao DROP NOT NULL;`
   - Converter status legado: `A_FAZER → PENDENTE(1)`, `EM_ANDAMENTO → EM_ANDAMENTO(2)`, `FINALIZADA → CONCLUIDA(4)`, `TRAVADA → EM_ANDAMENTO(2)` (confirmar regra).
   - FKs de `responsavel_id` (usuario) e `atividade_pai_id` (atividade).

**Critérios de aceite:**
- [ ] Scripts idempotentes (`IF NOT EXISTS`).
- [ ] Backfill de dados legados definido.
- [ ] Aplicáveis em banco vazio e em banco existente.

---

## T4.2 — Ajuste de testes existentes

**Arquivos:**
- Modificar: `src/test/java/com/orbytum/api/GrupoPaginacaoIntegrationTest.java`
- Modificar: `src/test/java/com/orbytum/api/service/GrupoServiceTest.java`

**Passos:**
1. Atualizar `new CreateGroupRequest(...)` para a nova assinatura (líder obrigatório).
2. Ajustar `testCriarGrupoComEmailLiderInexistenteLancaExcecao` (o e-mail inexistente agora gera convite, não exceção — redefinir comportamento esperado).
3. Ajustar mocks de `GrupoService` para os novos serviços/dependências.

**Critérios de aceite:**
- [ ] Testes existentes refletem o novo comportamento.
- [ ] Nenhum teste quebrado por mudança de contrato.

---

## T4.3 — Novos testes

**Arquivos sugeridos:**
- `src/test/java/com/orbytum/api/service/GrupoAcessoServiceTest.java`
- `src/test/java/com/orbytum/api/service/AtividadeServiceTest.java`
- `src/test/java/com/orbytum/api/ConviteGrupoAutocadastroIntegrationTest.java`

**Cenários mínimos:**
1. **Hierarquia:** líder reconhecido; coordenador não pode convidar líder; pesquisador não pode convidar.
2. **Líder único:** segundo líder ativo é rejeitado.
3. **Projeto inicial:** criar grupo gera projeto `isInicial = true`.
4. **Participantes:** só membros do grupo entram no projeto; só participantes recebem atividade de topo.
5. **Transições:** sequência válida e inválida de `AtividadeStatus`.
6. **Atraso:** `isAtrasada` verdadeiro apenas para prazo vencido e status aberto.
7. **Impedimento:** pesquisador só cria filha em atividade `EM_ANDAMENTO`; filha aceita membro do grupo.
8. **REQ-036/025:** encerrar última atividade sinaliza finalização; projeto inicial não finaliza com outro aberto.
9. **Autocadastro:** convite público cria usuário com `criador` preenchido e vínculo com o nível correto.

**Critérios de aceite:**
- [ ] Cobertura dos fluxos principais.
- [ ] `./gradlew test` verde.

---

## T4.4 — Verificação final (Definition of Done)

- [ ] `./gradlew build` (ou `test`) sem erros.
- [ ] Todos os checklists das Fases 1–3 marcados.
- [ ] Requisitos REQ-012 a REQ-021 (exceto REQ-014 descontinuado), REQ-024, REQ-025, REQ-027, REQ-028, REQ-029 a REQ-036, REQ-086, REQ-091, REQ-092 atendidos.
- [ ] Endpoints documentados no Swagger (`springdoc`) — verificar exposição em `/swagger-ui`.
- [ ] Sem segredos/credenciais no código ou nos docs.
- [ ] Mensagens de erro padronizadas e mapeadas no `GlobalExceptionHandler`.

---

## Matriz de rastreabilidade final

| REQ | Fase/Tarefa |
| :-- | :-- |
| REQ-012 | 2 |
| REQ-013 | 2.1 |
| REQ-014 | Descontinuado |
| REQ-015 | 1.1, 1.2 |
| REQ-016 | 2.1 |
| REQ-017 | 1.1, 2.2 |
| REQ-018 | Já atendido |
| REQ-019 | 2.2 |
| REQ-020 | 2.2 |
| REQ-021 | 2.1, 2.2 |
| REQ-024 | 2.3 |
| REQ-025 | 2.3, 3.6 |
| REQ-027 | 2.4 |
| REQ-028 | 1.3, 2.4 |
| REQ-029 | 3.1 |
| REQ-030 | 1.4, 3.1 |
| REQ-031 | 1.4, 3.2 |
| REQ-032 | 1.4, 3.3 |
| REQ-033 | 1.5, 3.3 |
| REQ-034 | 3.4 |
| REQ-035 | 1.4, 3.5 |
| REQ-036 | 3.6 |
| REQ-086 | 2.5 |
| REQ-091 | 2.6 |
| REQ-092 | 1.1, 2.6 |
