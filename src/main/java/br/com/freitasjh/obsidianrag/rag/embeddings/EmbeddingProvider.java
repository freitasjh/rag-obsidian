package br.com.freitasjh.obsidianrag.rag.embeddings;

import java.util.List;

public interface EmbeddingProvider {

    Embedding embed(String text);

    List<Embedding> embedAll(List<String> texts);

    int dimensions();

    class Embedding {
        private final List<Double> values;
        private final String text;

        public Embedding(List<Double> values, String text) {
            this.values = values;
            this.text = text;
        }

        public List<Double> getValues() {
            return values;
        }

        public String getText() {
            return text;
        }
    }
}
