# Guia de Integração Front-end — US12 (Grupos de Pesquisa e Atividades)

**Público:** time de front-end (React).
**Base:** implementação das Fases 1–4 descritas em `docs/planning/US12/`.
**Requisitos cobertos:** REQ-012 a REQ-021, REQ-024, REQ-025, REQ-027, REQ-028, REQ-029 a REQ-036, REQ-086, REQ-091, REQ-092.

> **Observação:** Organização foi **descontinuada**. O isolamento (multi-tenant) é baseado no **Grupo de Pesquisa**. REQ-010, REQ-011 e REQ-014 não se aplicam.

---

## 1. Resumo das mudanças relevantes para o front

| Área | O que mudou |
| :--- | :--- |
| Papéis | Novo enum `NivelMembro`: `LIDER`, `COORDENADOR`, `PESQUISADOR`, exposto em vínculos, convites e listagens. |
| Grupos | Líder **obrigatório** na criação; **um único** líder ativo por grupo; projeto inicial criado automaticamente. |
| Convites | Convite de grupo agora tem `nivel`; novo endpoint **público** de autocadastro; pesquisador/coordenador podem convidar conforme hierarquia. |
| Projetos | `isFavorito` e `isInicial` expostos; participantes (`/participantes`); `PATCH /favorito`; `POST /finalizar`. |
| Atividades | CRUD completo, status novos, hierarquia (impedimento = atividade filha), atraso calculado, sinalização de finalização de projeto. |
| Pesquisador | Campo `funcao` (VARCHAR(20)) editável. |

---

## 2. Enums

### `NivelMembro`
`LIDER` > `COORDENADOR` > `PESQUISADOR` (hierarquia).

### `AtividadeStatus` (novo fluxo)
```
PENDENTE -> EM_ANDAMENTO -> AGUARDANDO_CONFIRMACAO -> CONCLUIDA -> ENCERRADA
```
- `PENDENTE`: criada, não iniciada.
- `EM_ANDAMENTO`: em execução.
- `AGUARDANDO_CONFIRMACAO`: responsável finalizou, aguarda validação.
- `CONCLUIDA`: validada (grava `dthConclusao`).
- `ENCERRADA`: arquivada.
- Transições fora dessa ordem são rejeitadas (`400`).
- `isAberta = status ∉ {CONCLUIDA, ENCERRADA}`.

### `ProjetoStatus`
`PLANEJADO`, `EM_ANDAMENTO`, `CONCLUIDO`, `ENCERRADO`, `CANCELADO`.

---

## 3. Matriz de permissões

| Operação | Quem pode |
| :--- | :--- |
| CRUD grupo | Administrador **criador** do grupo |
| Cadastrar/editar/remover líder | Administrador criador |
| Listar/editar/remover pesquisadores | Administrador criador |
| Gerar convite de **Pesquisador** | Líder ou Coordenador do grupo |
| Gerar convite de **Coordenador** | Líder do grupo |
| Gerar convite de **Líder** | Administrador (via criação do grupo) |
| Criar projeto | Líder/Coordenador do grupo (ou Admin) |
| Gerenciar participantes do projeto | Líder/Coordenador do grupo (ou Admin) |
| Favoritar projeto | Qualquer membro do grupo (ou Admin) |
| Criar atividade **de topo** | Líder/Coordenador (ou Admin) |
| Criar atividade **filha (impedimento)** | Qualquer membro do grupo |
| Editar/excluir atividade | Líder/Coordenador (ou Admin) |
| Concluir/Encerrar atividade | Líder/Coordenador (ou Admin) |
| Demais transições de status | Qualquer membro do grupo |
| Finalizar projeto | Líder/Coordenador (ou Admin) |

> Todas as rotas exigem `Authorization: Bearer <jwt>` (exceto as públicas indicadas).

---

## 4. Autenticação (existente)

| Método | Rota | Auth | Body | Retorno |
| :-- | :-- | :-- | :-- | :-- |
| POST | `/auth/login` | pública | `{ email, senha }` | `{ token, tipo }` |
| POST | `/auth/register-admin` | admin | `RegisterAdminRequest` | `{ token, tipo }` |
| GET | `/roles` | autenticado | — | `[{ id, nome, isLider }]` |

