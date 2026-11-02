# URL Shortener with Click Analytics

A REST API that turns long URLs into short links, redirects visitors, and records analytics for every click.
Built with **Java 17+, Spring Boot 3, Spring Data JPA, PostgreSQL and Docker**.

## Features

- **Base62 short codes** generated from the database ID (`0-9a-zA-Z`), e.g. `100001 → q0V`
- **Custom aliases** (`/my-portfolio`) with a conflict check
- **Link expiry** (1–365 days); expired links return `410 Gone`
- **Click analytics**: total clicks, clicks per day, top 5 referrers, last-clicked time
- Input validation and consistent JSON error responses
- Runs with PostgreSQL via Docker Compose, or in-memory H2 for quick testing
- Integration tests with MockMvc

## Architecture

```
Client ──► UrlController ──► UrlService ──► ShortUrlRepository ──► PostgreSQL
              │                   │                                 (short_urls)
              │                   └──────► ClickEventRepository ──► (click_events)
              └── GET /{code} → 302 Found (Location: original URL)
```

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/urls` | Create a short link |
| GET | `/{code}` | Redirect to the original URL (records a click) |
| GET | `/api/urls/{code}/stats` | Click analytics for a link |
| GET | `/api/urls` | List all links |
| DELETE | `/api/urls/{code}` | Delete a link and its click history |

### Create a link

```bash
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{"url":"https://github.com/yavanika07","customAlias":"yavanika","expiryDays":30}'
```

```json
{
  "shortCode": "yavanika",
  "shortUrl": "http://localhost:8080/yavanika",
  "originalUrl": "https://github.com/yavanika07",
  "createdAt": "2026-11-02T21:10:00",
  "expiresAt": "2026-12-02T21:10:00"
}
```

`customAlias` and `expiryDays` are optional. Without an alias, a Base62 code is generated.

### Stats

```bash
curl http://localhost:8080/api/urls/yavanika/stats
```

```json
{
  "shortCode": "yavanika",
  "totalClicks": 3,
  "lastClickedAt": "2026-11-02T21:15:42",
  "clicksByDay": { "2026-11-02": 3 },
  "topReferrers": { "https://linkedin.com": 2, "direct": 1 }
}
```

## Run it

**Option 1 – Docker (app + PostgreSQL):**
```bash
docker compose up --build
```

**Option 2 – Without a database (H2 in memory):**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

**Option 3 – Local PostgreSQL:** create a database `urlshortener`, then `mvn spring-boot:run`
(override `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` if needed).

**Tests:**
```bash
mvn test
```

## Design notes

- **Why Base62 of the ID?** Every ID is unique, so codes never collide with each other and no retry loop
  is needed. An offset of 100,000 is added so the first codes aren't `1`, `2`, `3`.
- **Why 302 and not 301?** Browsers cache 301 redirects, so repeat visits would skip the server and
  clicks would go uncounted.
- Each click is stored as its own row (`click_events`), so stats can be grouped by day or referrer.
