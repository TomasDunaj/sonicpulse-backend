package com.sonicpulse.analyzer.controller;

import com.sonicpulse.analyzer.dto.ArtistDto;
import com.sonicpulse.analyzer.dto.PagingResponse;
import com.sonicpulse.analyzer.dto.SavePlaylistRequest;
import com.sonicpulse.analyzer.dto.SwapRequestDto;
import com.sonicpulse.analyzer.dto.TrackDto;
import com.sonicpulse.analyzer.enums.GenerationMode;
import com.sonicpulse.analyzer.service.GeminiService;
import com.sonicpulse.analyzer.service.SpotifyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spotify")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class SpotifyController {
    private final SpotifyService spotifyService;
    private final GeminiService geminiService;

    public SpotifyController(SpotifyService spotifyService, GeminiService geminiService) {
        this.spotifyService = spotifyService;
        this.geminiService = geminiService;
    }

    @GetMapping("/top-artists")
    public PagingResponse<ArtistDto> getTopArtists(@RegisteredOAuth2AuthorizedClient("spotify") OAuth2AuthorizedClient client, @RequestParam(defaultValue = "medium_term") String timeRange, @RequestParam(defaultValue = "20") Integer limit) {
        return this.spotifyService.getTopArtists(client, timeRange, limit);
    }

    @GetMapping("/top-tracks")
    public PagingResponse<TrackDto> getTopTracks(@RegisteredOAuth2AuthorizedClient("spotify") OAuth2AuthorizedClient client, @RequestParam(defaultValue = "medium_term") String timeRange, @RequestParam(defaultValue = "20") Integer limit) {
        return this.spotifyService.getTopTracks(client, timeRange, limit);
    }

    @GetMapping("/recommendations")
    public List<TrackDto> getRecommendations(@RegisteredOAuth2AuthorizedClient("spotify") OAuth2AuthorizedClient client) {
        return  this.spotifyService.getRecommendations(client);
    }

    @GetMapping("/generate-from-prompt")
    public List<TrackDto> generateFromPrompt(@RegisteredOAuth2AuthorizedClient("spotify") OAuth2AuthorizedClient client, @RequestParam String prompt, @RequestParam GenerationMode generationMode, @RequestParam(defaultValue = "10") Integer limit) {
        List<String> keywords = this.geminiService.extractKeyWords(prompt, generationMode);

        return this.spotifyService.generatePlaylistFromKeywords(client, keywords, generationMode, limit);
    }

    @PostMapping("/save-playlist")
    public ResponseEntity<?> savePlaylist(@RegisteredOAuth2AuthorizedClient("spotify") OAuth2AuthorizedClient client, @RequestBody SavePlaylistRequest savePlaylistRequest) {
        String playlistId = spotifyService.savePlaylist(client, savePlaylistRequest);

        return ResponseEntity.ok(Map.of("message", "Playlist was sucessfully created",
                "playlistId", playlistId
        ));
    }

    @PostMapping("/swap-track")
    public ResponseEntity<TrackDto> swapTracks(@RegisteredOAuth2AuthorizedClient("spotify")  OAuth2AuthorizedClient client, @RequestBody SwapRequestDto swapRequestDto) {
        return ResponseEntity.ok(this.spotifyService.swapTrack(client, swapRequestDto.keyword(), swapRequestDto.excludedIds()));
    }
}
