package com.yavanika.urlshortener.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public record UrlStatsResponse(
        String shortCode,
        String originalUrl,
        long totalClicks,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime lastClickedAt,
        Map<LocalDate, Long> clicksByDay,
        Map<String, Long> topReferrers
) {
}
