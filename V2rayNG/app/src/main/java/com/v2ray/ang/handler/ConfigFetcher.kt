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
 * for (see [IspChoice]); empty means it is meant for every provider.
 */
data class ConfigSource(val name: String, val url: String, val isps: Set<String> = emptySet())

/**
 * Matixray: the lists the "get configs" button reads: Mahsa NG's own lists first, then the Xfinder lists and the open
 * collectors. A list that is down, moved or
 * empty is skipped, so a dead entry costs only its timeout; add an entry here (with [ConfigSource.isps] for a list made
 * for one provider) and nothing else needs to change.
 */
object ConfigSources {
    val all: List<ConfigSource> = listOf(
        // Mahsa NG: its own public lists (the Topic repositories its community keeps for Iran).
        ConfigSource("Mahsa NG", "https://raw.githubusercontent.com/MahsaNetConfigTopic/config/main/xray_final.txt"),
        ConfigSource("Mahsa NG | mirror", "https://raw.githubusercontent.com/mhcfpTopic/config/main/xray_final.txt"),
        ConfigSource("Mahsa NG | vless", "https://raw.githubusercontent.com/MahsaNetConfigTopic/config/main/xray_final_vless.txt"),
        ConfigSource("Mahsa NG | mix", "https://raw.githubusercontent.com/MahsaNetConfigTopic/config/main/mix.txt"),
        // The app's own Xfinder lists (same links as the Xfinder screen).
        ConfigSource("Matix | Xfinder Top20", "https://imatixofficel.github.io/Xfinder/output/top20.txt"),
        ConfigSource("Matix | Xfinder All", "https://imatixofficel.github.io/Xfinder/output/all.txt"),
        // Lists that already keep only configs that were checked from inside Iran.
        ConfigSource("OpenRay Iran top100", "https://raw.githubusercontent.com/sakha1370/OpenRay/refs/heads/main/output_iran/iran_top100_checked.txt"),
        ConfigSource("Iran-configs", "https://raw.githubusercontent.com/therealaleph/Iran-configs/refs/heads/main/ir_configs.txt"),
        ConfigSource("Sub_Checker_Creator", "https://raw.githubusercontent.com/hamedp-71/Sub_Checker_Creator/refs/heads/main/final.txt"),
        ConfigSource("F0rc3Run best", "https://raw.githubusercontent.com/F0rc3Run/F0rc3Run/refs/heads/main/Best-Results/sub.txt"),
        ConfigSource("patterniha", "https://raw.githubusercontent.com/patterniha/Free-Configs/main/configs.txt"),
        ConfigSource("port-based top100", "https://raw.githubusercontent.com/hamedcode/port-based-v2ray-configs/main/sub/top100.txt"),
        ConfigSource("0xRadikal top100", "https://raw.githubusercontent.com/0xRadikal/Free-v2ray-Configs/main/top100.txt"),
        // Well-known open collectors, refreshed every few minutes by their owners (also from outside Iran).
        ConfigSource("Epodonios", "https://raw.githubusercontent.com/Epodonios/v2ray-configs/main/All_Configs_Sub.txt"),
        ConfigSource("Epodonios Sub1", "https://raw.githubusercontent.com/Epodonios/v2ray-configs/refs/heads/main/Sub1.txt"),
        ConfigSource("Epodonios Sub2", "https://raw.githubusercontent.com/Epodonios/v2ray-configs/refs/heads/main/Sub2.txt"),
        ConfigSource("barry-far", "https://raw.githubusercontent.com/barry-far/V2ray-Config/main/All_Configs_Sub.txt"),
        ConfigSource("barry-far vless", "https://raw.githubusercontent.com/barry-far/V2ray-Config/main/Splitted-By-Protocol/vless.txt"),
        ConfigSource("barry-far Sub1", "https://raw.githubusercontent.com/barry-far/V2ray-config/main/Sub1.txt"),
        ConfigSource("ebrasha", "https://raw.githubusercontent.com/ebrasha/free-v2ray-public-list/refs/heads/main/V2Ray-Config-By-EbraSha.txt"),
        ConfigSource("ebrasha vless", "https://raw.githubusercontent.com/ebrasha/free-v2ray-public-list/refs/heads/main/vless_configs.txt"),
        ConfigSource("MatinGhanbari", "https://raw.githubusercontent.com/MatinGhanbari/v2ray-configs/main/subscriptions/v2ray/super-sub.txt"),
        ConfigSource("TVC (yebekhe)", "https://raw.githubusercontent.com/yebekhe/TVC/main/subscriptions/xray/base64/mix"),
        ConfigSource("V2RayAggregator", "https://raw.githubusercontent.com/mahdibland/V2RayAggregator/master/sub/sub_merge.txt"),
        ConfigSource("SSAggregator", "https://raw.githubusercontent.com/mahdibland/SSAggregator/master/sub/sub_merge.txt"),
        ConfigSource("Telegram collector", "https://raw.githubusercontent.com/soroushmirzaei/telegram-configs-collector/main/splitted/mixed"),
        ConfigSource("Telegram collector (mohamadfg)", "https://raw.githubusercontent.com/mohamadfg-dev/telegram-v2ray-configs-collector/refs/heads/main/category/vless.txt"),
        ConfigSource("Pawdroid", "https://raw.githubusercontent.com/Pawdroid/Free-servers/main/sub"),
        ConfigSource("ermaozi", "https://raw.githubusercontent.com/ermaozi/get_subscribe/main/subscribe/v2ray.txt"),
        ConfigSource("4Diana", "https://raw.githubusercontent.com/EEvanescence/4Diana/main/AllConfigsSub.txt"),
        ConfigSource("NiREvil", "https://raw.githubusercontent.com/NiREvil/vless/refs/heads/main/sub/SSTime"),
        ConfigSource("Argh94", "https://raw.githubusercontent.com/Argh94/V2RayAutoConfig/refs/heads/main/configs/Hysteria2.txt"),
        ConfigSource("V2RayRoot", "https://raw.githubusercontent.com/V2RayRoot/V2RayConfig/refs/heads/main/Config/vless.txt"),
        ConfigSource("multi-proxy fetcher", "https://raw.githubusercontent.com/4n0nymou3/multi-proxy-config-fetcher/refs/heads/main/configs/proxy_configs.txt"),
        ConfigSource("free-v2ray-collector", "https://raw.githubusercontent.com/iboxz/free-v2ray-collector/main/main/mix.txt"),
        ConfigSource("NewCollector", "https://raw.githubusercontent.com/rango-cfs/NewCollector/refs/heads/main/v2ray_links.txt"),
        ConfigSource("kamaji", "https://raw.githubusercontent.com/shabane/kamaji/master/hub/merged.txt"),
        ConfigSource("awesome-vpn", "https://raw.githubusercontent.com/awesome-vpn/awesome-vpn/master/all"),
        ConfigSource("xray-config-toolkit", "https://raw.githubusercontent.com/wuqb2i4f/xray-config-toolkit/refs/heads/main/output/base64/mix-uri"),
        ConfigSource("freeSub", "https://raw.githubusercontent.com/Ruk1ng001/freeSub/main/v2ray"),
        ConfigSource("freenode", "https://raw.githubusercontent.com/ripaojiedian/freenode/main/sub"),
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
    private const val TIMEOUT_MS = 8_000L
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
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
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
                ConfigLinkParser.extractLinks(String(bytes, Charsets.UTF_8), truncated)
                    .shuffled()
                    .take(CANDIDATES_PER_SOURCE)
            }
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Config source failed: ${source.name}", e)
            emptyList()
        }
    }
}
