package com.drquizman.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.drquizman.core.i18n.LocalizedText
import com.drquizman.tv.questions.PacksState
import com.drquizman.tv.questions.PackInfo

@Composable
fun QuestionPacksScreen(state: PacksState, onSelect: (String, Boolean) -> Unit,
    onLink: () -> Unit, onRefresh: () -> Unit, onDownload: () -> Unit, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.92f)
            .background(TvColors.panel, RoundedCornerShape(22.dp))
            .border(2.dp, TvColors.gold, RoundedCornerShape(22.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(LocalizedText.get("Packs.title"), color = TvColors.goldSoft, fontSize = 27.sp)
            Hint("Packs.selectionHelp")
            if (!state.linked) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    JoinQr("https://t.me/DrQuizman_bot", Modifier.size(140.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Heading("Packs.telegram")
                        Hint("Packs.linkHelp")
                        if (state.code != null) {
                            Text(state.code, color = TvColors.goldSoft, fontSize = 28.sp)
                            Hint("Packs.codeHelp")
                        } else if (state.busy) {
                            Hint("Packs.working")
                        }
                        state.messageKey?.let { Hint(it) }
                        if (!state.busy && state.code == null) {
                            ShowButton(LocalizedText.get("Packs.newCode"), onClick = onLink)
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(LocalizedText.get("Packs.connected"), color = TvColors.goldSoft, fontSize = 16.sp)
                    ShowButton(LocalizedText.get("Packs.refresh"), enabled = !state.busy, onClick = onRefresh)
                }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Heading("Packs.installed") }
                item {
                    PackToggle(LocalizedText.get("Packs.base"), state.baseSelected,
                        !state.busy && (state.canDisableBase || !state.baseSelected),
                        subtitle = LocalizedText.get("Packs.included")) { onSelect("base", it) }
                }
                if (!state.canDisableBase) item { Hint("Packs.baseRule") }
                items(state.installed, key = { it.info.id }) { pack ->
                    PackToggle(packTitle(pack.info), pack.selected, !state.busy,
                        subtitle = packDetails(pack.info)) { onSelect(pack.info.id, it) }
                }
                if (state.linked) {
                    item { Heading("Packs.telegram") }
                    if (state.available.isEmpty() && !state.busy) item { Hint("Packs.nonePurchased") }
                    items(state.available, key = { "purchased.${it.id}" }) { pack ->
                        Column(Modifier.fillMaxWidth().background(TvColors.panelRaised, RoundedCornerShape(10.dp)).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(packTitle(pack), color = TvColors.cream, fontSize = 20.sp)
                            Text(packDetails(pack), color = TvColors.cream.copy(alpha = 0.75f), fontSize = 16.sp)
                            Text(LocalizedText.get("Packs.readyToDownload"), color = TvColors.goldSoft, fontSize = 16.sp)
                        }
                    }
                    if (state.available.isNotEmpty()) item {
                        ShowButton(LocalizedText.get("Packs.downloadAll"), enabled = !state.busy, onClick = onDownload)
                    }
                }
                if (state.busy && state.linked) item { Hint("Packs.working") }
                if (state.linked) state.messageKey?.let { key -> item { Hint(key) } }
            }
            ShowButton(LocalizedText.get("Packs.close"), Modifier.focusRequester(focus), onClick = onClose)
        }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    }
}

@Composable private fun Heading(key: String) {
    Text(LocalizedText.get(key), color = TvColors.goldSoft, fontSize = 20.sp)
}
@Composable private fun Hint(key: String) {
    Text(LocalizedText.get(key), color = TvColors.cream.copy(alpha = 0.75f), fontSize = 16.sp)
}
@Composable private fun PackToggle(title: String, selected: Boolean, enabled: Boolean,
    modifier: Modifier = Modifier, subtitle: String = "", onChange: (Boolean) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Row(modifier.fillMaxWidth().background(if (focused) TvColors.gold else TvColors.panelRaised, RoundedCornerShape(10.dp))
        .toggleable(value = selected, enabled = enabled, role = Role.Checkbox,
            interactionSource = interaction, indication = null, onValueChange = onChange)
        .padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val color = if (focused) TvColors.ink else TvColors.cream.copy(alpha = if (enabled) 1f else 0.65f)
        Text(if (selected) "☑" else "☐", color = color, fontSize = 22.sp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = color, fontSize = 20.sp)
            Text(subtitle, color = color.copy(alpha = 0.8f), fontSize = 16.sp)
        }
        Text(LocalizedText.get(if (selected) "Packs.selected" else "Packs.unselected"), color = color, fontSize = 16.sp)
    }
}

private fun packTitle(pack: PackInfo): String =
    if (pack.id == "new_750_questions_${pack.language}") LocalizedText.get("Packs.extraOne") else pack.title
private fun packDetails(pack: PackInfo): String = LocalizedText.get("Packs.details", pack.count, pack.topics)
