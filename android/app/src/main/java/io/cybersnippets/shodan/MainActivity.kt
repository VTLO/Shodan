package io.cybersnippets.shodan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val ShodanColors = darkColorScheme(
    primary = Color(0xFFE8403A),
    onPrimary = Color.White,
    secondary = Color(0xFF4FC3F7),
    background = Color(0xFF0B1016),
    surface = Color(0xFF121A24),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF1B2733),
    onSurfaceVariant = Color(0xFFAFC0D0),
    error = Color(0xFFFF6E6E),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = ShodanColors) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val vm: MainViewModel = viewModel()
                    AppScreen(vm)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(vm: MainViewModel) {
    val state by vm.state.collectAsState()
    var showKeyDialog by remember { mutableStateOf(false) }
    var customQuery by remember { mutableStateOf("") }

    if (showKeyDialog) {
        ApiKeyDialog(
            initial = state.apiKey,
            onDismiss = { showKeyDialog = false },
            onSave = {
                vm.saveApiKey(it)
                showKeyDialog = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shodan Dorks") },
                actions = {
                    IconButton(onClick = { showKeyDialog = true }) {
                        Icon(Icons.Filled.Key, contentDescription = "API key")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Key + custom query controls
            item {
                ControlsCard(
                    hasKey = state.apiKey.isNotBlank(),
                    customQuery = customQuery,
                    onQueryChange = { customQuery = it },
                    running = state.running,
                    onRun = { vm.runQuery(customQuery) },
                    onCheck = { vm.checkAccount() },
                    onOpenKey = { showKeyDialog = true },
                )
            }

            // Messages
            state.error?.let { msg ->
                item { Banner(msg, isError = true) { vm.clearMessages() } }
            }
            state.infoMessage?.let { msg ->
                item { Banner(msg, isError = false) { vm.clearMessages() } }
            }

            if (state.activeQuery.isNotBlank() && (state.results.isNotEmpty() || state.running)) {
                item {
                    ResultsHeader(
                        query = state.activeQuery,
                        total = state.total,
                        shown = state.results.size,
                    )
                }
                items(state.results) { host -> ResultCard(host) }
                item {
                    LoadMoreRow(
                        running = state.running,
                        canLoadMore = state.results.size < state.total && state.results.isNotEmpty(),
                        onLoadMore = { vm.loadNextPage() },
                    )
                }
            } else {
                // Catalog
                DorkCatalog.categories.forEach { category ->
                    item { SectionHeader(category.name) }
                    items(category.dorks) { dork ->
                        DorkRow(dork = dork, enabled = !state.running) { vm.runDork(dork) }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ControlsCard(
    hasKey: Boolean,
    customQuery: String,
    onQueryChange: (String) -> Unit,
    running: Boolean,
    onRun: () -> Unit,
    onCheck: () -> Unit,
    onOpenKey: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = customQuery,
                onValueChange = onQueryChange,
                label = { Text("Custom Shodan query") },
                placeholder = { Text("e.g. apache country:\"DE\" port:443") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRun,
                    enabled = !running,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Run")
                }
                OutlinedButton(onClick = onCheck, enabled = !running) {
                    Text("Check key")
                }
            }
            if (!hasKey) {
                TextButton(onClick = onOpenKey) {
                    Text("Set your Shodan API key to run searches →")
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp, start = 4.dp),
    )
}

@Composable
private fun DorkRow(dork: Dork, enabled: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    dork.title,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "Run",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                dork.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                dork.query,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun ResultsHeader(query: String, total: Int, shown: Int) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(
            "Results",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            query,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            "Showing $shown of $total matches",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ResultCard(host: HostResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "${host.ip}:${host.port}/${host.transport}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val subtitle = listOf(host.product, host.org, host.location)
                .filter { it.isNotBlank() }
                .joinToString(" • ")
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (host.hostnames.isNotEmpty()) {
                Text(
                    host.hostnames.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (host.banner.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    host.banner,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 8,
                )
            }
        }
    }
}

@Composable
private fun LoadMoreRow(running: Boolean, canLoadMore: Boolean, onLoadMore: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        when {
            running -> CircularProgressIndicator(modifier = Modifier.height(28.dp).width(28.dp))
            canLoadMore -> OutlinedButton(onClick = onLoadMore) { Text("Load more") }
            else -> Text(
                "End of results",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Banner(message: String, isError: Boolean, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.secondary,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                message,
                color = Color.Black,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onDismiss) { Text("Dismiss", color = Color.Black) }
        }
    }
}

@Composable
private fun ApiKeyDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shodan API key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Stored only on this device. Get yours from your Shodan account page.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    label = { Text("API key") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
