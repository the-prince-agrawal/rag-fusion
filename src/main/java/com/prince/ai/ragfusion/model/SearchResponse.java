package com.prince.ai.ragfusion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
  private String answer;
  private List<Chunk> retrievedChunks;
  private List<SearchResult> vectorResults;
  private List<SearchResult> bm25Results;

}