package com.drquizman.tv.ui

import com.drquizman.core.i18n.LocalizedText
import com.drquizman.core.i18n.LocaleManager
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxHeight
import com.drquizman.tv.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.drquizman.core.engine.MatchPlan
import com.drquizman.core.engine.ShowPlan
import com.drquizman.core.host.HostLines
import com.drquizman.core.mechanic.MechanicCatalog
import com.drquizman.core.mechanic.MechanicSlot
import com.drquizman.core.mechanic.MechanicSpec
import com.drquizman.core.model.CompetitionType
import com.drquizman.core.model.HostMood
import com.drquizman.tv.ui.studio.HostCharacter
import com.drquizman.tv.ui.studio.StudioBackground
import com.drquizman.tv.speech.HostSpeech

@Composable
fun MenuScreen(
    help: Boolean,
    onPlay: (ShowPlan) -> Unit,
    onHelp: () -> Unit,
    onBackFromHelp: () -> Unit,
    starting: Boolean = false,
    speechEngines: List<HostSpeech.EngineOption> = emptyList(),
    speechVoices: List<HostSpeech.VoiceOption> = emptyList(),
    selectedSpeechEngine: String? = null,
    selectedSpeechVoice: String? = null,
    speechStatus: HostSpeech.Status = HostSpeech.Status.LOADING,
    onSpeechEngineSelected: (String) -> Unit = {},
    onSpeechVoiceSelected: (String) -> Unit = {},
    onSpeechVoiceFocused: (String) -> Unit = {},
    language: String = "ru",
    onLanguageSelected: (String) -> Unit = {},
    packs: com.drquizman.tv.questions.PacksState = com.drquizman.tv.questions.PacksState(),
    onPackSelected: (String, Boolean) -> Unit = { _, _ -> },
    onLinkTelegram: () -> Unit = {},
    onRefreshPacks: () -> Unit = {},
    onDownloadPacks: () -> Unit = {},
    onClosePacks: () -> Unit = {},
) {
    var questionPacks by remember { mutableStateOf(false) }
    var pickRounds by remember { mutableStateOf(false) }
    var speechSettings by remember { mutableStateOf(false) }
    var languageSettings by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(help, pickRounds, speechSettings, languageSettings, questionPacks, starting) {
        if (!help && !pickRounds && !speechSettings && !languageSettings && !questionPacks && !starting) runCatching { focus.requestFocus() }
    }

    Box(Modifier.fillMaxSize()) {
        StudioBackground()
        Row(
            Modifier.fillMaxSize().padding(horizontal = 46.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HostCharacter(HostMood.HYPE, Modifier.weight(0.34f).fillMaxHeight())
            Column(
                Modifier.weight(0.66f).padding(start = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painterResource(R.drawable.quizman_wordmark),
                    contentDescription = "Dr. Quizman",
                    modifier = Modifier.fillMaxWidth(0.86f).height(120.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(16.dp))
                ModeButton(
                    title = if (starting) LocalizedText.get("MenuScreen.001") else LocalizedText.get("MenuScreen.002"),
                    subtitle = LocalizedText.get("MenuScreen.003", (MatchPlan.STANDARD_ROUNDS)),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    enabled = !starting,
                    featured = true,
                    onClick = { onPlay(ShowPlan.Standard) },
                )
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ModeButton(LocalizedText.get("MenuScreen.004"), LocalizedText.get("MenuScreen.005", (MatchPlan.LONG_ROUNDS)),
                        Modifier.weight(1f), enabled = !starting, onClick = { onPlay(ShowPlan.Long) })
                    ModeButton(LocalizedText.get("MenuScreen.006"), LocalizedText.get("MenuScreen.007"),
                        Modifier.weight(1f), enabled = !starting, onClick = { pickRounds = true })
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ShowButton(LocalizedText.get("MenuScreen.008"), Modifier.weight(1f), onClick = onHelp, enabled = !starting)
                    ShowButton(LocalizedText.get("MenuScreen.009"), Modifier.weight(1f), onClick = { speechSettings = true }, enabled = !starting)
                    ShowButton(
                        "${LocaleManager.flag(language)}  ${LocalizedText.get("LanguagePicker.button")}",
                        Modifier.weight(1f),
                        onClick = { languageSettings = true },
                        enabled = !starting,
                    )
                }
                if (speechStatus != HostSpeech.Status.READY) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when (speechStatus) {
                            HostSpeech.Status.LOADING -> LocalizedText.get("Speech.statusLoading")
                            HostSpeech.Status.UNAVAILABLE -> LocalizedText.get("Speech.statusUnavailable")
                            HostSpeech.Status.READY -> ""
                        },
                        color = TvColors.goldSoft,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(10.dp))
                ShowButton(LocalizedText.get("Packs.title"), Modifier.fillMaxWidth(), enabled = !starting,
                    onClick = { questionPacks = true; if (packs.linked) onRefreshPacks() else onLinkTelegram() })
            }
        }
        if (languageSettings) {
            LanguagePicker(
                language = language,
                onClose = { languageSettings = false },
                onSelected = {
                    languageSettings = false
                    onLanguageSelected(it)
                },
            )
        }
        if (questionPacks) QuestionPacksScreen(packs, onPackSelected, onLinkTelegram, onRefreshPacks,
            onDownloadPacks, onClose = { questionPacks = false; onClosePacks() })
        AnimatedVisibility(help, enter = fadeIn(), exit = fadeOut()) {
            HelpOverlay(onBackFromHelp)
        }
        AnimatedVisibility(pickRounds, enter = fadeIn(), exit = fadeOut()) {
            RoundPickOverlay(
                starting = starting,
                onStart = { picked -> onPlay(ShowPlan.Custom(picked)) },
                onClose = { if (!starting) pickRounds = false },
            )
        }
        AnimatedVisibility(speechSettings, enter = fadeIn(), exit = fadeOut()) {
            SpeechSettingsOverlay(
                engines = speechEngines,
                voices = speechVoices,
                selectedEngine = selectedSpeechEngine,
                selectedVoice = selectedSpeechVoice,
                onEngineSelected = onSpeechEngineSelected,
                onVoiceSelected = onSpeechVoiceSelected,
                onVoiceFocused = onSpeechVoiceFocused,
                onClose = { speechSettings = false },
            )
        }
    }
}

