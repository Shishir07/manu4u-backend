package com.manu4u.tools.service;

import com.manu4u.tools.config.Manu4uProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseTool {

    private final VectorStore vectorStore;
    private final RecencyRanker recencyRanker;
    private final Manu4uProperties properties;

    /**
     * Semantic search over the knowledge corpus with freshness-aware ranking.
     *
     * <p>Runs in two stages: over-fetch {@code topK * overFetchFactor} candidates from
     * the vector store, then re-rank them ({@link RecencyRanker}) so time-sensitive
     * content is ordered by freshness and archived content is demoted, and finally
     * truncate back to {@code topK}. Over-fetching is what gives the ranker room to
     * reorder — without it, a stale document that edged out a fresher one on raw
     * similarity would never appear as a candidate at all.</p>
     */
    public List<Document> search(String query, int topK, String contentTypeFilter) {
        int overFetch = Math.max(topK, topK * properties.getRetrieval().getOverFetchFactor());
        log.info("Knowledge base search: query='{}', topK={} (over-fetching {}), filter='{}'",
                query, topK, overFetch, contentTypeFilter);

        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(overFetch);

            if (contentTypeFilter != null && !contentTypeFilter.isBlank()) {
                FilterExpressionBuilder b = new FilterExpressionBuilder();
                builder.filterExpression(b.eq("contentType", contentTypeFilter).build());
            }

            List<Document> candidates = vectorStore.similaritySearch(builder.build());
            if (candidates == null || candidates.isEmpty()) {
                log.info("Knowledge base returned no candidates for query: '{}'", query);
                return Collections.emptyList();
            }

            List<Document> ranked = recencyRanker.rerank(candidates, topK);
            log.info("Knowledge base: {} candidate(s) re-ranked to {} for query '{}'",
                    candidates.size(), ranked.size(), query);
            return ranked;
        } catch (Exception e) {
            log.warn("Knowledge base unavailable (Qdrant not running?): {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
