package com.prince.ai.ragfusion.client;

import com.prince.ai.ragfusion.model.voyage.VoyageEmbedding;
import com.prince.ai.ragfusion.model.voyage.VoyageEmbeddingRequest;
import com.prince.ai.ragfusion.model.voyage.VoyageEmbeddingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class VoyageApiClient {

  private final RestClient restClient = RestClient.create();

  @Value("${voyage.api-key}")
  private String apiKey;

  @Value("${voyage.url}")
  private String url;

  @Value("${voyage.model}")
  private String model;

  public List<float[]> embedDocuments(List<String> texts) {

    VoyageEmbeddingRequest request = new VoyageEmbeddingRequest();
    request.setInput(texts);
    request.setModel(model);
    request.setInputType("document");

    VoyageEmbeddingResponse response = restClient.post()
        .uri(url)
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + apiKey)
        .body(request)
        .retrieve()
        .body(VoyageEmbeddingResponse.class);

    if (response == null || response.getData() == null) {
      throw new RuntimeException("Voyage API returned empty response.");
    }

    return response.getData()
        .stream()
        .map(VoyageEmbedding::getEmbedding)
        .toList();
  }

  public float[] embedQuery(String query) {
    VoyageEmbeddingRequest request = new VoyageEmbeddingRequest();
    request.setInput(List.of(query));
    request.setModel(model);
    request.setInputType("query");

    VoyageEmbeddingResponse response = restClient.post()
        .uri(url)
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + apiKey)
        .body(request)
        .retrieve()
        .body(VoyageEmbeddingResponse.class);

    if (response == null
        || response.getData() == null
        || response.getData().isEmpty()) {
      throw new RuntimeException("Voyage API returned empty response.");
    }
    return response.getData().get(0).getEmbedding();
  }

}