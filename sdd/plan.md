# Planejamento Técnico — EF Quality Check

> Documento operacional-arquitetural do projeto. Complementa a especificação em [spec.md](spec.md).

---

## 1. Visão da Arquitetura

Monólito modular em Spring Boot, exposto por API REST, com frontend Next.js atuando como BFF (proxy + SSR de UI) e PostgreSQL como storage único. Provedor de IA externo (OpenAI) chamado síncronamente. Observabilidade opcional via Langfuse.

```
                     ┌────────────────────────────────────────────┐
                     │              Browser (Arquiteto)           │
                     └────────────────────┬───────────────────────┘
                                          │ HTTPS
                                          ▼
┌────────────────────────────────────────────────────────────────────┐
│  Frontend Next.js 16 (React 19)                     porta 3000      │
│  - UI (App Router, RSC + Client Components)                         │
│  - Route Handler /api/proxy/[...path] → BFF/proxy do backend        │
└────────────────────┬───────────────────────────────────────────────┘
                     │ HTTP interno (API_URL=http://backend:8080)
                     ▼
┌────────────────────────────────────────────────────────────────────┐
│  Backend Spring Boot 3.3 (Java 21)                  porta 8080      │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────┐  ┌───────────┐ │
│  │ Controllers │─►│  Services    │─►│ Repositories│─►│ Postgres  │ │
│  └─────────────┘  │ + Extractors │  └─────────────┘  └───────────┘ │
│                   │ + Analyzer   │         ▲                        │
│                   │ + Scoring    │         │ Flyway (V1..V8)        │
│                   └──────┬───────┘         │                        │
│                          │                                          │
│                          ▼                                          │
│            ┌─────────────────────────┐                              │
│            │  AiProviderClient       │──► HTTPS → OpenAI GPT        │
│            │  LangFuseClient         │──► HTTPS → Langfuse (opt.)   │
│            └─────────────────────────┘                              │
└────────────────────────────────────────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────────────────┐
│  PostgreSQL 16 (imagem pgvector/pgvector:pg16)      porta interna   │
│  Volumes: pgdata; migrações Flyway; extensão pgvector disponível    │
│  na imagem mas não utilizada pelo app.                              │
└────────────────────────────────────────────────────────────────────┘
```

## 2. Stack Tecnológica

### Backend
| Item | Versão / Detalhe |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.3.13 (`spring-boot-starter-web`, `-data-jpa`, `-validation`, `-actuator`) |
| Build | Maven (multi-stage Dockerfile) |
| ORM | Spring Data JPA / Hibernate |
| Migrações | Flyway (`flyway-core`, `flyway-database-postgresql`) |
| Extração de texto | Apache Tika 3.2.x (`tika-core`, `tika-parsers-standard-package`) |
| HTTP client | `RestTemplateBuilder` (timeouts configuráveis) |
| Serialização | Jackson |
| Redução de boilerplate | Lombok 1.18.34 |
| Mapeamento de objetos | MapStruct 1.5.5.Final |
| Utilitários | Apache Commons Lang3 3.17.0 |
| Doc API | springdoc-openapi (Swagger UI) |
| Testes | JUnit 5 (Spring Boot Test) |

### Frontend
| Item | Versão / Detalhe |
|---|---|
| Framework | Next.js 16.2.x (App Router, output standalone) |
| UI | React 19 + TypeScript 5 |
| Estilização | Tailwind CSS 4 + `tw-animate-css` |
| Ícones / Feedback | `lucide-react`, `sonner` (toasts) |
| Utilitários | `clsx`, `tailwind-merge` |

### Dados & Infraestrutura
| Item | Versão / Detalhe |
|---|---|
| Banco | PostgreSQL 16 (imagem `pgvector/pgvector:pg16`) |
| Orquestração local | Docker Compose (compatível com Podman Compose) |
| Runtime containers | Backend: `eclipse-temurin:21-jre`; Frontend: `node:22-alpine` |

