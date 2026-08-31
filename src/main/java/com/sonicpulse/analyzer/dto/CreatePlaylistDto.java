package com.sonicpulse.analyzer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreatePlaylistDto(@JsonProperty("name") String name, @JsonProperty("public") Boolean isPublic, @JsonProperty("description") String description) {
}
