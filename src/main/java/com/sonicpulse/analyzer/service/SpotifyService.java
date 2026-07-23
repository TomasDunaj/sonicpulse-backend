package com.sonicpulse.analyzer.service;

import com.sonicpulse.analyzer.dto.ArtistDto;
import com.sonicpulse.analyzer.dto.PagingResponse;
import com.sonicpulse.analyzer.dto.RecommendationFiltersDto;
import com.sonicpulse.analyzer.dto.SpotifySearchResponse;
import com.sonicpulse.analyzer.dto.TrackDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Random;

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
}