@Composable
private fun LanguagePicker(language: String, onClose: () -> Unit, onSelected: (String) -> Unit) {
    val languages = LocaleManager.supportedLanguages().toList()
    val selectedIndex = languages.indexOf(language).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val focus = remember { FocusRequester() }
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.fillMaxWidth().background(TvColors.panel, RoundedCornerShape(24.dp))
                .border(2.dp, TvColors.gold, RoundedCornerShape(24.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(LocalizedText.get("LanguagePicker.title"), color = TvColors.goldSoft, fontSize = 24.sp)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().height(220.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(languages, key = { _, code -> code }) { index, code ->
                    SpeechChoice(
                        label = "${LocaleManager.flag(code)}  ${LocaleManager.nativeName(code)}",
                        selected = code == language,
                        accent = false,
                        modifier = if (index == selectedIndex) Modifier.focusRequester(focus) else Modifier,
                        onClick = { onSelected(code) },
                    )
                }
            }
            ShowButton(LocalizedText.get("LanguagePicker.close"), onClick = onClose)
        }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    }
}

@Composable
private fun SpeechSettingsOverlay(
    engines: List<HostSpeech.EngineOption>,
    voices: List<HostSpeech.VoiceOption>,
    selectedEngine: String?,
    selectedVoice: String?,
    onEngineSelected: (String) -> Unit,
    onVoiceSelected: (String) -> Unit,
    onVoiceFocused: (String) -> Unit,
    onClose: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Box(
        Modifier.fillMaxSize().background(TvColors.night.copy(0.78f)).padding(42.dp),
    ) {
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth().fillMaxHeight(0.88f)
                .background(Brush.verticalGradient(listOf(TvColors.panelRaised, TvColors.panel)), RoundedCornerShape(24.dp))
                .border(2.dp, TvColors.line, RoundedCornerShape(24.dp)).padding(28.dp)
                .focusGroup(),
        ) {
            Text(LocalizedText.get("MenuScreen.011"), color = TvColors.goldSoft, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(5.dp))
            Text(LocalizedText.get("MenuScreen.012"), color = TvColors.cream.copy(0.82f), fontSize = 16.sp)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.weight(1f).fillMaxWidth().focusGroup(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                SpeechChoiceColumn(
                    title = LocalizedText.get("MenuScreen.013"),
                    items = engines.map { SpeechChoiceItem(it.packageName, it.label) },
                    selected = selectedEngine,
                    modifier = Modifier.weight(1f),
                    firstFocus = focus,
                    onSelected = onEngineSelected,
                )
                SpeechChoiceColumn(
                    title = LocalizedText.get("MenuScreen.014"),
                    items = voices.map {
                        SpeechChoiceItem(it.name, buildString {
                            append(it.label)
                            if (it.isDefault) append(LocalizedText.get("MenuScreen.015"))
                            if (it.isRecommended) append(LocalizedText.get("MenuScreen.016"))
                            append(if (it.isNetwork) LocalizedText.get("MenuScreen.017") else LocalizedText.get("MenuScreen.018"))
                        }, accent = it.isDefault || it.isRecommended)
                    },
                    selected = selectedVoice,
                    modifier = Modifier.weight(1.35f),
                    onSelected = { voiceName ->
                        onVoiceFocused(voiceName)
                        onVoiceSelected(voiceName)
                    },
                    onFocused = onVoiceFocused,
                )
            }
            Spacer(Modifier.height(16.dp))
            ShowButton(LocalizedText.get("MenuScreen.019"), onClick = onClose)
        }
    }
}

