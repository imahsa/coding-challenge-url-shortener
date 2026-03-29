package com.urlshortener.service

import com.urlshortener.model.ShortenedUrl
import com.urlshortener.store.UrlStore
import org.springframework.stereotype.Service
import java.net.URI
import java.security.SecureRandom

@Service
class UrlShortenerService(private val store: UrlStore) {

    private val random = SecureRandom()

    companion object {
        const val CODE_LENGTH = 7
        private const val MAX_COLLISION_RETRIES = 10
        private val ALPHANUMERIC_CHARS = ('0'..'z').filter { it.isLetterOrDigit() }
    }

    fun create(url: String): ShortenedUrl {
        val normalised = validate(url)

        store.findByUrl(normalised)?.let {
            return ShortenedUrl(code = it, originalUrl = normalised)
        }

        val code = generateUniqueCode()
        store.save(code, normalised)

        return ShortenedUrl(code = code, originalUrl = normalised)
    }

    fun resolve(code: String): ShortenedUrl {
        val url = store.findByCode(code)
            ?: throw NoSuchElementException("Short code not found: $code")
        return ShortenedUrl(code = code, originalUrl = url)
    }

    private fun validate(url: String): String {
        require(url.isNotBlank()) { "URL must not be blank" }

        val trimmed = url.trim()
        val uri = try {
            URI(trimmed)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid URL format: $trimmed")
        }

        require(uri.scheme in listOf("http", "https")) { "URL must use http or https" }
        require(!uri.host.isNullOrBlank()) { "URL must have a valid host" }

        return trimmed
    }

    private fun generateUniqueCode(): String {
        repeat(MAX_COLLISION_RETRIES) {
            val code = generateCode()
            if (store.findByCode(code) == null) return code
        }
        throw IllegalStateException("Failed to generate a unique code after $MAX_COLLISION_RETRIES attempts")
    }

    private fun generateCode(): String {
        return (1..CODE_LENGTH).map { ALPHANUMERIC_CHARS[random.nextInt(ALPHANUMERIC_CHARS.size)] }.joinToString("")
    }
}
