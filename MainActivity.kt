package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import java.util.concurrent.ConcurrentHashMap
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import android.util.Rational
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraFilter
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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

    /**
     * iQOO 15 Ultra：与 iQOO 15 同代，型号号段一致（V2505 / PD2505），
     * Ultra 版额外带 "ultra" 字样，个别批次只上报 "i2505" 前缀，三条都判。
     * ⚠️ 光学档位同样是 0.6x / 1x / 3x（没有 2x 人像头），别照抄 X80 Pro 的 2x。
     */
    val isIQOO15Ultra: Boolean
        get() = isVivo && (
                model.startsWith("i2505") || device.startsWith("i2505") ||
                        (isIQOO15 && (model.contains("ultra") || device.contains("ultra")))
                )

    /** iQOO 15 系列（含 Ultra） */
    val isIQOO15Series: Boolean get() = isIQOO15 || isIQOO15Ultra

    /**
     * 小米 10 系列。DEVICE（内部代号）：小米10=umi、10 Pro=cmi、10 至尊版=cas、10S=thyme。
     * 这些机器的逻辑摄像头默认只出主摄，超广角/长焦要另想办法才切得过去。
     */
    val isXiaomiMi10Series: Boolean
        get() = isXiaomi && (
                model.contains("mi 10") || model.contains("mi10") ||
                        device.startsWith("umi") || device.startsWith("cmi") ||
                        device.startsWith("cas") || device.startsWith("thyme")
                )

    /**
     * 小米 13 Ultra。DEVICE=ishtar。
     * 四摄全 50MP：主摄 23mm(1英寸) + 超广角 12mm + 3.2x 长焦 75mm + 5x 潜望 120mm。
     * ⚠️ 小米在这台机器上不上报 SENSOR_INFO_PHYSICAL_SIZE（或上报 0），
     *    老版 probeLenses 拿不到传感器尺寸就 continue，结果只剩"主摄"一颗头。
     */
    val isXiaomi13Ultra: Boolean
        get() = isXiaomi && (
                model.contains("13 ultra") || model.contains("13ultra") ||
                        device.startsWith("ishtar")
                )

    /** 小米 13 Pro / 14 Ultra 等一英寸大底机型，同样容易缺传感器尺寸 */
    val isXiaomiOneInch: Boolean
        get() = isXiaomi13Ultra ||
                (isXiaomi && (model.contains("14 ultra") || model.contains("14ultra")))

    /**
     * 已知"逻辑摄像头切不动副摄"的机型。
     * 这些机器的 zoomState 要么把 minZoomRatio 报成 1.0（宣称没广角），
     * 要么 setZoomRatio 不报错但画面根本不换头 —— 只能靠直连物理头解决。
     *
     * ⚠️ 这里刻意【不包含】笼统的 isXiaomi：小米机型众多，
     *    实测很多小米机器 setZoomRatio 是能正常切广角的（比如本次反馈的 M31DA）。
     *    之前把 isXiaomi 加进来，导致所有小米机都"预判式"优先走物理直连，
     *    反而把本来能正常变焦的机器搞坏了。
     */
    val isStubbornMultiCam: Boolean
        get() = isXiaomiMi10Series || isXiaomi13Ultra || isXiaomiOneInch ||
                isIQOO15Series || isVivoX80Pro

    /**
     * 出厂已知的光学档位，用于探测失败时兜底。
     * 数字取自各家官方规格页的「等效焦距 ÷ 主摄等效焦距」。
     * ★最后一条 else 给通用兜底★：2020 年后的多摄机基本都是"超广角+主摄+长焦"，
     * 探测成功时用真实结果覆盖，探测失败时至少保证 0.6x 按钮还在、能点。
     */
    val knownOpticalStops: List<Float>
        get() = when {
            isIQOO15Series -> listOf(0.6f, 1.0f, 3.0f)
            isVivoX80Pro -> listOf(0.6f, 1.0f, 2.0f, 5.0f)
            isXiaomi13Ultra -> listOf(0.6f, 1.0f, 3.2f, 5.0f)
            isXiaomiOneInch -> listOf(0.6f, 1.0f, 3.2f, 5.0f)
            isXiaomiMi10Series -> listOf(0.6f, 1.0f, 2.0f)
            else -> listOf(0.6f, 1.0f, 2.0f)
        }

    /** 蔡司多摄机型：每颗焦段都是独立物理摄像头，切档需要更长收敛时间 */
    val isZeissMultiCam: Boolean get() = isVivoX80Pro || isIQOO15Series

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

    /**
     * 标定用的单档等待(ms)。标定是"逐档试一遍"，档位越多越慢，
     * 所以单独给一个比常规变焦更短的值，再配合轮询提前结束。
     * 标定只用来做镜头标签估算，不参与实际变焦（实际变焦走 setZoomRatio），
     * 精度要求不高，没必要等满。
     */
    val calibrationSettleMs: Long
        get() = when {
            isAndroid16Plus -> 320L
            isVivo -> 300L
            isXiaomi -> 260L
            else -> 220L
        }

    /** 单个标定点的最长轮询时间(ms)，到点就放弃，不无限等 */
    val calibrationTimeoutMs: Long get() = calibrationSettleMs * 3L

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
     * 挑一个本机真实支持的 JPEG 尺寸，避免写死 4080x3060 在 vivo / 小米上落到不支持的
     * 分辨率导致拍照失败，或被 FALLBACK 规则甩到很低的分辨率。
     *
     * @param aspect 目标比例（AspectRatio.RATIO_4_3 / RATIO_16_9），传 null 表示全屏
     * @param screenRational aspect 为 null 时用屏幕真实像素比
     */
    fun pickCaptureSize(
        context: Context,
        lensFacing: Int,
        maxPixels: Int = 13_000_000,
        aspect: Int? = AspectRatio.RATIO_4_3,
        screenRational: Rational? = null
    ): Size {
        // 目标比例统一换算成"横屏方向的 宽/高"
        val target = when (aspect) {
            AspectRatio.RATIO_16_9 -> 16f / 9f
            AspectRatio.RATIO_4_3 -> 4f / 3f
            null -> screenRational?.let {
                it.numerator.toFloat() / it.denominator.toFloat().coerceAtLeast(1f)
            } ?: (4f / 3f)
            else -> 4f / 3f
        }
        return try {
            val id = cameraIdOf(context, lensFacing) ?: return defaultCaptureSize
            val chars = cameraManager(context).getCameraCharacteristics(id)
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = map?.getOutputSizes(ImageFormat.JPEG)?.toList()
                ?: return defaultCaptureSize

            val inBudget = sizes.filter { it.width * it.height <= maxPixels }
            val best = inBudget
                .filter { abs(it.width.toFloat() / it.height - target) < target * 0.02f }
                .maxByOrNull { it.width * it.height }
                ?: inBudget.maxByOrNull { it.width * it.height }
                ?: sizes.maxByOrNull { it.width * it.height }
                ?: defaultCaptureSize
            Log.d(
                "Camera",
                "拍照尺寸[$id]: ${best.width}x${best.height}（比例 ${"%.3f".format(target)}，候选 ${sizes.size} 个）"
            )
            best
        } catch (e: Exception) {
            Log.w("Camera", "拍照尺寸探测失败", e)
            defaultCaptureSize
        }
    }

    /**
     * 机器支不支持录像防抖。
     * CameraX 1.3.4 没有官方的 isStabilizationSupported（1.4.0 才加），
     * 这里直接读 Camera2 的 CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES。
     *
     * ⚠️ 探测结果为"未知"时一律按【支持】处理（乐观策略）：
     *    · 这个 key 很多机器压根不上报（返回 null）
     *    · 少数机器上报了但实际能用
     *    以前我一刀切判成不支持，导致防抖按钮直接消失、功能形同虚设。
     *    就算真的不支持，setStabilizationRaw 里下发失败也只是被 HAL 忽略并被 catch，
     *    不会有副作用。所以宁可让用户能开。
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun isVideoStabilizationSupported(context: Context, camera: Camera?): Boolean {
        if (camera == null) return true
        return try {
            val cameraId = Camera2CameraInfo.from(camera.cameraInfo).cameraId
            val chars = cameraManager(context).getCameraCharacteristics(cameraId)
            val modes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
            val ok = when {
                modes == null -> {
                    Log.d("Camera", "录像防抖支持[$cameraId]: 未上报该能力，按支持处理")
                    true
                }
                else -> modes.contains(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON)
            }
            Log.d("Camera", "录像防抖支持[$cameraId]: $ok（可用模式=${modes?.joinToString()}）")
            ok
        } catch (e: Exception) {
            Log.w("Camera", "防抖能力探测失败，按支持处理", e)
            true
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

    /**
     * 主摄的"典型物理焦距"(mm)。只在机器完全不上报 SENSOR_INFO_PHYSICAL_SIZE 时使用：
     * 手机主摄物理焦距普遍落在 4~7mm（1 英寸大底约 8~9mm），取 5.5mm 做基准算倍率。
     */
    private const val BASE_FOCAL_MM = 5.5f

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
    /**
     * 探测结果缓存（按镜头朝向）。
     * 读 CameraCharacteristics 是 IPC + 文件操作，多摄机上遍历一遍要几百毫秒。
     * 切前后摄来回切时会重复探测，缓存后第二次起几乎零耗时。
     */
    private val lensProbeCache = ConcurrentHashMap<Int, List<LensProfile>>()
    private val physicalProbeCache = ConcurrentHashMap<Int, List<LensProfile>>()

    /** 调试用：清空探测缓存（换机或 HAL 异常时可以调） */
    fun clearProbeCache() {
        lensProbeCache.clear()
        physicalProbeCache.clear()
    }

    @SuppressLint("NewApi")
    fun probeLenses(context: Context, lensFacing: Int): List<LensProfile> {
        // 命中缓存直接返回，省掉一次完整的 CameraCharacteristics 遍历
        lensProbeCache[lensFacing]?.let { cached ->
            Log.d("Camera", "镜头参数命中缓存（${cached.size} 颗）")
            return cached
        }
        return try {
            val manager = cameraManager(context)
            val raw = ArrayList<LensProfile>()

            for (id in manager.cameraIdList) {
                val chars = try {
                    manager.getCameraCharacteristics(id)
                } catch (e: Exception) {
                    continue
                }
                if (chars.get(CameraCharacteristics.LENS_FACING) != lensFacing) continue

                // 一颗逻辑头可能对应多颗物理头（多摄模组），逐个读
                val physicalIds: Set<String> =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        try {
                            chars.physicalCameraIds
                        } catch (e: Exception) {
                            emptySet<String>()
                        }
                    } else emptySet<String>()

                val idsToRead: Set<String> =
                    if (physicalIds.isNotEmpty()) physicalIds else setOf(id)

                for (pid in idsToRead) {
                    val pc = try {
                        manager.getCameraCharacteristics(pid)
                    } catch (e: Exception) {
                        continue
                    }
                    val fl = pc.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                        ?.firstOrNull() ?: continue
                    if (fl <= 0f) continue

                    val phys = pc.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                    val sensorW: Float = phys?.width ?: 0f

                    // ★小米 / vivo 的头号坑★ SENSOR_INFO_PHYSICAL_SIZE 不上报或上报 0。
                    // 以前这里直接 continue，整台机器就只剩"主摄"一颗头 ——
                    // 这正是小米10 / 小米13 Ultra / iQOO 15 Ultra 只能调用主摄的根因。
                    // 现在改成先收下来（equiv35mm 记 0 表示"待补算"），后面用比例尺换算。
                    val equiv: Float = if (sensorW > 0f) fl * 36f / sensorW else 0f

                    // 同一颗头可能被多个逻辑头重复上报，按等效焦距去重
                    if (equiv > 0f && raw.any { abs(it.equiv35mm - equiv) < 0.8f }) continue

                    val fov: Float = if (sensorW > 0f) {
                        Math.toDegrees(2.0 * atan((sensorW / (2f * fl)).toDouble())).toFloat()
                    } else 0f
                    val ap = pc.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                        ?.firstOrNull()

                    raw.add(
                        LensProfile(
                            id = pid,
                            focalLengthMm = fl,
                            sensorWidthMm = sensorW,
                            equiv35mm = equiv,
                            fovDeg = fov,
                            aperture = ap,
                            zoomFactor = 0f // 占位，下面统一按基准换算
                        )
                    )
                }
            }

            if (raw.isEmpty()) {
                Log.w("Camera", "probeLenses 没读到任何镜头")
                return emptyList()
            }

            // ---------------- 统一换算等效焦距 ----------------
            // ⚠️ 刻意不用 lambda：Kotlin 推断不了 if/else 分支里 lambda 字面量的参数类型。
            //    改成先算出一个 Float 比例尺再相乘，逻辑等价且不会编译报错。
            val withEquiv: List<LensProfile> = raw.filter { it.equiv35mm > 0f }
            val scale: Float = if (withEquiv.isNotEmpty()) {
                // 有一部分头上报了传感器尺寸：拿「最接近 24mm 的那颗」当比例尺
                val b = withEquiv.minByOrNull { abs(it.equiv35mm - BASE_EQUIV_MM) }
                    ?: withEquiv.first()
                if (b.focalLengthMm > 0f) b.equiv35mm / b.focalLengthMm else 1f
            } else {
                // 全部头都没上报传感器尺寸（小米13 Ultra / iQOO 15 Ultra 的典型症状）：
                // 用主摄典型物理焦距当 1x 基准
                val b = raw.minByOrNull { abs(it.focalLengthMm - BASE_FOCAL_MM) } ?: raw.first()
                if (b.focalLengthMm > 0f) BASE_EQUIV_MM / b.focalLengthMm else 1f
            }
            Log.d("Camera", "等效焦距比例尺 ${"%.3f".format(scale)}（${withEquiv.size}/${raw.size} 颗头有传感器尺寸）")

            val filled = ArrayList<LensProfile>()
            for (p in raw) {
                val eq: Float = if (p.equiv35mm > 0f) p.equiv35mm else p.focalLengthMm * scale
                filled.add(p.copy(equiv35mm = eq))
            }

            // 以最接近 24mm 的那颗作为 1x 基准（原厂 App 也是拿主摄当 1x）
            val base = filled.minByOrNull { abs(it.equiv35mm - BASE_EQUIV_MM) } ?: filled.first()
            val result = filled
                .map {
                    val factor = if (base.equiv35mm > 0f) it.equiv35mm / base.equiv35mm else 1f
                    it.copy(zoomFactor = snapToNiceStop(factor))
                }
                .distinctBy { it.zoomFactor }
                .sortedBy { it.zoomFactor }

            // ★探测结果不合理时用出厂档位兜底★
            // 只探到一颗头 = 探测失败（多摄机至少主摄 + 超广角），
            // 此时给一组带 profile: 前缀的"虚拟镜头"，至少保证档位按钮还在。
            val final: List<LensProfile> = if (result.size >= 2) {
                result
            } else {
                Log.w("Camera", "只探到 ${result.size} 颗头，改用出厂档位 ${knownOpticalStops} 兜底")
                val out = ArrayList<LensProfile>()
                for (z in knownOpticalStops) {
                    out.add(
                        LensProfile(
                            id = PROFILE_LENS_PREFIX + z,
                            focalLengthMm = 0f,
                            sensorWidthMm = 0f,
                            equiv35mm = BASE_EQUIV_MM * z,
                            fovDeg = 0f,
                            aperture = null,
                            zoomFactor = z
                        )
                    )
                }
                out
            }

            Log.d(
                "Camera",
                "原厂镜头参数(基准 ${base.equiv35mm.roundToInt()}mm): " +
                        final.joinToString(", ") {
                            "${it.zoomFactor}x=${it.equiv35mm.roundToInt()}mm" +
                                    "(f=${it.focalLengthMm} sens=${it.sensorWidthMm}mm" +
                                    "${it.aperture?.let { a -> " F/$a" } ?: ""})"
                        }
            )
            if (final.isNotEmpty()) lensProbeCache[lensFacing] = final
            final
        } catch (e: Exception) {
            Log.w("Camera", "镜头参数探测失败", e)
            emptyList()
        }
    }

    /**
     * 扫描本机【真实可绑定】的摄像头 id，返回可直接拿去 bindToLifecycle 的镜头列表。
     *
     * ★为什么需要第二个探测函数★
     * [probeLenses] 探测失败时会产出带 "profile:" 前缀的虚拟镜头，那些 id 不能拿去绑定；
     * 而"物理直连"必须有真实 cameraId。所以这里单独扫一遍 CameraManager，
     * 返回的每一项都保证是真实 id。广角能不能切出去，全看这份列表里有没有 <1.0 的那颗。
     */
    @SuppressLint("NewApi")
    fun probePhysicalCameras(context: Context, lensFacing: Int): List<LensProfile> {
        physicalProbeCache[lensFacing]?.let { cached ->
            Log.d("Camera", "★可绑定物理头★ 命中缓存（${cached.size} 颗）")
            return cached
        }
        return try {
            val manager = cameraManager(context)
            val found = ArrayList<LensProfile>()

            for (id in manager.cameraIdList) {
                val chars = try {
                    manager.getCameraCharacteristics(id)
                } catch (e: Exception) {
                    continue
                }
                if (chars.get(CameraCharacteristics.LENS_FACING) != lensFacing) continue

                val fl = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    ?.firstOrNull()
                if (fl == null || fl <= 0f) continue

                val phys = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                val sensorW: Float = phys?.width ?: 0f
                val equiv: Float = if (sensorW > 0f) fl * 36f / sensorW else 0f

                found.add(
                    LensProfile(
                        id = id,
                        focalLengthMm = fl,
                        sensorWidthMm = sensorW,
                        equiv35mm = equiv,
                        fovDeg = 0f,
                        aperture = null,
                        zoomFactor = 0f
                    )
                )
            }

            if (found.isEmpty()) {
                Log.w("Camera", "可绑定物理头扫描：没找到任何 id")
                return emptyList()
            }

            val withEquiv = found.filter { it.equiv35mm > 0f }
            val scale: Float = if (withEquiv.isNotEmpty()) {
                val b = withEquiv.minByOrNull { abs(it.equiv35mm - BASE_EQUIV_MM) }
                    ?: withEquiv.first()
                if (b.focalLengthMm > 0f) b.equiv35mm / b.focalLengthMm else 1f
            } else {
                val b = found.minByOrNull { abs(it.focalLengthMm - BASE_FOCAL_MM) } ?: found.first()
                if (b.focalLengthMm > 0f) BASE_EQUIV_MM / b.focalLengthMm else 1f
            }

            val out = ArrayList<LensProfile>()
            for (p in found) {
                val eq: Float = if (p.equiv35mm > 0f) p.equiv35mm else p.focalLengthMm * scale
                out.add(p.copy(equiv35mm = eq))
            }
            val base = out.minByOrNull { abs(it.equiv35mm - BASE_EQUIV_MM) } ?: out.first()
            val sorted = out.map {
                val factor = if (base.equiv35mm > 0f) it.equiv35mm / base.equiv35mm else 1f
                it.copy(zoomFactor = snapToNiceStop(factor))
            }.sortedBy { it.zoomFactor }

            Log.d(
                "Camera",
                "★可绑定物理头★ " + sorted.joinToString(", ") { "${it.id}=${it.zoomFactor}x" } +
                        "　（<1.0 的那颗就是超广角）"
            )
            if (sorted.isNotEmpty()) physicalProbeCache[lensFacing] = sorted
            sorted
        } catch (e: Exception) {
            Log.w("Camera", "物理头扫描失败", e)
            emptyList()
        }
    }

    /** 虚拟镜头 id 前缀。带这个前缀的 id 不是真实 cameraId，不能拿去 bind */
    const val PROFILE_LENS_PREFIX = "profile:"

    /** 判断某个镜头 id 是不是"真实可绑定的 cameraId" */
    fun isRealCameraId(id: String): Boolean = !id.startsWith(PROFILE_LENS_PREFIX)

    /**
     * 组装一个"直连指定摄像头 id"的 CameraSelector。
     *
     * ★解决"只能调用主摄"的关键★
     * 小米 / iQOO 上逻辑摄像头的 zoomState 常常只报 min=1.0，setZoomRatio(0.6) 会被
     * 静默 clamp 回 1.0，超广角永远切不过去。CameraX 没有公开的"选物理头"API，
     * 官方推荐做法就是用 CameraFilter 按 cameraId 过滤。
     *
     * 注意：不是所有机器都允许单独打开某个 id，调用方必须 try/catch。
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun physicalCameraSelector(lensFacing: Int, physicalId: String): CameraSelector {
        return CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .addCameraFilter(
                // 用显式循环 + ArrayList，避免 Kotlin 的 List / MutableList 签名差异
                CameraFilter { cameraInfos ->
                    val picked = ArrayList<androidx.camera.core.CameraInfo>()
                    for (info in cameraInfos) {
                        val id = try {
                            Camera2CameraInfo.from(info).cameraId
                        } catch (e: Exception) {
                            null
                        }
                        if (id == physicalId) picked.add(info)
                    }
                    picked
                }
            )
            .build()
    }

    /**
     * 探测本机在指定比例下能拍到的【最大】JPEG 尺寸。
     * 供"高像素模式"使用：不再写死 5000 万，而是拿传感器真正能输出的最大尺寸
     * （可能是 5000 万、1 亿、2 亿，取决于机型和当前比例）。
     */
    fun probeMaxCaptureSize(
        context: Context,
        lensFacing: Int,
        aspect: Int? = AspectRatio.RATIO_4_3,
        screenRational: Rational? = null
    ): Size = pickCaptureSize(
        context, lensFacing,
        maxPixels = Int.MAX_VALUE,
        aspect = aspect,
        screenRational = screenRational
    )

    /**
     * 把本机摄像头家底全 dump 出来，定位"广角调不出来"到底卡在哪一环。
     * 重点看每颗头的 CONTROL_ZOOM_RATIO_RANGE 下限：
     * 只要某台机器这里报 1.0，CameraX 就永远不会帮你切到超广角。
     */
    @SuppressLint("NewApi")
    fun dumpCameraInventory(context: Context) {
        try {
            val manager = cameraManager(context)
            Log.d("Camera", "========== 摄像头清单开始 ==========")
            Log.d("Camera", "cameraIdList = ${manager.cameraIdList.joinToString(", ")}")

            for (id in manager.cameraIdList) {
                val chars = try {
                    manager.getCameraCharacteristics(id)
                } catch (e: Exception) {
                    Log.w("Camera", "[$id] 读取失败: ${e.message}")
                    continue
                }
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                val facingStr = when (facing) {
                    CameraCharacteristics.LENS_FACING_BACK -> "后置"
                    CameraCharacteristics.LENS_FACING_FRONT -> "前置"
                    else -> "外部"
                }
                val fls = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val fl = fls?.firstOrNull()
                val phys = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                val equiv = if (fl != null && fl > 0f && phys != null && phys.width > 0f) {
                    "${(fl * 36f / phys.width).roundToInt()}mm"
                } else "算不出"
                val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        chars.physicalCameraIds.joinToString(",")
                    } catch (e: Exception) {
                        "读取失败"
                    }
                } else "API<28"
                val zoomRange = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val r = chars.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                    if (r == null) "未上报" else "${r.lower} ~ ${r.upper}"
                } else "API<30"

                Log.d(
                    "Camera",
                    "[$id] $facingStr | 焦距=${fl ?: 0f}mm | 传感器宽=${phys?.width ?: 0f}mm " +
                            "| 等效=$equiv | 物理子头=[$physicalIds] | ZOOM_RATIO_RANGE=$zoomRange"
                )
            }
            Log.d("Camera", "========== 摄像头清单结束 ==========")
        } catch (e: Exception) {
            Log.w("Camera", "摄像头清单 dump 失败", e)
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

/** 顶部工具栏的胶囊小按钮。selected=true 时高亮成橙色 */
@Composable
        /**
         * 顶部状态栏上的小按钮（像素 / 画质 / 帧率 / 防抖 / 比例）。
         *
         * ★改成方块状★ 以前用 18.dp 圆角（胶囊形），现在收到 6.dp ——
         * 接近方形、只保留很小的圆角，看起来更硬朗，也不再像一颗颗圆形按键。
         * 需要胶囊形时把 corner 传大一点即可。
         */
        /**
         * @param compact 紧凑模式：缩小内边距和字号，让方块更窄。
         *   横屏左侧参数栏用它，避免黑边占掉太多横向空间、挤压取景框。
         */
fun TopChip(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    corner: Dp = TOP_CHIP_CORNER,
    compact: Boolean = false
) {
    val shape = RoundedCornerShape(corner)
    // 紧凑模式：左右内边距 10→6dp、上下 6→3dp、字号 12→10sp
    // 紧凑尺寸比竖屏小一档即可，别缩太狠 —— 之前收到 6/3/10sp 导致横屏栏太小不好点
    val padH: Dp = if (compact) TOP_CHIP_PAD_H_COMPACT else 10.dp
    val padV: Dp = if (compact) TOP_CHIP_PAD_V_COMPACT else 6.dp
    val fs = if (compact) TOP_CHIP_FONT_COMPACT else 12.sp
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) Color(0xFFFFA000) else Color.Black.copy(alpha = 0.4f)
            )
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
            .clickable { onClick() }
            .padding(horizontal = padH, vertical = padV),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.Black else Color.White,
            fontSize = fs,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/** 顶部小按钮的圆角：6.dp ≈ 方块状（原来是 18.dp 的胶囊形） */
