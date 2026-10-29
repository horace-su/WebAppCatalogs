package com.qomg.webappcatalogs.webapp

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qomg.webappcatalogs.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebAppHomeScreen() {
    val context = LocalContext.current
    val repository = remember { WebAppRepository(context) }
    val apps by remember { repository.observeAll() }.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<WebApp?>(null) }

    fun open(appId: Long) = context.startActivity(WebAppShortcuts.launchIntent(context, appId))

    fun add(name: String, url: String, openAfter: Boolean) {
        scope.launch {
            val id = repository.add(name, url)
            if (openAfter) open(id) else snackbar.showSnackbar(context.getString(R.string.webapp_added, name))
        }
    }

    fun pin(app: WebApp) {
        scope.launch {
            val icon = withContext(Dispatchers.IO) { WebAppIcons.shortcutIcon(context, app) }
            if (!WebAppShortcuts.requestPin(context, app, icon)) {
                snackbar.showSnackbar(context.getString(R.string.webapp_pin_unsupported))
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.webapp_add_custom))
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text(stringResource(R.string.webapp_tab_mine, apps.size)) }
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text(stringResource(R.string.webapp_tab_catalog)) }
                )
            }
            when (tab) {
                0 -> InstalledApps(
                    apps = apps,
                    onOpen = { open(it.id) },
                    onPin = ::pin,
                    onDelete = { pendingDelete = it }
                )

                else -> CatalogApps(
                    installedUrls = remember(apps) { apps.map { it.url }.toSet() },
                    onAdd = { add(it.name, it.url, openAfter = false) },
                    onOpen = { add(it.name, it.url, openAfter = true) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddWebAppDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, url ->
                showAddDialog = false
                add(name, url, openAfter = true)
            }
        )
    }

    pendingDelete?.let { app ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.webapp_delete_title, app.name)) },
            text = { Text(stringResource(R.string.webapp_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    scope.launch { repository.delete(app) }
                }) { Text(stringResource(R.string.webapp_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(android.R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun InstalledApps(
    apps: List<WebApp>,
    onOpen: (WebApp) -> Unit,
    onPin: (WebApp) -> Unit,
    onDelete: (WebApp) -> Unit
) {
    if (apps.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.webapp_empty),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
        items(apps, key = { it.id }) { app ->
            var menuExpanded by remember { mutableStateOf(false) }
            ListItem(
                modifier = Modifier.clickable { onOpen(app) },
                leadingContent = { InstalledAppIcon(app) },
                headlineContent = { Text(app.name) },
                supportingContent = { Text(WebAppUrls.displayHost(app.url)) },
                trailingContent = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.webapp_more))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.webapp_add_to_home)) },
                                onClick = { menuExpanded = false; onPin(app) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.webapp_delete)) },
                                onClick = { menuExpanded = false; onDelete(app) }
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun CatalogApps(
    installedUrls: Set<String>,
    onAdd: (CatalogEntry) -> Unit,
    onOpen: (CatalogEntry) -> Unit
) {
    val grouped = remember { WebAppCatalog.entries.groupBy { it.category } }
    LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
        grouped.forEach { (category, entries) ->
            item(key = "header:$category") {
                Text(
                    category,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                )
            }
            items(entries, key = { it.url }) { entry ->
                val installed = entry.url in installedUrls
                ListItem(
                    modifier = Modifier.clickable { onOpen(entry) },
                    leadingContent = { LetterIcon(entry.name) },
                    headlineContent = { Text(entry.name) },
                    supportingContent = { Text(WebAppUrls.displayHost(entry.url)) },
                    trailingContent = {
                        if (installed) {
                            TextButton(onClick = { onOpen(entry) }) { Text(stringResource(R.string.webapp_open)) }
                        } else {
                            TextButton(onClick = { onAdd(entry) }) { Text(stringResource(R.string.webapp_add)) }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun InstalledAppIcon(app: WebApp) {
    val context = LocalContext.current
    // Keyed on lastOpenedAt so a favicon fetched during the last visit shows up on return
    val favicon by produceState<Bitmap?>(null, app.id, app.lastOpenedAt) {
        value = loadIcon(context, app.id)
    }
    val bitmap = favicon
    if (bitmap != null) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(26.dp))
        }
    } else {
        LetterIcon(app.name)
    }
}

private suspend fun loadIcon(context: Context, id: Long): Bitmap? =
    withContext(Dispatchers.IO) { WebAppIcons.load(context, id) }

@Composable
private fun LetterIcon(name: String) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(WebAppIcons.colorFor(name))),
        contentAlignment = Alignment.Center
    ) {
        Text(WebAppIcons.initial(name), color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun AddWebAppDialog(onDismiss: () -> Unit, onConfirm: (name: String, url: String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    val normalized = WebAppUrls.normalize(url)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.webapp_add_custom)) },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.webapp_field_url)) },
                    placeholder = { Text("example.com") },
                    singleLine = true,
                    isError = url.isNotBlank() && normalized == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.webapp_field_name)) },
                    placeholder = { normalized?.let { Text(WebAppUrls.displayHost(it)) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = normalized != null,
                onClick = {
                    val target = normalized ?: return@TextButton
                    onConfirm(name.trim().ifEmpty { WebAppUrls.displayHost(target) }, target)
                }
            ) { Text(stringResource(R.string.webapp_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        }
    )
}
