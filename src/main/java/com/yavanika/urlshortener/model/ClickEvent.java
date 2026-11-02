package com.yavanika.urlshortener.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "click_events")
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "short_url_id")
    private ShortUrl shortUrl;

    @Column(nullable = false)
    private LocalDateTime clickedAt;

    @Column(length = 512)
    private String referrer;

    @Column(length = 512)
    private String userAgent;

    protected ClickEvent() {
    }

    public ClickEvent(ShortUrl shortUrl, String referrer, String userAgent) {
        this.shortUrl = shortUrl;
        this.referrer = truncate(referrer);
        this.userAgent = truncate(userAgent);
        this.clickedAt = LocalDateTime.now();
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() > 512 ? s.substring(0, 512) : s;
    }

    public Long getId() { return id; }
    public ShortUrl getShortUrl() { return shortUrl; }
    public LocalDateTime getClickedAt() { return clickedAt; }
    public String getReferrer() { return referrer; }
    public String getUserAgent() { return userAgent; }
}
