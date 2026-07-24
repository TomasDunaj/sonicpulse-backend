package com.sonicpulse.analyzer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;

@Service
public class GeminiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    public GeminiService() {
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
    }

    public record Part(String text){}

    public record Content(List<Part> parts){}

    public record GenerationConfig(String responseMimeType){}

    public record GeminiRequest(List<Content> contents, GenerationConfig generationConfig){}

    public List<String> extractKeyWords(String userPrompt) {
        String promptString = "Extract 3 to 5 relevant search keywords for Spotify based on this prompt. Return ONLY a JSON array of strings: " + userPrompt;

        GeminiRequest requestBody = new GeminiRequest(
                List.of(new Content(List.of(new Part(promptString)))),
                new GenerationConfig("application/json")
        );

        String rawResponse = restClient.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        try {
            com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(rawResponse);

            String jsonArrayText = rootNode
                    .path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            return objectMapper.readValue(
                    jsonArrayText,
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response", e);
        }
    }
}
