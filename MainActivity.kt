package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin

// ==================== 设备兼容层 ====================

object DeviceCompatibility {
    private val manufacturer = Build.MANUFACTURER.lowercase()
    private val brand = Build.BRAND.lowercase()
    private val model = Build.MODEL.lowercase()
    private val device = Build.DEVICE.lowercase()

    /** 系统 API 级别。用字面量 36，避免依赖 Build.VERSION_CODES.BAKLAVA（需要 compileSdk 36 才有） */
    val apiLevel: Int get() = Build.VERSION.SDK_INT
    val isAndroid14Plus: Boolean get() = apiLevel >= 34
    val isAndroid15Plus: Boolean get() = apiLevel >= 35
    /** Android 16 = API 36。OriginOS 6 / HyperOS 3 / ColorOS 16 / MagicOS 10 都基于它 */
    val isAndroid16Plus: Boolean get() = apiLevel >= 36

    // ---------------- 厂商识别 ----------------

    /** 严格意义的华为（荣耀已独立，单独判） */
    val isHuawei: Boolean
        get() = manufacturer.contains("huawei") || brand.contains("huawei")
    val isHonor: Boolean
        get() = manufacturer.contains("honor") || brand.contains("honor")
    val isXiaomi: Boolean
        get() = manufacturer.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco")
    /** vivo / iQOO。iQOO 的 BRAND 是 iqoo、MANUFACTURER 是 vivo，两边都要判 */
    val isVivo: Boolean
        get() = manufacturer.contains("vivo") || brand.contains("vivo") || brand.contains("iqoo")
    val isIQOO: Boolean
        get() = brand.contains("iqoo") || model.contains("iqoo")
    val isOppo: Boolean
        get() = manufacturer.contains("oppo") || brand.contains("oppo") ||
                brand.contains("oneplus") || brand.contains("realme")
    val isOnePlus: Boolean
        get() = manufacturer.contains("oneplus") || brand.contains("oneplus")
    val isSamsung: Boolean
        get() = manufacturer.contains("samsung") || brand.contains("samsung")
    /** 中兴 / 努比亚 / 红魔 */
    val isZte: Boolean
        get() = manufacturer.contains("zte") || brand.contains("zte") ||
                brand.contains("nubia") || brand.contains("redmagic")
    val isMeizu: Boolean
        get() = manufacturer.contains("meizu") || brand.contains("meizu")

    /**
     * vivo X80 Pro 识别。骁龙版常见 DEVICE=V2145A、天玑版 V2144A，部分批次 MODEL 才是 "X80 Pro"。
     * 如果识别不到，看 Logcat 里 logInfo() 打出来的 MODEL / DEVICE 再补一条即可。
     */
    val isVivoX80Pro: Boolean
        get() = isVivo && (
                model.contains("x80 pro") || model.contains("x80pro") ||
                        device.startsWith("v2145") || device.startsWith("v2144") ||
                        model.startsWith("v2145") || model.startsWith("v2144")
                )

    /**
     * iQOO 15：2025-10 发布，OriginOS 6(Android 16) + 第五代骁龙8至尊版(SM8850)。
     * 国行型号 V2505A，海外/印度 PD2505F。三摄 50MP：主摄 f/1.88 + 超广角 f/2.05 + 3x 潜望 f/2.65。
     * ⚠️ 它没有独立的 2x 人像头，真实光学档位是 0.6x / 1x / 3x，别照抄 X80 Pro 的 2x。
     */
    val isIQOO15: Boolean
        get() = isVivo && (
                model.startsWith("v2505") || device.startsWith("v2505") ||
                        model.startsWith("pd2505") || device.startsWith("pd2505")
                )

    /** 蔡司多摄机型：每颗焦段都是独立物理摄像头，切档需要更长收敛时间 */
    val isZeissMultiCam: Boolean get() = isVivoX80Pro

    // ---------------- ROM 识别 ----------------

    enum class Rom(val label: String) {
        ORIGIN_OS("OriginOS"),
        HYPER_OS("HyperOS"),
        COLOR_OS("ColorOS"),
        MAGIC_OS("MagicOS"),
        EMUI("EMUI"),
        MY_OS("MyOS"),
        FLYME("Flyme"),
        ONE_UI("One UI"),
        OTHER("原生/其他")
    }

    /** 一次性 dump 全部系统属性并缓存，避免反复 exec 拖慢启动 */
    private val systemProps: Map<String, String> by lazy {
        try {
            val proc = ProcessBuilder("getprop").redirectErrorStream(true).start()
            val text = proc.inputStream.bufferedReader().use { it.readText() }
            proc.destroy()
            val map = HashMap<String, String>(192)
            for (raw in text.lineSequence()) {
                // 每行形如： [ro.build.version.sdk]: [36]
                val line = raw.trim()
                if (!line.startsWith("[")) continue
                val split = line.indexOf("]: [")
                if (split <= 1) continue
                map[line.substring(1, split)] = line.substring(split + 4).removeSuffix("]")
            }
            map
        } catch (e: Exception) {
            Log.w("Camera", "读取系统属性失败（仅影响 ROM 识别）", e)
            emptyMap()
        }
    }

    private fun prop(key: String): String = systemProps[key].orEmpty()

    private val detectedRom: Rom by lazy {
        when {
            prop("ro.vivo.os.version").isNotBlank() || prop("ro.vivo.os.name").isNotBlank() -> Rom.ORIGIN_OS
            prop("ro.miui.ui.version.name").isNotBlank() -> Rom.HYPER_OS
            prop("ro.build.version.opporom").isNotBlank() -> Rom.COLOR_OS
            prop("ro.build.version.magic").isNotBlank() -> Rom.MAGIC_OS
            prop("ro.build.version.emui").isNotBlank() -> Rom.EMUI
            isVivo -> Rom.ORIGIN_OS
            isXiaomi -> Rom.HYPER_OS
            isOppo -> Rom.COLOR_OS
            isHonor -> Rom.MAGIC_OS
            isHuawei -> Rom.EMUI
            isSamsung -> Rom.ONE_UI
            else -> Rom.OTHER
        }
    }

    val rom: Rom get() = detectedRom

    /** ROM 版本号，主要用于日志排查 */
    val romVersion: String
        get() = when (rom) {
            Rom.ORIGIN_OS -> prop("ro.vivo.os.version").ifBlank { prop("ro.vivo.os.name") }
            Rom.HYPER_OS -> prop("ro.miui.ui.version.name")
            Rom.COLOR_OS -> prop("ro.build.version.opporom")
            Rom.MAGIC_OS -> prop("ro.build.version.magic")
            Rom.EMUI -> prop("ro.build.version.emui")
            else -> ""
        }

    // ---------------- Android 16 相关 ----------------

    /**
     * Android 16(API 36) 起强制全屏边到边，targetSdk 36 的应用无法再 opt-out。
     * 状态栏 / 手势条会盖在内容上，必须自己用 insets 让位。
     */
    val needsEdgeToEdgeInsets: Boolean get() = isAndroid16Plus

    /**
     * 内存页大小(字节)。Android 15 起出现 16KB 页设备，
     * 没按 16KB 对齐的 .so 在这类设备上加载会直接崩。
     * CameraX 及部分第三方 SDK 会带 .so，所以这里检测一下并打进日志。
     */
    val pageSizeBytes: Int by lazy {
        try {
            val proc = ProcessBuilder("getconf", "PAGE_SIZE").redirectErrorStream(true).start()
            val text = proc.inputStream.bufferedReader().use { it.readText().trim() }
            proc.destroy()
            text.toIntOrNull() ?: 4096
        } catch (e: Exception) {
            4096
        }
    }
    val is16KbPageDevice: Boolean get() = pageSizeBytes >= 16384

    // ---------------- 时序调校 ----------------

    /**
     * 绑定前是否等 PreviewView 完成布局。
     * OriginOS / HyperOS / ColorOS / MagicOS 上，未布局就 setSurfaceProvider 会黑屏或比例错乱；
     * Android 16 各家又改了一轮窗口时序，风险更高。
     * 这个循环只要 view 已经有尺寸就立刻返回，不会平白多等，所以统一打开。
     */
    val waitForLayoutBeforeBind: Boolean get() = true

    /** 下发手动参数后 HAL 的收敛等待(ms)。Android 16 的相机栈初始化更慢，需要多给一点 */
    val manualSettleDelayMs: Long
        get() = when {
            isAndroid16Plus -> 800L
            isVivo -> 700L
            isXiaomi -> 500L
            else -> 300L
        }

    /** 变焦切档后的等待(ms)。切物理镜头要重新收敛，潜望头尤其慢 */
    val zoomSettleDelayMs: Long
        get() = when {
            isAndroid16Plus -> 700L
            isVivo -> 650L
            isXiaomi -> 500L
            else -> 400L
        }

    /** 默认拍照尺寸兜底 */
    val defaultCaptureSize: Size get() = Size(4080, 3060)

    // ---------------- Camera2 能力探测 ----------------

    /**
     * 手动控制的安全上下限。
     * vivo / 天玑平台典型坑：SENSOR_INFO_EXPOSURE_TIME_RANGE 上报的极大值(比如 1s)，
     * 一旦超过 SENSOR_INFO_MAX_FRAME_DURATION，整个 CaptureRequest 会被 HAL 拒绝 → 预览卡死或全黑。
     */
    data class ManualLimits(
        val exposureMinNs: Long = 0L,
        val exposureMaxNs: Long = Long.MAX_VALUE,
        val maxFrameDurationNs: Long = Long.MAX_VALUE,
        val isoMin: Int = 100,
        val isoMax: Int = 6400,
        val evMin: Int = -2,
        val evMax: Int = 2,
        /** EV 补偿步长的分子/分母，等价于 android.util.Rational，但避免引入 API 21 的类 */
        val evStepNum: Int = 1,
        val evStepDen: Int = 3,
        val minFocusDistance: Float = -1f
    ) {
        val hasManualFocus: Boolean get() = minFocusDistance > 0f

        /** 把 UI 上的整数 EV 档位换算成 CONTROL_AE_EXPOSURE_COMPENSATION 需要的「步数」 */
        fun evToSteps(ev: Int): Int {
            val num = evStepNum.takeIf { it != 0 } ?: 1
            val den = evStepDen.takeIf { it != 0 } ?: 3
            return round(ev.toFloat() * den / num).toInt()
        }
    }

