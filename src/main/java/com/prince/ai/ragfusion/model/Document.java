package com.prince.ai.ragfusion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

  /**
   * Unique document id
   */
  private String id;

  /**
   * Original uploaded file name
   */
  private String fileName;

  /**
   * Location on disk
   */
  private String filePath;

  /**
   * File size in bytes
   */
  private Long fileSize;

  /**
   * Upload timestamp
   */
  private LocalDateTime uploadedAt;

}