O JWT contém `sub` (e-mail), `accessLevel` (`admin`/`user`/`initial_admin`), `permissoes` e `iat`.

---

## 5. Grupos de Pesquisa

### 5.1 `POST /grupos` — criar grupo (Admin)
```json
{ "nome": "Laboratório de IA", "emailLider": "lider@empresa.com" }
```
- `emailLider` é **obrigatório**.
- Se o e-mail **já existir** como usuário: ele é vinculado imediatamente como `LIDER`.
- Se **não existir**: gera convite de líder (limite 1) e envia e-mail; o líder completa o cadastro pelo link público.
- Efeito colateral: cria automaticamente o **Projeto Inicial** (`isInicial = true`, `titulo = "Projeto Inicial"`).

Retorno `GrupoResponse`:
```json
{ "id": 1, "nome": "...", "isAtivo": true, "nomeLider": "...", "idLider": 10, "totalParticipantes": 1 }
```

### 5.2 Demais rotas de grupos

| Método | Rota | Body | Retorno |
| :-- | :-- | :-- | :-- |
| PUT | `/grupos/{id}` | `{ nome, isAtivo }` | `GrupoResponse` |
| GET | `/grupos?page&size&nome&usuario` | — | `GrupoPaginadoResponse` |
| GET | `/grupos/{id}` | — | `GrupoResponse` |
| DELETE | `/grupos/{id}` | — | `204` (soft delete) |
| POST | `/grupos/{grupoId}/lideres` | `{ nome, email, telefone, titulo, senha }` | `LiderResponse` |
| PUT | `/grupos/{grupoId}/lideres/{usuarioId}` | `{ nome, telefone, titulo }` | `LiderResponse` |
| GET | `/grupos/{grupoId}/pesquisadores?page&size&nome` | — | `PesquisadorPaginadoResponse` |
| PUT | `/grupos/{grupoId}/pesquisadores/{usuarioId}` | `{ nome, telefone, titulo, funcao }` | `PesquisadorResponse` |
| DELETE | `/grupos/{grupoId}/pesquisadores/{usuarioId}` | — | `204` |
| GET | `/grupos/meus-grupos` | — | `[MeuGrupoResponse]` |

`PesquisadorResponse`:
```json
{
  "usuarioId": 10, "nome": "...", "email": "...", "telefone": "...", "titulo": "...",
  "grupoId": 1, "cargo": "Coordenador", "nivel": "COORDENADOR", "funcao": "Orientador", "isLider": false
}
```
`MeuGrupoResponse`:
```json
{ "id": 1, "nome": "...", "role": "Líder", "nivel": "LIDER", "isLider": true }
```
`GrupoPaginadoResponse`:
```json
{ "items": [...], "totalElements": 3, "totalPages": 1, "currentPage": 1, "pageSize": 10 }
```

---

## 6. Convites e autocadastro

### 6.1 Convite direto (e-mail) — Líder/Coordenador/Admin
`POST /convites`
```json
{
  "idGrupo": 1,
  "email": "convidado@empresa.com",
  "idsProjeto": [5],
  "diasValidade": 7,
  "idRole": null,
  "nivel": "PESQUISADOR"
}
```
- `nivel` aceita `PESQUISADOR` (Líder/Coordenador) ou `COORDENADOR` (somente Líder).
- O convidado precisa **já existir** como usuário neste fluxo.

### 6.2 Convite por link (compartilhável) — Líder/Coordenador
`POST /convites/grupo`
```json
{ "idGrupo": 1, "idsProjeto": [5], "diasValidade": 7, "limiteUso": 5, "idRole": null, "nivel": "PESQUISADOR" }
```
Retorno inclui `token` e `urlConvite`.

