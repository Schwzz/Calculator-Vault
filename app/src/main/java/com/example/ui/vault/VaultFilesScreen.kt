package com.example.ui.vault

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.collection.LruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.VaultFileType
import com.example.model.VaultItem
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultCardBackground
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VaultFilesScreen(
    categoryType: VaultFileType,
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val allItems by viewModel.activeItems.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val filteredItems = remember(allItems, categoryType) {
        allItems.filter { it.fileType == categoryType }
    }

    val categoryTitle = when (categoryType) {
        VaultFileType.PHOTO -> "Private Photos"
        VaultFileType.VIDEO -> "Private Videos"
        VaultFileType.AUDIO -> "Private Audio"
        VaultFileType.FILE -> "Private Files"
    }

    val mimeType = when (categoryType) {
        VaultFileType.PHOTO -> "image/*"
        VaultFileType.VIDEO -> "video/*"
        VaultFileType.AUDIO -> "audio/*"
        VaultFileType.FILE -> "*/*"
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(uris, context, categoryType)
        }
    }

    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }
    var selectedFileDetails by remember { mutableStateOf<VaultItem?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBackground),
        containerColor = VaultBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    if (uiState.isMultiSelectMode) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Selection", tint = Color.White)
                        }
                        Text(
                            text = "${uiState.selectedItemIds.size} Selected",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )
                        IconButton(onClick = {
                            if (uiState.selectedItemIds.size == filteredItems.size) {
                                viewModel.clearSelection()
                            } else {
                                viewModel.selectAll(filteredItems.map { it.id })
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                            Text(
                                text = categoryTitle,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${filteredItems.size} hidden items",
                                color = VaultTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (uiState.isMultiSelectMode) {
                Surface(
                    color = VaultCardBackground,
                    border = BorderStroke(1.dp, VaultCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Unhide Action
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.unhideSelected(context) }
                                .padding(8.dp)
                                .testTag("action_unhide_selected")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Unhide",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Unhide",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Delete (Move to Trash) Action
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.moveSelectedToTrash() }
                                .padding(8.dp)
                                .testTag("action_delete_selected")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Trash",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!uiState.isMultiSelectMode) {
                FloatingActionButton(
                    onClick = { filePicker.launch(mimeType) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp, end = 16.dp)
                        .testTag("fab_import_files")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Files", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { innerPadding ->
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(VaultCardBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (categoryType) {
                                VaultFileType.PHOTO -> Icons.Default.Image
                                VaultFileType.VIDEO -> Icons.Default.Videocam
                                VaultFileType.AUDIO -> Icons.Default.Audiotrack
                                VaultFileType.FILE -> Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = VaultTextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No $categoryTitle yet",
                        color = VaultTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the '+' button below to hide items",
                        color = VaultTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (categoryType == VaultFileType.PHOTO || categoryType == VaultFileType.VIDEO) 3 else 1),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentPadding = PaddingValues(bottom = 88.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    val isSelected = uiState.selectedItemIds.contains(item.id)

                    VaultItemCard(
                        item = item,
                        isSelected = isSelected,
                        isMultiSelectMode = uiState.isMultiSelectMode,
                        onClick = {
                            if (uiState.isMultiSelectMode) {
                                viewModel.toggleSelectItem(item.id)
                            } else {
                                when (item.fileType) {
                                    VaultFileType.VIDEO, VaultFileType.AUDIO -> {
                                        viewModel.playMedia(item)
                                    }
                                    VaultFileType.PHOTO -> {
                                        val photoList = filteredItems.filter { it.fileType == VaultFileType.PHOTO }
                                        val idx = photoList.indexOf(item)
                                        selectedPhotoIndex = if (idx >= 0) idx else 0
                                    }
                                    VaultFileType.FILE -> {
                                        selectedFileDetails = item
                                    }
                                }
                            }
                        },
                        onLongClick = {
                            viewModel.toggleSelectItem(item.id)
                        }
                    )
                }
            }
        }
    }

    // Full-Screen Photo Viewer with HorizontalPager & Gestures
    selectedPhotoIndex?.let { initialIndex ->
        val photoList = filteredItems.filter { it.fileType == VaultFileType.PHOTO }
        if (photoList.isNotEmpty()) {
            FullScreenPhotoViewer(
                photos = photoList,
                initialIndex = initialIndex,
                onDismiss = { selectedPhotoIndex = null }
            )
        }
    }

    // File Details Dialog
    selectedFileDetails?.let { fileItem ->
        Dialog(onDismissRequest = { selectedFileDetails = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                color = VaultCardBackground,
                border = BorderStroke(1.dp, VaultCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "File Details",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        IconButton(onClick = { selectedFileDetails = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("File Name: ${fileItem.name}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Size: ${formatFileSize(fileItem.sizeBytes)}", color = VaultTextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Type: ${fileItem.fileType.name}", color = VaultTextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Added: ${formatDate(fileItem.createdAt)}", color = VaultTextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Status: Encrypted in Private Sandbox", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            viewModel.unhideSelected(context)
                            selectedFileDetails = null
                        }) {
                            Text("Unhide to Storage", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VaultItemCard(
    item: VaultItem,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isMediaGrid = item.fileType == VaultFileType.PHOTO || item.fileType == VaultFileType.VIDEO

    if (isMediaGrid) {
        Card(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else VaultCardBorder,
                    shape = RoundedCornerShape(10.dp)
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .testTag("vault_item_${item.id}"),
            colors = CardDefaults.cardColors(containerColor = VaultCardBackground)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val file = File(item.storedPath)
                if (item.fileType == VaultFileType.PHOTO && file.exists()) {
                    AsyncImage(
                        model = file,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (item.fileType == VaultFileType.VIDEO) {
                    val videoThumb = rememberVideoThumbnail(item.storedPath)
                    if (videoThumb != null) {
                        Image(
                            bitmap = videoThumb.asImageBitmap(),
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(VaultCardBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(VaultCardBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Video play overlay icon
                if (item.fileType == VaultFileType.VIDEO) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Selection checkmark badge
                if (isMultiSelectMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f)
                            )
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Bottom gradient label
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.name,
                        color = Color.White,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    } else {
        // List style card for Audio / Files
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else VaultCardBorder,
                    shape = RoundedCornerShape(10.dp)
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .testTag("vault_item_${item.id}"),
            colors = CardDefaults.cardColors(containerColor = VaultCardBackground)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (item.fileType == VaultFileType.AUDIO) Color(0xFFFBBF24).copy(alpha = 0.15f)
                            else Color(0xFF34D399).copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.fileType == VaultFileType.AUDIO) Icons.Default.Audiotrack else Icons.Default.Description,
                        contentDescription = null,
                        tint = if (item.fileType == VaultFileType.AUDIO) Color(0xFFFBBF24) else Color(0xFF34D399),
                        modifier = Modifier.size(24.dp)
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
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${formatFileSize(item.sizeBytes)} • ${formatDate(item.createdAt)}",
                        color = VaultTextSecondary,
                        fontSize = 11.sp
                    )
                }

                if (isMultiSelectMode) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .border(1.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else VaultCardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> "${"%.1f".format(bytes / 1_000_000_000.0)} GB"
        bytes >= 1_000_000 -> "${"%.1f".format(bytes / 1_000_000.0)} MB"
        bytes >= 1_000 -> "${"%.1f".format(bytes / 1_000.0)} KB"
        else -> "$bytes B"
    }
}

fun formatDate(timeMillis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}

object VideoThumbnailCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024)
    private val lruCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.byteCount / 1024).coerceAtLeast(1)
        }
    }

    fun get(path: String): Bitmap? = lruCache.get(path)
    fun put(path: String, bitmap: Bitmap) {
        lruCache.put(path, bitmap)
    }
}

@Composable
fun rememberVideoThumbnail(videoPath: String): Bitmap? {
    var bitmap by remember(videoPath) { mutableStateOf<Bitmap?>(VideoThumbnailCache.get(videoPath)) }
    LaunchedEffect(videoPath) {
        if (bitmap == null) {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(videoPath)
                    if (file.exists() && file.length() > 0) {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(file.absolutePath)
                        val frame = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            ?: retriever.frameAtTime
                        retriever.release()
                        if (frame != null) {
                            VideoThumbnailCache.put(videoPath, frame)
                            withContext(Dispatchers.Main) {
                                bitmap = frame
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }
    return bitmap
}

@Composable
fun FullScreenPhotoViewer(
    photos: List<VaultItem>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    if (photos.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, photos.size - 1),
        pageCount = { photos.size }
    )
    var areControlsVisible by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val photo = photos[page]
                    var scale by remember { mutableFloatStateOf(1f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        areControlsVisible = !areControlsVisible
                                    },
                                    onDoubleTap = {
                                        if (scale > 1.2f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.5f
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                                    scale = newScale
                                    if (newScale > 1f) {
                                        val maxX = 1200f * (newScale - 1f)
                                        val maxY = 1200f * (newScale - 1f)
                                        offset = Offset(
                                            x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                            y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                        )
                                    } else {
                                        offset = Offset.Zero
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val file = File(photo.storedPath)
                        if (file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = photo.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                            )
                        } else {
                            Text("Image not found", color = Color.Gray)
                        }
                    }
                }

                // Controls Overlay
                AnimatedVisibility(
                    visible = areControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.65f))
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }

                            val currentPhoto = photos.getOrNull(pagerState.currentPage)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = currentPhoto?.name ?: "",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${pagerState.currentPage + 1} / ${photos.size}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.size(48.dp))
                        }

                        // Bottom Bar
                        val currentPhoto = photos.getOrNull(pagerState.currentPage)
                        if (currentPhoto != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .navigationBarsPadding()
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatFileSize(currentPhoto.sizeBytes),
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = formatDate(currentPhoto.createdAt),
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
