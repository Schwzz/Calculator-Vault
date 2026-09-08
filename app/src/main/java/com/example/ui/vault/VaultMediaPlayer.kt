package com.example.ui.vault

import android.media.MediaPlayer
import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.VaultFileType
import com.example.model.VaultItem
import com.example.ui.theme.VaultTextSecondary
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun VaultMediaPlayerDialog(
    item: VaultItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(if (item.durationMs > 0) item.durationMs else 90_000L) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }

    var areControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var gestureIndicatorText by remember { mutableStateOf<String?>(null) }
    var dragStartSeekMs by remember { mutableLongStateOf(0L) }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var surfaceHolder by remember { mutableStateOf<SurfaceHolder?>(null) }

    // Auto-hide controls after 3 seconds of user inactivity
    LaunchedEffect(areControlsVisible, isPlaying, lastInteractionTime) {
        if (areControlsVisible && isPlaying) {
            delay(3000)
            areControlsVisible = false
        }
    }

    // Indicator auto-dismiss
    LaunchedEffect(gestureIndicatorText) {
        if (gestureIndicatorText != null) {
            delay(900)
            gestureIndicatorText = null
        }
    }

    DisposableEffect(item) {
        val file = File(item.storedPath)
        val player = MediaPlayer()
        try {
            if (file.exists() && file.length() > 5000) {
                player.setDataSource(context, Uri.fromFile(file))
                player.prepareAsync()
                player.setOnPreparedListener { mp ->
                    if (surfaceHolder != null && item.fileType == VaultFileType.VIDEO) {
                        mp.setDisplay(surfaceHolder)
                    }
                    totalDurationMs = mp.duration.toLong().coerceAtLeast(1000L)
                    mp.start()
                    isPlaying = true
                }
                player.setOnCompletionListener {
                    isPlaying = false
                    currentPositionMs = totalDurationMs
                    areControlsVisible = true
                }
            } else {
                isPlaying = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isPlaying = true
        }
        mediaPlayer = player

        onDispose {
            try {
                player.stop()
                player.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Playback progression ticker
    LaunchedEffect(isPlaying, isUserSeeking) {
        while (isPlaying && !isUserSeeking) {
            delay(250)
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    currentPositionMs = mp.currentPosition.toLong()
                } else {
                    currentPositionMs = (currentPositionMs + 250).coerceAtMost(totalDurationMs)
                    if (currentPositionMs >= totalDurationMs) {
                        isPlaying = false
                        areControlsVisible = true
                    }
                }
            } ?: run {
                currentPositionMs = (currentPositionMs + 250).coerceAtMost(totalDurationMs)
                if (currentPositionMs >= totalDurationMs) {
                    isPlaying = false
                    areControlsVisible = true
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                areControlsVisible = !areControlsVisible
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            onDoubleTap = { offset ->
                                val isLeftSide = offset.x < size.width / 2
                                val skipAmountMs = 10_000L
                                if (isLeftSide) {
                                    val target = (currentPositionMs - skipAmountMs).coerceAtLeast(0L)
                                    currentPositionMs = target
                                    mediaPlayer?.seekTo(target.toInt())
                                    gestureIndicatorText = "-10s"
                                } else {
                                    val target = (currentPositionMs + skipAmountMs).coerceAtMost(totalDurationMs)
                                    currentPositionMs = target
                                    mediaPlayer?.seekTo(target.toInt())
                                    gestureIndicatorText = "+10s"
                                }
                                areControlsVisible = true
                                lastInteractionTime = System.currentTimeMillis()
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragStartSeekMs = currentPositionMs
                                areControlsVisible = true
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val deltaMs = (dragAmount * 80L).toLong()
                                dragStartSeekMs = (dragStartSeekMs + deltaMs).coerceIn(0L, totalDurationMs)
                                currentPositionMs = dragStartSeekMs
                                val sign = if (deltaMs >= 0) "▶▶" else "◀◀"
                                gestureIndicatorText = "$sign ${formatTime(dragStartSeekMs)} / ${formatTime(totalDurationMs)}"
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            onDragEnd = {
                                mediaPlayer?.seekTo(currentPositionMs.toInt())
                                gestureIndicatorText = null
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            onDragCancel = {
                                gestureIndicatorText = null
                                lastInteractionTime = System.currentTimeMillis()
                            }
                        )
                    }
            ) {
                // Video Surface
                if (item.fileType == VaultFileType.VIDEO) {
                    AndroidView(
                        factory = { ctx ->
                            SurfaceView(ctx).apply {
                                holder.addCallback(object : SurfaceHolder.Callback {
                                    override fun surfaceCreated(holder: SurfaceHolder) {
                                        surfaceHolder = holder
                                        try {
                                            mediaPlayer?.setDisplay(holder)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                                        surfaceHolder = null
                                    }
                                })
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Audio Waveform Graphic Placeholder
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Private Audio Playback",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Gesture Feedback Center Indicator (-10s, +10s, Drag Seek)
                AnimatedVisibility(
                    visible = gestureIndicatorText != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = gestureIndicatorText ?: "",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Controls Overlay with Auto-Hide Fade
                AnimatedVisibility(
                    visible = areControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Bar: Exit button & Title
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .testTag("player_exit_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Exit Player", tint = Color.White)
                            }

                            Text(
                                text = item.name,
                                color = Color.White,
                                fontSize = 16.sp,
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
                                    text = "Protected",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Center Playback Controls: -10s, Play/Pause, +10s
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // -10s Skip
                            IconButton(
                                onClick = {
                                    val target = (currentPositionMs - 10_000L).coerceAtLeast(0L)
                                    currentPositionMs = target
                                    mediaPlayer?.let { mp ->
                                        if (mp.isPlaying) mp.seekTo(target.toInt())
                                    }
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .testTag("player_skip_back_10s")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Rewind 10 seconds",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(28.dp))

                            // Center Play / Pause
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
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .testTag("player_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(28.dp))

                            // +10s Skip
                            IconButton(
                                onClick = {
                                    val target = (currentPositionMs + 10_000L).coerceAtMost(totalDurationMs)
                                    currentPositionMs = target
                                    mediaPlayer?.let { mp ->
                                        if (mp.isPlaying) mp.seekTo(target.toInt())
                                    }
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .testTag("player_skip_forward_10s")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Forward 10 seconds",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Bottom Progress Seek Bar & Timer
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
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
                                    mediaPlayer?.let { mp ->
                                        mp.seekTo(newTarget.toInt())
                                    }
                                    isUserSeeking = false
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color(0xFF4A4A4A)
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("player_seek_bar")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTime(if (isUserSeeking) (seekProgress * totalDurationMs).toLong() else currentPositionMs),
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = formatTime(totalDurationMs),
                                    color = VaultTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

