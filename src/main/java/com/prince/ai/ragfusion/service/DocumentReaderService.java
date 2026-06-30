package com.prince.ai.ragfusion.service;

import com.prince.ai.ragfusion.model.DocumentContent;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class DocumentReaderService {

  public DocumentContent readDocument(String documentId,
      String fileName,
      Path filePath) {

    try {

      String extension = getExtension(fileName);

      String content;

      switch (extension) {

        case "txt" -> content = readTextFile(filePath);

        case "pdf" -> content = readPdfFile(filePath);

        default -> throw new IllegalArgumentException(
            "Unsupported file type : " + extension);
      }

      log.info("Successfully read document [{}]", fileName);
      log.info("Total Characters : {}", content.length());

      return DocumentContent.builder()
          .documentId(documentId)
          .fileName(fileName)
          .content(content)
          .build();

    } catch (IOException ex) {

      throw new RuntimeException(
          "Failed to read document : " + fileName,
          ex);
    }
  }

  private String readTextFile(Path filePath) throws IOException {

    return Files.readString(filePath);

  }

  private String readPdfFile(Path filePath) throws IOException {

    try (PDDocument document = Loader.loadPDF(filePath.toFile())) {

      PDFTextStripper stripper = new PDFTextStripper();

      return stripper.getText(document);

    }

  }

  private String getExtension(String fileName) {

    int index = fileName.lastIndexOf('.');

    if (index == -1) {
      return "";
    }

    return fileName.substring(index + 1).toLowerCase();

  }

}