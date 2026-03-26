package com.urlshortener.service

import java.net.URI
import java.security.SecureRandom

@org.springframework.stereotype.Service
class Service {

    private val random = SecureRandom()

    fun create(url: String): String {
        validate(url)
        return generateCode()
    }

    fun resolve(code: String): String {
        return "hello from resolve"
    }

    private fun validate(url: String) {
        require(url.isNotBlank()) { "URL must not be blank" }

        val uri = try {
            URI(url.trim())
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid URL format: $url")
        }

        require(uri.scheme in listOf("http", "https")) { "URL must use http or https" }
        require(!uri.host.isNullOrBlank()) { "URL must have a valid host" }
    }

    private fun generateCode(): String {
        val chars = ('0'..'z').filter { it.isLetterOrDigit() }
        return (1..7).map { chars[random.nextInt(chars.size)] }.joinToString("")
    }
}
