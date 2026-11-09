# Plano de implementação — fallback de armazenamento S3 → PostgreSQL

> Documento de planejamento. Nenhum código foi alterado.

## 1. Diagnóstico atual

Hoje o único ponto de integração com AWS é o `S3StorageService`, usado por:

| Serviço | Uso |
|---|---|
| `PublicacaoService` | Upload do PDF da publicação e gravação da URL S3 em `Publicacao.url` |
| `LembreteService` | Upload/download da ata da reunião, usando `ataS3Key`, `ataUrl` e `ataNomeArquivo` |

Problemas atuais:

- `S3StorageService` é uma classe concreta injetada diretamente nos services.
- Não existe configuração para desligar o S3.
- Qualquer erro de AWS vira `S3StorageException` → HTTP 500.
- A entidade `Documento` já existe com `byte[]`, mas **não é usada em nenhum fluxo**.
- `Documento.documento` usa `@Lob`, que no PostgreSQL/Hibernate 6 tende a mapear para `OID` (large object), não para `bytea`.
- O front-end abre `publicacao.url` e `lembrete.ataUrl` diretamente com `<a href>`. Se o fallback passar a servir o arquivo pela API autenticada, esses links precisam ser ajustados.

---

## 2. Arquitetura proposta

Criar uma abstração de armazenamento com três modos:

```text
provider = auto      → tenta S3 se configurado; se falhar, salva no Postgres
provider = s3        → usa S3; se falhar, respeita fallback-enabled
provider = database  → nunca chama AWS; salva sempre no Postgres
```

Fluxo:

```text
PublicacaoService / LembreteService
              │
              ▼
      ArquivoStorage (interface)
              │
              ▼
   FallbackStorageService (orquestrador)
        │                    │
        ▼                    ▼
 S3StorageService      DatabaseStorageService
 (AWS existente)       (Documento + bytea)
```

### Componentes novos

| Componente | Responsabilidade |
|---|---|
| `StorageProvider` | Enum `AUTO`, `S3`, `DATABASE` |
| `StorageProperties` | Configuração `orbytum.storage.*` |
| `ArquivoArmazenado` | Resultado do upload: provedor, chave, URL, nome, content type, tamanho e `Documento` quando for banco |
| `ArquivoStorage` | Interface comum de salvar/baixar/excluir |
| `DatabaseStorageService` | Persiste o arquivo em `Documento` |
| `FallbackStorageService` | Decide S3 ou banco e faz o fallback em caso de erro |

### Componentes alterados

| Arquivo | Alteração |
|---|---|
| `Documento.java` | Adicionar setters/builder, `contentType`, `tamanho`, `chave`, `dthCriacao`; trocar `@Lob` por `bytea` |
| `Publicacao.java` | Adicionar `storageProvider`, `storageKey`; ajustar `@JoinColumn` para `documento_id`; `orphanRemoval` |
| `Evento.java` | Adicionar `ataDocumento`, `ataStorageProvider`; ajustar `isTemAta()` |
| `PublicacaoService.java` | Injetar `ArquivoStorage`; montar `Documento`/URL conforme provedor |
| `LembreteService.java` | Injetar `ArquivoStorage`; upload/download de ata com fallback |
| `PublicacaoController.java` | Novo endpoint de download do PDF |
| `SecurityConfig.java` | Nenhuma alteração se o download for autenticado |
| `application.yml` | Novas propriedades de storage |
| Front-end | Baixar PDF/ata via `getBlob` autenticado |

---

## 3. Modelo de dados

### 3.1 Reutilizar `Documento`

A entidade já está ligada a `Publicacao` e é o lugar natural para o binário. Estendê-la evita criar outra tabela.

```java
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String extensao;

    @Column(name = "content_type")
    private String contentType;

    private Long tamanho;

    @Column(unique = true, length = 500)
    private String chave;

    @Column(name = "dth_criacao")
    private LocalDateTime dthCriacao;

    // bytea é mais adequado que OID para este caso
    @Column(columnDefinition = "bytea", nullable = false)
    private byte[] documento;

    @OneToOne(mappedBy = "documento")
    private Publicacao publicacao;

    @OneToOne(mappedBy = "ataDocumento")
    private Evento evento;
}
```

### 3.2 `Publicacao`

