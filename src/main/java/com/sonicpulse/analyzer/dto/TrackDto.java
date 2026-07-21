package com.sonicpulse.analyzer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TrackDto(String id, String name, @JsonProperty("duration_ms") Integer durationMs, Integer popularity, AlbumDto album, List<ArtistDto> artists, @JsonProperty("external_urls") ExternalUrls externalUrls) {
    public record AlbumDto(String id, String name, List<ImageDto> images, @JsonProperty("release_date") String releaseDate) {}
}
