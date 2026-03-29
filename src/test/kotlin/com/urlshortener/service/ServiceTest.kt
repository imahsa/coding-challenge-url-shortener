package com.urlshortener.service

import com.urlshortener.store.InMemoryUrlStore
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServiceTest {

    private val service = Service(InMemoryUrlStore())

    // ── Code generation ──────────────────────────────────────────

    @Nested
    inner class CodeGeneration {

        @Test
        fun `create returns a 7-character alphanumeric code`() {
            val result = service.create("https://example.com")
            assertEquals(Service.CODE_LENGTH, result.code.length)
            assertTrue(result.code.all { it.isLetterOrDigit() })
        }

        @Test
        fun `create accepts http URLs`() {
            val result = service.create("http://example.com")
            assertEquals(Service.CODE_LENGTH, result.code.length)
        }

        @Test
        fun `create accepts URLs with paths and query params`() {
            val result = service.create("https://www.originenergy.com.au/electricity-gas/plans.html?foo=bar")
            assertEquals(Service.CODE_LENGTH, result.code.length)
        }
    }

    // ── In-memory store ──────────────────────────────────────────

    @Nested
    inner class Store {

        @Test
        fun `create returns the same code for the same URL (idempotent)`() {
            val first = service.create("https://example.com")
            val second = service.create("https://example.com")
            assertEquals(first.code, second.code)
        }

        @Test
        fun `create returns different codes for different URLs`() {
            val a = service.create("https://example.com/a")
            val b = service.create("https://example.com/b")
            assertTrue(a.code != b.code)
        }

        @Test
        fun `resolve returns the original URL for a stored code`() {
            val created = service.create("https://example.com")
            val resolved = service.resolve(created.code)
            assertEquals("https://example.com", resolved.originalUrl)
        }

        @Test
        fun `resolve throws NoSuchElementException for unknown code`() {
            val error = assertThrows<NoSuchElementException> {
                service.resolve("unknown")
            }
            assertEquals("Short code not found: unknown", error.message)
        }

        @Test
        fun `create trims whitespace before storing`() {
            val created = service.create("  https://example.com  ")
            assertEquals("https://example.com", created.originalUrl)
        }

        @Test
        fun `trimmed duplicate returns same code as original`() {
            val first = service.create("https://example.com")
            val second = service.create("  https://example.com  ")
            assertEquals(first.code, second.code)
        }
    }

    // ── URL validation ───────────────────────────────────────────

    @Nested
    inner class Validation {

        @Test
        fun `create rejects blank URL`() {
            val error = assertThrows<IllegalArgumentException> {
                service.create("")
            }
            assertEquals("URL must not be blank", error.message)
        }

        @Test
        fun `create rejects whitespace-only URL`() {
            assertThrows<IllegalArgumentException> {
                service.create("   ")
            }
        }

        @Test
        fun `create rejects malformed URL`() {
            assertThrows<IllegalArgumentException> {
                service.create("not-a-url")
            }
        }

        @Test
        fun `create rejects ftp scheme`() {
            val error = assertThrows<IllegalArgumentException> {
                service.create("ftp://files.example.com")
            }
            assertEquals("URL must use http or https", error.message)
        }

        @Test
        fun `create rejects mailto scheme`() {
            assertThrows<IllegalArgumentException> {
                service.create("mailto:user@example.com")
            }
        }

        @Test
        fun `create rejects URL without host`() {
            assertThrows<IllegalArgumentException> {
                service.create("http://")
            }
        }
    }
}