```java
@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
@JoinColumn(name = "documento_id", referencedColumnName = "id")
private Documento documento;

@Enumerated(EnumType.STRING)
@Column(name = "storage_provider", length = 20)
private StorageProvider storageProvider;

@Column(name = "storage_key", length = 500)
private String storageKey;
```

> O `@JoinColumn` atual usa `name = "documentoId"`. O padrão do projeto/SQL usa `documento_id`; vale padronizar.

### 3.3 `Evento`

```java
@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
@JoinColumn(name = "ata_documento_id")
private Documento ataDocumento;

@Enumerated(EnumType.STRING)
@Column(name = "ata_storage_provider", length = 20)
private StorageProvider ataStorageProvider;

public boolean isTemAta() {
    return ataDocumento != null
            || (ataUrl != null && !ataUrl.isBlank())
            || (ataS3Key != null && !ataS3Key.isBlank());
}
```

### 3.4 Migração

Criar `V9__storage_fallback_postgres.sql`:

```sql
ALTER TABLE documento
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(255),
    ADD COLUMN IF NOT EXISTS tamanho BIGINT,
    ADD COLUMN IF NOT EXISTS chave VARCHAR(500),
    ADD COLUMN IF NOT EXISTS dth_criacao TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW();

-- Se a coluna ainda estiver como OID (mapeamento antigo do @Lob)
ALTER TABLE documento
    ALTER COLUMN documento TYPE BYTEA USING lo_get(documento);

CREATE UNIQUE INDEX IF NOT EXISTS ux_documento_chave
    ON documento (chave)
    WHERE chave IS NOT NULL;

ALTER TABLE publicacao
    ADD COLUMN IF NOT EXISTS storage_provider VARCHAR(20),
    ADD COLUMN IF NOT EXISTS storage_key VARCHAR(500);

ALTER TABLE evento
    ADD COLUMN IF NOT EXISTS ata_storage_provider VARCHAR(20),
    ADD COLUMN IF NOT EXISTS ata_documento_id BIGINT;

ALTER TABLE evento
    ADD CONSTRAINT fk_evento_ata_documento
    FOREIGN KEY (ata_documento_id) REFERENCES documento (id);
```

> **Atenção:** o projeto está com `spring.flyway.enabled: false` e `ddl-auto: update`. Ou seja, essa migration **não roda automaticamente**. É preciso:
>
> - habilitar Flyway nos ambientes e trocar `ddl-auto` para `validate`, ou
> - aplicar o SQL manualmente antes do deploy.
>
> O `ddl-auto: update` não converte `OID` para `bytea` sozinho.

---

## 4. Configuração

Adicionar em `application.yml`:

```yaml
orbytum:
  storage:
    provider: ${STORAGE_PROVIDER:auto}                 # auto | s3 | database
    fallback-enabled: ${STORAGE_FALLBACK_ENABLED:true}
    max-file-size-bytes: ${STORAGE_MAX_FILE_SIZE_BYTES:20971520}
    public-base-url: ${API_PUBLIC_URL:http://localhost:8080}
```

E manter o bloco atual:

```yaml
aws:
  s3:
    region: ${AWS_REGION:sa-east-1}
    bucket-name: ${AWS_S3_BUCKET_NAME:}
    access-key-id: ${AWS_ACCESS_KEY_ID:}
    secret-access-key: ${AWS_SECRET_ACCESS_KEY:}
```

### `StorageProperties`

```java
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "orbytum.storage")
public class StorageProperties {
    private StorageProvider provider = StorageProvider.AUTO;
    private boolean fallbackEnabled = true;
    private long maxFileSizeBytes = 20 * 1024 * 1024;
    private String publicBaseUrl = "http://localhost:8080";
}
```

### S3 condicional

Hoje o `S3Config` sempre cria `S3Client` e `S3Presigner`. Para não inicializar AWS quando `provider=database` ou quando não há bucket:

```java
@Bean
@ConditionalOnExpression(
    "'${orbytum.storage.provider:auto}' != 'database' && '${aws.s3.bucket-name:}' != ''"
)
public S3Client s3Client() { ... }
```

O `FallbackStorageService` pode receber o S3 via `ObjectProvider<S3StorageService>` para funcionar mesmo sem o bean.

Também vale configurar timeouts para o fallback não demorar:

```java
.overrideConfiguration(c -> c
    .apiCallTimeout(Duration.ofSeconds(10))
    .apiCallAttemptTimeout(Duration.ofSeconds(5)))
```

