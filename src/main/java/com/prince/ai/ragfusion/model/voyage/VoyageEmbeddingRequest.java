package com.prince.ai.ragfusion.model.voyage;

import lombok.Data;

import java.util.List;

@Data
public class VoyageEmbeddingRequest {
  private List<String> input;
  private String model;
  private String input_type;
  public void setInputType(String inputType) {
    this.input_type = inputType;
  }
}
