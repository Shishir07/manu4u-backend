package com.manu4u.tools.service.tools;

import com.manu4u.tools.service.KnowledgeBaseTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseToolService {

    private final KnowledgeBaseTool knowledgeBaseTool;

    @Tool(name = "search_knowledge_base",
          description = "Search the Manchester United historical knowledge base for information about players, " +
                        "trophies, managers, seasons, and famous matches. " +
                        "Use this for ANY non-live historical question — e.g. '1999 Treble', 'Eric Cantona biography', " +
                        "'Ferguson greatest season', 'Old Trafford history'.")
    public List<Document> searchKnowledgeBase(String query, Integer topK) {
        log.info("Searching knowledge base: query='{}', topK={}", query, topK);
        return knowledgeBaseTool.search(query, topK != null ? topK : 5, null);
    }
}
