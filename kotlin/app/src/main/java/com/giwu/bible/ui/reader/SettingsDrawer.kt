package com.giwu.bible.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.giwu.bible.model.Bible
import com.giwu.bible.model.User
import com.giwu.bible.ui.common.VersionBadge
import com.giwu.bible.ui.theme.giwu

/**
 * The reader's settings, in the drawer that slides in from the right.
 *
 * Reset, the theme switch and the translation picker used to sit in the top
 * bar as loose icons. They live here now, in the order a reader asks for
 * them: who you are, how it looks, what you are reading, and the way back to
 * Genesis 1. The full Settings screen stays one row away for the rest.
 */
@Composable
fun SettingsDrawerSheet(
    bibles: List<Bible>,
    primaryBible: String,
    darkMode: Boolean,
    user: User?,
    onToggleDarkMode: () -> Unit,
    onSelectBible: (String) -> Unit,
    onReset: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenRegister: () -> Unit,
    onSignOut: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenAllSettings: () -> Unit,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close settings",
                    tint = MaterialTheme.giwu.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.giwu.divider)

        // ── Account ────────────────────────────────────────────────────────
        SectionLabel("ACCOUNT")
        if (user != null) {
            ListItem(
                leadingContent = {
                    Icon(Icons.Outlined.AccountCircle, contentDescription = null)
                },
                headlineContent = { Text(user.name) },
                supportingContent = {
                    Text(
                        text = user.email,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
            DrawerRow(
                icon = Icons.Outlined.BookmarkBorder,
                label = "Bookmarks",
                trailing = Icons.Rounded.ChevronRight,
                onClick = onOpenBookmarks,
            )
            DrawerRow(
                icon = Icons.AutoMirrored.Outlined.Logout,
                label = "Sign out",
                onClick = onSignOut,
            )
        } else {
            DrawerRow(
                icon = Icons.AutoMirrored.Outlined.Login,
                label = "Sign in",
                supporting = "Sync your bookmarks across devices",
                trailing = Icons.Rounded.ChevronRight,
                onClick = onOpenLogin,
            )
            DrawerRow(
                icon = Icons.Outlined.PersonAddAlt,
                label = "Sign up",
                supporting = "Reading itself needs no account",
                trailing = Icons.Rounded.ChevronRight,
                onClick = onOpenRegister,
            )
        }
        HorizontalDivider(color = MaterialTheme.giwu.divider)

        // ── Theme ──────────────────────────────────────────────────────────
        SectionLabel("THEME")
        ListItem(
            leadingContent = { Icon(Icons.Outlined.DarkMode, contentDescription = null) },
            headlineContent = { Text("Dark mode") },
            trailingContent = {
                Switch(checked = darkMode, onCheckedChange = { onToggleDarkMode() })
            },
            modifier = Modifier.clickable(onClick = onToggleDarkMode),
        )
        HorizontalDivider(color = MaterialTheme.giwu.divider)

        // ── Main bible ─────────────────────────────────────────────────────
        // Translations that are not on the device are listed but not
        // selectable; seeing what else exists is the point, and the Settings
        // screen is where they are fetched.
        SectionLabel("MAIN BIBLE")
        bibles.forEach { bible ->
            val isSelected = bible.table == primaryBible
            ListItem(
                leadingContent = {
                    VersionBadge(
                        abbreviation = bible.abbreviation,
                        enabled = bible.downloaded,
                    )
                },
                headlineContent = {
                    Text(
                        text = bible.version,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (bible.downloaded) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.giwu.muted
                        },
                    )
                },
                trailingContent = {
                    when {
                        isSelected -> Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Current translation",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )

                        !bible.downloaded -> Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = "Not downloaded",
                            tint = MaterialTheme.giwu.muted,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                },
                modifier = Modifier.clickable(enabled = bible.downloaded) {
                    onSelectBible(bible.table)
                },
            )
        }
        HorizontalDivider(color = MaterialTheme.giwu.divider)

        // ── Reset ──────────────────────────────────────────────────────────
        SectionLabel("RESET")
        DrawerRow(
            icon = Icons.Rounded.Refresh,
            label = "Back to Genesis 1 (KJV)",
            supporting = "Clears the verse you have open and starts the reader over",
            onClick = onReset,
        )
        HorizontalDivider(color = MaterialTheme.giwu.divider)

        DrawerRow(
            icon = Icons.Outlined.Settings,
            label = "All settings",
            supporting = "Read aloud, server and downloads",
            trailing = Icons.Rounded.ChevronRight,
            onClick = onOpenAllSettings,
        )
    }
}

@Composable
private fun DrawerRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    supporting: String? = null,
    trailing: ImageVector? = null,
) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(label) },
        supportingContent = supporting?.let {
            {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
        },
        trailingContent = trailing?.let {
            { Icon(it, contentDescription = null, modifier = Modifier.size(18.dp)) }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.giwu.muted,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 4.dp),
    )
}
