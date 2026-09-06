package com.sonicpulse.analyzer.dto;

import java.util.List;

public record SwapRequestDto(String keyword, List<String> excludedIds) {
}
