package com.rbtzinn.tvlauncher

import android.app.Application
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { Home, AllApps }

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val appsRepo = AppsRepository(app)
    private val favoritesStore = FavoritesStore(app)

    var allApps by mutableStateOf<List<TvApp>>(emptyList())
        private set
    var favorites by mutableStateOf(favoritesStore.load())
        private set
    var inputs by mutableStateOf<List<TvInputItem>>(emptyList())
        private set
    var screen by mutableStateOf(Screen.Home)
        private set
    var loaded by mutableStateOf(false)
        private set

    /** Favoritos na ordem escolhida, ignorando apps que foram desinstalados. */
    val favoriteApps: List<TvApp>
        get() {
            val byPackage = allApps.associateBy { it.packageName }
            return favorites.mapNotNull { byPackage[it] }
        }

    private var refreshJob: Job? = null

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            val context = getApplication<Application>()
            val apps = withContext(Dispatchers.IO) { appsRepo.loadApps() }
            val hdmi = withContext(Dispatchers.IO) { InputsRepository.loadHdmiInputs(context) }
            allApps = apps
            inputs = hdmi
            seedDefaultFavoritesOnce(apps)
            loaded = true
        }
    }

    /** No primeiro uso, já coloca os streamings mais comuns que estiverem instalados. */
    private fun seedDefaultFavoritesOnce(apps: List<TvApp>) {
        if (favoritesStore.hasSaved()) return
        val installed = apps.map { it.packageName }.toSet()
        updateFavorites(DEFAULT_FAVORITES.filter { it in installed })
    }

    fun goHome() {
        screen = Screen.Home
    }

    fun showAllApps() {
        screen = Screen.AllApps
    }

    fun isFavorite(app: TvApp): Boolean = app.packageName in favorites

    fun toggleFavorite(app: TvApp) {
        updateFavorites(
            if (isFavorite(app)) favorites - app.packageName else favorites + app.packageName
        )
    }

    /** Move um favorito para a esquerda (-1) ou direita (+1). */
    fun moveFavorite(app: TvApp, delta: Int) {
        val visible = favoriteApps.map { it.packageName }.toMutableList()
        val from = visible.indexOf(app.packageName)
        val to = from + delta
        if (from < 0 || to !in visible.indices) return
        visible.removeAt(from)
        visible.add(to, app.packageName)
        updateFavorites(visible)
    }

    private fun updateFavorites(list: List<String>) {
        favorites = list
        favoritesStore.save(list)
    }

    fun launch(app: TvApp) {
        val intent = appsRepo.launchIntent(app.packageName)
        if (intent == null) {
            toast("Não foi possível abrir ${app.label}")
            return
        }
        start(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), "Não foi possível abrir ${app.label}")
    }

    fun openInput(input: TvInputItem) {
        start(InputsRepository.openIntent(input), "A TV não deixou abrir ${input.label}")
    }

    fun openSettings() {
        start(
            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            "Não foi possível abrir as configurações",
        )
    }

    private fun start(intent: Intent, errorMessage: String) {
        try {
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            toast(errorMessage)
        }
    }

    private fun toast(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        val DEFAULT_FAVORITES = listOf(
            "com.google.android.youtube.tv",
            "com.netflix.ninja",
            "com.amazon.amazonvideo.livingroom",
            "com.disney.disneyplus",
            "com.wbd.stream",
            "com.globo.globotv",
            "com.spotify.tv.android",
        )
    }
}
