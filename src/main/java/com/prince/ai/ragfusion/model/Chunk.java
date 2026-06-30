package com.prince.ai.ragfusion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chunk {
  private String id;
  private String documentId;
  private String fileName;
  private Integer chunkIndex;
  private String text;
  private float[] embedding;
  private Integer startOffset;
  private Integer endOffset;

}