package com.urlshortener.store

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class InMemoryUrlStore : UrlStore {

    private val codeToUrl = ConcurrentHashMap<String, String>()
    private val urlToCode = ConcurrentHashMap<String, String>()

    override fun findByCode(code: String): String? = codeToUrl[code]

    override fun findByUrl(url: String): String? = urlToCode[url]

    override fun save(code: String, url: String) {
        codeToUrl[code] = url
        urlToCode[url] = code
    }
}
