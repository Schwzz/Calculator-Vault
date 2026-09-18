package com.example.ui.vault

import android.media.MediaPlayer
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.VaultFileType
import com.example.model.VaultItem
import com.example.model.VaultNote
import com.example.ui.theme.LocalVaultCornerStyle
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultCardBackground
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun VaultTrashScreen(
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit
) {
    val trashItems by viewModel.trashItems.collectAsStateWithLifecycle()
    val trashNotes by viewModel.trashNotes.collectAsStateWithLifecycle()

    var showEmptyTrashConfirm by remember { mutableStateOf(false) }
    var itemToDeletePermanently by remember { mutableStateOf<VaultItem?>(null) }
    var noteToDeletePermanently by remember { mutableStateOf<VaultNote?>(null) }
    var itemToRestore by remember { mutableStateOf<VaultItem?>(null) }
    var noteToRestore by remember { mutableStateOf<VaultNote?>(null) }

    // Preview inspection states
    var previewPhoto by remember { mutableStateOf<VaultItem?>(null) }
    var previewMedia by remember { mutableStateOf<VaultItem?>(null) }
    var previewFile by remember { mutableStateOf<VaultItem?>(null) }
    var previewNote by remember { mutableStateOf<VaultNote?>(null) }

    val totalTrashed = trashItems.size + trashNotes.size
    val cornerStyle = LocalVaultCornerStyle.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBackground),
        containerColor = VaultBackground,
        topBar = {
            Surface(
                color = VaultCardBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                        Text(
                            text = "Trash Bin",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalTrashed deleted items",
                            color = VaultTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    if (totalTrashed > 0) {
                        TextButton(
                            onClick = { showEmptyTrashConfirm = true },
                            modifier = Modifier.testTag("empty_trash_button")
                        ) {
                            Text("Empty", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (totalTrashed == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(VaultCardBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = VaultTextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Trash Bin is empty",
                        color = VaultTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Deleted files and notes will be stored here",
                        color = VaultTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Trashed Notes
                items(trashNotes, key = { "note_${it.id}" }) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { previewNote = note },
                        shape = cornerStyle.cardShape,
                        colors = CardDefaults.cardColors(containerColor = VaultCardBackground),
                        border = BorderStroke(1.dp, VaultCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFA78BFA).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val snippet = note.content.lineSequence().firstOrNull { it.isNotBlank() } ?: "Empty note"
                                Text(
                                    text = snippet,
                                    color = VaultTextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Deleted ${formatDate(note.trashedAt ?: note.updatedAt)}",
                                    color = VaultTextSecondary.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { noteToRestore = note },
                                    modifier = Modifier.size(44.dp).testTag("trash_restore_note_${note.id}")
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(
                                    onClick = { noteToDeletePermanently = note },
                                    modifier = Modifier.size(44.dp).testTag("trash_delete_note_${note.id}")
                                ) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete Forever", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }

                // Trashed Files
                items(trashItems, key = { "item_${it.id}" }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                when (item.fileType) {
                                    VaultFileType.PHOTO -> previewPhoto = item
                                    VaultFileType.VIDEO, VaultFileType.AUDIO -> previewMedia = item
                                    VaultFileType.FILE -> previewFile = item
                                }
                            },
                        shape = cornerStyle.cardShape,
                        colors = CardDefaults.cardColors(containerColor = VaultCardBackground),
                        border = BorderStroke(1.dp, VaultCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrashItemThumbnail(item = item)

                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatFileSize(item.sizeBytes)} • ${item.fileType.name}",
                                    color = VaultTextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Deleted ${formatDate(item.trashedAt ?: item.createdAt)}",
                                    color = VaultTextSecondary.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { itemToRestore = item },
                                    modifier = Modifier.size(44.dp).testTag("trash_restore_item_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = "Restore ${item.name}",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { itemToDeletePermanently = item },
                                    modifier = Modifier.size(44.dp).testTag("trash_delete_permanently_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = "Delete Forever ${item.name}",
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Preview Dialogs
    previewPhoto?.let { photo ->
        TrashPhotoPreviewDialog(
            item = photo,
            onDismiss = { previewPhoto = null },
            onRestore = {
                itemToRestore = photo
                previewPhoto = null
            },
            onDeletePermanently = {
                itemToDeletePermanently = photo
                previewPhoto = null
            }
        )
    }

    previewMedia?.let { media ->
        TrashMediaPreviewDialog(
            item = media,
            onDismiss = { previewMedia = null },
            onRestore = {
                itemToRestore = media
                previewMedia = null
            },
            onDeletePermanently = {
                itemToDeletePermanently = media
                previewMedia = null
            }
        )
    }

    previewFile?.let { fileItem ->
        TrashFileDetailsDialog(
            item = fileItem,
            onDismiss = { previewFile = null },
            onRestore = {
                itemToRestore = fileItem
                previewFile = null
            },
            onDeletePermanently = {
                itemToDeletePermanently = fileItem
                previewFile = null
            }
        )
    }

    previewNote?.let { note ->
        TrashNotePreviewDialog(
            note = note,
            onDismiss = { previewNote = null },
            onRestore = {
                noteToRestore = note
                previewNote = null
            },
            onDeletePermanently = {
                noteToDeletePermanently = note
                previewNote = null
            }
        )
    }

    // Confirmation Dialogs
    if (showEmptyTrashConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirm = false },
            title = { Text("Empty Trash Bin?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "All items in the trash bin will be permanently deleted from disk and cannot be recovered.",
                    color = VaultTextSecondary
                )
            },
            shape = cornerStyle.dialogShape,
            containerColor = VaultCardBackground,
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.emptyTrash()
                        showEmptyTrashConfirm = false
                    },
                    shape = cornerStyle.buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete Permanently", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEmptyTrashConfirm = false },
                    shape = cornerStyle.buttonShape
                ) {
                    Text("Cancel", color = Color(0xFFA0A0A0))
                }
            }
        )
    }

    itemToDeletePermanently?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDeletePermanently = null },
            title = { Text("Delete Permanently?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to permanently delete \"${item.name}\"? This action is irreversible and the file will be deleted forever.",
                    color = VaultTextSecondary
                )
            },
            shape = cornerStyle.dialogShape,
            containerColor = VaultCardBackground,
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteItemPermanently(item)
                        itemToDeletePermanently = null
                    },
                    shape = cornerStyle.buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete Forever", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { itemToDeletePermanently = null },
                    shape = cornerStyle.buttonShape
                ) {
                    Text("Cancel", color = Color(0xFFA0A0A0))
                }
            }
        )
    }

    noteToDeletePermanently?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDeletePermanently = null },
            title = { Text("Delete Note Permanently?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to permanently delete \"${note.title}\"? This action cannot be undone.",
                    color = VaultTextSecondary
                )
            },
            shape = cornerStyle.dialogShape,
            containerColor = VaultCardBackground,
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNotePermanently(note.id)
                        noteToDeletePermanently = null
                    },
                    shape = cornerStyle.buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete Forever", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { noteToDeletePermanently = null },
                    shape = cornerStyle.buttonShape
                ) {
                    Text("Cancel", color = Color(0xFFA0A0A0))
                }
            }
        )
    }

    itemToRestore?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToRestore = null },
            title = { Text("Restore File to Vault?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Restore \"${item.name}\" back to the Vault? It will be recovered from the Trash Bin and placed back in your private Vault.",
                    color = VaultTextSecondary
                )
            },
            shape = cornerStyle.dialogShape,
            containerColor = VaultCardBackground,
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreItem(item)
                        itemToRestore = null
                    },
                    shape = cornerStyle.buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Restore", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { itemToRestore = null },
                    shape = cornerStyle.buttonShape
                ) {
                    Text("Cancel", color = Color(0xFFA0A0A0))
                }
            }
        )
    }

    noteToRestore?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToRestore = null },
            title = { Text("Restore Note to Vault?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Restore \"${note.title}\" back to the Vault? It will be recovered from the Trash Bin and placed back in your private notes.",
                    color = VaultTextSecondary
                )
            },
            shape = cornerStyle.dialogShape,
            containerColor = VaultCardBackground,
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreNote(note.id)
                        noteToRestore = null
                    },
                    shape = cornerStyle.buttonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Restore", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { noteToRestore = null },
                    shape = cornerStyle.buttonShape
                ) {
                    Text("Cancel", color = Color(0xFFA0A0A0))
                }
            }
        )
    }
}

