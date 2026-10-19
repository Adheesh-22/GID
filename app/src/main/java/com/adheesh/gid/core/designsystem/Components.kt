package com.adheesh.gid.core.designsystem

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.toColorInt
import com.adheesh.gid.R
import com.adheesh.gid.core.database.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// ── Shared building blocks used by several screens ───────────────────────────
// Everything here reads colors, type and shapes from the design tokens only.

/** Screen title: Space Grotesk, uppercase (SPEC §5). */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text     = text.uppercase(),
        style    = MaterialTheme.typography.displayLarge,
        color    = GIDText,
        modifier = modifier,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, color: Color = GIDText) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = color, modifier = modifier)
}

/** Rounded 28dp card. Colored cards use near-black text (SPEC §5). */
@Composable
fun GIDCard(
    modifier : Modifier = Modifier,
    color    : Color = GIDSurface,
    onClick  : (() -> Unit)? = null,
    content  : @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color    = color,
        shape    = MaterialTheme.shapes.medium,
        modifier = modifier.then(if (onClick != null) Modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick) else Modifier),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

/**
 * Floating dialog with a back arrow top-left (SPEC §1). Used for AlarmPopup,
 * TimetablePopup and the History day details.
 */
@Composable
fun GIDDialog(
    title     : String,
    onDismiss : () -> Unit,
    modifier  : Modifier = Modifier,
    actions   : @Composable RowScope.() -> Unit = {},
    content   : @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color    = GIDSurface,
            shape    = MaterialTheme.shapes.medium,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            Column(Modifier.padding(bottom = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 4.dp),
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = GIDText)
                    }
                    Text(
                        title,
                        style    = MaterialTheme.typography.titleLarge,
                        color    = GIDText,
                        modifier = Modifier.weight(1f),
                    )
                    actions()
                }
                Column(Modifier.padding(horizontal = 16.dp), content = content)
            }
        }
    }
}

/** Text field styled with the GID tokens, with an inline error under it. */
@Composable
fun GIDTextField(
    value                : String,
    onValueChange        : (String) -> Unit,
    label                : String,
    modifier             : Modifier = Modifier,
    error                : String? = null,
    singleLine           : Boolean = true,
    readOnly             : Boolean = false,
    enabled              : Boolean = true,
    visualTransformation : VisualTransformation = VisualTransformation.None,
    keyboardOptions      : KeyboardOptions = KeyboardOptions.Default,
    keyboardActions      : KeyboardActions = KeyboardActions.Default,
    leadingIcon          : (@Composable () -> Unit)? = null,
    trailingIcon         : (@Composable () -> Unit)? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value                = value,
            onValueChange        = onValueChange,
            label                = { Text(label) },
            singleLine           = singleLine,
            readOnly             = readOnly,
            enabled              = enabled,
            isError              = error != null,
            visualTransformation = visualTransformation,
            keyboardOptions      = keyboardOptions,
            keyboardActions      = keyboardActions,
            leadingIcon          = leadingIcon,
            trailingIcon         = trailingIcon,
            modifier             = Modifier.fillMaxWidth(),
            textStyle            = MaterialTheme.typography.bodyLarge,
            colors               = OutlinedTextFieldDefaults.colors(
                focusedBorderColor    = GIDCyan,
                unfocusedBorderColor  = GIDSurfaceHigh,
                disabledBorderColor   = GIDSurfaceHigh,
                errorBorderColor      = Error,
                focusedLabelColor     = GIDCyan,
                unfocusedLabelColor   = GIDMutedText,
                disabledLabelColor    = GIDMutedText,
                cursorColor           = GIDCyan,
                focusedTextColor      = GIDText,
                unfocusedTextColor    = GIDText,
                disabledTextColor     = GIDMutedText,
            ),
            shape = MaterialTheme.shapes.small,
        )
        if (error != null) {
            Text(error, color = Error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** Circular profile picture, or the first letter of the name when there is none. */
@Composable
fun ProfileAvatar(
    photoPath : String?,
    name      : String?,
    size      : Dp,
    modifier  : Modifier = Modifier,
    onClick   : (() -> Unit)? = null,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, photoPath) {
        value = photoPath?.let { loadBitmap(it) }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(GIDSurfaceHigh)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap             = image,
                contentDescription = stringResource(R.string.cd_profile_photo),
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.size(size),
            )
        } else {
            Text(
                text  = name?.trim()?.firstOrNull()?.uppercase() ?: "?",
                style = if (size >= 64.dp) MaterialTheme.typography.displayMedium else MaterialTheme.typography.titleLarge,
                color = GIDCyan,
            )
        }
    }
}

/** Decodes a downscaled copy so a 12 MP photo doesn't use 50 MB of memory. */
private suspend fun loadBitmap(path: String): ImageBitmap? = withContext(Dispatchers.IO) {
    val file = File(path)
    if (!file.exists()) return@withContext null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= 512 && bounds.outHeight / (sample * 2) >= 512) sample *= 2
    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
}

fun Priority.color(): Color = when (this) {
    Priority.HIGH   -> GIDCoral
    Priority.MEDIUM -> GIDMustard
    Priority.LOW    -> GIDPeriwinkle
}

@Composable
fun Priority.label(): String = stringResource(
    when (this) {
        Priority.HIGH   -> R.string.priority_high
        Priority.MEDIUM -> R.string.priority_medium
        Priority.LOW    -> R.string.priority_low
    }
)

@Composable
fun Dot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

/** Activity types store their color as "#RRGGBB" (data, not a screen constant). */
fun colorFromHex(hex: String, fallback: Color = GIDMutedText): Color =
    runCatching { Color(hex.toColorInt()) }.getOrDefault(fallback)

/** The palette offered when picking an activity-type color. */
val ActivityTypePalette: List<Pair<String, Color>> = listOf(
    "#6E6BF2" to Periwinkle,
    "#F2675A" to Coral,
    "#F5C842" to Mustard,
    "#7FE3E6" to Cyan,
    "#5ED393" to HolidayGreen,
    "#C6DAD0" to Sage,
    "#9B9BA1" to TextMuted,
)

/** Empty-state line used inside cards and lists. */
@Composable
fun EmptyText(text: String, modifier: Modifier = Modifier, color: Color = GIDMutedText) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = color, modifier = modifier.heightIn(min = 24.dp))
}
