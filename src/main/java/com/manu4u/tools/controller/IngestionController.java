package com.manu4u.tools.controller;

import com.manu4u.tools.service.ingestion.DocumentIngestionService;
import com.manu4u.tools.service.ingestion.StaticFileLoaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/ingest")
@RequiredArgsConstructor
@Tag(name = "Ingestion", description = "Knowledge base ingestion endpoints (dev/admin use)")
public class IngestionController {

    private final StaticFileLoaderService staticFileLoaderService;
    private final DocumentIngestionService documentIngestionService;

    @Operation(summary = "Ingest all .txt files from the knowledge directory",
               description = "Scans src/main/resources/knowledge/, chunks and embeds each .txt file. " +
                             "Each file can have a companion .meta.json sidecar for metadata.")
    @PostMapping("/files")
    public ResponseEntity<Map<String, Object>> ingestFiles() throws IOException {
        log.info("Starting bulk file ingestion");
        try {
            int chunksIngested = staticFileLoaderService.ingestAllFiles();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ok");
            response.put("chunksIngested", chunksIngested);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Ingestion failed — is Qdrant running? (docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant)", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Qdrant unavailable: " + e.getMessage());
            response.put("hint", "Start Qdrant: docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    @Operation(summary = "Ingest a single text block",
               description = "Provide raw text and metadata inline. Useful for testing or one-off ingestion.")
    @PostMapping("/text")
    public ResponseEntity<Map<String, Object>> ingestText(@Valid @RequestBody IngestTextRequest request) {
        log.info("Ingesting text: source='{}', contentType='{}'", request.getSource(), request.getContentType());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", request.getSource());
        metadata.put("contentType", request.getContentType() != null ? request.getContentType() : "history");
        if (request.getEra() != null)        metadata.put("era", request.getEra());
        if (request.getPlayerName() != null) metadata.put("playerName", request.getPlayerName());
        if (request.getSeason() != null)     metadata.put("season", request.getSeason());

        try {
            int chunksIngested = documentIngestionService.ingest(request.getText(), metadata);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ok");
            response.put("source", request.getSource());
            response.put("chunksIngested", chunksIngested);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Text ingestion failed — is Qdrant running?", e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Qdrant unavailable: " + e.getMessage());
            response.put("hint", "Start Qdrant: docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    @Data
    public static class IngestTextRequest {
        @NotBlank(message = "text must not be blank")
        @Size(min = 10, message = "text must be at least 10 characters")
        private String text;

        @NotBlank(message = "source must not be blank")
        private String source;

        /** history | player_bio | season_review | trophy | news */
        private String contentType;
        private String era;
        private String playerName;
        private String season;
    }
}
