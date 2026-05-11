# Obsidian RAG - MCP Server

MCP (Model Context Protocol) server para buscar, ler e escrever notas no Obsidian usando busca semântica com embeddings locais via Ollama.

## Tools

| Tool | Args | Descrição |
|------|------|-----------|
| `search_notes` | `query`, `limit?` | Busca semântica + keyword boost |
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
- Ollama com modelo `nomic-embed-text`

## Configuração

### 1. Ollama

```bash
# Local
ollama pull nomic-embed-text
ollama serve

# Ou via Docker
docker compose up -d ollama
```

### 2. Build

```bash
mvn package -DskipTests
docker build -t obsidian-rag .
```

### 3. Conectar MCP Cliente

Arquivo `mcp-server-config.json`:

```json
{
  "mcpServers": {
    "obsidian-rag": {
      "command": "docker",
      "args": [
        "run", "--rm", "-i",
        "--network", "host",
        "-v", "/caminho/para/vault:/data/vault",
        "-e", "OLLAMA_BASE_URL=http://127.0.0.1:11434",
        "obsidian-rag"
      ]
    }
  }
}
```

Uso com Gemini CLI:

```bash
gemini --mcp-config mcp-server-config.json
```

### Variáveis de Ambiente

| Variável | Default | Descrição |
|----------|---------|-----------|
| `OBSIDIAN_VAULT_PATH` | `/data/vault` | Caminho do vault Obsidian |
| `OBSIDIAN_VAULT_NAME` | `brain` | Nome do vault |
| `OLLAMA_BASE_URL` | `http://host.docker.internal:11434` | URL do servidor Ollama |
| `OLLAMA_EMBEDDING_MODEL` | `nomic-embed-text` | Modelo de embedding |
| `RAG_CHUNK_SIZE` | `1000` | Tamanho dos chunks |
| `RAG_CHUNK_OVERLAP` | `200` | Sobreposição entre chunks |
| `RAG_MAX_RESULTS` | `10` | Máx. resultados por busca |

## Arquitetura

```
MCP Cliente (Gemini CLI, Claude Desktop)
    │  docker run --rm -i (STDIO)
    ▼
McpToolService (Quarkus + quarkus-mcp-server-stdio)
    │
    ├── SearchService    → InMemoryRetriever (embeddings via Ollama)
    ├── IndexingService  → VaultLoader + ChunkingPipeline
    └── VaultService     → VaultLoader (leitura/escrita no filesystem)
```

## Docker Compose (Ollama + Servidor)

```bash
docker compose up -d --build
```
