package com.virtualcam.ui

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.virtualcam.source.SourceType
import com.virtualcam.ui.theme.*

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val isRunning      by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val isStreaming    by viewModel.isStreaming.collectAsStateWithLifecycle()
    val activeSource   by viewModel.activeSource.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            TopBar(isStreaming)

            // ── Status card ───────────────────────────────────────────────────
            StatusCard(
                isRunning  = isRunning,
                isStreaming = isStreaming,
                onToggle   = { viewModel.toggleService() }
            )

            // ── Source selector ───────────────────────────────────────────────
            Text(
                text  = "Video Source",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
            SourceTabs(
                active    = activeSource,
                onSelect  = { viewModel.selectSource(it) }
            )

            // ── Source config card ────────────────────────────────────────────
            when (activeSource) {
                SourceType.IMAGE  -> ImageSourceCard(viewModel)
                SourceType.VIDEO  -> VideoSourceCard(viewModel)
                SourceType.SCREEN -> ScreenSourceCard(viewModel)
            }

            // ── Info footer ───────────────────────────────────────────────────
            InfoFooter()

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(isStreaming: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                text       = "VirtualCam",
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
                color      = TextPrimary,
                letterSpacing = (-0.5).sp
            )
            Text(
                text  = "Android 14+ virtual camera",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (isStreaming) LiveBadge()
    }
}

@Composable
private fun LiveBadge() {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(OrangeLive.copy(alpha = 0.15f))
            .border(1.dp, OrangeLive.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(OrangeLive.copy(alpha = pulse))
        )
        Text(
            text       = "LIVE",
            color      = OrangeLive,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}

// ── Status card ───────────────────────────────────────────────────────────────

@Composable
private fun StatusCard(
    isRunning: Boolean,
    isStreaming: Boolean,
    onToggle: () -> Unit
) {
    val accentColor = if (isRunning) CyanBright else TextDisabled
    val statusText  = when {
        isStreaming -> "Streaming to app"
        isRunning   -> "Ready — waiting for consumer"
        else        -> "Virtual camera is off"
    }

    Surface(
        shape  = RoundedCornerShape(16.dp),
        color  = BgSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Glowing status dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text       = if (isRunning) "Camera active" else "Camera off",
                        color      = accentColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp
                    )
                    Text(
                        text  = statusText,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                onClick  = onToggle,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFF1F2E4A) else CyanBright,
                    contentColor   = if (isRunning) OrangeLive else BgDeep
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = if (isRunning) "Stop virtual camera" else "Start virtual camera",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp
                )
            }
        }
    }
}

// ── Source tabs ───────────────────────────────────────────────────────────────

@Composable
private fun SourceTabs(active: SourceType, onSelect: (SourceType) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        SourceType.entries.forEach { type ->
            val selected = active == type
            SourceTab(
                label      = type.displayName,
                icon       = sourceIcon(type),
                selected   = selected,
                onClick    = { onSelect(type) },
                modifier   = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SourceTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg      = if (selected) CyanBright.copy(alpha = 0.15f) else BgSurface
    val border  = if (selected) CyanBright else DividerColor
    val content = if (selected) CyanBright else TextSecondary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = label, tint = content, modifier = Modifier.size(20.dp))
        Text(label, color = content, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun sourceIcon(type: SourceType) = when (type) {
    SourceType.IMAGE  -> Icons.Default.Image
    SourceType.VIDEO  -> Icons.Default.VideoFile
    SourceType.SCREEN -> Icons.Default.ScreenShare
}

// ── Image source card ─────────────────────────────────────────────────────────

@Composable
private fun ImageSourceCard(viewModel: MainViewModel) {
    val imageUri by viewModel.selectedImageUri.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.setImageUri(it) } }

    SourceCard(
        title       = "Image source",
        description = "Pick any JPEG, PNG, or WebP from your gallery."
    ) {
        if (imageUri != null) {
            SelectedFileRow(label = "Image selected ✓", uri = imageUri!!)
        }
        OutlinedButton(
            onClick  = { picker.launch("image/*") },
            shape    = RoundedCornerShape(10.dp),
            colors   = OutlinedButtonDefaults.outlinedButtonColors(contentColor = CyanBright),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (imageUri == null) "Pick image" else "Change image")
        }
    }
}

// ── Video source card ─────────────────────────────────────────────────────────

@Composable
private fun VideoSourceCard(viewModel: MainViewModel) {
    val videoUri    by viewModel.selectedVideoUri.collectAsStateWithLifecycle()
    val loopEnabled by viewModel.videoLoopEnabled.collectAsStateWithLifecycle()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.setVideoUri(it) } }

    SourceCard(
        title       = "Video file source",
        description = "Play back an MP4 or MKV from storage."
    ) {
        if (videoUri != null) {
            SelectedFileRow(label = "Video selected ✓", uri = videoUri!!)
        }
        OutlinedButton(
            onClick  = { picker.launch("video/*") },
            shape    = RoundedCornerShape(10.dp),
            colors   = OutlinedButtonDefaults.outlinedButtonColors(contentColor = CyanBright),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (videoUri == null) "Pick video" else "Change video")
        }

        Spacer(Modifier.height(4.dp))

        Row(
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text("Loop video", color = TextPrimary, fontSize = 14.sp)
                Text("Restart when it ends", color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked         = loopEnabled,
                onCheckedChange = { viewModel.setVideoLoop(it) },
                colors          = SwitchDefaults.colors(
                    checkedThumbColor       = BgDeep,
                    checkedTrackColor       = CyanBright,
                    uncheckedTrackColor     = DividerColor
                )
            )
        }
    }
}

// ── Screen capture card ───────────────────────────────────────────────────────

@Composable
private fun ScreenSourceCard(viewModel: MainViewModel) {
    val context           = LocalContext.current
    val hasPermission     by viewModel.hasScreenPermission.collectAsStateWithLifecycle()

    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            viewModel.onScreenPermissionGranted(result.resultCode, result.data!!)
        }
    }

    SourceCard(
        title       = "Screen share source",
        description = "Stream your device screen as the camera feed."
    ) {
        if (hasPermission) {
            InfoChip(text = "Screen permission granted ✓", color = CyanBright)
        } else {
            InfoChip(text = "Permission required", color = TextSecondary)
        }
        Spacer(Modifier.height(4.dp))
        OutlinedButton(
            onClick = {
                val pm = context.getSystemService(MediaProjectionManager::class.java)
                projectionLauncher.launch(pm.createScreenCaptureIntent())
            },
            shape    = RoundedCornerShape(10.dp),
            colors   = OutlinedButtonDefaults.outlinedButtonColors(contentColor = CyanBright),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ScreenShare, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (hasPermission) "Re-grant screen access" else "Grant screen access")
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
private fun SourceCard(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape  = RoundedCornerShape(16.dp),
        color  = BgSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(color = DividerColor)
            content()
        }
    }
}

@Composable
private fun SelectedFileRow(label: String, uri: Uri) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CyanBright.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = CyanBright,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = CyanBright, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                uri.lastPathSegment ?: "file",
                color    = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun InfoChip(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, fontSize = 12.sp)
    }
}

@Composable
private fun InfoFooter() {
    Surface(
        shape  = RoundedCornerShape(12.dp),
        color  = BgCard,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(14.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp).padding(top = 2.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text  = "Requires Android 14. After starting the virtual camera, open another app " +
                        "(e.g. Google Meet) and select \"VirtualCam\" in its camera picker.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp
            )
        }
    }
}
