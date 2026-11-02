package com.yavanika.urlshortener.dto;

import jakarta.validation.constraints.*;

public record CreateUrlRequest(
        @NotBlank(message = "url is required")
        @Size(max = 2048, message = "url must be at most 2048 characters")
        String url,

        @Pattern(regexp = "^[a-zA-Z0-9_-]{3,32}$",
                message = "customAlias must be 3-32 characters: letters, digits, '-' or '_'")
        String customAlias,

        @Min(value = 1, message = "expiryDays must be at least 1")
        @Max(value = 365, message = "expiryDays must be at most 365")
        Integer expiryDays
) {
}
