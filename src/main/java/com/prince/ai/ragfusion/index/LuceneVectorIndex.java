package com.prince.ai.ragfusion.index;

import com.prince.ai.ragfusion.model.Chunk;
import com.prince.ai.ragfusion.model.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * KNN vector index backed by Apache Lucene 10.5.0.
 *
 * Stores each {@link Chunk}'s embedding as a {@link KnnFloatVectorField} (HNSW-backed) alongside the chunk's metadata
 * as stored fields, and supports approximate nearest-neighbour search via {@link KnnFloatVectorQuery}.
 *
 * Thread-safety: IndexWriter is internally thread-safe for concurrent writes. The DirectoryReader reference is
 * refreshed under a synchronized block so readers always see a consistent, fully-committed view of the index.
 */
@Slf4j
public class LuceneVectorIndex implements Closeable {

  public static final String FIELD_ID = "id";
  public static final String FIELD_DOCUMENT_ID = "documentId";
  public static final String FIELD_FILE_NAME = "fileName";
  public static final String FIELD_CHUNK_INDEX = "chunkIndex";
  public static final String FIELD_TEXT = "text";
  public static final String FIELD_VECTOR = "embedding";
  public static final String FIELD_START_OFFSET = "startOffset";
  public static final String FIELD_END_OFFSET = "endOffset";

  private static final int UNSET_INT = -1;

  private final int vectorDimension;
  private final VectorSimilarityFunction similarityFunction;

  private final Directory directory;
  private final IndexWriter writer;

  private volatile DirectoryReader reader;

  /**
   * In-memory index (ByteBuffersDirectory) using cosine similarity. Good default for an application-scoped,
   * non-persistent vector store.
   */
  public LuceneVectorIndex(int vectorDimension) {
    this(vectorDimension, VectorSimilarityFunction.COSINE);
  }

  /**
   * In-memory index with an explicit similarity function (COSINE, DOT_PRODUCT, EUCLIDEAN, MAXIMUM_INNER_PRODUCT).
   */
  public LuceneVectorIndex(int vectorDimension, VectorSimilarityFunction similarityFunction) {
    this(new ByteBuffersDirectory(), vectorDimension, similarityFunction);
  }

  /**
   * Persistent index backed by an FSDirectory at the given path. Survives application restarts.
   */
  public LuceneVectorIndex(Path indexPath, int vectorDimension, VectorSimilarityFunction similarityFunction) {
    this(openFsDirectory(indexPath), vectorDimension, similarityFunction);
  }

  private LuceneVectorIndex(Directory directory, int vectorDimension, VectorSimilarityFunction similarityFunction) {
    if (vectorDimension <= 0) {
      throw new IllegalArgumentException("Vector dimension must be positive.");
    }
    if (similarityFunction == null) {
      throw new IllegalArgumentException("Similarity function cannot be null.");
    }

    this.directory = directory;
    this.vectorDimension = vectorDimension;
    this.similarityFunction = similarityFunction;

    try {
      IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
      config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
      this.writer = new IndexWriter(directory, config);
      this.reader = DirectoryReader.open(writer);
    } catch (IOException e) {
      throw new LuceneVectorIndexException("Failed to initialize Lucene vector index.", e);
    }

    log.info("----------------------------------------");
    log.info("LuceneVectorIndex Initialized");
    log.info("Vector Dimension     : {}", vectorDimension);
    log.info("Similarity Function  : {}", similarityFunction);
    log.info("Directory Type       : {}", directory.getClass().getSimpleName());
    log.info("----------------------------------------");
  }

  private static Directory openFsDirectory(Path path) {
    try {
      return FSDirectory.open(path);
    } catch (IOException e) {
      throw new LuceneVectorIndexException("Failed to open FSDirectory at: " + path, e);
    }
  }

  /**
   * Indexes (or re-indexes, via upsert) all given chunks.
   */
  public void addDocuments(List<Chunk> chunks) {
    if (chunks == null || chunks.isEmpty()) {
      return;
    }
    log.info("Adding {} chunk(s) to Lucene Vector Index", chunks.size());

    try {
      for (Chunk chunk : chunks) {
        validateChunk(chunk);
        Document document = toLuceneDocument(chunk);
        // updateDocument = delete-then-add, keyed on the unique chunk id,
        // so re-adding a chunk safely replaces the old version.
        writer.updateDocument(new Term(FIELD_ID, chunk.getId()), document);
        log.info("Indexed Chunk {} [{}]", chunk.getChunkIndex(), chunk.getId());
      }
      writer.commit();
      refresh();
    } catch (IOException e) {
      throw new LuceneVectorIndexException("Failed to add chunks to Lucene vector index.", e);
    }
    log.info("Lucene Vector Index Size : {}", size());
  }

