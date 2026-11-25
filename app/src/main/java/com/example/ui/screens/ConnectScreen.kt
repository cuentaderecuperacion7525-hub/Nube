package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.viewmodel.AuthMode
import com.example.ui.viewmodel.ConnectUiState

@Composable
fun ConnectScreen(
    state: ConnectUiState,
    onServerUrlChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onAuthModeSelected: (AuthMode) -> Unit,
    onProbeServer: () -> Unit,
    onConnect: () -> Unit,
    onExploreGitHub: () -> Unit,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_connect_hero),
                    contentDescription = "Ilustración ImmichVault",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                                    MaterialTheme.colorScheme.background,
                                ),
                            ),
                        ),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(99.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "IMMICH SELF-HOSTED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.connect_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            // Form Card Container
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.connect_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Server URL + Probe button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.serverUrl,
                        onValueChange = onServerUrlChanged,
                        label = { Text(stringResource(R.string.server_url_label)) },
                        placeholder = { Text("https://demo.immich.app") },
                        leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("server_url_input"),
                    )

                    FilledTonalButton(
                        onClick = onProbeServer,
                        enabled = state.serverUrl.isNotBlank() && !state.isProbing,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .height(52.dp)
                            .testTag("verify_server_button"),
                    ) {
                        if (state.isProbing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.verify_server))
                        }
                    }
                }

                // Live Server Ping Badge
                AnimatedVisibility(visible = state.probeResult != null || state.probeError != null) {
                    state.probeResult?.let { probe ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                                Column {
                                    Text(
                                        text = "Servidor Immich en línea",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    val ver = probe.about?.version?.takeIf { it.isNotBlank() }
                                    Text(
                                        text = if (ver != null) "Versión $ver · ${probe.normalizedRootUrl}"
                                        else probe.normalizedRootUrl,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                                    )
                                }
                            }
                        }
                    }
                    state.probeError?.let { err ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }

                // Auth Mode Switcher
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SegmentedButton(
                        selected = state.authMode == AuthMode.CREDENTIALS,
                        onClick = { onAuthModeSelected(AuthMode.CREDENTIALS) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("auth_mode_credentials"),
                    ) {
                        Text(stringResource(R.string.auth_mode_credentials))
                    }
                    SegmentedButton(
                        selected = state.authMode == AuthMode.API_KEY,
                        onClick = { onAuthModeSelected(AuthMode.API_KEY) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("auth_mode_api_key"),
                    ) {
                        Text(stringResource(R.string.auth_mode_api_key))
                    }
                }

                // Auth Inputs
                if (state.authMode == AuthMode.CREDENTIALS) {
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = onEmailChanged,
                        label = { Text(stringResource(R.string.email_label)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input"),
                    )
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = onPasswordChanged,
                        label = { Text(stringResource(R.string.password_label)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = "Mostrar u ocultar contraseña",
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { onConnect() }),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                    )
                } else {
                    OutlinedTextField(
                        value = state.apiKey,
                        onValueChange = onApiKeyChanged,
                        label = { Text(stringResource(R.string.api_key_label)) },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onConnect() }),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                    )
                }

                if (state.submitError != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                        ),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            text = state.submitError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                Button(
                    onClick = onConnect,
                    enabled = !state.isSubmitting && state.serverUrl.isNotBlank(),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("connect_submit_button"),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = stringResource(R.string.connect_button),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                // GitHub Explorer CTA (works without server login!)
                OutlinedButton(
                    onClick = onExploreGitHub,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("explore_github_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.explore_github_button))
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
