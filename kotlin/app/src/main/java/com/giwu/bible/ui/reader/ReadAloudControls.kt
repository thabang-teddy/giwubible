package com.giwu.bible.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.giwu.bible.tts.TtsPlayback
import com.giwu.bible.tts.TtsStatus

/**
 * The read-aloud control.
 *
 * One button while idle; a transport bar — previous, play/pause, next, speed,
 * stop — once a chapter is being read.
 */
@Composable
fun ReadAloudControls(
    playback: TtsPlayback,
    rate: Float,
    onStart: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    onCycleRate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!playback.isActive) {
        FloatingActionButton(onClick = onStart, modifier = modifier) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = "Read this chapter aloud",
            )
        }
        return
    }

    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val isPaused = playback.status == TtsStatus.PAUSED

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarButton(
                icon = Icons.Rounded.SkipPrevious,
                description = "Previous verse",
                tint = onPrimary,
                onClick = onPrevious,
            )
            BarButton(
                icon = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                description = if (isPaused) "Resume reading" else "Pause reading",
                tint = onPrimary,
                size = 26.dp,
                onClick = onPlayPause,
            )
            BarButton(
                icon = Icons.Rounded.SkipNext,
                description = "Next verse",
                tint = onPrimary,
                onClick = onNext,
            )
            Text(
                text = "${formatRate(rate)}x",
                style = MaterialTheme.typography.labelSmall,
                color = onPrimary,
                modifier = Modifier
                    .clickable(onClick = onCycleRate)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            )
            BarButton(
                icon = Icons.Rounded.Stop,
                description = "Stop reading",
                tint = onPrimary,
                onClick = onStop,
            )
        }
    }
}

@Composable
private fun BarButton(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit,
    size: Dp = 20.dp,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(size),
        )
    }
}

/** 1.0 reads as "1x", 1.25 as "1.25x". */
internal fun formatRate(rate: Float): String =
    if (rate == rate.toInt().toFloat()) "${rate.toInt()}" else "$rate"
