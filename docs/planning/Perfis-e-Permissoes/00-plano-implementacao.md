# Perfis, Permissões e Fluxos — Plano de Implementação

**Data:** 09/11/2026

**Documento base:** `docs/20260630_requisitos.md`

**Objetivo:** evoluir o back-end para os quatro perfis propostos (Líder de Grupo, Líder de Projeto, Pesquisador e Colaborador), fechar os fluxos de aprovação, escopo de lembretes e rótulos de editais/notícias, mantendo o padrão existente (`controller` → `service`/`fachada` → `repository`), com a menor mudança possível.

---

## 1. Resumo executivo

| Ideia / requisito | Situação atual | O que fazer |
| :-- | :-- | :-- |
| Quatro perfis | 🟡 Parcial. Existem `LIDER`, `COORDENADOR` e `PESQUISADOR` em `NivelMembro`. O líder de projeto já é representável por `ProjetoXUsuario.nivel = LIDER`, mas não há regra explícita. Não existe colaborador. | Adicionar `COLABORADOR(0)` em `NivelMembro` e centralizar as regras em `GrupoAcessoService`. Não criar novo enum de perfil. |
| Líder de grupo com todas as permissões e CRUD de editais | 🟡 O líder de grupo já é reconhecido. Editais hoje aceitam `COORDENADOR` ou acima. | Manter bypass de líder/admin nos serviços e restringir escrita de edital a líder de grupo/admin. |
| Líder de projeto | 🟡 `ProjetoXUsuario.nivel` já permite `LIDER`, mas qualquer coordenador pode definir e não há unicidade nem endpoint semântico. | Reutilizar `ProjetoXUsuario.nivel = LIDER` como fonte única. Adicionar `liderProjetoId` opcional no create do projeto e validar/definir via participantes. |
| Aprovar/rejeitar atividades | ❌ Atividade nasce `PENDENTE` e já pode transitar; não existe estado de aprovação nem endpoints. | Adicionar `AGUARDANDO_APROVACAO` e `REJEITADA`, endpoints de aprovar/rejeitar e regra de criação por perfil. |
| Colaborador somente leitura | ❌ Não existe. | Adicionar `COLABORADOR` e bloquear todas as escritas no escopo do grupo/projeto. |
| Lembrete vinculado a projeto ou grupo inteiro | ❌ `Evento` só referencia `Grupo`. | Adicionar `projeto_id` opcional, `paraGrupoInteiro` no DTO e validar que só líder de grupo cria para o grupo inteiro. |
| Rótulos (projetos) em editais e notícias | ❌ Não existe. | Adicionar relação N:N com `Projeto` em `Edital` e `Noticia`, com validação de grupo e filtro por projeto. |
| Datas inicial e final em editais | ✅ Já implementado. `Edital` possui `dthAbertura` e `dthEncerramento`, obrigatórios e validados. | Nenhuma mudança de domínio. Apenas garantir o contrato no front-end. |
| Datas inicial e final em notícias | ✅ Já implementado. `Noticia` possui `dataInicioExibicao` e `dataFimExibicao`; a data final é materializada na aprovação quando ausente. | Nenhuma mudança obrigatória. Se o produto exigir ambas obrigatórias, alterar validação. |
| CRUD de publicações | 🟡 Existe criar/listar/buscar/inativar. Não existe edição. Não há nenhuma validação de acesso no `PublicacaoService`. | Adicionar `PUT /publicacoes/{id}`, validar grupo/projeto e restringir escrita a líder de projeto/líder de grupo/admin. |
| Aprovação de notícia por líder de projeto | 🟡 Hoje aprovação é só `COORDENADOR` ou acima. | Com os rótulos, permitir que o líder de qualquer projeto rotulado aprove/rejeite a notícia. |

### Decisão geral de modelagem

**Não criar um novo enum `Perfil` nem uma nova entidade de perfil.** O projeto já usa `NivelMembro` no vínculo do grupo (`GrupoXUsuario`) e no vínculo do projeto (`ProjetoXUsuario`). Reutilizar esse enum é a menor mudança e mantém compatibilidade com convites, listagens e regras atuais.

- **Líder de grupo:** `GrupoXUsuario.nivel == LIDER`.
- **Líder de projeto:** `ProjetoXUsuario.nivel == LIDER`.
- **Pesquisador:** `NivelMembro.PESQUISADOR` no grupo e/ou projeto.
- **Colaborador:** novo `NivelMembro.COLABORADOR`, no grupo e/ou projeto.
- **Administrador:** continua sendo `AccessLevel.ADMIN` e fica fora dos quatro perfis, com bypass.

> O `COORDENADOR` atual **não será removido** nesta entrega para não quebrar dados e testes existentes. Ele permanece como perfil legado de gestão do grupo. Se o produto não quiser mais expô-lo, o front-end deixa de oferecê-lo e uma migração futura converte os vínculos.

---

## 2. Matriz de permissões proposta

| Ação | Líder de Grupo | Líder de Projeto | Pesquisador | Colaborador |
| :-- | :--: | :--: | :--: | :--: |
| Visualizar grupo, projetos, atividades, notícias, editais, lembretes e publicações | ✅ | ✅ | ✅ | ✅ |
| Criar/editar/excluir edital | ✅ | ❌ | ❌ | ❌ |
| Criar/editar/excluir projeto e participantes | ✅ | ❌ | ❌ | ❌ |
| Definir líder do projeto | ✅ | ❌ | ❌ | ❌ |
| Criar/editar/excluir atividade | ✅ | ✅ | ❌ | ❌ |
| Criar atividade para aprovação | ✅ (já aprovada) | ✅ (já aprovada) | ✅ (nasce aguardando aprovação) | ❌ |
| Aprovar/rejeitar atividade | ✅ | ✅ | ❌ | ❌ |
| Mover atividade para “a fazer”, “fazendo” | ✅ | ✅ | ✅ | ❌ |
| Concluir/encerrar atividade | ✅ | ✅ | ❌ | ❌ |
| Criar lembrete para o grupo inteiro | ✅ | ❌ | ❌ | ❌ |
| Criar lembrete para um projeto | ✅ | ✅ | ❌ | ❌ |
| Visualizar/confirmar presença em lembrete | ✅ | ✅ | ✅ | ✅ (conforme escopo) |
| Criar notícia | ✅ | ✅ | ✅ | ❌ |
| Aprovar/rejeitar notícia | ✅ | ✅ (projetos rotulados) | ❌ | ❌ |
| Criar/editar/excluir publicação | ✅ | ✅ | ❌ | ❌ |
| Favoritar projeto | ✅ | ✅ | ✅ | ✅ (preferência pessoal) |

