package com.prince.ai.ragfusion.model.voyage;

import lombok.Data;

import java.util.List;

@Data
public class VoyageEmbeddingResponse {
  private List<VoyageEmbedding> data;
}
