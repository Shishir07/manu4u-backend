package com.manu4u.tools.service;

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

    public List<Document> search(String query, int topK, String contentTypeFilter) {
        log.info("Knowledge base search: query='{}', topK={}, filter='{}'", query, topK, contentTypeFilter);

        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(topK);

            if (contentTypeFilter != null && !contentTypeFilter.isBlank()) {
                FilterExpressionBuilder b = new FilterExpressionBuilder();
                builder.filterExpression(b.eq("contentType", contentTypeFilter).build());
            }

            List<Document> results = vectorStore.similaritySearch(builder.build());
            log.info("Knowledge base returned {} results for query: '{}'", results.size(), query);
            return results;
        } catch (Exception e) {
            log.warn("Knowledge base unavailable (Qdrant not running?): {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
