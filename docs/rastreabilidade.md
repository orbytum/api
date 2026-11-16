# Rastreabilidade de requisitos — Orbytum

**Base:** `20260630_requisitos.md` revisado em 16/11/2026 (83 requisitos vigentes: 42 funcionais e 41 não funcionais), comparado ao código de `api/` e `front-end/`.
**Legenda:** ✅ implementado · 🟡 parcial · ❌ não implementado

## Resumo por módulo

| Módulo | Vigentes | ✅ | 🟡 | ❌ | Removidos na revisão |
| :-- | --: | --: | --: | --: | :-- |
| 1. Autenticação e Login | 8 | 8 | 0 | 0 | REQ-005 |
| 2. Administração | 2 | 2 | 0 | 0 | — |
| 3. Grupos de Pesquisa | 9 | 9 | 0 | 0 | REQ-017, 020 |
| 4. Projetos | 8 | 8 | 0 | 0 | — |
| 5. Atividades | 8 | 8 | 0 | 0 | — |
| 6. Solicitações | 7 | 7 | 0 | 0 | — |
| 7. Materiais | 6 | 6 | 0 | 0 | — |
| 8. Publicações | 3 | 3 | 0 | 0 | — |
| 9. Lembretes, Editais e Eventos | 16 | 16 | 0 | 0 | REQ-052, 061, 062, 064, 065 |
| 10. Calendário | 3 | 3 | 0 | 0 | — |
| 11. Dashboard | 2 | 2 | 0 | 0 | REQ-072, 073 |
| 12. Notícias | 5 | 5 | 0 | 0 | REQ-075, 076 |
| 13. Aspectos técnicos e administração | 6 | 6 | 0 | 0 | REQ-085 |
| **Total** | **83** | **83** | **0** | **0** | **13** |

## Mudanças da revisão de 16/11/2026

- **Removidos (10):** notificações por e-mail (REQ-052); busca automática de editais, eventos e notícias em portais externos (REQ-061, 062, 064, 065, 075, 076), pois esses itens serão cadastrados manualmente; distância orbital por status (REQ-072); painel consolidado do administrador (REQ-073); projeto multi-módulo (REQ-085).
- **Reescritos:** REQ-003, 010, 011 e 014 (não há entidade de organização; cada organização usa uma instância do sistema e o grupo de pesquisa é a entidade de mais alto nível); REQ-009 (payload sem `organizationid`); REQ-035 (comportamento atual de aprovação pelo líder do projeto); REQ-054 (sem notificações); REQ-066 (sem filtro por tema); REQ-084 (sem multi-tenant).
- **Perfis (revisão de 16/11/2026):** o perfil Coordenador foi removido e suas permissões passaram ao Líder do grupo. Removidos REQ-005, 017 e 020; reescritos REQ-004, 006, 015, 019, 027, 028, 033, 036, 045, 071, 080 e 093. Coordenadores existentes são convertidos em Pesquisadores na inicialização (`RemocaoCoordenadorInitializer`, espelhado na migração V12). O pesquisador passou a poder concluir atividades sozinho.
- **Acesso (revisão de 16/11/2026):** só o Administrador adiciona membros a grupos (convites); projetos, participantes, atividades, publicações e solicitações só são lidos por quem tem acesso ao projeto; membros do grupo só pelo Líder do grupo; o Colaborador não cria nem altera nada. Incluído REQ-094.
- **Regras de escopo:** REQ-059 (lembrete de projeto ou do grupo inteiro) e REQ-074 (datas da notícia) reescritos; incluído REQ-095 (editais geridos pelo Líder do grupo, com datas de abertura e encerramento).
- **Solicitações (revisão de 16/11/2026):** a solicitação passa a ser criada pelo líder do projeto ou pelo Líder do grupo e aprovada em duas etapas, primeiro pelo Líder do grupo e depois pelo administrador; a do próprio Líder do grupo vai direto ao administrador. Reescritos REQ-039 e REQ-041; incluído REQ-096. A tela de solicitações passou a ficar disponível também para os integrantes dos grupos.
- **Incluído:** REQ-093 (visibilidade de editais, notícias e eventos por projeto).
- **Implementados nesta revisão:** REQ-009, REQ-060, REQ-069, REQ-071, REQ-084, REQ-088 e REQ-093.

## Detalhamento

