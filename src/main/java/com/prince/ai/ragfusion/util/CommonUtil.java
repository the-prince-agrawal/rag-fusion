package com.prince.ai.ragfusion.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

public class CommonUtil {

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of("txt", "pdf");
  public static void validateFile(MultipartFile file) {

    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("File cannot be empty.");
    }

    String originalFileName = file.getOriginalFilename();

    if (originalFileName == null || originalFileName.isBlank()) {
      throw new IllegalArgumentException("Invalid file name.");
    }

    int lastDot = originalFileName.lastIndexOf('.');

    if (lastDot == -1) {
      throw new IllegalArgumentException("File extension is missing.");
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
