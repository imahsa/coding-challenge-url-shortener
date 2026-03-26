package com.urlshortener.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ServiceTest {

    private val service = Service()

    @Test
    fun `create returns a 7-character alphanumeric code`() {
        val code = service.create("https://example.com")
        assertEquals(7, code.length)
        assertTrue(code.all { it.isLetterOrDigit() })
    }

    @Test
    fun `create returns different codes each time`() {
        val first = service.create("https://example.com")
        val second = service.create("https://example.com")
        assertNotEquals(first, second)
    }

    @Test
    fun `create accepts http URLs`() {
        val code = service.create("http://example.com")
        assertEquals(7, code.length)
    }

    @Test
    fun `create accepts URLs with paths and query params`() {
        val code = service.create("https://www.originenergy.com.au/electricity-gas/plans.html?foo=bar")
        assertEquals(7, code.length)
    }

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
