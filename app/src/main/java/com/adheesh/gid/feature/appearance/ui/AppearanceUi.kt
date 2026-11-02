package com.adheesh.gid.feature.appearance.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adheesh.gid.R
import com.adheesh.gid.core.designsystem.DmSansFamily
import com.adheesh.gid.core.designsystem.GIDBackground
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDHolidayGreen
import com.adheesh.gid.core.designsystem.GIDMustard
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDPeriwinkle
import com.adheesh.gid.core.designsystem.GIDSurface
import com.adheesh.gid.core.designsystem.GIDSurfaceHigh
import com.adheesh.gid.core.designsystem.GIDText
import com.adheesh.gid.core.designsystem.GIDTextField
import com.adheesh.gid.core.designsystem.OnCard
import com.adheesh.gid.core.designsystem.Sage
import com.adheesh.gid.core.designsystem.SpaceGroteskFamily
import com.adheesh.gid.feature.appearance.domain.Appearance
import com.adheesh.gid.feature.appearance.domain.NameColor
import com.adheesh.gid.feature.appearance.domain.NameFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// ── Fonts and colours for each option (tokens only, RULES §5) ────────────────

val NameFont.family: FontFamily get() = when (this) {
    NameFont.GID     -> SpaceGroteskFamily
    NameFont.DM_SANS -> DmSansFamily
    NameFont.SERIF   -> FontFamily.Serif
    NameFont.MONO    -> FontFamily.Monospace
    NameFont.SCRIPT  -> FontFamily.Cursive
    NameFont.LIGHT   -> FontFamily.SansSerif
}

val NameFont.weight: FontWeight get() = when (this) {
    NameFont.GID     -> FontWeight.ExtraBold
    NameFont.DM_SANS -> FontWeight.Medium
    NameFont.SERIF   -> FontWeight.Bold
    NameFont.MONO    -> FontWeight.Bold
    NameFont.SCRIPT  -> FontWeight.Normal
    NameFont.LIGHT   -> FontWeight.Light
}

private val NameFont.label: Int get() = when (this) {
    NameFont.GID     -> R.string.font_gid
    NameFont.DM_SANS -> R.string.font_dm_sans
    NameFont.SERIF   -> R.string.font_serif
    NameFont.MONO    -> R.string.font_mono
    NameFont.SCRIPT  -> R.string.font_script
    NameFont.LIGHT   -> R.string.font_light
}

val NameColor.color: Color get() = when (this) {
    NameColor.TEXT       -> GIDText
    NameColor.CYAN       -> GIDCyan
    NameColor.CORAL      -> GIDCoral
    NameColor.MUSTARD    -> GIDMustard
    NameColor.PERIWINKLE -> GIDPeriwinkle
    NameColor.SAGE       -> Sage
    NameColor.GREEN      -> GIDHolidayGreen
}

private val NameColor.label: Int get() = when (this) {
    NameColor.TEXT       -> R.string.color_text
    NameColor.CYAN       -> R.string.color_cyan
    NameColor.CORAL      -> R.string.color_coral
    NameColor.MUSTARD    -> R.string.color_mustard
    NameColor.PERIWINKLE -> R.string.color_periwinkle
    NameColor.SAGE       -> R.string.color_sage
    NameColor.GREEN      -> R.string.color_green
}

/** 'Hello,' and the name on two lines, in the chosen font and colour. */
@Composable
fun Greeting(
    name       : String?,
    appearance : Appearance,
    fontSize   : TextUnit,
    lineHeight : TextUnit,
    modifier   : Modifier = Modifier,
) {
    Text(
        text       = name?.let { stringResource(R.string.home_hello_name, it) } ?: stringResource(R.string.home_hello_there),
        style      = TextStyle(
            fontFamily    = appearance.font.family,
            fontWeight    = appearance.font.weight,
            fontSize      = fontSize,
            lineHeight    = lineHeight,
            letterSpacing = if (appearance.font == NameFont.GID) (-0.5).sp else 0.sp,
        ),
        color      = appearance.color.color,
        maxLines   = 2,
        overflow   = TextOverflow.Ellipsis,
        modifier   = modifier,
    )
}

// ── Background photo behind the four main tabs ───────────────────────────────

/** The blurred photo with its dim layer; fades out where it must not show. */
@Composable
fun AppBackdrop(appearance: Appearance, visible: Boolean, modifier: Modifier = Modifier) {
    val path = appearance.backgroundPath
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        value = path?.let {
            withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull() }
        }
    }
    val alpha by animateFloatAsState(
        targetValue   = if (visible && bitmap != null) 1f else 0f,
        animationSpec = tween(250),
        label         = "backdrop",
    )
    val image = bitmap ?: return
    if (alpha == 0f) return
    Box(modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }) {
        Image(
            bitmap             = image,
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(GIDBackground.copy(alpha = appearance.backgroundDim)))
    }
}

