package com.sonicpulse.analyzer.dto;

import java.util.List;

public record SavePlaylistRequest(String name, List<String> uris) {
}
