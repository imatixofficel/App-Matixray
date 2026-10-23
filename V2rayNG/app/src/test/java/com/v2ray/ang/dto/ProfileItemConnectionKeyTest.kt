package com.v2ray.ang.dto

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class ProfileItemConnectionKeyTest {
    private fun vless(server: String = "1.2.3.4", port: String = "443", id: String = "u1", name: String = "a") =
        ProfileItem(configType = EConfigType.VLESS, remarks = name, server = server, serverPort = port, password = id)

    @Test
    fun nameAndTransportOptionsDoNotMakeANewConnection() {
        val a = vless(name = "one")
        val b = vless(name = "two").also { it.network = "ws"; it.path = "/x"; it.sni = "example.com" }
        assertEquals(a.connectionKey(), b.connectionKey())
    }

    @Test
    fun addressPortUserAndProtocolEachMakeANewConnection() {
        val base = vless().connectionKey()
        assertNotEquals(base, vless(server = "1.2.3.5").connectionKey())
        assertNotEquals(base, vless(port = "8443").connectionKey())
        assertNotEquals(base, vless(id = "u2").connectionKey())
        assertNotEquals(base, ProfileItem(configType = EConfigType.TROJAN, server = "1.2.3.4", serverPort = "443", password = "u1").connectionKey())
    }

    @Test
    fun hostCaseAndSurroundingSpacesAreIgnored() {
        assertEquals(vless(server = "Example.COM").connectionKey(), vless(server = " example.com ").connectionKey())
    }

    @Test
    fun aProfileWithoutPasswordUsesItsSecretKey() {
        fun wg(key: String) = ProfileItem(configType = EConfigType.WIREGUARD, server = "1.2.3.4", serverPort = "2408", secretKey = key)
        assertEquals(wg("k1").connectionKey(), wg("k1").connectionKey())
        assertNotEquals(wg("k1").connectionKey(), wg("k2").connectionKey())
    }
}