### 6.3 Consultar convite (público)
`GET /convites/aceitar/grupo/{token}` → `ConviteGrupoDetalheResponse`
```json
{
  "idConvite": 1, "token": "...", "idGrupo": 1, "nomeGrupo": "...",
  "nomeRemetente": "...", "cargo": "Pesquisador", "nivel": "PESQUISADOR",
  "dthExpiracao": "2026-07-01T10:00:00", "isAtivo": true
}
```

### 6.4 Aceitar como usuário já autenticado
`POST /convites/aceitar/grupo/{token}` (Bearer) → `ConviteGrupoEnviadoResponse`

### 6.5 **Autocadastro (público)** — novo
`POST /convites/aceitar/grupo/{token}/cadastro`
```json
{ "nome": "Novo Pesquisador", "senha": "Senha@123", "telefone": "11999999999", "titulo": "Mestrando", "email": "novo@empresa.com" }
```
- `email` é opcional **somente** se o convite foi gerado para um e-mail específico; caso contrário, é obrigatório.
- Cria usuário + credenciais (`accessLevel = user`, `criador` = remetente do convite) e vincula ao grupo com o `nivel` do convite.
- Retorna `{ token, tipo }` (login automático).

### 6.6 Convites de cadastro (Admin — existente)
| Método | Rota | Descrição |
| :-- | :-- | :-- |
| POST | `/convites/cadastro` | gera convite de cadastro |
| GET | `/convites/cadastro` | lista paginada |
| DELETE | `/convites/cadastro/{id}` | revoga |
| POST | `/convites/aceitar/cadastro/{token}` | pública, cria usuário |

---

## 7. Projetos

| Método | Rota | Body | Retorno |
| :-- | :-- | :-- | :-- |
| POST | `/projetos` | `{ grupoId, status, titulo, assunto }` | `ProjetoResponse` |
| PUT | `/projetos/{id}` | `{ status, titulo, assunto, isAtivo }` | `ProjetoResponse` |
| DELETE | `/projetos/{id}` | — | `204` |
| PATCH | `/projetos/{id}/favorito` | — | `ProjetoResponse` |
| POST | `/projetos/{id}/finalizar` | — | `ProjetoResponse` |
| GET | `/projetos/grupo/{grupoId}` | — | `[ProjetoResponse]` |
| GET | `/projetos/{id}` | — | `ProjetoResponse` |
| GET | `/projetos/{id}/participantes` | — | `[ParticipanteResponse]` |
| POST | `/projetos/{id}/participantes` | `{ usuarioId, nivel }` | `ParticipanteResponse` |
| DELETE | `/projetos/{id}/participantes/{usuarioId}` | — | `204` |

`ProjetoResponse`:
```json
{
  "id": 5, "grupoId": 1, "status": "PLANEJADO", "titulo": "...", "assunto": "...",
  "dthRegistro": "2026-06-30T10:00:00", "isAtivo": true, "isInicial": true, "isFavorito": false
}
```
`ParticipanteResponse`:
```json
{ "usuarioId": 10, "nome": "...", "email": "...", "nivel": "PESQUISADOR", "isAtivo": true }
```

**Regras:**
- Ao criar projeto, o criador entra automaticamente como participante.
- `POST /{id}/finalizar` respeita REQ-025: o **Projeto Inicial** só finaliza se não houver outros projetos abertos.
- `PATCH /{id}/favorito` alterna `isFavorito` (usado no dashboard/sistema solar).

---

## 8. Atividades

| Método | Rota | Body | Retorno |
| :-- | :-- | :-- | :-- |
| POST | `/atividades` | `CreateAtividadeRequest` | `AtividadeResponse` |
| PUT | `/atividades/{id}` | `EditAtividadeRequest` | `AtividadeResponse` |
| PATCH | `/atividades/{id}/status` | `{ status }` | `AtividadeResponse` |
| DELETE | `/atividades/{id}` | — | `204` |
| GET | `/atividades/{id}` | — | `AtividadeResponse` |
| GET | `/atividades/projeto/{projetoId}` | — | `[AtividadeResponse]` |
| GET | `/atividades/projeto/{projetoId}/atrasadas` | — | `[AtividadeResponse]` |

