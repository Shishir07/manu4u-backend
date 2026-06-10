package com.manu4u.tools.service.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
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
     * Chunk, embed, and store a single document.
     *
     * @param rawText  Full text content
     * @param metadata Map of: source, contentType, era, playerName, season (all optional except source)
     * @return number of chunks ingested
     */
    public int ingest(String rawText, Map<String, Object> metadata) {
        log.info("Ingesting document: source='{}', contentType='{}'",
                metadata.get("source"), metadata.get("contentType"));

        Document doc = new Document(rawText, metadata);
        List<Document> chunks = splitter.apply(List.of(doc));

        vectorStore.add(chunks);

        log.info("Ingested {} chunks for source '{}'", chunks.size(), metadata.get("source"));
        return chunks.size();
    }
}
