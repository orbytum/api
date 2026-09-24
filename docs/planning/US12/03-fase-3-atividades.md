# Fase 3 — Atividades

**Pré-requisito:** Fase 1 e Fase 2.
**Requisitos:** REQ-029, REQ-030, REQ-031, REQ-032, REQ-033, REQ-034, REQ-035, REQ-036.

Implementa o CRUD e as regras de negócio de atividades. Hoje só existem a entidade `Atividade.java` e o enum; não há controller, service ou repository.

---

## T3.1 — Repositório, service e controller (CRUD)

**Requisitos:** REQ-029, REQ-030

**Arquivos:**
- Criar: `src/main/java/com/orbytum/api/repository/AtividadeRepository.java`
- Criar: `src/main/java/com/orbytum/api/service/AtividadeService.java`
- Criar: `src/main/java/com/orbytum/api/controller/AtividadeController.java`
- Criar: `src/main/java/com/orbytum/api/models/dto/request/CreateAtividadeRequest.java`
- Criar: `src/main/java/com/orbytum/api/models/dto/request/EditAtividadeRequest.java`
- Criar: `src/main/java/com/orbytum/api/models/dto/response/AtividadeResponse.java`
- Criar exceções: `AtividadeNaoEncontradaErro`, `AtividadeNaoPertenceAoProjetoErro`

**Passos:**
1. Repository:
   - `List<Atividade> findAllByProjetoIdAndIsAtivoTrue(Long)`
   - `List<Atividade> findAllByProjetoIdAndStatusInAndIsAtivoTrue(Long, Collection<AtividadeStatus>)`
   - `List<Atividade> findAllByAtividadePaiIdAndIsAtivoTrue(Long)`
   - Query de atrasadas (ver T3.4).
2. Endpoints REST:
   - `POST /atividades`
   - `PUT /atividades/{id}`
   - `DELETE /atividades/{id}` (soft delete `isAtivo = false`)
   - `GET /atividades/{id}`
   - `GET /atividades/projeto/{projetoId}`
   - `GET /atividades/projeto/{projetoId}/atrasadas`
3. `AtividadeResponse`: `id, projetoId, responsavelId, responsavelNome, atividadePaiId, titulo, descricao, status, dthRegistro, dthPrazo, dthConclusao, isAtrasada, isAtivo`.
4. Mapear `Atividade` ↔ DTOs.

**Critérios de aceite:**
- [ ] CRUD completo de atividades por projeto.
- [ ] Soft delete não remove fisicamente.
- [ ] Somente membros do grupo/projeto acessam.

---

## T3.2 — Responsável vinculado ao projeto

**Requisitos:** REQ-031

**Arquivos:**
- Modificar: `CreateAtividadeRequest` (adicionar `responsavelId`)
- Modificar: `AtividadeService`

**Passos:**
1. `responsavelId` obrigatório na criação.
2. Validar via `GrupoAcessoService.validarMembroDoProjeto(projetoId, responsavelId)` para atividades **de topo**.
3. Permitir responsável em atividades filhas conforme T3.5.

**Critérios de aceite:**
- [ ] Não é possível criar atividade sem responsável.
- [ ] Responsável precisa participar do projeto (atividade de topo).

---

## T3.3 — Datas e transições de status

**Requisitos:** REQ-032, REQ-033

**Arquivos:**
- Modificar: `AtividadeService`
- Modificar: `AtividadeStatus` (transições da Fase 1)
- Criar: `TransicaoAtividadeInvalidaErro`

**Passos:**
1. Criação: `dthRegistro = now()`, `dthPrazo` obrigatória, `status = PENDENTE`, `dthConclusao = null`.
2. Ao transicionar para `CONCLUIDA`: preencher `dthConclusao = now()`.
3. Validar transições permitidas (`PENDENTE → EM_ANDAMENTO → AGUARDANDO_CONFIRMACAO → CONCLUIDA → ENCERRADA`).
4. Endpoint dedicado `PATCH /atividades/{id}/status` (ou usar `PUT`), registrando a transição.

