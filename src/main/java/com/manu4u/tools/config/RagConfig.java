package com.manu4u.tools.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        // chunkSize=512 tokens, minChunkSizeChars=50, minChunkLengthToEmbed=5,
        // maxNumChunks=10000, keepSeparator=true
        return new TokenTextSplitter(512, 50, 5, 10000, true);
    }
}