@Composable
private fun SpeechChoiceColumn(
    title: String,
    items: List<SpeechChoiceItem>,
    selected: String?,
    modifier: Modifier = Modifier,
    firstFocus: FocusRequester? = null,
    onSelected: (String) -> Unit,
    onFocused: (String) -> Unit = {},
) {
    Column(modifier) {
        Text(title, color = TvColors.goldSoft, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            Text(LocalizedText.get("MenuScreen.020"), color = TvColors.cream.copy(0.7f), fontSize = 16.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                    SpeechChoice(
                        label = item.label,
                        selected = item.key == selected,
                        accent = item.accent,
                        modifier = if (index == 0 && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier,
                        onClick = { onSelected(item.key) },
                        onFocused = { onFocused(item.key) },
                    )
                }
            }
        }
    }
}

private data class SpeechChoiceItem(val key: String, val label: String, val accent: Boolean = false)

@Composable
private fun SpeechChoice(label: String, selected: Boolean, accent: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit, onFocused: () -> Unit = {}) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) {
        if (focused) onFocused()
    }
    Box(
        modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp))
            .background(if (focused) TvColors.gold else if (selected) TvColors.panelRaised else TvColors.panel)
            .border(if (selected || accent) 2.dp else 1.dp, if (selected) TvColors.goldSoft else if (accent) TvColors.gold else TvColors.line, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .tvActivate(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text("${if (selected) "✓  " else ""}$label", color = if (focused) TvColors.ink else TvColors.cream, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun HelpOverlay(onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Box(
        Modifier
            .fillMaxSize()
            .background(TvColors.night.copy(0.72f))
            .padding(80.dp),
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .background(Brush.verticalGradient(listOf(TvColors.panelRaised, TvColors.panel)), RoundedCornerShape(24.dp))
                .border(2.dp, TvColors.line, RoundedCornerShape(24.dp))
                .padding(36.dp),
        ) {
            Text(LocalizedText.get("MenuScreen.021"), color = TvColors.goldSoft, fontSize = 32.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            Text(
                    LocalizedText.get("MenuScreen.022") +
                    LocalizedText.get("MenuScreen.023") +
                    LocalizedText.get("MenuScreen.024") +
                    LocalizedText.get("MenuScreen.025"),
                color = TvColors.cream,
                fontSize = 20.sp,
            )
            Spacer(Modifier.height(24.dp))
            ShowButton(LocalizedText.get("MenuScreen.026"), Modifier.focusRequester(focus), onClose)
        }
    }
}

@Composable
fun RoundPickOverlay(
    onStart: (List<CompetitionType>) -> Unit,
    onClose: () -> Unit,
    starting: Boolean = false,
) {
    val catalog = remember { MechanicCatalog.all() }
    var selected by remember { mutableStateOf(emptySet<CompetitionType>()) }
    val firstRoundFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstRoundFocus.requestFocus() } }
    val preview = MatchPlan.custom(selected)
    Box(
        Modifier
            .fillMaxSize()
            .background(TvColors.night.copy(0.72f))
            .padding(32.dp),
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(TvColors.panelRaised, TvColors.panel)), RoundedCornerShape(24.dp))
                .border(2.dp, TvColors.line, RoundedCornerShape(24.dp))
                .padding(24.dp),
        ) {
            Text(LocalizedText.get("MenuScreen.027"), color = TvColors.goldSoft, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(
                LocalizedText.get("MenuScreen.028"),
                color = TvColors.cream,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(catalog, key = { _, spec -> spec.type.name }) { index, spec ->
                    MechanicToggle(
                        spec = spec,
                        selected = spec.type in selected,
                        modifier = if (index == 0) Modifier.focusRequester(firstRoundFocus) else Modifier,
                        enabled = !starting,
                        onClick = {
                            selected = if (spec.type in selected) {
                                selected - spec.type
                            } else {
                                selected + spec.type
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.CenterStart) {
                Text(
                    if (preview.isEmpty()) LocalizedText.get("MenuScreen.029")
                    else preview.joinToString("  →  ") { HostLines.titles(it) },
                    color = TvColors.cream,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ShowButton(
                    if (starting) LocalizedText.get("MenuScreen.030") else LocalizedText.get("MenuScreen.031"),
                    enabled = !starting,
                    onClick = { if (preview.isNotEmpty() && !starting) onStart(preview) },
                )
                ShowButton(LocalizedText.get("MenuScreen.032"), enabled = !starting, onClick = onClose)
            }
        }
    }
}

@Composable
private fun MechanicToggle(
    spec: MechanicSpec,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val fill = when {
        focused -> TvColors.gold
        selected -> TvColors.panelRaised
        else -> TvColors.panel
    }
    val slot = when (spec.slot) {
        MechanicSlot.START -> LocalizedText.get("MenuScreen.033")
        MechanicSlot.MID -> LocalizedText.get("MenuScreen.034")
        MechanicSlot.END -> LocalizedText.get("MenuScreen.035")
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(fill)
            .border(
                if (selected && !focused) 2.dp else 1.dp,
                if (selected) TvColors.goldSoft else TvColors.line,
                RoundedCornerShape(16.dp),
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .tvActivate(enabled, onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${if (selected) "✓  " else ""}${spec.title}",
                color = if (focused) TvColors.ink else TvColors.cream,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                slot,
                color = if (focused) TvColors.ink.copy(0.7f) else TvColors.cream.copy(0.7f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
fun ModeButton(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    featured: Boolean = false,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        modifier
            .height(if (featured) 90.dp else 76.dp)
            .studioFinish()
            .clip(RoundedCornerShape(18.dp))
            .background(
                when {
                    !enabled -> Brush.horizontalGradient(listOf(TvColors.panelRaised, TvColors.panel))
                    focused -> Brush.verticalGradient(listOf(Color(0xFFFFF6E4), TvColors.goldSoft))
                    featured -> Brush.verticalGradient(listOf(TvColors.cream, TvColors.gold))
                    else -> Brush.horizontalGradient(listOf(TvColors.panelRaised, TvColors.panel))
                },
            )
            .border(
                if (focused) 3.dp else 1.dp,
                if (focused || featured) TvColors.goldSoft else TvColors.line,
                RoundedCornerShape(18.dp),
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .tvActivate(enabled, onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column {
            Text(
                title,
                color = if (focused || featured) TvColors.ink else TvColors.goldSoft,
                fontWeight = FontWeight.Black,
                fontSize = if (featured) 28.sp else 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                color = if (focused || featured) TvColors.ink.copy(0.8f) else TvColors.cream.copy(0.85f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ShowButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        modifier
            .height(56.dp)
            .studioFinish()
            .clip(RoundedCornerShape(18.dp))
            .background(
                when {
                    !enabled -> TvColors.panel.copy(0.45f)
                    focused -> TvColors.gold
                    else -> TvColors.panel
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .tvActivate(enabled, onClick)
            .border(if (focused) 3.dp else 1.dp, TvColors.gold, RoundedCornerShape(18.dp))
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (focused && enabled) TvColors.ink else TvColors.cream,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
        )
    }
}

private fun Modifier.tvActivate(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    onKeyEvent { event ->
        if (!enabled) return@onKeyEvent false
        val confirm = event.key == Key.DirectionCenter || event.key == Key.Enter
        if (!confirm) return@onKeyEvent false
        when (event.type) {
            KeyEventType.KeyDown -> true
            KeyEventType.KeyUp -> {
                onClick()
                true
            }
            else -> false
        }
    }

@Composable
fun SpeechBubble(text: String, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    Box(
        modifier
            .studioFinish(22.dp)
            .background(
                Brush.verticalGradient(listOf(Color(0xFFFFF8EA), TvColors.cream)),
                RoundedCornerShape(22.dp),
            )
            .border(3.dp, TvColors.gold, RoundedCornerShape(22.dp))
            .padding(horizontal = 22.dp, vertical = 14.dp),
    ) {
        Text(
            text,
            color = TvColors.ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
