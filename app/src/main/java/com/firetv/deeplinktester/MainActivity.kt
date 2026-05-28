package com.firetv.deeplinktester

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val feedRepository = FeedRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val overrides = DeeplinkOverrides(this)
        val launcher = DeeplinkLauncher(this)

        setContent {
            MaterialTheme {
                DeeplinkLauncherScreen(
                    feedRepository = feedRepository,
                    overrides = overrides,
                    launcher = launcher,
                    feedUrl = getString(R.string.feeds_url),
                    authUsername = getString(R.string.feed_auth_username),
                    authPassword = getString(R.string.feed_auth_password),
                    configMissingMessage = getString(R.string.feed_config_missing),
                    onShowToast = { message ->
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}

@Composable
private fun DeeplinkLauncherScreen(
    feedRepository: FeedRepository,
    overrides: DeeplinkOverrides,
    launcher: DeeplinkLauncher,
    feedUrl: String,
    authUsername: String,
    authPassword: String,
    configMissingMessage: String,
    onShowToast: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<FeedItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var statusText by rememberSaveable { mutableStateOf("") }
    var editItem by remember { mutableStateOf<FeedItem?>(null) }
    var editUrl by rememberSaveable { mutableStateOf("") }
    var addFeedDialogVisible by remember { mutableStateOf(false) }
    var customTitle by rememberSaveable { mutableStateOf("") }
    var customUrl by rememberSaveable { mutableStateOf("") }
    var manualItems by remember { mutableStateOf<List<FeedItem>>(emptyList()) }
    var refreshVersion by remember { mutableIntStateOf(0) }
    val visibleItems = remember(items, manualItems) { items + manualItems }

    val loadFeed: () -> Unit = remember(feedUrl, authUsername, authPassword) {
        {
            if (feedUrl.isBlank() || authUsername.isBlank() || authPassword.isBlank()) {
                loading = false
                items = emptyList()
                statusText = configMissingMessage
                return@remember
            }

            loading = true
            statusText = "Loading feed..."
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    try {
                        Result.success(
                            feedRepository.fetchFeed(
                                feedUrl = feedUrl,
                                username = authUsername,
                                password = authPassword,
                            ),
                        )
                    } catch (e: FeedException.HttpError) {
                        Result.failure(e)
                    } catch (e: FeedException.ParseError) {
                        Result.failure(e)
                    } catch (e: IOException) {
                        Result.failure(FeedException.NetworkError(e.message ?: "Network error"))
                    } catch (e: Exception) {
                        Result.failure(FeedException.NetworkError(e.message ?: "Unknown error"))
                    }
                }

                loading = false
                result.fold(
                    onSuccess = { loaded ->
                        items = loaded
                        statusText = "${loaded.size} deeplinks loaded"
                    },
                    onFailure = { error ->
                        items = emptyList()
                        statusText = when (error) {
                            is FeedException.HttpError -> "Feed error ${error.statusCode}: ${error.message}"
                            is FeedException.ParseError -> "Could not parse feed: ${error.message}"
                            is FeedException.NetworkError -> "Network error: ${error.message}"
                            else -> "Failed to load feed"
                        }
                        onShowToast(statusText)
                    },
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        loadFeed()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF7F8FA),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Deeplink Launcher (TV Compose)",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF1F2937),
                    fontWeight = FontWeight.Bold,
                )
                FocusableTvButton(text = "Reload Feed", onClick = loadFeed, enabled = !loading)
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4B5563),
            )

            Box(modifier = Modifier.weight(1f)) {
                if (loading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items = visibleItems, key = { it.id }) { item ->
                            val effectiveUrl = remember(refreshVersion, item.id, item.firetvUrl) {
                                overrides.getUrl(item.id, item.firetvUrl)
                            }
                            FeedRow(
                                item = item,
                                effectiveUrl = effectiveUrl,
                                onOpen = {
                                    val result = launcher.open(effectiveUrl)
                                    launcher.showResultToast(result)
                                },
                                onEdit = {
                                    editItem = item
                                    editUrl = effectiveUrl
                                },
                            )
                        }
                    }
                }
            }

            FocusableTvButton(
                text = "Add Feed",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    customTitle = ""
                    customUrl = ""
                    addFeedDialogVisible = true
                },
            )
        }
    }

    if (editItem != null) {
        AlertDialog(
            onDismissRequest = { editItem = null },
            title = {
                Text(text = "Edit: ${editItem?.title.orEmpty()}")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Update the firetvUrl used when opening this feed item.")
                    OutlinedTextField(
                        value = editUrl,
                        onValueChange = { editUrl = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        singleLine = false,
                    )
                }
            },
            confirmButton = {
                FocusableTvButton(
                    text = "Done",
                    onClick = {
                        val item = editItem ?: return@FocusableTvButton
                        val updated = editUrl.trim()
                        if (updated.isEmpty()) {
                            onShowToast("Please enter a URL")
                            return@FocusableTvButton
                        }
                        overrides.saveUrl(item.id, updated)
                        refreshVersion++
                        editItem = null
                        onShowToast("Deeplink updated")
                    },
                )
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FocusableTvButton(
                        text = "Reset to feed",
                        onClick = {
                            val item = editItem ?: return@FocusableTvButton
                            overrides.resetUrl(item.id)
                            refreshVersion++
                            editItem = null
                            onShowToast("Restored feed URL")
                        },
                    )
                    FocusableTvButton(text = "Cancel", onClick = { editItem = null })
                }
            },
        )
    }

    if (addFeedDialogVisible) {
        AlertDialog(
            onDismissRequest = { addFeedDialogVisible = false },
            title = { Text("Add Feed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Title") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        label = { Text("firetvUrl") },
                        singleLine = false,
                    )
                }
            },
            confirmButton = {
                FocusableTvButton(
                    text = "Add",
                    onClick = {
                        val url = customUrl.trim()
                        if (url.isEmpty()) {
                            onShowToast("Please enter a URL")
                            return@FocusableTvButton
                        }
                        val title = customTitle.trim().ifEmpty { "Custom Feed" }
                        val newItem = FeedItem(
                            id = "manual_${UUID.randomUUID()}",
                            title = title,
                            firetvUrl = url,
                        )
                        manualItems = manualItems + newItem
                        addFeedDialogVisible = false
                        statusText = "${visibleItems.size + 1} deeplinks loaded"
                        onShowToast("Feed added")
                    },
                )
            },
            dismissButton = {
                FocusableTvButton(
                    text = "Cancel",
                    onClick = { addFeedDialogVisible = false },
                )
            },
        )
    }
}

@Composable
private fun FeedRow(
    item: FeedItem,
    effectiveUrl: String,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF111827),
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusableTvButton(text = "Open URL", onClick = onOpen)
                FocusableTvButton(text = "Edit", onClick = onEdit)
            }
            Text(
                text = effectiveUrl,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF374151),
            )
        }
    }
}

@Composable
private fun FocusableTvButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val containerColor = when {
        !enabled -> Color(0xFFE5E7EB)
        isFocused -> Color(0xFFFF9900)
        else -> Color(0xFFF3F4F6)
    }
    val contentColor = if (isFocused) Color(0xFF1A1A1A) else Color(0xFF111827)
    val border = if (isFocused) {
        BorderStroke(3.dp, Color(0xFFFFCC66))
    } else {
        BorderStroke(1.dp, Color(0xFF9CA3AF))
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        interactionSource = interactionSource,
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Color(0xFFE5E7EB),
            disabledContentColor = Color(0xFF9CA3AF),
        ),
    ) {
        Text(text)
    }
}
