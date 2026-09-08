package com.example.ui.vault

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.VaultFileType
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultCardBackground
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultSurfaceVariant
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import com.example.ui.theme.VaultTextTertiary
import kotlinx.coroutines.flow.collectLatest

data class DashboardItemData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val isPrimaryAccent: Boolean = false,
    val badgeCount: Int = 0
)

@Composable
fun VaultDashboardScreen(
    viewModel: VaultViewModel,
    onNavigate: (String) -> Unit,
    onLockApp: () -> Unit
) {
    val context = LocalContext.current
    val items by viewModel.activeItems.collectAsStateWithLifecycle()
    val notes by viewModel.activeNotes.collectAsStateWithLifecycle()
    val trashItems by viewModel.trashItems.collectAsStateWithLifecycle()
    val trashNotes by viewModel.trashNotes.collectAsStateWithLifecycle()
    val downloads by viewModel.allDownloads.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // File pickers
    val generalPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(uris, context, VaultFileType.FILE)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(uris, context, VaultFileType.PHOTO)
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(uris, context, VaultFileType.VIDEO)
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(uris, context, VaultFileType.AUDIO)
        }
    }

    var showFabMenu by remember { mutableStateOf(false) }

    val photoCount = items.count { it.fileType == VaultFileType.PHOTO }
    val videoCount = items.count { it.fileType == VaultFileType.VIDEO }
    val audioCount = items.count { it.fileType == VaultFileType.AUDIO }
    val fileCount = items.count { it.fileType == VaultFileType.FILE }
    val noteCount = notes.size
    val totalTrash = trashItems.size + trashNotes.size
    val totalDownloads = downloads.size

    val dashboardCards = listOf(
        DashboardItemData(
            title = "Photos",
            subtitle = if (photoCount == 1) "1 Item" else "$photoCount Items",
            icon = Icons.Default.Image,
            route = "vault_files/PHOTO",
            isPrimaryAccent = true,
            badgeCount = photoCount
        ),
        DashboardItemData(
            title = "Videos",
            subtitle = if (videoCount == 1) "1 Item" else "$videoCount Items",
            icon = Icons.Default.Videocam,
            route = "vault_files/VIDEO",
            isPrimaryAccent = true,
            badgeCount = videoCount
        ),
        DashboardItemData(
            title = "Audio",
            subtitle = if (audioCount == 1) "1 Item" else "$audioCount Items",
            icon = Icons.Default.Audiotrack,
            route = "vault_files/AUDIO",
            isPrimaryAccent = false,
            badgeCount = audioCount
        ),
        DashboardItemData(
            title = "Files",
            subtitle = if (fileCount == 1) "1 Item" else "$fileCount Items",
            icon = Icons.Default.Description,
            route = "vault_files/FILE",
            isPrimaryAccent = false,
            badgeCount = fileCount
        ),
        DashboardItemData(
            title = "Notes",
            subtitle = if (noteCount == 1) "1 Note" else "$noteCount Notes",
            icon = Icons.Default.EditNote,
            route = "vault_notes",
            isPrimaryAccent = false,
            badgeCount = noteCount
        ),
        DashboardItemData(
            title = "Browser",
            subtitle = "Incognito",
            icon = Icons.Default.Public,
            route = "vault_browser",
            isPrimaryAccent = false
        ),
        DashboardItemData(
            title = "Downloads",
            subtitle = if (totalDownloads == 1) "1 Item" else "$totalDownloads Items",
            icon = Icons.Default.Download,
            route = "vault_downloads",
            isPrimaryAccent = false,
            badgeCount = totalDownloads
        ),
        DashboardItemData(
            title = "Trash Bin",
            subtitle = if (totalTrash == 0) "Empty" else "$totalTrash Items",
            icon = Icons.Outlined.DeleteOutline,
            route = "vault_trash",
            isPrimaryAccent = false,
            badgeCount = totalTrash
        ),
        DashboardItemData(
            title = "Settings",
            subtitle = "Customization",
            icon = Icons.Default.Settings,
            route = "vault_settings",
            isPrimaryAccent = false
        )
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBackground),
        containerColor = VaultBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showFabMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp, end = 16.dp)
                        .size(56.dp)
                        .testTag("vault_fab_add")
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add files to Vault",
                        modifier = Modifier.size(28.dp)
                    )
                }

                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false },
                    modifier = Modifier
                        .background(VaultCardBackground)
                        .border(1.dp, VaultCardBorder, RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Import Photos", color = VaultTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            showFabMenu = false
                            photoPicker.launch("image/*")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Import Videos", color = VaultTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            showFabMenu = false
                            videoPicker.launch("video/*")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Import Audio", color = VaultTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Audiotrack, contentDescription = null, tint = VaultTextTertiary) },
                        onClick = {
                            showFabMenu = false
                            audioPicker.launch("audio/*")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Import General Files", color = VaultTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = VaultTextTertiary) },
                        onClick = {
                            showFabMenu = false
                            generalPicker.launch("*/*")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New Private Note", color = VaultTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            showFabMenu = false
                            onNavigate("vault_notes")
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
        ) {
            // Elegant Dark Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(VaultCardBackground)
                            .border(1.dp, VaultCardBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Calculator Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Calculator",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp,
                            color = VaultTextPrimary
                        )
                        Text(
                            text = "PROTECTED VAULT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { onNavigate("vault_browser") },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = VaultTextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onLockApp,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .testTag("lock_vault_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock and Return to Calculator",
                            tint = VaultTextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2-Column Grid Dashboard (Compact 58dp cards with 8dp spacing)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 72.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dashboardCards) { item ->
                    VaultDashboardCard(
                        item = item,
                        onClick = { onNavigate(item.route) }
                    )
                }
            }
        }
    }
}

@Composable
fun VaultDashboardCard(
    item: DashboardItemData,
    onClick: () -> Unit
) {
    val iconContainerColor = if (item.isPrimaryAccent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        VaultSurfaceVariant
    }

    val iconTintColor = if (item.isPrimaryAccent) {
        MaterialTheme.colorScheme.primary
    } else {
        VaultTextTertiary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = onClick)
            .testTag("dashboard_card_${item.title.lowercase().replace(' ', '_')}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = VaultCardBackground),
        border = BorderStroke(1.dp, VaultCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = iconTintColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = VaultTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.subtitle,
                    fontSize = 10.sp,
                    color = VaultTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