---

## 5. Interface e implementações

### 5.1 Enum

```java
public enum StorageProvider {
    AUTO, S3, DATABASE
}
```

### 5.2 Resultado do upload

```java
public record ArquivoArmazenado(
        StorageProvider provedor,
        String chave,
        String url,
        String nomeArquivo,
        String contentType,
        long tamanho,
        Documento documento // preenchido apenas quando DATABASE
) {}
```

### 5.3 Interface

```java
public interface ArquivoStorage {

    ArquivoArmazenado salvar(String chave, MultipartFile arquivo);

    byte[] baixar(String chave, StorageProvider provedor);

    void excluir(String chave, StorageProvider provedor);

    boolean disponivel(StorageProvider provedor);
}
```

### 5.4 `DatabaseStorageService`

Responsabilidades:

- Ler `MultipartFile.getBytes()`.
- Validar tamanho máximo.
- Criar `Documento` com nome, extensão, content type, tamanho, chave e `dthCriacao`.
- Salvar via `DocumentoRepository`.
- Retornar `ArquivoArmazenado` com `provedor = DATABASE` e o `Documento`.

### 5.5 `FallbackStorageService`

Regra central:

```java
public ArquivoArmazenado salvar(String chave, MultipartFile arquivo) {
    if (properties.getProvider() == StorageProvider.DATABASE) {
        return databaseStorage.salvar(chave, arquivo);
    }

    if (properties.getProvider() == StorageProvider.AUTO && !s3Disponivel()) {
        return databaseStorage.salvar(chave, arquivo);
    }

    try {
        return s3Storage.salvar(chave, arquivo);
    } catch (S3StorageException ex) {
        if (!properties.isFallbackEnabled()) {
            throw ex;
        }
        logger.warn("Falha no S3 ao salvar '{}'. Usando fallback no PostgreSQL.", chave, ex);
        return databaseStorage.salvar(chave, arquivo);
    }
}
```

Importante:

- Capturar apenas erros de armazenamento (`S3StorageException`), não `IllegalArgumentException` de validação.
- `provider=s3` + `fallback-enabled=true` também pode cair no banco. Se a intenção for “modo S3 estrito”, usar `fallback-enabled=false`.
- `provider=auto` deve considerar S3 disponível somente se o bucket estiver configurado.

---

## 6. Alterações nos fluxos

### 6.1 Criação de publicação

Em `PublicacaoService.criarPublicacao`:

```java
UUID uuid = UUID.randomUUID();
String chave = "publicacoes/" + uuid + ".pdf";

ArquivoArmazenado arquivo = arquivoStorage.salvar(chave, request.arquivo());

Publicacao publicacao = Publicacao.builder()
        .titulo(request.titulo().trim())
        .descricao(request.descricao().trim())
        .projeto(projeto)
        .uuid(uuid)
        .url(resolverUrl(arquivo, uuid))
        .storageProvider(arquivo.provedor())
        .storageKey(arquivo.chave())
        .documento(arquivo.documento())
        .dthRegistro(LocalDateTime.now())
        .isAtivo(true)
        .build();
```

Resolução da URL:

```java
private String resolverUrl(ArquivoArmazenado arquivo, UUID uuid) {
    if (arquivo.provedor() == StorageProvider.S3) {
        return arquivo.url();
    }
    return properties.getPublicBaseUrl()
            + "/publicacoes/uuid/" + uuid + "/arquivo";
}
```

### 6.2 Atualização de publicação

- Se veio novo arquivo, salvar no provedor escolhido.
- Atualizar `storageProvider`, `storageKey`, `documento` e `url`.
- Guardar o arquivo antigo e excluir **após o commit**:
  - S3: `deleteFile(storageKeyAntigo)`.
  - Banco: `orphanRemoval=true` remove o `Documento` antigo, ou exclusão explícita via repositório.
- Se o upload falhar no S3 e o fallback for usado, a publicação passa a apontar para o `Documento` novo.

### 6.3 Ata de lembrete

Em `LembreteService.inserirAta`:

```java
ArquivoArmazenado arquivo = arquivoStorage.salvar(chave, ata);

evento.setAtaStorageProvider(arquivo.provedor());
evento.setAtaS3Key(arquivo.provedor() == StorageProvider.S3 ? arquivo.chave() : null);
evento.setAtaUrl(resolverUrlAta(arquivo, evento.getId()));
evento.setAtaNomeArquivo(arquivo.nomeArquivo());
evento.setAtaDocumento(arquivo.documento());
```