**Regras de escopo:**

1. `COLABORADOR` no grupo é um teto global de leitura: mesmo que tenha vínculo de projeto, não escreve.
2. `COLABORADOR` no projeto é leitura apenas naquele projeto.
3. Líder de grupo e administrador ignoram o vínculo de projeto e podem tudo no grupo.
4. Líder de projeto gerencia apenas o próprio projeto; não gerencia editais nem lembretes do grupo inteiro.
5. Pesquisador só escreve no projeto do qual participa e não pode aprovar.
6. Um colaborador não pode ser definido como líder de projeto.

---

## 3. Autorização central — `GrupoAcessoService`

**Arquivo:** `src/main/java/com/orbytum/api/service/GrupoAcessoService.java`

Hoje o serviço já centraliza membro, líder, coordenador e membro do projeto. A menor mudança é acrescentar métodos de leitura/validação sem duplicar regras nos services.

### 3.1 Métodos sugeridos

```java
public boolean isColaborador(Long grupoId, Long usuarioId)

public boolean isLiderDoProjeto(Long projetoId, Long usuarioId)

public boolean podeGerenciarProjeto(Long projetoId, Long usuarioId)

public void validarPodeGerenciarProjeto(Long projetoId, Long usuarioId)

public void validarNaoColaborador(Long grupoId, Long usuarioId)
```

### 3.2 Regra de `podeGerenciarProjeto`

```text
admin                         -> true
grupo LIDER ou COORDENADOR    -> true
grupo COLABORADOR             -> false
projeto LIDER                 -> true
demais                        -> false
```

### 3.3 Regra de escrita do pesquisador

```text
admin                         -> permitido
grupo COLABORADOR             -> negado
projeto COLABORADOR           -> negado
membro do projeto             -> permitido (cria atividade pendente, move status, etc.)
```

### 3.4 Repositório

**Arquivo:** `src/main/java/com/orbytum/api/repository/ProjetoXUsuarioRepository.java`

Adicionar:

```java
Optional<ProjetoXUsuario> findFirstByProjetoIdAndNivelAndIsAtivoTrue(Long projetoId, NivelMembro nivel);

List<ProjetoXUsuario> findAllByProjetoIdAndNivelAndIsAtivoTrue(Long projetoId, NivelMembro nivel);

long countByProjetoIdAndNivelAndIsAtivoTrue(Long projetoId, NivelMembro nivel);
```

---

## 4. Caso: mecanismo para definir líder do projeto

### 4.1 Situação atual

`ProjetoXUsuario` já possui `nivel`. O endpoint `POST /projetos/{id}/participantes` já aceita `nivel`, inclusive `LIDER`. Porém:

- qualquer `COORDENADOR` ou acima pode definir;
- não há garantia de um único líder;
- não há regra explícita de quem pode ser líder;
- a criação do projeto adiciona o criador com o nível do grupo, mas não permite escolher outro líder.

### 4.2 Solução recomendada (menor mudança)

**Não criar coluna `lider_id` em `Projeto`.** Usar `ProjetoXUsuario.nivel = LIDER` como fonte única de verdade.

1. Adicionar `Long liderProjetoId` opcional em `CreateProjetoRequest`.
   - Se ausente, o criador continua sendo o líder do projeto (comportamento atual).
   - Se informado, validar que é membro do grupo e não é colaborador.
2. Em `ProjetoFachada.criarProjeto`, criar o vínculo `ProjetoXUsuario` do líder com `NivelMembro.LIDER`.
3. Em `ProjetoFachada.adicionarParticipante`, quando `request.nivel() == LIDER`:
   - exigir `grupoAcessoService.validarLider(grupoId, solicitante.getId())` (ou admin);
   - rebaixar o líder anterior para `PESQUISADOR` e salvar;
   - impedir que o alvo seja `COLABORADOR` no grupo;
   - criar/atualizar o vínculo do novo líder.
4. Em `removerParticipante`, se o removido for o líder do projeto:
   - recomendação simples: permitir a remoção e deixar o projeto sem líder até que o líder do grupo defina outro. O líder do grupo continua podendo gerenciar.
   - alternativa mais rígida: bloquear a remoção enquanto for líder. Fica como decisão de produto.
5. Expor no `ProjetoResponse`:
   - `liderProjetoId`
   - `liderProjetoNome`

### 4.3 Endpoint

Não é obrigatório criar rota nova. O front-end pode usar a rota existente:

```http
POST /projetos/{id}/participantes
{
  "usuarioId": 123,
  "nivel": "LIDER"
}
```

Se for desejável uma rota semântica, criar apenas um alias:

```http
PUT /projetos/{id}/lider
{
  "usuarioId": 123
}
```

O alias deve chamar o mesmo método de serviço, sem duplicar regra.

### 4.4 Arquivos afetados

- `models/dto/request/CreateProjetoRequest.java`
- `models/dto/response/ProjetoResponse.java`
- `fachada/ProjetoFachada.java`
- `repository/ProjetoXUsuarioRepository.java`
- `service/GrupoAcessoService.java`
- `controller/ProjetoController.java` (somente se criar o alias)

### 4.5 Critérios de aceite

- [ ] É possível definir exatamente um líder ativo por projeto.
- [ ] Somente líder de grupo/admin define líder de projeto.
- [ ] Colaborador não pode ser líder.
- [ ] `GET /projetos/{id}` retorna o líder atual.
- [ ] Código existente que cria projeto continua funcionando sem informar `liderProjetoId`.

---

## 5. Caso: aprovar/rejeitar atividades

