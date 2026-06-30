package com.prince.ai.ragfusion.controller;

import com.prince.ai.ragfusion.model.SearchRequest;
import com.prince.ai.ragfusion.model.SearchResponse;
import com.prince.ai.ragfusion.model.SearchResult;
import com.prince.ai.ragfusion.model.UploadDocumentResponseDto;
import com.prince.ai.ragfusion.service.DocumentService;
import com.prince.ai.ragfusion.service.VectorSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Document API", description = "Document Upload and Indexing APIs")
public class DocumentController {

  private final DocumentService documentService;
  private final VectorSearchService vectorSearchService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Upload and index a document")
  public UploadDocumentResponseDto uploadDocument(
      @RequestParam("file") MultipartFile file) {
    return documentService.indexDocument(file);
  }

  @PostMapping("/search")
  @Operation(summary = "Vector search over indexed chunks")
  public SearchResponse search(@RequestBody @Valid SearchRequest request) {

    List<SearchResult> vectorResults = vectorSearchService.search(request.getQuery(), request.getTopK());

    return SearchResponse.builder()
        .vectorResults(vectorResults)
        .retrievedChunks(vectorResults.stream()
            .map(SearchResult::getChunk)
            .toList())
        .build();
  }

}