private val TOP_CHIP_CORNER = 6.dp

/** 竖屏时顶部按钮之间的间隔（原来是 10.dp，收紧到 5.dp 让整排更紧凑） */
private val TOP_BAR_GAP_PORTRAIT = 5.dp

// ── 横屏竖排参数栏的紧凑尺寸 ──
// 比竖屏小一档（竖屏是 10/6/12sp/36dp），既省横向空间又不至于小到不好点。
// 之前缩到 6/3/10sp/28dp 反馈"太小"，这里回调到中间值。
private val TOP_CHIP_PAD_H_COMPACT = 8.dp
private val TOP_CHIP_PAD_V_COMPACT = 5.dp
private val TOP_CHIP_FONT_COMPACT = 11.sp
private val TOP_CHIP_ICON_COMPACT = 32.dp

/** 参数栏用到的只读状态。打包成一个对象，方便横排 / 竖排两种布局共用 */
private data class SettingsBarState(
    val isFront: Boolean,
    val mode: CaptureMode,
    val highResMode: Boolean,
    val highResLabel: String,
    val videoQuality: VideoQuality,
    val videoFrameRate: VideoFrameRate,
    val videoStabilization: Boolean,
    val photoRatio: PhotoRatio,
    val flashMode: Int,
    val autoRotate: Boolean
)

/** 参数栏用到的回调 */
private data class SettingsBarActions(
    val onHaptic: () -> Unit,
    val onHighResChange: (Boolean) -> Unit,
    val onVideoQualityChange: (VideoQuality) -> Unit,
    val onVideoFrameRateChange: (VideoFrameRate) -> Unit,
    val onStabilizationChange: (Boolean) -> Unit,
    val onPhotoRatioChange: (PhotoRatio) -> Unit,
    val onAutoRotateChange: (Boolean) -> Unit,
    val onFlashExpand: () -> Unit,
    val onAbout: () -> Unit
)

/**
 * 参数栏的实际内容：像素 / 画质 / 帧率 / 防抖 / 比例 / 旋转 / 闪光 / 关于。
 * 不关心自己被横排还是竖排，由外层容器决定。
 */
