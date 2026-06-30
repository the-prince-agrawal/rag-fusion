package com.prince.ai.ragfusion.model.voyage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoyageEmbedding {
  private float[] embedding;
  private Integer index;
}
