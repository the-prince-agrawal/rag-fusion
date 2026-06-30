package com.prince.ai.ragfusion.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

  @Bean
  public OpenAPI fusionRagOpenApi() {

    return new OpenAPI()

        .info(
            new Info()
                .title("Fusion RAG API")
                .description("""
                    Hybrid Retrieval-Augmented Generation Engine.

                    Features:
                    • Document Upload
                    • Document Chunking
                    • Vector Embeddings
                    • BM25 Lexical Search
                    • Reciprocal Rank Fusion (RRF)
                    • Claude Answer Generation
                    """)
                .version("1.0.0")
                .contact(
                    new Contact()
                        .name("Prince")
                        .email("your-email@example.com"))
                .license(
                    new License()
                        .name("MIT License")))

        .servers(
            List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Local Development Server")))

        .externalDocs(
            new ExternalDocumentation()
                .description("GitHub Repository")
                .url("https://github.com/your-github/fusion-rag"));
  }

}