# SAP ABAP Spec Validator - Agente Validador de Especificacoes Funcionais

## Objetivo

Validar especificacoes funcionais SAP ABAP usando Inteligencia Artificial. O sistema recebe documentos de especificacao (PDF, DOCX, etc.), extrai o conteudo textual e utiliza a API da OpenAI para analisar e validar a qualidade e completude da especificacao.

## Tecnologias

| Tecnologia | Versao | Finalidade |
|---|---|---|
| Java | 21 | Linguagem principal |
| Spring Boot | 3.3.2 | Framework backend |
| PostgreSQL | 16 | Banco de dados relacional |
| Apache Tika | - | Extracao de texto de documentos |
| OpenAI API | - | Validacao com IA (GPT) |
| Lombok | - | Reducao de boilerplate |
| Flyway | - | Migracoes de banco de dados |
| Swagger/OpenAPI | - | Documentacao da API |

## Arquitetura

Monolito modular seguindo o padrao MVC (Model-View-Controller).

```
app/
  src/main/java/com/.../
    controller/    # Endpoints REST
    service/       # Logica de negocio
    repository/    # Acesso a dados (JPA)
    model/         # Entidades e DTOs
    config/        # Configuracoes Spring
```

## Como Executar

### 1. Subir o banco de dados

```bash
docker compose -f docker/docker-compose.yml up -d
```

### 2. Configurar a chave da OpenAI

Linux/Mac:
```bash
export OPENAI_API_KEY=sk-sua-chave-aqui
```

Windows (CMD):
```cmd
set OPENAI_API_KEY=sk-sua-chave-aqui
```

Windows (PowerShell):
```powershell
$env:OPENAI_API_KEY = "sk-sua-chave-aqui"
```

### 3. Executar a aplicacao

```bash
cd app
mvn spring-boot:run
```

Para desenvolvimento local com logs detalhados e DDL auto-update:
```bash
cd app
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Acessar a documentacao da API

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Endpoints da API

### Upload de Documento (somente upload)

```bash
curl -X POST http://localhost:8080/api/v1/documents \
  -F "file=@especificacao.pdf"
```

### Upload e Validar (upload + validacao automatica)

```bash
curl -X POST http://localhost:8080/api/v1/documents/validate \
  -F "file=@especificacao.pdf"
```

### Validar Documento Existente

```bash
curl -X POST http://localhost:8080/api/v1/documents/1/validate
```

### Listar Documentos (com paginacao)

```bash
curl http://localhost:8080/api/v1/documents?page=0&size=10
```

### Buscar Documento por ID

```bash
curl http://localhost:8080/api/v1/documents/1
```

### Consultar Relatorio de Validacao

```bash
curl http://localhost:8080/api/v1/validations/1
```

### Exemplo de Resposta - Relatorio de Validacao

```json
{
  "reportId": 1,
  "documentId": 1,
  "status": "APPROVED_WITH_WARNINGS",
  "score": 78,
  "summary": "O documento possui boa descricao geral, mas faltam criterios de aceite e detalhamento tecnico SAP.",
  "finalRecommendation": "Pode seguir para refinamento antes de desenvolvimento.",
  "issues": [
    {
      "severity": "CRITICAL",
      "category": "REGRA_NEGOCIO",
      "title": "Regra de bloqueio de fornecedor incompleta",
      "description": "O documento informa que o fornecedor deve ser validado, mas nao define quais campos, tabelas ou condicoes devem ser usados.",
      "suggestion": "Informar se a validacao sera feita por LFA1, LFB1 ou outra estrutura, e qual mensagem deve ser exibida."
    },
    {
      "severity": "MODERATE",
      "category": "TESTES",
      "title": "Cenarios de teste insuficientes",
      "description": "Apenas cenarios de caminho feliz foram descritos.",
      "suggestion": "Adicionar cenarios negativos: dados invalidos, erros de processamento, limites excedidos."
    }
  ],
  "questions": [
    {
      "question": "O bloqueio do fornecedor deve impedir a gravacao ou apenas exibir alerta?",
      "reason": "Sem essa regra o ABAP pode implementar comportamento incorreto.",
      "targetAudience": "FUNCIONAL"
    }
  ],
  "positivePoints": [
    "Objetivo claramente definido",
    "Escopo e fora de escopo delimitados"
  ],
  "missingSections": [
    "Criterios de aceite",
    "Perfis de autorizacao"
  ],
  "riskAnalysis": "Risco medio. Faltam detalhes tecnicos SAP que podem gerar retrabalho na fase de desenvolvimento."
}
```

## CI/CD

O projeto utiliza GitHub Actions com workflows reutilizaveis do repositorio `suportess/suportes-workflow`.

### Pipeline

```
Push na branch main
  → Job: build (docker-build.yml)
    → Build multi-stage (Maven + Eclipse Temurin 21 JRE)
    → Push da imagem Docker
  → Job: deploy (docker-deploy.yml)
    → Deploy no VPS via SSH
    → docker-compose up com .env.app
    → Healthcheck via /actuator/health
    → Traefik expoe em HTTPS
```

### Secrets necessarios no GitHub

| Secret | Descricao |
|--------|-----------|
| `DOCKER_TOKEN` | Token de acesso ao Docker registry |
| `DOCKER_USERNAME` | Usuario do Docker registry |
| `VPS_HOST` | IP ou hostname do VPS |
| `VPS_USER` | Usuario SSH |
| `VPS_PASSWORD` | Senha SSH |
| `VPS_PORT` | Porta SSH |
| `APP_ENV` | Conteudo do arquivo .env.app (variaveis de ambiente) |

### Variaveis de ambiente (.env.app)

```bash
DB_HOST=spec-validator-postgres
DB_PORT=5432
DB_NAME=spec_validator
DB_USERNAME=postgres
DB_PASSWORD=sua-senha-segura
OPENAI_API_KEY=sk-sua-chave
OPENAI_MODEL=gpt-4.1-mini
IMAGE_TAG=latest
DOCKER_USERNAME=suportess
```

---

## Evolucao Futura

### Filas com RabbitMQ

O processamento atual e sincrono. A evolucao planejada utiliza RabbitMQ para processamento assincrono:

**Filas planejadas:**
- `document.extraction.queue` - Extracao de texto do documento
- `document.validation.queue` - Envio para validacao pela IA
- `document.validation.result.queue` - Resultado da validacao

**Fluxo assincrono:**
1. Usuario faz upload do documento
2. Sistema publica mensagem na fila de extracao
3. Worker extrai o texto do documento (Apache Tika)
4. Worker publica na fila de validacao
5. Worker chama a IA (OpenAI) para validar
6. Resultado salvo no banco de dados
7. Frontend consulta o status periodicamente (polling)

### RAG com pgvector

Evolucao para Retrieval-Augmented Generation (RAG) utilizando pgvector:

- Armazenar embeddings dos documentos no PostgreSQL com extensao pgvector
- Busca semantica por similaridade (nearest neighbor search)
- Enriquecer o contexto enviado para a IA com documentos similares ja validados
- Melhorar a qualidade da validacao com base em historico de especificacoes

### Limitacoes do MVP

- Sem autenticacao/autorizacao (sem auth)
- Sem suporte a OCR (documentos escaneados nao sao processados)
- Sem RAG (cada documento e validado isoladamente, sem contexto historico)
- Processamento sincrono apenas (sem filas)
- Sem suporte multi-tenant (single tenant)
