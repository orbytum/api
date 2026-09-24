# Fase 1 — Domínio e Autorização

**Pré-requisito:** nenhum.
**Requisitos:** REQ-015, REQ-017, REQ-028, REQ-030, REQ-031, REQ-032, REQ-035, REQ-092.

Esta fase cria as fundações (enum de nível, vínculo, participantes de projeto, entidade de atividade corrigida e status) e o serviço central de autorização usado pelas fases seguintes.

---

## T1.1 — Enum `NivelMembro` e vínculo de grupo

**Requisitos:** REQ-015, REQ-017, REQ-092

**Descrição:** representar a hierarquia no vínculo `GrupoXUsuario` e no convite.

**Arquivos:**
- Criar: `src/main/java/com/orbytum/api/models/enums/NivelMembro.java`
- Modificar: `src/main/java/com/orbytum/api/models/entity/joinColumns/GrupoXUsuario.java`
- Modificar: `src/main/java/com/orbytum/api/models/entity/ConviteGrupo.java`
- Modificar: `src/main/java/com/orbytum/api/models/entity/Role.java`

**Passos:**
1. Criar enum `NivelMembro { LIDER, COORDENADOR, PESQUISADOR }`.
2. Em `GrupoXUsuario`, adicionar:
   - `@Convert(converter = NivelMembroAttributeConverter.class) private NivelMembro nivel;` (ou `@Enumerated(EnumType.STRING)`).
   - `@Column(length = 20) private String funcao;` (REQ-092).
3. Ajustar construtores de `GrupoXUsuario` para receber `nivel` (default `PESQUISADOR`) e manter compatibilidade dos construtores atuais.
4. Em `ConviteGrupo`, adicionar `nivel` (define o nível de quem aceitar o convite).
5. Em `Role`, manter `isLider` derivado (`nivel == LIDER`) ou adicionar `nivel` — escolher uma única fonte de verdade e documentar.

**Critérios de aceite:**
- [ ] Existe `NivelMembro` com os três níveis.
- [ ] `GrupoXUsuario` possui `nivel` e `funcao`.
- [ ] `ConviteGrupo` carrega o `nivel` a ser atribuído.
- [ ] Código compila e testes existentes continuam passando.

---

## T1.2 — Serviço central de autorização (`GrupoAcessoService`)

**Requisitos:** REQ-015

**Descrição:** concentrar as regras de hierarquia e vínculo, hoje espalhadas (ex.: `ConviteService.isLiderRole:446`, `GrupoService.validarPermissaoAdminCriador:433`).

**Arquivos:**
- Criar: `src/main/java/com/orbytum/api/service/GrupoAcessoService.java`
- Reutilizar: `GrupoXUsuarioRepository`, `ProjetoXUsuarioRepository` (T1.3), `CredenciaisLoginService`

**Passos:**
1. Implementar:
   - `boolean isMembro(Long grupoId, Long usuarioId)`
   - `boolean isLider(Long grupoId, Long usuarioId)`
   - `boolean isCoordenadorOuAcima(Long grupoId, Long usuarioId)`
   - `NivelMembro nivelDoUsuario(Long grupoId, Long usuarioId)`
   - `void validarMembro(Long grupoId, Long usuarioId)`
   - `void validarLider(Long grupoId, Long usuarioId)`
   - `void validarCoordenadorOuAcima(Long grupoId, Long usuarioId)`
   - `void validarMembroDoProjeto(Long projetoId, Long usuarioId)`
2. Lançar exceção de negócio dedicada (criar `SemPermissaoNoGrupoErro` em `models/exceptions`).

**Critérios de aceite:**
- [ ] Serviço cobre membro/líder/coordenador/membro-do-projeto.
- [ ] Existe exceção de negócio mapeada no `GlobalExceptionHandler`.

---

## T1.3 — Participantes de projeto (`ProjetoXUsuario`)

**Requisitos:** REQ-028, REQ-031

**Arquivos:**
- Criar: `src/main/java/com/orbytum/api/models/entity/joinColumns/ProjetoXUsuario.java`
- Criar: `src/main/java/com/orbytum/api/repository/ProjetoXUsuarioRepository.java`
- Modificar: `src/main/java/com/orbytum/api/models/entity/Projeto.java`

