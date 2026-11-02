package com.yavanika.urlshortener.repository;

import com.yavanika.urlshortener.model.ClickEvent;
import com.yavanika.urlshortener.model.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    List<ClickEvent> findByShortUrlOrderByClickedAtDesc(ShortUrl shortUrl);
}
