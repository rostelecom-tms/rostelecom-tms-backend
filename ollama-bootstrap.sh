#!/bin/sh
set -eu

LLM_MODEL="${OLLAMA_LLM_MODEL:-qwen3:4b}"
EMBED_MODEL="${OLLAMA_EMBEDDING_MODEL:-nomic-embed-text}"

# Start Ollama server in background inside the same container.
ollama serve &
OLLAMA_PID=$!

# Wait until API is ready.
until ollama list >/dev/null 2>&1; do
  sleep 1
done

# Pull required models once (cached in /root/.ollama volume).
ollama pull "$LLM_MODEL"
ollama pull "$EMBED_MODEL"

# Keep container running with Ollama server process.
wait "$OLLAMA_PID"
