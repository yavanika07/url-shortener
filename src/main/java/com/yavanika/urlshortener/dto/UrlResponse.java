package com.yavanika.urlshortener.dto;

import java.time.LocalDateTime;

public record UrlResponse(
        String shortCode,
        String shortUrl,
        String originalUrl,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {
}
