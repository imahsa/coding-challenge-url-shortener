# Step 1: Project Scaffold - Design Decisions

## How the Project Was Scaffolded

The project was generated using [Spring Initializr](https://start.spring.io) -- the official Spring Boot project generator. It produces a minimal, working Spring Boot application with the build file, application entry point, a smoke test, and the Gradle wrapper.

The Initializr was called with these parameters:
- **Project type:** Gradle with Kotlin DSL
- **Language:** Kotlin
- **Spring Boot version:** 3.5.0
- **Group:** `com.urlshortener`
- **Artifact:** `url-shortener`
- **Java version:** 21
- **Dependencies:** `web`, `validation`

After generation, two test libraries were added manually to `build.gradle.kts`: **MockK** and **SpringMockK**.

---

## Decision: Kotlin over Java

**Necessary?** Not strictly -- Java would work fine. But Kotlin gives us tangible advantages for this challenge:

- `data class` gives us immutable value objects with `equals`, `hashCode`, `toString`, and `copy` for free. No Lombok needed, no boilerplate.
- Null safety is built into the type system (`String` vs `String?`), which reduces defensive null-checking code.
- More concise syntax overall -- fewer lines means easier to read during code review.
- The challenge says "Java/Kotlin" -- both are accepted. Kotlin shows a broader skill set.

**Trade-off:** If the interviewing team is Java-only, they might be less familiar reading Kotlin. However, Kotlin is idiomatic in the Spring ecosystem and officially supported.

---

## Decision: JDK 21

**Necessary?** Yes -- Spring Boot 3.x requires JDK 17 minimum. We chose 21 because:

- It is the current **Long-Term Support (LTS)** release.
- It is the most widely used JDK version in production today.
- Spring Boot 3.5.0 explicitly supports it.
- It includes useful features like virtual threads (if we ever need them) and pattern matching.

**Why not JDK 17?** It would work, but 21 is newer LTS with no downsides for this project.

**Why not JDK 22/23/24?** These are short-term releases, not LTS. Using them signals instability in a production-oriented challenge.

---

## Decision: Gradle (Kotlin DSL) over Maven

**Necessary?** No -- Maven would be perfectly fine. Here is why Gradle was chosen:

- **Spring Initializr default for Kotlin projects.** When you select Kotlin, Gradle with Kotlin DSL is the recommended option.
- **Kotlin DSL for the build file** means the build configuration itself is type-safe Kotlin (`build.gradle.kts`). IDE auto-completion works in the build file. With Maven you get XML, which has no type safety.
- **Faster builds.** Gradle uses an incremental build cache and a daemon process. For a small project this barely matters, but it is a nice default.
- **Conciseness.** The `build.gradle.kts` file is 42 lines. An equivalent `pom.xml` would be ~80-100 lines of XML.

**Trade-off:** Maven is more widely understood. Gradle has a steeper learning curve. For a small challenge project, this difference is negligible.

---

## Decision: Spring Boot 3.5.0

**Necessary?** We need Spring Boot 3.x (for Kotlin + JDK 21 support). The exact minor version was dictated by Spring Initializr -- 3.5.0 is the minimum version it currently offers.

---

## What Each File Does

### `build.gradle.kts`
The build configuration. Defines:
- **Plugins:** Kotlin JVM compiler, Kotlin Spring plugin (opens classes for Spring proxying), Spring Boot plugin, Spring dependency management
- **Dependencies:** What libraries the project uses (explained below)
- **Java toolchain:** Tells Gradle to compile for JDK 21
- **Compiler flag** `-Xjsr305=strict`: Makes Spring's `@NonNull` annotations stricter in Kotlin

### `settings.gradle.kts`
Single line -- just names the project `url-shortener`.

### `Application.kt`
The entry point. `@SpringBootApplication` enables auto-configuration, component scanning, and Spring configuration. `runApplication` boots the embedded Tomcat server. Named simply `Application` -- the package `com.urlshortener` already provides the context, so prefixing the class name with "UrlShortener" would be redundant.

### `ApplicationTests.kt`
A smoke test. `@SpringBootTest` loads the full application context. The `contextLoads()` test verifies that Spring can wire everything together without errors. If any configuration is broken, this test fails.

### `application.properties`
Spring configuration file. Currently just sets the application name. We will add more config here later if needed.

---

## What Each Dependency Does

| Dependency | Purpose | Necessary? |
|---|---|---|
| `spring-boot-starter-web` | Embedded Tomcat, Spring MVC, REST support | **Yes** -- we are building a REST API |
| `spring-boot-starter-validation` | Bean validation (`@Valid`, `@NotBlank`, etc.) | **Yes** -- we need strict URL input validation |
| `jackson-module-kotlin` | JSON serialization that understands Kotlin data classes | **Yes** -- without this, Jackson cannot deserialize into Kotlin classes properly |
| `kotlin-reflect` | Runtime reflection for Kotlin | **Yes** -- required by Spring for Kotlin class introspection |
| `spring-boot-starter-test` | JUnit 5, AssertJ, MockMvc, Spring Test utilities | **Yes** -- our testing foundation |
| `kotlin-test-junit5` | Kotlin-friendly JUnit 5 assertions | **Yes** -- lets us write idiomatic Kotlin tests |
| `mockk` | Mocking library designed for Kotlin | **Yes** -- Mockito does not handle Kotlin's `final` classes and coroutines well. MockK is the standard for Kotlin testing |
| `springmockk` | Integrates MockK with Spring's `@MockkBean` | **Yes** -- lets us mock Spring beans in integration tests using MockK instead of Mockito |
| `junit-platform-launcher` | JUnit test runner (runtime only) | **Yes** -- required by Gradle to discover and run JUnit 5 tests |

---

## What Is NOT Included (and why)

- **Lombok:** Not needed. Kotlin's `data class` replaces `@Data`, `@Value`, `@Builder`, etc.
- **Spring Data JPA / H2 Database:** Not needed. The challenge says "in-memory store" -- we will use a simple `ConcurrentHashMap`, not a database.
- **Spring Security:** Not in scope for this challenge.
- **Docker / Dockerfile:** Not required by the deliverables. Could be added as a bonus later.


---

## Decision: Bidirectional ConcurrentHashMap for In-Memory Store

**Why two maps?** The challenge requires both shortening (URL → code) and resolving (code → URL). A single `Map<String, String>` would give O(1) in one direction but O(n) in the other. Two maps — `codeToUrl` and `urlToCode` — give O(1) in both directions.

**Why ConcurrentHashMap?** Spring Boot's embedded Tomcat handles requests on multiple threads. A plain `HashMap` would risk data corruption under concurrent access. `ConcurrentHashMap` provides thread-safe reads and writes without explicit synchronization, with minimal performance overhead.

**Why not a database?** The challenge explicitly says "in-memory store." A `ConcurrentHashMap` is the simplest correct solution. The architecture is layered so swapping in a `Repository` interface backed by a database would require zero changes to the controller or service interface.

---

## Decision: Idempotent URL Shortening

Calling `POST /shorten` with the same URL returns the same short code every time. This avoids polluting the store with duplicate entries and gives predictable behavior.

**Trade-off:** Some URL shorteners generate a new code each time, allowing different analytics per link. For this challenge, idempotency is simpler and avoids unnecessary storage growth.

---

## Decision: SecureRandom + Collision Retry

Short codes are 7-character alphanumeric strings generated via `SecureRandom`. The character space is 62 (a-z, A-Z, 0-9), giving 62^7 ≈ 3.5 trillion combinations — collision probability is negligible for an in-memory challenge.

If a collision does occur, the service retries up to 10 times before throwing `IllegalStateException`. In production, a better approach would be base62-encoding an auto-incrementing ID (guaranteed unique, no retries needed).

---

## Decision: 302 Found (Not 301)

`GET /{code}` returns HTTP 302 (temporary redirect) rather than 301 (permanent redirect).

**Why?** A 301 is cached aggressively by browsers and CDNs — once a client receives a 301, it may never hit the server again for that code. This makes it impossible to update the target URL, track analytics, or expire links. 302 ensures every redirect goes through the server.

---

## Decision: Controller-Level Exception Handlers

Validation errors (`IllegalArgumentException`) map to 400, unknown codes (`NoSuchElementException`) map to 404. These are handled via `@ExceptionHandler` methods on the controller rather than a global `@ControllerAdvice`.

**Why?** For a small API with one controller, local handlers are simpler and more readable. A global advice would be warranted if there were multiple controllers sharing the same error handling logic.

---

## Decision: MockMvc + MockK for Controller Tests

Controller tests use `@WebMvcTest` (loads only the web layer) with `@MockkBean` to mock the service. This verifies:
- HTTP status codes (201, 302, 400, 404)
- Response body structure (JSON fields, Location header)
- Exception handler wiring

Service tests run standalone (no Spring context) for fast feedback. This separation follows the test pyramid: many fast unit tests, fewer integration tests.

---

## Decision: Base URL Construction

The `shortUrl` in the response is built from the incoming request's scheme, host, and port. Default ports (80 for HTTP, 443 for HTTPS) are omitted to produce clean URLs like `http://localhost/abc1234` rather than `http://localhost:80/abc1234`.

This makes the API environment-agnostic — it works correctly behind a reverse proxy or load balancer without hardcoding a domain.