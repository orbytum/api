# Fase 2 — Grupos de Pesquisa

**Pré-requisito:** Fase 1.
**Requisitos:** REQ-012, REQ-013, REQ-016, REQ-017, REQ-019, REQ-020, REQ-021, REQ-004, REQ-005, REQ-007, REQ-024, REQ-027, REQ-028, REQ-086, REQ-091, REQ-092.

Fecha os gaps de grupos e as dependências de Projetos.

---

## T2.1 — Líder obrigatório e único por grupo

**Requisitos:** REQ-013, REQ-016

**Arquivos:**
- Modificar: `src/main/java/com/orbytum/api/models/dto/request/CreateGroupRequest.java`
- Modificar: `src/main/java/com/orbytum/api/service/GrupoService.java` (`criarGrupo:99`, `cadastrarLider:185`)
- Modificar: `src/main/java/com/orbytum/api/repository/GrupoXUsuarioRepository.java`

**Passos:**
1. `CreateGroupRequest` passa a exigir identificação do líder (`emailLider` obrigatório ou `liderId`). Decidir: manter `emailLider` obrigatório para não quebrar contrato, ou aceitar `liderId`.
2. `criarGrupo`:
   - Validar presença do líder; lançar `IllegalArgumentException`/exceção dedicada se ausente.
   - Se o e-mail não existir, gerar convite de grupo com `nivel = LIDER` e `limiteUso = 1` (REQ-021).
   - Se existir, vincular como LIDER (respeitando T2.3).
3. Adicionar no repository `existsByGrupoIdAndNivelAndIsAtivoTrue(Long, NivelMembro)` e `countByGrupoIdAndNivelAndIsAtivoTrue`.
4. Em `cadastrarLider`, bloquear segundo LÍDER ativo: `if (existeLiderAtivo) throw new GrupoJaPossuiLiderErro(...)`.

**Critérios de aceite:**
- [ ] Não é possível criar grupo sem líder.
- [ ] Não é possível ter dois LÍDER ativos no mesmo grupo.
- [ ] Líder pode pertencer a vários grupos.

---

## T2.2 — Autocadastro por convite de grupo

**Requisitos:** REQ-019, REQ-020, REQ-021, REQ-004, REQ-005, REQ-007

**Arquivos:**
- Modificar: `src/main/java/com/orbytum/api/service/ConviteService.java`
- Modificar: `src/main/java/com/orbytum/api/controller/ConviteController.java`
- Modificar: `src/main/java/com/orbytum/api/service/GrupoService.java`
- Criar: DTO(s) de resposta/registro conforme necessário

**Passos:**
1. **Permissões de geração de convite** (usar `GrupoAcessoService`):
   - Convite de **PESQUISADOR**: Líder ou Coordenador.
   - Convite de **COORDENADOR**: apenas Líder.
   - Convite de **LÍDER**: apenas Administrador (via criação de grupo).
   - Substituir `validarPermissaoConviteGrupo:427` e `isLiderRole:446`.
2. Definir `nivel` do convite em `GerarConviteGrupoRequest`/`EnviarConviteRequest` (ou derivar do `idRole`).
3. **Aceite público (autocadastro):** criar `POST /convites/aceitar/grupo/{token}/cadastro` que:
   - Recebe `RegisterRequest` (nome, telefone, titulo, senha).
   - Cria `Usuario` + `CredenciaisLogin` (`AccessLevel.USER`) com `criador = convite.usuarioRemetente` (REQ-007).
   - Cria `GrupoXUsuario` com `nivel` do convite e `isAtivo = true`.
   - Incrementa usos / desativa o convite conforme `limiteUso`.
   - Retorna `AuthResponse` (login automático).
4. Manter `POST /convites/aceitar/grupo/{token}` (autenticado) para usuários já existentes, agora atribuindo `nivel`.
5. Permitir no `SecurityConfig` a rota pública `/convites/aceitar/grupo/*/cadastro` (POST).

**Critérios de aceite:**
- [ ] Líder convida pesquisador e coordenador; Coordenador convida pesquisador.
- [ ] Convite de líder só é gerado no fluxo administrativo.
- [ ] Novo usuário consegue se cadastrar e entrar no grupo pelo link.
- [ ] `CredenciaisLogin.criador` preenchido no autocadastro.

---

## T2.3 — Projeto inicial automático + finalização

**Requisitos:** REQ-024, REQ-025

