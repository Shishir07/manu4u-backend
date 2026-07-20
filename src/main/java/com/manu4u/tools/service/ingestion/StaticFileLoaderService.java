package com.manu4u.tools.service.ingestion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the curated knowledge corpus from src/main/resources/knowledge/ into the
 * vector store. Files are flat, prefix-categorized markdown (era-, moment-, legend-,
 * club-, season-, topical-) — see CORPUS_GUIDE.md for authoring conventions.
 *
 * Metadata resolution per file (later layers override earlier ones):
 *   1. filename-derived defaults (source, contentType=history)
 *   2. optional {basename}.meta.json sidecar
 *   3. the in-file header block (TOPIC / CATEGORY / AS OF / STATUS) via KnowledgeHeaderParser
 *
 * Ingestion is idempotent: each file replaces its previous points (delete-by-source
 * before add), so re-running this — restart, corpus update, repeated endpoint calls —
 * never duplicates chunks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaticFileLoaderService {

    private final DocumentIngestionService documentIngestionService;
    private final KnowledgeHeaderParser headerParser;
    private final ObjectMapper objectMapper;

    /** Corpus is markdown by convention; legacy .txt still accepted. */
    private static final String[] KNOWLEDGE_PATTERNS = {
            "classpath:knowledge/*.md",
            "classpath:knowledge/*.txt"
    };

    /**
     * Scan the knowledge directory and (re-)ingest every corpus file.
     *
     * @return total chunks ingested across all files
     */
    public int ingestAllFiles() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        List<Resource> resources = new ArrayList<>();
        for (String pattern : KNOWLEDGE_PATTERNS) {
            for (Resource r : resolver.getResources(pattern)) {
                resources.add(r);
            }
        }

        if (resources.isEmpty()) {
            log.warn("No corpus files (.md/.txt) found in classpath:knowledge/");
            return 0;
        }

        int totalChunks = 0;
        List<String> failed = new ArrayList<>();

        for (Resource resource : resources) {
            String filename = resource.getFilename();
            try {
                log.info("Loading corpus file: {}", filename);

                String text = FileCopyUtils.copyToString(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

                String source = stripExtension(filename);

                // Layered metadata: sidecar first, then the in-file header wins.
                Map<String, Object> metadata = new LinkedHashMap<>(loadSidecarMetadata(resolver, source));
                metadata.putAll(headerParser.parse(source, text));

                totalChunks += documentIngestionService.ingestReplacing(
                        source, text, metadata, DocumentIngestionService.SplitStrategy.SPLIT);
            } catch (Exception e) {
                log.error("Failed to ingest corpus file: {}", filename, e);
                failed.add(filename);
            }
        }

        if (!failed.isEmpty()) {
            log.warn("Failed to ingest {} file(s): {}", failed.size(), failed);
        }

        log.info("Corpus ingestion complete: {} file(s) processed, {} failed, {} total chunks",
                resources.size(), failed.size(), totalChunks);
        return totalChunks;
    }

    /** Optional {basename}.meta.json sidecar — extra metadata the header can't express. */
    private Map<String, Object> loadSidecarMetadata(PathMatchingResourcePatternResolver resolver,
                                                    String baseName) {
        String metaPattern = "classpath:knowledge/" + baseName + ".meta.json";
        try {
            Resource[] metaResources = resolver.getResources(metaPattern);
            if (metaResources.length > 0 && metaResources[0].exists()) {
                String metaJson = FileCopyUtils.copyToString(
                        new InputStreamReader(metaResources[0].getInputStream(), StandardCharsets.UTF_8));
                return objectMapper.readValue(metaJson, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.debug("No usable metadata sidecar for '{}': {}", baseName, e.getMessage());
        }
        return Map.of();
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
