package com.prince.ai.ragfusion.service;

import com.prince.ai.ragfusion.index.LuceneVectorIndex;
import com.prince.ai.ragfusion.model.Chunk;
import com.prince.ai.ragfusion.model.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchService {

  private final LuceneVectorIndex luceneVectorIndex;
  private final EmbeddingService embeddingService;

  /**
   * Called right after chunks are embedded, during document upload.
   */
  public void indexChunks(List<Chunk> chunks) {
    luceneVectorIndex.addDocuments(chunks);
  }

  /**
   * Called at query time: embed the query text, then KNN search.
   */
  public List<SearchResult> search(String query, int topK) {
    float[] queryEmbedding = embeddingService.embedQuery(query);
    return luceneVectorIndex.search(queryEmbedding, topK);
  }
}