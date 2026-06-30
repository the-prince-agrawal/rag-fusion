package com.prince.ai.ragfusion.controller;

import com.prince.ai.ragfusion.model.UploadDocumentResponseDto;
import com.prince.ai.ragfusion.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Document API", description = "Document Upload and Indexing APIs")
public class DocumentController {

  private final DocumentService documentService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Upload and index a document")
  public UploadDocumentResponseDto uploadDocument(
      @RequestParam("file") MultipartFile file) {

    return documentService.indexDocument(file);
  }

}