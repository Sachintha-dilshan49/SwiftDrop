package lk.swiftdrop.ai.knowledge;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * Test stand-in for the ONNX model: a bag of words hashed into 384 dimensions. Texts that share
 * words are similar, which is enough to test retrieval without downloading a model.
 */
public class WordHashEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSIONS = 384;
    private static final Set<String> STOP_WORDS = Set.of("a", "an", "and", "are", "be", "by", "can", "do", "for",
            "how", "i", "if", "in", "is", "it", "me", "my", "of", "on", "or", "so", "the", "to", "what", "with", "you");

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        List<String> texts = request.getInstructions();
        for (int i = 0; i < texts.size(); i++) {
            embeddings.add(new Embedding(embed(texts.get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        for (String word : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (word.length() > 1 && !STOP_WORDS.contains(word)) {
                String stem = word.endsWith("s") ? word.substring(0, word.length() - 1) : word;
                vector[Math.floorMod(stem.hashCode(), DIMENSIONS)] += 1f;
            }
        }
        double norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        if (norm > 0) {
            float scale = (float) (1 / Math.sqrt(norm));
            for (int i = 0; i < vector.length; i++) {
                vector[i] *= scale;
            }
        }
        return vector;
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }
}
