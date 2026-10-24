package com.v2ray.ang.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
    fun wireguardLinksAreKept() {
        val wg = "wireguard://c2VjcmV0@162.159.192.1:2408?publickey=abc&address=172.16.0.2%2F32#wg"
        assertEquals(listOf(wg, trojan), ConfigLinkParser.extractLinks("$wg\n$trojan"))
    }

    @Test
    fun pingHintReadsTheLatencyOutOfTheName() {
        assertEquals(49, ConfigLinkParser.pingHint("vless://id@1.2.3.4:443#Xfinder%20%E2%80%A2%20VLESS%20%E2%80%A2%2049ms%20%E2%80%A2%20001"))
        assertEquals(120, ConfigLinkParser.pingHint("ss://x@1.2.3.4:8388#Name%20120%20ms"))
        assertNull(ConfigLinkParser.pingHint("vless://id@1.2.3.4:443#Plain"))
        assertNull(ConfigLinkParser.pingHint("vless://id@1.2.3.4:443"))
    }

    @Test
    fun fastestFirstPutsLowLatencyFirstAndKeepsTheRestInOrder() {
        val slow = "vless://a@1.1.1.1:443#x%20300ms"
        val fast = "vless://b@2.2.2.2:443#x%2040ms"
        val noPingOne = "vless://c@3.3.3.3:443#one"
        val noPingTwo = "vless://d@4.4.4.4:443#two"
        assertEquals(listOf(fast, slow, noPingOne, noPingTwo), ConfigLinkParser.fastestFirst(listOf(noPingOne, slow, noPingTwo, fast)))
    }

    @Test
    fun theAppsOwnXfinderListsComeFirstAllIsTheMainOneAndTheyAreTheOnlyOnesReadAtFirst() {
        val first = ConfigSources.all.filter { !it.fallback }.map { it.url }
        assertEquals(
            listOf(
                "https://imatixofficel.github.io/Xfinder/output/all.txt",
                "https://imatixofficel.github.io/Xfinder/output/top20.txt",
                "https://imatixofficel.github.io/Xfinder/output/vless.txt",
                "https://imatixofficel.github.io/Xfinder/output/vmess.txt",
                "https://imatixofficel.github.io/Xfinder/output/trojan.txt",
                "https://imatixofficel.github.io/Xfinder/output/wireguard.txt",
            ),
            first,
        )
        // The Xfinder ss and hysteria2 lists are empty and must not be read.
        assertTrue(ConfigSources.all.none { it.url.endsWith("/ss.txt") || it.url.endsWith("/hysteria2.txt") })
        // The sources that are read first come before every fallback one.
        assertEquals(ConfigSources.all.indices.filter { !ConfigSources.all[it].fallback }, (0 until first.size).toList())
    }

    @Test
    fun theMahsaListsAreTheFallback() {
        val base = "https://raw.githubusercontent.com/mahsanet/MahsaFreeConfig/refs/heads/main"
        val expected = listOf("mci", "mtn").flatMap { op -> (1..4).map { "$base/$op/sub_$it.txt" } }
        assertEquals(expected, ConfigSources.all.filter { it.fallback }.map { it.url })
    }

    @Test
    fun everyProviderChoiceGetsBothRoundsOfSources() {
        IspChoice.all.forEach { isp ->
            val sources = ConfigSources.forIsp(isp)
            assertTrue(sources.any { !it.fallback }, isp)
            assertTrue(sources.any { it.fallback }, isp)
        }
        assertEquals(ConfigSources.all, ConfigSources.forIsp("unknown"))
    }

    @Test
    fun theMultiProxySourceIsInTheFirstRoundAndNoRemovedSourceIsLeft() {
        val urls = ConfigSources.all.map { it.url }
        assertTrue(urls.any { it.contains("4n0nymou3/multi-proxy-config-fetcher") && it.endsWith("proxy_configs.txt") })
        assertTrue(ConfigSources.all.none { it.url.contains("balochscript") })
        assertTrue(ConfigSources.all.first { it.url.contains("4n0nymou3") }.fallback.not())
    }

    @Test
    fun switchedOffSourcesAreLeftOutAndUserSourcesAreAdded() {
        val off = ConfigSources.all.first()
        val mine = ConfigSource("mine", "https://example.com/list.txt", custom = true)
        val sources = ConfigSources.forIsp(IspChoice.AUTO, disabled = setOf(off.id), custom = listOf(mine))
        assertTrue(sources.none { it.id == off.id })
        assertTrue(mine in sources)
        assertTrue(ConfigSources.forIsp(IspChoice.AUTO, disabled = ConfigSources.all.map { it.id }.toSet()).isEmpty())
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