**Arquivos:**
- Modificar: `src/main/java/com/orbytum/api/service/GrupoService.java` (`criarGrupo`)
- Modificar: `src/main/java/com/orbytum/api/service/ProjetoService.java`

**Passos:**
1. Em `criarGrupo`, após salvar o grupo, criar `Projeto(grupo, ProjetoStatus.PLANEJADO, "Projeto Inicial", grupo.getNome(), isInicial = true)`.
2. Garantir que o projeto inicial **não** seja criado em duplicidade.
3. Expor finalização explícita (usada em REQ-036): `ProjetoService.finalizarProjeto(Projeto)` que reutiliza a regra de REQ-025 (`delete:44-56`), transicionando para `ENCERRADO` em vez de apenas inativar.

**Critérios de aceite:**
- [ ] Todo grupo novo nasce com um projeto inicial (`isInicial = true`).
- [ ] Projeto inicial só finaliza se não houver outros projetos abertos.

---

## T2.4 — Permissão de criação de projeto + participantes

**Requisitos:** REQ-027, REQ-028

**Arquivos:**
- Modificar: `src/main/java/com/orbytum/api/fachada/ProjetoFachada.java` (`criarProjeto:26`)
- Modificar: `src/main/java/com/orbytum/api/controller/ProjetoController.java`
- Criar: `ProjetoParticipanteController` (ou endpoints em `ProjetoController`)
- Criar: DTOs de participante

**Passos:**
1. Em `criarProjeto`, validar que o solicitante é Líder ou Coordenador do grupo do projeto (`GrupoAcessoService.validarCoordenadorOuAcima`).
2. Adicionar o criador como participante (`ProjetoXUsuario`) automaticamente.
3. Endpoints:
   - `GET /projetos/{id}/participantes`
   - `POST /projetos/{id}/participantes` (adiciona usuário do grupo)
   - `DELETE /projetos/{id}/participantes/{usuarioId}` (inativa vínculo)
4. Validar que só membros do **grupo** podem ser adicionados como participantes.

**Critérios de aceite:**
- [ ] Apenas Líder/Coordenador cria projeto.
- [ ] Criador vira participante automaticamente.
- [ ] É possível listar/adicionar/remover participantes.

---

## T2.5 — `isFavorite` (projetos em destaque)

**Requisitos:** REQ-086

**Arquivos:**
- Modificar: `src/main/java/com/orbytum/api/models/dto/response/ProjetoResponse.java`
- Modificar: `src/main/java/com/orbytum/api/fachada/ProjetoFachada.java` (`toResponse:75`)
- Modificar: `src/main/java/com/orbytum/api/controller/ProjetoController.java`

**Passos:**
1. Expor `isFavorito` no `ProjetoResponse`.
2. Criar `PATCH /projetos/{id}/favorito` (toggle) permitido a membros do grupo.
3. (Opcional) `GET /projetos?favoritos=true` para o dashboard.

**Critérios de aceite:**
- [ ] `isFavorito` retornado na API.
- [ ] Endpoint de toggle funcional e restrito a membros.

---

## T2.6 — Edição de pesquisador com `funcao`/`nivel`

**Requisitos:** REQ-091, REQ-092

**Arquivos:**
- Modificar: `src/main/java/com/orbytum/api/models/dto/request/EditLeaderRequest.java` (ou criar `EditPesquisadorRequest`)
- Modificar: `src/main/java/com/orbytum/api/service/GrupoService.java` (`atualizarPesquisador:346`)
- Modificar: `src/main/java/com/orbytum/api/models/dto/response/PesquisadorResponse.java`

**Passos:**
1. Permitir atualizar `funcao` e, se aplicável, `nivel` do vínculo.
2. Expor `funcao` no `PesquisadorResponse`.
3. Manter restrição ao Administrador criador do grupo.

**Critérios de aceite:**
- [ ] Admin consegue editar `funcao` do pesquisador.
- [ ] `funcao` aparece nas listagens.

---

## Checklist de saída da Fase 2

- [ ] Líder obrigatório e único (REQ-013/016).
- [ ] Convites por nível e autocadastro público (REQ-019/020/021).
- [ ] Projeto inicial automático (REQ-024).
- [ ] Criação de projeto restrita + participantes (REQ-027/028).
- [ ] `isFavorito` exposto (REQ-086).
- [ ] Edição de pesquisador com `funcao` (REQ-091/092).
- [ ] `./gradlew test` verde.
