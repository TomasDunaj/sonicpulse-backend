package com.sonicpulse.analyzer.service;

import com.sonicpulse.analyzer.dto.ArtistDto;
import com.sonicpulse.analyzer.dto.CreatePlaylistDto;
import com.sonicpulse.analyzer.dto.PagingResponse;
import com.sonicpulse.analyzer.dto.RecommendationFiltersDto;
import com.sonicpulse.analyzer.dto.SavePlaylistRequest;
import com.sonicpulse.analyzer.dto.SpotifySearchResponse;
import com.sonicpulse.analyzer.dto.TrackDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Service
public class SpotifyService {

    private final RestClient restClient;

    public SpotifyService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.spotify.com/v1")
                .build();
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

    public List<TrackDto> getRecommendations(OAuth2AuthorizedClient client, RecommendationFiltersDto recommendationFiltersDto) {
        String token = client.getAccessToken().getTokenValue();

        List<TrackDto> topTracks = this.getTopTracks(client, "medium_term", 3).items();

        if (topTracks.isEmpty()) {
            return List.of();
        }

        Random random = new Random();

        TrackDto randomTrack =  topTracks.get(random.nextInt(topTracks.size()));

        if (randomTrack.artists() == null || randomTrack.artists().isEmpty()) {
            return List.of();
        }

        String queryArtist = randomTrack.artists().get(random.nextInt(randomTrack.artists().size())).name();

        SpotifySearchResponse response =  restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", "artist:\"" + queryArtist + "\"")
                        .queryParam("type", "track")
                        .queryParam("limit", recommendationFiltersDto.limit() != null ? recommendationFiltersDto.limit() : 10)
                        .build()
                )
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(SpotifySearchResponse.class);

        return (response != null && response.tracks() != null) ? response.tracks().items() : List.of();
    }

    public List<TrackDto> searchTracks(OAuth2AuthorizedClient client, String keyword) {
        String token = client.getAccessToken().getTokenValue();

        SpotifySearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", keyword)
                        .queryParam("type", "track")
                        .build()
                )
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(SpotifySearchResponse.class);

        return response != null ? response.tracks().items() : List.of();
    }

    public List<TrackDto> generatePlaylistFromKeywords(OAuth2AuthorizedClient client, List<String> keywords, Integer limit) {
        List<TrackDto> tracks = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        for (String keyword : keywords) {
            if (tracks.size() >= limit) {
                break;
            }

            List<TrackDto> searchResults = searchTracks(client, keyword);

            int count = 0;
            for (TrackDto track : searchResults) {
                if (seenIds.add(track.id())) {
                    tracks.add(track);
                    count++;

                    if (tracks.size() >= limit) {
                        break;
                    }
                }

                if (count >= 3) {
                    break;
                }
            }
        }

        return tracks;
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
}