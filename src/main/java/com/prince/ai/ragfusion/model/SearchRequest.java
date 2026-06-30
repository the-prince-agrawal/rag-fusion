package com.prince.ai.ragfusion.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

  @NotBlank(message = "Query cannot be empty.")
  private String query;

  @Builder.Default
  @Min(1)
  @Max(20)
  private Integer topK = 5;

}