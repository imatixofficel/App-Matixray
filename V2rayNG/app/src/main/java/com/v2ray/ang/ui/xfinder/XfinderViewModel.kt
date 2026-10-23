package com.v2ray.ang.ui.xfinder

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.v2ray.ang.R
import com.v2ray.ang.ui.base.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class XfinderSource(val url: String, val label: String) {
    ALL(XfinderSources.ALL_URL, "all"),
    TOP20(XfinderSources.TOP20_URL, "top20"),
    VLESS(XfinderSources.VLESS_URL, "vless"),
    VMESS(XfinderSources.VMESS_URL, "vmess"),
    TROJAN(XfinderSources.TROJAN_URL, "trojan"),
    WIREGUARD(XfinderSources.WIREGUARD_URL, "wireguard"),
}

/** The lists that held at least one config, in their usual order: an empty or unreachable list is not offered. */
fun availableXfinderSources(results: Map<XfinderSource, List<XfinderConfig>>): List<XfinderSource> =
    XfinderSource.entries.filter { results[it].orEmpty().isNotEmpty() }

/** The list to show: [current] when it is still offered, else the first one that is, else null (nothing to show). */
fun pickXfinderSource(current: XfinderSource, available: List<XfinderSource>): XfinderSource? =
    if (current in available) current else available.firstOrNull()

data class XfinderUiState(
    val source: XfinderSource = XfinderSource.ALL,
    /** The lists offered as tabs; all of them until the first load tells which ones are empty. */
    val sources: List<XfinderSource> = XfinderSource.entries,
    val items: List<XfinderConfig> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    /** True while the subscription is being added (saved and downloaded). */
    val isAddingSubscription: Boolean = false,
)

class XfinderViewModel(application: Application) : BaseViewModel(application) {
    private val repository = XfinderRepository()

    private val _uiState = MutableStateFlow(XfinderUiState())
    val uiState: StateFlow<XfinderUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    /** What each list held at the last load (deduplicated, fastest first); switching tabs reads from here. */
    private var loaded: Map<XfinderSource, List<XfinderConfig>> = emptyMap()

    init {
        loadAll()
    }

    fun selectSource(source: XfinderSource) {
        val state = _uiState.value
        if (source == state.source && !state.hasError) return
        if (source !in state.sources) return
        _uiState.update { it.copy(source = source, items = loaded[source].orEmpty(), hasError = false) }
    }

    fun refresh() = loadAll()

    /** Reads every list at once, so the tabs can leave out the ones that came back empty. */
    private fun loadAll() {
        // A newer request supersedes the running one.
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, hasError = false) }
        loadJob = viewModelScope.launch {
            try {
                val results = coroutineScope {
                    XfinderSource.entries.map { source -> async { source to loadOne(source) } }.awaitAll()
                }.toMap()
                loaded = results
                val available = availableXfinderSources(results)
                val shown = pickXfinderSource(_uiState.value.source, available)
                if (shown == null) {
                    XfinderLogStore.error("fetch: every list failed or is empty")
                    _uiState.update { it.copy(sources = emptyList(), items = emptyList(), isLoading = false, hasError = true) }
                } else {
                    _uiState.update {
                        it.copy(source = shown, sources = available, items = results[shown].orEmpty(), isLoading = false, hasError = false)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                XfinderLogStore.error("fetch: ${e.javaClass.simpleName}")
                _uiState.update { it.copy(sources = emptyList(), items = emptyList(), isLoading = false, hasError = true) }
            }
        }
    }

    /** One list's configs; empty when it could not be downloaded or holds none. */
    private suspend fun loadOne(source: XfinderSource): List<XfinderConfig> {
        return try {
            XfinderLogStore.info("fetch ${source.label}: ${source.url}")
            val text = repository.fetchText(source.url)
            val parsed = withContext(Dispatchers.Default) { XfinderParser.parse(text) }
            if (parsed.isEmpty()) {
                XfinderLogStore.error("fetch ${source.label}: failed or empty, not listed")
            } else {
                XfinderLogStore.info("fetch ${source.label}: ${parsed.size} configs")
            }
            parsed
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            XfinderLogStore.error("fetch ${source.label}: ${e.javaClass.simpleName}")
            emptyList()
        }
    }

    /**
     * Adds the Xfinder all link as the subscription "Matix | Xfinder" in one tap (instead of adding configs one by
     * one); the subscription then keeps itself up to date.
     */
    fun addSubscription() {
        if (_uiState.value.isAddingSubscription) return
        _uiState.update { it.copy(isAddingSubscription = true) }
        viewModelScope.launch {
            try {
                XfinderLogStore.info("add subscription ${XfinderSources.SUBSCRIPTION_NAME}")
                val count = repository.addXfinderSubscription()
                when {
                    count == null -> {
                        XfinderLogStore.error("add subscription: could not be saved")
                        toastError(R.string.mx_xfinder_sub_failed)
                    }
                    count == XfinderRepository.NOT_FETCHED -> {
                        XfinderLogStore.error("add subscription: saved, download failed")
                        toast(R.string.mx_xfinder_sub_nofetch)
                    }
                    else -> {
                        XfinderLogStore.info("add subscription: $count configs")
                        toastSuccess(localizedContext.getString(R.string.mx_xfinder_sub_added, count))
                    }
                }
            } finally {
                _uiState.update { it.copy(isAddingSubscription = false) }
            }
        }
    }
}
