package com.urlshortener.controller

import jakarta.validation.constraints.NotBlank

data class CreateRequest(@field:NotBlank(message = "URL must not be blank") val url: String)
data class CreateResponse(val shortUrl: String, val originalUrl: String)
data class ErrorResponse(val error: String)
