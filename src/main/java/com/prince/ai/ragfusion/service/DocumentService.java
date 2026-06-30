package com.prince.ai.ragfusion.service;

import com.prince.ai.ragfusion.model.Chunk;
import com.prince.ai.ragfusion.model.DocumentContent;
import com.prince.ai.ragfusion.model.UploadDocumentResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
      "txt",
      "pdf");

  @Value("${fusion-rag.upload-directory}")
  private String uploadDirectory;

  private final DocumentReaderService documentReaderService;
  private final ChunkService chunkService;
  private final EmbeddingService embeddingService;
  private final VectorSearchService vectorSearchService;

  public UploadDocumentResponseDto indexDocument(MultipartFile file) {

    validateFile(file);

    try {

      Path uploadPath = Path.of(uploadDirectory);

      if (Files.notExists(uploadPath)) {
        Files.createDirectories(uploadPath);
      }

      String documentId = UUID.randomUUID().toString();

      String storedFileName = documentId + "-" + file.getOriginalFilename();

      Path destination = uploadPath.resolve(storedFileName);

      Files.copy(
          file.getInputStream(),
          destination,
          StandardCopyOption.REPLACE_EXISTING);

      log.info("Document uploaded successfully : {}", destination);

      /*
       * Read document
       */
      DocumentContent documentContent = documentReaderService.readDocument(
          documentId,
          file.getOriginalFilename(),
          destination);

      log.info("----------------------------------------");
      log.info("Document Successfully Read");
      log.info("Document Id : {}", documentContent.getDocumentId());
      log.info("File Name   : {}", documentContent.getFileName());
      log.info("Characters  : {}", documentContent.getContent().length());
      log.info("----------------------------------------");

      List<Chunk> chunks = chunkService.generateChunks(documentContent);
      embeddingService.embed(chunks);
      vectorSearchService.indexChunks(chunks);
      log.info("Chunking Completed, Total Chunks : {}", chunks.size());

      return UploadDocumentResponseDto.builder()
          .documentId(documentId)
          .fileName(file.getOriginalFilename())
          .chunks(chunks.size())
          .status("Indexed Successfully")
          .build();

    } catch (IOException ex) {
      log.error("Failed to upload document.", ex);
      throw new RuntimeException("Unable to upload document.", ex);
    }
  }

  private void validateFile(MultipartFile file) {

    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException(
          "File cannot be empty.");
    }

    String originalFileName = file.getOriginalFilename();

    if (originalFileName == null || originalFileName.isBlank()) {
      throw new IllegalArgumentException(
          "Invalid file name.");
    }

    int lastDot = originalFileName.lastIndexOf('.');

    if (lastDot == -1) {
      throw new IllegalArgumentException(
          "File extension is missing.");
    }

    String extension = originalFileName
        .substring(lastDot + 1)
        .toLowerCase(Locale.ROOT);

    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new IllegalArgumentException(
          "Only txt and pdf files are supported.");
    }
  }

}