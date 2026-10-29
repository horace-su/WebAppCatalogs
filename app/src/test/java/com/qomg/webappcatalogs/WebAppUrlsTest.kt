package com.qomg.webappcatalogs

import com.qomg.webappcatalogs.webapp.WebAppUrls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebAppUrlsTest {
    @Test
    fun normalize_addsHttpsWhenSchemeMissing() {
        assertEquals("https://github.com", WebAppUrls.normalize("  github.com "))
        assertEquals("https://mail.google.com/mail/u/0", WebAppUrls.normalize("mail.google.com/mail/u/0"))
    }

    @Test
    fun normalize_keepsExplicitWebSchemes() {
        assertEquals("http://192.168.1.10:8080", WebAppUrls.normalize("http://192.168.1.10:8080"))
        assertEquals("http://localhost:3000", WebAppUrls.normalize("http://localhost:3000"))
    }

    @Test
    fun normalize_rejectsNonWebInput() {
        assertNull(WebAppUrls.normalize(""))
        assertNull(WebAppUrls.normalize("hello world"))
        assertNull(WebAppUrls.normalize("notadomain"))
        assertNull(WebAppUrls.normalize("ftp://example.com"))
        assertNull(WebAppUrls.normalize("javascript://alert(1)"))
    }

    @Test
    fun displayHost_stripsWww() {
        assertEquals("notion.so", WebAppUrls.displayHost("https://www.notion.so"))
    }
}
