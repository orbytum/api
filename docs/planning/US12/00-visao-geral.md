# US12 — Grupos de Pesquisa e Atividades

**Objetivo:** fechar os requisitos de Grupos de Pesquisa (REQ-012 a REQ-021, REQ-091, REQ-092) e implementar Atividades (REQ-029 a REQ-036), incluindo as dependências de Projetos necessárias para o funcionamento das atividades (REQ-024, REQ-025, REQ-027, REQ-028, REQ-086).

**Documento base:** `docs/20260630_requisitos.md`

---

## 1. Decisões de arquitetura

| Tema | Decisão |
| :--- | :--- |
| Organização / multi-tenant | **Descontinuado.** Não será criada entidade `Organizacao`. O isolamento multi-tenant passa a ser baseado no **Grupo de Pesquisa**. Ignorar REQ-010/011/014 e a referência a organização em REQ-003/012. |
| Hierarquia de papéis | Novo enum `NivelMembro { LIDER, COORDENADOR, PESQUISADOR }` aplicado **no vínculo** (`GrupoXUsuario` e `ConviteGrupo`). `Role` continua existindo apenas para permissões finas. |
| Autorização | Centralizada em um `GrupoAcessoService` (membro, líder, coordenador-ou-acima, nível do usuário, membro do projeto). Elimina checagens ad-hoc espalhadas. |
| Participantes de projeto | Nova tabela/entidade `ProjetoXUsuario`, pré-requisito para REQ-028 e REQ-031. |
| Datas de atividade | `dthRegistro` (criação, automática), `dthPrazo` (entrega, obrigatória), `dthConclusao` (preenchida ao concluir, **nullable**). |
| Fluxo de status de atividade | Substituir o enum atual por `PENDENTE → EM_ANDAMENTO → AGUARDANDO_CONFIRMACAO → CONCLUIDA → ENCERRADA`. |
| Migrações | Manter Flyway (`V3+`). Hoje `spring.flyway.enabled=false` e `ddl-auto=update`; os scripts devem refletir as mudanças para ambientes com Flyway ligado. |

---

## 2. Mapa de requisitos → situação atual → onde será tratado

### Grupos de Pesquisa

| REQ | Situação | Tratado em |
| :-- | :-- | :-- |
| REQ-012 CRUD de grupos | 🟡 Parcial | Fase 2 |
| REQ-013 Líder obrigatório na criação | ❌ | Fase 2 |
| REQ-014 Grupos em organização | ⛔ Descontinuado | — |
| REQ-015 Hierarquia Líder > Coordenador > Pesquisador | ❌ | Fase 1 |
| REQ-016 Um líder por grupo | ❌ | Fase 2 |
| REQ-017 Coordenador múltiplo por grupo | ❌ | Fase 1 + 2 |
| REQ-018 Pesquisador múltiplo por grupo | ✅ | — |
| REQ-019 Autocadastro de Pesquisador via convite | ❌ | Fase 2 |
| REQ-020 Autocadastro de Coordenador via convite | ❌ | Fase 2 |
| REQ-021 Autocadastro de Líder via convite do Admin | 🟡 | Fase 2 |
| REQ-091 Admin edita/remove pesquisadores e grupos | ✅ | Fase 2 (ajuste `funcao`/`nivel`) |
| REQ-092 Campo `funcao` VARCHAR(20) | ❌ | Fase 1 |

### Dependências de Projetos

| REQ | Situação | Tratado em |
| :-- | :-- | :-- |
| REQ-024 Projeto inicial automático ao criar grupo | ❌ | Fase 2 |
| REQ-025 Finalização do projeto inicial | ✅ (`ProjetoService.delete`) | Fase 3 (endpoint de finalização) |
| REQ-027 Criação de projeto por Líder/Coordenador | ❌ | Fase 2 |
| REQ-028 Participantes do projeto | ❌ | Fase 1 + 2 |
| REQ-086 Campo `isFavorite` | 🟡 | Fase 2 |

### Atividades

| REQ | Situação | Tratado em |
| :-- | :-- | :-- |
| REQ-029 CRUD de atividades | ❌ | Fase 3 |
| REQ-030 Atividades vinculadas a projetos | 🟡 | Fase 1 + 3 |
| REQ-031 Associar pessoa (membro do projeto) | ❌ | Fase 3 |
| REQ-032 Três datas | 🟡 | Fase 1 + 3 |
| REQ-033 Fluxo de status | ❌ | Fase 1 + 3 |
| REQ-034 Destacar atrasadas | ❌ | Fase 3 |
| REQ-035 Impedimentos como atividade filha | ❌ | Fase 1 + 3 |
| REQ-036 Encerrar última atividade → finalizar projeto | ❌ | Fase 3 |

---

## 3. Ordem de execução

1. **Fase 1 — Domínio e autorização** (`01-fase-1-dominio-e-autorizacao.md`)
2. **Fase 2 — Grupos de Pesquisa** (`02-fase-2-grupos-de-pesquisa.md`)
3. **Fase 3 — Atividades** (`03-fase-3-atividades.md`)
4. **Fase 4 — Migrações e testes** (`04-fase-4-migracoes-e-testes.md`)

Cada fase é incremental e deve compilar/testar antes da seguinte. A Fase 1 é pré-requisito das demais.

---

## 4. Riscos e observações

- **Mudança de assinatura de DTOs:** alterar `CreateGroupRequest` quebra `GrupoPaginacaoIntegrationTest`. Ajustar junto (Fase 4).
- **Migração de status:** valores antigos de `AtividadeStatus` (`A_FAZER`, `FINALIZADA`, `TRAVADA`) precisam de script de conversão caso existam dados.
- **`ddl-auto: update`** pode mascarar migrações; validar em ambiente com Flyway habilitado.
- **Compatibilidade:** `Role.isLider` é usado em vários pontos (`ConviteService`, `GrupoService`); manter derivado de `nivel == LIDER` para não quebrar.
- **Escopo de calendário/dashboard (REQ-067..073) não faz parte desta entrega**, mas os campos de datas e status devem ser modelados para alimentá-los depois.

---

## 5. Convenções

- Seguir o padrão existente: `controller` → `service`/`fachada` → `repository`, DTOs `record` em `models/dto/request` e `models/dto/response`.
- Erros de negócio em `models/exceptions` e tratamento em `GlobalExceptionHandler`.
- Sem comentários no código-fonte, salvo Javadoc já existente.
- Endpoints REST nível 2 (REQ-083).