    fun cameraManager(context: Context): CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    /** 取指定朝向的相机 id */
    fun cameraIdOf(context: Context, lensFacing: Int): String? {
        return try {
            val manager = cameraManager(context)
            manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING) == lensFacing
            }
        } catch (e: Exception) {
            Log.w("Camera", "枚举相机失败", e)
            null
        }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun probeManualLimits(context: Context, camera: Camera?): ManualLimits {
        val cam = camera ?: return ManualLimits()
        return try {
            val cameraId = Camera2CameraInfo.from(cam.cameraInfo).cameraId
            val chars = cameraManager(context).getCameraCharacteristics(cameraId)

            val expRange = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
            val maxFrame = chars.get(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION)
                ?: Long.MAX_VALUE
            val isoRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
            val evRange = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
            val evStep = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
            val minFd = chars.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)

            // CONTROL_AE_COMPENSATION_STEP 返回 android.util.Rational，
            // 这里只取分子分母存成 Int，避免引入 android.util.Rational（API 21 才存在）
            val stepNum = evStep?.let { it.numerator.takeIf { n -> n != 0 } } ?: 1
            val stepDen = evStep?.let { it.denominator.takeIf { d -> d != 0 } } ?: 3

            val limits = ManualLimits(
                exposureMinNs = expRange?.lower ?: 0L,
                exposureMaxNs = (expRange?.upper ?: Long.MAX_VALUE).coerceAtMost(maxFrame),
                maxFrameDurationNs = maxFrame,
                isoMin = isoRange?.lower ?: 100,
                isoMax = isoRange?.upper ?: 6400,
                evMin = evRange?.lower ?: -2,
                evMax = evRange?.upper ?: 2,
                evStepNum = stepNum,
                evStepDen = stepDen,
                minFocusDistance = minFd ?: -1f
            )
            Log.d(
                "Camera",
                "手动范围[$cameraId]: 曝光=${limits.exposureMinNs / 1_000_000}ms~${limits.exposureMaxNs / 1_000_000}ms, " +
                        "帧上限=${maxFrame / 1_000_000}ms, ISO=${limits.isoMin}~${limits.isoMax}, " +
                        "EV=${limits.evMin}~${limits.evMax} step=$stepNum/$stepDen, " +
                        "minFD=${limits.minFocusDistance}"
            )
            limits
        } catch (e: Exception) {
            Log.w("Camera", "手动范围探测失败，使用保守默认值", e)
            ManualLimits()
        }
    }

    /**
     * 手动模式是否真的可用。
     * 只判断 AE_MODE_OFF 不够：部分 vivo 机型 HAL 声称支持但下发后不生效，
     * 必须同时具备 MANUAL_SENSOR 能力，且硬件等级高于 LEGACY / EXTERNAL。
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun isManualControlAllowed(context: Context, camera: Camera?): Boolean {
        if (camera == null) return false
        return try {
            val cameraId = Camera2CameraInfo.from(camera.cameraInfo).cameraId
            val chars = cameraManager(context).getCameraCharacteristics(cameraId)

            val level = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            if (level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY ||
                level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL
            ) {
                Log.w("Camera", "硬件等级不支持手动控制: level=$level")
                return false
            }

            val caps = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            val hasManualSensor =
                caps?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) == true
            val aeModes = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
            val hasAeOff = aeModes?.contains(CaptureRequest.CONTROL_AE_MODE_OFF) == true

            val ok = hasManualSensor && hasAeOff
            Log.d(
                "Camera",
                "手动控制[$cameraId]: MANUAL_SENSOR=$hasManualSensor, AE_OFF=$hasAeOff, level=$level"
            )
            ok
        } catch (e: Exception) {
            Log.w("Camera", "手动控制检测失败", e)
            false
        }
    }

    /**
     * 挑一个本机真实支持的 4:3 JPEG 尺寸，避免写死 4080x3060 在 vivo / 小米上落到不支持的
     * 分辨率导致拍照失败，或被 FALLBACK 规则甩到很低的分辨率。
     */
    fun pickCaptureSize(context: Context, lensFacing: Int, maxPixels: Int = 13_000_000): Size {
        return try {
            val id = cameraIdOf(context, lensFacing) ?: return defaultCaptureSize
            val chars = cameraManager(context).getCameraCharacteristics(id)
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = map?.getOutputSizes(ImageFormat.JPEG)?.toList()
                ?: return defaultCaptureSize

            val best = sizes
                .filter { it.width * it.height <= maxPixels }
                .filter { abs(it.width.toFloat() / it.height - 4f / 3f) < 0.02f }
                .maxByOrNull { it.width * it.height }
                ?: sizes.filter { it.width * it.height <= maxPixels }
                    .maxByOrNull { it.width * it.height }
                ?: defaultCaptureSize
            Log.d("Camera", "拍照尺寸[$id]: ${best.width}x${best.height}（候选 ${sizes.size} 个）")
            best
        } catch (e: Exception) {
            Log.w("Camera", "拍照尺寸探测失败", e)
            defaultCaptureSize
        }
    }

    // ---------------- 原厂镜头参数探测 ----------------

    enum class LensKind(val label: String) {
        ULTRA_WIDE("超广角"),
        WIDE("主摄"),
        TELE("长焦"),
        SUPER_TELE("潜望"),
        UNKNOWN("")
    }

    /**
     * 一颗物理镜头的出厂光学参数。
     * focal / sensor 都直接来自 HAL 上报的 CameraCharacteristics，即原厂相机 App 用的同一份数据。
     */
    data class LensProfile(
        val id: String,
        val focalLengthMm: Float,   // 实际焦距(mm)
        val sensorWidthMm: Float,   // 传感器物理宽度(mm)
        val equiv35mm: Float,       // 35mm 等效焦距 = focal / sensorWidth * 36
        val fovDeg: Float,          // 水平视场角
        val aperture: Float?,       // 光圈 F 值，读不到为 null
        val zoomFactor: Float,      // 相对主摄(≈24mm 等效)的倍率，即原厂 App 上显示的那个数字
        val isOptical: Boolean = true
    ) {
        val kind: LensKind
            get() = when {
                equiv35mm < 20f -> LensKind.ULTRA_WIDE
                equiv35mm <= 35f -> LensKind.WIDE
                equiv35mm <= 100f -> LensKind.TELE
                else -> LensKind.SUPER_TELE
            }

        /** 给 UI 用的短标签，如 "15mm 超广角" */
        val shortLabel: String
            get() = buildString {
                append(equiv35mm.roundToInt()).append("mm")
                kind.label.takeIf { it.isNotEmpty() }?.let { append(" ").append(it) }
            }
    }

    /** 算出来的倍率吸附到这些常用档位，跟原厂 App 显示的数字对齐 */
    private val NICE_STOPS = listOf(
        0.5f, 0.6f, 0.7f, 0.8f, 1f, 1.5f, 2f, 2.5f, 3f, 3.5f, 4f, 5f, 6f, 10f
    )

    /** 主摄的 35mm 等效焦距基准（业界惯例 24mm 上下） */
    private const val BASE_EQUIV_MM = 24f

    private fun snapToNiceStop(v: Float): Float {
        val best = NICE_STOPS.minByOrNull { abs(it - v) } ?: return v
        // 只在 18% 误差内吸附，避免把 3.4x 硬掰成 3x 这种明显偏差
        return if (abs(best - v) <= v * 0.18f) best else (v * 10f).roundToInt() / 10f
    }

    /**
     * 读取本机全部物理镜头的出厂光学参数。
     * 优先用逻辑摄像头的 physicalCameraIds（API 28+）拿到每一颗物理头；
     * 拿不到就退化成逻辑摄像头自身的焦距。
     */
    @SuppressLint("NewApi")
    fun probeLenses(context: Context, lensFacing: Int): List<LensProfile> {
        return try {
            val manager = cameraManager(context)
            val raw = mutableListOf<LensProfile>()

            for (id in manager.cameraIdList) {
                val chars = try {
                    manager.getCameraCharacteristics(id)
                } catch (e: Exception) {
                    continue
                }
                if (chars.get(CameraCharacteristics.LENS_FACING) != lensFacing) continue

                // 一颗逻辑头可能对应多颗物理头（多摄模组），逐个读
                val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        chars.physicalCameraIds
                    } catch (e: Exception) {
                        emptySet<String>()
                    }
                } else emptySet()

                val idsToRead = if (physicalIds.isNotEmpty()) physicalIds else setOf(id)

                for (pid in idsToRead) {
                    val pc = try {
                        manager.getCameraCharacteristics(pid)
                    } catch (e: Exception) {
                        continue
                    }
                    val fl = pc.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                        ?.firstOrNull() ?: continue
                    val phys = pc.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                        ?: continue
                    if (phys.width <= 0f || fl <= 0f) continue

                    val equiv = fl * 36f / phys.width
                    // 同一颗头可能被多个逻辑头重复上报，按等效焦距去重
                    if (raw.any { abs(it.equiv35mm - equiv) < 0.8f }) continue

                    val fov = Math.toDegrees(
                        2.0 * atan((phys.width / (2f * fl)).toDouble())
                    ).toFloat()
                    val ap = pc.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                        ?.firstOrNull()

                    raw += LensProfile(
                        id = pid,
                        focalLengthMm = fl,
                        sensorWidthMm = phys.width,
                        equiv35mm = equiv,
                        fovDeg = fov,
                        aperture = ap,
                        zoomFactor = equiv // 占位，下面统一按基准换算
                    )
                }
            }

            if (raw.isEmpty()) return emptyList()

            // 以最接近 24mm 的那颗作为 1x 基准（原厂 App 也是拿主摄当 1x）
            val base = raw.minByOrNull { abs(it.equiv35mm - BASE_EQUIV_MM) } ?: raw.first()
            val result = raw
                .map {
                    val factor = if (base.equiv35mm > 0f) it.equiv35mm / base.equiv35mm else 1f
                    it.copy(zoomFactor = snapToNiceStop(factor))
                }
                .distinctBy { it.zoomFactor }
                .sortedBy { it.zoomFactor }

            Log.d(
                "Camera",
                "原厂镜头参数(基准 ${base.equiv35mm.roundToInt()}mm): " +
                        result.joinToString(", ") {
                            "${it.zoomFactor}x=${it.equiv35mm.roundToInt()}mm" +
                                    "(f=${it.focalLengthMm} sens=${it.sensorWidthMm}mm" +
                                    "${it.aperture?.let { a -> " F/$a" } ?: ""})"
                        }
            )
            result
        } catch (e: Exception) {
            Log.w("Camera", "镜头参数探测失败", e)
            emptyList()
        }
    }

    fun logInfo() {
        Log.d(
            "Camera",
            "设备: ${Build.MANUFACTURER} / ${Build.BRAND} / ${Build.MODEL} / ${Build.DEVICE}"
        )
        Log.d(
            "Camera",
            "系统: ${rom.label} ${romVersion.ifBlank { "?" }} / Android ${Build.VERSION.RELEASE} (API $apiLevel)"
        )
        Log.d(
            "Camera",
            "识别: vivo=$isVivo(iqoo=$isIQOO, iqoo15=$isIQOO15, x80pro=$isVivoX80Pro), " +
                    "xiaomi=$isXiaomi, huawei=$isHuawei, honor=$isHonor, " +
                    "oppo=$isOppo(1+=$isOnePlus), samsung=$isSamsung, zte=$isZte"
        )
        Log.d(
            "Camera",
            "参数: 页大小=${pageSizeBytes}B(16KB设备=$is16KbPageDevice), " +
                    "边到边=$needsEdgeToEdgeInsets, 等布局=$waitForLayoutBeforeBind, " +
                    "手动=${manualSettleDelayMs}ms, 变焦=${zoomSettleDelayMs}ms"
        )
        if (is16KbPageDevice) {
            Log.w(
                "Camera",
                "16KB 页设备：若打包含未对齐的 .so 会崩溃，用 Build > Analyze APK 检查 lib/ 目录"
            )
        }
    }
}

