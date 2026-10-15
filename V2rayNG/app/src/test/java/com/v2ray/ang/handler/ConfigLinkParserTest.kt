package com.v2ray.ang.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Base64

class ConfigLinkParserTest {
    private val vless = "vless://e5cc16a6-ea42-46b2-82ae-ad2157e1641b@198.41.197.63:8080?security=none&type=ws#one"
    private val trojan = "trojan://secret@example.com:443?security=tls#two"
    private val hy2 = "hy2://pass@example.org:8443?sni=example.org#three"

    @Test
    fun emptyBlankAndNullGiveNoLinks() {
        assertTrue(ConfigLinkParser.extractLinks(null).isEmpty())
        assertTrue(ConfigLinkParser.extractLinks("").isEmpty())
        assertTrue(ConfigLinkParser.extractLinks("  \n \n").isEmpty())
    }

    @Test
    fun plainLinesKeepOnlySupportedSchemesAndDropDuplicates() {
        val body = "# comment\r\n$vless\r\nhttps://example.com/not-a-config\n$trojan\n$vless\n$hy2\n"
        assertEquals(listOf(vless, trojan, hy2), ConfigLinkParser.extractLinks(body))
    }

    @Test
    fun base64BodyIsDecoded() {
        val body = Base64.getEncoder().encodeToString("$vless\n$trojan".toByteArray())
        assertEquals(listOf(vless, trojan), ConfigLinkParser.extractLinks(body))
    }

    @Test
    fun textThatIsNeitherLinksNorBase64GivesNoLinks() {
        assertTrue(ConfigLinkParser.extractLinks("<html><body>404 not found</body></html>").isEmpty())
    }

    @Test
    fun overlongLineIsSkipped() {
        val long = "vless://" + "a".repeat(5000)
        assertEquals(listOf(trojan), ConfigLinkParser.extractLinks("$long\n$trojan"))
    }

    @Test
    fun everySourceHasAnHttpsUrlAndAName() {
        assertTrue(ConfigSources.all.isNotEmpty())
        ConfigSources.all.forEach {
            assertTrue(it.url.startsWith("https://"), it.url)
            assertTrue(it.name.isNotBlank())
        }
        assertEquals(ConfigSources.all.size, ConfigSources.all.map { it.url }.toSet().size)
    }
}
