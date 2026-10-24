package com.rbtzinn.tvlauncher

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import androidx.compose.runtime.Immutable

@Immutable
data class TvInputItem(val id: String, val label: String)

object InputsRepository {

    /**
     * Lista as entradas HDMI da TV usando a API oficial do Android TV.
     * Cada fabricante implementa isso de um jeito, por isso tudo é defensivo:
     * se a TCL não expuser as entradas, a lista volta vazia e a seção some.
     */
    fun loadHdmiInputs(context: Context): List<TvInputItem> {
        val manager = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
            ?: return emptyList()

        val inputs = runCatching { manager.tvInputList }.getOrNull() ?: return emptyList()

        return inputs
            .filter { it.type == TvInputInfo.TYPE_HDMI && it.parentId == null }
            .filter { runCatching { !it.isHidden(context) }.getOrDefault(true) }
            .map { info ->
                val custom = runCatching { info.loadCustomLabel(context) }.getOrNull()
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
                TvInputItem(
                    id = info.id,
                    label = custom ?: info.loadLabel(context).toString(),
                )
            }
            .sortedBy { it.label }
    }

    /** Intent que pede ao app de TV do sistema para abrir a entrada HDMI. */
    fun openIntent(input: TvInputItem): Intent =
        Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(input.id))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
