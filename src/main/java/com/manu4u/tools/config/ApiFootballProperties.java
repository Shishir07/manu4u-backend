package com.manu4u.tools.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "apifootball")
public class ApiFootballProperties {
    private String baseUrl = "https://v3.football.api-sports.io";
    private String apiKey;
}