**Passos:**
1. Entidade `ProjetoXUsuario` com `id` (UUID ou Long), `projeto`, `usuario`, `nivel`, `isAtivo`.
2. Repository com:
   - `boolean existsByProjetoIdAndUsuarioIdAndIsAtivoTrue(Long, Long)`
   - `Optional<ProjetoXUsuario> findByProjetoIdAndUsuarioIdAndIsAtivoTrue(Long, Long)`
   - `List<ProjetoXUsuario> findAllByProjetoIdAndIsAtivoTrue(Long)`
3. Em `Projeto`, adicionar `@OneToMany(mappedBy = "projeto") List<ProjetoXUsuario> participantes`.

**Critérios de aceite:**
- [ ] Tabela/entidade de participantes criada.
- [ ] É possível verificar se um usuário participa de um projeto.

---

## T1.4 — Corrigir entidade `Atividade`

**Requisitos:** REQ-030, REQ-031, REQ-032, REQ-035

**Arquivos:**
- Modificar: `src/main/java/com/orbytum/api/models/entity/Atividade.java`

**Passos:**
1. Adicionar Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`).
2. Aplicar `@Convert(converter = AtividadeStatusAttributeConverter.class)` no `status`.
3. Tornar `dthConclusao` **nullable** (`LocalDateTime`).
4. Adicionar `@ManyToOne @JoinColumn(name = "responsavel_id") private Usuario responsavel;`.
5. Adicionar auto-relacionamento de impedimento:
   - `@ManyToOne @JoinColumn(name = "atividade_pai_id") private Atividade atividadePai;`
   - `@OneToMany(mappedBy = "atividadePai") private List<Atividade> atividadesFilhas;`
6. Definir `dthPrazo` como data de **entrega** (obrigatória) e `dthRegistro` como data de criação (automática no construtor).
7. Adicionar construtor de negócio que inicializa `dthRegistro = LocalDateTime.now()`, `isAtivo = true` e `status = PENDENTE`.

**Critérios de aceite:**
- [ ] Entidade tem getters/setters e converter de status.
- [ ] `dthConclusao` aceita nulo.
- [ ] Existem `responsavel` e `atividadePai`.

---

## T1.5 — Novo `AtividadeStatus` e transições

**Requisitos:** REQ-033

**Arquivos:**
- Modificar: `src/main/java/com/orbytum/api/models/enums/AtividadeStatus.java`
- Modificar: `src/main/java/com/orbytum/api/models/converter/AtividadeStatusAttributeConverter.java`
- Criar (opcional): `src/main/java/com/orbytum/api/models/enums/AtividadeStatusTransicao.java` (ou método no service)

**Passos:**
1. Substituir valores por `PENDENTE(1), EM_ANDAMENTO(2), AGUARDANDO_CONFIRMACAO(3), CONCLUIDA(4), ENCERRADA(5)`.
2. Manter `fromId`/`getId` para o converter.
3. Implementar validação de transição permitida:
   - `PENDENTE → EM_ANDAMENTO`
   - `EM_ANDAMENTO → AGUARDANDO_CONFIRMACAO`
   - `AGUARDANDO_CONFIRMACAO → CONCLUIDA`
   - `CONCLUIDA → ENCERRADA`
   - (definir se há retorno/estorno; documentar)
4. Tratar dados legados no script de migração (Fase 4).

**Critérios de aceite:**
- [ ] Enum com os 5 status do requisito.
- [ ] Método `podeTransicionarPara(AtividadeStatus)` disponível.
- [ ] Transições inválidas lançam exceção de negócio (`TransicaoAtividadeInvalidaErro`).

---

## Checklist de saída da Fase 1

- [ ] `NivelMembro`, `funcao`, `nivel` em vínculo/convite.
- [ ] `GrupoAcessoService` implementado e testável.
- [ ] `ProjetoXUsuario` + repository.
- [ ] `Atividade` corrigida (datas, responsável, hierarquia, converter).
- [ ] `AtividadeStatus` novo + transições.
- [ ] `./gradlew test` verde (com testes ajustados quando necessário).
