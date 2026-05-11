#!/bin/sh
set -e

# Inicia o servidor Ollama em background
ollama serve &
OLLAMA_PID=$!

# Aguarda o servidor ficar pronto
echo "Aguardando Ollama iniciar..."
until ollama list >/dev/null 2>&1; do
  sleep 1
done
echo "Ollama pronto."

# Baixa o modelo de embedding (ignora falha de DNS/rede)
echo "Baixando modelo: nomic-embed-text..."
ollama pull nomic-embed-text || echo "Aviso: falha ao baixar modelo. Execute manualmente: docker compose exec ollama ollama pull nomic-embed-text"

# Mantém o processo do servidor em foreground
wait $OLLAMA_PID
