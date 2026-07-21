package com.sonicpulse.analyzer.dto;

import java.util.List;

public record PagingResponse<T>(List<T> items, Integer total, Integer limit, Integer offset) {
}