Em `downloadAta`:

```java
StorageProvider provedor = evento.getAtaStorageProvider() != null
        ? evento.getAtaStorageProvider()
        : StorageProvider.S3; // legado

if (provedor == StorageProvider.DATABASE) {
    return evento.getAtaDocumento().getDocumento();
}
return s3StorageService.downloadFile(evento.getAtaS3Key());
```

### 6.4 Download de publicação

Adicionar endpoint autenticado:

```java
@GetMapping("/uuid/{uuid}/arquivo")
public ResponseEntity<byte[]> baixarArquivo(
        @PathVariable UUID uuid,
        Authentication authentication) {

    ArquivoDownload download = publicacaoService.baixarArquivo(
            uuid, authentication.getName());

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + download.nomeArquivo() + "\"")
            .contentType(MediaType.parseMediaType(download.contentType()))
            .body(download.bytes());
}
```

O service deve:

1. Buscar a publicação ativa por UUID.
2. Validar permissão de leitura do projeto.
3. Se provedor `DATABASE`, retornar `documento.getDocumento()`.
4. Se provedor `S3`, baixar por `storageKey` ou, para legado, extrair a key da `url`.
5. Retornar também nome e content type.

---

## 7. Ajustes no front-end

Como o fallback serve o arquivo pela API autenticada, os links diretos `<a href>` não funcionarão para arquivos no banco.

O `BaseService` já possui `getBlob`, então:

### `PublicacaoService.ts`

```ts
async baixarArquivo(uuid: string): Promise<Blob> {
    return this.getBlob(`/publicacoes/uuid/${uuid}/arquivo`);
}
```

Em `Publicacoes.tsx`, trocar o `<a href={publicacao.url}>` por um botão que:

1. Chama `baixarArquivo(publicacao.uuid)`.
2. Cria `const blobUrl = URL.createObjectURL(blob)`.
3. Abre em nova aba ou força download.
4. Revoga a URL depois.

### `LembreteService.ts`

```ts
async baixarAta(id: number): Promise<Blob> {
    return this.getBlob(`/lembretes/${id}/ata`);
}
```

Em `Lembretes.tsx`, substituir o link direto de `ataUrl` pelo mesmo padrão de blob.

> Alternativa: manter link direto apenas para S3 e usar blob para `DATABASE`. Mas o padrão “sempre baixar pela API” é mais seguro e uniforme. Se quiser links públicos, seria necessário criar endpoint com token assinado de curta duração.

---

## 8. Passo a passo de implementação

### Fase 1 — Configuração e abstração

1. Criar `StorageProvider`, `StorageProperties`, `ArquivoArmazenado` e `ArquivoStorage`.
2. Criar `DatabaseStorageService` e `FallbackStorageService`.
3. Ajustar `S3Config` para criação condicional e timeouts.
4. Adicionar propriedades em `application.yml` e `.env.example`.

### Fase 2 — Banco e entidades

5. Estender `Documento`.
6. Ajustar `Publicacao` e `Evento`.
7. Criar migration `V9`.
8. Definir estratégia de execução da migration (Flyway ou SQL manual).

### Fase 3 — Services

9. Refatorar `PublicacaoService` para usar `ArquivoStorage`.
10. Refatorar `LembreteService` para upload/download de ata com fallback.
11. Implementar limpeza de arquivo antigo em atualizações.
12. Implementar endpoint de download de publicação.

### Fase 4 — Front-end

13. Adicionar métodos `baixarArquivo` e `baixarAta` nos services.
14. Substituir links diretos por download via blob autenticado.

### Fase 5 — Testes e validação

15. Atualizar testes unitários.
16. Adicionar testes do fallback.
17. Rodar `./gradlew test` e validar manualmente a matriz de cenários.

---

## 9. Testes necessários

### Unitários

- `FallbackStorageServiceTest`
  - `provider=database` → nunca chama S3.
  - `provider=auto` sem bucket → salva no banco.
  - S3 lança `S3StorageException` → salva no banco.
  - `fallback-enabled=false` → propaga a exceção.
  - S3 com sucesso → não salva no banco.

