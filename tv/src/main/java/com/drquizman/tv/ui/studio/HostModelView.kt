package com.drquizman.tv.ui.studio

import android.content.Context
import android.util.Log
import android.view.Choreographer
import android.view.Surface
import android.view.TextureView
import com.drquizman.core.host.HostDirector
import com.drquizman.core.model.GameSnapshot
import com.google.android.filament.*
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.*
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.tan
import androidx.compose.ui.geometry.Rect

/** A transparent native GL layer. All Filament calls and teardown run on the UI thread. */
internal class HostModelView(context: Context, private val director: HostDirector,
                             waitForFraming: Boolean = false,
                             private val onFailure: () -> Unit) : TextureView(context), Choreographer.FrameCallback {
    var snapshot: GameSnapshot? = null
    var speaking = false
    var viseme = 0
    var framingBounds: Rect? = if (waitForFraming) Rect.Zero else null
    private var active = false
    private var scheduled = false
    private var lastFrame = 0L
    private var nextFrame = 0L
    private var scope: CoroutineScope? = null
    private var graphics: Graphics? = null
    private val choreographer = Choreographer.getInstance()
    private val releaseDetached = Runnable {
        if (!isAttachedToWindow) {
            scope?.cancel(); scope = null
            graphics?.close(); graphics = null
        }
    }

    init { isOpaque = false; isFocusable = false; isClickable = false; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        removeCallbacks(releaseDetached)
        if (graphics != null) { schedule(); return }
        try {
            Gltfio.init()
            graphics = Graphics()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { scope ->
                scope.launch {
                    try {
                        val bytes = withContext(Dispatchers.IO) {
                            modelBytes ?: context.assets.open("host/doctor.glb").use { it.readBytes() }.also { modelBytes = it }
                        }
                        ensureActive()
                        graphics?.load(bytes)
                    } catch (_: CancellationException) {
                    } catch (e: Exception) { fail(e) }
                }
            }
            schedule()
        } catch (e: Throwable) { fail(e) }
    }

    fun setActive(value: Boolean) {
        if (active == value) return
        active = value; lastFrame = 0; nextFrame = 0
        if (value) schedule() else { choreographer.removeFrameCallback(this); scheduled = false }
    }

    private fun schedule() {
        if (active && isAttachedToWindow && !scheduled) {
            scheduled = true
            choreographer.postFrameCallback(this)
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        scheduled = false
        if (!active || !isAttachedToWindow) return
        try {
            // A steady 30 fps costs half the skinning/render work. Use a deadline
            // rather than a strict elapsed threshold (which skips extra vsyncs).
            if (nextFrame == 0L || frameTimeNanos + 2_000_000L >= nextFrame) {
                val dt = if (lastFrame == 0L) 0f else ((frameTimeNanos - lastFrame) / 1e9f).coerceAtMost(.1f)
                lastFrame = frameTimeNanos
                graphics?.render(frameTimeNanos, dt)
                nextFrame = if (nextFrame == 0L || frameTimeNanos - nextFrame > 33_333_333L)
                    frameTimeNanos + 33_333_333L else nextFrame + 33_333_333L
            }
            schedule()
        } catch (e: Exception) { fail(e) }
    }

    private fun fail(error: Throwable) {
        Log.e("Quizman3D", "Unable to render the host; using the image fallback", error)
        setActive(false)
        post { onFailure() }
    }

    override fun onDetachedFromWindow() {
        choreographer.removeFrameCallback(this); scheduled = false; lastFrame = 0
        // Compose moves this one actor between phase layouts in the same UI pass.
        // Keep the engine during a reparent, release it when the screen really disappears.
        post(releaseDetached)
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        graphics?.resizeBuffers(w, h)
    }

    private inner class Graphics {
        private val engine = Engine.create(Engine.Backend.OPENGL)
        private val renderer = engine.createRenderer()
        private val scene = engine.createScene()
        private val view = engine.createView()
        private val cameraEntity = EntityManager.get().create()
        private val camera = engine.createCamera(cameraEntity)
        private val materials = UbershaderProvider(engine)
        private val loader = AssetLoader(engine, materials, EntityManager.get())
        private val resources = ResourceLoader(engine, true)
        private val ui = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
        private var swapChain: SwapChain? = null
        private var asset: FilamentAsset? = null
        private var animator: Animator? = null
        private var modelBuffer: ByteBuffer? = null
        private var clips = emptyMap<String, Int>()
        private var durations = emptyMap<String, Float>()
        private var mouthInstance = 0
        private var eyeInstance = 0
        private val morphWeight = FloatArray(1)
        private var cameraY = 3.77
        private var cameraDistance = 13.5
        private var cameraReady = false
        private var loggedClip = ""
        private var closed = false
        private var renderWidth = 1
        private var renderHeight = 1
        private val lights = mutableListOf<Int>()
        private val indirect = IndirectLight.Builder()
            .irradiance(1, floatArrayOf(.8f, .85f, 1f)).intensity(25_000f).build(engine)

        init {
            view.scene = scene; view.camera = camera
            view.blendMode = View.BlendMode.TRANSLUCENT
            view.antiAliasing = View.AntiAliasing.FXAA
            view.isPostProcessingEnabled = true
            scene.indirectLight = indirect
            renderer.clearOptions = Renderer.ClearOptions().apply {
                clear = true; clearColor = doubleArrayOf(0.0, 0.0, 0.0, 0.0)
            }
            camera.setExposure(16f, 1f / 125f, 100f)
            addLight(-.5f, -.7f, -1f, 85_000f, 1f, .94f, .84f)
            addLight(.8f, -.2f, .3f, 40_000f, .65f, .8f, 1f)
            ui.isOpaque = false
            ui.renderCallback = object : UiHelper.RendererCallback {
                override fun onNativeWindowChanged(surface: Surface) {
                    swapChain?.let(engine::destroySwapChain)
                    swapChain = engine.createSwapChain(surface, ui.swapChainFlags)
                }
                override fun onDetachedFromSurface() {
                    swapChain?.let(engine::destroySwapChain); swapChain = null
                    engine.flushAndWait()
                }
                override fun onResized(width: Int, height: Int) {
                    // UiHelper's first TextureView callback reports layout dimensions
                    // even when the backing buffer already uses the requested size.
                    renderWidth = ui.desiredWidth.takeIf { it > 0 } ?: width
                    renderHeight = ui.desiredHeight.takeIf { it > 0 } ?: height
                    view.viewport = Viewport(0, 0, renderWidth, renderHeight)
                    camera.setProjection(34.0, width.toDouble() / height.coerceAtLeast(1), .05, 100.0, Camera.Fov.VERTICAL)
                    engine.flushAndWait()
                }
            }
            resizeBuffers(this@HostModelView.width, this@HostModelView.height)
            ui.attachTo(this@HostModelView)
        }

        fun resizeBuffers(w: Int, h: Int) {
            if (w <= 0 || h <= 0) return
            // Limit only the transparent 3D layer; Compose text stays native-resolution.
            val scale = minOf(1.0, 1280.0 / w, 720.0 / h)
            val rw = (w * scale).toInt().coerceAtLeast(1)
            val rh = (h * scale).toInt().coerceAtLeast(1)
            if (ui.desiredWidth != rw || ui.desiredHeight != rh) {
                ui.setDesiredSize(rw, rh)
                Log.d("Quizman3D", "Render buffer=${rw}x${rh}, display=${w}x${h}, target=30fps")
            }
        }

        private fun addLight(x: Float, y: Float, z: Float, power: Float, r: Float, g: Float, b: Float) {
            val entity = EntityManager.get().create()
            lights += entity
            LightManager.Builder(LightManager.Type.DIRECTIONAL).direction(x, y, z)
                .color(r, g, b).intensity(power).castShadows(false).build(engine, entity)
            scene.addEntity(entity)
        }

        fun load(bytes: ByteArray) {
            modelBuffer = ByteBuffer.allocateDirect(bytes.size).apply { put(bytes); flip() }
            val loaded = requireNotNull(loader.createAsset(modelBuffer!!)) { "Invalid host GLB" }
            asset = loaded
            check(resources.asyncBeginLoad(loaded)) { "Cannot load host textures" }
            animator = loaded.instance.animator
            val animation = animator!!
            clips = (0 until animation.animationCount).associateBy { animation.getAnimationName(it) }
            check(HostDirector.TALK.all(clips::containsKey)) { "Missing conversation animations" }
            durations = clips.mapValues { animation.getAnimationDuration(it.value) }
            val renderables = engine.renderableManager
            loaded.renderableEntities.forEach { entity ->
                val instance = renderables.getInstance(entity)
                // Bind-pose bounds do not cover every hand gesture.
                renderables.setCulling(instance, false)
                when {
                    loaded.getName(entity)?.contains("Mouth") == true -> mouthInstance = instance
                    loaded.getName(entity)?.contains("Eyes") == true -> eyeInstance = instance
                }
            }
            loaded.releaseSourceData()
            Log.i("Quizman3D", "Loaded rig v2: ${clips.keys}; mouth=$mouthInstance eyes=$eyeInstance")
        }

        fun render(now: Long, dt: Float) {
            if (!ui.isReadyToRender || closed) return
            resources.asyncUpdateLoad()
            // Never draw a centred fallback frame before the layout slot is measured.
            val bounds = if (framingBounds != null) {
                framingBounds!!.takeIf { it.width > 0f && it.height > 0f } ?: return
            } else {
                if (width <= 0 || height <= 0) return
                Rect(0f, 0f, width.toFloat(), height.toFloat())
            }
            asset?.let { model ->
                while (true) {
                    val entity = model.popRenderable()
                    if (entity == 0) break
                    scene.addEntity(entity)
                }
            }
            val animation = animator
            if (animation != null) {
                val frame = director.advance(snapshot, speaking, viseme, dt, durations)
                if (frame.clip != loggedClip) {
                    loggedClip = frame.clip
                    Log.d("Quizman3D", "Clip=${frame.clip} shot=${frame.shot}")
                }
                clips[frame.clip]?.let { animation.applyAnimation(it, frame.seconds) }
                if (frame.blend < 1f) frame.previousClip?.let(clips::get)?.let {
                    animation.applyCrossFade(it, frame.previousSeconds, frame.blend)
                }
                animation.updateBoneMatrices()
                val rm = engine.renderableManager
                if (mouthInstance != 0) { morphWeight[0] = frame.mouth; rm.setMorphWeights(mouthInstance, morphWeight, 0) }
                if (eyeInstance != 0) { morphWeight[0] = frame.blink; rm.setMorphWeights(eyeInstance, morphWeight, 0) }
                val targetY: Double
                val distance: Double
                val aspect = bounds.width.toDouble() / bounds.height.coerceAtLeast(1f)
                // Off-axis projection keeps the actor centred in its layout slot, while
                // rendering hands across the whole screen without a horizontal crop.
                val near = .05
                val unitsPerPixel = 2.0 * near * tan(Math.toRadians(17.0)) / bounds.height
                // Full surface, with no clipping to the actor's layout slot.
                view.viewport = Viewport(0, 0, renderWidth, renderHeight)
                camera.setProjection(Camera.Projection.PERSPECTIVE,
                    -bounds.center.x * unitsPerPixel, (width - bounds.center.x) * unitsPerPixel,
                    -(height - bounds.center.y) * unitsPerPixel, bounds.center.y * unitsPerPixel, near, 100.0)
                when (frame.shot) {
                    HostDirector.Shot.FULL -> { targetY = 4.0; distance = 15.5 * max(1.0, .45 / aspect) }
                    HostDirector.Shot.MEDIUM -> { targetY = 5.4; distance = 10.2 * max(1.0, .65 / aspect) }
                    HostDirector.Shot.CLOSE -> { targetY = 6.0; distance = 5.8 * max(1.0, .65 / aspect) }
                }
                val ease = if (!cameraReady) 1.0 else if (snapshot?.paused == true) 0.0 else 1.0 - exp(-dt * 5.0)
                cameraY += (targetY - cameraY) * ease; cameraDistance += (distance - cameraDistance) * ease
                cameraReady = true
            }
            camera.lookAt(cameraDistance * -.08, cameraY + .1, cameraDistance, 0.0, cameraY, 0.0, 0.0, 1.0, 0.0)
            swapChain?.let { chain ->
                if (renderer.beginFrame(chain, now)) { renderer.render(view); renderer.endFrame() }
            }
        }

        fun close() {
            if (closed) { return }
            closed = true
            ui.detach()
            resources.asyncCancelLoad()
            asset?.let { scene.removeEntities(it.entities); loader.destroyAsset(it) }
            animator = null; asset = null; modelBuffer = null
            loader.destroy(); resources.destroy(); materials.destroyMaterials(); materials.destroy()
            lights.forEach { engine.destroyEntity(it); EntityManager.get().destroy(it) }
            scene.indirectLight = null
            engine.destroyIndirectLight(indirect)
            engine.destroyRenderer(renderer); engine.destroyView(view); engine.destroyScene(scene)
            engine.destroyCameraComponent(cameraEntity); EntityManager.get().destroy(cameraEntity)
            engine.destroy()
        }
    }

    companion object { @Volatile private var modelBytes: ByteArray? = null }
}
