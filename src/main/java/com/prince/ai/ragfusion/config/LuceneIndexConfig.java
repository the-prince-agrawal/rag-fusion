package com.prince.ai.ragfusion.config;

import com.prince.ai.ragfusion.index.LuceneVectorIndex;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LuceneIndexConfig {

  @Value("${fusion-rag.vector.dimension}")
  private int vectorDimension;

  /**
   * Spring calls close() on this bean automatically at shutdown because LuceneVectorIndex implements Closeable.
   */
  @Bean
  public LuceneVectorIndex luceneVectorIndex() {
    return new LuceneVectorIndex(vectorDimension, VectorSimilarityFunction.COSINE);
  }
}