**Criar atividade** (`CreateAtividadeRequest`):
```json
{
  "projetoId": 5,
  "responsavelId": 10,
  "atividadePaiId": null,
  "titulo": "Coletar amostras",
  "descricao": "Coletar 10 amostras",
  "dthPrazo": "2026-07-15T18:00:00"
}
```
- `atividadePaiId = null` → atividade de topo (apenas Líder/Coordenador); o responsável **precisa** ser participante do projeto.
- `atividadePaiId != null` → **impedimento**; a atividade pai precisa estar `EM_ANDAMENTO`; o responsável pode ser **qualquer membro do grupo**.

**Editar** (`EditAtividadeRequest`): `{ responsavelId?, titulo, descricao, dthPrazo? }`.

**`AtividadeResponse`:**
```json
{
  "id": 1,
  "projetoId": 5,
  "responsavelId": 10,
  "responsavelNome": "Fulano",
  "atividadePaiId": null,
  "titulo": "...",
  "descricao": "...",
  "status": "EM_ANDAMENTO",
  "dthRegistro": "2026-06-30T10:00:00",
  "dthPrazo": "2026-07-15T18:00:00",
  "dthConclusao": null,
  "isAtrasada": false,
  "isAtivo": true,
  "projetoPodeSerFinalizado": false
}
```

### Fluxo de finalização de projeto (REQ-036)
1. O front avança a atividade por `PATCH /atividades/{id}/status` até `ENCERRADA`.
2. A resposta traz `projetoPodeSerFinalizado = true` quando **não há mais atividades abertas** no projeto.
3. Se `true`, o front abre o modal: *"Deseja finalizar o projeto?"*.
4. Ao confirmar, chamar `POST /projetos/{id}/finalizar`.

`dthConclusao` é preenchida automaticamente quando o status vira `CONCLUIDA`.

---

## 9. Códigos de erro

Formato padrão `ErroResponse`: `{ "status": <int>, "mensagem": "...", "timestamp": <long> }`.

| HTTP | Situação |
| :-- | :-- |
| 400 | validação, transição de status inválida, responsável inválido, regra de negócio |
| 403 | sem permissão no grupo / papel insuficiente |
| 404 | grupo/projeto/atividade/usuário não encontrado |
| 409 | nome de grupo duplicado, líder já existente, usuário já no grupo, projeto de outro grupo |

Mensagens úteis para exibição:
- `"O líder do grupo é obrigatório"`
- `"Este grupo de pesquisa já possui um líder ativo"`
- `"Apenas o Líder do grupo pode convidar coordenadores"`
- `"Impedimentos só podem ser registrados em atividades Em Andamento"`
- `"Transição de status inválida: PENDENTE -> CONCLUIDA"`
- `"O projeto Inicial só pode ser finalizado se não houver outros projetos ativos"`

---

## 10. Sugestão de telas/fluxos

1. **Gestão de grupos (Admin):** criar grupo (com líder obrigatório), listar/paginar, editar, ativar/inativar, gerenciar líder e pesquisadores (com `funcao`).
2. **Convites:** gerar link (com seleção de nível), listar convites ativos, tela pública de aceite/autocadastro (lê `GET /convites/aceitar/grupo/{token}` e decide entre login ou cadastro).
3. **Projetos:** lista por grupo (destacar `isFavorito`), criar/editar, gerenciar participantes, botão "Finalizar".
4. **Atividades:** Kanban/lista por projeto com colunas pelos status; indicador de atraso (`isAtrasada`); criação de impedimento a partir de atividade `EM_ANDAMENTO`; modal de finalização acionado por `projetoPodeSerFinalizado`.
5. **Meus grupos:** usar `/grupos/meus-grupos` para montar o seletor de contexto (tenant = grupo).

---

## 11. Fora de escopo desta entrega

- Calendário (REQ-067–069), Dashboard sistema solar (REQ-070–073), Notícias (REQ-074–080), Materiais/Solicitações/Publicações (já parcialmente implementados em outros módulos).
- Integrações externas de editais/eventos/notícias.

Os campos de **datas** e **status** de atividades/projetos já foram modelados para alimentar calendário e dashboard futuramente.
