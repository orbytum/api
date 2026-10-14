# Notícias — Plano de Execução

**Objetivo:** implementar o módulo de Notícias atendendo REQ-074, REQ-077, REQ-078 e REQ-080.

**Documento base:** `docs/20260630_requisitos.md` (seção 12).

**Escopo desta entrega (back-end):**

| REQ | Descrição | Tratado em |
| :-- | :-- | :-- |
| REQ-074 | Cadastrar notícia com título, conteúdo e referência | Fase 3 |
| REQ-077 | Filtrar notícias por assunto e/ou data | Fase 3 |
| REQ-078 | Ocultar automaticamente notícias expiradas após data final de exibição ou prazo padrão | Fase 3 |
| REQ-080 | Líder ou Coordenador valida/aprova notícias cadastradas por pesquisadores | Fase 3 |

**Responsabilidade do front-end (não do back-end):**

| REQ | Descrição | Observação |
| :-- | :-- | :-- |
| REQ-079 | Preencher automaticamente título e descrição de notícia inserida por link (web scraping) | **Executado no front-end.** O back-end apenas armazena o `referencia`/link informado. Nenhuma biblioteca de scraping é usada no servidor. |
| REQ-075/076 | Busca em portais externos e listagem de resultados | Fora de escopo. |

---

## 1. Decisões de arquitetura

| Tema | Decisão |
| :--- | :--- |
| Isolamento multi-tenant | A notícia pertence a um **Grupo de Pesquisa** (mesmo modelo de Atividades/Projetos). Autorização via `GrupoAcessoService`. |
| Autorização | Segue o padrão de `AtividadeService` (checagens no service), **não** `@RequiresPermission`. Admin (`AccessLevel.ADMIN`) tem bypass. |
| Fluxo de aprovação (REQ-080) | Qualquer membro do grupo cadastra; a notícia nasce como `PENDENTE`. Líder/Coordenador (ou Admin) aprovam ou rejeitam. |
| Status | Novo enum `NoticiaStatus { PENDENTE, APROVADA, REJEITADA }` com `AttributeConverter` (mesmo padrão de `AtividadeStatus`). |
| Expiração (REQ-078) | Ao aprovar, se `dataFimExibicao` não informada, materializa `dataFimExibicao = now + prazoExibicaoDias` (default 30, configurável). A listagem pública filtra `dataFimExibicao >= now`. |
| Visibilidade | Listagem principal retorna apenas `APROVADA`, ativas e não expiradas. Aba de aprovação usa `GET /noticias/pendentes`. |
| Web scraping (REQ-079) | **Removido do back-end.** A extração de título/descrição a partir de link é responsabilidade do front-end. O back-end recebe tais campos já preenchidos (e editáveis) no cadastro. |
| Migrações | Flyway `V7__noticias.sql` (idempotente). `spring.flyway.enabled=false` e `ddl-auto=update` no runtime atual. |
| Convenções | `controller` → `service` → `repository`; DTOs `record`; erros em `models/exceptions` tratados no `GlobalExceptionHandler`; sem comentários no código. |

---

## 2. Modelo de dados

Entidade `Noticia`:

| Campo | Tipo | Observação |
| :--- | :--- | :--- |
| `id` | Long (identity) | PK |
| `uuid` | UUID | Identificador externo |
| `grupo` | ManyToOne `Grupo` | Dono (multi-tenant) |
| `autor` | ManyToOne `Usuario` | Quem cadastrou |
| `aprovador` | ManyToOne `Usuario` (nullable) | Quem aprovou/rejeitou |
| `titulo` | String | REQ-074 |
| `conteudo` | String (TEXT) | REQ-074 |
| `referencia` | String (nullable) | REQ-074 (link/fonte) |
| `assunto` | String | REQ-077 |
| `dataInicioExibicao` | LocalDateTime (nullable) | REQ-078 |
| `dataFimExibicao` | LocalDateTime (nullable) | REQ-078 (materializada na aprovação) |
| `motivoRejeicao` | String (nullable) | REQ-080 |
| `status` | `NoticiaStatus` | REQ-080 |
| `dthRegistro` | LocalDateTime | criação |
| `dthAprovacao` | LocalDateTime (nullable) | auditoria |
| `isAtivo` | boolean | soft delete |

---

## 3. Endpoints (REST nível 2 — REQ-083)

| Método | Rota | Descrição | Autorização |
| :-- | :-- | :-- | :-- |
| POST | `/noticias` | Cadastra notícia (nasce `PENDENTE`) | membro do grupo |
| GET | `/noticias?grupoId=&assunto=&dataInicio=&dataFim=&page=&size=` | Lista aprovadas e vigentes | membro do grupo |
| GET | `/noticias/pendentes?grupoId=&page=&size=` | Aba de aprovação | Líder/Coordenador/Admin |
| GET | `/noticias/{id}` | Detalhe | membro do grupo |
| PUT | `/noticias/{id}` | Edita | autor ou Líder/Coordenador |
| PATCH | `/noticias/{id}/aprovar` | Aprova publicação | Líder/Coordenador/Admin |
| PATCH | `/noticias/{id}/rejeitar` | Rejeita com motivo | Líder/Coordenador/Admin |
| DELETE | `/noticias/{id}` | Soft delete | autor ou Líder/Coordenador |

---

## 4. Fases

### Fase 1 — Domínio e persistência
- Criar `NoticiaStatus`, `NoticiaStatusAttributeConverter`, entidade `Noticia`.
- Criar `NoticiaRepository` (busca por id ativo, paginação com filtros de vigência).

### Fase 2 — DTOs e erros
- `CreateNoticiaRequest`, `EditNoticiaRequest`, `RejeitarNoticiaRequest`.
- `NoticiaResponse`, `NoticiaPaginadaResponse`.
- `NoticiaNaoEncontradaErro`, `NoticiaNaoPendenteErro`.

### Fase 3 — Regras de negócio (`NoticiaService`)
- CRUD, validação de membros, aprovação/rejeição, expiração e filtros.

### Fase 4 — API
- `NoticiaController`, tratamento de erros, configuração em `application.yml`.

### Fase 5 — Migração
- `V7__noticias.sql`.

### Fase 6 — Testes
- `NoticiaServiceTest` (Mockito), `NoticiaIntegrationTest` (SpringBootTest).
- `./gradlew test`.

---

## 5. Matriz de rastreabilidade

| REQ | Fase |
| :-- | :-- |
| REQ-074 | 1, 2, 3, 4 |
| REQ-077 | 3, 4 |
| REQ-078 | 3, 4 |
| REQ-079 | Front-end |
| REQ-080 | 3, 4 |

---

## 6. Critérios de aceite (Definition of Done)

- [ ] Cadastro com título, conteúdo e referência.
- [ ] Filtro por assunto e/ou intervalo de datas.
- [ ] Notícias expiradas deixam de aparecer automaticamente.
- [ ] Notícia de pesquisador só é publicada após aprovação de Líder/Coordenador.
- [ ] Nenhuma dependência/biblioteca de scraping no back-end (REQ-079 é do front-end).
- [ ] `./gradlew test` verde.
