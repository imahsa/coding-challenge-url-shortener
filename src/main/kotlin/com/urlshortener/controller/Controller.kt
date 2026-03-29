package com.urlshortener.controller

import com.urlshortener.service.UrlShortenerService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
class Controller(private val service: UrlShortenerService) {

    @PostMapping("/shorten")
    fun create(
        @Valid @RequestBody request: CreateRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<CreateResponse> {
        val shortened = service.create(request.url)
        val baseUrl = buildBaseUrl(httpRequest)
        val response = CreateResponse(
            shortUrl = "$baseUrl/${shortened.code}",
            originalUrl = shortened.originalUrl,
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{code}")
    fun resolve(@PathVariable code: String): ResponseEntity<Void> {
        val shortened = service.resolve(code)
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI(shortened.originalUrl))
            .build()
    }

    private fun buildBaseUrl(req: HttpServletRequest): String {
        val scheme = req.scheme
        val port = req.serverPort
        val defaultPort = if (scheme == "https") 443 else 80
        val portSuffix = if (port == defaultPort || port <= 0) "" else ":$port"
        return "$scheme://${req.serverName}$portSuffix"
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = e.bindingResult.fieldErrors.joinToString("; ") { it.defaultMessage ?: "Invalid value" }
        return ResponseEntity.badRequest().body(ErrorResponse(message))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.badRequest().body(ErrorResponse(e.message ?: "Invalid request"))
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(e.message ?: "Not found"))
    }
}
