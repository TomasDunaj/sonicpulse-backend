package com.sonicpulse.analyzer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ArtistDto(String id, String name, List<String> genres, Integer popularity, List<ImageDto> images, @JsonProperty("external_urls") ExternalUrls externalUrls) {
}
