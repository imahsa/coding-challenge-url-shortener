package com.urlshortener.service

import com.urlshortener.model.ShortenedUrl
import java.net.URI
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

@org.springframework.stereotype.Service
class Service {

    private val random = SecureRandom()
    private val codeToUrl = ConcurrentHashMap<String, String>()
    private val urlToCode = ConcurrentHashMap<String, String>()

    companion object {
        const val CODE_LENGTH = 7
        private const val MAX_COLLISION_RETRIES = 10
    }

    fun create(url: String): ShortenedUrl {
        val normalised = validate(url)

        val code = urlToCode.computeIfAbsent(normalised) {
            val newCode = generateUniqueCode()
            codeToUrl[newCode] = normalised
            newCode
        }

        return ShortenedUrl(code = code, originalUrl = normalised)
    }

    fun resolve(code: String): ShortenedUrl {
        val url = codeToUrl[code]
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
            if (!codeToUrl.containsKey(code)) return code
        }
        throw IllegalStateException("Failed to generate a unique code after $MAX_COLLISION_RETRIES attempts")
    }

    private fun generateCode(): String {
        val chars = ('0'..'z').filter { it.isLetterOrDigit() }
        return (1..CODE_LENGTH).map { chars[random.nextInt(chars.size)] }.joinToString("")
    }
}
