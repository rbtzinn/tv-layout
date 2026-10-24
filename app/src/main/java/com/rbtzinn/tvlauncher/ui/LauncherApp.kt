package com.rbtzinn.tvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import com.rbtzinn.tvlauncher.LauncherViewModel
import com.rbtzinn.tvlauncher.Screen
import com.rbtzinn.tvlauncher.TvApp

private val Background = Color(0xFF0E1116)
private val Panel = Color(0xFF1B2028)
private val Accent = Color(0xFF7CC4FF)
private val TextMain = Color(0xFFECEFF4)
private val TextDim = Color(0xFF9AA4B2)
private val Star = Color(0xFFFFD54F)

private const val COLUMNS = 5

@Composable
fun LauncherApp(vm: LauncherViewModel) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Accent)) {
        CompositionLocalProvider(LocalContentColor provides TextMain) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Background)
            ) {
                when (vm.screen) {
                    Screen.Home -> HomeScreen(vm)
                    Screen.AllApps -> AllAppsScreen(vm)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Tela inicial

@Composable
private fun HomeScreen(vm: LauncherViewModel) {
    // Na tela inicial o "Voltar" não faz nada (não existe tela antes dela).
    BackHandler(enabled = true) {}

    var menuFor by remember { mutableStateOf<TvApp?>(null) }
    val favorites = vm.favoriteApps
    val firstFocus = remember { FocusRequester() }

    LauncherGrid {
        if (vm.inputs.isNotEmpty()) {
            sectionTitle("Entradas")
            fullLine {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(vm.inputs, key = { it.id }) { input ->
                        Button(onClick = { vm.openInput(input) }) { Text(input.label) }
                    }
                }
            }
        }

        sectionTitle(
            "Favoritos",
            hint = "Segure OK num app para mover ou remover",
        )

        itemsIndexed(favorites, key = { _, app -> app.packageName }) { index, app ->
            AppCard(
                app = app,
                onClick = { vm.launch(app) },
                onLongClick = { menuFor = app },
                modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
            )
        }

        item(key = "__all_apps") {
            TextTile(
                title = "Todos os apps",
                subtitle = "${vm.allApps.size} instalados",
                onClick = vm::showAllApps,
                modifier = if (favorites.isEmpty()) Modifier.focusRequester(firstFocus) else Modifier,
            )
        }

        item(key = "__settings") {
            TextTile(title = "Configurações", subtitle = "da TV", onClick = vm::openSettings)
        }
    }

    LaunchedEffect(vm.loaded) {
        if (vm.loaded) runCatching { firstFocus.requestFocus() }
    }

    menuFor?.let { app ->
        FavoriteMenu(
            app = app,
            onDismiss = { menuFor = null },
            onMove = { delta ->
                vm.moveFavorite(app, delta)
                menuFor = null
            },
            onRemove = {
                vm.toggleFavorite(app)
                menuFor = null
            },
        )
    }
}

// ---------------------------------------------------------------- Todos os apps

@Composable
private fun AllAppsScreen(vm: LauncherViewModel) {
    BackHandler { vm.goHome() }

    val firstFocus = remember { FocusRequester() }

    LauncherGrid {
        sectionTitle(
            "Todos os apps",
            hint = "Segure OK para adicionar ou tirar dos favoritos (★)",
        )
        itemsIndexed(vm.allApps, key = { _, app -> app.packageName }) { index, app ->
            AppCard(
                app = app,
                starred = vm.isFavorite(app),
                onClick = { vm.launch(app) },
                onLongClick = { vm.toggleFavorite(app) },
                modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
            )
        }
    }

    LaunchedEffect(Unit) {
        runCatching { firstFocus.requestFocus() }
    }
}

// ---------------------------------------------------------------- Componentes

@Composable
private fun LauncherGrid(content: LazyGridScope.() -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(COLUMNS),
        contentPadding = PaddingValues(horizontal = 56.dp, vertical = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize(),
        content = content,
    )
}

private fun LazyGridScope.fullLine(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

private fun LazyGridScope.sectionTitle(title: String, hint: String? = null) {
    item(span = { GridItemSpan(maxLineSpan) }, key = "__title_$title") {
        Column(Modifier.padding(top = 8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = TextDim)
            }
        }
    }
}

@Composable
private fun AppCard(
    app: TvApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    starred: Boolean = false,
) {
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.aspectRatio(16f / 9f),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (app.banner != null) {
                Image(
                    bitmap = app.banner,
                    contentDescription = app.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Panel)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    app.icon?.let {
                        Image(bitmap = it, contentDescription = null, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        app.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (starred) {
                Text(
                    "★",
                    color = Star,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                )
            }
        }
    }
}

@Composable
private fun TextTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.aspectRatio(16f / 9f)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Panel)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = TextMain,
                textAlign = TextAlign.Center,
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextDim)
        }
    }
}

@Composable
private fun FavoriteMenu(
    app: TvApp,
    onDismiss: () -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val firstButton = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalContentColor provides TextMain) {
            Column(
                modifier = Modifier
                    .background(Panel, RoundedCornerShape(16.dp))
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(app.label, style = MaterialTheme.typography.titleLarge)
                Button(onClick = { onMove(-1) }, modifier = Modifier.focusRequester(firstButton)) {
                    Text("◀  Mover para a esquerda")
                }
                Button(onClick = { onMove(+1) }) { Text("Mover para a direita  ▶") }
                Button(onClick = onRemove) { Text("Remover dos favoritos") }
            }
            // Fica dentro do Dialog para rodar depois que os botões existem.
            LaunchedEffect(Unit) {
                runCatching { firstButton.requestFocus() }
            }
        }
    }
}
