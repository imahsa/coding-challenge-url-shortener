# URL Shortener API

> A Spring Boot REST API that shortens URLs, stores them in memory, and redirects short codes back to the original URL.

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | [Spring Boot](https://spring.io/projects/spring-boot) 3.5.0 |
| Language | Kotlin (JDK 21) |
| Build | Gradle (Kotlin DSL) |
| Storage | In-memory (`ConcurrentHashMap`) |
| Testing | JUnit 5, MockK, SpringMockK, MockMvc |

---

## Quick Start

**Prerequisites:** JDK 21

```bash
./gradlew bootRun
```

Server starts at **http://localhost:8080**

---

## API Endpoints

### `POST /shorten` — Shorten a URL

```bash
curl -s -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.originenergy.com.au/electricity-gas/plans.html"}'
```

**Response (201 Created):**

```json
{
  "shortUrl": "http://localhost:8080/abc1234",
  "originalUrl": "https://www.originenergy.com.au/electricity-gas/plans.html"
}
```

| Status | Meaning |
|---|---|
| `201` | URL shortened successfully |
| `400` | Invalid URL (blank, malformed, non-http/https, no host) |

### `GET /{code}` — Redirect to original URL

```bash
curl -v http://localhost:8080/abc1234
```

**Response (302 Found):** Redirects via `Location` header to the original URL.

| Status | Meaning |
|---|---|
| `302` | Redirect to original URL |
| `404` | Short code not found |

---

## Validation Rules

| Rule | Behavior |
|---|---|
| Non-blank | Empty or whitespace-only URLs rejected (400) |
| Valid format | Must be parseable as a URI |
| HTTP/HTTPS only | `ftp://`, `mailto:`, etc. rejected (400) |
| Valid host | URL must contain a non-blank hostname |
| Whitespace trimmed | Leading/trailing whitespace stripped before storage |

---

## Design Decisions

See [DECISIONS.md](./DECISIONS.md) for the full log. Highlights:

- **Kotlin over Java** — `data class` for immutability, null safety in the type system, concise syntax
- **ConcurrentHashMap** — thread-safe, O(1) bidirectional lookup via two maps (`code→url` and `url→code`)
- **Idempotent create** — same URL always returns the same short code (no duplicates)
- **SecureRandom** — cryptographically strong random code generation, 7-char alphanumeric (62^7 ≈ 3.5 trillion combinations)
- **Collision retry** — up to 10 attempts if a generated code already exists
- **302 redirect** — standard temporary redirect; 301 would be cached by browsers and harder to change later
- **Controller-level exception handlers** — `IllegalArgumentException` → 400, `NoSuchElementException` → 404
- **MockMvc + MockK** — controller tests mock the service layer, service tests run standalone

---

## Tests

```bash
./gradlew test
```

| Suite | Count | Covers |
|---|---|---|
| `ApplicationTests` | 1 | Spring context loads |
| `ServiceTest` — CodeGeneration | 3 | Code format, http/https, paths + query params |
| `ServiceTest` — Store | 6 | Idempotent create, different URLs → different codes, resolve, 404, trim, trim dedup |
| `ServiceTest` — Validation | 6 | Blank, whitespace, malformed, ftp, mailto, no host |
| `ControllerTest` — Shorten | 4 | 201 + JSON body, 400 invalid, 400 blank, 400 non-http |
| `ControllerTest` — Resolve | 2 | 302 redirect + Location header, 404 unknown |
| **Total** | **22** | |

---

## Project Structure

```
url-shortener/
├── build.gradle.kts
├── src/
│   ├── main/kotlin/com/urlshortener/
│   │   ├── Application.kt               # Spring Boot entry point
│   │   ├── controller/
│   │   │   └── Controller.kt            # REST endpoints + exception handlers
│   │   └── service/
│   │       └── Service.kt               # URL validation, code generation, in-memory store
│   ├── main/resources/
│   │   └── application.properties
│   └── test/kotlin/com/urlshortener/
│       ├── ApplicationTests.kt           # Context load smoke test
│       ├── controller/
│       │   └── ControllerTest.kt         # MockMvc integration tests
│       └── service/
│           └── ServiceTest.kt            # Unit tests (validation, store, code gen)
```

---

## Limitations & Production Readiness

| # | Area | Current State | Production Change |
|---|---|---|---|
| 1 | **Persistence** | In-memory `ConcurrentHashMap` — data lost on restart | Database (Postgres, Redis, DynamoDB) with proper persistence |
| 2 | **Collision safety** | Retry loop (up to 10 attempts) | Base62-encoded auto-increment ID or pre-generated ID pool for guaranteed uniqueness |
| 3 | **Rate limiting** | None | Spring `@RateLimiter` or API gateway throttling to prevent abuse |
| 4 | **Analytics** | None | Track click counts, referrer, timestamp per redirect |
| 5 | **Custom codes** | Not supported | Allow users to choose vanity short codes |
| 6 | **Expiration** | URLs never expire | TTL support with background cleanup |
| 7 | **API authentication** | None | API key or OAuth2 for create endpoint |
| 8 | **Horizontal scaling** | Single JVM, in-memory state | Shared store (Redis/DB) so multiple instances can serve the same codes |
| 9 | **Monitoring** | `stdout` only | Structured logging (Logback), Micrometer metrics, health endpoint via Actuator |
| 10 | **CI/CD** | No pipeline | GitHub Actions: test on PR, build on merge |
