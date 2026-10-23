package com.v2ray.ang.ui.xfinder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class XfinderSourcesTest {
    private val one = listOf(XfinderParser.parseLine("vless://a@1.1.1.1:443#x 10ms")!!)

    @Test
    fun theTabsAreTheSixXfinderListsInOrderWithAllFirst() {
        assertEquals(
            listOf(
                "https://imatixofficel.github.io/Xfinder/output/all.txt",
                "https://imatixofficel.github.io/Xfinder/output/top20.txt",
                "https://imatixofficel.github.io/Xfinder/output/vless.txt",
                "https://imatixofficel.github.io/Xfinder/output/vmess.txt",
                "https://imatixofficel.github.io/Xfinder/output/trojan.txt",
                "https://imatixofficel.github.io/Xfinder/output/wireguard.txt",
            ),
            XfinderSource.entries.map { it.url },
        )
        assertEquals(XfinderSource.entries, XfinderUiState().sources)
        assertEquals(XfinderSource.ALL, XfinderUiState().source)
    }

    @Test
    fun anEmptyOrMissingListIsNotOffered() {
        val results = mapOf(
            XfinderSource.ALL to one,
            XfinderSource.VLESS to one,
            XfinderSource.VMESS to emptyList(),
            XfinderSource.TROJAN to one,
        )
        // WIREGUARD is absent (its download failed) and VMESS is empty: neither is offered.
        assertEquals(listOf(XfinderSource.ALL, XfinderSource.VLESS, XfinderSource.TROJAN), availableXfinderSources(results))
        assertEquals(emptyList<XfinderSource>(), availableXfinderSources(emptyMap()))
    }

    @Test
    fun theShownListIsTheCurrentOneWhenOfferedElseTheFirstOfferedElseNone() {
        val available = listOf(XfinderSource.VLESS, XfinderSource.TROJAN)
        assertEquals(XfinderSource.TROJAN, pickXfinderSource(XfinderSource.TROJAN, available))
        assertEquals(XfinderSource.VLESS, pickXfinderSource(XfinderSource.ALL, available))
        assertNull(pickXfinderSource(XfinderSource.ALL, emptyList()))
    }
}