**Critérios de aceite:**
- [ ] As três datas registradas com semântica correta.
- [ ] Transições inválidas rejeitadas.
- [ ] `dthConclusao` preenchida somente ao concluir.

---

## T3.4 — Atividades atrasadas

**Requisitos:** REQ-034

**Arquivos:**
- Modificar: `AtividadeService`, `AtividadeRepository`, `AtividadeResponse`

**Passos:**
1. Definir atraso: `dthPrazo < now()` **e** status não em `CONCLUIDA`/`ENCERRADA`.
2. Expor `isAtrasada` no response (calculado).
3. Query/endpoint para listar atrasadas por projeto (e futuramente por grupo para o dashboard).

**Critérios de aceite:**
- [ ] Atividades atrasadas são identificáveis na API.
- [ ] Atividades concluídas/encerradas nunca aparecem como atrasadas.

---

## T3.5 — Impedimentos como atividade filha

**Requisitos:** REQ-035

**Arquivos:**
- Modificar: `AtividadeService`, `CreateAtividadeRequest`, `AtividadeController`

**Passos:**
1. `CreateAtividadeRequest` aceita `atividadePaiId` opcional.
2. Regra de perfil:
   - **PESQUISADOR**: só pode criar atividade **filha** de uma atividade `EM_ANDAMENTO` do projeto.
   - **LÍDER/COORDENADOR**: podem criar atividades de topo e filhas.
3. Atividade filha (impedimento) pode ser atribuída a **qualquer membro do grupo** (não apenas participante do projeto).
4. Validar que a atividade pai pertence ao mesmo projeto.

**Critérios de aceite:**
- [ ] Pesquisador não cria atividade de topo.
- [ ] Pesquisador cria impedimento apenas em atividade em andamento.
- [ ] Filha pode ser atribuída a qualquer membro do grupo.

---

## T3.6 — Encerrar última atividade → finalizar projeto

**Requisitos:** REQ-036, REQ-025

**Arquivos:**
- Modificar: `AtividadeService`, `AtividadeController`
- Modificar: `ProjetoService`, `ProjetoFachada`, `ProjetoController`

**Passos:**
1. Ao encerrar (`ENCERRADA`) uma atividade, verificar se é a **última atividade não encerrada** do projeto.
2. Se for, retornar na resposta uma flag `projetoPodeSerFinalizado = true` (o front abre o modal).
3. Criar `POST /projetos/{id}/finalizar`:
   - Restrito a Líder/Coordenador.
   - Reutiliza a regra de REQ-025 (projeto inicial só finaliza sem outros projetos abertos).
   - Transiciona o projeto para `ENCERRADO`.
4. Definir o que "última atividade" significa: nenhuma outra atividade com status ∉ {`CONCLUIDA`, `ENCERRADA`}.

**Critérios de aceite:**
- [ ] Encerrar a última atividade sinaliza finalização do projeto.
- [ ] Endpoint de finalização respeita REQ-025.
- [ ] Apenas Líder/Coordenador finaliza projeto.

---

## T3.7 — (Opcional) Integração com `isFavorito`/dashboard

**Requisitos:** REQ-070, REQ-071, REQ-086 (preparação)

**Passos:**
1. Garantir que consultas de atividades retornem dados suficientes para o calendário/dashboard (projeto, status, datas).
2. Não implementar UI nem sistema solar nesta entrega.

---

## Checklist de saída da Fase 3

- [ ] CRUD de atividades (REQ-029/030).
- [ ] Responsável membro do projeto (REQ-031).
- [ ] Três datas + transições (REQ-032/033).
- [ ] Atrasadas identificáveis (REQ-034).
- [ ] Hierarquia pai/filha e regras de pesquisador (REQ-035).
- [ ] Encerramento integrado à finalização de projeto (REQ-036).
- [ ] `./gradlew test` verde.
