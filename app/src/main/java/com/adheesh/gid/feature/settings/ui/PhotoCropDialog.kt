package com.adheesh.gid.feature.settings.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.adheesh.gid.R
import com.adheesh.gid.core.designsystem.GIDBackground
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.feature.settings.domain.CropSquare
import com.adheesh.gid.feature.settings.domain.PhotoCrop
import kotlin.math.roundToInt

// ── Preview and crop a new profile photo (SPEC §2.5) ─────────────────────────
// Nothing is saved until 'Use photo'. The circle shows exactly what the avatar
// will look like.
@Composable
fun PhotoCropDialog(
    uri       : Uri,
    load      : suspend (Uri) -> Bitmap?,
    onFailed  : () -> Unit,
    onCancel  : () -> Unit,
    onSave    : (Bitmap, CropSquare) -> Unit,
) {
    var image by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri) {
        val loaded = load(uri)
        if (loaded == null) onFailed() else image = loaded
    }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = GIDBackground, modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp),
            ) {
                Text(stringResource(R.string.photo_crop_title), style = MaterialTheme.typography.titleLarge, color = GIDText)
                Text(stringResource(R.string.photo_crop_hint), style = MaterialTheme.typography.bodyMedium, color = GIDMutedText)

                val bitmap = image
                if (bitmap == null) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GIDCyan) }
                    OutlinedButton(onClick = onCancel, shape = CircleShape, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text(stringResource(R.string.action_cancel), color = GIDText)
                    }
                } else {
                    CropArea(bitmap, onCancel, onSave)
                }
            }
        }
    }
}

@Composable
private fun CropArea(bitmap: Bitmap, onCancel: () -> Unit, onSave: (Bitmap, CropSquare) -> Unit) {
    val picture = remember(bitmap) { bitmap.asImageBitmap() }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var windowPx by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        windowPx = with(LocalDensity.current) { side.toPx() }
        Canvas(
            Modifier
                .size(side)
                .pointerInput(bitmap) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(1f, PhotoCrop.MAX_ZOOM)
                        val scale = PhotoCrop.scale(bitmap.width, bitmap.height, windowPx, zoom)
                        // Clamped every time, as zooming out can leave the photo off-centre.
                        offsetX = PhotoCrop.clampOffset(offsetX + pan.x, bitmap.width, scale, windowPx)
                        offsetY = PhotoCrop.clampOffset(offsetY + pan.y, bitmap.height, scale, windowPx)
                    }
                },
        ) {
            val window = size.minDimension
            val scale = PhotoCrop.scale(bitmap.width, bitmap.height, window, zoom)
            val width = bitmap.width * scale
            val height = bitmap.height * scale
            clipRect {
                drawImage(
                    image     = picture,
                    dstOffset = IntOffset(((window - width) / 2 + offsetX).roundToInt(), ((window - height) / 2 + offsetY).roundToInt()),
                    dstSize   = IntSize(width.roundToInt(), height.roundToInt()),
                )
            }
            // Darken everything outside the circle the avatar will show.
            val center = Offset(window / 2, window / 2)
            val outside = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size))
                addOval(androidx.compose.ui.geometry.Rect(center, window / 2))
            }
            drawPath(outside, GIDBackground.copy(alpha = 0.7f))
            drawCircle(GIDCyan, window / 2, center, style = Stroke(2.dp.toPx()))
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onCancel, shape = CircleShape, modifier = Modifier.weight(1f).height(52.dp)) {
            Text(stringResource(R.string.action_cancel), color = GIDText)
        }
        Button(
            onClick  = { onSave(bitmap, PhotoCrop.square(bitmap.width, bitmap.height, windowPx, zoom, offsetX, offsetY)) },
            shape    = CircleShape,
            colors   = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard),
            modifier = Modifier.weight(1f).height(52.dp),
        ) { Text(stringResource(R.string.photo_crop_use)) }
    }
}
