package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * One public list of share links the "get configs" button reads. [isps] names the internet providers the list is meant
 * for (see [IspChoice]); empty means it is meant for every provider. A [fallback] list is read only when the lists
 * that come first left too few working configs after the test (see [ConfigSources]).
 */
data class ConfigSource(
    val name: String,
    val url: String,
    val isps: Set<String> = emptySet(),
    val fallback: Boolean = false,
    /** True for a list the user added in the settings; only those can be removed there. */
    val custom: Boolean = false,
) {
    /** The link is the stable identity of a list: the settings store its on/off state and its count under it. */
    val id: String get() = url
}

/**
 * Matixray: the lists the "get configs" button reads. The app's own Xfinder lists come first and are the only ones
 * read at first (all.txt is the main one; top20.txt is small, so it arrives first); the Mahsa free-config lists are [ConfigSource.fallback]: they are read only when the Xfinder ones
 * left too few working configs after the test. A list that is down, moved or empty is skipped, so a dead entry costs
 * only its timeout; add an entry here (with [ConfigSource.isps] for a list made for one provider) and nothing else
 * needs to change. The Xfinder ss and hysteria2 lists are not here: they are empty.
 */
object ConfigSources {
    private const val XFINDER = "https://imatixofficel.github.io/Xfinder/output"
    private const val MAHSA = "https://raw.githubusercontent.com/mahsanet/MahsaFreeConfig/refs/heads/main"

    val all: List<ConfigSource> = listOf(
        ConfigSource("Matix | Xfinder All", "$XFINDER/all.txt"),
        ConfigSource("Matix | Xfinder Top 20", "$XFINDER/top20.txt"),
        ConfigSource("Matix | Xfinder VLESS", "$XFINDER/vless.txt"),
        ConfigSource("Matix | Xfinder VMess", "$XFINDER/vmess.txt"),
        ConfigSource("Matix | Xfinder Trojan", "$XFINDER/trojan.txt"),
        ConfigSource("Matix | Xfinder WireGuard", "$XFINDER/wireguard.txt"),
        ConfigSource("4n0nymou3 | Multi Proxy", "https://raw.githubusercontent.com/4n0nymou3/multi-proxy-config-fetcher/refs/heads/main/configs/proxy_configs.txt"),
        ConfigSource("Mahsa | MCI 1", "$MAHSA/mci/sub_1.txt", fallback = true),
        ConfigSource("Mahsa | MCI 2", "$MAHSA/mci/sub_2.txt", fallback = true),
        ConfigSource("Mahsa | MCI 3", "$MAHSA/mci/sub_3.txt", fallback = true),
        ConfigSource("Mahsa | MCI 4", "$MAHSA/mci/sub_4.txt", fallback = true),
        ConfigSource("Mahsa | MTN 1", "$MAHSA/mtn/sub_1.txt", fallback = true),
        ConfigSource("Mahsa | MTN 2", "$MAHSA/mtn/sub_2.txt", fallback = true),
        ConfigSource("Mahsa | MTN 3", "$MAHSA/mtn/sub_3.txt", fallback = true),
        ConfigSource("Mahsa | MTN 4", "$MAHSA/mtn/sub_4.txt", fallback = true),
    )

    /**
     * The lists that fit the provider the user chose ([IspChoice.AUTO] or an unknown id means all of them), plus the
     * lists the user added ([custom]), minus the ones the user switched off ([disabled], by [ConfigSource.id]).
     */
    fun forIsp(
        isp: String,
        disabled: Set<String> = emptySet(),
        custom: List<ConfigSource> = emptyList(),
    ): List<ConfigSource> {
        val fitting = all.filter { it.isps.isEmpty() || isp in it.isps }.ifEmpty { all }
        return (fitting + custom).filter { it.id !in disabled }
    }
}

/**
 * Matixray: what the settings remember about the sources: the ones switched off, the ones the user added, and how many
 * configs of each worked in the last run. Stored in the app settings; the screens reach it through the ViewModel.
 */
object ConfigSourceStore {
    private const val SEP = '\u0001'

