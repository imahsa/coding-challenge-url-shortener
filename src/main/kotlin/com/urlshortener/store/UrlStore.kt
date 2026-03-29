package com.urlshortener.store

interface UrlStore {
    fun findByCode(code: String): String?
    fun findByUrl(url: String): String?
    fun save(code: String, url: String)
}
