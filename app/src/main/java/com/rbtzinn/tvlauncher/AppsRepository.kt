package com.rbtzinn.tvlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

@Immutable
data class TvApp(
    val packageName: String,
    val label: String,
    /** Banner 16:9 que os apps de TV fornecem (Netflix, YouTube etc.). */
    val banner: ImageBitmap?,
    /** Ícone quadrado, usado só quando o app não tem banner. */
    val icon: ImageBitmap?,
)

class AppsRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    // Imagens já carregadas ficam em memória: abrir a tela inicial de novo é instantâneo.
    private val cache = HashMap<String, TvApp>()

    /** Lista os apps que podem ser abertos na TV. Rodar fora da thread principal. */
    fun loadApps(): List<TvApp> {
        val found = LinkedHashMap<String, ResolveInfo>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (info in queryActivities(intent)) {
                found.putIfAbsent(info.activityInfo.packageName, info)
            }
        }
        found.remove(context.packageName)
        cache.keys.retainAll(found.keys)

        return found.values
            .map { info -> cache.getOrPut(info.activityInfo.packageName) { build(info) } }
            .sortedBy { it.label.lowercase() }
    }

    fun launchIntent(packageName: String): Intent? =
        pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)

    @Suppress("DEPRECATION")
    private fun queryActivities(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            pm.queryIntentActivities(intent, 0)
        }

    private fun build(info: ResolveInfo): TvApp {
        val activity = info.activityInfo
        val label = info.loadLabel(pm).toString()

        val banner = runCatching {
            (activity.loadBanner(pm) ?: activity.applicationInfo.loadBanner(pm))
                ?.toBitmap(BANNER_W, BANNER_H)
                ?.asImageBitmap()
        }.getOrNull()

        val icon = if (banner == null) {
            runCatching { info.loadIcon(pm).toBitmap(ICON_SIZE, ICON_SIZE).asImageBitmap() }.getOrNull()
        } else {
            null
        }

        return TvApp(activity.packageName, label, banner, icon)
    }

    private companion object {
        const val BANNER_W = 320
        const val BANNER_H = 180
        const val ICON_SIZE = 128
    }
}