// ── Personalise sheet (tap the greeting on Home) ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PersonaliseSheet(
    appearance     : Appearance,
    profileName    : String?,
    onName         : (String?) -> Unit,
    onFont         : (NameFont) -> Unit,
    onColor        : (NameColor) -> Unit,
    onDim          : (Float) -> Unit,
    onPickPhoto    : (Uri) -> Unit,
    onRemovePhoto  : () -> Unit,
    photoBusy      : Boolean,
    photoFailed    : Boolean,
    onDismiss      : () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(appearance.homeName.orEmpty()) }
    // The slider moves freely and saves when let go; the photo behind updates live.
    var dim by remember { mutableFloatStateOf(appearance.backgroundDim) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPickPhoto)
    }
    val close = {
        onName(name)
        onDismiss()
    }
    val preview = appearance.copy(homeName = name, backgroundDim = dim)

    ModalBottomSheet(
        onDismissRequest = close,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = GIDSurface,
        // No dark scrim, so the background and its dim show live behind the sheet.
        scrimColor       = Color.Transparent,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
        ) {
            Text(stringResource(R.string.personalise_title), style = MaterialTheme.typography.titleLarge, color = GIDText)

            Greeting(
                name       = preview.greetingName(profileName),
                appearance = preview,
                fontSize   = 36.sp,
                lineHeight = 40.sp,
                modifier   = Modifier
                    .fillMaxWidth()
                    .background(GIDBackground, MaterialTheme.shapes.medium)
                    .padding(16.dp),
            )

            GIDTextField(
                value         = name,
                onValueChange = { name = it },
                label         = stringResource(R.string.personalise_name),
                trailingIcon  = if (name.isNotEmpty()) {
                    { TextButton(onClick = { name = "" }) { Text(stringResource(R.string.personalise_name_reset), color = GIDCyan) } }
                } else null,
            )
            Text(stringResource(R.string.personalise_name_hint), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)

            SectionLabel(stringResource(R.string.personalise_font))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NameFont.entries.forEach { font ->
                    val selected = font == appearance.font
                    FilterChip(
                        selected = selected,
                        onClick  = { onFont(font) },
                        label    = { Text(stringResource(font.label), fontFamily = font.family, fontWeight = font.weight) },
                        shape    = CircleShape,
                        border   = null,
                        colors   = FilterChipDefaults.filterChipColors(
                            containerColor         = GIDSurfaceHigh,
                            labelColor             = GIDText,
                            selectedContainerColor = GIDCyan,
                            selectedLabelColor     = OnCard,
                        ),
                    )
                }
            }

            SectionLabel(stringResource(R.string.personalise_color))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NameColor.entries.forEach { option ->
                    ColorSwatch(option, selected = option == appearance.color, onClick = { onColor(option) })
                }
            }

            SectionLabel(stringResource(R.string.personalise_background))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick  = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    enabled  = !photoBusy,
                    shape    = CircleShape,
                    colors   = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard),
                ) {
                    Text(stringResource(if (photoBusy) R.string.personalise_photo_busy else R.string.personalise_photo_choose))
                }
                if (appearance.backgroundPath != null) {
                    OutlinedButton(onClick = onRemovePhoto, enabled = !photoBusy, shape = CircleShape) {
                        Text(stringResource(R.string.personalise_photo_remove), color = GIDText)
                    }
                }
            }
            if (photoFailed) {
                Text(stringResource(R.string.personalise_photo_failed), style = MaterialTheme.typography.bodySmall, color = GIDCoral)
            }
            Text(stringResource(R.string.personalise_photo_hint), style = MaterialTheme.typography.bodySmall, color = GIDMutedText)

            if (appearance.backgroundPath != null) {
                Text(
                    stringResource(R.string.personalise_dim, (dim * 100).roundToInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = GIDText,
                )
                Slider(
                    value                 = dim,
                    onValueChange         = { dim = it; onDim(it) },
                    valueRange            = 0f..Appearance.MAX_DIM,
                    colors                = SliderDefaults.colors(thumbColor = GIDCyan, activeTrackColor = GIDCyan, inactiveTrackColor = GIDSurfaceHigh),
                )
            }

            Button(
                onClick  = close,
                shape    = CircleShape,
                colors   = ButtonDefaults.buttonColors(containerColor = GIDCyan, contentColor = OnCard),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.action_done)) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = GIDMutedText)
}

@Composable
private fun ColorSwatch(option: NameColor, selected: Boolean, onClick: () -> Unit) {
    val name = stringResource(option.label)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .then(if (selected) Modifier.border(2.dp, GIDText, CircleShape) else Modifier)
            .padding(4.dp)
            .clip(CircleShape)
            .background(option.color)
            .clickable(onClickLabel = name, onClick = onClick),
    ) {
        if (selected) Icon(Icons.Rounded.Check, contentDescription = name, tint = OnCard, modifier = Modifier.size(18.dp))
    }
}