@Composable
private fun SettingsChipItems(st: SettingsBarState, act: SettingsBarActions, compact: Boolean = false) {
    // ── 像素按钮：只在【后置 + 拍照/专业模式】显示 ──
    // 前置摄像头通常只有固定分辨率，切像素没意义还容易绑失败
    if (!st.isFront && st.mode != CaptureMode.VIDEO) {
        TopChip(
            // 显示本机真实最大像素，不再写死 5000 万
            text = if (st.highResMode) st.highResLabel else "12M",
            selected = st.highResMode,
            onClick = {
                act.onHaptic()
                act.onHighResChange(!st.highResMode)
            },
            compact = compact
        )
    }

    // ── 录像模式：用【画质】和【帧率】两个按钮替代像素按钮 ──
    if (st.mode == CaptureMode.VIDEO) {
        TopChip(
            text = st.videoQuality.label,
            onClick = {
                act.onHaptic()
                val vals = VideoQuality.values()
                act.onVideoQualityChange(vals[(vals.indexOf(st.videoQuality) + 1) % vals.size])
            },
            compact = compact
        )
        TopChip(
            text = st.videoFrameRate.label,
            selected = st.videoFrameRate == VideoFrameRate.FPS_60,
            onClick = {
                act.onHaptic()
                act.onVideoFrameRateChange(
                    if (st.videoFrameRate == VideoFrameRate.FPS_30)
                        VideoFrameRate.FPS_60 else VideoFrameRate.FPS_30
                )
            },
            compact = compact
        )
        // 录像防抖。不再因 4K / 60帧 而隐藏——官方只是"不保证生效"，
        // 实际多数机器照样有效。真正不支持的机器由 stabilizationSupported 判定。
        TopChip(
            text = "防抖",
            selected = st.videoStabilization,
            onClick = {
                act.onHaptic()
                act.onStabilizationChange(!st.videoStabilization)
            },
            compact = compact
        )
    }

    // ── 拍摄比例：3:4 / 16:9 / 全屏（录像模式下同样可以切换）──
    PhotoRatio.values().forEach { r ->
        TopChip(
            text = r.label,
            selected = st.photoRatio == r,
            onClick = {
                act.onHaptic()
                if (st.photoRatio != r) act.onPhotoRatioChange(r)
            },
            compact = compact
        )
    }

    // ── 自动旋转开关 ──
    TopChip(
        text = "旋转",
        selected = st.autoRotate,
        onClick = {
            act.onHaptic()
            act.onAutoRotateChange(!st.autoRotate)
        },
        compact = compact
    )

    // ── 闪光灯：常驻一个当前模式的图标，点击后展开选择面板 ──
    Box(
        modifier = Modifier
            .size(if (compact) TOP_CHIP_ICON_COMPACT else 36.dp)
            .clip(CircleShape)
            .background(
                if (st.flashMode == 3) Color(0xFFFFC107).copy(alpha = 0.9f)
                else Color.Black.copy(alpha = 0.4f)
            )
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .clickable {
                act.onHaptic()
                act.onFlashExpand()
            },
        contentAlignment = Alignment.Center
    ) {
        FlashIcon(mode = st.flashMode)
    }

    Box(
        modifier = Modifier
            .size(if (compact) TOP_CHIP_ICON_COMPACT else 36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .clickable {
                act.onHaptic()
                act.onAbout()
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

/** 竖屏：参数栏横着排在顶部 */
@Composable
private fun SettingsChipsRow(
    modifier: Modifier = Modifier,
    st: SettingsBarState,
    act: SettingsBarActions
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TOP_BAR_GAP_PORTRAIT)
    ) {
        SettingsChipItems(st, act, compact = false)
    }
}

/** 横屏：参数栏竖着排在屏幕左边 */
@Composable
private fun SettingsChipsColumn(
    modifier: Modifier = Modifier,
    st: SettingsBarState,
    act: SettingsBarActions
) {
    Column(
        modifier = modifier,
        // 竖排时用 6.dp，配合紧凑方块，整条栏更窄、给取景框让出空间
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SettingsChipItems(st, act, compact = true)
    }
}

/** 闪光灯模式对应的文字标签（无障碍/日志用） */
fun flashLabel(mode: Int): String = when (mode) {
    0 -> "关闭"
    1 -> "自动"
    2 -> "开启"
    3 -> "手电"
    else -> "关闭"
}

/**
 * 闪光灯图标（纯 Canvas 画，不依赖图片资源）。
 * 0=关闭(闪电+红斜杠) 1=自动(闪电+A) 2=开启(闪电) 3=手电/常亮(闪电+星芒)
 */
@Composable
fun FlashIcon(
    mode: Int,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val mainColor = when {
        selected -> Color(0xFFFFC107)
        mode == 0 -> Color.White.copy(alpha = 0.55f)
        else -> Color.White
    }
    Canvas(modifier = modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val bolt = Path().apply {
            moveTo(w * 0.60f, h * 0.02f)
            lineTo(w * 0.18f, h * 0.58f)
            lineTo(w * 0.45f, h * 0.58f)
            lineTo(w * 0.36f, h * 0.98f)
            lineTo(w * 0.82f, h * 0.40f)
            lineTo(w * 0.53f, h * 0.40f)
            close()
        }
        drawPath(bolt, mainColor)

        when (mode) {
            0 -> {
                // 红色斜杠表示关闭
                drawLine(
                    color = Color(0xFFE53935),
                    start = Offset(w * 0.10f, h * 0.94f),
                    end = Offset(w * 0.94f, h * 0.10f),
                    strokeWidth = w * 0.11f,
                    cap = StrokeCap.Round
                )
            }
            1 -> {
                // 右下角一个 "A" 表示自动
                drawContext.canvas.nativeCanvas.drawText(
                    "A", w * 0.72f, h * 0.99f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = h * 0.46f
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        isAntiAlias = true
                    }
                )
            }
            3 -> {
                // 星芒：常亮/手电
                val cx = w * 0.82f
                val cy = h * 0.18f
                val r = w * 0.13f
                for (i in 0 until 4) {
                    val a = Math.toRadians((i * 45 - 45).toDouble())
                    drawLine(
                        color = mainColor,
                        start = Offset(cx - (cos(a) * r).toFloat(), cy - (sin(a) * r).toFloat()),
                        end = Offset(cx + (cos(a) * r).toFloat(), cy + (sin(a) * r).toFloat()),
                        strokeWidth = w * 0.055f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

enum class ProParam { EV, SHUTTER, ISO, WB, FOCUS }

/**
 * 拍照比例。
 * RATIO_4_3 / RATIO_16_9 直接用 CameraX 的标准比例；
 * FULL 按屏幕真实宽高比走，拍出来刚好铺满手机屏幕。
 */
enum class PhotoRatio(val label: String, val aspect: Int?) {
    RATIO_3_4("3:4", AspectRatio.RATIO_4_3),
    RATIO_16_9("16:9", AspectRatio.RATIO_16_9),
    FULL("全屏", null)
}

/** 录像画质档位。CameraX 的 Quality 会按机型自动挑对应的分辨率 */
enum class VideoQuality(val label: String, val quality: Quality) {
    HD("720P", Quality.HD),
    FHD("1080P", Quality.FHD),
    UHD("4K", Quality.UHD)
}

/** 录像帧率 */
enum class VideoFrameRate(val label: String, val fps: Int) {
    FPS_30("30", 30),
    FPS_60("60", 60)
}

// ==================== 设置持久化 ====================

/** 需要跨会话保存的全部设置 */
data class CameraSettings(
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val mode: CaptureMode = CaptureMode.PHOTO,
    val photoRatio: PhotoRatio = PhotoRatio.RATIO_3_4,
    val highResMode: Boolean = false,
    val flashMode: Int = 0,
    val videoQuality: VideoQuality = VideoQuality.FHD,
    val videoFrameRate: VideoFrameRate = VideoFrameRate.FPS_30,
    val videoStabilization: Boolean = false,
    /** 自动旋转：屏幕方向跟随传感器（关掉则跟随系统设置） */
    val autoRotate: Boolean = true,
    val zoom: Float = 1.0f,
    val proIso: Int = 0,
    val proShutterNs: Long = 0L,
    val proEv: Int = 0,
    val proFocus: Float = -1f,
    val proWb: Int = 0,
    /**
     * 变焦标定表（全局倍率 → linearZoom）。
     * ★性能优化的关键★：标定要逐档下发倍率再轮询 zoomState，通常要 1~2 秒，
     *   这是"打开相机慢"的主因。存下来之后，第二次打开直接用，跳过标定秒开。
     * 只在后置摄像头上标定，所以只存一份。
     */
    val linearMap: Map<Float, Float> = emptyMap()
)

/**
 * 一个不触发重组的可变容器。
 * 用途：Compose 里写 State 会触发重组，写普通对象的字段不会。
 * 我们用它在每次重组时把"最新设置"记下来，Activity 退到后台时直接读它落盘。
 */
class Holder<T>(var value: T)

/**
 * 设置的读写。用 SharedPreferences，纯同步 API，
 * 这样 Activity 被系统回收（onPause/onStop）时也能安全地立刻落盘。
 */
object SettingsStore {
    private const val PREFS = "camera_settings"

    private const val K_LENS = "lens_facing"
    private const val K_MODE = "mode"
    private const val K_RATIO = "photo_ratio"
    private const val K_HIGH_RES = "high_res"
    private const val K_FLASH = "flash"
    private const val K_VQ = "video_quality"
    private const val K_FPS = "video_fps"
    private const val K_STAB = "video_stab"
    private const val K_ROTATE = "auto_rotate"
    private const val K_LM = "linear_map"
    private const val K_ZOOM = "zoom"
    private const val K_ISO = "pro_iso"
    private const val K_SHUTTER = "pro_shutter"
    private const val K_EV = "pro_ev"
    private const val K_FOCUS = "pro_focus"
    private const val K_WB = "pro_wb"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 把标定表编成 "0.6=0.123;1.0=0.5" 这样的字符串 */
    private fun encodeLinearMap(m: Map<Float, Float>): String? {
        if (m.isEmpty()) return null
        return try {
            m.entries.joinToString(";") { "${it.key}=${it.value}" }
        } catch (e: Exception) {
            null
        }
    }

    /** 反序列化标定表 */
    private fun decodeLinearMap(raw: String?): Map<Float, Float> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            raw.split(";").mapNotNull { part ->
                val kv = part.split("=")
                if (kv.size != 2) return@mapNotNull null
                val k = kv[0].toFloatOrNull() ?: return@mapNotNull null
                val v = kv[1].toFloatOrNull() ?: return@mapNotNull null
                k to v
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /** 标定完成后单独落盘（立刻写，不等普通设置的防抖） */
    fun saveLinearMap(context: Context, m: Map<Float, Float>) {
        val enc = encodeLinearMap(m) ?: return
        try {
            prefs(context).edit().putString(K_LM, enc).apply()
            Log.d("Settings", "标定表已缓存（${m.size} 档），下次打开可跳过标定")
        } catch (e: Exception) {
            Log.w("Settings", "标定表缓存失败", e)
        }
    }

    fun load(context: Context): CameraSettings {
        return try {
            val p = prefs(context)
            if (!p.contains(K_MODE) && !p.contains(K_RATIO)) {
                Log.d("Settings", "无存档，使用默认设置")
                return CameraSettings()
            }
            CameraSettings(
                lensFacing = p.getInt(K_LENS, CameraSelector.LENS_FACING_BACK),
                mode = runCatching { CaptureMode.valueOf(p.getString(K_MODE, "") ?: "") }
                    .getOrDefault(CaptureMode.PHOTO),
                photoRatio = runCatching { PhotoRatio.valueOf(p.getString(K_RATIO, "") ?: "") }
                    .getOrDefault(PhotoRatio.RATIO_3_4),
                highResMode = p.getBoolean(K_HIGH_RES, false),
                flashMode = p.getInt(K_FLASH, 0),
                videoQuality = runCatching { VideoQuality.valueOf(p.getString(K_VQ, "") ?: "") }
                    .getOrDefault(VideoQuality.FHD),
                videoFrameRate = runCatching { VideoFrameRate.valueOf(p.getString(K_FPS, "") ?: "") }
                    .getOrDefault(VideoFrameRate.FPS_30),
                videoStabilization = p.getBoolean(K_STAB, false),
                autoRotate = p.getBoolean(K_ROTATE, true),
                linearMap = decodeLinearMap(p.getString(K_LM, null)),
                zoom = p.getFloat(K_ZOOM, 1.0f),
                proIso = p.getInt(K_ISO, 0),
                proShutterNs = p.getLong(K_SHUTTER, 0L),
                proEv = p.getInt(K_EV, 0),
                proFocus = p.getFloat(K_FOCUS, -1f),
                proWb = p.getInt(K_WB, 0)
            ).also {
                Log.d(
                    "Settings",
                    "已恢复上次设置: 模式=${it.mode.label} 比例=${it.photoRatio.label} " +
                            "高像素=${it.highResMode} 倍率=${it.zoom}x 闪光=${it.flashMode}"
                )
            }
        } catch (e: Exception) {
            Log.w("Settings", "读取设置失败，用默认值", e)
            CameraSettings()
        }
    }

    fun save(context: Context, s: CameraSettings) {
        try {
            prefs(context).edit()
                .putInt(K_LENS, s.lensFacing)
                .putString(K_MODE, s.mode.name)
                .putString(K_RATIO, s.photoRatio.name)
                .putBoolean(K_HIGH_RES, s.highResMode)
                .putInt(K_FLASH, s.flashMode)
                .putString(K_VQ, s.videoQuality.name)
                .putString(K_FPS, s.videoFrameRate.name)
                .putBoolean(K_STAB, s.videoStabilization)
                .putBoolean(K_ROTATE, s.autoRotate)
                .putFloat(K_ZOOM, s.zoom)
                .putInt(K_ISO, s.proIso)
                .putLong(K_SHUTTER, s.proShutterNs)
                .putInt(K_EV, s.proEv)
                .putFloat(K_FOCUS, s.proFocus)
                .putInt(K_WB, s.proWb)
                .apply()
            // 标定表单独维护：普通设置保存时不覆盖它
            if (s.linearMap.isNotEmpty()) encodeLinearMap(s.linearMap)?.let {
                prefs(context).edit().putString(K_LM, it).apply()
            }
            Log.d("Settings", "设置已保存（${s.zoom}x / ${s.mode.label} / ${s.photoRatio.label}）")
        } catch (e: Exception) {
            Log.w("Settings", "保存设置失败", e)
        }
    }
}

/**
 * HAL 完全不上报变焦能力时的兜底数码变焦上限。
 * 荣耀 50 SE 这类机器 zoomState 会报 min=1.0 max=1.0，
 * 不兜底的话所有变焦档位都会被过滤掉，连数码裁切都用不了。
 */
private const val FALLBACK_MAX_ZOOM = 8.0f

/** 手动曝光时帧时长相对曝光时间留出的余量（ns），1ms。vivo / 天玑 HAL 要求 frame >= exposure */
private const val FRAME_OVERHEAD_NS = 1_000_000L

/**
 * 档位按钮的倍率上限。超过这个值的纯数码变焦（iQOO 15 能到 100x）不生成按钮，
 * 想拉到极限可以用标尺拖。20x 以内基本还有可用性。
 */
private const val MAX_USEFUL_STOP = 20f

/**
 * 点变焦档位按钮（切换焦段）时的动画时长基准(ms)。
 *
 * 手感目标：前段冲得快、后段明显慢下来收尾。
 * 上一版收到 280~420ms 实测偏快，中间过程一晃就过去，看不出"减速"的那一段。
 * 现在放宽到 460~680ms，让尾段的减速过程看得见，又不至于拖沓。
 * 大幅度的变焦（跨好几档）用短时长，小幅度的用长时长，避免"拉到底等半天"。
 */
private const val ZOOM_ANIM_BASE_MS = 720f
/**
 * 每跨 1 倍变焦范围「节省」的时长(ms)。
 * ★这个值越小，大幅度变焦就越慢★
 * 以前 160f 会让"从 0.6x 拉到 10x"被压到下限，跨得越远反而越赶。
 * 现在降到 25f：大幅变焦几乎吃满 BASE(720ms)，小幅变焦也在 690~720ms 附近，
 * 整体节奏更接近"大幅度慢慢推、小幅度轻轻点"。
 * 还想再慢就把它调成 0f（所有幅度统一吃满 BASE）。
 */
private const val ZOOM_ANIM_SPAN_FACTOR = 25f
private const val ZOOM_ANIM_MIN_MS = 520f

/**
 * ★变焦曲线：先快、后段明显慢下来★
 * cubic-bezier(0.22, 0.86, 0.08, 1)
 *   · P1 = (0.22, 0.86)：x 很小而 y 很大 → 起步几乎立刻冲出去（前两成时间走掉大半路程）
 *   · P2 = (0.08, 1)：   y 顶到 1 → 后段收得很平，形成长长的减速尾巴
 * 这样"先快后慢"的对比很明显，跟 iPhone 切焦段的手感一致。
 *
 * 对比一下之前的几条：
 *   (0.55, 0.1, 0.3, 1) 起步有个"蓄力"过程，点档位会觉得相机顿一下才动；
 *   (0.32, 0.72, 0, 1)  起步好了，但尾段收敛过长、整体显得急躁 —— 就是"曲线太快"的来源。
 */
private val APPLE_EASE = CubicBezierEasing(0.22f, 0.86f, 0.08f, 1f)

/**
 * ★★ 真正的元凶：Compose 重组，不是 HAL ★★
 *
 * 之前三轮都判断错了方向：
 *   第一轮：以为"下发太密"，把间隔 24ms 放宽到 48ms —— 没用
 *   第二轮：以为"步数太多"，减到 4~5 步 —— 还是卡
 *   第三轮：干脆改硬切 —— 不卡了，但也没动画了
 *
 * 实际上真正吃性能的是这一行：
 *       displayedRatio = r        // ← 在变焦循环里，每步都执行
 *
 * displayedRatio 是 mutableFloatStateOf，它一变就会触发【整个 CameraApp 重组】——
 * 而 CameraApp 里包着 PreviewView(AndroidView)、所有按钮、所有动画。
 * 34 步 = 34 次全屏重组，主线程被彻底占满，中间帧全被丢掉，
 * 所以看起来就像"硬切"。
 *
 * 正确思路：HAL 下发频率 和 UI 刷新频率 必须解耦。
 *   · 给相机下发变焦很便宜（只是改个 crop region），可以做到 60fps
 *   · Compose 重组很贵，刷新到 ~14fps 肉眼就完全够（数字在跳就行）
 *   两者分开后：变焦本身极顺滑，界面又完全不掉帧。
 */

/** 给相机下发变焦的间隔(ms)。16ms ≈ 60fps，变焦过程极其顺滑 */
private const val ZOOM_ANIM_STEP_MS = 16L
/** 录制中编码器也在跑，稍微放宽一档 */
private const val ZOOM_ANIM_STEP_MS_REC = 24L

/**
 * 变焦过程中，把倍率【提交给 UI】的最小间隔(ms)。
 * 这是关键改动：以前每下发一次就更新一次 UI（16~24ms 一次），
 * 现在固定 ~72ms 一次（约 14fps），重组次数降到原来的 1/4 左右。
 * 肉眼看倍率数字跳动完全够用，但主线程彻底解放了。
 */
private const val ZOOM_UI_REFRESH_MS = 72L

/**
 * 变焦结束后等多久再恢复防抖。
 * 让 crop 先落定，否则 EIS 一恢复就要重新收敛稳定窗口，
 * 画面会在变焦结束的瞬间又顿一下。
 */
private const val STAB_RESTORE_DELAY_MS = 150L

/**
 * 拖动变焦的下发节流间隔(ms)。
 * detectTransformGestures 是按【指针事件】回调的，一秒能来 100+ 次，
 * 每个事件都 setZoomRatio 一次，防抖开着时等于每秒重建 100 次稳定窗口——必卡。
 * 节流后最多每 N ms 下发一次。
 */
private const val DRAG_ZOOM_THROTTLE_MS = 60L
private const val DRAG_ZOOM_THROTTLE_STAB_MS = 120L
private const val DRAG_ZOOM_THROTTLE_REC_MS = 150L

/**
 * 关于页展示的版本号。发新版时改这一处即可（两处显示都用它）。
 * 注意：更新检查判断用的是 build.gradle 里的 versionCode，跟这个字符串无关。
 */
private const val APP_VERSION_NAME = "3.1.1"

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

        // ★自动旋转★ 默认让屏幕方向跟随传感器（忽略系统"方向锁定"）。
        // 相机会横竖屏换布局，如果被系统锁定在竖屏，横屏布局就永远用不上。
        // 用户在设置栏里关掉"旋转"后，改回跟随系统设置（UNSPECIFIED）。
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR

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

    // 横竖屏。横屏时变焦控件和模式栏要挪到屏幕两边，别糊在取景框正中间
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // ── 设置持久化 ──
    // 启动后先从 SharedPreferences 恢复上次退出时的设置，settingsLoaded 置 true 才允许绑定相机。
    // 不这么做会先用默认值绑一次、恢复完再重绑，白白黑屏两下。
    var settingsLoaded by remember { mutableStateOf(false) }
    // 每次重组都把最新设置同步进来（写普通对象字段不触发重组），供退出时落盘
    val latestSettings = remember { Holder(CameraSettings()) }
    // 恢复出来的倍率，等标定完成后再真正下发给相机
    var restoredZoom by remember { mutableFloatStateOf(0f) }

    // ── 高像素模式 ──
    // 不再写死"5000万"：绑定时探测本机在所选比例下真正能输出的最大尺寸
    var maxPhotoPixels by remember { mutableIntStateOf(0) }

    // ── 物理摄像头直连 ──
    // 小米 / iQOO 上逻辑摄像头的 zoomState 常只报 min=1.0，setZoomRatio(0.6) 被 clamp 回 1.0，
    // 超广角永远切不过去。这时改为直接绑定那颗物理摄像头，等于原厂 App 的"切镜头"。
    var boundLensId by remember { mutableStateOf<String?>(null) }
    // 真实可绑定的物理头列表（由 probePhysicalCameras 独立扫描，保证是真 id）
    var physicalLensOptions by remember {
        mutableStateOf<List<DeviceCompatibility.LensProfile>>(emptyList())
    }
    // 本机允不允许单独打开某个摄像头 id。失败一次就永久关掉这条路径，退回数码变焦
    var physicalSwitchWorks by remember { mutableStateOf(true) }
    // 物理头绑定完成后要落到哪个"全局倍率"
    var pendingZoomTarget by remember { mutableFloatStateOf(0f) }
    // ★逻辑摄像头到不了广角★ HAL 把 minZoomRatio 报成 1.0，但本机确实有 0.6x 光学档。
    // 置 true 后，所有 <1.0 的目标一律走物理直连，不再浪费一次无效下发。
    var logicWideBlocked by remember { mutableStateOf(false) }

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
    // ★标定结果跨"横竖屏切换"保留★
    // 横竖屏切换会销毁重建 Activity，用 remember 存的标定结果会丢，
    // 于是每次转屏都要重新跑一遍标定（还会闪一下"正在校准..."遮罩）。
    // 改成 rememberSaveable：结果写进 Bundle，重建后直接恢复，不再重复标定。
    var linearMap by rememberSaveable(stateSaver = linearMapSaver) {
        mutableStateOf<Map<Float, Float>>(emptyMap())
    }
    var scanDone by remember { mutableStateOf(false) }
    var hasCalibrated by rememberSaveable { mutableStateOf(false) }
    var zoomJob by remember { mutableStateOf<Job?>(null) }

    var showRuler by remember { mutableStateOf(false) }
    var lastRulerInteraction by remember { mutableLongStateOf(0L) }
    // 拖动变焦节流用的两个记录：上次真正下发的时间 + 被节流掉时累积的缩放量
    var lastZoomApplyAt by remember { mutableLongStateOf(0L) }
    var pendingZoomFactor by remember { mutableFloatStateOf(1f) }

    // ★不触发重组的标志位★
    // 用数组包一层，这样改它的值不会引发 Compose 重组（State 会）。
    // 变焦动画期间置 true，告诉轮询循环别来更新 displayedRatio，
    // 避免"动画在高频下发"和"轮询在高频刷新"两股力量打架。
    val zoomAnimating = remember { booleanArrayOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    var highResMode by remember { mutableStateOf(false) }
    var flashMode by remember { mutableIntStateOf(0) }
    // 点闪光灯后展开那一排图标，再点收起
    var flashExpanded by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showReward by remember { mutableStateOf(false) }

    // 拍照比例：3:4 / 16:9 / 全屏
    var photoRatio by remember { mutableStateOf(PhotoRatio.RATIO_3_4) }

    // 录像画质与帧率（视频模式下替代像素按钮）
    var videoQuality by remember { mutableStateOf(VideoQuality.FHD) }
    var videoFrameRate by remember { mutableStateOf(VideoFrameRate.FPS_30) }
    // 录像防抖开关。★默认关闭★（进视频模式时不自动开启，由用户手动打开）
    var videoStabilization by remember { mutableStateOf(false) }
    // ★自动旋转开关★ true = 屏幕方向跟随传感器；false = 跟随系统设置
    var autoRotate by remember { mutableStateOf(true) }
    var stabilizationSupported by remember { mutableStateOf(true) }
    // 变焦期间临时挂起了防抖（true 表示要记得恢复）
    var stabSuspended by remember { mutableStateOf(false) }

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
     * 规整变焦上下限。
     * 荣耀 50 SE 这类机器 HAL 只报 min=1.0 max=1.0（等于宣称"不支持变焦"），
     * 不兜底的话所有变焦档位都会被过滤掉，连数码裁切都用不了。
     * 这里给它一个数码裁切范围，保证至少能变焦。
     */
    fun normalizeZoomRange(rawMin: Float, rawMax: Float): Pair<Float, Float> {
        var mn = rawMin
        var mx = rawMax
        if (mn.isNaN() || mn <= 0f) mn = 1.0f
        if (mx.isNaN() || mx <= 1.0f + 0.001f) mx = FALLBACK_MAX_ZOOM
        if (mx <= mn) mx = mn * FALLBACK_MAX_ZOOM
        return mn to mx
    }

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

    // ---------------- 物理摄像头直连 ----------------

    /** 当前绑定物理头的光学倍率；未直连（用逻辑摄像头）时为 1.0 */
    fun boundLensFactor(): Float =
        boundLensId?.let { id -> lensProfiles.firstOrNull { it.id == id }?.zoomFactor } ?: 1f

    /** 全局倍率 → 真正下发给相机的本地倍率（物理头自己就是 1.0x，要除掉它的光学倍率） */
    /**
     * 全局倍率 → 真正下发给相机的本地倍率。
     *
     * ★★ 这里踩过一个大坑，务必保持现在这个写法 ★★
     * 以前写成 (global / boundLensFactor()).coerceAtLeast(1f) 且无条件生效。
     * 未直连物理头时 boundLensFactor() = 1f，于是：
     *     localZoomOf(0.6f) = (0.6 / 1).coerceAtLeast(1f) = 1.0   ← 广角被吃掉！
     * 表现为"点 0.6x 按钮画面完全没变化"，而原本的代码明明是可以调广角的。
     *
     * 那句 coerceAtLeast(1f) 的本意是"物理头自己就是 1.0x，不能再小"，
     * 只在【直连物理头】时才成立。未直连时必须原样下发。
     */
    fun localZoomOf(global: Float): Float {
        val lid = boundLensId
        if (lid == null) return global          // 逻辑摄像头：原样下发
        val factor: Float =
            lensProfiles.firstOrNull { it.id == lid }?.zoomFactor ?: 1f
        return if (factor > 0f) (global / factor).coerceAtLeast(1f) else global
    }

    /** 当前状态下的全局倍率下限（物理头直连时，最低就是这颗头自己的倍率） */
    fun effMinZoom(): Float =
        if (boundLensId != null) boundLensFactor() else minRatio

    /** 当前状态下的全局倍率上限（物理头上再给 4 倍数码裁切余量，但不超全局上限） */
    fun effMaxZoom(): Float =
        if (boundLensId != null) (boundLensFactor() * 4f).coerceAtMost(maxRatio) else maxRatio

    /**
     * 承载某个全局倍率的物理头：不超过它、且最接近的那颗。
     * ★优先用 probePhysicalCameras 扫出来的真实 id 池★
     * lensProfiles 探测失败时装的是画像虚拟 id，拿去 bind 会失败。
     */
    fun lensForTarget(global: Float): DeviceCompatibility.LensProfile? {
        val real = physicalLensOptions.filter { DeviceCompatibility.isRealCameraId(it.id) }
        val pool: List<DeviceCompatibility.LensProfile> =
            if (real.isNotEmpty()) real
            else lensProfiles.filter { DeviceCompatibility.isRealCameraId(it.id) }
        if (pool.isEmpty()) return null
        return pool.filter { it.zoomFactor <= global + 0.001f }
            .maxByOrNull { it.zoomFactor }
            ?: pool.minByOrNull { it.zoomFactor }
    }

    /**
     * 目标倍率如果"当前这颗头到不了"，就切到能到位那颗物理头。
     *
     * @return true 表示已发起切换（会重新绑定相机），调用方应直接 return，
     *         倍率会在绑定完成后按 pendingZoomTarget 落位。
     */
    fun requestLensSwitchIfNeeded(target: Float): Boolean {
        if (!physicalSwitchWorks) return false
        if (isFront) return false
        // 用真实可绑定物理头的数量判断，而不是 lensProfiles
        // （后者可能全是画像虚拟 id，size 看着够其实一颗都 bind 不了）
        val pool = physicalLensOptions.filter { DeviceCompatibility.isRealCameraId(it.id) }
        if (pool.size < 2) return false
        val lens = lensForTarget(target) ?: return false

        val inRange = target >= minRatio - 0.01f && target <= maxRatio + 0.01f
        val sameLens = lens.id == boundLensId
        val need: Boolean = when {
            // ★唯一允许"抢占"的情形：实测确认逻辑头到不了广角★
            // logicWideBlocked 只在下面两种情况被置 true：
            //   ① 绑定时读到 HAL 原生下限 >= 1.0，但本机确实有 <1.0 的光学档
            //   ② 变焦校验发现目标 0.6x 实际回读是 1.0x
            // 没坐实之前一律不直连 —— 否则会抢掉本来能正常工作的 setZoomRatio。
            boundLensId == null && logicWideBlocked && target < 0.99f -> !sameLens

            // 当前用逻辑摄像头：只在"逻辑头范围根本够不到"时才走直连。
            // ⚠️ 这里曾经按机型预判优先直连，结果把本来能正常变焦的小米机型搞坏了，
            //    所以现在不看机型，只看范围。
            boundLensId == null ->
                !inRange && abs(lens.zoomFactor - target) < 0.01f

            // 已直连某颗头：目标跑出这颗头的可用区间了才换头
            else -> !sameLens && (target < effMinZoom() - 0.01f || target > effMaxZoom() + 0.01f)
        }

        if (!need) return false

        // ★离开当前物理头时，优先"退回逻辑摄像头"而不是直连另一颗物理头★
        // 例：广角头(0.6x)上想切到 3x。3x 逻辑摄像头用 setZoomRatio 就能到，
        // 没必要再去绑一颗物理主摄 —— 每次绑物理头都要 unbindAll 重来，
        // 预览会先闪一下、画面比例也跟着变一下（就是反馈里"突然更改一下比例"的现象）。
        // 回到逻辑头之后，后续变焦都是纯 setZoomRatio，不再反复重绑。
        if (boundLensId != null && target >= minRatio - 0.01f && target <= maxRatio + 0.01f) {
            Log.d(
                "Camera",
                "离开物理头 ${boundLensId}，退回逻辑摄像头并落到 ${"%.2f".format(target)}x"
            )
            pendingZoomTarget = target
            boundLensId = null
            return true
        }

        Log.d(
            "Camera",
            "切换物理头 ${lens.id}（${lens.zoomFactor}x）以到达 ${"%.2f".format(target)}x"
        )
        pendingZoomTarget = target
        boundLensId = lens.id      // 改它就会触发绑定流程重跑
        return true
    }

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

    /**
     * 当前录像配置下防抖"是否保证生效"。
     * CameraX 官方写了：视频防抖只在【分辨率 ≤1920x1080 且帧率 ≤30fps】时保证生效。
     *
     * ⚠️ 注意：4K / 60帧 只是"不保证"，不代表一定无效。
     *    之前我把这两种情况直接判成不可用并隐藏按钮，属于过度限制——
     *    很多机器在 4K 下防抖照样工作。现在改为：仍然允许开启，只打日志提示。
     *    真正不支持的机器由 stabilizationSupported 判定。
     */
    fun stabilizationGuaranteed(): Boolean =
        videoQuality != VideoQuality.UHD && videoFrameRate != VideoFrameRate.FPS_60

    /**
     * 是否真的要开录像防抖：开关打开 + 机器支持 + 当前在录像模式。
     * 注意：写成函数而不是 val get()，因为这是 CameraApp() 函数内部的局部声明，
     * 局部变量不允许带 getter。
     */
    fun wantStabilization(): Boolean =
        videoStabilization && stabilizationSupported && mode == CaptureMode.VIDEO

    /**
     * 直接下发防抖开关（EIS + OIS），不做任何模式判断。
     * 给变焦期间"临时挂起 / 恢复"用——那时需要绕过 mode 检查强行关掉。
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun setStabilizationRaw(enabled: Boolean) {
        val cam = camera ?: return
        try {
            val b = CaptureRequestOptions.Builder()
            b.setCaptureRequestOption(
                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                if (enabled) CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
                else CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
            )
            b.setCaptureRequestOption(
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                if (enabled) CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
                else CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
            )
            Camera2CameraControl.from(cam.cameraControl).setCaptureRequestOptions(b.build())
        } catch (e: Exception) {
            Log.w("Camera", "设置防抖失败", e)
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

            // 防抖要跟手动参数一起下发：setCaptureRequestOptions 是全量替换，
            // 分开调会把对方清掉。录像模式下才开，拍照时开了会压低分辨率。
            val stabOn = wantStabilization()
            b.setCaptureRequestOption(
                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                if (stabOn) CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
                else CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
            )
            b.setCaptureRequestOption(
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                if (stabOn) CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
                else CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
            )

            c2.setCaptureRequestOptions(b.build())
            Log.d(
                "Camera",
                "专业参数: ISO=$iso, S=$shutter, EV=$ev(${limits.evToSteps(ev)}步), F=$focus, WB=$wb"
            )
        } catch (e: Exception) {
            Log.e("Camera", "应用专业参数失败", e)
        }
    }

    /**
     * 录像防抖开关。
     * CameraX 1.3.4 还没有 setVideoStabilizationEnabled（那是 1.4.0 才加的），
     * 所以用 Camera2Interop 直接下发 CONTROL_VIDEO_STABILIZATION_MODE。
     *
     * ⚠️ 只在录像模式开：防抖靠裁剪画面实现，开着会压低拍照分辨率
     *    （这也是以前 iQOO 15 / OPPO 上 5000万失效的原因之一）。
     *
     * ⚠️ setCaptureRequestOptions 是全量替换，所以专业模式下必须走 applyProParams
     *    把防抖和手动参数一起下发，否则两边会互相清掉。
     *
     * ⚠️ 必须定义在 applyProParams 之后：Kotlin 的局部函数只能引用【前面】已声明的，
     *    放在前面会 Unresolved reference。
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun applyStabilization(enabled: Boolean) {
        val cam = camera ?: return
        if (mode == CaptureMode.PRO && manualControlSupported) {
            applyProParams(proIso, proShutterNs, proEv, proFocus, proWb)
            return
        }
        val on = enabled && wantStabilization()
        setStabilizationRaw(on)
        Log.d(
            "Camera",
            "录像防抖: ${if (on) "开" else "关"}" +
                    if (on && !stabilizationGuaranteed())
                        "（当前 4K/60帧，官方不保证生效，如画面异常可关闭）"
                    else ""
        )
    }

    // ── 启动时恢复上次退出时的设置 ──
    // 必须在绑定相机的 LaunchedEffect 之前声明：Compose 按声明顺序启动协程，
    // 放后面会先用默认值绑一次相机，恢复完再重绑，白白黑屏两下。
    LaunchedEffect(Unit) {
        val s = withContext(Dispatchers.IO) { SettingsStore.load(context) }
        lensFacing = s.lensFacing
        mode = s.mode
        photoRatio = s.photoRatio
        highResMode = s.highResMode
        flashMode = s.flashMode
        videoQuality = s.videoQuality
        videoFrameRate = s.videoFrameRate
        videoStabilization = s.videoStabilization
        autoRotate = s.autoRotate
        proIso = s.proIso
        proShutterNs = s.proShutterNs
        proEv = s.proEv
        proFocus = s.proFocus
        proWb = s.proWb
        restoredZoom = s.zoom
        selectedZoom = s.zoom
        displayedRatio = s.zoom
        currentRatio = s.zoom

        // ★用上次缓存的标定表，跳过本次标定★
        // 标定要逐档下发倍率再轮询，通常 1~2 秒，是"打开相机慢"的主因。
        // 有缓存就直接恢复并标记已标定，绑定流程不会再跑标定循环。
        if (s.linearMap.isNotEmpty()) {
            linearMap = s.linearMap
            hasCalibrated = true
            Log.d("Camera", "已加载缓存标定表（${s.linearMap.size} 档），本次跳过标定")
        }

        settingsLoaded = true
    }

    // 每次重组把最新设置同步进普通对象（写字段不触发重组），供退出时落盘
    latestSettings.value = CameraSettings(
        lensFacing = lensFacing,
        mode = mode,
        photoRatio = photoRatio,
        highResMode = highResMode,
        flashMode = flashMode,
        videoQuality = videoQuality,
        videoFrameRate = videoFrameRate,
        videoStabilization = videoStabilization,
        autoRotate = autoRotate,
        zoom = selectedZoom,
        proIso = proIso,
        proShutterNs = proShutterNs,
        proEv = proEv,
        proFocus = proFocus,
        proWb = proWb
    )

    // ── 设置自动保存（防抖 400ms）──
    // 好处：就算 App 被系统直接杀掉（不走 onPause），最近一次操作也大概率已经落盘
    LaunchedEffect(
        settingsLoaded, lensFacing, mode, photoRatio, highResMode, flashMode,
        videoQuality, videoFrameRate, videoStabilization, autoRotate, selectedZoom,
        proIso, proShutterNs, proEv, proFocus, proWb
    ) {
        if (!settingsLoaded) return@LaunchedEffect
        delay(400) // 连续拖动参数时只存最后一次
        val snap = latestSettings.value
        withContext(Dispatchers.IO) { SettingsStore.save(context, snap) }
    }

    // ── 退到后台 / 退出应用时立刻落盘（不等防抖）──
    DisposableEffect(Unit) {
        val activity = context as? ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                SettingsStore.save(context, latestSettings.value)
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    // ── 自动旋转 ──
    // FULL_SENSOR：方向跟随传感器，不受系统"方向锁定"影响（相机横竖屏布局要靠它）。
    // UNSPECIFIED：交回系统，由用户在系统里决定要不要转。
    LaunchedEffect(autoRotate) {
        val activity = context as? ComponentActivity ?: return@LaunchedEffect
        activity.requestedOrientation = if (autoRotate) {
            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        Log.d("Camera", "自动旋转: $autoRotate")
    }

    // ── 横屏进入沉浸式全屏 ──
    // 横屏本来竖向空间就紧张，系统状态栏（时间/电量/信号）横在顶部会压住顶部按钮，
    // 导航栏占掉底部一条也会挤到快门。横屏时把两条系统栏都藏掉，整块屏幕全给相机。
    // 竖屏恢复显示，保持常规手感。
    // BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE：需要看时间时从边缘滑一下即可临时唤出。
    DisposableEffect(isLandscape) {
        val window = (context as? ComponentActivity)?.window
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isLandscape) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                Log.d("Camera", "横屏：进入沉浸式全屏（隐藏状态栏与导航栏）")
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            // 退出时务必恢复，否则回到桌面系统栏还是藏着的
            val w = (context as? ComponentActivity)?.window
            if (w != null) {
                WindowInsetsControllerCompat(w, w.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
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
            // 一次性 dump 摄像头家底，排查"广角调不出来"时看这一块
            withContext(Dispatchers.IO) {
                DeviceCompatibility.dumpCameraInventory(context)
            }
        } catch (e: Exception) {
            Log.e("Camera", "获取 Provider 失败", e)
        }
    }

    // 注意：videoStabilization 不放这里——它只是下发一个 Camera2 参数，
    // 不需要重新绑定（重绑会黑屏一下）。防抖单独用下面的 LaunchedEffect 处理。
    LaunchedEffect(
        settingsLoaded, lensFacing, highResMode, photoRatio,
        videoQuality, videoFrameRate, boundLensId
    ) {
        // 设置还没恢复完就别绑，否则会用默认值绑一次再重绑一次
        if (!settingsLoaded) return@LaunchedEffect

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

        // 固定等待从 100ms 减到 30ms：只是给 PreviewView 一点喘息，不需要那么久
        if (!hasCalibrated) scanDone = false
        delay(30)

        try {
            // ★两次探测并行发起★
            // probeLenses 和 probePhysicalCameras 都要遍历 CameraCharacteristics，
            // 串行跑是两份完整耗时；它们互不依赖，用 async 同时跑能省掉一半。
            val lensesDeferred = async(Dispatchers.IO) {
                DeviceCompatibility.probeLenses(context, lensFacing)
            }
            val physicalDeferred = async(Dispatchers.IO) {
                DeviceCompatibility.probePhysicalCameras(context, lensFacing)
            }

            // 每次打开/切换相机都重新读取原厂镜头参数（焦距、传感器尺寸、光圈），
            // 后面的变焦档位、标定、吸附全部基于这份真实光学数据
            val lenses = lensesDeferred.await()
            lensProfiles = lenses
            // 光学档位：只记真实物理头的倍率，供吸附 / 高画质模式 / 镜头标签使用。
            // 按钮档位不直接用它（见下方），因为很多机器没有 2x 物理头，会导致 2x 按钮消失。
            // ⚠️ knownOpticalStops 是 DeviceCompatibility 的成员，必须带前缀，
            //    裸写会报 Unresolved reference
            opticalStops = if (lenses.size >= 2) {
                lenses.map { it.zoomFactor }.distinct().sorted()
            } else DeviceCompatibility.knownOpticalStops
            zoomStopsFromHardware = opticalStops.isNotEmpty()
            Log.d("Camera", "光学档位: ${opticalStops.joinToString("/").ifEmpty { "未探测到" }}")

            // ★独立扫描真实可绑定物理头★
            // 这一步不看 probeLenses 的结果，直接问 CameraManager 要 cameraId 列表，
            // 保证"物理直连"有真实 id 可用 —— 广角能不能切出去全靠它。
            // 直接取并行任务的结果（已经在跑或已跑完，这里几乎不额外耗时）
            physicalLensOptions = physicalDeferred.await()
            if (physicalLensOptions.size >= 2) {
                // 扫到多颗真实头，说明确实是多摄机，光学档位以这份真实数据为准
                val realStops = physicalLensOptions.map { it.zoomFactor }.distinct().sorted()
                if (realStops.size >= 2) {
                    opticalStops = realStops
                    zoomStopsFromHardware = true
                    Log.d("Camera", "光学档位已按真实物理头修正: ${realStops.joinToString("/")}")
                }
            }

            // vivo 适配：getInstance().get() 会阻塞（vivo 上 HAL 初始化更慢），
            // 必须切到 IO 线程，否则主线程掉帧甚至 ANR
            val provider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }
            // 目标比例：3:4 / 16:9 用标准值，"全屏"按屏幕真实像素比算（如 20:9）
            // 注意取 大:小，因为 CameraX 给的尺寸都是横屏方向（宽 > 高）
            val screenRational = run {
                val dm = context.resources.displayMetrics
                val w = dm.widthPixels.coerceAtLeast(1)
                val h = dm.heightPixels.coerceAtLeast(1)
                Rational(maxOf(w, h), minOf(w, h))
            }
            val screenRatio =
                screenRational.numerator.toFloat() /
                        screenRational.denominator.toFloat().coerceAtLeast(1f)

            /**
             * 给 ResolutionSelector 套上比例设置。
             *
             * ⚠️ 关键：AspectRatioStrategy 的构造函数只接受 Int 常量
             *    （AspectRatio.RATIO_4_3 / RATIO_16_9），**不接受任意 Rational**。
             *    CameraX 官方只支持这两个比例，想要"全屏"这类任意比例，
             *    必须改用 setResolutionFilter 自己从候选尺寸里挑。
             */
            val applyAspect: (ResolutionSelector.Builder) -> Unit = { b ->
                when (photoRatio) {
                    PhotoRatio.RATIO_3_4 -> b.setAspectRatioStrategy(
                        AspectRatioStrategy(AspectRatio.RATIO_4_3, AspectRatioStrategy.FALLBACK_RULE_AUTO)
                    )
                    PhotoRatio.RATIO_16_9 -> b.setAspectRatioStrategy(
                        AspectRatioStrategy(AspectRatio.RATIO_16_9, AspectRatioStrategy.FALLBACK_RULE_AUTO)
                    )
                    PhotoRatio.FULL -> b.setResolutionFilter { supportedSizes, _ ->
                        // 优先挑跟屏幕比例一致的；一台机器都不匹配时，退而求其次取最接近的
                        val near = supportedSizes.filter {
                            abs(it.width.toFloat() / it.height - screenRatio) < screenRatio * 0.02f
                        }
                        if (near.isNotEmpty()) {
                            near.sortedByDescending { it.width * it.height }
                        } else {
                            supportedSizes.sortedWith(
                                compareBy<Size> { abs(it.width.toFloat() / it.height - screenRatio) }
                                    .thenByDescending { it.width * it.height }
                            )
                        }
                    }
                }
            }

            // 预览分辨率：保持原本的写法（按所选比例给 Preview 一个 ResolutionSelector）。
            // ⚠️ 这里曾经改成"不设分辨率让 CameraX 自己挑"，理由是担心锁死分辨率会
            //    妨碍物理头切换。但实测原本带 ResolutionSelector 的版本是能正常调广角的，
            //    改掉反而引入了不确定性。既然原配置可用，就恢复原配置。
            val preview = Preview.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder().also { applyAspect(it) }.build()
                )
                .build()
                .also { it.setSurfaceProvider(pv.surfaceProvider) }

            val resolutionSelector = if (highResMode) {
                // ★高像素 = 本机在所选比例下能拍到的最大像素，不再写死 5000 万★
                // 2 亿像素的小米、1 亿的三星、5000 万的 iQOO 各不相同，
                // 所以直接问 HAL 要它真正支持的最大 JPEG 尺寸，UI 上显示真实数字。
                // 另外必须告诉 CameraX 优先分辨率而不是帧率；
                // 且下面不再创建 VideoCapture（三流共存会把拍照分辨率压回 1200 万）。
                val maxSize = withContext(Dispatchers.IO) {
                    DeviceCompatibility.probeMaxCaptureSize(
                        context, lensFacing,
                        aspect = photoRatio.aspect,
                        screenRational = screenRational
                    )
                }
                maxPhotoPixels = maxSize.width * maxSize.height
                Log.d(
                    "Camera",
                    "高像素模式：本机最大 ${maxSize.width}x${maxSize.height}" +
                            "（约 ${maxPhotoPixels / 1_000_000}M）"
                )
                ResolutionSelector.Builder()
                    .setAllowedResolutionMode(ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            maxSize,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .also { applyAspect(it) }
                    .build()
            } else {
                // vivo 适配：不再写死 4080x3060，改成问 HAL 要一个它真正支持的尺寸
                val probed = DeviceCompatibility.pickCaptureSize(
                    context, lensFacing,
                    aspect = photoRatio.aspect,
                    screenRational = screenRational
                )
                // 小米部分机型 HAL 上报的尺寸偏激进，直接绑会失败。
                // 原来写死 4000x3000 会忽略用户选的比例（选 16:9 也给 4:3），
                // 所以改成"按用户比例重新挑一个 1300 万以内的安全尺寸"。
                val targetSize = if (DeviceCompatibility.isXiaomi &&
                    probed.width * probed.height > 13_000_000
                ) {
                    DeviceCompatibility.pickCaptureSize(
                        context, lensFacing, maxPixels = 13_000_000,
                        aspect = photoRatio.aspect,
                        screenRational = screenRational
                    )
                } else probed
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(targetSize, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                    )
                    .also { applyAspect(it) }
                    .build()
            }

            val ic = ImageCapture.Builder()
                .setResolutionSelector(resolutionSelector)
                .setCaptureMode(
                    if (highResMode) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                    else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                )
                .build()

            // 录像用例。高像素模式下不创建——三流共存会拖低拍照分辨率
            val vc = if (highResMode) null else {
                val recorder = Recorder.Builder()
                    .setQualitySelector(
                        QualitySelector.from(
                            videoQuality.quality,
                            FallbackStrategy.higherQualityOrLowerThan(Quality.FHD)
                        )
                    )
                    .build()
                VideoCapture.Builder(recorder)
                    .setTargetFrameRate(Range(videoFrameRate.fps, videoFrameRate.fps))
                    .build()
            }

            val baseSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            // ── 物理摄像头直连 ──
            // 小米 / iQOO 上逻辑摄像头切不动副摄时，直接按 cameraId 绑定那颗头，
            // 等价于原厂 App 的"切镜头"。
            val lensId = boundLensId
            val usePhysical = lensId != null &&
                    physicalSwitchWorks &&
                    DeviceCompatibility.isRealCameraId(lensId)
            // 局部变量收窄，避免用 !! 强制解包
            val targetId: String? = if (usePhysical) lensId else null
            val selector: CameraSelector = if (targetId != null) {
                try {
                    DeviceCompatibility.physicalCameraSelector(lensFacing, targetId)
                } catch (e: Exception) {
                    Log.w("Camera", "构造物理头选择器失败，退回逻辑摄像头", e)
                    baseSelector
                }
            } else baseSelector

            provider.unbindAll()
            var bound = false

            if (vc != null) {
                try {
                    camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, ic, vc)
                    imageCapture = ic
                    videoCapture = vc
                    bound = true
                } catch (e: Exception) {
                    Log.w("Camera", "三流绑定失败", e)
                }
            }

            if (!bound) {
                try {
                    provider.unbindAll()
                    camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, ic)
                    imageCapture = ic
                    videoCapture = null
                    bound = true
                } catch (e: Exception) {
                    Log.e("Camera", "两流绑定也失败", e)
                }
            }

            // 物理头单独打开失败（部分 ROM 不允许）→ 永久关掉这条路径，退回逻辑摄像头
            if (!bound && usePhysical) {
                physicalSwitchWorks = false
                Log.w("Camera", "物理头 $lensId 无法单独打开，已禁用物理直连")
                try {
                    provider.unbindAll()
                    camera = provider.bindToLifecycle(lifecycleOwner, baseSelector, preview, ic)
                    imageCapture = ic
                    videoCapture = null
                    bound = true
                } catch (e2: Exception) {
                    Log.e("Camera", "退回逻辑摄像头后仍绑定失败", e2)
                }
            }

            // 有些 ROM 会静默忽略 CameraFilter，照样开逻辑头。绑定后核对一次真实 cameraId
            if (bound && usePhysical) {
                val camNow = camera
                val actualId = if (camNow != null) {
                    try {
                        Camera2CameraInfo.from(camNow.cameraInfo).cameraId
                    } catch (e: Exception) {
                        null
                    }
                } else null
                if (actualId == lensId) {
                    Log.d("Camera", "★物理头直连成功: $lensId★")
                } else {
                    physicalSwitchWorks = false
                    Log.w("Camera", "物理头直连未生效（实际开的是 $actualId），已禁用该路径")
                }
            }

            // 录像防抖。CameraX 1.3.4 还没有 setVideoStabilizationEnabled（那是 1.4.0 才加的），
            // 所以走 Camera2Interop 直接下发 CONTROL_VIDEO_STABILIZATION_MODE。
            // 同时打开光学防抖(OIS)，iQOO 15 这类机器有独立的 OIS 马达。
            stabilizationSupported = DeviceCompatibility.isVideoStabilizationSupported(context, camera)
            applyStabilization(videoStabilization)

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
                val (mn, mx) = normalizeZoomRange(zs.minZoomRatio, zs.maxZoomRatio)
                if (boundLensId == null) {
                    // 逻辑摄像头：它上报的就是全局范围。
                    //
                    // ★HAL 谎报下限★ 小米 / iQOO 的逻辑头常把 minZoomRatio 报成 1.0，
                    // 等于宣称"没有广角"。代码里到处是 coerceIn(minRatio, ...)，
                    // 用户点 0.6x 会被直接 clamp 回 1.0 —— 这就是广角调不出来的直接原因。
                    // 只要探测到确实存在更广的光学档位，就强制把下限放宽到那个档位。
                    val opticalMin: Float? = opticalStops.filter { it > 0f }.minOrNull()
                    val finalMin: Float =
                        if (opticalMin != null && opticalMin < mn - 0.01f) opticalMin else mn
                    if (finalMin < mn) {
                        Log.w(
                            "Camera",
                            "HAL 上报下限 ${mn}x 但存在 ${opticalMin}x 光学档，已放宽到 ${finalMin}x"
                        )
                    }
                    minZoomRatio = finalMin
                    maxZoomRatio = mx

                    // ★检测"HAL 撒谎"★
                    // mn 是 HAL 原生上报的下限。它 >= 1.0 说明逻辑头根本不接受 <1.0 的倍率，
                    // 但本机光学档位里确实有 0.6x —— 那这个广角只能靠物理直连才拿得到。
                    val hasWideOptic: Boolean = opticalStops.any { it < 0.95f }
                    val blocked: Boolean = mn >= 0.99f && hasWideOptic
                    if (blocked != logicWideBlocked) {
                        logicWideBlocked = blocked
                        Log.w(
                            "Camera",
                            if (blocked) "⚠️ 逻辑头变焦下限=${mn}x（无广角），但存在 " +
                                    "${opticalStops.filter { it < 0.95f }}x 光学档" +
                                    " → <1.0 的目标全部改走物理直连"
                            else "逻辑头变焦下限=${mn}x，广角可直接用 setZoomRatio 到达"
                        )
                    }
                } else {
                    // 物理头：zoomState 报的是"这颗头内部"的倍率，不能当全局范围用
                    Log.d(
                        "Camera",
                        "物理头 ${boundLensId} 本地变焦范围 ${mn}x ~ ${mx}x（不覆盖全局范围）"
                    )
                }
            }
            Log.d("Camera", "全局变焦范围: ${minZoomRatio}x ~ ${maxZoomRatio}x")

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
                .filter {
                    val inZoomRange = it >= minZoomRatio - 0.01f && it <= maxZoomRatio + 0.01f
                    // ★真实存在的光学档位无条件保留★
                    // 小米 / iQOO 上 zoomState 常只报 min=1.0，按范围一过滤，
                    // 0.6x 超广角按钮直接消失 —— 这就是"只能调用主摄"的表现之一。
                    // 物理头是真实存在的，点它时走"物理头直连"，不该被残缺的 HAL 上报砍掉。
                    inZoomRange || opticalStops.any { o -> abs(o - it) < 0.01f }
                }
                .sorted()
                .ifEmpty { defaultStops }
            Log.d(
                "Camera",
                "按钮档位: ${zoomStops.joinToString("/")} " +
                        "(光学=${opticalStops.size}个, 硬件上限=${maxZoomRatio}x)"
            )

            // 物理头直连时不做标定：标定是靠"逻辑摄像头的 zoomState"反推 linearZoom，
            // 单颗物理头没有多摄切换，标定出来的表是错的，还会把倍率搞乱。
            if (!hasCalibrated && lensFacing == CameraSelector.LENS_FACING_BACK && boundLensId == null) {
                val cam = camera ?: return@LaunchedEffect
                // 标定单独用一个更短的等待，并且轮询 zoomState 提前结束：
                // 以前每档都死等 zoomSettleDelayMs（最多 700ms），6 个档位就是 4 秒多。
                // 现在多数档位 100~200ms 就到位了，整体能快一半以上。
                val settle = DeviceCompatibility.calibrationSettleMs
                val timeout = DeviceCompatibility.calibrationTimeoutMs
                // 标定点：★只标真实光学档 + 最大档★
                // 以前把 zoomStops 里每一档都标（含 1x/2x/10x 等数码档），
                // 6~7 个档位 × 每档数百毫秒，光标定就要 2 秒以上。
                // 数码档位的 linearZoom 本来就能线性插值算出来，没必要实测，
                // 所以只测真实光学档，再补一个最大档定住上界。
                val inRangeStops = zoomStops.filter {
                    it >= minZoomRatio - 0.01f && it <= maxZoomRatio + 0.01f
                }
                val opticalTargets = inRangeStops.filter { z ->
                    opticalStops.any { o -> abs(o - z) < 0.01f }
                }
                val targets = (opticalTargets + listOf(maxZoomRatio))
                    .distinct()
                    .sorted()
                    .ifEmpty { listOf(1.0f) }
                val result = mutableMapOf<Float, Float>()
                val maxZ = maxZoomRatio
                val minZ = minZoomRatio

                val t0 = System.currentTimeMillis()
                Log.d("Camera", "开始标定 ${targets.size} 个光学档位: ${targets.joinToString("/")}")

                for (t in targets) {
                    cam.cameraControl.setZoomRatio(t)
                    // 先给一个短的固定等待，再轮询确认，到位就立刻走下一档
                    delay(settle / 2)
                    var waited = settle / 2
                    var actualRatio = cam.cameraInfo.zoomState.value?.zoomRatio ?: t
                    var linear = cam.cameraInfo.zoomState.value?.linearZoom ?: 0f
                    while (abs(actualRatio - t) > t * 0.05f && waited < timeout) {
                        delay(40)
                        waited += 40
                        actualRatio = cam.cameraInfo.zoomState.value?.zoomRatio ?: actualRatio
                        linear = cam.cameraInfo.zoomState.value?.linearZoom ?: linear
                    }
                    Log.d("Camera", "标定 ${t}x → 实际 ${"%.2f".format(actualRatio)}x，用时 ${waited}ms")

                    if (t >= maxZ - 0.01f) {
                        result[t] = if (actualRatio >= maxZ * 0.95f) linear else 1.0f
                    } else {
                        result[t] = if (abs(actualRatio - t) < t * 0.2f) linear
                        else ((t - minZ) / (maxZ - minZ)).coerceIn(0f, 1f)
                    }
                }
                cam.cameraControl.setZoomRatio(1.0f)
                delay(settle / 2)
                linearMap = result
                hasCalibrated = true
                // 落盘缓存：下次打开直接用，跳过标定
                withContext(Dispatchers.IO) {
                    SettingsStore.saveLinearMap(context, result)
                }
                Log.d("Camera", "标定完成（耗时 ${System.currentTimeMillis() - t0}ms）: " +
                        result.entries.sortedBy { it.key }
                            .joinToString(", ") { "${it.key}x→${"%.3f".format(it.value)}" })
            }

            // ── 物理头直连完成后，把"全局倍率"换算成这颗头的本地倍率 ──
            // 例：0.6x 超广角绑上来后它自己就是 1.0x，要显示成 0.6x，
            //     下发 setZoomRatio(全局目标 ÷ 0.6) 即可，UI 上仍显示 0.6x。
            val lid = boundLensId
            if (lid != null) {
                val factor: Float =
                    lensProfiles.firstOrNull { it.id == lid }?.zoomFactor ?: 1f
                val target = pendingZoomTarget
                if (target > 0f) {
                    val local: Float = (target / factor).coerceAtLeast(1f)
                    camera?.cameraControl?.setZoomRatio(local)
                    displayedRatio = target
                    currentRatio = target
                    selectedZoom = nearestStop(target)
                    pendingZoomTarget = 0f
                    Log.d(
                        "Camera",
                        "物理头 $lid(${factor}x) 就位：本地 ${"%.2f".format(local)}x → 全局 ${target}x"
                    )
                }
            }

            // ── 从物理头切回逻辑摄像头时，倍率要接着走，不能跳 ──
            // 例：广角头(0.6x)上想切到 3x 长焦，会先解绑物理头、重新绑回逻辑摄像头。
            // 重新绑定后相机默认是 1.0x，如果不还原，画面就会"先跳回 1x 再变到 3x"，
            // 看起来就是"切换时比例突然变了一下"。这里绑定完成后立刻落回目标倍率。
            if (lid == null && pendingZoomTarget > 0f) {
                val target = pendingZoomTarget
                pendingZoomTarget = 0f
                val clamped = target.coerceIn(minZoomRatio, maxZoomRatio)
                camera?.cameraControl?.setZoomRatio(clamped)
                displayedRatio = clamped
                currentRatio = clamped
                selectedZoom = nearestStop(clamped)
                Log.d("Camera", "切回逻辑摄像头，倍率落回 ${"%.2f".format(clamped)}x")
            }

            // ── 恢复上次退出时的变焦倍率 ──
            // 放在标定之后，否则会被标定循环里的 setZoomRatio(1.0f) 覆盖掉
            if (restoredZoom > 0f) {
                val rz = restoredZoom
                restoredZoom = 0f
                val clamped = rz.coerceIn(minZoomRatio, maxZoomRatio)
                camera?.cameraControl?.setZoomRatio(clamped)
                displayedRatio = clamped
                currentRatio = clamped
                selectedZoom = nearestStop(clamped)
                Log.d("Camera", "已恢复上次倍率 ${"%.2f".format(clamped)}x")
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
                // vivo 适配：变焦上下限跟着实际会话刷新。
                // 这两个值基本不变，只有真的变了才写入，避免无谓重组
                // vivo 适配：变焦上下限跟着实际会话刷新。
                // 这两个值基本不变，只有真的变了才写入，避免无谓重组。
                // 物理头直连时不能刷新 —— 那只是这颗头内部的局部范围。
                if (boundLensId == null) {
                    val (mn, mx) = normalizeZoomRange(it.minZoomRatio, it.maxZoomRatio)
                    if (mn != minZoomRatio) minZoomRatio = mn
                    if (mx != maxZoomRatio) maxZoomRatio = mx
                }

                // ★变焦动画进行中就不要插手★
                // 动画那边正按 16ms 高频下发，轮询如果同时每 50ms 也来写
                // displayedRatio，两股重组叠加起来正好把中间帧全吃掉——
                // 这就是之前"看着像硬切"的直接原因。
                if (!zoomAnimating[0]) {
                    // 物理头直连时 zoomState 报的是"这颗头内部"的倍率，
                    // 乘上这颗头的光学倍率才是用户认知里的全局倍率
                    val global: Float = it.zoomRatio * boundLensFactor()
                    currentRatio = global
                    if (!isDragging && (zoomJob == null || zoomJob?.isActive == false)) {
                        if (global <= maxRatio * 1.05f) {
                            displayedRatio = global
                        }
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

    // 录像防抖：开关或模式变化时重新下发，不重新绑定相机
    LaunchedEffect(videoStabilization, mode, camera) {
        applyStabilization(videoStabilization)
    }

    // 拖动变焦没有明确的"结束"回调，这里靠超时检测：
    // 260ms 内没有新的手势事件，就认为手离开了屏幕，把挂起的防抖恢复回来
    LaunchedEffect(lastRulerInteraction) {
        if (!stabSuspended) return@LaunchedEffect
        delay(280)
        if (System.currentTimeMillis() - lastRulerInteraction >= 260) {
            stabSuspended = false
            if (mode == CaptureMode.PRO && manualControlSupported) {
                applyProParams(proIso, proShutterNs, proEv, proFocus, proWb)
            } else {
                applyStabilization(videoStabilization)
            }
            Log.d("Camera", "拖动结束，防抖已恢复")
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

    /**
     * 平滑变焦。
     * ★关键修复★ 以前用 setLinearZoom(ratioToLinear(...))，完全依赖标定表，
     * 荣耀 50 SE / iQOO 15 上标定不准就直接"变焦没反应"。
     * 现在改用 setZoomRatio()——它接受的是绝对倍率(1.0/2.0/5.0)，
     * CameraX 内部自己换算，不依赖我们的标定，稳得多。
     */
    fun smoothZoomTo(targetRatio: Float) {
        val cam = camera ?: return
        if (!scanDone) return
        // 只有在【实测坐实】逻辑头到不了广角时才换物理头。
        // ⚠️ 这里曾经无条件调用 requestLensSwitchIfNeeded，等于每次变焦都先尝试直连，
        //    结果把本来 setZoomRatio 能正常工作的机型搞坏了。现在严格限定条件。
        if (logicWideBlocked && targetRatio < 0.99f && boundLensId == null) {
            if (requestLensSwitchIfNeeded(targetRatio)) return
        }
        val endRatio = targetRatio.coerceIn(effMinZoom(), effMaxZoom())
        val startRatio = displayedRatio

        zoomJob?.cancel()
        zoomJob = coroutineScope.launch {
            if (abs(startRatio - endRatio) < 0.005f) {
                cam.cameraControl.setZoomRatio(localZoomOf(endRatio))
                displayedRatio = endRatio
                return@launch
            }

            // ★防抖与变焦会互相打架★
            // EIS 每帧做运动估计、OIS 驱动马达位移，而变焦会不断改变 crop region，
            // 两者同时进行时 HAL 每帧都要重算。所以变焦期间先挂起防抖，结束后再恢复。
            // 挂起之后 EIS 就不再参与，变焦的代价回到普通水平，可以放心用高频下发。
            val stabWasOn = wantStabilization()
            if (stabWasOn) setStabilizationRaw(false)

            // 通知下面的轮询循环：变焦动画进行中，别来抢着更新 UI 状态
            zoomAnimating[0] = true

            try {
                val span = abs(endRatio - startRatio) / maxRatio.coerceAtLeast(1f)
                val durationMs = (ZOOM_ANIM_BASE_MS - span * ZOOM_ANIM_SPAN_FACTOR)
                    .coerceIn(ZOOM_ANIM_MIN_MS, ZOOM_ANIM_BASE_MS).toLong()
                val stepMs = if (isRecording) ZOOM_ANIM_STEP_MS_REC else ZOOM_ANIM_STEP_MS
                val steps = (durationMs / stepMs).toInt().coerceAtLeast(1)
                val rangeRatio = endRatio - startRatio
                // 苹果风格曲线：起步快、尾巴柔，跟 iOS 切焦段的手感对齐
                val easing = APPLE_EASE

                var uiCommits = 0
                var lastUiAt = 0L

                Log.d(
                    "Camera",
                    "变焦 ${"%.2f".format(startRatio)}→${"%.2f".format(endRatio)}" +
                            " 下发${steps}步/${stepMs}ms UI刷新${ZOOM_UI_REFRESH_MS}ms"
                )

                for (step in 1..steps) {
                    val t = step.toFloat() / steps
                    val eased = easing.transform(t)
                    val r = startRatio + rangeRatio * eased

                    // ① 每帧都给相机下发——这个是顺滑感的来源，很便宜
                    //    物理头直连时要把全局倍率换算成这颗头的本地倍率
                    cam.cameraControl.setZoomRatio(localZoomOf(r))

                    // ② UI 状态只在攒够间隔时才提交——重组很贵，必须节流
                    val now = System.currentTimeMillis()
                    if (step == steps || now - lastUiAt >= ZOOM_UI_REFRESH_MS) {
                        displayedRatio = r
                        lastUiAt = now
                        uiCommits++
                    }
                    delay(stepMs)
                }
                cam.cameraControl.setZoomRatio(localZoomOf(endRatio))
                displayedRatio = endRatio
                Log.d("Camera", "变焦完成，UI 实际重组 $uiCommits 次（共 $steps 步）")

                // ★下发后校验★ 不依赖任何预判，直接看相机实际到没到位。
                // HAL 把倍率 clamp 回 1.0 时不会报错、不抛异常，只有读回来才知道。
                // 这一步是所有广角兜底里最可靠的：只要没到位，就改用物理直连再试。
                if (boundLensId == null && physicalSwitchWorks && !isFront) {
                    delay(DeviceCompatibility.zoomSettleDelayMs)
                    val actual: Float = cam.cameraInfo.zoomState.value?.zoomRatio ?: endRatio
                    val off: Boolean = abs(actual - endRatio) > endRatio * 0.12f
                    Log.d(
                        "Camera",
                        "变焦校验: 目标 ${"%.2f".format(endRatio)}x → 实际 " +
                                "${"%.2f".format(actual)}x" + if (off) " ❌ 未到位" else " ✅"
                    )
                    if (off) {
                        if (endRatio < 0.99f && !logicWideBlocked) {
                            Log.w("Camera", "逻辑头到不了 ${endRatio}x，标记为需物理直连")
                            logicWideBlocked = true
                        }
                        // 换物理头重来一次
                        requestLensSwitchIfNeeded(endRatio)
                    }
                }
            } finally {
                // 无论中途是否异常，都要撤掉"动画中"标志，
                // 否则轮询循环会永远不再更新倍率显示
                zoomAnimating[0] = false

                // 恢复防抖。专业模式下要走 applyProParams，
                // 否则 setStabilizationRaw 会把手动参数清掉（它是全量替换）
                if (stabWasOn) {
                    // 先让 crop 落定再恢复 EIS，否则 EIS 一恢复就要
                    // 重新收敛稳定窗口，画面会在变焦结束时又顿一下
                    delay(STAB_RESTORE_DELAY_MS)
                    if (mode == CaptureMode.PRO && manualControlSupported) {
                        applyProParams(proIso, proShutterNs, proEv, proFocus, proWb)
                    } else {
                        setStabilizationRaw(true)
                    }
                }
            }
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

        // 物理头直连时，把全局倍率换算成这颗头的本地倍率再下发
        val clamped = finalRatio.coerceIn(effMinZoom(), effMaxZoom())
        displayedRatio = clamped
        cam.cameraControl.setZoomRatio(localZoomOf(clamped))
        selectedZoom = nearestStop(clamped)
    }

    fun onZoomButtonClick(zoom: Float) {
        // 2x 再点一下切 3.5x：这是原厂 App 常见的"人像/长焦中间档"手感，
        // 但如果本机根本没有这个焦段的物理头，就直接吸附回最近的光学档
        val target: Float = if (zoom == 2.0f && abs(selectedZoom - 2.0f) < 0.05f) {
            snapToOptical(3.5f)
        } else zoom
        selectedZoom = target
        // 直接走 setZoomRatio，跟原本能正常工作的行为一致。
        // 物理直连只在 smoothZoomTo 内部的"下发后校验"发现真的到不了时才启用。
        smoothZoomTo(target)
    }

    /** 拖动标尺松手后调用：吸到最近的光学镜头上，避免停在数码变焦的模糊区 */
    fun commitZoom(ratio: Float) {
        val cam = camera ?: return
        if (!scanDone) return
        val snapped = snapToOptical(ratio)
        if (abs(snapped - ratio) > 0.001f) {
            smoothZoomTo(snapped)
        } else {
            val clamped = snapped.coerceIn(effMinZoom(), effMaxZoom())
            cam.cameraControl.setZoomRatio(localZoomOf(clamped))
            displayedRatio = clamped
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
        val vc = videoCapture
        if (vc == null) {
            // 5000万模式下不创建录像用例，这里兜底退出高像素，
            // 重新绑定后用户再点一次就能录了
            if (highResMode) {
                highResMode = false
                Log.w("Camera", "录像需要退出 5000万模式，已自动切换")
            }
            return
        }
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

    // ★预览框比例 = 当前拍照比例（宽/高），且横屏时自动转成横构图★
    // 以前预览永远 fillMaxSize 铺满屏幕，结果选 3:4 拍出来的却跟取景看到的对不上。
    // 这里让取景框本身变成所选比例，框外露黑边（根布局已是黑色），跟原厂相机一致。
    //
    // 横屏适配：3:4 的照片在竖屏下是竖着的窄条；横屏时它应该转成 4:3 横构图，
    // 这样取景框才铺得开、也跟"横屏看这张照片"的样子一致。16:9 本来就是横向，不用转。
    val previewRatio = remember(photoRatio, configuration) {
        val dm = context.resources.displayMetrics
        val w = dm.widthPixels.coerceAtLeast(1).toFloat()
        val h = dm.heightPixels.coerceAtLeast(1).toFloat()
        when (photoRatio) {
            PhotoRatio.RATIO_3_4 -> if (isLandscape) 4f / 3f else 3f / 4f
            PhotoRatio.RATIO_16_9 -> 16f / 9f
            // "全屏" = 当前屏幕方向下的真实宽高比
            PhotoRatio.FULL -> w / h
        }
    }

    // ★横屏挖孔避让★
    // 挖孔屏的前置摄像头在横屏时会落到屏幕某一条长边上（常见于左侧），
    // 正好压住放在左边的模式切换栏。这里读系统真实的 displayCutout 内缩值，
    // 给左侧留出安全距离。拿不到就退化为 0，不额外加多余留白。
    val view = LocalView.current
    val cutoutLeftDp: Dp = remember(view, configuration) {
        val density = view.resources.displayMetrics.density.coerceAtLeast(1f)
        val leftPx = try {
            ViewCompat.getRootWindowInsets(view)
                ?.getInsets(WindowInsetsCompat.Type.displayCutout())?.left ?: 0
        } catch (e: Exception) {
            0
        }
        (leftPx.toFloat() / density).dp
    }

    // 高像素按钮文案：显示本机真实能拍到的最大像素（50M / 108M / 200M…），
    // 不再是写死的 "50M"。还没绑定探测过时退化为 "MAX"。
    // ⚠️ 必须声明在 settingsBarState 之前：Kotlin 局部 val 只能引用【前面】已声明的。
    val highResLabel = remember(maxPhotoPixels) {
        if (maxPhotoPixels > 0) "${(maxPhotoPixels / 1_000_000f).roundToInt()}M" else "MAX"
    }

    // 参数栏（像素 / 画质 / 帧率 / 防抖 / 比例 / 旋转 / 闪光 / 关于）的状态与回调。
    // 打包成一个对象，竖屏横排、横屏竖排两种布局共用同一份，避免两处逻辑不同步。
    val settingsBarState = SettingsBarState(
        isFront = isFront,
        mode = mode,
        highResMode = highResMode,
        highResLabel = highResLabel,
        videoQuality = videoQuality,
        videoFrameRate = videoFrameRate,
        videoStabilization = videoStabilization,
        photoRatio = photoRatio,
        flashMode = flashMode,
        autoRotate = autoRotate
    )
    val settingsBarActions = SettingsBarActions(
        onHaptic = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
        onHighResChange = { next ->
            highResMode = next
            // 超广角镜头一般没有高像素模式，切到主摄再开
            if (next && isUltraWide) {
                selectedZoom = 1.0f
                smoothZoomTo(1.0f)
            }
        },
        onVideoQualityChange = { videoQuality = it },
        onVideoFrameRateChange = { videoFrameRate = it },
        onStabilizationChange = { videoStabilization = it },
        onPhotoRatioChange = { photoRatio = it },
        onAutoRotateChange = { autoRotate = it },
        onFlashExpand = { flashExpanded = true },
        onAbout = { showAbout = true }
    )

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
                },
            // 取景框按拍摄比例【居中】，框外露黑边。
            // 横屏时的左移由 AndroidView 上的 offset 完成（用于平衡右侧三列控件），
            // 这里的对齐必须保持 Center —— 改成 CenterStart 会让画面贴死左边缘。
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier
                    .aspectRatio(previewRatio)
                    // 横屏：在居中的基础上向左微调，平衡右侧"变焦+拍摄+模式栏"三列
                    // 占掉的空间。只是微调，画面整体仍然居中。
                    .offset(x = if (isLandscape) (-40).dp else 0.dp)
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

                            // 拖动变焦期间同样挂起防抖，否则 EIS/OIS 与 crop 变化打架会卡。
                            // 这里没有明确的"结束"回调，靠后面的超时检测恢复。
                            if (wantStabilization() && !stabSuspended) {
                                setStabilizationRaw(false)
                                stabSuspended = true
                            }

                            // ★节流★
                            // detectTransformGestures 是按指针事件回调的，一秒能来 100+ 次。
                            // 不节流的话，防抖开着时等于每秒重建上百次稳定窗口——必卡。
                            // 挂起/录制时把间隔拉得更开，进一步减少 crop 变化次数。
                            val now = System.currentTimeMillis()
                            val throttle = when {
                                isRecording -> DRAG_ZOOM_THROTTLE_REC_MS
                                stabSuspended -> DRAG_ZOOM_THROTTLE_STAB_MS
                                else -> DRAG_ZOOM_THROTTLE_MS
                            }
                            if (now - lastZoomApplyAt < throttle) {
                                // 这次不下发，但把这次的缩放量累积起来，下次一起用，
                                // 这样慢拖也不会丢操作（手感不会变"迟钝"）
                                pendingZoomFactor *= zoomChange
                                lastRulerInteraction = now
                                return@detectTransformGestures
                            }
                            val factor = pendingZoomFactor * zoomChange
                            pendingZoomFactor = 1f
                            lastZoomApplyAt = now

                            // 用 zoomRatio 而不是 linearZoom：后者依赖标定表，
                            // 荣耀 50 SE 等机型标定不准会导致拖动无反应
                            // 物理头直连时 zoomState 报的是本地倍率，乘光学倍率还原成全局倍率
                            val curGlobal: Float =
                                (cam.cameraInfo.zoomState.value?.zoomRatio
                                    ?: localZoomOf(displayedRatio)) * boundLensFactor()
                            val newRatio =
                                (curGlobal * factor).coerceIn(effMinZoom(), effMaxZoom())
                            cam.cameraControl.setZoomRatio(localZoomOf(newRatio))
                            displayedRatio = newRatio
                            selectedZoom = nearestStop(newRatio)
                            lastRulerInteraction = now
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

        // ── 参数栏（像素 / 画质 / 帧率 / 防抖 / 比例 / 旋转 / 闪光 / 关于）──
        // 竖屏：横排放在顶部。横屏：改到屏幕左边竖排（见下方横屏分支），
        // 因为横屏时顶部那条很窄，参数挤在一排会压住取景框。
        if (!isLandscape) {
            SettingsChipsRow(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp),
                st = settingsBarState,
                act = settingsBarActions
            )
        }
        // ── 闪光灯展开：全屏透明层，点任意地方关闭 ──
        // 放在顶部栏之后绘制，所以它盖在下面所有控件之上；
        // 选择面板再画在它之上，这样点面板上的图标不会被关掉。
        if (flashExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable(
                        // 去掉点击水波纹，纯透明关闭层不该有反馈
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        flashExpanded = false
                    }
            )
        }

        // ── 闪光灯选择面板：带弹出动画，叠在关闭层之上 ──
        AnimatedVisibility(
            visible = flashExpanded,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 56.dp),
            enter = fadeIn(tween(120)) + scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                initialScale = 0.75f
            ),
            exit = fadeOut(tween(100)) + scaleOut(targetScale = 0.85f)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (m in 0..3) {
                    // 图标逐个错开一点点出场，比整排一起弹出更有质感
                    val appear = remember { Animatable(0f) }
                    LaunchedEffect(flashExpanded) {
                        if (flashExpanded) {
                            appear.snapTo(0f)
                            appear.animateTo(
                                1f,
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer {
                                scaleX = appear.value
                                scaleY = appear.value
                                alpha = appear.value
                            }
                            .clip(CircleShape)
                            .background(
                                if (flashMode == m) Color.White.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (flashMode == m) Color(0xFFFFC107)
                                else Color.Transparent,
                                CircleShape
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                flashMode = m
                                flashExpanded = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        FlashIcon(mode = m, selected = flashMode == m)
                    }
                }
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

        // ── 横屏：模式栏挪到左边、变焦挪到右边，中间整块留给取景框 ──
        // 竖屏时这两块都堆在底部会糊在画面正中；横屏屏幕更矮，必须分开到两侧。
        // ════════ 横屏布局 ════════
        // 横屏屏幕"矮而宽"，顶部那一条很窄、中间要完整留给取景框，所以三块分开：
        //   左边 = 参数栏（像素/画质/帧率/防抖/比例/旋转/闪光/关于），竖排
        //   右边 = 模式切换 + 变焦（倍率+档位）+ 拍摄键，统一放进【同一个 Column】竖排
        //   中间 = 取景框
        // ★右边合成一个 Column 是关键★：以前模式栏、变焦栏、快门各自 align，
        //   变焦档位一多就会盖住快门；放进同一个 Column 后按顺序排，永远不会重叠。
        // ════════ 横屏布局（参照参考图的结构）════════
        // 参考图（竖屏）的三块 → 横屏的对应位置：
        //   参考图左下角的变焦档位(0.6/1x/2/5/10)  → 横屏【左侧】竖排
        //   参考图顶部那排图标(闪光/滤镜/AI/设置)   → 横屏【右侧】竖排
        //   参考图底部 相册|快门|切换 + 下方模式栏   → 横屏【底部】保持同样结构
        // 中间整块留给取景框。
        // ════════ 横屏布局 ════════
        //   左侧 = 参数栏（像素/画质/帧率/防抖/比例/旋转/闪光/关于），竖排
        //   右侧 = 变焦（当前倍率 + 档位），竖排
        //   底部 = 相册 | 快门 | 切换，其下方是模式栏
        //   中间 = 取景框
        // 挖孔避让跟着【左侧那栏】走（挖孔通常在某一条长边上，左侧栏要往里推）。
        if (isLandscape) {
            // ── 左侧：参数栏，竖排 ──
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .navigationBarsPadding()
                    // 挖孔在左边时往里推，避免被前置摄像头挡住点不到
                    // 基础留白 10.dp：比原来 12.dp 略收，但仍保证不至于贴在屏幕边缘
                    .padding(start = 10.dp + cutoutLeftDp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SettingsChipsColumn(st = settingsBarState, act = settingsBarActions)
            }

        }

        // 竖屏：整块控制区在底部居中；横屏：挪到屏幕【右侧】垂直居中
        // （参考图向左转 90° 后，原本在底部的东西都落到右边）
        Column(
            modifier = if (isLandscape) {
                Modifier
                    .align(Alignment.CenterEnd)
                    .navigationBarsPadding()
                    .padding(end = 14.dp)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!isFront && !isLandscape) {
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
                        // 这台机器到不了的档位（比如 HAL 没上报 0.6x 超广角）直接置灰。
                        // ★真实存在的光学档位例外★：小米 / iQOO 上 zoomState 常只报 min=1.0，
                        // 按范围过滤会把 0.6x 超广角按钮弄没 —— 点它时走"物理头直连"兜底。
                        val isOptical = opticalStops.any { o -> abs(o - zoom) < 0.01f }
                        val allowed = isOptical ||
                                (zoom >= minRatio - 0.01f && zoom <= maxRatio + 0.01f &&
                                        // 高画质模式只允许真实光学档
                                        (!highResMode || isOpticalStop(zoom)))

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
                if (!isLandscape) {
                    Spacer(modifier = Modifier.height(14.dp))
                }
            } else if (!isLandscape) {
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


            // 拍摄控制组（相册 / 快门 / 切换）。
            // 横屏时竖排：参考图向左转 90° 后，底部横排的这三个键变成竖排。
            val captureControls: @Composable () -> Unit = {
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

                // ★拍摄键常驻底部中央（跟参考图一致）★
                // 横屏也放在这里：参考图的快门就是底部居中，
                // 左侧变焦、右侧参数，中间底部留给快门和模式栏。
                ShutterButton(
                    isVideoMode = mode == CaptureMode.VIDEO,
                    isRecording = isRecording,
                    landscape = isLandscape,
                    onVideoClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isRecording) stopRecording() else startRecording()
                    },
                    onPhotoClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        takePhoto()
                    }
                )

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
                                // 切换前后摄时物理头直连状态作废，回到"逻辑摄像头 + 数码变焦"
                                boundLensId = null
                                pendingZoomTarget = 0f
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

            if (isLandscape) {
                // 横屏：快门组靠内、模式栏靠外，都竖排，整体放到屏幕【右侧】
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ① 变焦档位：竖排在拍摄键的【左边】
                    //    横屏时从左到右依次是 变焦 → 拍摄 → 模式栏。
                    if (!isFront) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 当前倍率 + 正在用哪颗镜头
                            // ★横屏竖着显示★：倍率一行、镜头标签一行。
                            // 横着排成 "1.0x · 23mm 主摄" 会把这一列撑得很宽，
                            // 挤掉取景框的空间，所以拆成上下两行。
                            val opticalLens = opticalLensAt(displayedRatio)
                            val activeLens = activeLensAt(displayedRatio)
                            val lensLabel: String = if (zoomStopsFromHardware) {
                                when {
                                    opticalLens != null -> opticalLens.shortLabel
                                    activeLens != null -> "${activeLens.shortLabel} 数码"
                                    else -> ""
                                }
                            } else ""
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = formatZoom(displayedRatio),
                                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                                if (lensLabel.isNotEmpty()) {
                                    Text(
                                        text = lensLabel,
                                        color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // ★档位竖排，自下而上：广角 → 长焦★
                            // Column 是"先声明的排在上面"，所以要【降序】：
                            // 最上面是长焦(10x)，最下面是 0.6x 广角。
                            //
                            // ★强行补一个 10x★：很多机器的 zoomStops 里没有 10x
                            // （原厂档位只到 5x），但数码变焦上限通常到 10x 甚至更高，
                            // 横屏竖向空间够放 6 个，就把 10x 补上。
                            val landscapeStops = remember(zoomStops, opticalStops, maxRatio) {
                                val optical = zoomStops.filter { z ->
                                    opticalStops.any { o -> abs(o - z) < 0.01f }
                                }
                                val extras = listOf(1.0f, 2.0f).filter { it in zoomStops }
                                // 10x 只要没超出这台机器的变焦上限就加上
                                val ten = listOf(10.0f).filter { it <= maxRatio + 0.01f }
                                (optical + extras + ten)
                                    .distinct()
                                    .sortedDescending()
                                    .take(6)
                                    .ifEmpty { zoomStops.sortedDescending().take(6) }
                            }
                            Column(
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                landscapeStops.forEach { zoom ->
                                    val isSelected = when {
                                        zoom == 2.0f && abs(selectedZoom - 3.5f) < 0.05f -> true
                                        abs(selectedZoom - zoom) < 0.05f -> true
                                        else -> false
                                    }
                                    // 光学档位无条件可用（哪怕 HAL 上报的变焦范围不含它，
                                    // 点它会走"物理头直连"），其它档位仍按变焦范围判断
                                    val isOptical = opticalStops.any { o -> abs(o - zoom) < 0.01f }
                                    val allowed = isOptical ||
                                            (zoom >= minRatio - 0.01f && zoom <= maxRatio + 0.01f &&
                                                    (!highResMode || isOpticalStop(zoom)))

                                    ZoomButton(
                                        label = formatStop(zoom),
                                        isSelected = isSelected,
                                        enabled = scanDone && allowed,
                                        size = 34.dp,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onZoomButtonClick(zoom)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        captureControls()
                    }
                    ModeSelector(
                        current = mode,
                        vertical = true,
                        onSelect = { next ->
                            mode = next
                            // 高像素模式下没有录像用例（三流共存会把拍照分辨率压到 1200万），
                            // 切进录像时要先退出高像素，否则点录制会没反应
                            if (next == CaptureMode.VIDEO && highResMode) {
                                highResMode = false
                            }
                        }
                    )
                }
            } else {
                // 竖屏：模式栏在上、快门组在下，保持原来的结构
                ModeSelector(
                    current = mode,
                    onSelect = { next ->
                        mode = next
                        if (next == CaptureMode.VIDEO && highResMode) {
                            highResMode = false
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    captureControls()
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

                    // ── 免责声明：放在关于卡片最后一行 ──
                    Spacer(modifier = Modifier.height(14.dp))
                    // 用 Box 画分隔线，不依赖 material3 的 Divider（版本差异可能没有）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.10f))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "此软件造成的任何问题不由作者承担。",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
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

/**
 * 标定表的持久化转换器。
 * 把 Map<Float, Float> 编成 "0.6=0.123;1.0=0.5;3.0=1.0" 这样的字符串存进 Bundle，
 * 这样横竖屏切换（Activity 被销毁重建）后能直接恢复，不必重新标定一次。
 */
private val linearMapSaver: Saver<Map<Float, Float>, String> = Saver(
    save = { m -> m.entries.joinToString(";") { "${it.key}=${it.value}" } },
    restore = { s ->
        if (s.isBlank()) {
            emptyMap()
        } else {
            s.split(";").mapNotNull { part ->
                val kv = part.split("=")
                if (kv.size != 2) return@mapNotNull null
                val k = kv[0].toFloatOrNull() ?: return@mapNotNull null
                val v = kv[1].toFloatOrNull() ?: return@mapNotNull null
                k to v
            }.toMap()
        }
    }
)

/**
 * 中央拍摄键（拍照时是白圈，录像时是白圈包红点、录制中变红色方角）。
 *
 * ★竖屏 + 录像模式做过瘦身★
 * 原来外圈 76dp、内圈红点 60dp，在竖屏录像模式下几乎顶满一排，显得特别笨重。
 * 现在录像模式收到外圈 64dp、红点 44dp，跟旁边 52dp 的相册/翻转键比例协调。
 * 拍照模式保持 76dp 不变（那是用户熟悉的手感，动它反而不好按）。
 *
 * @param landscape 横屏时整体再收一点，避免竖排栏太宽挤到取景框
 */
@Composable
private fun ShutterButton(
    isVideoMode: Boolean,
    isRecording: Boolean,
    onVideoClick: () -> Unit,
    onPhotoClick: () -> Unit,
    landscape: Boolean = false
) {
    // 录像模式（且未在录制）用缩小版；拍照模式 / 录制中保持原尺寸
    val shrink: Boolean = isVideoMode && !isRecording
    val outer: Dp = when {
        shrink && landscape -> 60.dp
        shrink -> 64.dp
        landscape -> 68.dp
        else -> 76.dp
    }
    // 录像待机时的红色圆点
    val innerDot: Dp = if (landscape) 40.dp else 44.dp
    val borderWidth: Dp = if (shrink) 3.dp else 4.dp

    Box(
        modifier = Modifier
            .size(outer)
            .clip(CircleShape)
            .background(Color.White)
            .border(
                width = borderWidth,
                color = if (isRecording) Color.Red else Color.White.copy(alpha = 0.6f),
                shape = CircleShape
            )
            .clickable { if (isVideoMode) onVideoClick() else onPhotoClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            // 录制中：红色圆角方块（停止按钮）
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Red)
            )
        } else if (isVideoMode) {
            Box(
                modifier = Modifier
                    .size(innerDot)
                    .clip(CircleShape)
                    .background(Color.Red)
            )
        }
    }
}

/**
 * 单个模式项（拍照 / 专业 / 录像）。
 * ⚠️ 刻意做成顶层 @Composable，而不是 ModeSelector 内部的局部 @Composable 函数：
 *    Compose 编译器对"函数内嵌 @Composable 局部函数"的支持不稳定，容易报编译错误。
 */
@Composable
private fun ModeItem(
    m: CaptureMode,
    selected: Boolean,
    vertical: Boolean,
    onSelect: (CaptureMode) -> Unit
) {
    val haptic = LocalHapticFeedback.current

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
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .background(
                color = if (vertical) Color.Black.copy(alpha = 0.35f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = if (vertical) 8.dp else 0.dp)
        ) {
            Text(
                text = m.label,
                color = textColor,
                fontSize = if (vertical) 13.sp else 14.sp,
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

/**
 * 模式切换栏（拍照 / 专业 / 录像）。
 * @param vertical 横屏时传 true：竖排到屏幕左边，不再横着堆在底部挡取景
 */
@Composable
fun ModeSelector(
    current: CaptureMode,
    onSelect: (CaptureMode) -> Unit,
    vertical: Boolean = false
) {
    if (vertical) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CaptureMode.values().forEach { m ->
                ModeItem(m = m, selected = m == current, vertical = true, onSelect = onSelect)
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CaptureMode.values().forEach { m ->
                ModeItem(m = m, selected = m == current, vertical = false, onSelect = onSelect)
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
    onClick: () -> Unit,
    // 横屏竖向空间紧张，档位竖排时用小一号的按钮
    size: Dp = 44.dp
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
            .size(size)
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