## 3. Estrutura do Projeto

```
DTT-2026-INT-002_EF-QUALITY-CHECK/
├── docker-compose.yml          # Orquestração raiz (postgres + backend + frontend)
├── README.md
├── sdd/
│   ├── spec.md                 # Especificação funcional
│   └── plan.md                 # Este documento
├── backend/
│   ├── Dockerfile              # Multi-stage Maven → JRE 21
│   ├── docker-compose.yml      # Backend standalone (dev)
│   ├── doc/insomnia-collection.json
│   └── app/
│       ├── pom.xml
│       └── src/main/
│           ├── java/com/company/specvalidator/
│           │   ├── SpecValidatorApplication.java
│           │   ├── config/       # OpenAiConfig, LangFuseConfig/Properties, ScoringConfig, SwaggerConfig, WebConfig (CORS)
│           │   ├── controller/   # DocumentController, ValidationController
│           │   ├── service/
│           │   │   ├── DocumentService, ValidationAgentService, ValidationReportService
│           │   │   ├── TextExtractionService, DocumentNormalizerService, SectionAnalyzerService
│           │   │   ├── FileStorageService, InMemoryMultipartFile
│           │   │   ├── extractor/  # PdfTextExtractor, DocxTextExtractor, TextExtractorFacade
│           │   │   ├── ai/         # AiProviderClient, OpenAiProviderClient, PromptBuilderService,
│           │   │   │                 AiResponseParser, LangFuseClient
│           │   │   └── validator/  # ScoreCalculator
│           │   ├── repository/   # ChecklistItemRepository, DocumentRepository,
│           │   │                    ExtractedDocumentRepository, PontoCriticoRepository,
│           │   │                    ValidationReportRepository
│           │   ├── entity/       # ChecklistItemEntity, DocumentEntity,
│           │   │                    ExtractedDocumentEntity, PontoCriticoEntity,
│           │   │                    ValidationReportEntity
│           │   ├── dto/          # ai/, request/, response/
│           │   ├── enums/        # ChecklistItemKey, ChecklistStatus, DevType,
│           │   │                    DocumentStatus, DocumentType, ValidationStatus
│           │   ├── mapper/       # DocumentMapper, ValidationReportMapper
│           │   └── exception/    # GlobalExceptionHandler + exceções específicas
│           └── resources/
│               ├── application.yml
│               └── db/migration/  # V1__schema_inicial.sql … V8__add_checklist_item_aplicavel.sql
└── frontend/
    ├── Dockerfile              # Multi-stage Node 22 → standalone
    ├── next.config.mjs
    ├── package.json
    ├── app/
    │   ├── layout.tsx, page.tsx, globals.css
    │   ├── api/proxy/[...path]/route.ts    # BFF/proxy para o backend
    │   └── validador/
    │       ├── page.tsx / _components/validador-view.tsx    (upload + validar)
    │       ├── documentos/                                   (lista)
    │       └── relatorio/[id]/                               (relatório)
    ├── components/
    │   ├── providers/theme-init.tsx
    │   └── shell/{sidebar,topbar,nav-config,sidebar-context}
    └── lib/{api.ts, types.ts, utils.ts}
```

## 4. Componentes-Chave do Backend

### 4.1 Camada de orquestração
- **`ValidationAgentService`** — orquestra o pipeline completo: `extract → normalize → analyze sections → build prompt → call OpenAI → score → persist`. Envolve cada etapa em um span Langfuse.
- **`DocumentService`** — valida arquivo (tipo, tamanho ≤ 20 MB), persiste metadados, resolve `DocumentType`.
- **`ValidationReportService`** — persistência e recuperação de `ValidationReportEntity` + `ChecklistItemEntity` + `PontoCriticoEntity`.

