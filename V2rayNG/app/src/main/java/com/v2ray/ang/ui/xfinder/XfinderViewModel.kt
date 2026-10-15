package com.v2ray.ang.ui.xfinder

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.v2ray.ang.R
import com.v2ray.ang.ui.base.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class XfinderSource(val url: String, val label: String) {
    TOP20(XfinderSources.TOP20_URL, "top20"),
    ALL(XfinderSources.ALL_URL, "all"),
}

data class XfinderUiState(
    val source: XfinderSource = XfinderSource.TOP20,
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

    init {
        load(XfinderSource.TOP20)
    }

    fun selectSource(source: XfinderSource) {
        if (source == _uiState.value.source && !_uiState.value.hasError) return
        load(source)
    }

    fun refresh() = load(_uiState.value.source)

    private fun load(source: XfinderSource) {
        // A newer request supersedes the running one, so a slow "all" never overwrites "top 20".
        loadJob?.cancel()
        _uiState.update { it.copy(source = source, isLoading = true, hasError = false) }
        loadJob = viewModelScope.launch {
            try {
                XfinderLogStore.info("fetch ${source.label}: ${source.url}")
                val text = repository.fetchText(source.url)
                val parsed = withContext(Dispatchers.Default) { XfinderParser.parse(text) }
                if (text == null || parsed.isEmpty()) {
                    XfinderLogStore.error("fetch ${source.label}: failed or empty")
                    _uiState.update { it.copy(items = emptyList(), isLoading = false, hasError = true) }
                } else {
                    XfinderLogStore.info("fetch ${source.label}: ${parsed.size} configs")
                    _uiState.update { it.copy(items = parsed, isLoading = false, hasError = false) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                XfinderLogStore.error("fetch ${source.label}: ${e.javaClass.simpleName}")
                _uiState.update { it.copy(items = emptyList(), isLoading = false, hasError = true) }
            }
        }
    }

    /**
     * Adds the Xfinder top 20 link as the subscription "Matix | Xfinder" in one tap (instead of adding configs one by
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