### 5.1 Situação atual

- `AtividadeService.criar` cria direto como `PENDENTE`.
- Atividade de topo exige `COORDENADOR` ou acima.
- Pesquisador só cria atividade filha (impedimento) e ela também nasce `PENDENTE`.
- Fluxo existente: `PENDENTE → EM_ANDAMENTO → AGUARDANDO_CONFIRMACAO → CONCLUIDA → ENCERRADA`.
- Não existe estado “aguardando aprovação” nem endpoints de aprovar/rejeitar.

### 5.2 Modelo de status recomendado

Em `AtividadeStatus`, acrescentar dois valores **sem renumerar os existentes**:

```java
AGUARDANDO_APROVACAO(0),
PENDENTE(1),
EM_ANDAMENTO(2),
AGUARDANDO_CONFIRMACAO(3),
CONCLUIDA(4),
ENCERRADA(5),
REJEITADA(6);
```

Transições:

```text
AGUARDANDO_APROVACAO -> PENDENTE   (aprovar)
AGUARDANDO_APROVACAO -> REJEITADA  (rejeitar)
PENDENTE             -> EM_ANDAMENTO
EM_ANDAMENTO         -> AGUARDANDO_CONFIRMACAO
AGUARDANDO_CONFIRMACAO -> CONCLUIDA
CONCLUIDA            -> ENCERRADA
```

`isAberta()` deve considerar `AGUARDANDO_APROVACAO` aberta e `REJEITADA` fechada:

```java
return this != CONCLUIDA && this != ENCERRADA && this != REJEITADA;
```

Em `AtividadeService.STATUS_ABERTOS`, incluir `AGUARDANDO_APROVACAO`. Assim o projeto não pode ser finalizado enquanto houver atividade pendente de aprovação.

### 5.3 Regra de criação

```text
Administrador / Líder de grupo / Coordenador / Líder do projeto
    -> cria com status PENDENTE

Pesquisador (membro do projeto, não colaborador)
    -> cria com status AGUARDANDO_APROVACAO

Colaborador
    -> negado
```

Para atender “pesquisador cria atividades a serem aprovadas”, a criação de atividade **de topo** pelo pesquisador deve ser liberada. Isso conflita com a regra antiga REQ-035 (“pesquisador só cria atividade filha”); a recomendação é que a nova regra prevaleça para atividades de topo pendentes, mantendo a regra de impedimento para atividades filhas.

### 5.4 Regra de edição/exclusão

Trocar as validações atuais de `validarCoordenadorOuAcima` por `validarPodeGerenciarProjeto` em:

- `AtividadeService.atualizar`
- `AtividadeService.deletar`
- `AtividadeService.atualizarStatus` (somente para `CONCLUIDA`/`ENCERRADA`)

Assim o líder de projeto passa a gerenciar o próprio projeto sem depender de nível de grupo.

### 5.5 Endpoints novos

```http
PATCH /atividades/{id}/aprovar
PATCH /atividades/{id}/rejeitar
```

Corpo opcional do rejeitar:

```json
{
  "motivo": "Faltou detalhar o escopo"
}
```

Regras:

- Só permite quando `status == AGUARDANDO_APROVACAO`.
- Autorização: administrador, líder de grupo/coordenador ou líder do projeto.
- Aprovar: status `PENDENTE`.
- Rejeitar: status `REJEITADA`.
- Se não quiser criar campo de motivo agora, rejeitar sem corpo. Para manter simplicidade, o campo `motivoRejeicao` em `Atividade` é **opcional** e pode entrar em uma segunda etapa.

### 5.6 Resposta

Adicionar em `AtividadeResponse` apenas o necessário:

- `motivoRejeicao` (se implementado)
- `podeSerAprovada` (opcional, calculado por perfil) — evita lógica duplicada no front.

Não é necessário adicionar `aprovadorId`/`dthAprovacao` nesta entrega; o status já resolve o fluxo. Se auditoria for requisito, adicionar depois.

### 5.7 Arquivos afetados

- `models/enums/AtividadeStatus.java`
- `service/AtividadeService.java`
- `controller/AtividadeController.java`
- `models/dto/response/AtividadeResponse.java`
- `models/dto/request/RejeitarAtividadeRequest.java` (se houver motivo)
- `models/entity/Atividade.java` (somente se houver motivo)
- `configuration/exception/GlobalExceptionHandler.java` (reutilizar `TransicaoAtividadeInvalidaErro`; não é necessário novo handler)

### 5.8 Critérios de aceite

- [ ] Atividade criada por pesquisador nasce `AGUARDANDO_APROVACAO`.
- [ ] Atividade criada por líder de grupo/projeto nasce `PENDENTE`.
- [ ] Líder de projeto aprova/rejeita atividade do próprio projeto.
- [ ] Pesquisador e colaborador não aprovam.
- [ ] Atividade rejeitada não conta como aberta.
- [ ] Atividade aguardando aprovação impede finalização do projeto.
- [ ] Front-end consegue distinguir “aguardando aprovação” de “pendente”.

---

## 6. Caso: colaborador somente leitura

### 6.1 Alteração de domínio

Em `NivelMembro`:

```java
COLABORADOR(0),
LIDER(3),
COORDENADOR(2),
PESQUISADOR(1);
```

A prioridade `0` faz `isPeloMenos` continuar funcionando sem alteração.

### 6.2 Onde aplicar o bloqueio

Como o colaborador continua sendo **membro** do grupo, `validarMembro` deve continuar permitindo leitura. É preciso adicionar `validarNaoColaborador` (ou `validarPodeInteragir`) nos fluxos de escrita.

| Fluxo | Arquivo | Ação |
| :-- | :-- | :-- |
| Criar lembrete | `LembreteService.criar` | bloquear colaborador |
| Criar notícia | `NoticiaService.criar` | bloquear colaborador |
| Criar atividade filha | `AtividadeService.criar` | bloquear colaborador |
| Mudar status de atividade | `AtividadeService.atualizarStatus` | bloquear colaborador |
| Confirmar presença | `LembreteService.confirmarPresenca` | permitir (é interação de leitura/presença) |
| Favoritar projeto | `ProjetoFachada.alternarFavorito` | permitir (preferência pessoal) |
| Escritas de edital | `EditalService` | já serão líder-only |
| Escritas de publicação | `PublicacaoService` | já serão líder de projeto/grupo |

