package com.prince.ai.ragfusion.chunk;

import com.prince.ai.ragfusion.model.Chunk;
import com.prince.ai.ragfusion.model.DocumentContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class ChunkService {

  @Value("${fusion-rag.chunk.size}")
  private int chunkSize;

  @Value("${fusion-rag.chunk.overlap}")
  private int overlap;

  public List<Chunk> generateChunks(DocumentContent documentContent) {
    return slidingWindowChunk(documentContent);
  }

  private List<Chunk> slidingWindowChunk(DocumentContent documentContent) {
    List<Chunk> chunks = new ArrayList<>();
    String text = documentContent.getContent();
    int start = 0;
    int chunkIndex = 0;
    while (start < text.length()) {
      int end = Math.min(start + chunkSize, text.length());
      String chunkText = text.substring(start, end);
      Chunk chunk = Chunk.builder()
          .id(UUID.randomUUID().toString())
          .documentId(documentContent.getDocumentId())
          .fileName(documentContent.getFileName())
          .chunkIndex(chunkIndex)
          .startOffset(start)
          .endOffset(end)
          .text(chunkText)
          .build();

      chunks.add(chunk);
      log.info("--------------------------------------------");
      log.info("Chunk {}", chunkIndex);
      log.info("Start Offset : {}", start);
      log.info("End Offset   : {}", end);
      log.info("Characters   : {}", chunkText.length());
      chunkIndex++;
      if (end == text.length()) {
        break;
      }
      start = end - overlap;
      if (start < 0) {
        start = 0;
      }
    }

    log.info("--------------------------------------------");
    log.info("Total Chunks Generated : {}", chunks.size());

    return chunks;
  }

  /**
   * Future implementation
   */
  public List<Chunk> chunkBySentence(DocumentContent documentContent) {

    throw new UnsupportedOperationException(
        "Sentence chunking is not implemented yet.");
  }

  /**
   * Future implementation
   */
  public List<Chunk> chunkByFixedLength(DocumentContent documentContent) {

    throw new UnsupportedOperationException(
        "Fixed Length chunking is not implemented yet.");
  }

}