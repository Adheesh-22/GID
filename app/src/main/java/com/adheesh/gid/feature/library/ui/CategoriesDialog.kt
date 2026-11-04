package com.adheesh.gid.feature.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.adheesh.gid.R
import com.adheesh.gid.core.database.CaptureCategoryEntity
import com.adheesh.gid.core.designsystem.GIDCoral
import com.adheesh.gid.core.designsystem.GIDCyan
import com.adheesh.gid.core.designsystem.GIDDialog
import com.adheesh.gid.core.designsystem.GIDMutedText
import com.adheesh.gid.core.designsystem.GIDText

// ── Category manager (SPEC §2.8) ─────────────────────────────────────────────
// Add, rename or remove the categories shared by every subject. Removing one
// keeps its items under All; Deadline stays because it carries the due date.
@Composable
fun CategoriesDialog(
    categories : List<CaptureCategoryEntity>,
    onAdd      : (String) -> Unit,
    onRename   : (CaptureCategoryEntity, String) -> Unit,
    onRemove   : (CaptureCategoryEntity) -> Unit,
    onDismiss  : () -> Unit,
) {
    var adding   by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<CaptureCategoryEntity?>(null) }
    var removing by remember { mutableStateOf<CaptureCategoryEntity?>(null) }

    GIDDialog(title = stringResource(R.string.category_manage), onDismiss = onDismiss) {
        Column(
            Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            categories.forEach { category ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(category.name, style = MaterialTheme.typography.bodyLarge, color = GIDText, modifier = Modifier.weight(1f))
                    IconButton(onClick = { renaming = category }) {
                        Icon(Icons.Rounded.Edit, stringResource(R.string.category_rename), tint = GIDMutedText)
                    }
                    if (category.isRemovable) {
                        IconButton(onClick = { removing = category }) {
                            Icon(Icons.Rounded.Delete, stringResource(R.string.category_remove), tint = GIDCoral)
                        }
                    }
                }
            }
            TextButton(onClick = { adding = true }) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = GIDCyan)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.category_new), color = GIDCyan)
            }
        }
    }

    if (adding) NameDialog(stringResource(R.string.category_new), onDismiss = { adding = false }, onSave = onAdd)
    renaming?.let { category ->
        NameDialog(stringResource(R.string.category_rename), initial = category.name, onDismiss = { renaming = null }) {
            onRename(category, it)
        }
    }
    removing?.let { category ->
        ConfirmDialog(
            title     = stringResource(R.string.category_remove),
            body      = stringResource(R.string.category_remove_body, category.name),
            confirm   = stringResource(R.string.category_remove),
            onDismiss = { removing = null },
            onConfirm = { onRemove(category); removing = null },
        )
    }
}
