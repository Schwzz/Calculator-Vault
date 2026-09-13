package com.example.ui.vault

import android.content.Intent
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.model.VaultFileType
import com.example.model.VaultItem
import com.example.ui.theme.LocalVaultCornerStyle
import com.example.ui.theme.VaultCardBackground
import com.example.ui.theme.VaultTextSecondary
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun VaultMediaPlayerDialog(
    item: VaultItem,
    viewModel: VaultViewModel? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val cornerStyle = LocalVaultCornerStyle.current

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(if (item.durationMs > 0) item.durationMs else 60_000L) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }

    var areControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Hold-to-seek state
    var isHoldSeeking by remember { mutableStateOf(false) }
    var holdSeekDeltaSec by remember { mutableLongStateOf(0L) }
    var holdSeekTargetMs by remember { mutableLongStateOf(0L) }

    // Audio & Action states
    var isMuted by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showUnhideConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var currentItemName by remember { mutableStateOf(item.name) }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var videoSurface by remember { mutableStateOf<Surface?>(null) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }

    // Back handler dismisses cleanly
    BackHandler {
        onDismiss()
    }

    // Auto-hide controls after 3 seconds of inactivity while playing
    LaunchedEffect(areControlsVisible, isPlaying, lastInteractionTime, isHoldSeeking) {
        if (areControlsVisible && isPlaying && !isHoldSeeking) {
            delay(3000)
            areControlsVisible = false
        }
    }

    // Initialize MediaPlayer
    DisposableEffect(item.storedPath) {
        val file = File(item.storedPath)
        val mp = MediaPlayer()
        mediaPlayer = mp

        try {
            if (file.exists()) {
                mp.setDataSource(file.absolutePath)
            } else {
                mp.setDataSource(context, Uri.parse(item.originalPath))
            }

            mp.setOnPreparedListener { preparedMp ->
                val duration = preparedMp.duration.toLong()
                if (duration > 0) {
                    totalDurationMs = duration
                }
                if (preparedMp.videoWidth > 0 && preparedMp.videoHeight > 0) {
                    videoWidth = preparedMp.videoWidth
                    videoHeight = preparedMp.videoHeight
                }
                preparedMp.start()
                isPlaying = true
            }

            mp.setOnVideoSizeChangedListener { _, width, height ->
                if (width > 0 && height > 0) {
                    videoWidth = width
                    videoHeight = height
                }
            }

            mp.setOnCompletionListener {
                isPlaying = false
                currentPositionMs = totalDurationMs
                areControlsVisible = true
            }

            mp.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        onDispose {
            try {
                mp.stop()
            } catch (_: Exception) {}
            try {
                mp.release()
            } catch (_: Exception) {}
            videoSurface?.release()
            videoSurface = null
            mediaPlayer = null
        }
    }

    // Sync position timer
    LaunchedEffect(isPlaying, isUserSeeking, isHoldSeeking) {
        while (isPlaying && !isUserSeeking && !isHoldSeeking) {
            mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        currentPositionMs = mp.currentPosition.toLong()
                    }
                } catch (_: Exception) {}
            }
            delay(250)
        }
    }

    // Isolated full-screen container with solid black background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_screen")
    ) {
        // Video / Media rendering layer with correct aspect ratio
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (item.fileType == VaultFileType.VIDEO) {
                val containerAspect = maxWidth / maxHeight
                val videoAspect = if (videoWidth > 0 && videoHeight > 0) {
                    videoWidth.toFloat() / videoHeight.toFloat()
                } else null

                val videoModifier = if (videoAspect != null) {
                    if (videoAspect > containerAspect) {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(videoAspect)
                    } else {
                        Modifier
                            .fillMaxHeight()
                            .aspectRatio(videoAspect)
                    }
                } else {
                    Modifier.fillMaxSize()
                }

                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                                    val s = Surface(surface)
                                    videoSurface = s
                                    try {
                                        mediaPlayer?.setSurface(s)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }

                                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                    try {
                                        mediaPlayer?.setSurface(null)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    videoSurface?.release()
                                    videoSurface = null
                                    return true
                                }

                                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                            }
                        }
                    },
                    modifier = videoModifier
                )
            } else {
                // Audio visual representation
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = "Audio file",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = currentItemName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Gesture Detection Overlay: Tap to toggle controls, Hold ~1.5s then drag to seek
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(totalDurationMs) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        val startX = down.position.x
                        val startY = down.position.y
                        var activeSeeking = false
                        var seekBaseMs = currentPositionMs
                        var pointerMovedExcessivelyBeforeHold = false
                        var currentDeltaSec = 0L

                        while (true) {
                            val event = awaitPointerEvent()
                            val currentPointer = event.changes.firstOrNull { it.id == down.id }

                            if (currentPointer == null || !currentPointer.pressed) {
                                if (activeSeeking) {
                                    val finalTarget = (seekBaseMs + currentDeltaSec * 1000L).coerceIn(0L, totalDurationMs)
                                    try {
                                        mediaPlayer?.seekTo(finalTarget.toInt())
                                        currentPositionMs = finalTarget
                                    } catch (_: Exception) {}
                                    isHoldSeeking = false
                                    holdSeekDeltaSec = 0L
                                    lastInteractionTime = System.currentTimeMillis()
                                } else if (!pointerMovedExcessivelyBeforeHold && (System.currentTimeMillis() - downTime) < 450) {
                                    areControlsVisible = !areControlsVisible
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                                break
                            }

                            val elapsedMs = System.currentTimeMillis() - downTime
                            val currentX = currentPointer.position.x
                            val currentY = currentPointer.position.y
                            val diffX = currentX - startX
                            val diffY = currentY - startY
                            val distance = kotlin.math.hypot(diffX, diffY)

                            if (!activeSeeking) {
                                if (distance > 24.dp.toPx() && elapsedMs < 1300) {
                                    pointerMovedExcessivelyBeforeHold = true
                                }
                                if (!pointerMovedExcessivelyBeforeHold && elapsedMs >= 1300) {
                                    activeSeeking = true
                                    isHoldSeeking = true
                                    seekBaseMs = currentPositionMs
                                    areControlsVisible = false
                                }
                            }

                            if (activeSeeking) {
                                currentPointer.consume()
                                currentDeltaSec = (diffX / 16f).toLong()
                                val targetMs = (seekBaseMs + currentDeltaSec * 1000L).coerceIn(0L, totalDurationMs)
                                holdSeekDeltaSec = currentDeltaSec
                                holdSeekTargetMs = targetMs
                            }
                        }
                    }
                }
        )

        // Hold-to-seek feedback badge in dead center of viewer (never blocked by controls)
        if (isHoldSeeking) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.88f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .testTag("hold_seek_feedback"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val arrow = if (holdSeekDeltaSec < 0) "↶ " else if (holdSeekDeltaSec > 0) "" else "⇄ "
                    val suffix = if (holdSeekDeltaSec > 0) " ↷" else ""
                    val sign = if (holdSeekDeltaSec > 0) "+" else ""

                    Text(
                        text = if (holdSeekDeltaSec == 0L) "Hold & Drag to Seek" else "$arrow$sign$holdSeekDeltaSec seconds$suffix",
                        color = if (holdSeekDeltaSec != 0L) MaterialTheme.colorScheme.primary else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${formatTime(holdSeekTargetMs)} / ${formatTime(totalDurationMs)}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Unified playback controls overlay (Fades in/out automatically)
        AnimatedVisibility(
            visible = areControlsVisible && !isHoldSeeking,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .testTag("player_exit_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close Viewer",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = currentItemName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Vault Video",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Center Play / Pause button
                IconButton(
                    onClick = {
                        isPlaying = !isPlaying
                        mediaPlayer?.let { mp ->
                            if (isPlaying) {
                                if (currentPositionMs >= totalDurationMs) {
                                    currentPositionMs = 0L
                                    mp.seekTo(0)
                                }
                                mp.start()
                            } else {
                                mp.pause()
                            }
                        }
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("player_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Bottom Panel: Timeline + Action Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.75f))
                ) {
                    // Timeline: 0:32 ─────────────── 2:15
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(if (isUserSeeking) (seekProgress * totalDurationMs).toLong() else currentPositionMs),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        val progressFraction = if (totalDurationMs > 0) {
                            (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        Slider(
                            value = if (isUserSeeking) seekProgress else progressFraction,
                            onValueChange = {
                                isUserSeeking = true
                                seekProgress = it
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            onValueChangeFinished = {
                                val newTarget = (seekProgress * totalDurationMs).toLong()
                                currentPositionMs = newTarget
                                try {
                                    mediaPlayer?.seekTo(newTarget.toInt())
                                } catch (_: Exception) {}
                                isUserSeeking = false
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color(0xFF4A4A4A)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .testTag("player_seek_bar")
                        )

                        Text(
                            text = formatTime(totalDurationMs),
                            color = VaultTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Bottom Action Bar: Share, Edit, Unhide, Delete, Volume/Mute
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Share
                        PlayerActionButton(
                            icon = Icons.Default.Share,
                            label = "Share",
                            testTag = "player_action_share",
                            onClick = {
                                val shareFile = File(item.storedPath)
                                if (shareFile.exists()) {
                                    try {
                                        val shareUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            shareFile
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = item.mimeType.ifEmpty { "video/*" }
                                            putExtra(Intent.EXTRA_STREAM, shareUri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        viewModel?.setAwaitingExternalActivity(true)
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        )

                        // 2. Edit (Rename)
                        PlayerActionButton(
                            icon = Icons.Default.Edit,
                            label = "Edit",
                            testTag = "player_action_edit",
                            onClick = {
                                showRenameDialog = true
                            }
                        )

                        // 3. Unhide
                        PlayerActionButton(
                            icon = Icons.Default.FileDownload,
                            label = "Unhide",
                            testTag = "player_action_unhide",
                            onClick = {
                                showUnhideConfirm = true
                            }
                        )

                        // 4. Delete
                        PlayerActionButton(
                            icon = Icons.Default.Delete,
                            label = "Delete",
                            tint = Color(0xFFEF4444),
                            testTag = "player_action_delete",
                            onClick = {
                                showDeleteConfirm = true
                            }
                        )

                        // 5. Volume / Mute
                        PlayerActionButton(
                            icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            label = if (isMuted) "Muted" else "Mute",
                            tint = if (isMuted) Color(0xFFEF4444) else Color.White,
                            testTag = "player_action_volume",
                            onClick = {
                                isMuted = !isMuted
                                val vol = if (isMuted) 0f else 1f
                                mediaPlayer?.setVolume(vol, vol)
                                lastInteractionTime = System.currentTimeMillis()
                            }
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialogs
    if (showUnhideConfirm) {
        VaultConfirmationDialog(
            title = "Unhide \"$currentItemName\"?",
            message = "This video will be restored to your public phone gallery and removed from the Vault.",
            confirmText = "Unhide",
            isDestructive = false,
            confirmTestTag = "confirm_unhide_video",
            cancelTestTag = "cancel_unhide_video",
            onConfirm = {
                showUnhideConfirm = false
                viewModel?.unhideSingleItem(context, item) { success ->
                    if (success) {
                        onDismiss()
                    }
                }
            },
            onDismiss = {
                showUnhideConfirm = false
            }
        )
    }

    if (showDeleteConfirm) {
        VaultConfirmationDialog(
            title = "Move to Trash?",
            message = "Move \"$currentItemName\" to the Trash Bin? You can restore it later.",
            confirmText = "Delete",
            isDestructive = true,
            confirmTestTag = "confirm_delete_video",
            cancelTestTag = "cancel_delete_video",
            onConfirm = {
                showDeleteConfirm = false
                viewModel?.moveItemToTrash(item)
                onDismiss()
            },
            onDismiss = {
                showDeleteConfirm = false
            }
        )
    }

    if (showRenameDialog) {
        var newNameText by remember { mutableStateOf(currentItemName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename File", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                OutlinedTextField(
                    value = newNameText,
                    onValueChange = { newNameText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF4A4A4A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("rename_input_field")
                )
            },
            shape = cornerStyle.dialogShape,
            containerColor = VaultCardBackground,
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = newNameText.trim()
                        if (trimmed.isNotEmpty()) {
                            currentItemName = trimmed
                            viewModel?.renameItem(item, trimmed)
                        }
                        showRenameDialog = false
                    },
                    modifier = Modifier.testTag("confirm_rename_button")
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRenameDialog = false },
                    modifier = Modifier.testTag("cancel_rename_button")
                ) {
                    Text("Cancel", color = Color(0xFFA0A0A0))
                }
            }
        )
    }
}

@Composable
private fun PlayerActionButton(
    icon: ImageVector,
    label: String,
    tint: Color = Color.White,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}