Também é necessário revisar `ConviteService`:

- `resolverRole` possui `switch` exaustivo por `NivelMembro`. Adicionar o caso `COLABORADOR` mapeando para `"Colaborador"` (`isLider = false`).
- `validarPermissaoConviteGrupo`: permitir que líder/coordenador convide colaborador, seguindo a mesma regra de pesquisador.

### 6.3 Regra de vínculo

- Colaborador no grupo pode ser adicionado a projetos, mas sempre como `COLABORADOR` (não como `PESQUISADOR`/`LIDER`).
- Se o usuário já é `COLABORADOR` no grupo, `ProjetoFachada.adicionarParticipante` deve forçar `NivelMembro.COLABORADOR`, independentemente do nível enviado.
- Colaborador no projeto não pode ser promovido a líder.

### 6.4 Arquivos afetados

- `models/enums/NivelMembro.java`
- `service/GrupoAcessoService.java`
- `service/LembreteService.java`
- `service/NoticiaService.java`
- `service/AtividadeService.java`
- `service/ConviteService.java`
- `fachada/ProjetoFachada.java`
- `service/GrupoService.java` (se necessário ao listar/editar)

### 6.5 Critérios de aceite

- [ ] Colaborador consegue listar/buscar todos os recursos do grupo.
- [ ] Colaborador recebe 403 em qualquer criação/edição/exclusão.
- [ ] Colaborador não aparece como opção de líder de projeto.
- [ ] Convite para colaborador funciona e cria o vínculo correto.
- [ ] Testes existentes de líder/coordenador/pesquisador continuam verdes.

---

## 7. Caso: lembrete para projeto ou grupo inteiro

### 7.1 Situação atual

- `Evento` referencia apenas `Grupo` e `Usuario criador`.
- `CreateLembreteRequest` não possui `projetoId`.
- Qualquer membro do grupo pode criar lembrete.
- Listagem é sempre por grupo.

### 7.2 Modelo recomendado

Adicionar em `Evento`:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "projeto_id")
private Projeto projeto;
```

Semântica:

- `projeto != null` → lembrete do projeto.
- `projeto == null` → lembrete do grupo inteiro.

O checkbox do front-end pode ser traduzido para o booleano `paraGrupoInteiro`, mas o banco não precisa desse booleano: ele é derivado de `projeto == null`.

### 7.3 DTOs

`CreateLembreteRequest` e `EditLembreteRequest`:

```java
Long projetoId,
Boolean paraGrupoInteiro
```

Regras:

- `paraGrupoInteiro == true`: `projetoId` deve ser nulo; somente líder de grupo/admin.
- `paraGrupoInteiro == false` ou ausente: `projetoId` obrigatório; projeto deve pertencer ao grupo; somente líder de grupo/coordenador/admin ou líder do projeto.
- Se ambos forem enviados, retornar `IllegalArgumentException` (400).

`LembreteResponse`:

```java
Long projetoId,
String projetoNome,
boolean paraGrupoInteiro
```

### 7.4 Permissões de criação

| Escopo | Quem cria |
| :-- | :-- |
| Grupo inteiro | Líder de grupo, coordenador legado e admin |
| Projeto | Líder do projeto, líder de grupo, coordenador legado e admin |

Pesquisador e colaborador **não criam** lembrete; apenas visualizam e confirmam presença. Isso ajusta o comportamento atual (qualquer membro criava).

### 7.5 Edição e exclusão

- Edição continua restrita ao criador ou admin (regra atual), mas se o criador mudar o escopo para “grupo inteiro”, precisa ser líder de grupo.
- Exclusão continua permitida ao criador, líder do grupo ou admin.
- Líder do projeto pode excluir lembrete do próprio projeto? Recomendação: sim, alinhado a “gerenciar lembretes do projeto”. Ajustar `deletar` para aceitar `validarPodeGerenciarProjeto` quando o lembrete for de projeto.

### 7.6 Listagem e visibilidade

Adicionar filtro opcional:

```http
GET /lembretes?grupoId=1&projetoId=10
GET /lembretes/grupo/1?projetoId=10
```

Visibilidade recomendada:

- Líder de grupo/coordenador/admin: vê todos os lembretes do grupo.
- Demais membros: veem lembretes do grupo inteiro + lembretes dos projetos dos quais participam.

Essa regra evita que um pesquisador veja lembrete de projeto alheio. Se a simplicidade for prioridade absoluta, a primeira versão pode listar todos e filtrar apenas por `projetoId`; porém isso não cumpre o isolamento por projeto.

### 7.7 Repositório

Em `EventoRepository`:

- incluir `LEFT JOIN FETCH e.projeto` nos métodos de busca;
- adicionar parâmetro `projetoId` em `filtrarPorGrupo`;
- para visibilidade, adicionar condição:
  - `e.projeto IS NULL OR e.projeto.id IN :projetosDoUsuario`, quando o solicitante não for gestor.

### 7.8 Migração

```sql
ALTER TABLE evento ADD COLUMN IF NOT EXISTS projeto_id BIGINT;

ALTER TABLE evento DROP CONSTRAINT IF EXISTS fk_evento_on_projeto;
ALTER TABLE evento ADD CONSTRAINT fk_evento_on_projeto
    FOREIGN KEY (projeto_id) REFERENCES projeto (id);

