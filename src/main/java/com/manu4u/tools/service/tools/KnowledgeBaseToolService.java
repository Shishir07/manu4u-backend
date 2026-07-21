package com.manu4u.tools.service.tools;

import com.manu4u.tools.service.KnowledgeBaseTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseToolService {

    private final KnowledgeBaseTool knowledgeBaseTool;

    /**
     * Flattened search result. Returning an explicit {@code asOf}/{@code status} per
     * snippet (rather than the raw Document metadata map) is what lets the model apply
     * the freshness rules in the system prompt — it can only prefer recent facts, and
     * caveat stale ones, if it can actually see the dates.
     */
    public record KnowledgeSnippet(
            String source,
            String topic,
            String category,
            String asOf,
            String status,
            String content
    ) {}

    @Tool(name = "search_knowledge_base",
          description = "Search the Manchester United knowledge base: club history, legends, famous matches, " +
                        "eras, the stadium, ownership, seasons, and current topical material such as transfers " +
                        "and squad news. Use for ANY non-live question — e.g. '1999 Treble', 'Eric Cantona', " +
                        "'who did United sign this summer', 'Old Trafford history'. " +
                        "Each result carries an 'asOf' date and a 'status': prefer the most recent asOf for " +
                        "current-events questions, and treat 'stable-history' results as timeless.")
    public List<KnowledgeSnippet> searchKnowledgeBase(String query, Integer topK) {
        log.info("Searching knowledge base: query='{}', topK={}", query, topK);
        List<Document> results = knowledgeBaseTool.search(query, topK != null ? topK : 5, null);
        return results.stream().map(this::toSnippet).toList();
    }

    private KnowledgeSnippet toSnippet(Document doc) {
        Map<String, Object> md = doc.getMetadata();
        return new KnowledgeSnippet(
                str(md.get("source")),
                str(md.get("topic")),
                str(md.get("category")),
                str(md.get("asOf")),
                str(md.get("status")),
                doc.getText()
        );
    }

    private String str(Object value) {
        return value != null ? value.toString() : null;
    }
}
