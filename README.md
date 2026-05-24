# Obsidian RAG - MCP Server

MCP (Model Context Protocol) server para buscar, ler e escrever notas no Obsidian usando busca semântica com embeddings locais via `all-MiniLM-L6-v2-quantized` (rodando embarcado na JVM, sem necessidade de Ollama).

## Tools

| Tool | Args | Descrição |
|------|------|-----------|
| `search_notes` | `query`, `limit?` | Busca semântica nos chunks indexados |
| `read_note` | `path` | Lê conteúdo bruto de uma nota |
| `get_note` | `path` | Lê nota com metadados |
| `write_note` | `path`, `content` | Cria/atualiza nota e indexa incrementalmente |
| `list_notes` | `limit?` | Lista caminhos das notas |
| `refresh_index` | `full?` | Reindexa o vault em background |
| `get_index_status` | — | Status atual da indexação |

## Pré-requisitos

- Java 21+
- Quarkus 3.18.1
- Docker

## Configuração

### 1. Build e Iniciar

```bash
# Build da imagem
mvn package -DskipTests
docker build -t obsidian-rag .

# Sobe container persistente (modo SSE/HTTP)
docker compose up -d

# Logs
docker compose logs -f
```

### 3. Conectar MCP Cliente

Arquivo `mcp-server-config.json`:

```json
{
  "mcpServers": {
    "ObsidianBrain": {
      "url": "http://localhost:8087/mcp"
    }
  }
}
```

Uso com Gemini CLI:

```bash
gemini --mcp-config mcp-server-config.json
```

> **Nota:** Agora o servidor roda em modo SSE (HTTP) em um único container persistente.
> Múltiplos CLIs podem conectar simultaneamente sem quedas ou sobresscarga de containers.

### Variáveis de Ambiente

| Variável | Default | Descrição |
|----------|---------|-----------|
| `OBSIDIAN_VAULT_PATH` | `/data/vault` | Caminho do vault Obsidian |
| `OBSIDIAN_VAULT_NAME` | `brain` | Nome do vault |
| `RAG_CHUNK_SIZE` | `1000` | Tamanho dos chunks |
| `RAG_CHUNK_OVERLAP` | `200` | Sobreposição entre chunks |
| `RAG_MAX_RESULTS` | `10` | Máx. resultados por busca |

## API REST

O servidor também expõe uma API REST em `http://localhost:8087`:

| Endpoint | Método | Descrição |
|----------|--------|-----------|
| `/api/health` | GET | Status do serviço |
| `/api/search?q=query&limit=5` | GET | Busca semântica |
| `/api/notes` | GET | Lista notas indexadas |
| `/api/notes/{path}` | GET | Obtém nota por caminho |
| `/api/reindex?full=true` | POST | Reindexa o vault |
| `/api/status` | GET | Status da indexação |

## Arquitetura

```
MCP Cliente (Gemini CLI, Claude Desktop)
    │  HTTP SSE (http://localhost:8087/mcp)
    ▼
McpToolService (Quarkus + quarkus-mcp-server-sse)
    │
    ├── SearchService    → InMemoryEmbeddingStore (embeddings locais)
    ├── IndexingService  → VaultLoader + ChunkingPipeline
    └── VaultService     → VaultLoader (leitura/escrita no filesystem)

Embedding Model: all-MiniLM-L6-v2-quantized (384 dim, roda na JVM)

Container único e persistente via docker compose up -d
```

## Execução Local (sem Docker)

```bash
# Configurar caminho do vault
export OBSIDIAN_VAULT_PATH=/caminho/para/vault

# Executar
mvn quarkus:dev
```

## Persistência dos Embeddings

Os embeddings são persistidos em `~/.obsidian-rag/embeddings.json` e recarregados na inicialização, evitando reindexação completa a cada restart.