// ==================== 数据模型 ====================

data class CapturedMedia(
    val uri: Uri,
    val isVideo: Boolean,
    val timestamp: Long
)

enum class CaptureMode(val label: String) {
    PRO("专业"),
    VIDEO("录像"),
    PHOTO("拍照")
}

enum class ProParam { EV, SHUTTER, ISO, WB, FOCUS }

/** 手动曝光时帧时长相对曝光时间留出的余量（ns），1ms。vivo / 天玑 HAL 要求 frame >= exposure */
private const val FRAME_OVERHEAD_NS = 1_000_000L

/**
 * 档位按钮的倍率上限。超过这个值的纯数码变焦（iQOO 15 能到 100x）不生成按钮，
 * 想拉到极限可以用标尺拖。20x 以内基本还有可用性。
 */
private const val MAX_USEFUL_STOP = 20f

/**
 * 关于页展示的版本号。发新版时改这一处即可（两处显示都用它）。
 * 注意：更新检查判断用的是 build.gradle 里的 versionCode，跟这个字符串无关。
 */
private const val APP_VERSION_NAME = "3.0.3"

// ==================== 版本更新检查 ====================

/**
 * 必须是 HTTPS（Android 9+ 默认禁止明文 HTTP）。
 * 当前指向：GitHub 仓库 hmjs2001/silky-camera-update，经 jsDelivr CDN 加速。
 *
 * ⚠️ jsDelivr 对 @main 有最长 7 天缓存。每次改完 update.json，务必在浏览器打开这个地址刷新一次：
 *    https://purge.jsdelivr.net/gh/hmjs2001/silky-camera-update@main/update.json
 *    否则手机拿到的还是旧内容（版本号对不上、或 apkUrl 还是占位符）。
 */
private const val UPDATE_JSON_URL =
    "https://cdn.jsdelivr.net/gh/hmjs2001/silky-camera-update@main/update.json"

data class UpdateInfo(
    val versionCode: Long,      // 整数版本号，必须递增，用来判断是否更新
    val versionName: String,    // 展示用，如 "3.1"
    val apkUrl: String,         // APK 下载地址，点「去下载」会跳到这里
    val sizeBytes: Long,        // 包体积，0 表示不显示
    val changelog: String,      // 更新说明，换行用 \n
    val force: Boolean          // true = 强制更新，用户不能跳过
)

object AppUpdate {
    private const val PREFS = "app_update"
    private const val KEY_SKIP = "skip_version_code"
    private const val TIMEOUT_MS = 5000

    /**
     * 当前 App 的 versionCode。
     * 比较一定要用这个整数，别用 versionName 字符串比（"3.10" < "3.9" 会出错）。
     */
    fun currentVersionCode(context: Context): Long {
        return try {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        } catch (e: Exception) {
            Log.w("Update", "读取当前版本失败", e)
            0L
        }
    }

    /** 用户点过「跳过此版本」的版本号 */
    fun skippedCode(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_SKIP, 0L)

    fun skipVersion(context: Context, code: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_SKIP, code).apply()
    }

    /** 清掉跳过记录（发新版时如果怕老用户被卡住可以用） */
    fun clearSkip(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_SKIP).apply()
    }

    /**
     * 拉取版本文件。失败一律静默返回 null——更新检查不能影响 App 正常使用。
     */
    suspend fun fetchUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(UPDATE_JSON_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("Cache-Control", "no-cache")
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w("Update", "版本文件返回 ${conn.responseCode}")
                return@withContext null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val info = UpdateInfo(
                versionCode = json.optLong("versionCode", 0L),
                versionName = json.optString("versionName", ""),
                apkUrl = json.optString("apkUrl", ""),
                sizeBytes = json.optLong("sizeBytes", 0L),
                changelog = json.optString("changelog", ""),
                force = json.optBoolean("force", false)
            )
            if (info.versionCode <= 0L || info.apkUrl.isBlank()) {
                Log.w("Update", "版本文件字段不合法: $body")
                return@withContext null
            }
            info
        } catch (e: Exception) {
            Log.w("Update", "检查更新失败（已忽略）", e)
            null
        } finally {
            conn?.disconnect()
        }
    }

    fun formatSize(bytes: Long): String = when {
        bytes <= 0L -> ""
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024f / 1024f)
        else -> "%.0f KB".format(bytes / 1024f)
    }
}

