package com.example.prompt.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AlanAiConfiguration {

    // application.yml 의 alan.base-url 값을 주입
    @Value("${alan.base-url}")
    private String baseUrl;

    // 앨런 AI 호출용 WebClient Bean 등록
    @Bean
    public WebClient alanWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                // 응답 최대 크기 10MB (기본값 256KB → 긴 AI 응답 대비)
                .codecs(config -> config
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}