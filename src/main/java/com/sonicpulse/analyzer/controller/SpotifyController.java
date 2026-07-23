package com.sonicpulse.analyzer.controller;

import com.sonicpulse.analyzer.dto.ArtistDto;
import com.sonicpulse.analyzer.dto.PagingResponse;
import com.sonicpulse.analyzer.dto.RecommendationFiltersDto;
import com.sonicpulse.analyzer.dto.TrackDto;
import com.sonicpulse.analyzer.service.SpotifyService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/spotify")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"}, allowCredentials = "true")
public class SpotifyController {
    private final SpotifyService spotifyService;

    public SpotifyController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
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
    public List<TrackDto> getRecommendations(@RegisteredOAuth2AuthorizedClient("spotify") OAuth2AuthorizedClient client, RecommendationFiltersDto recommendationFiltersDto) {
        return  this.spotifyService.getRecommendations(client, recommendationFiltersDto);
    }
}
