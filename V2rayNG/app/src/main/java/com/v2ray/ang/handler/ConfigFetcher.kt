package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.Base64

/**
 * One public list of share links the "get configs" button reads. [isps] names the internet providers the list is meant
 * for (see [IspChoice]); empty means it is meant for every provider.
 */
data class ConfigSource(val name: String, val url: String, val isps: Set<String> = emptySet())

/**
 * Matixray: the lists the "get configs" button reads: only Mahsa NG's own public lists. A list that is down, moved or
 * empty is skipped, so a dead entry costs only its timeout; add an entry here (with [ConfigSource.isps] for a list made
 * for one provider) and nothing else needs to change.
 */
object ConfigSources {
    val all: List<ConfigSource> = listOf(
        ConfigSource("Mahsa NG", "https://raw.githubusercontent.com/MahsaNetConfigTopic/config/main/xray_final.txt"),
        ConfigSource("Mahsa NG | vless", "https://raw.githubusercontent.com/MahsaNetConfigTopic/config/main/xray_final_vless.txt"),
        ConfigSource("Mahsa NG | mix", "https://raw.githubusercontent.com/MahsaNetConfigTopic/config/main/mix.txt"),
    )

    /** The lists that fit the provider the user chose ([IspChoice.AUTO] or an unknown id means all of them). */
    fun forIsp(isp: String): List<ConfigSource> {
        val fitting = all.filter { it.isps.isEmpty() || isp in it.isps }
        return fitting.ifEmpty { all }
    }
}

/** The internet providers the user can say they use; stored by id in the settings. */
object IspChoice {
    const val AUTO = "auto"
    const val MCI = "mci"
    const val IRANCELL = "irancell"
    const val SHATEL = "shatel"
    const val TCI = "tci"
    const val RIGHTEL = "rightel"
    val all = listOf(AUTO, MCI, IRANCELL, SHATEL, TCI, RIGHTEL)
}

/** What one source gave: the share links found in it (shuffled), empty when it was down or held none. */
data class ConfigSourceResult(val source: ConfigSource, val links: List<String>)

/** Pure parsing of a downloaded list: plain lines or a base64 body, only the link kinds the app can import. */
object ConfigLinkParser {
    private val schemes = listOf("vless://", "vmess://", "trojan://", "ss://", "hysteria2://", "hy2://")
    private const val MAX_LINK_LENGTH = 4000

    fun extractLinks(body: String?): List<String> {
        if (body.isNullOrBlank()) return emptyList()
        val direct = linksIn(body)
        if (direct.isNotEmpty()) return direct
        val decoded = decodeBase64(body) ?: return emptyList()
        return linksIn(decoded)
    }

    private fun linksIn(text: String): List<String> =
        text.lineSequence()
            .map { it.trim() }
            .filter { line ->
                line.length in 12..MAX_LINK_LENGTH && schemes.any { line.startsWith(it, ignoreCase = true) }
            }
            .distinct()
            .toList()

    private fun decodeBase64(input: String): String? {
        val clean = input.trim().replace('-', '+').replace('_', '/')
        if (clean.isEmpty()) return null
        return try {
            String(Base64.getMimeDecoder().decode(clean), Charsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

/** Downloads the sources, a few at a time, and hands each source's links over as soon as that source is read. */
object ConfigFetcher {
    private const val TIMEOUT_MS = 15_000
    private const val PARALLEL_SOURCES = 4

    /** How many shuffled links of one source are offered to the importer; the importer stops at its own limits. */
    const val CANDIDATES_PER_SOURCE = 120

    suspend fun fetchAll(
        sources: List<ConfigSource>,
        onSource: suspend (ConfigSourceResult) -> Unit,
    ) = coroutineScope {
        val gate = Semaphore(PARALLEL_SOURCES)
        sources.map { source ->
            async {
                val links = gate.withPermit { download(source) }
                onSource(ConfigSourceResult(source, links))
            }
        }.forEach { it.await() }
    }

    private suspend fun download(source: ConfigSource): List<String> = withContext(Dispatchers.IO) {
        try {
            val body = HttpUtil.getUrlContent(UrlContentRequest(url = source.url, timeout = TIMEOUT_MS))
            ConfigLinkParser.extractLinks(body).shuffled().take(CANDIDATES_PER_SOURCE)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Config source failed: ${source.name}", e)
            emptyList()
        }
    }
}
