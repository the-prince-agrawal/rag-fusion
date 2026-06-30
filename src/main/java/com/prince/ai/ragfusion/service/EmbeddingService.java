package com.prince.ai.ragfusion.service;

import com.prince.ai.ragfusion.client.VoyageApiClient;
import com.prince.ai.ragfusion.model.Chunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

  private final VoyageApiClient voyageApiClient;
  public void embed(List<Chunk> chunks) {

    if (chunks == null || chunks.isEmpty()) {
      return;
    }

    log.info("----------------------------------------");
    log.info("Generating Embeddings...");
    log.info("Total Chunks : {}", chunks.size());

    List<String> texts = chunks.stream()
        .map(Chunk::getText)
        .toList();

    List<float[]> embeddings = voyageApiClient.embedDocuments(texts);

    if (embeddings.size() != chunks.size()) {
      throw new IllegalStateException(
          "Embedding count does not match chunk count.");
    }

    for (int i = 0; i < chunks.size(); i++) {
      Chunk chunk = chunks.get(i);
      chunk.setEmbedding(embeddings.get(i));
      log.info(
          "Embedded Chunk {} -> Dimension {}",
          chunk.getChunkIndex(),
          chunk.getEmbedding().length);
    }

    log.info("----------------------------------------");
    log.info("Successfully Generated {} Embeddings",
        embeddings.size());
    log.info("----------------------------------------");
  }

  public float[] embedQuery(String query) {

    log.info("Generating query embedding...");

    float[] embedding = voyageApiClient.embedQuery(query);

    log.info("Query Embedding Dimension : {}",
        embedding.length);

    return embedding;
  }

}