/** 更新提示弹窗：只提示，下载交给浏览器 / 系统下载器 */
@Composable
fun UpdateDialog(
    info: UpdateInfo,
    onDismiss: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = { if (!info.force) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !info.force, dismissOnClickOutside = !info.force)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1C1C1E))
                .padding(20.dp)
        ) {
            Text(
                "发现新版本 ${info.versionName.ifBlank { info.versionCode.toString() }}",
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (info.changelog.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(12.dp)
                ) {
                    info.changelog.split("\n").forEach { line ->
                        if (line.isNotBlank()) {
                            Text(
                                "• $line",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            val sizeText = AppUpdate.formatSize(info.sizeBytes)
            if (sizeText.isNotEmpty()) {
                Text("安装包大小约 $sizeText", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                "点击下载后会跳转到浏览器，下载完成手动安装即可。",
                color = Color.Gray, fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(18.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!info.force) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(23.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { onSkip() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("跳过此版本", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(Color(0xFFFFA000))
                        .clickable {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl))
                                )
                            } catch (e: Exception) {
                                Log.e("Update", "打开下载链接失败", e)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("去下载", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceCompatibility.logInfo()

        // 全屏 + 透明系统栏。全部用 Android 框架原生 API，
        // 不依赖 androidx.core 的 WindowCompat，避免额外依赖问题。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ 才有，Android 15/16 上边到边靠它
            window.setDecorFitsSystemWindows(false)
        }
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 关掉系统自动加的对比度蒙层，否则手势条后面会有一条灰底
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        setContent {
            MaterialTheme {
                CameraApp()
            }
        }
    }
}

@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun CameraApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var hasPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> hasPermissions = result.values.all { it } }

    if (!hasPermissions) {
        LaunchedEffect(Unit) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("正在请求相机和麦克风权限...", color = Color.White)
        }
        return
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // 更新提示：每次打开 App 检查一次，有新版本才弹
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    LaunchedEffect(Unit) {
        // 放在 IO 线程 + 有超时，弱网不会拖慢相机启动
        Log.d("Update", "开始检查: $UPDATE_JSON_URL")
        val info = AppUpdate.fetchUpdate(context)
        if (info == null) {
            Log.w("Update", "没取到版本信息，已跳过（看上面失败原因）")
            return@LaunchedEffect
        }
        val current = AppUpdate.currentVersionCode(context)
        val skipped = AppUpdate.skippedCode(context)
        Log.d("Update", "当前=$current, 服务器=${info.versionCode}, 已跳过=$skipped")
        when {
            info.versionCode <= current ->
                Log.d("Update", "不弹窗：服务器版本号不大于本地，请把 JSON 的 versionCode 调大")
            info.versionCode == skipped ->
                Log.d("Update", "不弹窗：用户已跳过此版本（卸载重装或发新版本可重置）")
            else -> updateInfo = info
        }
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var mode by remember { mutableStateOf(CaptureMode.PHOTO) }

    var selectedZoom by remember { mutableFloatStateOf(1.0f) }
    var currentRatio by remember { mutableFloatStateOf(1.0f) }
    var displayedRatio by remember { mutableFloatStateOf(1.0f) }
    var linearMap by remember { mutableStateOf<Map<Float, Float>>(emptyMap()) }
    var scanDone by remember { mutableStateOf(false) }
    var hasCalibrated by remember { mutableStateOf(false) }
    var zoomJob by remember { mutableStateOf<Job?>(null) }

    var showRuler by remember { mutableStateOf(false) }
    var lastRulerInteraction by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }

    var highResMode by remember { mutableStateOf(false) }
    var flashMode by remember { mutableIntStateOf(0) }
    var showAbout by remember { mutableStateOf(false) }
    var showReward by remember { mutableStateOf(false) }

    var proIso by remember { mutableIntStateOf(0) }
    var proShutterNs by remember { mutableLongStateOf(0L) }
    var proEv by remember { mutableIntStateOf(0) }
    var proFocus by remember { mutableFloatStateOf(-1f) }
    var proWb by remember { mutableIntStateOf(0) }

    var manualControlSupported by remember { mutableStateOf(false) }
    // vivo 适配：手动参数的安全上下限（曝光/帧时长/ISO/EV步长/对焦距离），绑定成功后探测
    var manualLimits by remember { mutableStateOf<DeviceCompatibility.ManualLimits?>(null) }

    var flipAngle by remember { mutableFloatStateOf(0f) }
    var isFlipping by remember { mutableStateOf(false) }
    var pendingLensFacing by remember { mutableIntStateOf(-1) }
    var cameraReadyAfterSwitch by remember { mutableStateOf(true) }

    val isFront = lensFacing == CameraSelector.LENS_FACING_FRONT

    val sessionMedia = remember { mutableStateListOf<CapturedMedia>() }
    var showGallery by remember { mutableStateOf(false) }

    // vivo 适配：变焦范围改成从 zoomState 实时读，不再写死 0.6x~10x。
    // X80 Pro 的 0.6x 超广角 / 5x 潜望能不能被 HAL 正常上报因机而异，读出来才准。
    var minZoomRatio by remember { mutableFloatStateOf(0.6f) }
    var maxZoomRatio by remember { mutableFloatStateOf(10.0f) }
    val minRatio = minZoomRatio
    val maxRatio = maxZoomRatio

    // 原厂镜头参数：每次绑定相机时读取，变焦档位/标定/吸附都以它为准
    var lensProfiles by remember { mutableStateOf<List<DeviceCompatibility.LensProfile>>(emptyList()) }
    val defaultStops = listOf(0.6f, 1.0f, 2.0f, 5.0f, 10.0f)
    var zoomStops by remember { mutableStateOf(defaultStops) }
    // 真实物理头的倍率（光学档位），与按钮档位分开：
    // 按钮档位 = 光学档位 ∪ 常用档位，保证 2x 这类没有物理头的档位按钮依然存在
    var opticalStops by remember { mutableStateOf<List<Float>>(emptyList()) }
    // 探测失败时为 false，此时退回写死的档位，行为跟以前一致
    var zoomStopsFromHardware by remember { mutableStateOf(false) }

    /** 某个倍率是不是落在真实物理镜头上（用于显示"光学"标记、高画质模式放行） */
    fun isOpticalStop(z: Float): Boolean =
        opticalStops.any { abs(it - z) < 0.01f }

    /** 档位按钮上的文字：0.6x 显示 ".6x"，2.5x 显示 "2.5x"，整数档显示 "2x" */
    fun formatStop(z: Float): String =
        if (z < 1f) ".${(z * 10).roundToInt()}x"
        else if (abs(z - z.roundToInt()) < 0.05f) "${z.roundToInt()}x"
        else "${(z * 10).roundToInt() / 10f}x"

    /** 判定为"就在这颗头上（原生光学输出）"的容差：相对该镜头倍率的 ±10% */
    fun onThisLens(ratio: Float, lensZoom: Float): Boolean =
        abs(lensZoom - ratio) <= lensZoom * 0.10f

    /**
     * 当前倍率正好落在哪颗物理镜头上（原生光学输出），不在任何头上时返回 null。
     * 注意必须"够近"才算，不能简单取最近的头——否则 3.5x 会被算到 5x 潜望头上。
     */
    fun opticalLensAt(ratio: Float): DeviceCompatibility.LensProfile? =
        lensProfiles.firstOrNull { onThisLens(ratio, it.zoomFactor) }

    /**
     * 当前实际在干活的物理镜头：取倍率不超过当前值、且最接近的那颗（向下取）。
     * 比如镜头是 1x / 5x，变焦到 3.5x 时实际仍是主摄(1x)在做数码裁切，而不是潜望。
     * 低于最广的那颗时（如 0.5x 但最宽只有 0.6x）就用最宽那颗。
     */
    fun activeLensAt(ratio: Float): DeviceCompatibility.LensProfile? {
        if (lensProfiles.isEmpty()) return null
        return lensProfiles.filter { it.zoomFactor <= ratio + 0.001f }
            .maxByOrNull { it.zoomFactor }
            ?: lensProfiles.minByOrNull { it.zoomFactor }
    }

    /**
     * 光学吸附：物理镜头之间靠数字变焦插值，画质会掉。
     * 松手/点档位时如果离某颗物理头很近(±10%)，直接吸到那颗头上，走原生光学输出。
     */
    fun snapToOptical(ratio: Float): Float {
        if (lensProfiles.isEmpty()) return ratio
        val best = lensProfiles.minByOrNull { abs(it.zoomFactor - ratio) } ?: return ratio
        return if (onThisLens(ratio, best.zoomFactor)) best.zoomFactor else ratio
    }

    val isUltraWide = currentRatio < 0.95f && minZoomRatio < 0.95f && !isFront
    val ultraWidePaint = remember {
        Paint().apply {
            colorFilter = ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        1.25f, 0f, 0f, 0f, -18f,
                        0f, 1.25f, 0f, 0f, -18f,
                        0f, 0f, 1.25f, 0f, -18f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
    }

    /**
     * 倍率 → linearZoom。
     * 标定点是真实物理镜头的倍率，两点之间按 log 插值（跟视觉效果一致）。
     */
    fun ratioToLinear(ratio: Float): Float {
        if (linearMap.isEmpty()) return 0f
        val keys = linearMap.keys.sorted()
        val r = ratio.coerceIn(keys.first(), keys.last())
        if (r <= keys.first()) return linearMap[keys.first()] ?: 0f
        if (r >= keys.last()) return 1.0f
        for (i in 0 until keys.size - 1) {
            val r1 = keys[i]
            val r2 = keys[i + 1]
            if (r in r1..r2) {
                val t = (ln(r) - ln(r1)) / (ln(r2) - ln(r1))
                val l1 = linearMap[r1] ?: 0f
                val l2 = linearMap[r2] ?: 1f
                return l1 + (l2 - l1) * t
            }
        }
        return 1.0f
    }

    /** 取最近的可点档位（用的是原厂镜头倍率，探测失败时退回默认档位） */
    fun nearestStop(actualRatio: Float): Float =
        zoomStops.minByOrNull { abs(it - actualRatio) } ?: 1.0f

    /**
     * 清空 Camera2 手动参数。
     * 退出专业模式时必须调用：vivo 上如果 AE_MODE_OFF / 手动 ISO 残留在 repeating request 里，
     * 回到拍照/录像模式画面会一直过曝或全黑。
     */
    fun resetProParams() {
        val cam = camera ?: return
        try {
            Camera2CameraControl.from(cam.cameraControl).clearCaptureRequestOptions()
            Log.d("Camera", "已清空 Camera2 手动参数")
        } catch (e: Exception) {
            Log.w("Camera", "清空手动参数失败", e)
        }
    }

    fun applyProParams(iso: Int, shutter: Long, ev: Int, focus: Float, wb: Int) {
        val cam = camera ?: return
        if (mode != CaptureMode.PRO) return
        if (!manualControlSupported) return

        // vivo 适配：所有手动值都按 HAL 上报的范围裁剪后再下发
        val limits = manualLimits ?: DeviceCompatibility.ManualLimits()

        try {
            val c2 = Camera2CameraControl.from(cam.cameraControl)
            val b = CaptureRequestOptions.Builder()

            val isoManual = iso > 0
            val shutterManual = shutter > 0L
            val focusManual = focus >= 0f && limits.hasManualFocus

            if (isoManual || shutterManual) {
                b.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_OFF
                )

                if (isoManual) {
                    val realIso = iso.coerceIn(limits.isoMin, limits.isoMax)
                    b.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, realIso)
                }

                if (shutterManual) {
                    // ① 曝光时间不能超过 HAL 的帧时长上限，否则整个 request 被拒 → 预览卡死/全黑
                    val expNs = shutter.coerceIn(limits.exposureMinNs, limits.exposureMaxNs)
                    // ② 帧时长必须 >= 曝光时间，否则同样非法；vivo / 天玑平台对此特别严格
                    val frameNs = (expNs + FRAME_OVERHEAD_NS)
                        .coerceAtMost(limits.maxFrameDurationNs)
                        .coerceAtLeast(expNs)
                    b.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, expNs)
                    b.setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, frameNs)
                } else {
                    // 只锁 ISO 时不限制帧时长，交给 HAL 自动
                    b.setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, 0L)
                }

                // ③ AE 关掉的时候下发 EV 是无效且有害的（部分 vivo HAL 会直接忽略整包参数）
                b.setCaptureRequestOption(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0)
            } else {
                b.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )
                // ④ EV 的单位是「补偿步长」而不是 EV。vivo 的步长可能是 1/2 或 1/3，
                //    直接用 UI 的整数档会导致 ±2 档实际只生效 ±1 档（或超出范围被裁剪）
                val realEv = ev.coerceIn(limits.evMin, limits.evMax)
                b.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                    limits.evToSteps(realEv)
                )
            }

            if (focusManual) {
                b.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF
                )
                b.setCaptureRequestOption(
                    CaptureRequest.LENS_FOCUS_DISTANCE,
                    focus.coerceIn(0f, limits.minFocusDistance)
                )
            } else {
                b.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            }

            val wbMode = when (wb) {
                0 -> CaptureRequest.CONTROL_AWB_MODE_AUTO
                1 -> CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT
                2 -> CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT
                3 -> CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
                4 -> CaptureRequest.CONTROL_AWB_MODE_SHADE
                else -> CaptureRequest.CONTROL_AWB_MODE_AUTO
            }
            b.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, wbMode)

            c2.setCaptureRequestOptions(b.build())
            Log.d(
                "Camera",
                "专业参数: ISO=$iso, S=$shutter, EV=$ev(${limits.evToSteps(ev)}步), F=$focus, WB=$wb"
            )
        } catch (e: Exception) {
            Log.e("Camera", "应用专业参数失败", e)
        }
    }

    LaunchedEffect(Unit) {
        try {
            // vivo 适配：getInstance().get() 会阻塞（vivo 上 HAL 初始化更慢），
            // 必须切到 IO 线程，否则 LaunchedEffect 默认跑在主线程上会掉帧甚至 ANR
            withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }
            Log.d("Camera", "CameraX 初始化完成")
        } catch (e: Exception) {
            Log.e("Camera", "获取 Provider 失败", e)
        }
    }

    LaunchedEffect(lensFacing, highResMode) {
        var waited = 0
        while (previewViewRef == null && waited < 3000) {
            delay(50); waited += 50
        }
        val pv = previewViewRef ?: return@LaunchedEffect

        if (DeviceCompatibility.waitForLayoutBeforeBind) {
            var waitLayout = 0
            while ((pv.width <= 0 || pv.height <= 0) && waitLayout < 2000) {
                delay(30)
                waitLayout += 30
            }
        }

        if (!hasCalibrated) scanDone = false
        delay(100)

        try {
            // 每次打开/切换相机都重新读取原厂镜头参数（焦距、传感器尺寸、光圈），
            // 后面的变焦档位、标定、吸附全部基于这份真实光学数据
            val lenses = withContext(Dispatchers.IO) {
                DeviceCompatibility.probeLenses(context, lensFacing)
            }
            lensProfiles = lenses
            // 光学档位：只记真实物理头的倍率，供吸附 / 高画质模式 / 镜头标签使用。
            // 按钮档位不直接用它（见下方），因为很多机器没有 2x 物理头，会导致 2x 按钮消失。
            opticalStops = if (lenses.size >= 2) {
                lenses.map { it.zoomFactor }.distinct().sorted()
            } else emptyList()
            zoomStopsFromHardware = opticalStops.isNotEmpty()
            Log.d("Camera", "光学档位: ${opticalStops.joinToString("/").ifEmpty { "未探测到" }}")

            // vivo 适配：getInstance().get() 会阻塞（vivo 上 HAL 初始化更慢），
            // 必须切到 IO 线程，否则主线程掉帧甚至 ANR
            val provider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(pv.surfaceProvider)
            }

            val resolutionSelector = if (highResMode) {
                ResolutionSelector.Builder()
                    .setAllowedResolutionMode(ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
                    .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .build()
            } else {
                // vivo 适配：不再写死 4080x3060，改成问 HAL 要一个它真正支持的 4:3 JPEG 尺寸
                val targetSize = if (DeviceCompatibility.isXiaomi) {
                    Size(4000, 3000)
                } else {
                    DeviceCompatibility.pickCaptureSize(context, lensFacing)
                }
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(targetSize, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                    )
                    .build()
            }

            val ic = ImageCapture.Builder()
                .setResolutionSelector(resolutionSelector)
                .setCaptureMode(
                    if (highResMode) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                    else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                )
                .build()

            val vc = VideoCapture.withOutput(Recorder.Builder().build())

            val baseSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            provider.unbindAll()
            var bound = false

            try {
                camera = provider.bindToLifecycle(lifecycleOwner, baseSelector, preview, ic, vc)
                imageCapture = ic
                videoCapture = vc
                bound = true
            } catch (e: Exception) {
                Log.w("Camera", "三流绑定失败", e)
            }

            if (!bound) {
                try {
                    provider.unbindAll()
                    camera = provider.bindToLifecycle(lifecycleOwner, baseSelector, preview, ic)
                    imageCapture = ic
                    videoCapture = null
                } catch (e: Exception) {
                    Log.e("Camera", "两流绑定也失败", e)
                }
            }

            when (flashMode) {
                0 -> ic.flashMode = ImageCapture.FLASH_MODE_OFF
                1 -> ic.flashMode = ImageCapture.FLASH_MODE_AUTO
                2 -> ic.flashMode = ImageCapture.FLASH_MODE_ON
                3 -> ic.flashMode = ImageCapture.FLASH_MODE_OFF
            }

            manualControlSupported = DeviceCompatibility.isManualControlAllowed(context, camera)
            manualLimits = DeviceCompatibility.probeManualLimits(context, camera)

            // vivo 适配：变焦范围实时读取，不再写死 0.6x ~ 10x
            // （X80 Pro 的 0.6x 超广角能不能被 HAL 上报因机而异；读不到就退回旧默认值）
            camera?.cameraInfo?.zoomState?.value?.let { zs ->
                if (zs.minZoomRatio > 0f && zs.minZoomRatio <= 1.0f) {
                    minZoomRatio = zs.minZoomRatio
                }
                if (zs.maxZoomRatio >= 1f) {
                    maxZoomRatio = zs.maxZoomRatio
                }
            }
            Log.d("Camera", "变焦范围: ${minZoomRatio}x ~ ${maxZoomRatio}x")

            // 按钮档位 = 光学档位 ∪ 常用档位，再按本机变焦范围过滤。
            // 这样即便某台机器没有 2x 物理头（很常见），2x 按钮依然保留，只是走数码变焦。
            val merged = mutableListOf<Float>()
            merged.addAll(opticalStops)
            merged.addAll(defaultStops)
            merged.add(minZoomRatio)
            // 最大数码变焦也补一个档位，但只在合理范围内。
            // iQOO 15 这类机器 zoomState 会报 100x，做成按钮没意义还挤爆一排。
            if (maxZoomRatio <= MAX_USEFUL_STOP) merged.add(maxZoomRatio)
            zoomStops = merged.distinct()
                .filter { it >= minZoomRatio - 0.01f && it <= maxZoomRatio + 0.01f }
                .sorted()
                .ifEmpty { defaultStops }
            Log.d(
                "Camera",
                "按钮档位: ${zoomStops.joinToString("/")} " +
                        "(光学=${opticalStops.size}个, 硬件上限=${maxZoomRatio}x)"
            )

            if (!hasCalibrated && lensFacing == CameraSelector.LENS_FACING_BACK) {
                val cam = camera ?: return@LaunchedEffect
                // vivo 适配：切物理镜头（超广角 / 2x / 5x 潜望）收敛更慢，等待时间按机型放宽
                val settle = DeviceCompatibility.zoomSettleDelayMs
                // 标定点 = 原厂光学档位，过滤掉这台机器到不了的
                val targets = zoomStops.filter {
                    it >= minZoomRatio - 0.01f && it <= maxZoomRatio + 0.01f
                }.ifEmpty { listOf(1.0f) }
                val result = mutableMapOf<Float, Float>()
                val maxZ = maxZoomRatio
                val minZ = minZoomRatio

                Log.d("Camera", "开始标定 ${targets.size} 个光学档位: ${targets.joinToString("/")}")

                for (t in targets) {
                    cam.cameraControl.setZoomRatio(t)
                    delay(settle)
                    val state = cam.cameraInfo.zoomState.value
                    val actualRatio = state?.zoomRatio ?: t
                    val linear = state?.linearZoom ?: 0f

                    if (t >= maxZ - 0.01f) {
                        result[t] = if (actualRatio >= maxZ * 0.95f) linear else 1.0f
                    } else {
                        result[t] = if (abs(actualRatio - t) < t * 0.2f) linear
                        else ((t - minZ) / (maxZ - minZ)).coerceIn(0f, 1f)
                    }
                }
                cam.cameraControl.setZoomRatio(1.0f)
                delay(settle)
                linearMap = result
                hasCalibrated = true
                Log.d("Camera", "标定完成: " + result.entries.sortedBy { it.key }
                    .joinToString(", ") { "${it.key}x→${"%.3f".format(it.value)}" })
            }
            scanDone = true

            if (mode == CaptureMode.PRO && manualControlSupported) {
                delay(DeviceCompatibility.manualSettleDelayMs)
                applyProParams(proIso, proShutterNs, proEv, proFocus, proWb)
            }
        } catch (e: Exception) {
            Log.e("Camera", "相机绑定流程异常", e)
            e.printStackTrace()
        }
        cameraReadyAfterSwitch = true
    }

    LaunchedEffect(mode, camera, manualControlSupported) {
        if (mode == CaptureMode.PRO && camera != null && manualControlSupported) {
            delay(DeviceCompatibility.manualSettleDelayMs)
            applyProParams(proIso, proShutterNs, proEv, proFocus, proWb)
        } else {
            // vivo 适配：离开专业模式必须清空手动参数，
            // 否则 AE_MODE_OFF / 手动 ISO 会残留在 repeating request 里，画面一直全黑或过曝
            resetProParams()
        }
    }

    LaunchedEffect(camera, scanDone) {
        if (!scanDone) return@LaunchedEffect
        while (true) {
            camera?.cameraInfo?.zoomState?.value?.let {
                currentRatio = it.zoomRatio
                // vivo 适配：变焦上下限跟着实际会话刷新
                if (it.minZoomRatio > 0f && it.minZoomRatio <= 1.0f) {
                    minZoomRatio = it.minZoomRatio
                }
                if (it.maxZoomRatio >= 1f) {
                    maxZoomRatio = it.maxZoomRatio
                }
                if (!isDragging && (zoomJob == null || zoomJob?.isActive == false)) {
                    if (displayedRatio <= it.maxZoomRatio * 1.05f) {
                        displayedRatio = it.zoomRatio
                    }
                }
            }
            delay(50)
        }
    }

    LaunchedEffect(showRuler) {
        if (showRuler) {
            while (true) {
                delay(500)
                if (System.currentTimeMillis() - lastRulerInteraction > 5000) {
                    showRuler = false
                    break
                }
            }
        }
    }

    LaunchedEffect(flashMode, imageCapture, camera) {
        val ic = imageCapture ?: return@LaunchedEffect
        when (flashMode) {
            0 -> {
                ic.flashMode = ImageCapture.FLASH_MODE_OFF
                camera?.cameraControl?.enableTorch(false)
            }
            1 -> {
                ic.flashMode = ImageCapture.FLASH_MODE_AUTO
                camera?.cameraControl?.enableTorch(false)
            }
            2 -> {
                ic.flashMode = ImageCapture.FLASH_MODE_ON
                camera?.cameraControl?.enableTorch(false)
            }
            3 -> {
                ic.flashMode = ImageCapture.FLASH_MODE_OFF
                camera?.cameraControl?.enableTorch(true)
            }
        }
    }

    fun smoothZoomTo(targetRatio: Float) {
        val cam = camera ?: return
        if (!scanDone) return
        val endRatio = targetRatio.coerceIn(minRatio, maxRatio)
        val startRatio = displayedRatio
        val startLinear = cam.cameraInfo.zoomState.value?.linearZoom ?: 0f
        val endLinear = ratioToLinear(endRatio)

        zoomJob?.cancel()
        zoomJob = coroutineScope.launch {
            if (abs(startLinear - endLinear) < 0.005f) {
                cam.cameraControl.setLinearZoom(endLinear)
                displayedRatio = endRatio
                return@launch
            }
            val span = abs(startLinear - endLinear)
            val durationMs = (520f - span * 180f).coerceIn(340f, 520f).toLong()
            val steps = (durationMs / 20L).toInt().coerceAtLeast(1)
            val rangeLinear = endLinear - startLinear
            val rangeRatio = endRatio - startRatio
            val easing = CubicBezierEasing(0.55f, 0.1f, 0.3f, 1f)

            for (step in 1..steps) {
                val t = step.toFloat() / steps
                val eased = easing.transform(t)
                cam.cameraControl.setLinearZoom(startLinear + rangeLinear * eased)
                displayedRatio = startRatio + rangeRatio * eased
                delay(20)
            }
            cam.cameraControl.setLinearZoom(endLinear)
            displayedRatio = endRatio
        }
    }

    fun directSetRatio(targetRatio: Float) {
        val cam = camera ?: return
        if (!scanDone) return
        zoomJob?.cancel()

        // 高画质模式只允许落在真实光学头上（数码插值出来的中间画质没意义）
        val finalRatio = if (highResMode) {
            snapToOptical(targetRatio)
        } else {
            targetRatio
        }

        displayedRatio = finalRatio
        cam.cameraControl.setLinearZoom(ratioToLinear(finalRatio))
        selectedZoom = nearestStop(finalRatio)
    }

    fun onZoomButtonClick(zoom: Float) {
        // 2x 再点一下切 3.5x：这是原厂 App 常见的"人像/长焦中间档"手感，
        // 但如果本机根本没有这个焦段的物理头，就直接吸附回最近的光学档
        if (zoom == 2.0f && abs(selectedZoom - 2.0f) < 0.05f) {
            val next = snapToOptical(3.5f)
            selectedZoom = next
            smoothZoomTo(next)
        } else {
            selectedZoom = zoom
            smoothZoomTo(zoom)
        }
    }

    /** 拖动标尺松手后调用：吸到最近的光学镜头上，避免停在数码变焦的模糊区 */
    fun commitZoom(ratio: Float) {
        val cam = camera ?: return
        if (!scanDone) return
        val snapped = snapToOptical(ratio)
        if (abs(snapped - ratio) > 0.001f) {
            smoothZoomTo(snapped)
        } else {
            cam.cameraControl.setLinearZoom(ratioToLinear(snapped))
            displayedRatio = snapped
        }
        selectedZoom = nearestStop(snapped)
    }

    fun takePhoto() {
        val ic = imageCapture ?: return
        val name = "IMG_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/MyApp")
        }
        val options = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ).build()
        ic.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val uri = outputFileResults.savedUri ?: return
                    sessionMedia.add(0, CapturedMedia(uri, false, System.currentTimeMillis()))
                }
                override fun onError(exception: ImageCaptureException) {
                    Log.e("Camera", "拍照失败", exception)
                }
            }
        )
    }

    fun startRecording() {
        val vc = videoCapture ?: return
        val name = "VID_${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/MyApp")
        }
        val outputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(values).build()

        val pending = vc.output.prepareRecording(context, outputOptions)
        val withAudio = try { pending.withAudioEnabled() } catch (e: SecurityException) { pending }

        recording = withAudio.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> isRecording = true
                is VideoRecordEvent.Finalize -> {
                    isRecording = false
                    recording = null
                    if (!event.hasError()) {
                        val uri = event.outputResults.outputUri
                        if (uri != Uri.EMPTY) {
                            sessionMedia.add(0, CapturedMedia(uri, true, System.currentTimeMillis()))
                        }
                    }
                }
            }
        }
    }

    fun stopRecording() {
        recording?.stop()
        recording = null
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // 更新提示：Dialog 是独立窗口，放在这里不影响相机预览层级
        updateInfo?.let { info ->
            UpdateDialog(
                info = info,
                onDismiss = { updateInfo = null },
                onSkip = {
                    AppUpdate.skipVersion(context, info.versionCode)
                    updateInfo = null
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = flipAngle
                    cameraDistance = 12f * density
                }
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        if (isUltraWide) {
                            drawContext.canvas.saveLayer(
                                Rect(0f, 0f, size.width, size.height),
                                ultraWidePaint
                            )
                            drawContent()
                            drawContext.canvas.restore()
                        } else {
                            drawContent()
                        }
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoomChange, _ ->
                            if (isFront) return@detectTransformGestures
                            if (highResMode) return@detectTransformGestures
                            val cam = camera ?: return@detectTransformGestures
                            if (!scanDone) return@detectTransformGestures
                            zoomJob?.cancel()
                            val currentLinear = cam.cameraInfo.zoomState.value?.linearZoom ?: 0f
                            val delta = (zoomChange - 1f) * 0.3f
                            val newLinear = (currentLinear + delta).coerceIn(0f, 1f)
                            cam.cameraControl.setLinearZoom(newLinear)
                            selectedZoom = nearestStop(
                                cam.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                            )
                            lastRulerInteraction = System.currentTimeMillis()
                        }
                    },
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewViewRef = this
                    }
                },
                update = { }
            )
        }

        if (flipAngle > 0.5f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (flipAngle / 90f).coerceIn(0f, 1f)))
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        highResMode = !highResMode
                        if (highResMode) {
                            val ok = abs(displayedRatio - 1.0f) < 0.05f ||
                                    abs(displayedRatio - 5.0f) < 0.05f
                            if (!ok) {
                                selectedZoom = 1.0f
                                smoothZoomTo(1.0f)
                            }
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (highResMode) "50M" else "12.5M",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (flashMode == 3) Color(0xFFFFC107).copy(alpha = 0.9f)
                        else Color.Black.copy(alpha = 0.4f)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        flashMode = (flashMode + 1) % 4
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = when (flashMode) {
                        0 -> "⚡ 关"
                        1 -> "⚡ 自动"
                        2 -> "⚡ 开"
                        3 -> "🔦 手电"
                        else -> "⚡"
                    },
                    color = if (flashMode == 3) Color.Black else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showAbout = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!scanDone) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text("正在校准...", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isFront) {
                // 显示当前倍率 + 正在用哪颗物理镜头。
                // 正好落在某颗头上 → "2.0x · 50mm 长焦"；
                // 两颗头之间（数码裁切）→ 显示实际干活的镜头 + "数码"，如 "3.5x · 23mm 主摄 数码"
                val lensText = if (zoomStopsFromHardware) {
                    val optical = opticalLensAt(displayedRatio)
                    if (optical != null) {
                        " · ${optical.shortLabel}"
                    } else {
                        val active = activeLensAt(displayedRatio)
                        if (active != null) " · ${active.shortLabel} 数码" else ""
                    }
                } else ""
                Text(
                    text = formatZoom(displayedRatio) + lensText,
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))

                AnimatedVisibility(
                    visible = showRuler,
                    enter = expandVertically(
                        expandFrom = Alignment.Bottom,
                        animationSpec = tween(durationMillis = 320)
                    ) + fadeIn(animationSpec = tween(durationMillis = 250)),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Bottom,
                        animationSpec = tween(durationMillis = 320)
                    ) + fadeOut(animationSpec = tween(durationMillis = 200))
                ) {
                    ZoomRuler(
                        currentRatio = displayedRatio,
                        minRatio = minRatio, maxRatio = maxRatio,
                        onRatioChange = {
                            lastRulerInteraction = System.currentTimeMillis()
                            directSetRatio(it)
                        },
                        onDragStateChange = { isDragging = it },
                        modifier = Modifier.fillMaxWidth().height(105.dp),
                        stops = zoomStops,
                        onCommit = {
                            lastRulerInteraction = System.currentTimeMillis()
                            commitZoom(it)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                showRuler = true
                                lastRulerInteraction = System.currentTimeMillis()
                            }
                        ) { change, _ ->
                            change.consume()
                            lastRulerInteraction = System.currentTimeMillis()
                        }
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 档位按钮 = 光学档位 ∪ 常用档位（2x 等没有物理头的档位保留，走数码变焦）
                    zoomStops.forEach { zoom ->
                        val isSelected = when {
                            zoom == 2.0f && abs(selectedZoom - 3.5f) < 0.05f -> true
                            abs(selectedZoom - zoom) < 0.05f -> true
                            else -> false
                        }
                        // 这台机器到不了的档位（比如 HAL 没上报 0.6x 超广角）直接置灰
                        val allowed = zoom >= minRatio - 0.01f && zoom <= maxRatio + 0.01f &&
                                // 高画质模式只允许真实光学档
                                (!highResMode || isOpticalStop(zoom))

                        ZoomButton(
                            label = formatStop(zoom),
                            isSelected = isSelected,
                            enabled = scanDone && allowed,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onZoomButtonClick(zoom)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (mode == CaptureMode.PRO && !isFront && scanDone) {
                if (!manualControlSupported) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "此设备不支持手动控制（相机 HAL 未开放）",
                            color = Color(0xFFFFA000),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    ProControls(
                        proIso = proIso,
                        onIsoChange = {
                            proIso = it
                            applyProParams(it, proShutterNs, proEv, proFocus, proWb)
                        },
                        proShutterNs = proShutterNs,
                        onShutterChange = {
                            proShutterNs = it
                            applyProParams(proIso, it, proEv, proFocus, proWb)
                        },
                        proEv = proEv,
                        onEvChange = {
                            proEv = it
                            applyProParams(proIso, proShutterNs, it, proFocus, proWb)
                        },
                        proFocus = proFocus,
                        onFocusChange = {
                            proFocus = it
                            applyProParams(proIso, proShutterNs, proEv, it, proWb)
                        },
                        proWb = proWb,
                        onWbChange = {
                            proWb = it
                            applyProParams(proIso, proShutterNs, proEv, proFocus, it)
                        },
                        onReset = {
                            proIso = 0
                            proShutterNs = 0L
                            proEv = 0
                            proFocus = -1f
                            proWb = 0
                            applyProParams(0, 0L, 0, -1f, 0)
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            ModeSelector(
                current = mode,
                onSelect = { mode = it }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp).clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { showGallery = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (sessionMedia.isNotEmpty()) {
                        SessionThumbnail(media = sessionMedia[0], modifier = Modifier.fillMaxSize())
                    } else {
                        Text("🖼", fontSize = 20.sp)
                    }
                }

                val isVideoMode = mode == CaptureMode.VIDEO
                Box(
                    modifier = Modifier
                        .size(76.dp).clip(CircleShape)
                        .background(Color.White)
                        .border(
                            width = 4.dp,
                            color = if (isRecording) Color.Red else Color.White.copy(alpha = 0.6f),
                            shape = CircleShape
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isVideoMode) {
                                if (isRecording) stopRecording() else startRecording()
                            } else {
                                takePhoto()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier.size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Red)
                        )
                    } else if (isVideoMode) {
                        Box(
                            modifier = Modifier.size(60.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(52.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable {
                            if (isFlipping) return@clickable
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showRuler = false
                            isFlipping = true
                            pendingLensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                CameraSelector.LENS_FACING_FRONT
                            else
                                CameraSelector.LENS_FACING_BACK
                            cameraReadyAfterSwitch = false

                            coroutineScope.launch {
                                val dur = 220L
                                var t0 = System.currentTimeMillis()
                                while (true) {
                                    val el = System.currentTimeMillis() - t0
                                    val t = (el.toFloat() / dur).coerceIn(0f, 1f)
                                    flipAngle = 90f * t
                                    if (t >= 1f) break
                                    delay(16)
                                }
                                lensFacing = pendingLensFacing
                                var wait = 0L
                                while (!cameraReadyAfterSwitch && wait < 3000) {
                                    delay(16)
                                    wait += 16
                                }
                                t0 = System.currentTimeMillis()
                                while (true) {
                                    val el = System.currentTimeMillis() - t0
                                    val t = (el.toFloat() / dur).coerceIn(0f, 1f)
                                    flipAngle = 90f * (1f - t)
                                    if (t >= 1f) break
                                    delay(16)
                                }
                                flipAngle = 0f
                                isFlipping = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔄", fontSize = 22.sp)
                }
            }
        }

        AnimatedVisibility(
            visible = showGallery,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(
                    durationMillis = 350,
                    easing = CubicBezierEasing(0.3f, 0.85f, 0.2f, 1f)
                )
            ) + fadeIn(animationSpec = tween(durationMillis = 250)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(
                    durationMillis = 300,
                    easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)
                )
            ) + fadeOut(animationSpec = tween(durationMillis = 250))
        ) {
            SessionGallery(media = sessionMedia, onClose = { showGallery = false })
        }

        AnimatedVisibility(
            visible = showAbout,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(durationMillis = 300, easing = CubicBezierEasing(0.3f, 0.85f, 0.2f, 1f))
            ) + fadeIn(tween(200)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(durationMillis = 260, easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f))
            ) + fadeOut(tween(180))
        ) {
            AboutScreen(
                onClose = { showAbout = false },
                onShowReward = { showReward = true }
            )
        }

        AnimatedVisibility(
            visible = showReward,
            enter = fadeIn(tween(durationMillis = 220)),
            exit = fadeOut(tween(durationMillis = 180))
        ) {
            RewardDialog(onClose = { showReward = false })
        }
    }
}

// ==================== 专业模式 UI ====================

@Composable
fun ProControls(
    proIso: Int, onIsoChange: (Int) -> Unit,
    proShutterNs: Long, onShutterChange: (Long) -> Unit,
    proEv: Int, onEvChange: (Int) -> Unit,
    proFocus: Float, onFocusChange: (Float) -> Unit,
    proWb: Int, onWbChange: (Int) -> Unit,
    onReset: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var activeParam by remember { mutableStateOf<ProParam?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = activeParam != null,
            enter = expandVertically(
                expandFrom = Alignment.Bottom,
                animationSpec = tween(220)
            ) + fadeIn(tween(180)),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Bottom,
                animationSpec = tween(180)
            ) + fadeOut(tween(140))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                when (activeParam) {
                    ProParam.EV -> {
                        ProSlider(
                            label = "EV",
                            displayValue = when {
                                proEv > 0 -> "+$proEv"
                                proEv < 0 -> "$proEv"
                                else -> "0"
                            },
                            value = proEv.toFloat(),
                            range = -2f..2f,
                            isAuto = proEv == 0,
                            onValueChange = { onEvChange(it.toInt()) },
                            onResetAuto = { onEvChange(0) }
                        )
                    }
                    ProParam.SHUTTER -> {
                        val speedMin = 30f
                        val speedMax = 1000f
                        val currentSpeed = if (proShutterNs == 0L) {
                            speedMin
                        } else {
                            (1_000_000_000.0 / proShutterNs)
                                .toFloat()
                                .coerceIn(speedMin, speedMax)
                        }
                        val disp = if (proShutterNs == 0L) "自动"
                        else "1/${currentSpeed.toInt().coerceAtLeast(1)}"
                        ProSlider(
                            label = "S",
                            displayValue = disp,
                            value = currentSpeed,
                            range = speedMin..speedMax,
                            isAuto = proShutterNs == 0L,
                            onValueChange = {
                                val speed = it.coerceIn(speedMin, speedMax)
                                val ns = (1_000_000_000.0 / speed).toLong()
                                onShutterChange(ns)
                            },
                            onResetAuto = { onShutterChange(0L) }
                        )
                    }
                    ProParam.ISO -> {
                        val v = if (proIso == 0) 100f else proIso.toFloat()
                        val disp = if (proIso == 0) "自动" else "$proIso"
                        ProSlider(
                            label = "ISO",
                            displayValue = disp,
                            value = v,
                            range = 100f..3200f,
                            isAuto = proIso == 0,
                            onValueChange = { onIsoChange(it.toInt()) },
                            onResetAuto = { onIsoChange(0) }
                        )
                    }
                    ProParam.WB -> {
                        ProSlider(
                            label = "WB",
                            displayValue = wbLabel(proWb),
                            value = proWb.toFloat(),
                            range = 0f..4f,
                            isAuto = proWb == 0,
                            onValueChange = { onWbChange(it.toInt()) },
                            onResetAuto = { onWbChange(0) }
                        )
                    }
                    ProParam.FOCUS -> {
                        ProSlider(
                            label = "F",
                            displayValue = if (proFocus < 0f) "自动" else "%.1f".format(proFocus),
                            value = if (proFocus < 0f) 0f else proFocus,
                            range = 0f..10f,
                            isAuto = proFocus < 0f,
                            onValueChange = { onFocusChange(it) },
                            onResetAuto = { onFocusChange(-1f) }
                        )
                    }
                    null -> { }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProCircleButton(
                value = "↻",
                label = "",
                isActive = false,
                isReset = true,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeParam = null
                    onReset()
                }
            )

            ProCircleButton(
                value = when {
                    proEv > 0 -> "+$proEv"
                    proEv < 0 -> "$proEv"
                    else -> "0"
                },
                label = "EV",
                isActive = activeParam == ProParam.EV,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeParam = if (activeParam == ProParam.EV) null else ProParam.EV
                }
            )

            ProCircleButton(
                value = if (proShutterNs == 0L) "AUTO" else {
                    val s = 1_000_000_000.0 / proShutterNs
                    "1/${s.toInt().coerceAtLeast(1)}"
                },
                label = "S",
                isActive = activeParam == ProParam.SHUTTER,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeParam = if (activeParam == ProParam.SHUTTER) null else ProParam.SHUTTER
                }
            )

            ProCircleButton(
                value = if (proIso == 0) "AUTO" else "$proIso",
                label = "ISO",
                isActive = activeParam == ProParam.ISO,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeParam = if (activeParam == ProParam.ISO) null else ProParam.ISO
                }
            )

            ProCircleButton(
                value = wbShortLabel(proWb),
                label = "WB",
                isActive = activeParam == ProParam.WB,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeParam = if (activeParam == ProParam.WB) null else ProParam.WB
                }
            )

            ProCircleButton(
                value = if (proFocus < 0f) "AF" else "%.1f".format(proFocus),
                label = "F",
                isActive = activeParam == ProParam.FOCUS,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    activeParam = if (activeParam == ProParam.FOCUS) null else ProParam.FOCUS
                }
            )
        }
    }
}

