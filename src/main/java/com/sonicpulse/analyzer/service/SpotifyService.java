package com.sonicpulse.analyzer.service;

import com.sonicpulse.analyzer.dto.ArtistDto;
import com.sonicpulse.analyzer.dto.CreatePlaylistDto;
import com.sonicpulse.analyzer.dto.PagingResponse;
import com.sonicpulse.analyzer.dto.SavePlaylistRequest;
import com.sonicpulse.analyzer.dto.SpotifySearchResponse;
import com.sonicpulse.analyzer.dto.TrackDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SpotifyService {

    private final RestClient restClient;
    private final GeminiService geminiService;

    public SpotifyService(GeminiService geminiService) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.spotify.com/v1")
                .build();

        this.geminiService = geminiService;
    }

    public PagingResponse<ArtistDto> getTopArtists(OAuth2AuthorizedClient client, String timeRange, Integer limit) {
        String token = client.getAccessToken().getTokenValue();
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/me/top/artists")
                        .queryParam("time_range", timeRange)
                        .queryParam("limit", limit)
                        .build()
                )
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<PagingResponse<ArtistDto>>() {});
    }

    public PagingResponse<TrackDto> getTopTracks(OAuth2AuthorizedClient client, String timeRange, Integer limit) {
        String token = client.getAccessToken().getTokenValue();

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/me/top/tracks")
                        .queryParam("time_range", timeRange)
                        .queryParam("limit", limit)
                .build()
                )
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<PagingResponse<TrackDto>>() {});
    }

    public List<TrackDto> getRecommendations(OAuth2AuthorizedClient client) {

        PagingResponse<ArtistDto> topArtistsPage = this.getTopArtists(client, "medium_term", 5);
        if (topArtistsPage == null || topArtistsPage.items() == null || topArtistsPage.items().isEmpty()) {
            return List.of();
        }

        List<String> myTopArtists = topArtistsPage.items().stream()
                .map(ArtistDto::name)
                .toList();

        List<String> geminiRecommended = this.geminiService.generateKeywordsFromTopArtists(myTopArtists);

        Set<String> targetArtists = new LinkedHashSet<>(myTopArtists);
        if (geminiRecommended != null) {
            targetArtists.addAll(geminiRecommended);
        }

        List<TrackDto> recommendedTracks = new ArrayList<>();

        for (String artist : targetArtists) {
            List<TrackDto> tracksForArtist = this.searchTracks(client, artist, 4);
            recommendedTracks.addAll(tracksForArtist);
        }

        if (recommendedTracks.isEmpty()) {
            return List.of();
        }

        Collections.shuffle(recommendedTracks);

        return recommendedTracks.stream().limit(15).toList();
    }

    public List<TrackDto> searchTracks(OAuth2AuthorizedClient client, String keyword, int targetCount) {
        String token = client.getAccessToken().getTokenValue();
        List<TrackDto> allTracksForKeyword = new ArrayList<>();

        int limitPerPage = 10;
        int offset = 0;

        String searchQuery = "artist:\"" + keyword + "\"";

        while (allTracksForKeyword.size() < targetCount) {

            final int currentOffset = offset;

            SpotifySearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", searchQuery)
                            .queryParam("type", "track")
                            .queryParam("limit", limitPerPage)
                            .queryParam("offset", currentOffset)
                            .build()
                    )
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .body(SpotifySearchResponse.class);

            if (response == null || response.tracks() == null || response.tracks().items().isEmpty()) {
                break;
            }

            allTracksForKeyword.addAll(response.tracks().items());

            offset += limitPerPage;
        }

        return allTracksForKeyword;
    }

    public List<TrackDto> generatePlaylistFromKeywords(OAuth2AuthorizedClient client, List<String> keywords, Integer limit) {
        if (keywords == null || keywords.isEmpty() || limit == null || limit <= 0) {
            return List.of();
        }

        List<List<TrackDto>> foundTracks = new ArrayList<>();

        for (String keyword : keywords) {
            List<TrackDto> tracks = this.searchTracks(client, keyword, 20);

            if (!tracks.isEmpty()) {
                foundTracks.add(tracks);
            }
        }

        Set<TrackDto> finalTracks = new LinkedHashSet<>();
        int maxTracksPerList = 20;

        for (int i = 0; i < maxTracksPerList; i++) {
            for (List<TrackDto> tracks : foundTracks) {
                if (i < tracks.size()) {
                    finalTracks.add(tracks.get(i));
                }

                if (finalTracks.size() >= limit) {
                    break;
                }
            }

            if (finalTracks.size() >= limit) {
                break;
            }
        }

        return new ArrayList<>(finalTracks);
    }

    public String savePlaylist(OAuth2AuthorizedClient client, SavePlaylistRequest request) {
        String accessToken = client.getAccessToken().getTokenValue();

        String playlistName = (request.name() != null && !request.name().isBlank())
                ? request.name()
                : "SonicPulse Playlist";

        CreatePlaylistDto spotifyBody = new CreatePlaylistDto(
                playlistName,
                false,
                "Generated by SonicPulse - AI playlist generator."
        );

        Map<?, ?> newPlaylist = restClient.post()
                .uri("/me/playlists")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(spotifyBody
                )
                .retrieve()
                .body(Map.class);

        if (newPlaylist == null || !newPlaylist.containsKey("id")) {
            throw new RuntimeException("Error creating playlist.");
        }

        String playlistId = newPlaylist.get("id").toString();

        List<String> cleanUris = request.uris().stream()
                .filter(uri -> uri != null && !uri.isBlank())
                .toList();

        if (cleanUris.isEmpty()) {
            return playlistId;
        }

        restClient.post()
                .uri("/playlists/{playlistId}/items", playlistId)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("uris", cleanUris))
                .retrieve()
                .toBodilessEntity();

        return playlistId;
    }

    public TrackDto swapTrack(OAuth2AuthorizedClient client, String keyword, List<String> excludedIds) {
        List<TrackDto> foundTracks =  this.searchTracks(client, keyword, 20);

        for (TrackDto track : foundTracks) {
            if (!excludedIds.contains(track.id())) {
                return track;
            }
        }

        throw new RuntimeException("No unique track found.");
    }
}