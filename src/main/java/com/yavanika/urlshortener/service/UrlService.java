package com.yavanika.urlshortener.service;

import com.yavanika.urlshortener.dto.CreateUrlRequest;
import com.yavanika.urlshortener.dto.UrlResponse;
import com.yavanika.urlshortener.dto.UrlStatsResponse;
import com.yavanika.urlshortener.exception.ApiExceptions;
import com.yavanika.urlshortener.model.ClickEvent;
import com.yavanika.urlshortener.model.ShortUrl;
import com.yavanika.urlshortener.repository.ClickEventRepository;
import com.yavanika.urlshortener.repository.ShortUrlRepository;
import com.yavanika.urlshortener.util.Base62Encoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UrlService {

    /** Added to the DB id before encoding so the first codes are not "1", "2", "3"... */
    private static final long ID_OFFSET = 100_000L;

    private final ShortUrlRepository urlRepository;
    private final ClickEventRepository clickRepository;
    private final String baseUrl;

    public UrlService(ShortUrlRepository urlRepository,
                      ClickEventRepository clickRepository,
                      @Value("${app.base-url}") String baseUrl) {
        this.urlRepository = urlRepository;
        this.clickRepository = clickRepository;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Transactional
    public UrlResponse shorten(CreateUrlRequest request) {
        String url = normalizeAndValidate(request.url());

        LocalDateTime expiresAt = request.expiryDays() == null
                ? null
                : LocalDateTime.now().plusDays(request.expiryDays());

        ShortUrl entity = new ShortUrl(url, expiresAt);

        if (request.customAlias() != null && !request.customAlias().isBlank()) {
            String alias = request.customAlias();
            if (urlRepository.existsByShortCode(alias)) {
                throw new ApiExceptions.ConflictException("Alias '" + alias + "' is already taken");
            }
            entity.setShortCode(alias);
            entity = urlRepository.save(entity);
        } else {
            // Save first to get the auto-generated id, then derive the code from it.
            entity = urlRepository.save(entity);
            String code = Base62Encoder.encode(entity.getId() + ID_OFFSET);
            // Guard against clashing with a custom alias someone already picked.
            while (urlRepository.existsByShortCode(code)) {
                code = code + Base62Encoder.encode(new Random().nextInt(62));
            }
            entity.setShortCode(code);
        }
        return toResponse(entity);
    }

    @Transactional
    public String resolveAndRecordClick(String code, String referrer, String userAgent) {
        ShortUrl url = urlRepository.findByShortCode(code)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Short URL '" + code + "' not found"));
        if (url.isExpired()) {
            throw new ApiExceptions.GoneException("Short URL '" + code + "' has expired");
        }
        url.incrementClicks();
        clickRepository.save(new ClickEvent(url, referrer, userAgent));
        return url.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public UrlStatsResponse getStats(String code) {
        ShortUrl url = urlRepository.findByShortCode(code)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Short URL '" + code + "' not found"));

        List<ClickEvent> clicks = clickRepository.findByShortUrlOrderByClickedAtDesc(url);

        Map<LocalDate, Long> byDay = clicks.stream()
                .collect(Collectors.groupingBy(c -> c.getClickedAt().toLocalDate(),
                        TreeMap::new, Collectors.counting()));

        Map<String, Long> referrers = clicks.stream()
                .map(c -> c.getReferrer() == null || c.getReferrer().isBlank() ? "direct" : c.getReferrer())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        LocalDateTime lastClicked = clicks.isEmpty() ? null : clicks.get(0).getClickedAt();

        return new UrlStatsResponse(url.getShortCode(), url.getOriginalUrl(), url.getClickCount(),
                url.getCreatedAt(), url.getExpiresAt(), lastClicked, byDay, referrers);
    }

    @Transactional(readOnly = true)
    public List<UrlResponse> listAll() {
        return urlRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(String code) {
        ShortUrl url = urlRepository.findByShortCode(code)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("Short URL '" + code + "' not found"));
        clickRepository.deleteAll(clickRepository.findByShortUrlOrderByClickedAtDesc(url));
        urlRepository.delete(url);
    }

    private String normalizeAndValidate(String raw) {
        String url = raw.trim();
        if (!url.matches("(?i)^https?://.*")) {
            url = "https://" + url;
        }
        try {
            URI uri = new URI(url);
            if (uri.getHost() == null || !uri.getHost().contains(".")) {
                throw new ApiExceptions.BadRequestException("Invalid URL: " + raw);
            }
        } catch (URISyntaxException e) {
            throw new ApiExceptions.BadRequestException("Invalid URL: " + raw);
        }
        return url;
    }

    private UrlResponse toResponse(ShortUrl u) {
        return new UrlResponse(u.getShortCode(), baseUrl + "/" + u.getShortCode(),
                u.getOriginalUrl(), u.getCreatedAt(), u.getExpiresAt());
    }
}