CREATE INDEX IF NOT EXISTS idx_evento_projeto ON evento (projeto_id);
```

### 7.9 Arquivos afetados

- `models/entity/Evento.java`
- `models/dto/request/CreateLembreteRequest.java`
- `models/dto/request/EditLembreteRequest.java`
- `models/dto/response/LembreteResponse.java`
- `service/LembreteService.java`
- `controller/LembreteController.java`
- `repository/EventoRepository.java`
- `util/IcsCalendarBuilder.java` (opcional: incluir projeto na descrição)
- `resources/db/migration/V8__...sql`

### 7.10 Critérios de aceite

- [ ] Lembrete do grupo inteiro só é criado por líder de grupo/admin.
- [ ] Lembrete de projeto é criado por líder do projeto/líder de grupo/admin.
- [ ] Pesquisador e colaborador não criam lembrete.
- [ ] É possível filtrar lembretes por projeto.
- [ ] Lembrete de projeto não aparece para quem não participa do projeto (salvo gestores).
- [ ] Contrato atual de criação sem `projetoId` é tratado explicitamente (ver risco 16.2).

---

## 8. Caso: rótulos (projetos) em editais e notícias

### 8.1 Modelo recomendado

Como o rótulo é apenas uma referência a `Projeto` e não possui atributos próprios, a solução mais simples é `@ManyToMany` com tabela de junção. Isso evita criar duas entidades e dois repositórios adicionais, mantendo a mudança pequena.

Em `Edital`:

```java
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
        name = "edital_x_projeto",
        joinColumns = @JoinColumn(name = "edital_id"),
        inverseJoinColumns = @JoinColumn(name = "projeto_id"))
@Builder.Default
private Set<Projeto> projetos = new HashSet<>();
```

Em `Noticia`:

```java
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
        name = "noticia_x_projeto",
        joinColumns = @JoinColumn(name = "noticia_id"),
        inverseJoinColumns = @JoinColumn(name = "projeto_id"))
@Builder.Default
private Set<Projeto> projetos = new HashSet<>();
```

> Exceção consciente ao padrão de `EditalXGrupo`: aquela entidade existe porque o vínculo com grupo tem regra de duplicidade e contagem. Rótulos não têm atributos; `@ManyToMany` é suficiente e menor.

### 8.2 DTOs

Requests:

- `CriarEditalRequest`: `List<Long> projetoIds`
- `EditarEditalRequest`: `List<Long> projetoIds`
- `CreateNoticiaRequest`: `List<Long> projetoIds`
- `EditNoticiaRequest`: `List<Long> projetoIds`

Respostas:

Criar um record simples reutilizável:

```java
public record ProjetoRotuloResponse(Long id, String titulo) {}
```

Adicionar `List<ProjetoRotuloResponse> projetos` em:

- `EditalResponse`
- `NoticiaResponse`

### 8.3 Validação

Em `EditalService` e `NoticiaService`:

1. Resolver os IDs com `ProjetoService.findAllByIds`.
2. Verificar se todos existem.
3. Verificar se todos pertencem ao grupo do edital/notícia.
4. Substituir a coleção no update (limpar e adicionar).
5. Ignorar duplicatas.

Não criar exceção nova: usar `IllegalArgumentException` para IDs inválidos e `SemPermissaoNoGrupoErro` para projeto de outro grupo, mantendo o padrão atual.

### 8.4 Filtro por rótulo

Adicionar `projetoId` opcional nas listagens:

```http
GET /grupos/{grupoId}/editais?projetoId=10
GET /noticias?grupoId=1&projetoId=10
```

Repositórios:

- `EditalRepository.filtrarPorGrupo`: adicionar `AND (:projetoId IS NULL OR EXISTS (SELECT 1 FROM e.projetos p WHERE p.id = :projetoId))`.
- `NoticiaRepository.filtrarVigentes`: mesma condição com `n.projetos`.

Usar `EXISTS` em vez de `JOIN` para não duplicar resultados e não quebrar a paginação.

### 8.5 Isolamento de editais compartilhados

`Edital` é global por `uri` e pode estar vinculado a vários grupos. Um projeto pertence a um grupo. Portanto:

- ao montar `EditalResponse` para um grupo, filtrar os rótulos por `projeto.grupo.id == grupoId`;
- no create/update, só aceitar projetos do grupo que está vinculando o edital;
- nunca devolver rótulos de projetos de outro grupo.

Isso exige que `EditalResponse.fromEntity` receba o `grupoId` (ou que o service monte o DTO). É uma mudança pequena, mas necessária para não vazar dados entre grupos.

### 8.6 Uso dos rótulos na aprovação de notícias

Com os rótulos, a aprovação por líder de projeto passa a ser possível:

```text
admin / líder de grupo / coordenador -> aprova qualquer notícia do grupo
líder de projeto -> aprova se a notícia tiver pelo menos um projeto rotulado
                    do qual ele é líder