  /**
   * Re-opens the reader so subsequent searches see the latest committed writes. Cheap to call: openIfChanged() returns
   * null (no-op) if nothing changed.
   */
  public synchronized void refresh() {
    try {
      DirectoryReader newReader = DirectoryReader.openIfChanged(reader, writer);
      if (newReader != null) {
        DirectoryReader old = this.reader;
        this.reader = newReader;
        old.close();
        log.info("Lucene Vector Index reader refreshed.");
      }
    } catch (IOException e) {
      throw new LuceneVectorIndexException("Failed to refresh Lucene index reader.", e);
    }
  }

  /**
   * Number of live (non-deleted) documents currently visible to readers.
   */
  public int size() {
    return reader.numDocs();
  }

  /**
   * Deletes every document in the index.
   */
  public synchronized void clear() {
    try {
      writer.deleteAll();
      writer.commit();
      refresh();
      log.info("Lucene Vector Index Cleared.");
    } catch (IOException e) {
      throw new LuceneVectorIndexException("Failed to clear Lucene vector index.", e);
    }
  }

  /**
   * Approximate nearest-neighbour search over indexed embeddings.
   *
   * @param queryEmbedding
   *          query vector, must match the configured vectorDimension
   * @param topK
   *          number of nearest chunks to return
   */
  public List<SearchResult> search(float[] queryEmbedding, int topK) {
    if (queryEmbedding == null) {
      throw new IllegalArgumentException("Query embedding cannot be null.");
    }
    if (queryEmbedding.length != vectorDimension) {
      throw new IllegalArgumentException(
          "Query embedding dimension (" + queryEmbedding.length +
              ") does not match index dimension (" + vectorDimension + ").");
    }
    if (topK <= 0) {
      throw new IllegalArgumentException("topK must be positive.");
    }
    log.info("Lucene Vector Search Started");

    List<SearchResult> results = new ArrayList<>();

    // Snapshot the reader locally so a concurrent refresh() can't swap
    // it out mid-search.
    DirectoryReader currentReader = this.reader;

    try {
      IndexSearcher searcher = new IndexSearcher(currentReader);
      KnnFloatVectorQuery query = new KnnFloatVectorQuery(FIELD_VECTOR, queryEmbedding, topK);
      TopDocs topDocs = searcher.search(query, topK);

      StoredFields storedFields = searcher.storedFields();

      for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
        results.add(toSearchResult(scoreDoc, storedFields));
      }
    } catch (IOException e) {
      throw new LuceneVectorIndexException("Lucene vector search failed.", e);
    }

