package com.manu4u.tools.service.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter;

    /**
     * How a document is turned into vector-store points. Chunking is per-layer,
     * not global — the two content layers have opposite geometry:
     * <ul>
     *   <li>{@link #SPLIT} — curated corpus docs (era/legend/topical/…): multi-paragraph
     *       essays authored for ~800-token chunks (see CORPUS_GUIDE.md).</li>
     *   <li>{@link #NONE} — raw news items: already smaller than any chunk (50–300 tokens),
     *       one item = one embedding. Never split, and never batch multiple items into one
     *       document — a grab-bag chunk of unrelated items embeds to semantic mush.</li>
     * </ul>
     */
    public enum SplitStrategy { SPLIT, NONE }

    /**
     * Chunk, embed, and store a single document (additive — does not remove
     * previously ingested points for the same source; see {@link #ingestReplacing}).
     *
     * @param rawText  Full text content
     * @param metadata Map of: source (required), contentType, category, status, asOf, asOfEpoch, …
     *                 The splitter copies this metadata onto every chunk.
     * @return number of chunks ingested
     */
    public int ingest(String rawText, Map<String, Object> metadata) {
        return ingest(rawText, metadata, SplitStrategy.SPLIT);
    }

    public int ingest(String rawText, Map<String, Object> metadata, SplitStrategy strategy) {
        log.info("Ingesting document: source='{}', contentType='{}', strategy={}",
                metadata.get("source"), metadata.get("contentType"), strategy);

        Document doc = new Document(rawText, metadata);
        List<Document> chunks = (strategy == SplitStrategy.NONE)
                ? List.of(doc)
                : splitter.apply(List.of(doc));

        vectorStore.add(chunks);

        log.info("Ingested {} chunk(s) for source '{}'", chunks.size(), metadata.get("source"));
        return chunks.size();
    }

    /**
     * Idempotent ingest: deletes any existing points whose metadata {@code source}
     * matches, then ingests the new content. Re-running ingestion (app restart,
     * corpus update, topical-doc refresh) replaces instead of duplicating.
     */
    public int ingestReplacing(String source, String rawText, Map<String, Object> metadata,
                               SplitStrategy strategy) {
        deleteBySource(source);
        return ingest(rawText, metadata, strategy);
    }

    /** Remove all vector-store points previously ingested for the given source id. */
    public void deleteBySource(String source) {
        try {
            vectorStore.delete(new FilterExpressionBuilder().eq("source", source).build());
            log.debug("Deleted existing points for source '{}'", source);
        } catch (Exception e) {
            // First-time ingest has nothing to delete; a real failure here still lets
            // ingestion proceed (worst case: duplicates until the next successful replace).
            log.warn("Delete-by-source failed for '{}' (continuing with ingest): {}", source, e.getMessage());
        }
    }
}