```

Se a notícia não tiver rótulo, somente líder de grupo/coordenador/admin aprova.

Em `NoticiaService`:

- `aprovar` e `rejeitar`: substituir `validarCoordenadorOuAdmin` por `validarPodeAprovarNoticia(noticia, solicitante)`.
- `listarPendentes`: para líder de projeto, exigir/filtrar `projetoId` (ou filtrar pelos projetos que ele lidera); para líder de grupo/coordenador, listar todos.

Recomendação de contrato:

```http
GET /noticias/pendentes?grupoId=1
GET /noticias/pendentes?grupoId=1&projetoId=10
```

### 8.7 Migração

```sql
CREATE TABLE IF NOT EXISTS edital_x_projeto
(
    edital_id  BIGINT NOT NULL,
    projeto_id BIGINT NOT NULL,
    CONSTRAINT pk_edital_x_projeto PRIMARY KEY (edital_id, projeto_id),
    CONSTRAINT fk_edital_x_projeto_on_edital FOREIGN KEY (edital_id) REFERENCES edital (id) ON DELETE CASCADE,
    CONSTRAINT fk_edital_x_projeto_on_projeto FOREIGN KEY (projeto_id) REFERENCES projeto (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS noticia_x_projeto
(
    noticia_id BIGINT NOT NULL,
    projeto_id BIGINT NOT NULL,
    CONSTRAINT pk_noticia_x_projeto PRIMARY KEY (noticia_id, projeto_id),
    CONSTRAINT fk_noticia_x_projeto_on_noticia FOREIGN KEY (noticia_id) REFERENCES noticia (id) ON DELETE CASCADE,
    CONSTRAINT fk_noticia_x_projeto_on_projeto FOREIGN KEY (projeto_id) REFERENCES projeto (id) ON DELETE CASCADE
);
```

### 8.8 Arquivos afetados

- `models/entity/Edital.java`
- `models/entity/Noticia.java`
- `models/dto/request/CriarEditalRequest.java`
- `models/dto/request/EditarEditalRequest.java`
- `models/dto/request/CreateNoticiaRequest.java`
- `models/dto/request/EditNoticiaRequest.java`
- `models/dto/response/EditalResponse.java`
- `models/dto/response/NoticiaResponse.java`
- `models/dto/response/ProjetoRotuloResponse.java`
- `service/EditalService.java`
- `service/NoticiaService.java`
- `repository/EditalRepository.java`
- `repository/NoticiaRepository.java`
- `controller/EditalController.java`
- `controller/NoticiaController.java`
- `resources/db/migration/V8__...sql`

### 8.9 Critérios de aceite

- [ ] É possível criar/editar edital com zero ou mais projetos como rótulos.
- [ ] É possível criar/editar notícia com zero ou mais projetos como rótulos.
- [ ] IDs de projeto de outro grupo são rejeitados.
- [ ] Listagens filtram por `projetoId`.
- [ ] Edital compartilhado não mostra rótulos de projetos de outro grupo.
- [ ] Líder de projeto rotulado consegue aprovar/rejeitar a notícia.

---

## 9. Caso: datas inicial e final em editais e notícias

### 9.1 Editais — já implementado

`Edital` já possui:

- `dthAbertura` (`@Column(name = "dth_abertura", nullable = false)`)
- `dthEncerramento` (`@Column(name = "dth_encerramento", nullable = false)`)

`CriarEditalRequest` e `EditarEditalRequest` já exigem as duas datas, e `EditalService.validarPeriodo` garante que o encerramento é posterior à abertura. O `EditalResponse` já devolve ambas.

**Nenhuma mudança de back-end é necessária.** O que pode faltar é o front-end exibir/editar esses campos.

### 9.2 Notícias — já implementado

`Noticia` já possui:

- `dataInicioExibicao` (opcional)
- `dataFimExibicao` (opcional no cadastro; materializada na aprovação se ausente)
- `isExpirada` calculado no response
- filtro de vigência na listagem (`dataInicioExibicao <= agora` e `dataFimExibicao >= agora`)

`CreateNoticiaRequest`/`EditNoticiaRequest` já aceitam ambas, e `validarPeriodoExibicao` garante que a final é posterior à inicial quando ambas existem.

**Decisão de produto:** se a regra passou a ser “toda notícia deve ter data inicial e final obrigatórias”, basta:

- adicionar `@NotNull` nos dois campos dos requests;
- remover ou manter o fallback de `prazoExibicaoDias` na aprovação;
- ajustar `NoticiaServiceTest` e o contrato do front.

Recomendação: **não tornar obrigatório agora**, porque o comportamento atual já atende “notícia tem data final” (materializada na aprovação) e evita quebra de clientes existentes.

### 9.3 Critérios de aceite

- [ ] `GET` de edital retorna `dthAbertura` e `dthEncerramento`.
- [ ] `GET` de notícia retorna `dataInicioExibicao` e `dataFimExibicao`.
- [ ] Edital com encerramento anterior à abertura retorna 400.
- [ ] Notícia com data final anterior à inicial retorna 400.
- [ ] Notícia sem data final recebe prazo padrão na aprovação.

---

## 10. Caso: publicações — CRUD e autorização

### 10.1 Situação atual

- `PublicacaoService` não recebe autenticação e **não valida grupo nem projeto**.
- Qualquer usuário autenticado pode criar, listar e inativar publicação de qualquer projeto.
- Não existe endpoint de edição (`PUT`).
- `PublicacaoController` não recebe `Authentication`.

### 10.2 Mudanças recomendadas

1. Injetar `UsuarioService` e `GrupoAcessoService` em `PublicacaoService`.
2. Receber `String emailLogado` nos métodos de escrita/leitura (ou usar `SecurityContextHolder`, como em `ProjetoFachada`; recomenda-se o parâmetro explícito, alinhado a `NoticiaService`/`EditalService`).
3. `criarPublicacao`:
   - buscar projeto;
   - validar `grupoAcessoService.validarPodeGerenciarProjeto(projeto.getId(), solicitante.getId())`.
4. `listarPublicacoes`:
   - adicionar `grupoId` e/ou exigir `projetoId` para não listar tudo;
   - validar que o solicitante é membro do grupo/projeto.
5. `buscarPorId`/`buscarPorUuid`:
   - validar membro do grupo do projeto da publicação.
6. `inativarPublicacao`:
   - validar gestor do projeto.
7. Criar `PUT /publicacoes/{id}`:
   - `EditarPublicacaoRequest(titulo, descricao, projetoId, arquivo opcional)`;
   - se `arquivo` vier, subir novo PDF e substituir `url`;
   - validar gestor do projeto de destino.
8. Atualizar `PublicacaoRepository.filtrarPublicacoes` para aceitar `grupoId` (join `p.projeto.grupo`).

### 10.3 Arquivos afetados

- `service/PublicacaoService.java`
- `controller/PublicacaoController.java`
- `models/dto/request/EditarPublicacaoRequest.java`
- `models/dto/request/CreatePublicacaoRequest.java` (sem mudança obrigatória)
- `repository/PublicacaoRepository.java`
- `test/service/PublicacaoServiceTest.java`

### 10.4 Critérios de aceite

- [ ] Pesquisador e colaborador recebem 403 ao criar/editar/excluir publicação.
- [ ] Líder do projeto e líder do grupo conseguem criar/editar/excluir.
- [ ] Listagem não vaza publicação de outro grupo/projeto.
- [ ] `PUT /publicacoes/{id}` atualiza metadados e, opcionalmente, o PDF.
- [ ] Testes existentes ajustados para o novo parâmetro de autenticação.

---

## 11. Caso: editais — escrita somente pelo líder de grupo

Hoje `EditalService.validarPodeGerenciar` chama `validarCoordenadorOuAcima`. Para alinhar à proposta:

```java
private void validarPodeGerenciar(Long grupoId, Usuario solicitante) {
    if (isAdmin(solicitante)) {
        return;
    }
    grupoAcessoService.validarLider(grupoId, solicitante.getId());
}
```

Isso remove o coordenador legado da escrita de edital. Se for necessário manter compatibilidade, a alternativa é:

```java
if (isAdmin(solicitante) || grupoAcessoService.isLider(grupoId, solicitante.getId())) {
    return;
}
// manter coordenador apenas durante transição, se desejado
```

Leitura (`listar`, `buscar`) continua permitida a qualquer membro, incluindo colaborador.

Arquivos: `service/EditalService.java`, `test/service/EditalServiceTest.java`.

Critérios:

- [ ] Líder de grupo cria/edita/remove edital.
- [ ] Líder de projeto, pesquisador e colaborador recebem 403 na escrita.
- [ ] Colaborador continua lendo.

---

## 12. Migrações

Criar um único script idempotente, seguindo o padrão dos `V*` existentes:

`src/main/resources/db/migration/V8__perfis_aprovacao_rotulos_lembretes.sql`

Conteúdo previsto:

```sql
-- Lembrete vinculado a projeto
ALTER TABLE evento ADD COLUMN IF NOT EXISTS projeto_id BIGINT;
ALTER TABLE evento DROP CONSTRAINT IF EXISTS fk_evento_on_projeto;
ALTER TABLE evento ADD CONSTRAINT fk_evento_on_projeto
    FOREIGN KEY (projeto_id) REFERENCES projeto (id);
CREATE INDEX IF NOT EXISTS idx_evento_projeto ON evento (projeto_id);

-- Rótulos de edital
CREATE TABLE IF NOT EXISTS edital_x_projeto (...);

-- Rótulos de notícia
CREATE TABLE IF NOT EXISTS noticia_x_projeto (...);

-- Status de atividade: garante que não existe check legado bloqueando 0/6
ALTER TABLE atividade DROP CONSTRAINT IF EXISTS atividade_status_check;

-- Opcional: motivo de rejeição de atividade
-- ALTER TABLE atividade ADD COLUMN IF NOT EXISTS motivo_rejeicao VARCHAR(255);
```

Observações:

- `NivelMembro.COLABORADOR` não exige alteração de schema: `nivel` já é `VARCHAR(20)`.
- `ProjetoXUsuario.nivel` também é `VARCHAR(20)`.
- O status de atividade é inteiro; não é necessário alterar tipo, mas é preciso remover constraints antigas.
- O projeto está com `spring.flyway.enabled=false` e `ddl-auto=update`. Os scripts devem existir para ambientes com Flyway ligado; validar em ambos.

---

## 13. Endpoints impactados

| Método | Rota | Mudança |
| :-- | :-- | :-- |
| `PATCH` | `/atividades/{id}/aprovar` | Novo |
| `PATCH` | `/atividades/{id}/rejeitar` | Novo |
| `POST` | `/projetos/{id}/participantes` | Regra nova quando `nivel=LIDER` |
| `POST` | `/projetos` | Campo opcional `liderProjetoId` |
| `GET` | `/projetos/{id}` | Retorna líder do projeto |
| `POST` | `/lembretes` | Campos `projetoId` e `paraGrupoInteiro` |
| `PUT` | `/lembretes/{id}` | Campos `projetoId` e `paraGrupoInteiro` |
| `GET` | `/lembretes` e `/lembretes/grupo/{id}` | Filtro `projetoId` e visibilidade por projeto |
| `POST` | `/grupos/{grupoId}/editais` | Campo `projetoIds` |
| `PUT` | `/grupos/{grupoId}/editais/{id}` | Campo `projetoIds` |
| `GET` | `/grupos/{grupoId}/editais` | Filtro `projetoId` |
| `POST` | `/noticias` | Campo `projetoIds` |
| `PUT` | `/noticias/{id}` | Campo `projetoIds` |
| `GET` | `/noticias` | Filtro `projetoId` |
| `GET` | `/noticias/pendentes` | Filtro `projetoId`; líder de projeto |
| `PATCH` | `/noticias/{id}/aprovar` | Líder de projeto rotulado |
| `PATCH` | `/noticias/{id}/rejeitar` | Líder de projeto rotulado |
| `PUT` | `/publicacoes/{id}` | Novo |
| `GET` | `/publicacoes` | Filtro `grupoId`/`projetoId` e validação de acesso |

---

## 14. Impacto no front-end

1. **Perfil do usuário:** a tela de membros do grupo deve exibir o nível (`LIDER`, `COORDENADOR`, `PESQUISADOR`, `COLABORADOR`) e o líder do projeto deve ser indicado no projeto.
2. **Atividades:** exibir coluna “Aguardando aprovação” para líder de projeto/grupo; pesquisador vê suas atividades pendentes e pode mover “A fazer”/“Fazendo”.
3. **Lembretes:** checkbox “Para o grupo inteiro” habilitado apenas para líder de grupo; select de projeto obrigatório quando o checkbox estiver desmarcado.
4. **Editais/notícias:** multi-select de projetos para rótulos e filtro por projeto.
5. **Notícias:** aba de pendentes para líder de projeto, limitada aos projetos que ele lidera.
6. **Publicações:** botão de editar e bloqueio de criação para pesquisador/colaborador.
7. **Modo somente leitura:** quando o perfil efetivo for `COLABORADOR`, esconder/desabilitar todos os botões de escrita.

---

## 15. Testes

### 15.1 Testes unitários a ajustar/criar

| Arquivo | Casos |
| :-- | :-- |
| `AtividadeServiceTest` | criação por pesquisador → `AGUARDANDO_APROVACAO`; criação por líder → `PENDENTE`; aprovar/rejeitar; líder de projeto autorizado; colaborador bloqueado; atividade pendente de aprovação conta como aberta |
| `NoticiaServiceTest` | líder de projeto rotulado aprova/rejeita; notícia sem rótulo só líder de grupo; colaborador não cria; filtro por projeto |
| `EditalServiceTest` | escrita somente líder; rótulos válidos; rótulo de outro grupo rejeitado; filtro por projeto |
| `LembreteServiceTest` | grupo inteiro só líder; projeto por líder do projeto; pesquisador/colaborador não criam; filtro/visibilidade por projeto |
| `PublicacaoServiceTest` | autorização de criação/edição/exclusão; `PUT`; listagem por grupo/projeto |
| `GrupoAcessoServiceTest` | `isColaborador`, `isLiderDoProjeto`, `podeGerenciarProjeto`, colaborador como teto |
| `GrupoServiceTest` | convite/role de colaborador; definição de líder de projeto |

### 15.2 Testes de integração a revisar

- `AtividadeIntegrationTest`
- `NoticiaIntegrationTest`
- `EditalIntegrationTest`
- `GrupoPaginacaoIntegrationTest`

Atenção: os construtores de records mudam (`CreateLembreteRequest`, `CreateNoticiaRequest`, `CriarEditalRequest`, `CreateProjetoRequest`). Ajustar todos os `new ...(...)` dos testes.

### 15.3 Comando

```bash
./gradlew test
```

---

## 16. Riscos e decisões pendentes

### 16.1 Conflito com REQ-035

A regra antiga diz que pesquisador só cria atividade filha. A nova ideia diz que pesquisador cria atividades para aprovação. **Decisão recomendada:** permitir atividade de topo pendente para pesquisador e manter a regra de impedimento para filhas. Documentar a substituição no requisito.

### 16.2 Compatibilidade do lembrete sem `projetoId`

Hoje qualquer membro cria lembrete apenas com `grupoId`. Se a nova regra exigir `projetoId` ou `paraGrupoInteiro`, clientes antigos quebram. Opções:

- **Recomendada:** tratar ausência de ambos como erro 400 e atualizar o front junto.
- **Transitória:** assumir `paraGrupoInteiro=true` quando `projetoId` for nulo, mas exigir líder de grupo. Ainda assim há quebra de permissão para pesquisadores.

### 16.3 Edital global e rótulos

`Edital` é compartilhado entre grupos por `uri`. Filtrar os rótulos pelo grupo requisitante é obrigatório para não vazar projetos de outro grupo. Não esquecer de fazer isso no `buscar`, `listar` e no retorno do `criar`/`atualizar`.

### 16.4 `COORDENADOR` legado

O perfil não faz parte dos quatro propostos. Mantê-lo nesta entrega evita migração de dados e quebra de testes. Se o produto quiser removê-lo, planejar migração:

```sql
UPDATE grupoxusuario SET nivel = 'LIDER' WHERE nivel = 'COORDENADOR';
```

(ou `PESQUISADOR`, conforme regra de negócio).

### 16.5 `ddl-auto=update` x Flyway

O runtime atual usa `ddl-auto=update` e Flyway desligado. O Hibernate pode criar as tabelas/colunas, mas não garante constraints, índices e scripts de conversão. Validar sempre com Flyway habilitado em ambiente de teste.

### 16.6 N+1 e lazy em rótulos

`@ManyToMany` é lazy. Para listagens, usar `LEFT JOIN FETCH` apenas quando necessário e com cuidado para não combinar duas coleções no mesmo fetch (evitar `MultipleBagFetchException`). Preferir `Set` para rótulos.

### 16.7 Enums e `switch`

Adicionar `COLABORADOR` quebra `switch` exaustivo em `ConviteService.resolverRole`. Compilar e procurar todos os `switch` sobre `NivelMembro` antes de finalizar.

### 16.8 Status de atividade e front-end

Novos valores `0` e `6` precisam ser mapeados no front. Se o front usa o `id` numérico, validar a ordem. A documentação da API (Swagger) deve refletir os novos status.

---

## 17. Ordem de execução sugerida

### Fase 1 — Domínio e autorização

- Adicionar `COLABORADOR` em `NivelMembro`.
- Adicionar métodos em `GrupoAcessoService` e queries em `ProjetoXUsuarioRepository`.
- Ajustar `ConviteService` e criação de `Role`.
- Testes de `GrupoAcessoService`.

### Fase 2 — Líder de projeto

- `CreateProjetoRequest.liderProjetoId`.
- Regra de unicidade e permissão em `ProjetoFachada.adicionarParticipante`.
- `ProjetoResponse` com líder.
- Testes.

### Fase 3 — Aprovação de atividades

- Novos status e transições.
- Criação por perfil.
- Endpoints aprovar/rejeitar.
- Ajuste de `AtividadeService` para líder de projeto.
- Testes.

### Fase 4 — Lembretes com escopo

- `Evento.projeto`.
- DTOs e regras de criação/edição.
- Filtro/visibilidade.
- Migração.
- Testes.

### Fase 5 — Rótulos e aprovação de notícias

- `@ManyToMany` em `Edital` e `Noticia`.
- DTOs, validação, filtros.
- Aprovação por líder de projeto.
- Migração.
- Testes.

### Fase 6 — Publicações e fechamento de editais

- Autorização em `PublicacaoService`.
- `PUT /publicacoes/{id}`.
- Restringir escrita de edital ao líder de grupo.
- Ajustar testes de integração.
- Rodar `./gradlew test`.

---

## 18. Checklist final

- [ ] `COLABORADOR` existe e é somente leitura no grupo/projeto.
- [ ] Líder de grupo tem todas as permissões e CRUD de editais.
- [ ] Líder de projeto é definido por `ProjetoXUsuario.nivel = LIDER`, com unicidade.
- [ ] Líder de projeto gerencia atividades e aprova/rejeita.
- [ ] Pesquisador cria atividade pendente e interage com o fluxo.
- [ ] Lembrete tem escopo projeto ou grupo inteiro.
- [ ] Só líder de grupo cria lembrete do grupo inteiro.
- [ ] Editais e notícias aceitam rótulos de projetos.
- [ ] Líder de projeto rotulado aprova notícia.
- [ ] Datas de edital e notícia estão expostas no contrato (já implementadas).
- [ ] Publicações têm CRUD completo e autorização.
- [ ] Migração `V8` criada.
- [ ] `./gradlew test` verde.