@Composable
private fun TrashItemThumbnail(item: VaultItem) {
    val file = remember(item.storedPath) { File(item.storedPath) }

    when (item.fileType) {
        VaultFileType.PHOTO -> {
            if (file.exists() && file.length() > 0L) {
                AsyncImage(
                    model = file,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        VaultFileType.VIDEO -> {
            val videoThumb = rememberVideoThumbnail(item.storedPath)
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E2E)),
                contentAlignment = Alignment.Center
            ) {
                if (videoThumb != null) {
                    Image(
                        bitmap = videoThumb.asImageBitmap(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        VaultFileType.AUDIO -> {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        VaultFileType.FILE -> {
            val ext = item.name.substringAfterLast('.', "").uppercase().take(4)
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(22.dp)
                    )
                    if (ext.isNotEmpty()) {
                        Text(
                            text = ext,
                            color = Color(0xFF3B82F6),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrashPhotoPreviewDialog(
    item: VaultItem,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val cornerStyle = LocalVaultCornerStyle.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val file = remember(item.storedPath) { File(item.storedPath) }
                if (file.exists()) {
                    AsyncImage(
                        model = file,
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Top Header Bar
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(
                                text = item.name,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatFileSize(item.sizeBytes)} • Deleted ${formatDate(item.trashedAt ?: item.createdAt)}",
                                color = Color(0xFFA0A0A0),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Bottom Action Bar
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDeletePermanently,
                            shape = cornerStyle.buttonShape,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Forever")
                        }
                        Button(
                            onClick = onRestore,
                            shape = cornerStyle.buttonShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrashMediaPreviewDialog(
    item: VaultItem,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val cornerStyle = LocalVaultCornerStyle.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(if (item.durationMs > 0) item.durationMs else 1L) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(item.storedPath) {
        val player = MediaPlayer()
        try {
            player.setDataSource(item.storedPath)
            player.prepare()
            if (item.durationMs <= 0) {
                durationMs = player.duration.toLong().coerceAtLeast(1L)
            }
            player.setOnCompletionListener {
                isPlaying = false
                currentPosMs = 0L
            }
            mediaPlayer = player
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            player.release()
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    currentPosMs = mp.currentPosition.toLong()
                }
            }
            delay(250)
        }
    }

    Dialog(
        onDismissRequest = {
            mediaPlayer?.pause()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (item.fileType == VaultFileType.VIDEO) {
                    AndroidView(
                        factory = { ctx ->
                            SurfaceView(ctx).apply {
                                holder.addCallback(object : SurfaceHolder.Callback {
                                    override fun surfaceCreated(holder: SurfaceHolder) {
                                        mediaPlayer?.setDisplay(holder)
                                    }
                                    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, height: Int) {}
                                    override fun surfaceDestroyed(h: SurfaceHolder) {
                                        mediaPlayer?.setDisplay(null)
                                    }
                                })
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Top Header Bar
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            mediaPlayer?.pause()
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(
                                text = item.name,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatFileSize(item.sizeBytes)} • Deleted ${formatDate(item.trashedAt ?: item.createdAt)}",
                                color = Color(0xFFA0A0A0),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Controls & Action Bar
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDuration(currentPosMs),
                                color = Color(0xFFA0A0A0),
                                fontSize = 12.sp
                            )
                            Slider(
                                value = if (durationMs > 0) (currentPosMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                                onValueChange = { fraction ->
                                    val target = (fraction * durationMs).toLong()
                                    currentPosMs = target
                                    mediaPlayer?.seekTo(target.toInt())
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = formatDuration(durationMs),
                                color = Color(0xFFA0A0A0),
                                fontSize = 12.sp
                            )
                        }

                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            IconButton(
                                onClick = {
                                    val mp = mediaPlayer ?: return@IconButton
                                    if (mp.isPlaying) {
                                        mp.pause()
                                        isPlaying = false
                                    } else {
                                        mp.start()
                                        isPlaying = true
                                    }
                                },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    mediaPlayer?.pause()
                                    onDeletePermanently()
                                },
                                shape = cornerStyle.buttonShape,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Delete Forever")
                            }
                            Button(
                                onClick = {
                                    mediaPlayer?.pause()
                                    onRestore()
                                },
                                shape = cornerStyle.buttonShape,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restore")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrashFileDetailsDialog(
    item: VaultItem,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val cornerStyle = LocalVaultCornerStyle.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = cornerStyle.dialogShape,
        containerColor = VaultCardBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "File Details",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailRow("Name", item.name)
                DetailRow("Size", "${formatFileSize(item.sizeBytes)} (${item.sizeBytes} bytes)")
                DetailRow("Type", item.mimeType.ifBlank { item.fileType.name })
                DetailRow("Date Deleted", formatDate(item.trashedAt ?: item.createdAt))
                if (item.originalPath.isNotBlank()) {
                    DetailRow("Original Location", item.originalPath)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRestore,
                shape = cornerStyle.buttonShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Restore", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDeletePermanently,
                    shape = cornerStyle.buttonShape
                ) {
                    Text("Delete Forever", color = Color(0xFFEF4444))
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = onDismiss,
                    shape = cornerStyle.buttonShape
                ) {
                    Text("Close", color = Color(0xFFA0A0A0))
                }
            }
        }
    )
}

@Composable
fun TrashNotePreviewDialog(
    note: VaultNote,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val cornerStyle = LocalVaultCornerStyle.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = cornerStyle.dialogShape,
        containerColor = VaultCardBackground,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFA78BFA).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = note.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Deleted ${formatDate(note.trashedAt ?: note.updatedAt)} • ${note.content.length} characters",
                    color = VaultTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = note.content.ifBlank { "(Empty note)" },
                        color = VaultTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRestore,
                shape = cornerStyle.buttonShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Restore", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDeletePermanently,
                    shape = cornerStyle.buttonShape
                ) {
                    Text("Delete Forever", color = Color(0xFFEF4444))
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = onDismiss,
                    shape = cornerStyle.buttonShape
                ) {
                    Text("Close", color = Color(0xFFA0A0A0))
                }
            }
        }
    )
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = VaultTextSecondary, fontSize = 11.sp)
        Text(text = value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
