package com.prince.ai.ragfusion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

  /**
   * Chunk information
   */
  private Chunk chunk;

  /**
   * Cosine similarity score
   */
  private Double vectorScore;

  /**
   * BM25 score
   */
  private Double bm25Score;

  /**
   * Reciprocal Rank Fusion score
   */
  private Double rrfScore;

}