package com.manu4u.tools.service.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the plain-text header block that every knowledge corpus file starts with
 * (see CORPUS_GUIDE.md):
 *
 * <pre>
 * TOPIC: Alex Ferguson's rise — from near-sacking to the Treble (1986–1999)
 * CATEGORY: era   AS OF: 2026-07   STATUS: stable-history
 * </pre>
 *
 * Fields may appear on one line or across several; AS OF may be YYYY-MM or YYYY-MM-DD;
 * STATUS may carry a parenthetical note which is stripped during normalisation.
 *
 * The parsed fields become Qdrant point metadata (copied onto every chunk by the
 * splitter), which is what enables downstream filtering and ranking:
 * <ul>
 *   <li>{@code category} / {@code contentType} — era | moment | legend | club | season | topical | news</li>
 *   <li>{@code status} — stable-history | needs-update | volatile | current | archived</li>
 *   <li>{@code asOf} (string) + {@code asOfEpoch} (numeric, epoch seconds) — recency ranking + range filters</li>
 * </ul>
 *
 * Files without a header are not an error — they get filename-derived defaults.
 * This component is intentionally ingestion-source-agnostic so the news ingester
 * can reuse it for raw item files.
 */
@Slf4j
@Component
public class KnowledgeHeaderParser {

    /** Only scan the top of the file — the header block is always first. */
    private static final int HEADER_SCAN_CHARS = 600;

    private static final Pattern TOPIC    = Pattern.compile("(?m)^TOPIC:\\s*(.+?)\\s*$");
    private static final Pattern CATEGORY = Pattern.compile("CATEGORY:\\s*([A-Za-z][A-Za-z-]*)");
    private static final Pattern AS_OF    = Pattern.compile("AS OF:\\s*(\\d{4}-\\d{2}(?:-\\d{2})?)");
    private static final Pattern STATUS   = Pattern.compile("STATUS:\\s*([A-Za-z][A-Za-z-]*)");

    /**
     * Parse the header block of a corpus document into ingestion metadata.
     *
     * @param source   stable document id — filename without extension (e.g. "era-1986-1999-ferguson-rise")
     * @param rawText  full document text (header included — the header stays in the
     *                 embedded text on purpose; it is entity-rich and aids retrieval)
     * @return metadata map: source always present; topic/category/contentType/status/asOf/asOfEpoch when found
     */
    public Map<String, Object> parse(String source, String rawText) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", source);

        String head = rawText.length() > HEADER_SCAN_CHARS
                ? rawText.substring(0, HEADER_SCAN_CHARS)
                : rawText;

        Matcher m = TOPIC.matcher(head);
        if (m.find()) metadata.put("topic", m.group(1));

        m = CATEGORY.matcher(head);
        if (m.find()) {
            String category = m.group(1).toLowerCase();
            metadata.put("category", category);
            metadata.put("contentType", category);
        } else {
            metadata.put("contentType", "history"); // pre-convention files
        }

        m = STATUS.matcher(head);
        if (m.find()) metadata.put("status", m.group(1).toLowerCase());

        m = AS_OF.matcher(head);
        if (m.find()) {
            String asOf = m.group(1);
            metadata.put("asOf", asOf);
            Long epoch = toEpochSeconds(asOf);
            if (epoch != null) metadata.put("asOfEpoch", epoch);
        }

        if (!metadata.containsKey("topic")) {
            log.debug("No header block found in '{}' — using filename-derived defaults", source);
        }
        return metadata;
    }

    /** YYYY-MM-DD → that day; YYYY-MM → first of month. Midnight UTC, epoch seconds. */
    private Long toEpochSeconds(String asOf) {
        try {
            LocalDate date = (asOf.length() == 7)
                    ? YearMonth.parse(asOf).atDay(1)
                    : LocalDate.parse(asOf);
            return date.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        } catch (Exception e) {
            log.warn("Unparseable AS OF date '{}': {}", asOf, e.getMessage());
            return null;
        }
    }
}