- `PublicacaoServiceTest`
  - Criar com S3 → `storageProvider=S3`, `url` S3, sem `Documento`.
  - Criar com fallback → `storageProvider=DATABASE`, `documento` preenchido, `url` da API.
  - Atualizar arquivo → troca provedor e limpa arquivo antigo.

- `LembreteServiceTest`
  - Inserir ata com S3.
  - Inserir ata com fallback no banco.
  - Download de ata no banco.
  - Download de ata no S3.
  - Compatibilidade com registros antigos (`ataS3Key` sem provider).

- `DocumentoRepositoryTest` / integração
  - Persistência de `bytea`.
  - Busca por `chave`.
  - Exclusão em cascata/orphan removal.

### Matriz manual

| Cenário | Resultado esperado |
|---|---|
| AWS configurada e válida | Arquivo no S3, sem linha em `documento` |
| Sem bucket AWS | Arquivo no `documento.documento` como `bytea` |
| Credenciais AWS inválidas | Fallback para Postgres, sem HTTP 500 |
| S3 fora do ar | Fallback para Postgres |
| `provider=database` | Nunca tenta AWS |
| `fallback-enabled=false` + erro S3 | HTTP 500 com mensagem de storage |
| Publicação existente antes da mudança | Continua abrindo pelo link S3 |
| Download de PDF no banco | Endpoint autenticado retorna `application/pdf` |
| Download de ata no banco | Endpoint autenticado retorna o PDF |
| Atualização de publicação | Arquivo antigo removido após commit |

---

## 10. Riscos e cuidados

1. **Flyway desabilitado**
   - A migration não será aplicada automaticamente. Esse é o ponto mais crítico do plano.
   - Recomendação: habilitar Flyway em produção e usar `ddl-auto: validate`.

2. **`@Lob` e `OID` no PostgreSQL**
   - Trocar para `bytea` evita problemas de large object, transações e backup.
   - Confirmar o tipo real da coluna antes de rodar a migration.

3. **Crescimento do banco**
   - PDFs em `bytea` aumentam backup e tamanho do Postgres.
   - Definir limite máximo (`max-file-size-bytes`) e configurar também:
     ```yaml
     spring:
       servlet:
         multipart:
           max-file-size: 20MB
           max-request-size: 25MB
     ```

4. **Memória**
   - `MultipartFile.getBytes()` carrega o arquivo inteiro em memória.
   - Para PDFs de até ~20 MB é aceitável. Se houver arquivos maiores, avaliar streaming ou `large object`.

5. **Upload S3 fora da transação**
   - Se o S3 receber o arquivo e o save no banco falhar, sobra um objeto órfão.
   - Mitigação: registrar limpeza após rollback ou excluir o objeto em caso de falha no save.

6. **Falha ao gerar URL após upload**
   - O `S3StorageService.uploadFile` faz `putObject` e depois `getFileUrl`.
   - Se `getFileUrl` falhar, o arquivo já está no S3 e o fallback pode duplicar.
   - Mitigação: separar upload de geração de URL ou tratar falha de URL sem acionar fallback.

7. **Registros legados**
   - Publicações antigas têm apenas `url` S3, sem `storageKey`.
   - Para download, derivar a key da URL ou redirecionar para a URL existente.
   - Atas antigas têm `ataS3Key`, então funcionam sem backfill.

8. **Segurança**
   - O endpoint de download deve validar permissão de leitura.
   - Não expor `Documento` diretamente em DTOs.
   - Não logar conteúdo binário.

9. **Atualização concorrente**
   - Evitar que duas atualizações simultâneas deixem `storageKey` e `documento` inconsistentes.
   - Manter a troca dentro da mesma transação e limpar o antigo após commit.

---

## 11. Critérios de aceite

- [ ] Criar publicação sem AWS configurada grava o PDF em `documento.documento` (`bytea`) e retorna sucesso.
- [ ] Criar publicação com AWS válida continua gravando no S3.
- [ ] Erro de credencial/bucket/rede no S3 aciona fallback e não retorna 500.
- [ ] `provider=database` não inicializa nem chama AWS.
- [ ] Download de publicação funciona nos dois provedores.
- [ ] Upload/download de ata de lembrete funciona nos dois provedores.
- [ ] Registros antigos continuam acessíveis.
- [ ] Atualização de arquivo remove o antigo.
- [ ] Testes unitários e de integração passam com `./gradlew test`.
- [ ] Migration aplicada corretamente no banco.