fun wbLabel(mode: Int): String = when (mode) {
    0 -> "自动"
    1 -> "白炽灯 3000K"
    2 -> "日光 5500K"
    3 -> "阴天 6500K"
    4 -> "阴影 7500K"
    else -> "自动"
}

fun wbShortLabel(mode: Int): String = when (mode) {
    0 -> "AUTO"
    1 -> "3000K"
    2 -> "5500K"
    3 -> "6500K"
    4 -> "7500K"
    else -> "AUTO"
}

@Composable
fun ProCircleButton(
    value: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    isReset: Boolean = false
) {
    val bgColor = when {
        isActive -> Color(0xFFFFC107)
        isReset -> Color.Transparent
        else -> Color.Black.copy(alpha = 0.4f)
    }
    val textColor = if (isActive) Color.Black else Color.White
    val borderColor = when {
        isActive -> Color.Transparent
        isReset -> Color.Transparent
        else -> Color.White.copy(alpha = 0.2f)
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (label.isNotEmpty()) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = label,
                    color = if (isActive) Color.Black.copy(alpha = 0.7f)
                    else Color.White.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
        }
    }
}

// 🎯 新增：带"自动"按钮的滑块
@Composable
fun ProSlider(
    label: String,
    displayValue: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    isAuto: Boolean,
    onValueChange: (Float) -> Unit,
    onResetAuto: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(34.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .pointerInput(range) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val frac = (offset.x / size.width).coerceIn(0f, 1f)
                            val v = range.start + frac * (range.endInclusive - range.start)
                            onValueChange(v)
                        }
                    ) { change, _ ->
                        change.consume()
                        val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                        val v = range.start + frac * (range.endInclusive - range.start)
                        onValueChange(v)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val h = size.height
                val w = size.width
                val y = h / 2f
                drawLine(
                    color = Color.White.copy(alpha = 0.18f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                for (i in 0..10) {
                    val x = w * i / 10f
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(x, y - 4.dp.toPx()),
                        end = Offset(x, y + 4.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                val frac = if (range.endInclusive > range.start) {
                    ((value - range.start) /
                            (range.endInclusive - range.start)).coerceIn(0f, 1f)
                } else 0f
                // 自动状态时用灰色轨道提示
                drawLine(
                    color = if (isAuto) Color.White.copy(alpha = 0.3f)
                    else Color(0xFFFFC107),
                    start = Offset(0f, y),
                    end = Offset(w * frac, y),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = if (isAuto) Color.White.copy(alpha = 0.5f)
                    else Color(0xFFFFC107),
                    radius = 8.dp.toPx(),
                    center = Offset(w * frac, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(w * frac, y)
                )
            }
        }
        Text(
            displayValue,
            color = if (isAuto) Color.White.copy(alpha = 0.6f) else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(52.dp),
            textAlign = TextAlign.End
        )
        Spacer(modifier = Modifier.width(8.dp))
        // 🎯 自动按钮
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                    if (isAuto) Color(0xFFFFC107) else Color.White.copy(alpha = 0.12f)
                )
                .border(
                    1.dp,
                    if (isAuto) Color.Transparent else Color.White.copy(alpha = 0.25f),
                    CircleShape
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onResetAuto()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "A",
                color = if (isAuto) Color.Black else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==================== 关于页 ====================

@Composable
fun AboutScreen(
    onClose: () -> Unit,
    onShowReward: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClose()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", color = Color.White, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("关于", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Silky Camera", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("版本 $APP_VERSION_NAME", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)

                Spacer(modifier = Modifier.height(40.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    AboutRow(label = "版本", value = APP_VERSION_NAME)
                    Spacer(modifier = Modifier.height(14.dp))
                    AboutRow(label = "制作", value = "Kotlin + Jetpack Compose + CameraX")
                    Spacer(modifier = Modifier.height(14.dp))
                    AboutRow(label = "制作人", value = "hmjs")
                    Spacer(modifier = Modifier.height(14.dp))
                    AboutRow(label = "b站/抖音", value = "小码-初中")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFFFC107).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFFFC107).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onShowReward()
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "如果觉得还可以，可以考虑请作者喝一杯柠檬水 🍋(点击我即可)",
                        color = Color(0xFFFFC107),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun RewardDialog(onClose: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .clickable(enabled = false) { }
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.reward_qr),
                contentDescription = "赞赏码",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "长按识别赞赏码",
                color = Color(0xFF333333),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(Color(0xFFFFC107))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClose()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("关闭", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 13.sp,
            // 宽度要放得下最长的标签 "b站/抖音"，并让各行内容左对齐
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==================== 模式栏 ====================

@Composable
fun ModeSelector(
    current: CaptureMode,
    onSelect: (CaptureMode) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        CaptureMode.values().forEach { m ->
            val selected = m == current

            val textColor by animateColorAsState(
                targetValue = when {
                    selected -> Color(0xFFFFA000)
                    else -> Color.White.copy(alpha = 0.75f)
                },
                animationSpec = tween(durationMillis = 250),
                label = "modeTextColor"
            )

            val underlineWidth by animateDpAsState(
                targetValue = if (selected) 16.dp else 0.dp,
                animationSpec = tween(durationMillis = 250),
                label = "modeUnderline"
            )

            Box(
                modifier = Modifier
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect(m)
                    }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = m.label,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(underlineWidth)
                            .height(2.dp)
                            .background(
                                color = Color(0xFFFFA000),
                                shape = RoundedCornerShape(1.dp)
                            )
                    )
                }
            }
        }
    }
}

// ==================== 相册 ====================

@Composable
fun SessionGallery(media: List<CapturedMedia>, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", color = Color.White, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("本次拍摄 (${media.size})", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            if (media.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("还没有拍摄任何内容", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(media) { item ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.DarkGray)
                        ) {
                            SessionThumbnail(media = item, modifier = Modifier.fillMaxSize())
                            if (item.isVideo) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd).padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .padding(4.dp)
                                ) {
                                    Text("▶", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionThumbnail(media: CapturedMedia, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, media.uri) {
        value = withContext(Dispatchers.IO) {
            try {
                if (media.isVideo) {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, media.uri)
                    val frame = retriever.getFrameAtTime(0)
                    retriever.release()
                    frame?.asImageBitmap()
                } else {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    context.contentResolver.openInputStream(media.uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                    }
                }
            } catch (e: Exception) { null }
        }
    }
    bitmap?.let {
        Image(bitmap = it, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    } ?: Box(modifier = modifier.background(Color(0xFF333333)))
}

// ==================== 变焦 ====================

fun formatZoom(ratio: Float): String = "%.1fx".format(ratio)

@Composable
fun ZoomRuler(
    currentRatio: Float,
    minRatio: Float,
    maxRatio: Float,
    onRatioChange: (Float) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    // 光学档位刻度：来自原厂镜头参数的真实倍率，空则退回默认刻度
    stops: List<Float> = listOf(0.6f, 1.0f, 2.0f, 5.0f, 10.0f),
    // 松手回调：用于吸附到最近的光学镜头
    onCommit: ((Float) -> Unit)? = null
) {
    val logMin = remember(minRatio) { ln(minRatio) }
    val logMax = remember(maxRatio) { ln(maxRatio) }

    fun ratioToT(ratio: Float): Float {
        if (logMax <= logMin) return 0f
        val r = ratio.coerceIn(minRatio, maxRatio)
        return ((ln(r) - logMin) / (logMax - logMin)).coerceIn(0f, 1f)
    }
    fun tToRatio(t: Float): Float = exp(logMin + t * (logMax - logMin))

    val currentT = ratioToT(currentRatio)
    val latestRatio by rememberUpdatedState(currentRatio)

    Canvas(
        modifier = modifier.pointerInput(minRatio, maxRatio) {
            var localT = ratioToT(latestRatio)
            detectDragGestures(
                onDragStart = {
                    localT = ratioToT(latestRatio)
                    onDragStateChange(true)
                },
                onDragEnd = {
                    onDragStateChange(false)
                    onCommit?.invoke(tToRatio(localT))
                },
                onDragCancel = { onDragStateChange(false) }
            ) { change, dragAmount ->
                change.consume()
                val pixelsPerT = size.width * 0.9f
                val dt = -dragAmount.x / pixelsPerT
                localT = (localT + dt).coerceIn(0f, 1f)
                onRatioChange(tToRatio(localT))
            }
        }
    ) {
        val W = size.width
        val H = size.height
        val cX = W / 2f

        val R = W * 1.4f
        val arcTopY = H * 0.42f
        val cY = arcTopY + R

        val halfW = W / 2f - 4.dp.toPx()
        val sinT = (halfW / R).coerceIn(-1f, 1f)
        val theta = asin(sinT.toDouble()).toFloat()
        val thetaDeg = Math.toDegrees(theta.toDouble()).toFloat()

        fun tToAngle(t: Float): Float = (t - currentT) * 2f * theta

        fun arcPos(a: Float, radius: Float): Offset = Offset(
            cX + radius * sin(a),
            cY - radius * cos(a)
        )

        drawArc(
            color = Color.White.copy(alpha = 0.4f),
            startAngle = 270f - thetaDeg,
            sweepAngle = thetaDeg * 2f,
            useCenter = false,
            topLeft = Offset(cX - R, cY - R),
            size = GeoSize(R * 2f, R * 2f),
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
        )

        val minorStep = 0.005f
        var t = 0f
        while (t <= 1.001f) {
            val a = tToAngle(t)
            if (abs(a) <= theta * 1.02f) {
                val p1 = arcPos(a, R)
                val p2 = arcPos(a, R - 6.dp.toPx())
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = p1, end = p2,
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            t += minorStep
        }

        val majors = stops.filter { it <= maxRatio + 0.001f && it >= minRatio - 0.001f }
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 11.dp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        majors.forEach { ratio ->
            val t2 = ratioToT(ratio)
            val a = tToAngle(t2)
            if (abs(a) <= theta * 1.02f) {
                val p1 = arcPos(a, R)
                val p2 = arcPos(a, R - 12.dp.toPx())
                drawLine(
                    color = Color.White,
                    start = p1, end = p2,
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                val label = if (ratio < 1f) "%.1f".format(ratio)
                else if (abs(ratio - ratio.roundToInt()) < 0.05f) "${ratio.roundToInt()}"
                else "%.1f".format(ratio)
                val textPos = arcPos(a, R - 26.dp.toPx())
                drawContext.canvas.nativeCanvas.drawText(
                    label, textPos.x, textPos.y + 4.dp.toPx(), textPaint
                )
            }
        }

        val arcCenter = arcPos(0f, R)
        drawLine(
            color = Color(0xFFFFC107),
            start = Offset(arcCenter.x, arcCenter.y - 4.dp.toPx()),
            end = Offset(arcCenter.x, arcCenter.y + 18.dp.toPx()),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun ZoomButton(
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val targetScale = when {
        !enabled -> 1.0f
        isPressed -> 0.88f
        isSelected -> 1.10f
        else -> 1.0f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "zoomButtonScale"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .size(44.dp)
            .background(
                color = when {
                    !enabled -> Color.Black.copy(alpha = 0.2f)
                    isSelected -> Color.White
                    else -> Color.Black.copy(alpha = 0.4f)
                },
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else Color.White.copy(alpha = if (enabled) 0.3f else 0.1f),
                shape = CircleShape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = when {
                !enabled -> Color.White.copy(alpha = 0.25f)
                isSelected -> Color.Black
                else -> Color.White
            },
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