    fun disabled(): Set<String> = MmkvManager.decodeSettingsStringSet(AppConfig.PREF_SRC_DISABLED)?.toSet() ?: emptySet()

    fun setEnabled(id: String, enabled: Boolean) {
        val set = disabled().toMutableSet()
        if (enabled) set.remove(id) else set.add(id)
        MmkvManager.encodeSettings(AppConfig.PREF_SRC_DISABLED, set)
    }

    fun custom(): List<ConfigSource> =
        (MmkvManager.decodeSettingsStringSet(AppConfig.PREF_SRC_CUSTOM) ?: emptySet<String>())
            .mapNotNull { entry ->
                val name = entry.substringBefore(SEP, "")
                val url = entry.substringAfter(SEP, "")
                if (name.isBlank() || url.isBlank()) null else ConfigSource(name, url, custom = true)
            }
            .sortedBy { it.name.lowercase() }

    /** Adds a list; false when [url] is not an http(s) link or the list is already there. */
    fun addCustom(name: String, url: String): Boolean {
        val cleanUrl = url.trim()
        if (!isValidUrl(cleanUrl)) return false
        if (ConfigSources.all.any { it.url == cleanUrl } || custom().any { it.url == cleanUrl }) return false
        val cleanName = name.trim().ifBlank { cleanUrl.substringAfter("://").substringBefore('/') }
        val set = (MmkvManager.decodeSettingsStringSet(AppConfig.PREF_SRC_CUSTOM) ?: emptySet<String>()).toMutableSet()
        set.add("$cleanName$SEP$cleanUrl")
        MmkvManager.encodeSettings(AppConfig.PREF_SRC_CUSTOM, set)
        return true
    }

    fun removeCustom(id: String) {
        val set = (MmkvManager.decodeSettingsStringSet(AppConfig.PREF_SRC_CUSTOM) ?: emptySet<String>()).toMutableSet()
        set.removeAll { it.substringAfter(SEP, "") == id }
        MmkvManager.encodeSettings(AppConfig.PREF_SRC_CUSTOM, set)
        setEnabled(id, true)
        MmkvManager.encodeSettings(AppConfig.PREF_SRC_OK_PREFIX + id, -1)
    }

    /** How many configs of the list worked in the last run; null when no run has read it yet. */
    fun workingCount(id: String): Int? =
        MmkvManager.decodeSettingsInt(AppConfig.PREF_SRC_OK_PREFIX + id, -1).takeIf { it >= 0 }

    fun setWorkingCount(id: String, count: Int) {
        MmkvManager.encodeSettings(AppConfig.PREF_SRC_OK_PREFIX + id, count)
    }

    fun isValidUrl(url: String): Boolean =
        (url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)) &&
            url.length > 10 && url.none { it.isWhitespace() }
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

/** What one source gave: the share links found in it (fastest first, or shuffled for a fallback list), empty when it was down or held none. */
data class ConfigSourceResult(val source: ConfigSource, val links: List<String>)

/** Pure parsing of a downloaded list: plain lines or a base64 body, only the link kinds the app can import. */
object ConfigLinkParser {
    private val schemes = listOf("vless://", "vmess://", "trojan://", "ss://", "hysteria2://", "hy2://", "wireguard://")
    private const val MAX_LINK_LENGTH = 4000

    /**
     * @param truncated true when [body] is only the first part of a longer list: the last line may be cut in the
     * middle, so it is dropped rather than imported as a broken link.
     */
    fun extractLinks(body: String?, truncated: Boolean = false): List<String> {
        if (body.isNullOrBlank()) return emptyList()
        val direct = linksIn(body, truncated)
        if (direct.isNotEmpty()) return direct
        val decoded = decodeBase64(body, truncated) ?: return emptyList()
        return linksIn(decoded, truncated)
    }

    private val pingInName = Regex("""(\d+)\s*ms""", RegexOption.IGNORE_CASE)

    /** The latency a list wrote into a link's name ("... 49ms ..."), or null when the name has none. */
    fun pingHint(link: String): Int? {
        val name = link.substringAfter('#', "")
        if (name.isEmpty()) return null
        val decoded = try {
            java.net.URLDecoder.decode(name.replace("+", "%2B"), "UTF-8")
        } catch (e: IllegalArgumentException) {
            name
        }
        return pingInName.find(decoded)?.groupValues?.get(1)?.toIntOrNull()
    }