### 4.2 Extração e pré-processamento
- **`TextExtractionService`** (interface) implementada por `TextExtractorFacade`, que delega para `PdfTextExtractor` e `DocxTextExtractor` (Apache Tika).
- **`DocumentNormalizerService`** — remove ruído do texto (headers/footers repetidos, espaços múltiplos) antes de mandar para a IA.
- **`SectionAnalyzerService`** — heurística determinística (não usa IA) que classifica cada uma das 12 seções obrigatórias como `PRESENTE`, `PARCIAL` ou `AUSENTE` por matching de headings/palavras-chave.

### 4.3 Camada de IA
- **`PromptBuilderService`** — único ponto de verdade para o system prompt; detecta `DevType` (REPORT, ENHANCEMENT, INTERFACE, WORKFLOW, FORMS, BATCH, TABLE, ARQUIVO, TELA_FIORI, UNKNOWN) por scoring de palavras-chave e injeta critérios específicos.
- **`AiProviderClient`** (interface) → **`OpenAiProviderClient`** (implementação):
  - Endpoint: `https://api.openai.com/v1/chat/completions`.
  - Retry com backoff (até `max-retries = 3`).
  - Timeouts de conexão e leitura iguais a `timeout-seconds` (padrão 120).
  - Registra `generation` no Langfuse com `traceId`.
- **`AiResponseParser`** — extrai o JSON do conteúdo textual devolvido pela IA, tolerando o bloco `RACIOCINIO [ ... ]` que antecede o JSON.

### 4.4 Scoring
- **`ScoreCalculator`** — score proporcional: `(conquistado / possível) × 100`.
  - `OK` = peso cheio; `Parcial` = peso × `parcial-multiplier` (padrão 0.5, configurável); `Ausente` = 0.
  - Critérios não aplicáveis ao `DevType` detectado ficam fora do denominador (não penalizam nem beneficiam).
  - Pesos por critério (1–5) e `parcial-multiplier` configurados em `application.yml` (`app.scoring`).
  - Limiares de classificação: `score ≤ 39` → `REPROVADO`; `score ≤ 60` → `ACEITAVEL` ("Aprovado com Ressalvas"); `score > 60` → `APROVADO`.
- **`ScoringConfig`** — `@ConfigurationProperties(prefix = "app.scoring")` que expõe `parcialMultiplier` e o mapa de `pesos` para injeção no `ScoreCalculator`.

### 4.5 Persistência
- **Repositórios JPA:** `ChecklistItemRepository`, `DocumentRepository`, `ExtractedDocumentRepository`, `PontoCriticoRepository`, `ValidationReportRepository`.
- **Entidades:** ver seção 6.
- **`FileStorageService`** — grava binários no disco em `${STORAGE_PATH}` (default `./uploads`; container: `/app/uploads`). Nome armazenado é UUID + extensão original.

### 4.6 Cross-cutting
- **`GlobalExceptionHandler`** — mapeia exceções em respostas JSON padronizadas (`ApiErrorResponse`): `VALIDATION_ERROR` (400), `DOCUMENT_EXTRACTION_ERROR` (400), `AI_PROVIDER_ERROR` (502), `DOCUMENT_NOT_FOUND` (404), `FILE_TOO_LARGE` (400), `INTERNAL_ERROR` (500).
- **`WebConfig`** — CORS liberado para `localhost:3000/4200/5173` em `/api/**`.
- **`SwaggerConfig`** — expõe `/swagger-ui.html` e `/api-docs`.

## 5. Fluxos Principais

### 5.1 Upload + validação síncrona (`POST /api/v1/documents/validate`)
1. `DocumentController.uploadAndValidate(file)`
2. `ValidationAgentService.uploadAndValidate(file)`:
   1. `DocumentService.upload(file)` → grava binário e cria `DocumentEntity` (`UPLOADED`).
   2. `validateDocument(...)`:
      - `TextExtractionService.extract(file)` → `ExtractedDocument` (raw text + páginas + seções detectadas).
      - `DocumentNormalizerService.normalize(rawText)` → `NormalizedDocument`.
      - Persiste `ExtractedDocumentEntity` (status → `EXTRACTED`).
      - `SectionAnalyzerService.analyze(...)` → lista de `SectionStatus`.
      - `PromptBuilderService.buildSystemPrompt / buildUserPrompt`.
      - `AiProviderClient.validateFunctionalSpecification(...)` → `AiValidationResponse`.
      - `ScoreCalculator.calculateScore(checklist, devType)` → score; `calculateClassificacao(score)` → `ValidationStatus`.
      - `ValidationReportService.saveReport(...)` → `ValidationReportEntity` + `ChecklistItemEntity` list + `PontoCriticoEntity` list.
      - Documento marcado como `VALIDATED`.
