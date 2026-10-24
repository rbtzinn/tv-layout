package com.rbtzinn.tvlauncher

import android.content.Context

/** Guarda a lista ordenada de favoritos (nomes de pacote) nas preferências do app. */
class FavoritesStore(context: Context) {

    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    fun hasSaved(): Boolean = prefs.contains(KEY)

    fun load(): List<String> =
        prefs.getString(KEY, null)
            ?.split('\n')
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    fun save(packages: List<String>) {
        prefs.edit().putString(KEY, packages.joinToString("\n")).apply()
    }

    private companion object {
        const val KEY = "favorites"
    }
}
