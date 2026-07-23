package com.sonicpulse.analyzer.dto;

public record SpotifySearchResponse(
        PagingResponse<TrackDto> tracks
) {
}