    /** [links] with the ones that name a low latency first; the others keep the order the list gave them. */
    fun fastestFirst(links: List<String>): List<String> = links.sortedBy { pingHint(it) ?: Int.MAX_VALUE }

    private fun linksIn(text: String, truncated: Boolean = false): List<String> {
        val lines = text.lineSequence().map { it.trim() }.toList()
        val complete = if (truncated && lines.isNotEmpty()) lines.dropLast(1) else lines
        return complete
            .filter { line ->
                line.length in 12..MAX_LINK_LENGTH && schemes.any { line.startsWith(it, ignoreCase = true) }
            }
            .distinct()
    }

    private fun decodeBase64(input: String, truncated: Boolean = false): String? {
        var clean = input.trim().replace('-', '+').replace('_', '/')
        if (clean.isEmpty()) return null
        if (truncated) {
            // A cut list ends in the middle of a 4-character group: keep only the whole groups.
            clean = clean.filter { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' }
            clean = clean.substring(0, clean.length - clean.length % 4)
        }
        return try {
            String(Base64.getMimeDecoder().decode(clean), Charsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

/**
 * Downloads the sources all at once (each on its own coroutine, a few waiting at a time) and hands each source's links
 * over as soon as that source is read, so the first answers are used while slow or dead sources are still waiting.
 */
object ConfigFetcher {
    private const val TIMEOUT_MS = 6_000L
    /** All the sources start at once: they are small requests, and most of them are on the same few hosts. */
    private const val PARALLEL_SOURCES = 48
    /** Only the first part of a list is read: 80 links per source are enough, and a few huge lists never slow the run. */
    private const val MAX_BODY_BYTES = 2L * 1024 * 1024

    /**
     * One client for every source, so connections are reused (HTTP/2 to the same host carries many lists at once)
     * instead of opening and closing a connection per list.
     */
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectionPool(ConnectionPool(PARALLEL_SOURCES, 60, TimeUnit.SECONDS))
            .build()
    }

    /** How many shuffled links of one source are offered to the importer; the importer stops at its own limits. */
    const val CANDIDATES_PER_SOURCE = 80

    /**
     * Reads [sources] and calls [onSource] for each one in the order they answer. As soon as [enough] says so the rest
     * are given up: their downloads are cancelled and not waited for, so a few slow servers never hold the run back.
     */
    suspend fun fetchAll(
        sources: List<ConfigSource>,
        enough: () -> Boolean = { false },
        onSource: suspend (ConfigSourceResult) -> Unit,
    ) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val gate = Semaphore(PARALLEL_SOURCES)
            val results = Channel<ConfigSourceResult>(Channel.UNLIMITED)
            sources.forEach { source ->
                scope.launch { results.send(ConfigSourceResult(source, gate.withPermit { download(source) })) }
            }
            for (i in sources.indices) {
                onSource(results.receive())
                if (enough()) break
            }
        } finally {
            scope.cancel()
            // Downloads still running are cut at once instead of holding a thread until their timeout.
            client.dispatcher.cancelAll()
        }
    }

    private fun download(source: ConfigSource): List<String> {
        return try {
            val request = Request.Builder().url(source.url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    LogUtil.w(AppConfig.TAG, "Config source ${source.name}: HTTP ${response.code}")
                    return emptyList()
                }
                val src = response.body.source()
                val truncated = src.request(MAX_BODY_BYTES + 1)
                val bytes = src.buffer.readByteArray(minOf(src.buffer.size, MAX_BODY_BYTES))
                val links = ConfigLinkParser.extractLinks(String(bytes, Charsets.UTF_8), truncated)
                // The app's own lists name each config's latency: the fastest are offered first. A fallback list
                // does not, so its links are offered in random order.
                (if (source.fallback) links.shuffled() else ConfigLinkParser.fastestFirst(links))
                    .take(CANDIDATES_PER_SOURCE)
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Config source failed: ${source.name}", e)
            emptyList()
        }
    }
}
