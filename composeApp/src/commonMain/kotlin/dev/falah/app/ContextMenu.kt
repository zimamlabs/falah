package dev.falah.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.vector.ImageVector
import dev.falah.core.Str

/** A question asked before an action that removes something. */
data class ConfirmSpec(val title: String, val text: String, val button: String)

/** One entry of a long-press menu. [confirm] makes the action ask first; [danger] colours it as destructive. */
data class MenuAction(val label: String, val icon: ImageVector? = null, val confirm: ConfirmSpec? = null, val danger: Boolean = false, val run: () -> Unit)

fun deleteAction(label: String, confirm: ConfirmSpec, run: () -> Unit) = MenuAction(label, Icons.Rounded.Delete, confirm, danger = true, run = run)

/**
 * The one long-press menu used on every row and card: tap does [onClick], a long press gives a light haptic and opens
 * the menu of [actions] (Edit, Details, Reset, Delete with a confirm, and so on, as each place needs). Nothing runs
 * until an entry is chosen, and entries with a [ConfirmSpec] ask first.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContextMenuBox(actions: List<MenuAction>, modifier: Modifier = Modifier, onClick: () -> Unit = {}, content: @Composable () -> Unit) {
    var open by remember { mutableStateOf(false) }
    var asking by remember { mutableStateOf<MenuAction?>(null) }
    val haptic = LocalHapticFeedback.current
    Box(modifier.combinedClickable(onClick = onClick, onLongClick = { if (actions.isNotEmpty()) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); open = true } })) {
        content()
        DropdownMenu(open, { open = false }) {
            actions.forEach { a ->
                DropdownMenuItem(
                    text = { Text(a.label, color = if (a.danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = a.icon?.let { icon -> { Icon(icon, null, tint = if (a.danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) } },
                    onClick = { open = false; if (a.confirm != null) asking = a else a.run() },
                )
            }
        }
    }
    asking?.let { a ->
        val c = a.confirm!!
        AlertDialog(
            onDismissRequest = { asking = null },
            title = { Text(c.title) }, text = { Text(c.text) },
            confirmButton = { TextButton({ asking = null; a.run() }) { Text(c.button, color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ asking = null }) { Text(Str.Cancel.t()) } },
        )
    }
}

/** The menu shared by every prayer row (Times, Home, Log): details, prayed, and the reminder. */
@Composable
fun prayerActions(s: FalahState, p: dev.falah.core.Prayer, date: kotlinx.datetime.LocalDate = s.today): List<MenuAction> = buildList {
    LocalPrayerOpener.current?.let { open -> add(MenuAction(Str.MenuDetails.t(), null) { open(p) }) }
    if (p in dev.falah.core.loggedPrayers) {
        val on = p in s.log.done(date)
        if (on) add(deleteAction(Str.MenuMarkNotPrayed.t(), ConfirmSpec(Str.MenuDeleteEntryTitle.t(), Str.MenuDeleteEntryText.t(p.shown(), num(date.toString())), Str.MenuDelete.t())) { s.markOn(date, p, false) })
        else add(MenuAction(Str.MenuMarkPrayed.t()) { s.markOn(date, p, true) })
    }
    if (s.platform.canNotify && p != dev.falah.core.Prayer.Sunrise) add(MenuAction(if (s.notifyOn(p)) Str.MenuReminderOff.t() else Str.MenuReminderOn.t()) { s.setNotify(p, !s.notifyOn(p)) })
}