    log.info("Top {} Vector Results", results.size());
    for (SearchResult result : results) {
      log.info(
          "Chunk {}  Score {}",
          result.getChunk().getChunkIndex(),
          String.format("%.4f", result.getVectorScore()));
    }
    return results;
  }

  /**
   * Converts a single Lucene hit into the application's SearchResult shape. Lucene's KNN score is cosine similarity (or
   * whatever similarityFunction is configured), already normalized roughly to [0,1] for COSINE.
   */
  private SearchResult toSearchResult(ScoreDoc scoreDoc, StoredFields storedFields) throws IOException {
    Document document = storedFields.document(scoreDoc.doc);
    Chunk chunk = toChunk(document);

    return SearchResult.builder()
        .chunk(chunk)
        .vectorScore((double) scoreDoc.score)
        .build();
  }

  /**
   * Rehydrates a Chunk from its stored Lucene fields. Note: the raw embedding itself is indexed (for KNN) but not
   * stored, so chunk.getEmbedding() will be null here by design — callers that need the vector back should keep it
   * elsewhere (e.g. alongside the chunk text in your document store) or re-embed on demand.
   */
  private Chunk toChunk(Document document) {
    return Chunk.builder()
        .id(document.get(FIELD_ID))
        .documentId(document.get(FIELD_DOCUMENT_ID))
        .fileName(document.get(FIELD_FILE_NAME))
        .chunkIndex(getIntField(document, FIELD_CHUNK_INDEX))
        .text(document.get(FIELD_TEXT))
        .startOffset(getIntField(document, FIELD_START_OFFSET))
        .endOffset(getIntField(document, FIELD_END_OFFSET))
        .build();
  }

  /**
   * Builds the Lucene Document for a Chunk: the KNN vector field plus stored metadata fields needed to reconstruct a
   * Chunk on search.
   */
  private Document toLuceneDocument(Chunk chunk) {
    Document document = new Document();

    document.add(new StringField(FIELD_ID, chunk.getId(), Field.Store.YES));
    document.add(new StringField(FIELD_DOCUMENT_ID, nullToEmpty(chunk.getDocumentId()), Field.Store.YES));
    document.add(new StringField(FIELD_FILE_NAME, nullToEmpty(chunk.getFileName()), Field.Store.YES));
    document.add(new StoredField(FIELD_CHUNK_INDEX, toIntOrUnset(chunk.getChunkIndex())));
    document.add(new TextField(FIELD_TEXT, nullToEmpty(chunk.getText()), Field.Store.YES));
    document.add(new StoredField(FIELD_START_OFFSET, toIntOrUnset(chunk.getStartOffset())));
    document.add(new StoredField(FIELD_END_OFFSET, toIntOrUnset(chunk.getEndOffset())));

    // KnnFloatVectorField is indexed (HNSW graph) but never "stored" in
    // the traditional sense — that's by design, it's what makes KNN search fast.
    document.add(new KnnFloatVectorField(FIELD_VECTOR, chunk.getEmbedding(), similarityFunction));

    return document;
  }

  private void validateChunk(Chunk chunk) {
    if (chunk == null) {
      throw new IllegalArgumentException("Chunk cannot be null.");
    }
    if (chunk.getId() == null || chunk.getId().isBlank()) {
      throw new IllegalArgumentException("Chunk id cannot be null or blank.");
    }
    if (chunk.getEmbedding() == null) {
      throw new IllegalArgumentException("Chunk embedding cannot be null: " + chunk.getId());
    }
    if (chunk.getEmbedding().length != vectorDimension) {
      throw new IllegalArgumentException(
          "Chunk embedding dimension (" + chunk.getEmbedding().length +
              ") does not match index dimension (" + vectorDimension +
              ") for chunk: " + chunk.getId());
    }
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private int toIntOrUnset(Integer value) {
    return value == null ? UNSET_INT : value;
  }

  private Integer getIntField(Document document, String fieldName) {
    var field = document.getField(fieldName);
    if (field == null) {
      return null;
    }
    Number number = field.numericValue();
    if (number == null) {
      return null;
    }
    int value = number.intValue();
    return value == UNSET_INT ? null : value;
  }

  /**
   * Removes a single chunk by id.
   */
  public synchronized void deleteById(String chunkId) {
    if (chunkId == null || chunkId.isBlank()) {
      return;
    }
    try {
      writer.deleteDocuments(new Term(FIELD_ID, chunkId));
      writer.commit();
      refresh();
      log.info("Deleted Chunk [{}] from Lucene Vector Index.", chunkId);
    } catch (IOException e) {
      throw new LuceneVectorIndexException("Failed to delete chunk: " + chunkId, e);
    }
  }

  /**
   * Checks whether a chunk id currently exists in the index.
   */
  public boolean exists(String chunkId) {
    if (chunkId == null || chunkId.isBlank()) {
      return false;
    }
    try {
      IndexSearcher searcher = new IndexSearcher(this.reader);
      TopDocs topDocs = searcher.search(new TermQuery(new Term(FIELD_ID, chunkId)), 1);
      return topDocs.totalHits.value > 0;
    } catch (IOException e) {
      throw new LuceneVectorIndexException("Failed to check existence for chunk: " + chunkId, e);
    }
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public int getVectorDimension() {
    return vectorDimension;
  }

  public VectorSimilarityFunction getSimilarityFunction() {
    return similarityFunction;
  }

  /**
   * Lightweight snapshot of index stats, handy for a health/debug endpoint.
   */
  public Map<String, Object> getIndexMetadata() {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("vectorDimension", vectorDimension);
    metadata.put("similarityFunction", similarityFunction.name());
    metadata.put("documentCount", size());
    metadata.put("directoryType", directory.getClass().getSimpleName());
    return metadata;
  }

  @Override
  public String toString() {
    return "LuceneVectorIndex{" +
        "vectorDimension=" + vectorDimension +
        ", similarityFunction=" + similarityFunction +
        ", documentCount=" + size() +
        '}';
  }

  @Override
  public synchronized void close() {
    try {
      if (reader != null) {
        reader.close();
      }
      if (writer != null) {
        writer.close();
      }
      if (directory != null) {
        directory.close();
      }
      log.info("LuceneVectorIndex closed.");
    } catch (IOException e) {
      throw new LuceneVectorIndexException("Failed to close Lucene vector index.", e);
    }
  }

  /**
   * Wraps low-level Lucene/IO failures into a single unchecked type so callers (services/controllers) don't need to
   * catch IOException everywhere this index is used.
   */
  public static class LuceneVectorIndexException extends RuntimeException {
    public LuceneVectorIndexException(String message) {
      super(message);
    }

    public LuceneVectorIndexException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
