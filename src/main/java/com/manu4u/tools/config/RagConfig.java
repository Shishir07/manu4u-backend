package com.manu4u.tools.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    /**
     * Splitter for CURATED corpus documents only (raw news items are never split —
     * see DocumentIngestionService.SplitStrategy).
     *
     * chunkSize=800 tokens: matches how the corpus is authored (CORPUS_GUIDE.md —
     * one topic per file, ~300–600 words). At 800, 13 of the 18 seed files fit in a
     * single chunk (chunk == document, the ideal for a one-topic-per-file corpus);
     * at the previous 512, the same files split into a full chunk plus a ~150-token
     * orphan tail with weak retrieval signal.
     *
     * Note: TokenTextSplitter has NO overlap parameter — the corpus guide's
     * "150 overlap" is not achievable with this splitter. The guide's
     * every-paragraph-stands-alone rule is what actually protects chunk boundaries.
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        // chunkSize=800 tokens, minChunkSizeChars=50, minChunkLengthToEmbed=5,
        // maxNumChunks=10000, keepSeparator=true
        return new TokenTextSplitter(800, 50, 5, 10000, true);
    }
}
