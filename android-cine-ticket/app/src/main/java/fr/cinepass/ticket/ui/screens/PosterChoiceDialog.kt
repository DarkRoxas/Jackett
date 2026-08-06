package fr.cinepass.ticket.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/**
 * Sélection de l'affiche : les affiches officielles de TMDB d'un côté, les
 * animations trouvées sur GIPHY de l'autre — même grille, même geste.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosterChoiceDialog(
    state: PosterChoiceState,
    onSelect: (String) -> Unit,
    onAnimatedTabOpened: () -> Unit,
    onAnimatedQueryChange: (String) -> Unit,
    onAnimatedSearch: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!state.visible) return

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) onAnimatedTabOpened()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Choisir l'affiche") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    },
                )
            },
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Affiches") },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Animées") },
                    )
                }

                if (selectedTab == 0) {
                    StillPosters(state = state, onSelect = onSelect)
                } else {
                    AnimatedPosters(
                        state = state,
                        onSelect = onSelect,
                        onQueryChange = onAnimatedQueryChange,
                        onSearch = onAnimatedSearch,
                    )
                }
            }
        }
    }
}

@Composable
private fun StillPosters(state: PosterChoiceState, onSelect: (String) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

            state.posters.isEmpty() -> Hint("Aucune affiche disponible pour ce film.")

            else -> PosterGrid(
                items = state.posters,
                key = { it.url },
                preview = { it.thumbnailUrl },
                caption = { it.resolutionLabel },
                onSelect = { onSelect(it.url) },
            )
        }
    }
}

@Composable
private fun AnimatedPosters(
    state: PosterChoiceState,
    onSelect: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.animatedQuery,
            onValueChange = onQueryChange,
            label = { Text("Rechercher une animation") },
            singleLine = true,
            enabled = state.animatedAvailable,
            trailingIcon = {
                IconButton(
                    onClick = { onSearch(state.animatedQuery) },
                    enabled = state.animatedAvailable,
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Rechercher")
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Box(Modifier.fillMaxSize()) {
            when {
                !state.animatedAvailable -> Hint(
                    "Les affiches animées viennent de GIPHY : aucune base de cinéma n'en " +
                        "diffuse par API.\n\nRenseignez une clé GIPHY dans les réglages pour " +
                        "activer cette recherche.",
                )

                state.animatedLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.animatedError != null -> Hint(state.animatedError)

                state.animated.isEmpty() -> Hint(
                    "Aucune animation pour « ${state.animatedQuery} ». Essayez le titre " +
                        "original, ou ajoutez « motion poster » à la recherche.",
                )

                else -> PosterGrid(
                    items = state.animated,
                    key = { it.id },
                    preview = { it.previewUrl },
                    caption = { it.resolutionLabel },
                    onSelect = { onSelect(it.url) },
                )
            }
        }
    }
}

@Composable
private fun <T> PosterGrid(
    items: List<T>,
    key: (T) -> Any,
    preview: (T) -> String,
    caption: (T) -> String,
    onSelect: (T) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = key) { item ->
            Column(modifier = Modifier.clickable { onSelect(item) }) {
                AsyncImage(
                    model = preview(item),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = caption(item),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Hint(message: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
