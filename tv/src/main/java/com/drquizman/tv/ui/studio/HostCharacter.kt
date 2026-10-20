package com.drquizman.tv.ui.studio

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.drquizman.core.host.HostDirector
import com.drquizman.core.model.GameSnapshot
import com.drquizman.core.model.HostMood
import com.drquizman.core.model.Phase
import com.drquizman.tv.R

internal class HostStageState(val director: HostDirector) {
    var snapshot: GameSnapshot? by mutableStateOf(null)
    var speaking by mutableStateOf(false)
    var viseme by mutableIntStateOf(0)
    var bounds by mutableStateOf(Rect.Zero)
}
internal val LocalHostStage = staticCompositionLocalOf<HostStageState?> { null }

@Composable
fun DirectedHostStage(snapshot: GameSnapshot, speaking: Boolean, viseme: Int, content: @Composable () -> Unit) {
    val stage = remember { HostStageState(HostDirector()) }
    stage.snapshot = snapshot
    stage.speaking = speaking
    stage.viseme = viseme
    CompositionLocalProvider(LocalHostStage provides stage, content = content)
}

/** One stable surface, outside every phase branch and below transient overlays. */
@Composable
fun HostStageLayer() {
    val stage = LocalHostStage.current ?: return
    val snapshot = stage.snapshot ?: return
    NativeHostCharacter(snapshot.hostMood, Modifier.fillMaxSize(), stage.speaking, stage.viseme, snapshot.phase)
}

@Composable
fun HostCharacter(
    mood: HostMood,
    modifier: Modifier = Modifier,
    speaking: Boolean = false,
    viseme: Int = 0,
    phase: Phase? = null,
) {
    val stage = LocalHostStage.current
    if (stage != null) {
        // Layout slot only; changing scenes never removes the native surface.
        Box(modifier.onGloballyPositioned {
            stage.bounds = Rect(it.positionInWindow(), androidx.compose.ui.geometry.Size(it.size.width.toFloat(), it.size.height.toFloat()))
        })
    } else NativeHostCharacter(mood, modifier, speaking, viseme, phase)
}

@Composable
private fun NativeHostCharacter(mood: HostMood, modifier: Modifier, speaking: Boolean, viseme: Int, phase: Phase?) {
    val stage = LocalHostStage.current
    val localDirector = remember { HostDirector() }
    val director = stage?.director ?: localDirector
    val snapshot = stage?.snapshot ?: phase?.let {
        GameSnapshot(pin = "", phase = it, players = emptyList(), hostLine = "", hostMood = mood)
    }
    var failed by remember { mutableStateOf(false) }
    var texture by remember { mutableStateOf<HostModelView?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            texture?.setActive(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); texture?.setActive(false) }
    }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    Box(modifier.onGloballyPositioned { origin = it.positionInWindow() }) {
        if (failed) {
            // Only devices with no usable graphics context / failed asset load use the fallback.
            Image(painterResource(R.drawable.host_idle), contentDescription = null,
                modifier = if (stage != null && stage.bounds.width > 0f) {
                    Modifier.offset { IntOffset((stage.bounds.left - origin.x).toInt(), (stage.bounds.top - origin.y).toInt()) }
                        .size(with(density) { stage.bounds.width.toDp() }, with(density) { stage.bounds.height.toDp() })
                } else Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    HostModelView(context, director, waitForFraming = stage != null) { failed = true }.also { texture = it }
                },
                update = { view ->
                    view.snapshot = snapshot
                    view.speaking = speaking
                    view.viseme = viseme
                    view.framingBounds = stage?.bounds?.translate(-origin)
                    view.setActive(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
                },
                onRelease = { view -> view.setActive(false); texture = null },
            )
        }
    }
}
