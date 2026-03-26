package com.urlshortener.controller

import com.urlshortener.service.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class Controller(private val service: Service) {

    @PostMapping("/shorten")
    fun shorten(@RequestBody url: String): String {
        return service.shorten(url)
    }

    @GetMapping("/{code}")
    fun resolve(@PathVariable code: String): String {
        return service.resolve(code)
    }
}
