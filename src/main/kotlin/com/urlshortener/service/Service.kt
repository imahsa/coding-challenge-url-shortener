package com.urlshortener.service

@org.springframework.stereotype.Service
class Service {

    fun shorten(url: String): String {
        return "hello from shorten"
    }

    fun resolve(code: String): String {
        return "hello from resolve"
    }
}
