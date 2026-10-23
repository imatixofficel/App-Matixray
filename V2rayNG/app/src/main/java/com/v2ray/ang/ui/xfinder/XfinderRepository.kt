package com.v2ray.ang.ui.xfinder

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.dto.entities.SubscriptionCache
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.handler.AngConfigManager
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsChangeManager
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Remote sources for the Xfinder and scanner tabs. */
object XfinderSources {
    const val ALL_URL = "https://imatixofficel.github.io/Xfinder/output/all.txt"
    const val TOP20_URL = "https://imatixofficel.github.io/Xfinder/output/top20.txt"
    const val VLESS_URL = "https://imatixofficel.github.io/Xfinder/output/vless.txt"
    const val VMESS_URL = "https://imatixofficel.github.io/Xfinder/output/vmess.txt"
    const val TROJAN_URL = "https://imatixofficel.github.io/Xfinder/output/trojan.txt"
    const val WIREGUARD_URL = "https://imatixofficel.github.io/Xfinder/output/wireguard.txt"
    /** The name the Xfinder subscription gets in the subscription list. */
    const val SUBSCRIPTION_NAME = "Matix | Xfinder"
    const val SCANNER_URL = "https://imatixofficel.github.io/Scanner-matix/data/clean_ips.json"
}

/**
 * Network + import plumbing for the Xfinder screens. Fetching uses the app's existing OkHttp stack
 * through [HttpUtil]; importing goes through the same [AngConfigManager] path the main screen uses.
 */
class XfinderRepository {
    /** raw share link -> GUID it was imported as, so re-tapping a row never duplicates the profile. */
    private val importedGuids = HashMap<String, String>()

    suspend fun fetchText(url: String): String? = withContext(Dispatchers.IO) {
        HttpUtil.getUrlContent(UrlContentRequest(url = url, timeout = FETCH_TIMEOUT_MS))
    }

    /**
     * Imports [raw], marks it as the selected server and flags the main screen to refresh its
     * lists and restart a running service. Returns the GUID, or null when nothing was imported.
     */
    suspend fun importAndSelect(raw: String): String? = withContext(Dispatchers.IO) {
        try {
            val known = importedGuids[raw]
            val existing = MmkvManager.decodeAllServerList().toSet()
            val guid = if (known != null && known in existing) {
                known
            } else {
                val (count, _) = AngConfigManager.importBatchConfig(raw, "", true)
                if (count <= 0) return@withContext null
                val created = MmkvManager.decodeAllServerList().firstOrNull { it !in existing }
                    ?: return@withContext null
                importedGuids[raw] = created
                created
            }
            MmkvManager.setSelectServer(guid)
            SettingsChangeManager.makeSetupGroupTab()
            SettingsChangeManager.makeRestartService()
            guid
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Xfinder: failed to import and select config", e)
            null
        }
    }

    /** Imports every share link of [raws] in one batch as connection links; returns how many profiles were added. */
    suspend fun importAll(raws: List<String>): Int = withContext(Dispatchers.IO) {
        try {
            val (count, _) = AngConfigManager.importBatchConfig(raws.joinToString("\n"), "", true)
            if (count > 0) {
                SettingsChangeManager.makeSetupGroupTab()
            }
            count
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Xfinder: failed to import all configs", e)
            0
        }
    }

    /**
     * Adds the Xfinder all link as a subscription named [XfinderSources.SUBSCRIPTION_NAME] (the one already there,
     * found by its link or by the older top 20 link, when it was added before) and downloads its configs. Returns the number of configs it now
     * holds, [NOT_FETCHED] when the subscription is saved but the download failed (the updater will try again), or
     * null when it could not be saved.
     */
    suspend fun addXfinderSubscription(): Int? = withContext(Dispatchers.IO) {
        try {
            val existing = MmkvManager.decodeSubscriptions()
                .firstOrNull { it.subscription.url.trim() in listOf(XfinderSources.ALL_URL, XfinderSources.TOP20_URL) }
            val subId = existing?.guid ?: Utils.getUuid()
            val item = existing?.subscription ?: SubscriptionItem()
            item.remarks = XfinderSources.SUBSCRIPTION_NAME
            item.url = XfinderSources.ALL_URL
            item.enabled = true
            item.autoUpdate = true
            MmkvManager.encodeSubscription(subId, item)
            val result = AngConfigManager.updateConfigViaSub(SubscriptionCache(subId, item))
            SettingsChangeManager.makeSetupGroupTab()
            if (result.successCount > 0) result.configCount else NOT_FETCHED
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Xfinder: failed to add the subscription", e)
            null
        }
    }

    companion object {
        /** [addXfinderSubscription]: the subscription was saved but nothing could be downloaded. */
        const val NOT_FETCHED = -1
        private const val FETCH_TIMEOUT_MS = 15_000
    }
}
