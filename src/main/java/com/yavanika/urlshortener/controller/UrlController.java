package com.yavanika.urlshortener.controller;

import com.yavanika.urlshortener.dto.CreateUrlRequest;
import com.yavanika.urlshortener.dto.UrlResponse;
import com.yavanika.urlshortener.dto.UrlStatsResponse;
import com.yavanika.urlshortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping("/api/urls")
    public ResponseEntity<UrlResponse> create(@Valid @RequestBody CreateUrlRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(urlService.shorten(request));
    }

    @GetMapping("/api/urls")
    public List<UrlResponse> list() {
        return urlService.listAll();
    }

    @GetMapping("/api/urls/{code}/stats")
    public UrlStatsResponse stats(@PathVariable String code) {
        return urlService.getStats(code);
    }

    @DeleteMapping("/api/urls/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        urlService.delete(code);
        return ResponseEntity.noContent().build();
    }

    /** The public short link: GET /{code} -> 302 redirect to the original URL. */
    @GetMapping("/{code:[a-zA-Z0-9_-]+}")
    public ResponseEntity<Void> redirect(@PathVariable String code,
                                         @RequestHeader(value = HttpHeaders.REFERER, required = false) String referrer,
                                         @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        String target = urlService.resolveAndRecordClick(code, referrer, userAgent);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }
}
