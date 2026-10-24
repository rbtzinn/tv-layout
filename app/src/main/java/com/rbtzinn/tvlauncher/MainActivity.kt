package com.rbtzinn.tvlauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.rbtzinn.tvlauncher.ui.LauncherApp

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<LauncherViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LauncherApp(viewModel) }
    }

    override fun onResume() {
        super.onResume()
        // Recarrega ao voltar para a tela inicial (pega apps instalados/removidos).
        viewModel.refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Botão Home apertado com o launcher já aberto: volta para a tela principal.
        viewModel.goHome()
    }
}