| Req. | Situação | Onde / observação |
| :-- | :-: | :-- |
| REQ-001 | ✅ | `AdminInicialInitializer` cria o administrador inicial quando não há administradores |
| REQ-002 | ✅ | `AuthService.registerAdmin` exclui o administrador inicial após criar o definitivo |
| REQ-003 | ✅ | Administrador cria grupos, edita os que criou e cadastra líderes (`GrupoService`) |
| REQ-004 | ✅ | `ConviteService.validarPermissaoConviteGrupo`: só o administrador gera e envia convites de grupo |
| REQ-006 | ✅ | `GrupoAcessoService.validarPodeEscreverNoProjeto` e checagens de colaborador em atividades, solicitações, presença e favoritos |
| REQ-007 | ✅ | `credenciais_login.criador_id` |
| REQ-008 | ✅ | `Argon2PasswordEncoder` em `SecurityConfig` |
| REQ-009 | ✅ | `JwtUtil.generateToken`: claims `username`, `role` e `iat` (mais `accessLevel` e `permissoes`) |
| REQ-010 | ✅ | Administrador cria administradores (`/auth/register-admin`) e líderes |
| REQ-011 | ✅ | `GrupoService.validarPermissaoAdminCriador` e listagem filtrada pelo criador |
| REQ-012 | ✅ | `GrupoController` (CRUD) |
| REQ-013 | ✅ | `CreateGroupRequest.emailLider` obrigatório |
| REQ-014 | ✅ | Implantação por instância (`compose.yaml`); `Grupo` é a entidade de mais alto nível |
| REQ-015 | ✅ | `NivelMembro` (LIDER, PESQUISADOR, COLABORADOR) no grupo e `projetoxusuario.nivel = LIDER` para o líder de projeto |
| REQ-016 | ✅ | `GrupoJaPossuiLiderErro`; líder pode ter vários grupos |
| REQ-018 | ✅ | Idem para pesquisadores |
| REQ-019 | ✅ | Convite de grupo por link (`/convites/aceitar/grupo/{token}/cadastro`), gerado pelo administrador |
| REQ-021 | ✅ | Convite de líder gerado na criação do grupo |
| REQ-094 | ✅ | `GrupoAcessoService.podeAcessarProjeto` em `ProjetoFachada`, `AtividadeService`, `PublicacaoService` e `SolicitacaoService`; `GrupoService.listarPesquisadores` exige o Líder do grupo |
| REQ-022 | ✅ | `ProjetoController` (CRUD) |
| REQ-023 | ✅ | `atividade.projeto_id` |
| REQ-024 | ✅ | `GrupoService.criarProjetoInicial` |
| REQ-025 | ✅ | `ProjetoService` lança `ProjetoNaoPodeSerFinalizado` |
| REQ-026 | ✅ | `ProjetoStatus`: Planejado, Em andamento, Concluído, Encerrado (+ Cancelado) |
| REQ-027 | ✅ | `validarPodeGerenciarProjetos` exige o Líder do grupo |
| REQ-028 | ✅ | `projetoxusuario`; `ResponsavelNaoPertenceAoProjetoErro` |
| REQ-086 | ✅ | `projeto.is_favorito` |
| REQ-029 | ✅ | `AtividadeController` (CRUD) |
| REQ-030 | ✅ | `atividade.projeto_id` |
| REQ-031 | ✅ | `validarResponsavelNoProjeto` |
| REQ-032 | ✅ | `dth_registro`, `dth_conclusao`, `dth_prazo` |
| REQ-033 | ✅ | `AtividadeStatus`: Em andamento → Concluída direto ou via Aguardando confirmação; encerrar exige Líder do grupo ou do projeto |
| REQ-034 | ✅ | `isAtrasada` e destaque no quadro |
| REQ-035 | ✅ | Impedimentos como atividade filha; atividades de pesquisadores ficam aguardando aprovação do líder do projeto |
| REQ-036 | ✅ | `projetoPodeSerFinalizado` na resposta e modal no front-end, exibido ao Líder do grupo |
| REQ-037 | ✅ | `SolicitacaoController` (CRUD) |
| REQ-038 | ✅ | Tipos `uso_material`, `financiamento`, `compra_material_catalogado` (+ `compra_material`) |
| REQ-039 | ✅ | `SolicitacaoService.criar`: exige o líder do projeto (`isLiderDoProjeto`) ou o Líder do grupo; `projetoId` obrigatório |
| REQ-040 | ✅ | Título, descrição e justificativa obrigatórios |
| REQ-041 | ✅ | `SolicitacaoStatus`: Pendente (aguardando líder), Aguardando administrador, Em andamento, Concluída, Encerrada, Rejeitada |
| REQ-042 | ✅ | Aprovação de uso de material coloca o material Em uso e registra o empréstimo |
| REQ-096 | ✅ | `SolicitacaoService.aprovar`/`rejeitar`: Pendente → Líder do grupo; Aguardando administrador → administrador; `registrarAprovacaoDoLider` na criação pelo Líder do grupo; testes em `SolicitacaoFluxoIntegrationTest` |
| REQ-043 | ✅ | `MaterialController` (CRUD) |
| REQ-044 | ✅ | `CriarMaterialRequest` |
| REQ-045 | ✅ | `MaterialService` restringe ao Líder do grupo |
| REQ-046 | ✅ | `MaterialStatus` |
| REQ-047 | ✅ | `usuarioEmUsoNome` e `dthDevolucao` |
| REQ-048 | ✅ | Status efetivo calculado pela quantidade disponível |
| REQ-087 | ✅ | `PublicacaoController` |
| REQ-088 | ✅ | `publicacao.tipo` (20), `data_publicacao` e `link`; formulário e cartão em `Publicacoes.tsx` |
| REQ-089 | ✅ | PDF em `documento` (ou S3) |
| REQ-049 | ✅ | `LembreteController` (CRUD) |
| REQ-050 | ✅ | `TipoLembrete` |
| REQ-051 | ✅ | `dataHora` obrigatório |
| REQ-053 | ✅ | Título, descrição, link, localização, participantes e organizador |
| REQ-054 | ✅ | Listagem por escopo e confirmação de presença (`/lembretes/{id}/confirmar-presenca`) |
| REQ-055 | ✅ | `CreateLembreteRequest` |
| REQ-056 | ✅ | `POST /lembretes/{id}/ata`, restrito ao criador |
| REQ-057 | ✅ | `validarCriadorOuAdmin` |
| REQ-058 | ✅ | Abas Em andamento, Próximos e Encerrados em `Lembretes.tsx` |
| REQ-059 | ✅ | `LembreteService.resolverProjetoDoLembrete`: grupo inteiro exige o Líder do grupo; projeto exige o Líder do grupo ou do projeto |
| REQ-060 | ✅ | `grupo_palavra_chave`; `GET/PUT /grupos/{id}/editais/palavras-chave`; filtro `somenteRelevantes` |
| REQ-063 | ✅ | Filtros por palavra-chave, organização e datas |
| REQ-066 | ✅ | Filtros de eventos por nome e data (além do tipo) |
| REQ-090 | ✅ | `TipoRecorrencia` |
| REQ-093 | ✅ | Consultas de editais, notícias e lembretes filtram por participação no projeto; detalhe retorna erro para quem não tem acesso |
| REQ-095 | ✅ | `EditalService.validarPodeGerenciar` (Líder do grupo); `dthAbertura` e `dthEncerramento` obrigatórios |
| REQ-067 | ✅ | `Calendario.tsx` com tipo e subtipo |
| REQ-068 | ✅ | Lista dos próximos 30 dias |
| REQ-069 | ✅ | Link de assinatura iCalendar (`/calendario/feed/{token}`) com atalhos para Google Calendar e Outlook |
| REQ-070 | ✅ | `SolarSystem.tsx` com projetos favoritos |
| REQ-071 | ✅ | `Dashboard.tsx`: o Líder do grupo vê todos os destaques; demais, só os projetos de que participam |
| REQ-074 | ✅ | `CreateNoticiaRequest` com `dataInicioExibicao` e `dataFimExibicao` |
| REQ-077 | ✅ | Filtros por assunto e data |
| REQ-078 | ✅ | `dataFimExibicao` e prazo padrão (`prazo-exibicao-dias`) |
| REQ-079 | ✅ | `LinkPreviewService` (Jsoup) |
| REQ-080 | ✅ | Notícias de pesquisadores ficam pendentes até aprovação do Líder do grupo ou do líder do projeto rotulado |
| REQ-081 | ✅ | React |
| REQ-082 | ✅ | Java 25 e Spring Boot 4 |
| REQ-083 | ✅ | Recursos REST com verbos e códigos HTTP |
| REQ-084 | ✅ | `postgres:18-alpine` em `compose.yaml`; esquema gerado pelas entidades |
| REQ-091 | ✅ | Edição/remoção de pesquisadores e grupos pelo administrador |
| REQ-092 | ✅ | `grupoxusuario.funcao` |

## Observações

- **REQ-060:** a relevância é calculada por substring (descrição ou organização do edital contém a palavra-chave, sem diferenciar maiúsculas). Palavras muito curtas, como "IA", geram falsos positivos (ex.: "Araucária").
- **REQ-069:** Google Calendar e Outlook só conseguem assinar o link quando a API está publicada em um endereço acessível pela internet (`API_PUBLIC_URL`). Em `localhost`, o link funciona apenas em aplicativos da própria máquina. O link é revalidado a cada consulta e deixa de funcionar se o usuário for desativado ou sair do grupo; não há revogação individual de links.
- **REQ-071:** a filtragem é feita no front-end a partir dos participantes de cada projeto em destaque.
