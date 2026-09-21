package com.sonicpulse.analyzer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
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

    public record Part(String text) {
    }

    public record Content(List<Part> parts) {
    }

    public record GenerationConfig(Double temperature, String responseMimeType) {
    }

    public record GeminiRequest(
            List<Content> contents,
            Content systemInstruction,
            GenerationConfig generationConfig) {
    }

    public List<String> extractKeyWords(String userPrompt) {
        String systemInstruction = """
                Extract 3-5 Spotify search keywords from the prompt as a JSON array of strings.
                
                RULES:
                - If user uses restrictive words ("only", "exclusively"), return ONLY filters for those exact artists (e.g. artist:"Calin"). NO other artists.
                - If prompt is open-ended, feel free to include similar artists or genres.
                - Always use artist:"Name" syntax for artist names.
                """;

        return callGeminiForKeywords(userPrompt, systemInstruction, List.of("top hits"));
    }

    public List<String> generateKeywordsFromTopArtists(List<String> artistNames) {
        if (artistNames == null || artistNames.isEmpty()) {
            return List.of("Glass Animals", "Foals", "The Strokes");
        }

        String inputPrompt = "My favorite artists are: " + String.join(", ", artistNames);

        String systemInstruction = """
            Based on the provided list of favorite artists, generate 3-5 similar well-known artist names to discover tracks on Spotify.
            Return a JSON array of strings.
            
            RULES:
            - Return ONLY clean artist/band names.
            - Do NOT use genres (no 'indie', 'rock', 'post-punk').
            - Do NOT use special syntax like 'artist:', quotes, or hyphenation unless it's part of the real band name.
            - Example output: ["Glass Animals", "Foals", "The Strokes"]
            """;

        return callGeminiForKeywords(inputPrompt, systemInstruction, List.of("Glass Animals", "Foals", "The Strokes"));
    }

    private List<String> callGeminiForKeywords(String userPrompt, String instructions, List<String> fallbackKeywords) {

        GeminiRequest requestBody = new GeminiRequest(
                List.of(new Content(List.of(new Part(userPrompt)))),
                new Content(List.of(new Part(instructions))),
                new GenerationConfig(0.2, "application/json")
        );

        try {
            String rawResponse = restClient.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=" + apiKey)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode rootNode = objectMapper.readTree(rawResponse);

            String jsonArrayText = rootNode
                    .path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            return objectMapper.readValue(
                    jsonArrayText,
                    new TypeReference<List<String>>() {
                    }
            );

        } catch (HttpClientErrorException.TooManyRequests e) {
            System.err.println("Gemini API limit reached (429)! Using fallback keywords.");
            return fallbackKeywords;

        } catch (Exception e) {
            System.err.println("Error calling Gemini: " + e.getMessage());
            return fallbackKeywords;
        }
    }
}