3. Retorna `ValidationReportResponse` (HTTP 201).

### 5.2 Revalidação de documento existente (`POST /api/v1/documents/{id}/validate`)
Reutiliza `ExtractedDocumentEntity` quando presente; caso contrário, relê o binário do disco via `FileStorageService.resolve(...)` e reprocessa via Tika. Gera novo relatório sem apagar histórico.

### 5.3 Consulta de relatório (`GET /api/v1/validations/{reportId}`)
`ValidationController` → `ValidationReportService.getReport` + checklist items + pontos críticos → `ValidationReportMapper.toResponse`.

## 6. Modelo de Dados

Schema definido em `db/migration/V1..V8`. Tabelas principais:

| Tabela | Campos-chave | Observações |
|---|---|---|
| `documents` | `id`, `original_file_name`, `stored_file_name UNIQUE`, `content_type`, `document_type`, `file_size`, `status`, `created_at`, `updated_at` | Status: `UPLOADED`, `EXTRACTED`, `VALIDATED`, `FAILED`. |
| `extracted_documents` | `id`, `document_id UNIQUE FK`, `raw_text`, `normalized_text`, `detected_sections`, `page_count` | 1:1 com `documents`. |
| `validation_reports` | `id`, `document_id FK`, `score`, `classificacao`, `qualidade`, `resumo_executivo`, `principais_riscos_json`, `recomendacoes_json`, `parecer_final`, `specification_summary`, `section_analysis_json` | 1:N com `documents`. Colunas do formato antigo (V1–V4) permanecem nullable por retrocompatibilidade. |
| `checklist_items` | `id`, `report_id FK`, `chave`, `item`, `status`, `comentario`, `pontos`, `peso`, `pontos_conquistados`, `aplicavel` | N:1 com `validation_reports`. Criada em V5; `peso`/`pontos_conquistados` em V6; `pontos_conquistados` tipo `NUMERIC` em V7; `aplicavel` em V8. |
| `pontos_criticos` | `id`, `report_id FK`, `gap`, `impacto` | N:1 com `validation_reports`. Criada em V5. |

Índices adicionais em `V2__add_indexes.sql`. `ddl-auto: validate` em runtime — nenhuma alteração de schema fora do Flyway.

## 7. Contratos de API

Base: `/api/v1`. Documentado em Swagger (`/swagger-ui.html`).

| Método | Rota | Descrição | Status esperado |
|---|---|---|---|
| POST | `/documents` | Upload sem validação | 201 |
| POST | `/documents/validate` | Upload + validação síncrona | 201 |
| POST | `/documents/{documentId}/validate` | Revalidar documento existente | 200 |
| GET | `/documents` | Lista paginada (padrão 20, ordem `createdAt`) | 200 |
| GET | `/documents/{documentId}` | Detalhes do documento | 200 |
| GET | `/validations/{reportId}` | Relatório de validação completo | 200 |

Formato de erro padronizado (`ApiErrorResponse`): `timestamp`, `status`, `error`, `message`, `path`.

Coleção Insomnia disponível em [backend/doc/insomnia-collection.json](../backend/doc/insomnia-collection.json).

## 8. Integrações Externas

