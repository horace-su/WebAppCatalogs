package com.qomg.webappcatalogs.webapp

import java.net.URI

object WebAppUrls {
    /** Turns user input such as "github.com" into "https://github.com", or returns null if it is not a web URL. */
    fun normalize(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
        val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
        val uri = runCatching { URI(withScheme) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in setOf("http", "https")) return null
        val host = uri.host ?: return null
        if ('.' !in host && host != "localhost") return null
        return uri.toString()
    }

    fun displayHost(url: String): String =
        runCatching { URI(url).host }.getOrNull()?.removePrefix("www.") ?: url
}
