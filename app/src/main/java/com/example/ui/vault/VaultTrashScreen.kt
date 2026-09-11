package com.example.ui.vault

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.VaultFileType
import com.example.model.VaultItem
import com.example.model.VaultNote
import com.example.ui.theme.LocalVaultCornerStyle
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultCardBackground
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

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
                        modifier = Modifier.fillMaxWidth(),
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFA78BFA).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = Color(0xFFA78BFA))
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
                                Text("Note • ${note.content.length} chars", color = VaultTextSecondary, fontSize = 11.sp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.restoreNote(note.id) },
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
                        modifier = Modifier.fillMaxWidth(),
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (item.fileType == VaultFileType.PHOTO) Icons.Default.Image else Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444)
                                )
                            }
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
                                Text("${formatFileSize(item.sizeBytes)} • ${item.fileType.name}", color = VaultTextSecondary, fontSize = 11.sp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.restoreItem(item) },
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

    if (showEmptyTrashConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirm = false },
            title = { Text("Empty Trash Bin?", color = Color.White) },
            text = {
                Text(
                    "All items in the trash bin will be permanently deleted and cannot be recovered.",
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
}
