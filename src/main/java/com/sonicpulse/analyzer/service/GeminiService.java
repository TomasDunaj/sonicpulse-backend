package com.sonicpulse.analyzer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sonicpulse.analyzer.enums.GenerationMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

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

    public record Candidate(Content content) {}
    public record GeminiResponse(List<Candidate> candidates) {}

    public List<String> extractKeyWords(String userPrompt, GenerationMode mode) {
        String systemInstruction;

        if (mode == GenerationMode.RESTRICTIVE) {
            systemInstruction = """
        Extract ONLY the exact artist or band names mentioned in the prompt as a JSON array of strings.
        
        CRITICAL RULES FOR RESTRICTIVE MODE:
        - Return ONLY the exact artists requested.
        - ABSOLUTELY NO similar artists, genres, or descriptive words.
        - Do NOT use 'artist:' syntax or quotes inside the strings.
        - Output format example: ["Calin"]
        """;
        } else {
            systemInstruction = """
        Analyze the prompt and extract search keywords as a JSON array of strings.
        
        RULES FOR OPEN-MINDED MODE:
        - Include the main artist/band from the prompt.
        - Feel free to use your creativity to add similar artists, bands, or matching genres.
        - Return clean strings, no special syntax like 'artist:' or quotes.
        - Example output: ["Calin", "Viktor Sheen", "czsk pop", "rap"]
        """;
        }

        double temperature = (mode == GenerationMode.RESTRICTIVE) ? 0.0 : 0.2;

        return callGeminiForKeywords(userPrompt, systemInstruction, temperature, List.of("top hits"));
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

        return callGeminiForKeywords(inputPrompt, systemInstruction, 0.2, List.of("Glass Animals", "Foals", "The Strokes"));
    }

    private List<String> callGeminiForKeywords(String userPrompt, String instructions, double temperature, List<String> fallbackKeywords) {

        GeminiRequest requestBody = new GeminiRequest(
                List.of(new Content(List.of(new Part(userPrompt)))),
                new Content(List.of(new Part(instructions))),
                new GenerationConfig(temperature, "application/json")
        );

        try {
            GeminiResponse response = restClient.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=" + apiKey)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(GeminiResponse.class);

            String jsonText = response.candidates().get(0).content().parts().get(0).text();

            return objectMapper.readValue(jsonText, new TypeReference<List<String>>() {});

        } catch (HttpClientErrorException.TooManyRequests e) {
            System.err.println("Gemini API limit reached (429)! Using fallback keywords.");
            return fallbackKeywords;

        } catch (Exception e) {
            System.err.println("Error calling Gemini: " + e.getMessage());
            return fallbackKeywords;
        }
    }
}
