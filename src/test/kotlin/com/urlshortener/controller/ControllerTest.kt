package com.urlshortener.controller

import com.ninjasquad.springmockk.MockkBean
import com.urlshortener.model.ShortenedUrl
import com.urlshortener.service.Service
import io.mockk.every
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(Controller::class)
class ControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @MockkBean
    private lateinit var service: Service

    // ── POST /shorten ────────────────────────────────────────────

    @Nested
    inner class Shorten {

        @Test
        fun `returns 201 with shortUrl and originalUrl`() {
            every { service.create("https://example.com") } returns ShortenedUrl("abc1234", "https://example.com")

            mvc.post("/shorten") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"url": "https://example.com"}"""
            }.andExpect {
                status { isCreated() }
                jsonPath("$.shortUrl") { value("http://localhost/abc1234") }
                jsonPath("$.originalUrl") { value("https://example.com") }
            }
        }

        @Test
        fun `returns 400 for invalid URL`() {
            every { service.create("not-a-url") } throws IllegalArgumentException("Invalid URL format: not-a-url")

            mvc.post("/shorten") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"url": "not-a-url"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { value("Invalid URL format: not-a-url") }
            }
        }

        @Test
        fun `returns 400 for blank URL`() {
            every { service.create("") } throws IllegalArgumentException("URL must not be blank")

            mvc.post("/shorten") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"url": ""}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { value("URL must not be blank") }
            }
        }

        @Test
        fun `returns 400 for non-http scheme`() {
            every { service.create("ftp://files.example.com") } throws IllegalArgumentException("URL must use http or https")

            mvc.post("/shorten") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"url": "ftp://files.example.com"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { value("URL must use http or https") }
            }
        }
    }

    // ── GET /{code} ──────────────────────────────────────────────

    @Nested
    inner class Resolve {

        @Test
        fun `returns 302 redirect with Location header`() {
            every { service.resolve("abc1234") } returns ShortenedUrl("abc1234", "https://example.com")

            mvc.get("/abc1234").andExpect {
                status { isFound() }
                header { string("Location", "https://example.com") }
            }
        }

        @Test
        fun `returns 404 for unknown code`() {
            every { service.resolve("unknown") } throws NoSuchElementException("Short code not found: unknown")

            mvc.get("/unknown").andExpect {
                status { isNotFound() }
                jsonPath("$.error") { value("Short code not found: unknown") }
            }
        }
    }
}
