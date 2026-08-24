package com.sonicpulse.analyzer.service;

import com.sonicpulse.analyzer.dto.ArtistDto;
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

    public List<TrackDto> generatePlaylistFromKeywords(OAuth2AuthorizedClient client, List<String> keywords) {
        List<TrackDto> tracks = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        for (String keyword : keywords) {
            List<TrackDto> searchResults = searchTracks(client, keyword);

            int count = 0;
            for (TrackDto track : searchResults) {
                if (seenIds.add(track.id())) {
                    tracks.add(track);
                    count++;
                }

                if (count >= 3) {
                    break;
                }
            }
        }

        return tracks;
    }

    public String savePlaylist(String accessToken,  SavePlaylistRequest request) {
        Map<?, ?> userProfile = restClient.get()
                .uri("https://api.spotify.com/v1/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        String userId = userProfile.get("id").toString();

        Map<?,?> newPlaylist = restClient.post()
                .uri("https://api.spotify.com/v1/users/" + userId + "/playlists")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", request.name(), "public", false))
                .retrieve()
                .body(Map.class);

        String playlistId = newPlaylist.get("id").toString();

        restClient.post()
                .uri("https://api.spotify.com/v1/playlists/" + playlistId + "/tracks")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("uris", request.uris()))
                .retrieve()
                .toBodilessEntity();

        return playlistId;
    }
}