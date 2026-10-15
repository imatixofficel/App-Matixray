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

/** One public list of share links the "get configs" button reads. */
data class ConfigSource(val name: String, val url: String)

/**
 * Matixray: the public lists the "get configs" button reads. A source that is down, moved or empty is skipped, so a
 * dead entry costs only its timeout; add or remove entries here, nothing else needs to change.
 */
object ConfigSources {
    val all: List<ConfigSource> = listOf(
        // The app's own Xfinder lists (same links as the Xfinder screen).
        ConfigSource("Matix | Xfinder Top20", "https://imatixofficel.github.io/Xfinder/output/top20.txt"),
        ConfigSource("Matix | Xfinder All", "https://imatixofficel.github.io/Xfinder/output/all.txt"),
        // Mahsa NG's public list.
        ConfigSource("Mahsa NG", "https://raw.githubusercontent.com/MahsaNetConfigTopic/config/main/xray_final.txt"),
        // Well-known open collectors.
        ConfigSource("Epodonios", "https://raw.githubusercontent.com/Epodonios/v2ray-configs/main/All_Configs_Sub.txt"),
        ConfigSource("barry-far", "https://raw.githubusercontent.com/barry-far/V2ray-Config/main/All_Configs_Sub.txt"),
        ConfigSource("V2RayAggregator", "https://raw.githubusercontent.com/mahdibland/V2RayAggregator/master/sub/sub_merge.txt"),
        ConfigSource("Telegram collector", "https://raw.githubusercontent.com/soroushmirzaei/telegram-configs-collector/main/splitted/mixed"),
        ConfigSource("Pawdroid", "https://raw.githubusercontent.com/Pawdroid/Free-servers/main/sub"),
        ConfigSource("ermaozi", "https://raw.githubusercontent.com/ermaozi/get_subscribe/main/subscribe/v2ray.txt"),
    )
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
    const val CANDIDATES_PER_SOURCE = 150

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
