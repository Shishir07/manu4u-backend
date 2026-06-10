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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaticFileLoaderService {

    private final DocumentIngestionService documentIngestionService;
    private final ObjectMapper objectMapper;

    private static final String KNOWLEDGE_DIR = "classpath:knowledge/*.txt";

    /**
     * Scan the knowledge directory, ingest all .txt files.
     * Each .txt file may have a companion {filename}.meta.json sidecar for metadata.
     *
     * @return total chunks ingested across all files
     */
    public int ingestAllFiles() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(KNOWLEDGE_DIR);

        if (resources.length == 0) {
            log.warn("No .txt files found in knowledge directory ({})", KNOWLEDGE_DIR);
            return 0;
        }

        int totalChunks = 0;
        List<String> failed = new ArrayList<>();

        for (Resource resource : resources) {
            try {
                String filename = resource.getFilename();
                log.info("Loading file: {}", filename);

                String text = FileCopyUtils.copyToString(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

                Map<String, Object> metadata = loadMetadata(resolver, filename);

                int chunks = documentIngestionService.ingest(text, metadata);
                totalChunks += chunks;
            } catch (Exception e) {
                log.error("Failed to ingest file: {}", resource.getFilename(), e);
                failed.add(resource.getFilename());
            }
        }

        if (!failed.isEmpty()) {
            log.warn("Failed to ingest {} file(s): {}", failed.size(), failed);
        }

        log.info("Ingestion complete: {} files processed, {} total chunks", resources.length, totalChunks);
        return totalChunks;
    }

    private Map<String, Object> loadMetadata(PathMatchingResourcePatternResolver resolver, String filename) {
        String baseName = filename.replace(".txt", "");
        String metaPattern = "classpath:knowledge/" + baseName + ".meta.json";

        try {
            Resource[] metaResources = resolver.getResources(metaPattern);
            if (metaResources.length > 0) {
                String metaJson = FileCopyUtils.copyToString(
                        new InputStreamReader(metaResources[0].getInputStream(), StandardCharsets.UTF_8));
                return objectMapper.readValue(metaJson, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.debug("No metadata sidecar found for '{}', using filename-derived defaults", filename);
        }

        // Fall back to filename-derived metadata
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("source", baseName);
        defaults.put("contentType", "history");
        return defaults;
    }
}
