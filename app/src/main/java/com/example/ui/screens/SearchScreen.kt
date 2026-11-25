package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.example.R
import com.example.data.model.AssetDto
import com.example.ui.components.AssetGridTile
import com.example.ui.components.ErrorStateCard
import com.example.ui.viewmodel.SearchMode
import com.example.ui.viewmodel.SearchUiState
import com.example.ui.viewmodel.UiState

private val SmartSuggestions = listOf("Atardecer", "Playa", "Montaña", "Noche", "Retrato", "Arquitectura")

@Composable
fun SearchScreen(
    state: SearchUiState,
    serverUrl: String,
    imageLoader: ImageLoader,
    onQueryChanged: (String) -> Unit,
    onModeChanged: (SearchMode) -> Unit,
    onExecuteSearch: (String?) -> Unit,
    onAssetClick: (AssetDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = state.mode == SearchMode.SMART_AI,
                onClick = { onModeChanged(SearchMode.SMART_AI) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                modifier = Modifier.testTag("search_mode_smart"),
            ) {
                Text(stringResource(R.string.search_smart_mode))
            }
            SegmentedButton(
                selected = state.mode == SearchMode.METADATA,
                onClick = { onModeChanged(SearchMode.METADATA) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {
                    Icon(
                        Icons.Default.ImageSearch,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                modifier = Modifier.testTag("search_mode_metadata"),
            ) {
                Text(stringResource(R.string.search_metadata_mode))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                placeholder = {
                    Text(
                        if (state.mode == SearchMode.SMART_AI) {
                            stringResource(R.string.search_placeholder_smart)
                        } else {
                            stringResource(R.string.search_placeholder_meta)
                        }
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        onExecuteSearch(null)
                    },
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_query_input"),
            )
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onExecuteSearch(null)
                },
                enabled = state.query.isNotBlank(),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .height(54.dp)
                    .testTag("search_submit_button"),
            ) {
                Text("Ir")
            }
        }

        // Quick Discovery Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmartSuggestions.forEach { term ->
                AssistChip(
                    onClick = {
                        focusManager.clearFocus()
                        onExecuteSearch(term)
                    },
                    label = { Text(term) },
                )
            }
        }

        when (val results = state.resultsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorStateCard(
                        message = results.message,
                        onRetry = { onExecuteSearch(null) },
                    )
                }
            }

            is UiState.Success -> {
                val items = results.data
                if (!state.hasSearched) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Busca por concepto o nombre de archivo",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Smart Search usa los vectores CLIP de tu servidor Immich para encontrar escenas y objetos al instante.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Sin resultados para \"${state.query}\".",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 112.dp),
                        contentPadding = PaddingValues(bottom = 88.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("search_results_grid"),
                    ) {
                        items(items = items, key = { it.id }) { asset ->
                            AssetGridTile(
                                asset = asset,
                                serverUrl = serverUrl,
                                imageLoader = imageLoader,
                                onClick = { onAssetClick(asset) },
                            )
                        }
                    }
                }
            }
        }
    }
}