### 8.1 OpenAI (obrigatório)
- **Endpoint:** `POST https://api.openai.com/v1/chat/completions`.
- **Autenticação:** Bearer token via `OPENAI_API_KEY`.
- **Modelo padrão:** `gpt-4.1-mini` (configurável por `OPENAI_MODEL`).
- **Parâmetros:** `temperature: 0.0`, `messages: [system, user]`.
- **Timeout:** 120 s (conexão e leitura); retry até 3 tentativas.
- **Falha externa:** encapsulada em `AiProviderException` → HTTP 502 (`AI_PROVIDER_ERROR`).

### 8.2 Langfuse (opcional)
- **Endpoint:** `POST {host}/api/public/ingestion` (padrão `https://cloud.langfuse.com`).
- **Autenticação:** Basic Auth (`public-key` : `secret-key`, Base64).
- **Ativação:** flag `LANGFUSE_ENABLED=true`.
- **Eventos gerados por validação:**
  - `trace-create` (nome `document-validation`, metadata `documentId`).
  - `span-create`/`span-update` para: `text-extraction`, `normalization`, `section-analysis`, chamada OpenAI (via `generation-create/update`), `scoring`.
- **Falha na ingestão** é silenciada (log `WARN`); não interrompe o fluxo principal.

### 8.3 PostgreSQL
- Conexão via JDBC (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
- Pool Hikari: `maximum-pool-size=10`, `minimum-idle=5`, `connection-timeout=30s`.
- Extensão `pgvector` disponível na imagem, porém não utilizada pelo app.

## 9. Configuração & Variáveis de Ambiente

Arquivo canônico: [backend/app/src/main/resources/application.yml](../backend/app/src/main/resources/application.yml).

| Variável | Obrigatória | Default | Uso |
|---|---|---|---|
| `OPENAI_API_KEY` | Sim | — | Autenticação na OpenAI. |
| `OPENAI_MODEL` | Não | `gpt-4.1-mini` | Modelo GPT. |
| `DB_URL` | Não | `jdbc:postgresql://localhost:5436/spec_validator` | Conexão JDBC. |
| `DB_USERNAME` | Não | `postgres` | Usuário do PostgreSQL. |
| `DB_PASSWORD` | Não | `postgres` | Senha do PostgreSQL (recomendado sobrescrever em produção). |
| `STORAGE_PATH` | Não | `./uploads` (container: `/app/uploads`) | Diretório de binários. |
| `LANGFUSE_ENABLED` | Não | `false` | Liga/desliga observabilidade. |
| `LANGFUSE_HOST` | Não | `https://cloud.langfuse.com` | Host Langfuse. |
| `LANGFUSE_PUBLIC_KEY` | Cond. | — | Obrigatória se Langfuse ativado. |
| `LANGFUSE_SECRET_KEY` | Cond. | — | Obrigatória se Langfuse ativado. |
| `API_URL` (frontend) | Não | `http://localhost:8080` | Alvo do proxy Next.js. |

## 10. Segurança

- **Segredos apenas via ambiente/`.env.app`** — nunca commitados; `.gitignore` protege os arquivos de env.
- **Prompt injection** mitigado pelo encapsulamento do conteúdo do documento em `<documento_para_analise>` e instruções explícitas para tratar o conteúdo como dado, não instrução.
- **Validação de entrada** no boundary: tipo/extensão/MIME e tamanho antes de persistir; erros retornados como HTTP 400 padronizado.
- **CORS restrito** a origens de desenvolvimento (`localhost:3000/4200/5173`). Restringir por ambiente antes de qualquer exposição pública.
- **Sem autenticação/autorização hoje** — restrição herdada do MVP; qualquer exposição além do dev deve incluir camada de auth.
- **Logs sem PII** — persiste-se apenas contagens e tamanhos, não trechos do documento.

## 11. Monitoramento & Logs

### 11.1 Logs
- **Framework:** Slf4j via Spring Boot.
- **Configuração:** `application.yml` → nível `INFO` para `com.company.specvalidator`; `WARN` para `org.springframework.web` e `org.hibernate.SQL`.
- **Eventos-chave logados:**
  - Upload (nome + tipo detectado).
  - Extração (tamanho do texto + seções detectadas).
  - Normalização (tamanho pós-normalização).
  - Chamada IA (tentativa/retry, modelo).
  - Scoring (score calculado, classificação, devType).
  - Conclusão (`reportId`).

### 11.2 Health / Actuator
- Endpoints expostos: `management.endpoints.web.exposure.include=health,info`.
- URLs: `/actuator/health`, `/actuator/info`.
- Disponíveis para readiness/liveness em ambiente containerizado. Hoje apenas o serviço `postgres` no `docker-compose.yml` possui healthcheck configurado; o backend não (poderia consumir `/actuator/health` no futuro).

### 11.3 Tracing (Langfuse)
- Cada validação = 1 trace com 5 spans + 1 generation (OpenAI). Ver seção 8.2.
- Métricas registradas nos spans: `rawTextLength`, `normalizedTextLength`, `sectionsDetected`, `sectionsAnalyzed`, `checklistCount`, `devType`, `score`, `classificacao`.
- Erros em qualquer span são registrados via `endSpanWithError`.

## 12. Testes

Localização: [backend/app/src/test/java/com/company/specvalidator/service](../backend/app/src/test/java/com/company/specvalidator/service).

| Área | Cobertura atual |
|---|---|
| `DocumentNormalizerServiceTest` | Normalização de texto (remoção de ruído, headers). |
| `ai/AiResponseParserTest` | Parsing do JSON retornado pela IA (incluindo bloco `RACIOCINIO`). |
| `ai/PromptBuilderServiceTest` | Geração do system prompt, detecção de `DevType`, textos de critérios. |
| `validator/` | `ScoreCalculator` (cálculo proporcional, aplicabilidade por DevType, classificação). |

Execução: `mvn test` na pasta `backend/app`. Testes não fazem I/O externo — chamadas a OpenAI/Langfuse são unitárias com stubs/mocks.

## 13. Build & Execução Local

### 13.1 Docker Compose (recomendado)
```bash
cp .env.app.example .env.app          # preencher OPENAI_API_KEY (DB_PASSWORD opcional, default: postgres)
docker compose up --build -d          # ou: podman compose up --build -d
docker compose ps                     # esperar status "healthy"
```
Sobe `postgres` (com healthcheck), `backend` (depende de postgres saudável) e `frontend` (depende do backend). Portas expostas: `3000` (UI) e `8080` (API).

### 13.2 Backend isolado
```bash
cd backend/app
mvn spring-boot:run
# perfil dev: mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
Requer `OPENAI_API_KEY` e um PostgreSQL alcançável em `DB_URL`.

### 13.3 Frontend isolado
```bash
cd frontend
npm ci
npm run dev            # http://localhost:3000, proxy para API_URL
```

## 14. Riscos Técnicos & Débitos Conhecidos

| Risco / Débito | Impacto | Mitigação atual / planejada |
|---|---|---|
| Sem autenticação | Exposição pública insegura | Restringir CORS + rodar somente em rede interna até auth ser implementada. |
| Processamento síncrono | Timeout do cliente em documentos grandes | Limite de 20 MB + timeout de 120 s no cliente OpenAI. |
| Custo/latência OpenAI | Custo proporcional ao volume + latência 5–30 s por doc | `temperature=0.0` + retry limitado (até 3 tentativas). |
| Instabilidade de classificação em EFs borderline | Score na zona 35–60 pode mudar de classificação entre execuções devido à variação natural da IA | `temperature=0.0` minimiza mas não elimina; mitigação planejada: score banding + re-execução automática na zona de risco. |
| Extração via Tika sem OCR | Documentos escaneados retornam texto vazio | Fora do escopo do MVP. |
| Deploy não definido | Sem pipeline de promoção estável | Aguardar decisão de plataforma antes de formalizar CI/CD. |
| pgvector presente mas não usado | Confusão sobre features ativas | Documentado explicitamente aqui e em spec.md como recurso não utilizado. |


