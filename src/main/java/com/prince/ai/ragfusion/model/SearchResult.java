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
  private Chunk chunk;
  private Double bm25Score;
  private Double vectorScore;
  private Double rrfScore;

}