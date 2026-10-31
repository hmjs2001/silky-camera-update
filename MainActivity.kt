package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.view.OrientationEventListener
import android.view.WindowManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.activity.compose.BackHandler
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
import androidx.camera.core.CameraInfo
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
// ★rememberInfiniteTransition / infiniteRepeatable 已不再使用★
//   时间码红点的呼吸渐变改成了按秒数取两档（见 RecordingTimecode），
//   因为无限动画会每帧触发重组，是 4K60 录制卡顿的主因。
//   保留 import 无害（只是未使用警告），将来若要用持续动画可直接启用。
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
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

import androidx.camera.core.ImageProxy
import androidx.compose.material3.Slider
import kotlinx.coroutines.isActive

// ==================== 设备兼容层 ====================

object DeviceCompatibility {
    private val manufacturer = Build.MANUFACTURER.lowercase()
    private val brand = Build.BRAND.lowercase()
    private val model = Build.MODEL.lowercase()
    private val device = Build.DEVICE.lowercase()

    /** 系统 API 级别。用字面量 36，避免依赖 Build.VERSION_CODES.BAKLAVA（需要 compileSdk 36 才有） */
    val apiLevel: Int get() = Build.VERSION.SDK_INT
    /** Android 16 = API 36。OriginOS 6 / HyperOS 3 / ColorOS 16 / MagicOS 10 都基于它 */
    val isAndroid16Plus: Boolean get() = apiLevel >= 36

    // ---------------- 用户机型档案（首启选择的品牌 / 系列）----------------
    /** 当前生效的机型档案（null = 用户还没选 / 选了「其他」，走通用兼容） */
    val userProfile: SeriesProfile? get() = ActiveDevice.profile

    /**
     * ★快速切镜头★
     * 打开后：① 复用镜头扫描结果（换头不重新遍历 CameraCharacteristics）
     *         ② 缩短换头后的收敛等待
     *         ③ 不再为了切物理头而把 PreviewView 在 SurfaceView / TextureView 之间来回切
     * 想要最稳（宁可慢）可以在「机型与色彩」里关掉。
     */
    val fastLensSwitch: Boolean get() = ActiveDevice.fastLensSwitch

    /** 换头时是否直接复用缓存的镜头表 */
    val reuseLensScan: Boolean get() = fastLensSwitch

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

    // ---------------- 本次新增：OPPO Find / 华为 P 系列 / 红米 Note ----------------

    /**
     * OPPO 内部代号格式判定：OPPO 的 DEVICE 字段是 "OP" + 4 位十六进制
     * （Find X6 Pro = OP5A3D、Find X7 Ultra = OP5B3B、Find X8 Pro = OP5C7B …）。
     * 只在已判定为 OPPO 系（isOppo）时才用，避免和其它品牌的 "opxx" 撞车。
     */
    private fun isOppoDeviceCode(dev: String): Boolean {
        if (dev.length != 6) return false
        if (!dev.startsWith("op", ignoreCase = true)) return false
        val tail = dev.substring(2)
        return tail.all { it.isDigit() || (it in 'a'..'f') || (it in 'A'..'F') }
    }

    /**
     * OPPO Find 系列（Find X3 / X5 / X6 / X7 / X8 / Find N …）。
     * MODEL 常见 "PHK110"、"CPH2557"、"Find X6"，DEVICE 常见 "OP5A3D" 之类。
     *
     * ⚠️ 这批机器的逻辑摄像头对第三方 App 只肯给主摄，潜望长焦（3x / 6x）必须物理直连，
     *    否则点 3x 只是主摄裁切、画质很糊 —— 这就是"Find 调不了长焦"的来源。
     */
    val isOppoFindSeries: Boolean
        get() = isOppo && (
                // ① 机型名直接带 Find（"Find X6"、"Find X6 Pro"、"Find N3"…）
                model.contains("find x") || model.contains("findx") ||
                        model.contains("find n") || model.contains("findn") ||
                        // ② OPPO 的机型号段：PHK / PHB / PHT / PHA …（Find X5~X9 各代都有）
                        model.startsWith("phk") || model.startsWith("phb") ||
                        model.startsWith("pht") || model.startsWith("pha") ||
                        model.startsWith("phj") || model.startsWith("phz") ||
                        // ③ 内部代号：OPPO 的 DEVICE 是 "OP" + 4 位十六进制（如 OP5A3D = Find X6 Pro）
                        //    只认 isOppo 品牌下这个格式，不会误伤其它品牌
                        isOppoDeviceCode(device) ||
                        device.contains("find")
                )

    /**
     * 努比亚 Z 系列（Z50/Z60/Z70/Z80…，含 Z50S Pro / Z60 Ultra）。
     * 两个特殊性正是"广角和长焦都调不出来"的根因：
     * ① 主摄是 35mm（别家 24~26mm），1x 本身就窄；广角相对主摄约 0.5x
     *    （别家约 0.6x），拿别家的 0.6 判定会整档错位。
     * ② MyOS 对第三方 App 的逻辑摄像头限制很严：setZoomRatio(0.5) 静默 clamp 回 1.0，
     *    长焦也只给主摄裁切 —— 必须物理直连才拿得到。
     * MODEL 常见 NX721J / NX729J / NX769J / "Z60 Pro"；DEVICE 多 NX 开头。
     */
    val isNubiaZSeries: Boolean
        get() = isZte && (
                model.startsWith("nx") || device.startsWith("nx") ||
                        model.contains("z50") || model.contains("z60") ||
                        model.contains("z70") || model.contains("z80") ||
                        model.contains("z40") || model.contains("z30") ||
                        device.contains("nubia")
                )

    /** 红魔（游戏机，摄像只求够用：主摄 + 超广角，没有长焦） */
    val isNubiaRedMagic: Boolean
        get() = brand.contains("redmagic") || model.contains("redmagic") ||
                model.contains("nx7") && model.contains("magic") ||
                device.contains("redmagic")

    /**
     * ★小米数字系列★（小米 10 / 11 / 12 / 13 / 14 / 15，含 Pro / Ultra / 至尊版）
     *
     * MODEL 常见："Mi 10"、"M2102K1C"、"23127PN0CG"、"Xiaomi 14"、"MI 9" …
     * DEVICE（内部代号）：umi(10) / cmi(10Pro) / cas(10至尊) / thyme(10S) /
     *                    venus(11) / star(11Pro) / zeus(12) / cupid(12S) /
     *                    ishtar(13Ultra) / houji(14) / shennong(14Ultra) /
     *                    dada(15) / aurora(15Ultra)…
     */
    val isXiaomiDigitalSeries: Boolean
        // ⚠️ 排除 Redmi / POCO：它们 BRAND 就是 "Redmi"/"POCO"，
        //    而 MODEL 常见 "Redmi Note 12"、"Redmi K70" —— 里面含 "mi 12"、"mi K" 之类，
        //    不排除会被误判成小米数字系列，进而误开长焦物理直连。
        //    而 Redmi 的多数机型根本没有独立长焦头，直连只会失败。
        get() = isXiaomi && !brand.contains("redmi") && !brand.contains("poco") && (
                Regex("mi\\s?\\d{1,2}").containsMatchIn(model) ||
                        Regex("xiaomi\\s?\\d{1,2}").containsMatchIn(model) ||
                        model.contains("mi 9") || model.contains("mi9") ||
                        device.startsWith("umi") || device.startsWith("cmi") ||
                        device.startsWith("cas") || device.startsWith("thyme") ||
                        device.startsWith("venus") || device.startsWith("star") ||
                        device.startsWith("zeus") || device.startsWith("cupid") ||
                        device.startsWith("ishtar") || device.startsWith("houji") ||
                        device.startsWith("shennong") || device.startsWith("dada") ||
                        device.startsWith("aurora") || device.startsWith("zizhan") ||
                        device.startsWith("nuwa") || device.startsWith("fuxi") ||
                        device.startsWith("l18") || device.startsWith("marble") ||
                        device.startsWith("cannon") || device.startsWith("psyche")
                )

    /**
     * 小米数字系列"长焦调不出来"：点 3x/5x 画面放大了，但画质是主摄裁切的糊图。
     * 根因：MIUI/HyperOS 的逻辑摄像头对第三方 App 只走数码变焦 ——
     *   setZoomRatio(3.0) 不报错、zoomState 也回报 3.0，所以"回读校验"这一招在
     *   小米上完全失效（倍率到位了镜头却没换），这也是它比 OPPO/华为更难查的原因。
     * 解法：一律长焦物理直连。这批机器长焦头都在 cameraIdList 里，成功率很高；
     *   万一绑不上，teleBindFailCount 累计 2 次自动退回逻辑头，不会搞死。
     */
    val isXiaomiTeleStubborn: Boolean
        get() = isXiaomiDigitalSeries

    /**
     * 一加（与 OPPO 同宗 HAL）。一加 11 / 12 / 13 的长焦在第三方 App 里同样常年切不动。
     */
    val isOnePlusTeleStubborn: Boolean
        get() = isOnePlus && (model.contains("oneplus") || model.contains("op") || model.contains("kb"))

    /**
     * 华为 P40 / P40 Pro / P40 Pro+。
     * DEVICE：P40=ANA-AN00(HWANA)、P40 Pro=ELS-AN00(HWELS)、P40 Pro+=VOG 系列。
     * MODEL 常见 "ELS-AN00"、"ANA-AN00"、"ELS-NX9"、"HUAWEI P40 Pro"。
     *
     * ⚠️ 华为 P 系列两个已知问题：
     *   ① 变焦按键与实际变焦不对应：逻辑头的 zoomRatio 域跟光学档位不是一回事，
     *      必须"下发后回读真实倍率"再刷新 UI，不能只显示下发值。
     *   ② 切到广角之后锁死在广角头：从广角切回 1x / 3x 时老代码判定"不需要换头"，
     *      于是只用广角做数码裁切，看起来就是"只能调用广角、别的头调不出来"。
     */
    val isHuaweiP40: Boolean
        get() = isHuawei && (
                model.startsWith("els") || device.startsWith("els") ||
                        model.startsWith("ana") || device.startsWith("ana") ||
                        model.contains("p40") || device.contains("p40")
                )

    /** 华为 P 系列（P20 / P30 / P40 / P50 / P60 / P70…） */
    val isHuaweiPSeries: Boolean
        get() = isHuawei && (
                isHuaweiP40 || model.contains("p30") || model.contains("p50") ||
                        model.contains("p60") || model.contains("p70") || model.contains("p20")
                )

    /**
     * 红米 Note 数字系列（Note 9 / 10 / 11 / 12 / 13 / 14 / 15 …）。
     * 这些机器的逻辑摄像头 minZoomRatio 报 1.0，setZoomRatio(0.6) 被静默 clamp 回 1.0，
     * 广角在第三方 App 里根本调不出来 —— 只能物理直连那颗超广角 id。
     */
    val isRedmiNoteSeries: Boolean
        get() = (brand.contains("redmi") || model.startsWith("redmi")) && model.contains("note")

    /**
     * ★"广角直接走物理直连"的机型★
     * 红米 Note 数字系列 / OPPO Find 系列实测：逻辑头 setZoomRatio(0.6) 完全无效，
     * 与其浪费一次无效下发再校验，不如一开始就直连超广角物理头。
     */
    val preferPhysicalWide: Boolean
        get() = (ActiveDevice.profile?.preferPhysicalWide == true) ||
                isRedmiNoteSeries || isOppoFindSeries || isXiaomiMi10Series ||
                isNubiaZSeries || isNubiaRedMagic

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

    /** realme / 真我。ColorOS 同宗，但品牌与型号段独立 */
    val isRealme: Boolean
        get() = manufacturer.contains("realme") || brand.contains("realme") ||
                model.startsWith("rmx") || device.startsWith("rmx")

    /**
     * 真我 GT5 Pro：2023-11 发布，realme UI 5.0(Android 14) + 骁龙 8 Gen 3，型号 RMX3888。
     * 三摄官方规格：主摄 LYT-808 23mm + 超广角 IMX355 16mm(112°) + 潜望 IMX890 65mm(36.4°)。
     * 折算倍率 ≈ 0.7x / 1x / 2.8x，UI 档位按官方标称写 0.6 / 1 / 3。
     * ⚠️ 它的逻辑摄像头在第三方 App 里只肯用主摄做数码裁切，长焦必须物理直连。
     *    识别不到时看 logInfo() 打出来的 MODEL / DEVICE，再补一条 startWith 即可。
     */
    val isRealmeGT5Pro: Boolean
        get() = isRealme && (
                model.contains("gt5 pro") || model.contains("gt5pro") ||
                        model.startsWith("rmx3888") || device.startsWith("rmx3888") ||
                        model.startsWith("rmx3889") || device.startsWith("rmx3889")
                )

    /**
     * iQOO 10 Pro：2022-07 发布，OriginOS(Android 12) + 骁龙 8+ Gen 1，型号 V2218A(海外 PD2218)。
     * 三摄：主摄 50MP 23mm + 超广角 50MP 15mm(150°) + 长焦 14.6MP 69mm。
     * 折算倍率 ≈ 0.65x / 1x / 3x（部分资料写 2.5x，以 dumpCameraInventory 的实测等效焦距为准）。
     * ⚠️ 那颗长焦只有 14.6MP，主摄那套大 JPEG / 4K 分辨率它不接受，绑定必须能降级。
     */
    val isIQOO10Pro: Boolean
        get() = isVivo && (
                model.startsWith("v2218") || device.startsWith("v2218") ||
                        model.startsWith("pd2218") || device.startsWith("pd2218") ||
                        model.contains("iqqo 10 pro") || model.contains("iqqo10pro")
                )

    /** 已确认"逻辑头不切长焦、长焦必须物理直连"的机型 */
    val isKnownTeleStubborn: Boolean
        get() = (ActiveDevice.profile?.teleStubborn == true) ||
                isRealmeGT5Pro || isIQOO10Pro || isVivoX80Pro || isIQOO15Series ||
                isOppoFindSeries || isOnePlusTeleStubborn || isHuaweiPSeries ||
                isXiaomiTeleStubborn

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
        get() = (ActiveDevice.profile?.teleStubborn == true) ||
                (ActiveDevice.profile?.preferPhysicalWide == true) ||
                isXiaomiMi10Series || isXiaomi13Ultra || isXiaomiOneInch ||
                isIQOO15Series || isVivoX80Pro || isRealmeGT5Pro || isIQOO10Pro ||
                isRedmiNoteSeries || isOppoFindSeries || isHuaweiPSeries ||
                isNubiaZSeries || isNubiaRedMagic

    /**
     * 出厂已知的光学档位，用于探测失败时兜底。
     * 数字取自各家官方规格页的「等效焦距 ÷ 主摄等效焦距」。
     * ★最后一条 else 给通用兜底★：2020 年后的多摄机基本都是"超广角+主摄+长焦"，
     * 探测成功时用真实结果覆盖，探测失败时至少保证 0.6x 按钮还在、能点。
     */
    val knownOpticalStops: List<Float>
        get() = when {
            isRealmeGT5Pro -> listOf(0.6f, 1.0f, 3.0f)   // 实测 16/23/65mm ≈ 0.7 / 1 / 2.8
            isIQOO10Pro -> listOf(0.6f, 1.0f, 3.0f)      // 实测 15/23/69mm ≈ 0.65 / 1 / 3
            isIQOO15Series -> listOf(0.6f, 1.0f, 3.0f)
            isVivoX80Pro -> listOf(0.6f, 1.0f, 2.0f, 5.0f)
            // ★档位一律整数★：真实焦距是 3.2x，按钮显示 3x（3.5 → 4）
            isXiaomi13Ultra -> listOf(0.6f, 1.0f, 3.0f, 5.0f)
            isXiaomiOneInch -> listOf(0.6f, 1.0f, 3.0f, 5.0f)
            isXiaomiMi10Series -> listOf(0.6f, 1.0f, 2.0f)
            // OPPO Find：超广角 + 主摄 + 潜望(3x)，Find X7 Ultra 还有 6x
            isOppoFindSeries -> listOf(0.6f, 1.0f, 3.0f, 6.0f)
            // ★努比亚 Z 系列：主摄 35mm，广角相对它是 0.5x，长焦 2.4x（≈85mm）★
            isNubiaZSeries -> listOf(0.5f, 1.0f, 2.0f)
            // 红魔：主摄 + 超广角，没有长焦
            isNubiaRedMagic -> listOf(0.6f, 1.0f, 2.0f)
            isOnePlusTeleStubborn -> listOf(0.6f, 1.0f, 3.0f)
            // 华为 P40：3x 光学(80mm)；P40 Pro / Pro+：5x 潜望(125mm)
            isHuaweiP40 -> if (model.contains("pro")) listOf(0.6f, 1.0f, 5.0f)
            else listOf(0.6f, 1.0f, 3.0f)
            // P50 Pro / P60 Pro：3.5x + 5x 两颗长焦
            isHuaweiPSeries -> listOf(0.6f, 1.0f, 4.0f, 5.0f)
            // 红米 Note 数字系列：超广角 + 主摄（多数没有独立长焦头）
            isRedmiNoteSeries -> listOf(0.6f, 1.0f, 2.0f)
            else -> listOf(0.6f, 1.0f, 2.0f)
        }

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
        get() = (zoomSettleDelayMs + 40L).coerceAtMost(360L)

    /** 变焦切档后的等待(ms)。切物理镜头要重新收敛，潜望头尤其慢 */
    /**
     * ★快切★：这一档是「切镜头慢」的第二大头。
     * 老值 400~700ms 是给 HAL 收敛留的余量，实测多数机器 180~260ms 就够，
     * 再叠加机型档案里给的值（用户机型已知时可以再压一点）。
     */
    val zoomSettleDelayMs: Long
        get() = ActiveDevice.settleMs() ?: when {
            isAndroid16Plus -> 260L
            isVivo -> 260L
            isXiaomi -> 220L
            else -> 180L
        }

    /**
     * 长焦头收敛等待(ms)。
     * 潜望 / 长焦的对焦行程长，绑上来立刻下发 setZoomRatio 会被 HAL 吞掉，
     * 表现就是"切到 3x 后画面还停在 1x"。比普通变焦多给 200~300ms。
     */
    val teleSettleDelayMs: Long
        get() = (zoomSettleDelayMs * 1.35f).roundToInt().toLong().coerceAtMost(420L)

    /** >= 这个值才考虑"换一颗物理长焦头"（避免 1.2x 这种数码档也去抢主摄头） */
    const val TELE_ENTER_RATIO = 1.8f
    /** 已在长焦头上时，低于这个值才退回逻辑头。留 0.25 滞回，防止 3x 拖到 2.8x 就抖回主摄 */
    const val TELE_EXIT_RATIO = 1.55f
    /** 长焦直连连续失败多少次才真正禁用（1 = 一次就封，太激进；2 起才封） */
    const val TELE_BIND_FAIL_LIMIT = 2

    /**
     * ★长焦头能"顺带"覆盖多少倍数码变焦★
     *
     * 单颗物理头自己的变焦范围很窄（很多长焦头本地只有 1.0~2.0x）。
     * 所以直连某颗 Nx 的长焦头后，最多只能靠它再放大 [TELE_HEAD_ROOM] 倍；
     * 超过这个倍数的目标必须回到逻辑摄像头走 setZoomRatio ——
     * 否则就是拿一颗 3x 头去扛 10x（本地要 3.33x，头根本给不了），
     * 表现正是"10x 切不过去 / 卡住不动"。
     */
    const val TELE_HEAD_ROOM = 2.5f

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
        val isOptical: Boolean = true,
        // ↓↓↓ 新增：这颗头能输出的最大 JPEG 尺寸（0 = 未探测到）。
        // 物理直连长焦头时要用它兜底，否则拿主摄的尺寸去绑长焦头会必失败。
        val maxJpegW: Int = 0,
        val maxJpegH: Int = 0
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

    /**
     * 清空所有进程级探测缓存。
     *
     * ★这个函数以前是【死代码】—— 定义了却从未被调用★
     *   （全文件搜不到任何调用点）。于是换机型、相机服务重启之后，
     *   这三份 static 缓存仍返回旧机型的镜头表 / 焦段，
     *   新档案的直连策略和档位根本套不上，而且没有任何途径能清掉，
     *   只能杀进程。这是"间歇性卡死/失效"的第二个来源。
     *
     * 现在在换机型的 onConfirm 里调用。
     */
    fun clearProbeCache() {
        lensProbeCache.clear()
        physicalProbeCache.clear()
        vendorStopsCache.clear()
        Log.d("Camera", "进程级探测缓存已清空（镜头表/物理头/厂商焦段）")
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
     * ★是否启用"CaptureRequest.PHYSICAL_CAMERA_ID 切换物理镜头"★
     *
     * 这条路不重新绑定相机（会话一直是逻辑摄像头），只是在每帧的 CaptureRequest 里
     * 指定"用哪颗物理头出图"。好处：
     *   · 不需要 cameraId 出现在 cameraIdList 里（ColorOS 的副摄只在 physicalCameraIds 下）
     *   · 不 unbind/rebind → 预览不闪、画面比例不跳、录像不会被打断
     *
     * ⚠️ 关闭它就退回"重新绑定物理头"的老路。
     */
    const val USE_PHYSICAL_REQUEST_SWITCH = true

    /**
     * physicalCameraId → 它所属的【逻辑摄像头】id。
     * 由 [probePhysicalCameras] 扫描 physicalCameraIds 时填入。
     */
    private val physicalOwner = HashMap<String, String>()

    /** 这颗物理头挂在哪个逻辑摄像头下 */
    fun ownerOfPhysical(id: String): String? = physicalOwner[id]

    /**
     * ★这颗头能不能用 CaptureRequest.PHYSICAL_CAMERA_ID 切换★
     *
     * 判定：它挂在某个逻辑头下（physicalCameraIds 里有），
     * 但**不在 cameraIdList 里**（不能独立打开、CameraX 绑不上）。
     * 两条同时成立才走这条路 —— 能独立打开的头继续走重新绑定的老路，
     * 已经适配好的机型行为完全不变。
     */
    fun isSwitchableViaRequest(id: String): Boolean =
        physicalOwner.containsKey(id) && !canBindDirectly(id)

    /**
     * ★CaptureRequest.PHYSICAL_CAMERA_ID 的 Key★
     *
     * ⚠️ 不能直接用 `CaptureRequest.PHYSICAL_CAMERA_ID` 静态常量：
     *    这个 key 是 API 29 才加入的，compileSdk < 29 的项目会报 Unresolved reference。
     *    改成运行时反射读取：
     *      · 编译期不依赖 SDK 版本 → 任何 compileSdk 都能编过
     *      · 运行时读不到（Android 9 及以下）→ 返回 null，自动退回"重新绑定物理头"的老路
     */
    @Suppress("UNCHECKED_CAST")
    fun physicalCameraIdKey(): CaptureRequest.Key<String>? = try {
        val f = CaptureRequest::class.java.getField("PHYSICAL_CAMERA_ID")
        f.get(null) as? CaptureRequest.Key<String>
    } catch (e: Exception) {
        null
    }

    /**
     * 能【独立打开】的 cameraId 集合。
     * 分两层：知道有这颗头（physicalCameraIds）→ 用于识别倍率、算档位；
     * 能独立打开（cameraIdList）→ 才能走物理直连。
     * ColorOS（OPPO Find X7 等）把长焦头挂在逻辑头下而它本身不在 cameraIdList 里，
     * 而 CameraX 的 CameraFilter 按 cameraId 匹配、候选集来自 cameraIdList，
     * 不在里面的 id 根本绑不上。
     * ⚠️ 放在这里是因为 object 成员的 probePhysicalCameras 要用它。
     */
    private var bindableIds: Set<String> = emptySet()

    /** 这颗头能不能被 CameraX 独立绑定 */
    fun canBindDirectly(id: String): Boolean = bindableIds.contains(id)

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

            // 先收集"待检查的 id"：
            //   ① cameraIdList 里所有同朝向的（传统路径）
            //   ② ★每颗逻辑头下挂的物理头★（physicalCameraIds，API 28+）
            //
            // ⚠️ ②是"OPPO Find X7 调不出长焦"的根因：
            //    ColorOS 上后置长焦头往往【不在 cameraIdList 里】，只挂在逻辑头的
            //    physicalCameraIds 下（cameraIdList 只有 "0" 后置逻辑 + "1" 前置逻辑）。
            //    老代码只遍历 cameraIdList → 池里只有 1 颗后置头 →
            //    requestLensSwitchIfNeeded 的 `pool.size < 2` 直接 return false，
            //    换头逻辑从头到尾没机会执行。
            //    （probeLenses 早就读了 physicalCameraIds，probePhysicalCameras 却漏了，
            //     两边不一致 —— 所以"镜头参数里有长焦，可用池里却没有"。）
            val idsToScan = LinkedHashSet<String>()
            // ★记录"可独立打开"的 id★：CameraX 只能绑这些，其余的只用来识别倍率
            bindableIds = manager.cameraIdList.toSet()
            for (id in manager.cameraIdList) {
                val chars = try {
                    manager.getCameraCharacteristics(id)
                } catch (e: Exception) {
                    continue
                }
                if (chars.get(CameraCharacteristics.LENS_FACING) != lensFacing) continue
                idsToScan.add(id)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        val phys = chars.physicalCameraIds
                        if (!phys.isEmpty()) {
                            Log.d("Camera", "逻辑头 $id 下挂物理头: ${phys.joinToString(",")}")
                            idsToScan.addAll(phys)
                            // ★记录归属★：这些头只能用 CaptureRequest.PHYSICAL_CAMERA_ID 切
                            for (pid in phys) physicalOwner[pid] = id
                        }
                    } catch (e: Exception) {
                        // 部分 ROM 这里会抛，忽略即可
                    }
                }
            }

            for (id in idsToScan) {
                val chars = try {
                    manager.getCameraCharacteristics(id)
                } catch (e: Exception) {
                    Log.w("Camera", "物理头 $id 读不到 CameraCharacteristics，跳过")
                    continue
                }
                // physicalCameraIds 里的头不一定上报 LENS_FACING，
                // 上报了且与目标朝向不符才过滤（没上报就按"属于这个逻辑头"处理）
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing != lensFacing) continue

                // ★长焦头常见坑①：不上报焦距★
                // vivo / realme 的部分物理头 LENS_INFO_AVAILABLE_FOCAL_LENGTHS 是 null 或 0，
                // 老代码在这里 continue，长焦头直接从池里消失 —— 后面自然"调不出长焦"。
                // 现在改成：焦距读不到也先收下（zoomFactor 记 0 = 待指派），
                // 只要这颗头能出 JPEG 就认为它可绑（景深 / 微距这类辅助头才会被丢掉）。
                val fl = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    ?.firstOrNull()
                val cfg = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val jpegSizes = try {
                    cfg?.getOutputSizes(ImageFormat.JPEG)?.toList()
                } catch (e: Exception) {
                    null
                }
                if ((fl == null || fl <= 0f) && jpegSizes.isNullOrEmpty()) continue

                val phys = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                val sensorW: Float = phys?.width ?: 0f
                val equiv: Float =
                    if (fl != null && fl > 0f && sensorW > 0f) fl * 36f / sensorW else 0f
                val maxJ = jpegSizes?.maxByOrNull { it.width * it.height }

                found.add(
                    LensProfile(
                        id = id,
                        focalLengthMm = fl ?: 0f,
                        sensorWidthMm = sensorW,
                        equiv35mm = equiv,
                        fovDeg = 0f,
                        aperture = null,
                        zoomFactor = 0f,
                        maxJpegW = maxJ?.width ?: 0,
                        maxJpegH = maxJ?.height ?: 0
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
            val mapped = out.map {
                val factor = if (base.equiv35mm > 0f) it.equiv35mm / base.equiv35mm else 0f
                it.copy(zoomFactor = if (factor > 0f) snapToNiceStop(factor) else 0f)
            }.sortedBy { it.zoomFactor }

            // ★长焦头常见坑②：焦距读不到 → 倍率算不出来 → 池里少一颗头★
            // 拿出厂档位兜底指派：主摄 / 超广角一般都会上报焦距，
            // 所以未知的那颗基本就是长焦，把 knownOpticalStops 里没被占用的档位给它。
            val sorted = reconcileStops(mapped)

            Log.d(
                "Camera",
                "★物理头池★ " + sorted.joinToString(", ") {
                    "${it.id}=${it.zoomFactor}x${if (canBindDirectly(it.id)) "" else "(仅识别)"}"
                } + "　（<1.0 超广角，>1.8 长焦）"
            )
            val cannotBind = sorted.filter { !canBindDirectly(it.id) }
            if (cannotBind.isNotEmpty()) {
                Log.w(
                    "Camera",
                    "以下头不在 cameraIdList 里，无法物理直连，只能靠逻辑头 setZoomRatio: " +
                            cannotBind.joinToString(", ") { "${it.id}(${it.zoomFactor}x)" }
                )
            }
            if (sorted.isNotEmpty()) physicalProbeCache[lensFacing] = sorted
            sorted
        } catch (e: Exception) {
            Log.w("Camera", "物理头扫描失败", e)
            emptyList()
        }
    }

    /**
     * 把"焦距没上报、倍率算不出来"的真实头按出厂档位补齐倍率。
     * 主摄 / 超广角一般都会上报焦距，所以未知的那颗基本就是长焦 ——
     * 不补齐的话它就以 0f 留在池里，换头逻辑永远不会挑中它。
     */
    private fun reconcileStops(list: List<LensProfile>): List<LensProfile> {
        val unknown = list.filter { it.zoomFactor <= 0f }
        if (unknown.isEmpty()) return list
        val used = list.filter { it.zoomFactor > 0f }.map { it.zoomFactor }
        val remain = knownOpticalStops.filter { k -> used.none { abs(it - k) < 0.05f } }
        if (remain.isEmpty()) {
            Log.w("Camera", "有 ${unknown.size} 颗头倍率未知且出厂档位已占满，按 1x 处理")
            return list.map { if (it.zoomFactor <= 0f) it.copy(zoomFactor = 1f) else it }
                .sortedBy { it.zoomFactor }
        }
        // 按 id 数字升序指派剩余档位（HAL 的 id 顺序基本就是 主摄 → 超广角 → 长焦）
        val ordered = unknown.sortedBy { it.id.toIntOrNull() ?: Int.MAX_VALUE }
        val assigned = ordered.mapIndexed { i, p ->
            val z = remain.getOrElse(i) { remain.last() }
            Log.w("Camera", "镜头 ${p.id} 未上报焦距，按出厂档位指派为 ${z}x")
            p.copy(
                zoomFactor = z,
                equiv35mm = if (p.equiv35mm > 0f) p.equiv35mm else BASE_EQUIV_MM * z
            )
        }
        return (list.filter { it.zoomFactor > 0f } + assigned).sortedBy { it.zoomFactor }
    }

    /**
     * ★读取"原相机（原厂 App）焦段"★
     *
     * 两条路，取到就用、取不到就空着（调用方会退回 Camera2 实测焦距，不会因此崩）：
     *   ① 系统属性：部分 ROM 把原厂焦段 / 变焦档位写在 ro.vendor.camera.*、vendor.camera.* 里
     *   ② 厂商配置文件：/system/etc/camera、/vendor/etc/camera 下的 xml / cfg，
     *      里面常有 "zoom_ratio"、"focal_length"、"ultra_wide" 之类的字段
     *
     * ⚠️ 全程 try/catch：Android 10+ 对 /vendor 的读取越来越严，读不到是常态，不影响其它逻辑。
     */
    private val VENDOR_CFG_DIRS = listOf(
        "/system/etc/camera", "/vendor/etc/camera", "/odm/etc/camera",
        "/system/etc", "/vendor/etc", "/my_product/etc/camera"
    )

    private val vendorStopsCache = ConcurrentHashMap<Int, List<Float>>()

    fun readVendorZoomStops(lensFacing: Int): List<Float> {
        vendorStopsCache[lensFacing]?.let { return it }
        val out = LinkedHashSet<Float>()
        try {
            // ① 系统属性
            for ((k, v) in systemProps) {
                val key = k.lowercase()
                if (!key.contains("camera")) continue
                if (!(key.contains("zoom") || key.contains("focal") ||
                            key.contains("lens") || key.contains("ratio"))
                ) continue
                val f = v.toFloatOrNull() ?: continue
                if (f in 0.3f..30f) out.add(snapToNiceStop(f))
            }
            // ② 厂商配置文件
            for (dir in VENDOR_CFG_DIRS) {
                val d = java.io.File(dir)
                if (!d.exists() || !d.isDirectory) continue
                val files = d.listFiles() ?: continue
                for (f in files) {
                    if (!f.isFile) continue
                    val n = f.name.lowercase()
                    if (!(n.endsWith(".xml") || n.endsWith(".cfg") || n.endsWith(".ini") ||
                                n.endsWith(".prop") || n.endsWith(".txt"))
                    ) continue
                    if (!(n.contains("camera") || n.contains("zoom") || n.contains("lens"))) continue
                    try {
                        // 只看前 400 行：焦段配置一般写在文件开头，避免读大文件拖慢启动
                        val text = f.bufferedReader().use { r ->
                            val sb = StringBuilder()
                            var line = r.readLine()
                            var count = 0
                            while (line != null && count < 400) {
                                sb.append(line).append('\n')
                                line = r.readLine()
                                count++
                            }
                            sb.toString()
                        }
                        val m = Regex("""(?:zoom|focal|ratio|factor)[^0-9-]{0,12}([0-9]{1,2}(?:\.[0-9]{1,2})?)""")
                        for (mr in m.findAll(text)) {
                            val v = mr.groupValues[1].toFloatOrNull() ?: continue
                            if (v in 0.3f..30f) out.add(snapToNiceStop(v))
                        }
                    } catch (e: Exception) {
                        // 单个文件读不动就算了
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("Camera", "读取原厂焦段失败（不影响主流程）", e)
        }
        // 至少保证 1x 存在；超过 6 个的多半是噪声
        val list = out.filter { it > 0f }.sorted().take(6).ifEmpty { emptyList() }
        if (list.isNotEmpty()) {
            vendorStopsCache[lensFacing] = list
            Log.d("Camera", "★原相机焦段★ ${list.joinToString("/")}")
        }
        return list
    }

    /**
     * 某个分辨率下能跑到的最高帧率（判断 4K60 在这台机器上到底行不行）。
     * 原理：getOutputMinFrameDuration(size) 返回最小帧间隔(ns)，maxFps = 1e9 / 该值。
     * 必须探测而非直接下发：setTargetFrameRate(Range(60,60)) 在不支持的机器上【不报错】，
     * 结果静默失败 —— 点了 4K60 录出来还是 30fps，或绑定失败退回 1080p。
     * 这正是 vivo 上"无法拍摄 4K60"的成因。
     * @return 该朝向所有摄像头里的最高帧率；探测不出来则乐观返回 60。
     */
    @SuppressLint("NewApi")
    fun maxFpsForSize(context: Context, lensFacing: Int, w: Int, h: Int): Int {
        return try {
            val manager = cameraManager(context)
            val size = android.util.Size(w, h)
            var best = 0
            //
            // ★"没有任何一颗头上报帧间隔"时才走乐观兜底★
            //   以前是【每遇到一颗不上报的头就 best = maxOf(best, 60)】，
            //   于是只要该朝向里有一颗头不上报（副摄很常见），
            //   哪怕主摄明确报了 30fps，结论也会被拉成 60 —— 探测形同虚设，
            //   vivo 上"点了 4K60 却录不出来"照样会发生。
            //   现在先把所有【有效上报】收集起来，只有一份有效数据都没有才兜底。
            //
            var reported = 0
            for (id in manager.cameraIdList) {
                val chars = try {
                    manager.getCameraCharacteristics(id)
                } catch (e: Exception) {
                    continue
                }
                if (chars.get(CameraCharacteristics.LENS_FACING) != lensFacing) continue
                val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?: continue
                val dur = try {
                    map.getOutputMinFrameDuration(android.media.MediaRecorder::class.java, size)
                } catch (e: Exception) {
                    null
                }
                if (dur == null || dur == 0L) continue   // 这颗不上报，跳过，不参与结论
                reported++
                best = maxOf(best, (1_000_000_000.0 / dur).toInt())
            }
            if (best <= 0) {
                Log.w("Camera", "帧率能力：${w}x${h} 没有任何一颗头上报帧间隔，按支持 60 处理")
                60
            } else best
        } catch (e: Exception) {
            Log.w("Camera", "帧率能力探测失败，按支持处理", e)
            60
        }
    }

    /**
     * ★4K60 能不能拍★
     * 留 1 帧余量：HAL 报 60 但实际可能掉到 59，按 >= 58 算支持。
     */
    fun supportsUhd60(context: Context, lensFacing: Int): Boolean =
        maxFpsForSize(context, lensFacing, 3840, 2160) >= 58

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
                    "oppo=$isOppo(1+=$isOnePlus, realme=$isRealme, gt5pro=$isRealmeGT5Pro), " +
                    "iqoo10pro=$isIQOO10Pro, samsung=$isSamsung, zte=$isZte"
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

// ==================== 品牌 / 系列机型库（首次启动选择） ====================
//
// 这一层的作用：把「用户手上的具体机型」翻译成相机 App 真正需要的几组策略：
//   ① 光学档位（0.6x / 1x / 2x / 3x / 5x…）—— 硬件扫不出来时用它兜底
//   ② 广角 / 长焦是走「逻辑摄像头 setZoomRatio」还是「物理直连」
//   ③ 切镜头后的收敛等待（决定切镜头快不快）
//   ④ 该系列默认的镜头色彩修正（统一多摄色彩用）
//   ⑤ 该系列的星空 / 星轨默认参数（长曝光能力、ISO、堆栈张数）
//
// 用户首次打开 App 时选一次（自动识别会先猜一个），之后可以在「关于 → 机型与色彩」里改。

/** 品牌。xiaomi 包含 Redmi / POCO（同 HAL、同一套适配策略） */
enum class BrandKey(val cn: String, val keywords: List<String>) {
    XIAOMI("小米 / Redmi", listOf("xiaomi", "redmi", "poco")),
    HUAWEI("华为", listOf("huawei", "harmony")),
    HONOR("荣耀", listOf("honor")),
    VIVO("vivo", listOf("vivo")),
    IQOO("iQOO", listOf("iqoo")),
    OPPO("OPPO", listOf("oppo")),
    ONEPLUS("一加", listOf("oneplus")),
    REALME("真我 realme", listOf("realme")),
    SAMSUNG("三星", listOf("samsung")),
    NUBIA("努比亚 / 红魔", listOf("nubia", "redmagic", "zte")),
    OTHER("其他 / 不确定", emptyList())
}

/** 切镜头策略 */
enum class LensSwitchStrategy(val label: String, val desc: String) {
    LOGICAL(
        "逻辑头变焦",
        "只用逻辑摄像头 setZoomRatio，最广最稳，副摄靠系统自己切。小米 / 荣耀 / 三星多数机型推荐"
    ),
    HYBRID(
        "混合",
        "广角 / 长焦切不动时才物理直连那颗头，中间倍率仍然走逻辑头"
    ),
    PHYSICAL_FIRST(
        "优先物理直连",
        "一上来就直连对应物理头。OPPO Find / 一加 / 华为 P、Mate 这类第三方调不动副摄的机器用"
    )
}

/** 该系列的星空 / 星轨默认参数 */
data class StarTrailPreset(
    val exposureSec: Float = 8f,     // 单帧曝光（秒），实际会按 HAL 上限再夹一次
    val iso: Int = 1600,             // 单帧 ISO
    val frames: Int = 60,            // 堆栈张数
    val intervalMs: Long = 400L,     // 帧间隔（写卡 + 让传感器喘口气）
    val denoise: Int = 55,           // 智能降噪强度 0~100
    val brighten: Int = 60           // 智能提亮强度 0~100
)

/**
 * 一个系列的完整适配档案。
 *
 * @param opticalStops   该系列常见的光学档位（相对主摄 ≈24mm 的倍率）
 * @param maxUsefulZoom  该系列可用变焦上限（照片模式 UI 的上限，超过画质不可看）
 * @param preferPhysicalWide 超广角必须物理直连（逻辑头 setZoomRatio(0.6) 无效的机器）
 * @param teleStubborn   长焦必须物理直连（逻辑头只肯给主摄裁切的机器）
 * @param strategy       切镜头策略
 * @param settleMs       切镜头 / 换物理头后的收敛等待（越小切得越快，太小会切不过去）
 * @param tint           该系列各摄之间的默认色彩偏差修正 [R,G,B] 增益，1.0 = 不动
 * @param hints          自动识别用的机型关键字（MODEL / DEVICE 含其一即命中）
 */
data class SeriesProfile(
    val brand: BrandKey,
    val id: String,
    val name: String,
    val summary: String,
    val opticalStops: List<Float>,
    val maxUsefulZoom: Float,
    val preferPhysicalWide: Boolean,
    val teleStubborn: Boolean,
    val strategy: LensSwitchStrategy,
    val settleMs: Long,
    val tint: List<Float>,
    val starTrail: StarTrailPreset,
    val hints: List<String>,
    val notes: String
)

private fun profile(
    brand: BrandKey,
    id: String,
    name: String,
    summary: String,
    stops: List<Float>,
    maxZoom: Float,
    wide: Boolean = false,
    tele: Boolean = false,
    strategy: LensSwitchStrategy = LensSwitchStrategy.LOGICAL,
    settle: Long = 220L,
    tint: List<Float> = listOf(1f, 1f, 1f),
    exposure: Float = 8f,
    iso: Int = 1600,
    frames: Int = 60,
    interval: Long = 400L,
    denoise: Int = 55,
    brighten: Int = 60,
    hints: List<String> = emptyList(),
    notes: String = ""
) = SeriesProfile(
    brand = brand,
    id = id,
    name = name,
    summary = summary,
    opticalStops = stops,
    maxUsefulZoom = maxZoom,
    preferPhysicalWide = wide,
    teleStubborn = tele,
    strategy = strategy,
    settleMs = settle,
    tint = tint,
    starTrail = StarTrailPreset(exposure, iso, frames, interval, denoise, brighten),
    hints = hints,
    notes = notes
)

object SeriesDb {

    val all: List<SeriesProfile> = listOf(
        // ───────────────────────── 小米 / Redmi ─────────────────────────
        profile(
            BrandKey.XIAOMI, "mi_flag", "小米数字系列",
            "小米 13 / 14 / 15、Ultra、Pro。徕卡色彩，广角靠 setZoomRatio 正常，长焦必须物理直连",
            listOf(0.6f, 1f, 2f, 3f, 5f), 30f,
            //
            // ★★改成 PHYSICAL_FIRST（原来是 HYBRID，长焦调不出来）★★
            //
            // HYBRID 的语义是"先试逻辑头，切不动了才直连"。
            // 但小米的判定根本不成立 —— MIUI / HyperOS 的逻辑头对第三方 App
            // 只做数码变焦：setZoomRatio(3.0) 不报错、zoomState 也照样回报 3.0，
            // 于是 HYBRID 认为"切成功了"，永远不会走到直连那一步。
            // 表现就是"能放大、但画质是主摄裁切的糊图"，也就是用户反馈的长焦调不出来。
            //
            // 改成 PHYSICAL_FIRST：一上来就直连那颗长焦头。
            // 小米这批机器的长焦头都在 cameraIdList 里（CameraX 绑得上），
            // 直连成功率很高；万一绑不上，teleBindFailCount 累计 2 次自动退回逻辑头。
            //
            wide = false, tele = true, strategy = LensSwitchStrategy.PHYSICAL_FIRST, settle = 260L,
            tint = listOf(1.00f, 1.00f, 1.02f),
            hints = listOf(
                "mi 1", "mi1", "mi 9", "mi9", "2304", "2403", "2501",
                "ishtar", "umi", "cmi", "cas", "thyme", "venus", "star",
                "zeus", "cupid", "houji", "shennong", "dada", "aurora", "nuwa", "fuxi"
            ),
            notes = "★档位已统一为整数★（3.2x→3x）；★长焦走 PHYSICAL_FIRST 直连★" +
                    "（HYBRID 在小米上永远判不出‘切不动’，所以必须一上来就直连）；" +
                    "若连续 2 次绑定失败会自动退回逻辑头，不会卡死"
        ),
        profile(
            BrandKey.XIAOMI, "mi_mix", "小米 MIX / 折叠",
            "MIX Fold / MIX Flip。折叠形态，副摄与主摄方向不同，切头要多给一点收敛时间",
            listOf(0.6f, 1f, 2f, 3f), 20f,
            strategy = LensSwitchStrategy.HYBRID, settle = 280L,
            tint = listOf(1.00f, 1.00f, 1.02f),
            hints = listOf("mix", "fold"),
            notes = "折叠机展开 / 合盖时会触发配置变更，切头后等 280ms 再下发倍率"
        ),
        profile(
            BrandKey.XIAOMI, "mi_civi", "小米 Civi 系列",
            "轻薄自拍机，前置强、后置双摄为主，长焦基本没有",
            listOf(0.6f, 1f, 2f), 10f,
            tint = listOf(1.00f, 1.00f, 1.01f),
            hints = listOf("civi"),
            notes = "后置只有主摄 + 超广角，2x 以上全是数码裁切"
        ),
        profile(
            BrandKey.XIAOMI, "redmi_k", "Redmi K / Turbo / Note",
            "走量性能机。Note 数字系列的逻辑头把 minZoomRatio 报成 1.0，0.6x 必须物理直连",
            listOf(0.6f, 1f, 2f), 10f,
            wide = true, tele = false, strategy = LensSwitchStrategy.HYBRID, settle = 240L,
            tint = listOf(1.00f, 1.00f, 1.02f),
            hints = listOf("redmi note", "note 1", "note1", "2312", "2411", "2412", "m2101"),
            notes = "红米 Note 的广角只能直连物理头，已内置；长焦档位（2x）是主摄裁切"
        ),
        profile(
            BrandKey.XIAOMI, "redmi_a", "Redmi 数字 / A 系列",
            "入门机，单摄或主摄 + 凑数副摄，没有可用的光学变焦",
            listOf(1f, 2f), 8f,
            tint = listOf(1.00f, 1.00f, 1.02f),
            hints = listOf("redmi a", "redmi 1", "redmi1", "2302", "2311"),
            exposure = 1f, iso = 3200, frames = 120, interval = 200L,
            notes = "没有手动长曝光能力，星空靠「短曝光多张堆栈」"
        ),

        // ───────────────────────── 华为 ─────────────────────────
        profile(
            BrandKey.HUAWEI, "hw_mate", "华为 Mate 系列",
            "XMAGE 商务旗舰，潜望长焦强。第三方 App 里长焦常年切不动，必须物理直连",
            listOf(0.6f, 1f, 3f, 5f, 10f), 50f,
            wide = false, tele = true, strategy = LensSwitchStrategy.PHYSICAL_FIRST, settle = 320L,
            tint = listOf(1.02f, 1.00f, 0.98f),
            hints = listOf("mate", "al00", "lx9", "ana", "els", "vog"),
            notes = "变焦按钮要「下发后回读倍率」再刷新 UI，不能只显示下发值"
        ),
        profile(
            BrandKey.HUAWEI, "hw_pura", "华为 Pura / P 系列",
            "影像旗舰，可变光圈。P40 一代有两个老坑：倍率不对应、切到广角后锁死在广角头",
            listOf(0.6f, 1f, 3.5f, 10f), 50f,
            wide = false, tele = true, strategy = LensSwitchStrategy.PHYSICAL_FIRST, settle = 320L,
            tint = listOf(1.02f, 1.00f, 0.98f),
            hints = listOf("pura", "p40", "p50", "p60", "p70", "els", "ana", "vog", "hre", "jef"),
            notes = "P40 系列已单独兜底：广角→1x/3x 会强制换头，不会停在广角上裁切"
        ),
        profile(
            BrandKey.HUAWEI, "hw_nova", "华为 nova 系列",
            "中端颜值机，前置人像强，后置主摄 + 超广角为主",
            listOf(0.6f, 1f, 2f, 3f), 10f,
            tint = listOf(1.02f, 1.00f, 0.98f),
            hints = listOf("nova"),
            notes = "2x / 3x 多为主摄裁切，色彩偏暖已内置修正"
        ),
        profile(
            BrandKey.HUAWEI, "hw_enjoy", "华为 畅享系列",
            "入门长续航，单摄 / 双摄，只有数码变焦",
            listOf(1f, 2f), 8f,
            tint = listOf(1.02f, 1.00f, 0.98f),
            hints = listOf("changxiang", "enjoy", "畅享", "tyh", "mga"),
            exposure = 1f, iso = 3200, frames = 120, interval = 200L,
            notes = "没有手动长曝光，星空靠短曝光多张堆栈"
        ),
        profile(
            BrandKey.HUAWEI, "hw_fold", "华为 折叠（Mate X / Pocket）",
            "折叠形态，多摄分布在不同机身块上，切头收敛更慢",
            listOf(0.6f, 1f, 3f, 5f), 20f,
            tele = true, strategy = LensSwitchStrategy.HYBRID, settle = 340L,
            tint = listOf(1.02f, 1.00f, 0.98f),
            hints = listOf("mate x", "pocket", "bal", "tah"),
            notes = "折叠屏外屏 / 内屏切换会重建会话，切头等待给到 340ms"
        ),

        // ───────────────────────── 荣耀 ─────────────────────────
        profile(
            BrandKey.HONOR, "honor_magic", "荣耀 Magic 系列",
            "高端全能旗舰，鹰眼抓拍。部分机器 zoomState 报 min=max=1.0，需要兜底数码变焦",
            listOf(0.6f, 1f, 2.5f, 5f, 10f), 30f,
            tele = true, strategy = LensSwitchStrategy.HYBRID, settle = 300L,
            tint = listOf(1.01f, 1.00f, 0.99f),
            hints = listOf("magic", "bvl", "pct", "bab"),
            notes = "zoomState 全 1.0 时走 FALLBACK_MAX_ZOOM 兜底，不会「变焦没反应」"
        ),
        profile(
            BrandKey.HONOR, "honor_num", "荣耀数字系列",
            "中高端轻薄影像（50 / 90 / 100 / 200 / 300 / 400）",
            listOf(0.6f, 1f, 2f, 3f), 20f,
            tint = listOf(1.01f, 1.00f, 0.99f),
            hints = listOf("honor ", "hma", "lge", "ali", "brp"),
            notes = "长焦多为 2x~3x 直立长焦，逻辑头可切"
        ),
        profile(
            BrandKey.HONOR, "honor_x", "荣耀 X 系列",
            "千元续航机，主摄 + 凑数副摄，只有数码变焦",
            listOf(1f, 2f), 8f,
            tint = listOf(1.01f, 1.00f, 0.99f),
            hints = listOf("honor x", "x40", "x50", "x60", "clk", "llc"),
            exposure = 1f, iso = 3200, frames = 120, interval = 200L,
            notes = "没有手动长曝光，星空靠短曝光多张堆栈"
        ),
        profile(
            BrandKey.HONOR, "honor_fold", "荣耀 折叠（Magic V / Vs / V2 / V3）",
            "折叠形态，内屏外屏各一套取景，切头收敛慢一点",
            listOf(0.6f, 1f, 3f, 5f), 20f,
            strategy = LensSwitchStrategy.HYBRID, settle = 320L,
            tint = listOf(1.01f, 1.00f, 0.99f),
            hints = listOf("magic v", "vs", "ver"),
            notes = "展开状态才有多摄全焦段，合盖时只有外屏单摄"
        ),

        // ───────────────────────── vivo ─────────────────────────
        profile(
            BrandKey.VIVO, "vivo_x", "vivo X 系列",
            "蔡司影像旗舰（X80 / X90 / X100 / X200 / X300）。潜望长焦在第三方里切不动，要直连",
            listOf(0.6f, 1f, 2f, 4.3f, 10f), 30f,
            tele = true, strategy = LensSwitchStrategy.PHYSICAL_FIRST, settle = 300L,
            tint = listOf(1.02f, 0.99f, 0.98f),
            hints = listOf("v2145", "v2144", "v2218", "v2301", "v2312", "v2405", "x80", "x90", "x100", "x200"),
            notes = "X80 Pro / X100 Pro 的 2x 是人像裁切档，真实光学档位是 0.6 / 1 / 4.3"
        ),
        profile(
            BrandKey.VIVO, "vivo_s", "vivo S 系列",
            "中端人像自拍机，前置是卖点，后置主摄 + 超广角",
            listOf(0.6f, 1f, 2f), 10f,
            tint = listOf(1.02f, 0.99f, 0.98f),
            hints = listOf("vivo s", "s15", "s16", "s17", "s18", "s19", "s20", "v2230"),
            notes = "2x 是主摄裁切，色彩偏暖已内置修正"
        ),
        profile(
            BrandKey.VIVO, "vivo_y", "vivo Y 系列",
            "入门长续航，单摄 / 双摄，只有数码变焦",
            listOf(1f, 2f), 8f,
            tint = listOf(1.02f, 0.99f, 0.98f),
            hints = listOf("vivo y", "y35", "y36", "y37", "y50", "y100", "y200", "v2235"),
            exposure = 1f, iso = 3200, frames = 120, interval = 200L,
            notes = "没有手动长曝光，星空靠短曝光多张堆栈"
        ),
        profile(
            BrandKey.VIVO, "vivo_fold", "vivo 折叠（X Fold / X Flip）",
            "折叠形态，多摄分布在不同机身块，切头收敛慢",
            listOf(0.6f, 1f, 2f, 3f), 20f,
            strategy = LensSwitchStrategy.HYBRID, settle = 320L,
            tint = listOf(1.02f, 0.99f, 0.98f),
            hints = listOf("x fold", "xfold", "x flip", "xflip"),
            notes = "内屏展开时才有完整多摄"
        ),

        // ───────────────────────── iQOO ─────────────────────────
        profile(
            BrandKey.IQOO, "iqoo_flag", "iQOO 数字 / Neo 系列",
            "性能旗舰（iQOO 10 / 11 / 12 / 13 / 15）。iQOO 15 光学档位是 0.6 / 1 / 3（没有 2x 人像头）",
            listOf(0.6f, 1f, 3f), 20f,
            tele = true, strategy = LensSwitchStrategy.HYBRID, settle = 280L,
            tint = listOf(1.02f, 1.00f, 0.98f),
            hints = listOf("iqoo", "v2218", "v2304", "v2314", "v2401", "v2505", "pd2505", "i2505"),
            notes = "iQOO 10 Pro 的长焦只有 14.6MP，大 JPEG / 4K 分辨率它不吃，绑定会自动降级"
        ),
        profile(
            BrandKey.IQOO, "iqoo_z", "iQOO Z / U 系列",
            "千元性能机，主摄 + 凑数副摄，只有数码变焦",
            listOf(1f, 2f), 8f,
            tint = listOf(1.02f, 1.00f, 0.98f),
            hints = listOf("iqoo z", "iqoo u", "i2201", "i2215", "i2301"),
            exposure = 1f, iso = 3200, frames = 120, interval = 200L,
            notes = "没有手动长曝光，星空靠短曝光多张堆栈"
        ),

        // ───────────────────────── OPPO ─────────────────────────
        profile(
            BrandKey.OPPO, "oppo_findx", "OPPO Find X 系列",
            "哈苏影像旗舰（X5 / X6 / X7 / X8 / X9）。逻辑头只肯给主摄，广角和潜望都必须物理直连",
            listOf(0.6f, 1f, 3f, 6f), 30f,
            wide = true, tele = true, strategy = LensSwitchStrategy.PHYSICAL_FIRST, settle = 320L,
            tint = listOf(1.01f, 1.00f, 1.00f),
            hints = listOf("find x", "findx", "phk", "phb", "pht", "pha", "phj", "phz", "op5", "cph"),
            notes = "内部代号是 OP+4 位十六进制（如 OP5A3D），已自动识别"
        ),
        profile(
            BrandKey.OPPO, "oppo_findn", "OPPO 折叠（Find N / N2 / N3 / N5）",
            "折叠形态，悬停拍摄，多摄分布在不同机身块",
            listOf(0.6f, 1f, 2f, 3f), 20f,
            strategy = LensSwitchStrategy.HYBRID, settle = 320L,
            tint = listOf(1.01f, 1.00f, 1.00f),
            hints = listOf("find n", "findn", "cph24", "cph25"),
            notes = "折叠机展开 / 合盖会重建会话"
        ),
        profile(
            BrandKey.OPPO, "oppo_reno", "OPPO Reno 系列",
            "中端轻薄人像，主摄 + 超广角 + 直立长焦（部分带潜望）",
            listOf(0.6f, 1f, 2f, 5f), 20f,
            tint = listOf(1.01f, 1.00f, 1.00f),
            hints = listOf("reno", "pht110", "cph25", "phj110"),
            notes = "5x 档位在带潜望的机型上才是光学，其余是裁切"
        ),
        profile(
            BrandKey.OPPO, "oppo_ka", "OPPO K / A 系列",
            "线上性价比 / 入门耐用机，主摄 + 凑数副摄，只有数码变焦",
            listOf(1f, 2f), 8f,
            tint = listOf(1.01f, 1.00f, 1.00f),
            hints = listOf("oppo k", "oppo a", "cph2", "pku", "phw"),
            exposure = 1f, iso = 3200, frames = 120, interval = 200L,
            notes = "没有手动长曝光，星空靠短曝光多张堆栈"
        ),

        // ───────────────────────── 一加 ─────────────────────────
        profile(
            BrandKey.ONEPLUS, "op_num", "一加数字系列",
            "一加 11 / 12 / 13 / 15。与 OPPO 同宗 HAL，长焦在第三方里常年切不动，必须直连",
            listOf(0.6f, 1f, 2f, 3f, 6f), 30f,
            tele = true, strategy = LensSwitchStrategy.PHYSICAL_FIRST, settle = 300L,
            tint = listOf(1.00f, 1.00f, 1.01f),
            hints = listOf("oneplus", "kb200", "cph25", "pjd", "op59", "op5a"),
            notes = "一加的长焦逻辑头同样只给主摄裁切，已按「顽固机型」处理"
        ),
        profile(
            BrandKey.ONEPLUS, "op_ace", "一加 Ace 系列",
            "性能中端（Ace / Ace Pro / Ace 2 / Ace 3 / Ace 5），主摄 + 超广角为主",
            listOf(0.6f, 1f, 2f), 10f,
            tint = listOf(1.00f, 1.00f, 1.01f),
            hints = listOf("ace", "php", "pje", "pjw"),
            notes = "2x 是主摄裁切，没有独立长焦头"
        ),

        // ───────────────────────── 真我 realme ─────────────────────────
        profile(
            BrandKey.REALME, "rm_gt", "真我 GT 系列",
            "GT / GT Neo / GT5 / GT5 Pro / GT6 / GT7 / GT8。GT5 Pro 的潜望逻辑头只给主摄裁切，要直连",
            listOf(0.6f, 1f, 2.8f, 3f), 20f,
            tele = true, strategy = LensSwitchStrategy.HYBRID, settle = 280L,
            tint = listOf(1.01f, 1.00f, 0.99f),
            hints = listOf("gt", "rmx3888", "rmx3889", "rmx3", "rmx5"),
            notes = "GT5 Pro 光学档位 0.7 / 1 / 2.8（官方标 0.6 / 1 / 3）"
        ),
        profile(
            BrandKey.REALME, "rm_neo", "真我 Neo 系列",
            "Neo / Neo SE，中端性能，主摄 + 超广角",
            listOf(0.6f, 1f, 2f), 10f,
            tint = listOf(1.01f, 1.00f, 0.99f),
            hints = listOf("neo", "rmx3", "rmx4"),
            notes = "没有独立长焦，2x 是主摄裁切"
        ),
        profile(
            BrandKey.REALME, "rm_num", "真我数字 / V / Q 系列",
            "入门走量机，单摄 / 双摄，只有数码变焦",
            listOf(1f, 2f), 8f,
            tint = listOf(1.01f, 1.00f, 0.99f),
            hints = listOf("rmx1", "rmx2", "realme v", "realme q"),
            exposure = 1f, iso = 3200, frames = 120, interval = 200L,
            notes = "没有手动长曝光，星空靠短曝光多张堆栈"
        ),

        // ───────────────────────── 三星 ─────────────────────────
        profile(
            BrandKey.SAMSUNG, "ss_s", "三星 S 系列",
            "S22 / S23 / S24 / S25（含 + / Ultra）。全焦段覆盖，第三方能拿到物理头但需要先解锁",
            listOf(0.6f, 1f, 2f, 3f, 5f, 10f), 30f,
            tele = true, strategy = LensSwitchStrategy.HYBRID, settle = 300L,
            tint = listOf(0.99f, 1.01f, 1.02f),
            hints = listOf("sm-s", "sm-s9", "s918", "s921", "s928", "s931", "galaxy s"),
            notes = "三星对第三方开放物理头，但 10x 需要切到那颗头后再下发倍率，否则是主摄裁切"
        ),
        profile(
            BrandKey.SAMSUNG, "ss_z", "三星 折叠（Z Fold / Z Flip）",
            "折叠旗舰，多摄 + 特殊比例，Flex 模式悬停拍摄",
            listOf(0.6f, 1f, 2f, 3f), 20f,
            strategy = LensSwitchStrategy.HYBRID, settle = 320L,
            tint = listOf(0.99f, 1.01f, 1.02f),
            hints = listOf("sm-f", "z fold", "zfold", "z flip", "zflip"),
            notes = "折叠机外屏只有单摄，展开才有完整多摄"
        ),
        profile(
            BrandKey.SAMSUNG, "ss_a", "三星 A 系列",
            "中端 / 入门普及机（A3x / A5x / A7x），主摄 + 超广角为主",
            listOf(0.6f, 1f, 2f), 8f,
            tint = listOf(0.99f, 1.01f, 1.02f),
            hints = listOf("sm-a", "galaxy a"),
            exposure = 2f, iso = 2000, frames = 90, interval = 300L,
            notes = "A 系列色彩偏冷偏绿，已内置修正"
        ),

        // ───────────────────────── 努比亚 / 红魔 ─────────────────────────
        profile(
            BrandKey.NUBIA, "nb_z", "努比亚 Z 系列",
            "真全面屏，35mm 主摄是特色（等效倍率 0.5 / 1 / 2.4），星空 / 星轨玩法原生支持好",
            listOf(0.5f, 1f, 2.4f), 20f,
            // ★广角和长焦都必须物理直连★：MyOS 对第三方 App 的逻辑摄像头限制很严，
            // setZoomRatio(0.5) 会被静默 clamp 回 1.0，长焦也只给主摄裁切。
            // 这就是"努比亚调不出广角和长焦"的根因 —— 之前只写了 tele = true，
            // 广角那条路没开，0.5x 一直靠逻辑头，永远拿不到。
            wide = true, tele = true,
            strategy = LensSwitchStrategy.PHYSICAL_FIRST, settle = 320L,
            tint = listOf(1.00f, 1.00f, 1.00f),
            hints = listOf("nubia", "nx7", "nx8", "nx", "z50", "z60", "z70", "z80", "z40", "z30"),
            exposure = 15f, iso = 1250, frames = 40, interval = 500L, denoise = 50, brighten = 65,
            notes = "主摄是 35mm，所以「1x」比别家窄，广角档是 0.5x（不是别家的 0.6x）；" +
                    "广角与长焦均已强制物理直连"
        ),
        profile(
            BrandKey.NUBIA, "nb_red", "红魔系列",
            "专业电竞机，摄像只求够用，主摄 + 超广角，没有长焦",
            listOf(0.6f, 1f, 2f), 8f,
            // 红魔虽然没有长焦，但超广角同样受 MyOS 限制，需要物理直连
            wide = true,
            strategy = LensSwitchStrategy.HYBRID, settle = 280L,
            tint = listOf(1.00f, 1.00f, 1.00f),
            hints = listOf("redmagic", "red magic", "魔", "nx7"),
            exposure = 2f, iso = 2000, frames = 90, interval = 300L,
            notes = "游戏机优先帧率，长曝光能力有限，用短曝光堆栈；超广角需物理直连"
        ),
        profile(
            BrandKey.NUBIA, "nb_flip", "努比亚 折叠 / 小牛",
            "Flip 折叠与入门小牛，主摄 + 超广角",
            listOf(0.6f, 1f, 2f), 10f,
            wide = true,
            strategy = LensSwitchStrategy.HYBRID, settle = 260L,
            tint = listOf(1.00f, 1.00f, 1.00f),
            hints = listOf("flip", "小牛", "nb"),
            notes = "折叠机外屏只有单摄；超广角需物理直连"
        ),

        // ───────────────────────── 其他 ─────────────────────────
        profile(
            BrandKey.OTHER, "other_generic", "通用兼容模式",
            "不确定机型 / 小众品牌。全部走逻辑摄像头 setZoomRatio，行为最保守也最稳",
            listOf(0.6f, 1f, 2f, 5f, 10f), 20f,
            strategy = LensSwitchStrategy.LOGICAL, settle = 220L,
            tint = listOf(1f, 1f, 1f),
            hints = emptyList(),
            notes = "如果发现广角 / 长焦切不出来，到「机型与色彩」里手动换成「混合」或「优先物理直连」"
        )
    )

    fun byBrand(brand: BrandKey): List<SeriesProfile> = all.filter { it.brand == brand }

    fun find(id: String?): SeriesProfile? = all.firstOrNull { it.id == id }

    val fallback: SeriesProfile = all.last()

    /**
     * 自动识别：先定品牌，再按机型关键字猜系列。
     * 只是给个默认建议，用户可以在弹窗里改。
     */
    fun guess(): SeriesProfile? {
        val brandL = Build.BRAND.lowercase()
        val manuL = Build.MANUFACTURER.lowercase()
        val modelL = Build.MODEL.lowercase()
        val deviceL = Build.DEVICE.lowercase()
        val text = "$brandL $manuL $modelL $deviceL"

        val brand = BrandKey.values().firstOrNull { key ->
            key != BrandKey.OTHER && key.keywords.any { text.contains(it) }
        } ?: return null

        val pool = byBrand(brand)
        return pool.firstOrNull { p ->
            p.hints.any { h -> h.isNotBlank() && (modelL.contains(h) || deviceL.contains(h)) }
        } ?: pool.firstOrNull()
    }

    /** 给 Logcat 用的一行摘要 */
    fun describe(p: SeriesProfile?): String =
        if (p == null) "未指定机型（走通用兼容）" else "${p.brand.cn} · ${p.name}（${p.id}）"
}

/** 当前生效的机型档案。整进程共享，改它就会立刻影响 DeviceCompatibility 的判定 */
object ActiveDevice {
    @Volatile
    var profile: SeriesProfile? = null

    /** 快速切镜头开关（「机型与色彩」里可关） */
    @Volatile
    var fastLensSwitch: Boolean = true

    fun apply(p: SeriesProfile?) {
        profile = p
        LensColorUnifier.onProfileChanged(p)
        Log.d("DeviceProfile", "机型档案生效：${SeriesDb.describe(p)}")
    }

    fun stops(): List<Float> = profile?.opticalStops ?: emptyList()
    fun maxUsefulZoom(): Float = profile?.maxUsefulZoom ?: 20f
    fun settleMs(): Long? = profile?.settleMs
    fun strategy(): LensSwitchStrategy = profile?.strategy ?: LensSwitchStrategy.LOGICAL
    fun tint(): List<Float> = profile?.tint ?: listOf(1f, 1f, 1f)
    fun starTrail(): StarTrailPreset = profile?.starTrail ?: StarTrailPreset()
}

/** 机型档案的持久化（独立 prefs，不跟相机设置混在一起） */
object DeviceProfileStore {
    private const val PREFS = "silky_device_profile"
    private const val K_SERIES = "series_id"
    private const val K_ONBOARD = "onboarded"
    private const val K_COLOR = "color_unify"
    private const val K_FAST = "fast_lens_switch"
    private const val K_LOCK_AWB = "lock_awb"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isOnboarded(context: Context): Boolean = prefs(context).getBoolean(K_ONBOARD, false)

    fun load(context: Context): SeriesProfile? {
        val id = prefs(context).getString(K_SERIES, null) ?: return null
        return SeriesDb.find(id)
    }

    fun save(context: Context, p: SeriesProfile?) {
        prefs(context).edit()
            .putString(K_SERIES, p?.id)
            .putBoolean(K_ONBOARD, true)
            .apply()
    }

    fun colorUnify(context: Context): Boolean = prefs(context).getBoolean(K_COLOR, true)
    fun setColorUnify(context: Context, on: Boolean) =
        prefs(context).edit().putBoolean(K_COLOR, on).apply()

    /** 快速切镜头（省掉重复的镜头扫描和不必要的等待） */
    fun fastLensSwitch(context: Context): Boolean = prefs(context).getBoolean(K_FAST, true)
    fun setFastLensSwitch(context: Context, on: Boolean) =
        prefs(context).edit().putBoolean(K_FAST, on).apply()

    /** 锁定白平衡：默认【关】。它的作用是防白平衡漂移，不是统一色彩（见 LensColorUnifier.lockAwb） */
    fun lockAwb(context: Context): Boolean = prefs(context).getBoolean(K_LOCK_AWB, false)
    fun setLockAwb(context: Context, on: Boolean) =
        prefs(context).edit().putBoolean(K_LOCK_AWB, on).apply()
}

/**
 * ★变焦档位持久化★ —— 修「每次打开相机变焦按键都不一样」
 *
 * 变焦按键（0.6x / 1x / 2x / 5x…）每次冷启动都由多个【非确定性输入】现算：
 *   · opticalStops 走三条路径：Camera2 实测焦距 / 读厂商配置文件 / 出厂档位兜底
 *   · minZoomRatio / maxZoomRatio 从 zoomState 读，HAL 上报时机不定，
 *     刚绑定时可能还是默认占位值（0.6 / 10.0）
 *   · readVendorZoomStops 读系统属性和厂商配置文件，有时读到有时读不到
 * 于是同样的手机，开三次相机可能算出三套不同的档位 —— 按键数量、倍率全在变。
 *
 * 解决：把算出来的档位存下来，下次启动【先套用缓存】，保证一致；
 * 只有这次探测结果确实更完整（光学档位更多）时才更新缓存。
 */
object ZoomStopsStore {
    private const val PREFS = "silky_zoom_stops"
    private const val K_BACK = "stops_back"
    private const val K_FRONT = "stops_front"
    private const val K_OPT_BACK = "optical_back"
    private const val K_OPT_FRONT = "optical_front"

    private const val SEP = ","

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun keyFor(facing: Int) =
        if (facing == CameraSelector.LENS_FACING_FRONT) K_FRONT else K_BACK

    private fun optKeyFor(facing: Int) =
        if (facing == CameraSelector.LENS_FACING_FRONT) K_OPT_FRONT else K_OPT_BACK

    private fun encode(list: List<Float>): String =
        list.joinToString(SEP) { "%.2f".format(it) }

    private fun decode(raw: String?): List<Float> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(SEP).mapNotNull { it.toFloatOrNull() }
            .filter { it > 0f && it.isFinite() }
            .distinct()
            .sorted()
    }

    fun load(context: Context, facing: Int): List<Float> =
        decode(prefs(context).getString(keyFor(facing), null))

    fun loadOptical(context: Context, facing: Int): List<Float> =
        decode(prefs(context).getString(optKeyFor(facing), null))

    fun save(
        context: Context,
        facing: Int,
        stops: List<Float>,
        optical: List<Float>
    ) {
        if (stops.isEmpty()) return
        prefs(context).edit()
            .putString(keyFor(facing), encode(stops))
            .putString(optKeyFor(facing), encode(optical))
            .apply()
        Log.d("Camera", "变焦档位已存档(${if (facing == CameraSelector.LENS_FACING_FRONT) "前" else "后"}摄): ${stops.joinToString("/")}")
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}

/**
 * ★判断本次探测结果是否「比缓存更完整」★
 *
 * 只在新结果【严格更好】时才更新，避免某一次探测残缺（比如厂商配置文件这次没读到）
 * 把好不容易稳定下来的档位污染成差的 —— 那正是"按键忽多忽少"的直接来源。
 *
 * 判定按顺序：
 *   ① 缓存为空 → 新结果直接采用（首次启动）
 *   ② 新结果的光学档位更多 → 采用（探测更充分了）
 *   ③ 新结果的光学档位是缓存光学档的【超集】→ 采用
 *   ④ 否则 → 保留缓存（哪怕这次多探到一两个数码档，也不冒险换掉整套档位）
 */
fun zoomStopsBetter(
    cached: List<Float>,
    cachedOptical: List<Float>,
    fresh: List<Float>,
    freshOptical: List<Float>
): Boolean {
    if (cached.isEmpty()) return fresh.isNotEmpty()
    if (fresh.isEmpty()) return false
    // ★结果完全一致就不算"更好"★：避免每次都无谓地重写存档、刷一堆日志
    if (fresh == cached && freshOptical == cachedOptical) return false
    if (freshOptical.size > cachedOptical.size) return true
    if (cachedOptical.isNotEmpty() && freshOptical.containsAll(cachedOptical) &&
        fresh.size >= cached.size
    ) return true
    return false
}

/**
 * 镜头扫描结果缓存：换头时不再重新遍历 CameraCharacteristics（切镜头慢的大头）。
 *
 * ⚠️★这里曾是「间歇性卡死」的头号成因★：【空列表也是非 null】。
 *   首次绑定时相机服务可能还没就绪，探测返回 emptyList()，
 *   旧实现照单全收 → 之后 hit() 恒为 true → 永远复用这份空缓存 →
 *   广角/长焦全调不出来，且只有杀进程才恢复（所以表现为"间歇性"）。
 *
 * 四道闸：put 拒绝空结果 / hit 要求非空 / 复用 N 次强制重扫 / markInvalid 主动作废。
 */
object LensScanCache {
    /**
     * ★连续复用多少次后强制重扫一次★
     *
     * 防止缓存内容【陈旧】：相机服务重启、ROM 动态开关副摄、
     * 外接摄像头热插拔等场景下，缓存里的 id 可能已经失效，
     * 但没有任何回调通知我们。定期重扫一次是自愈手段。
     * 取 12 次：一次切镜头复用一次，正常拍摄很难连点 12 次，
     * 而真陈旧时最多卡 12 次操作就被纠正。
     */
    private const val REUSE_LIMIT = 12

    @Volatile
    var facing: Int? = null

    @Volatile
    var lenses: List<DeviceCompatibility.LensProfile>? = null

    @Volatile
    var physical: List<DeviceCompatibility.LensProfile>? = null

    /** 已连续复用次数（put / clear 时清零） */
    @Volatile
    private var reuseCount = 0

    /**
     * ★至少要有这么多颗头，才算一次"像样的扫描"★
     *
     * 单摄机（很多入门机、平板、模拟器）确实只有 1 颗头，
     * 那也要把它记下来 —— 否则每次都重扫，白白付出几百毫秒。
     * 所以下限取 1，只挡住【完全空】的结果。
     */
    fun hit(facing: Int): Boolean {
        if (this.facing != facing) return false
        val l = lenses
        val p = physical
        if (l.isNullOrEmpty()) return false
        // physical 允许为空（单摄机没有副摄），但 lenses 必须有
        if (reuseCount >= REUSE_LIMIT) {
            Log.d("Camera", "镜头扫描缓存已复用 $reuseCount 次，本次强制重扫")
            return false
        }
        return true
    }

    /**
     * 写入缓存。
     * @return 是否真的写入（空结果会被拒绝）
     */
    fun put(
        facing: Int,
        lenses: List<DeviceCompatibility.LensProfile>,
        physical: List<DeviceCompatibility.LensProfile>
    ): Boolean {
        //
        // ★拒绝缓存空结果★
        // 空结果意味着"这次扫描没成功"（服务未就绪 / HAL 异常 / 权限未授予），
        // 而不是"这台机器没有摄像头"。缓存它会让后续的切镜头全部复用这份空数据。
        //
        if (lenses.isEmpty()) {
            Log.w("Camera", "镜头扫描结果为空，【不写入缓存】（否则会污染后续所有切镜头）")
            return false
        }
        this.facing = facing
        this.lenses = lenses
        this.physical = physical
        reuseCount = 0
        return true
    }

    /** 记一次复用（由调用方在命中缓存时调用） */
    fun noteReuse() {
        reuseCount++
    }

    /**
     * ★主动作废★
     * 绑定流程如果发现结果不对（比如扫出来是空的、物理头全绑不上），
     * 调这个让下次强制重扫。
     */
    fun markInvalid(reason: String) {
        if (facing != null || lenses != null) {
            Log.w("Camera", "镜头扫描缓存已作废（$reason），下次强制重扫")
        }
        facing = null
        lenses = null
        physical = null
        reuseCount = 0
    }

    fun clear() {
        facing = null
        lenses = null
        physical = null
        reuseCount = 0
    }
}

// ==================== 首次启动：品牌 → 系列 选择弹窗 ====================

/**
 * 首次打开 App 时弹：先选品牌，再选系列。
 * @param detected 自动识别出来的系列（会标成「识别为」并默认选中）
 */
@Composable
fun BrandSeriesPickerDialog(
    detected: SeriesProfile?,
    onConfirm: (SeriesProfile) -> Unit,
    onSkip: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var brand by remember { mutableStateOf(detected?.brand ?: BrandKey.XIAOMI) }
    var picked by remember { mutableStateOf(detected) }
    var fromDetected by remember { mutableStateOf(detected != null) }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF20D0D0D))
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF171719))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (step == 0) "选择你的手机品牌" else "选择系列",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (step == 0)
                        "Silky Camera 会按品牌加载对应的多摄适配策略"
                    else "${brand.cn} · 选一个最接近你机器的系列",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp
                )
                if (step == 1 && fromDetected && detected != null && detected.brand == brand) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "已自动识别：${detected.name}（可改）",
                        color = Color(0xFFFFA000),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (step == 0) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.heightIn(max = 420.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(BrandKey.values().toList()) { b ->
                            val sel = b == brand
                            Box(
                                modifier = Modifier
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (sel) Color(0xFFFFA000).copy(alpha = 0.22f)
                                        else Color.White.copy(alpha = 0.06f)
                                    )
                                    .border(
                                        1.dp,
                                        if (sel) Color(0xFFFFA000) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        brand = b
                                        picked = SeriesDb.byBrand(b).firstOrNull()
                                        fromDetected = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = b.cn,
                                    color = if (sel) Color(0xFFFFA000) else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SeriesDb.byBrand(brand).forEach { s ->
                            val sel = picked?.id == s.id
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (sel) Color(0xFFFFA000).copy(alpha = 0.18f)
                                        else Color.White.copy(alpha = 0.05f)
                                    )
                                    .border(
                                        1.dp,
                                        if (sel) Color(0xFFFFA000) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        picked = s
                                        fromDetected = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = s.name,
                                    color = if (sel) Color(0xFFFFA000) else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = s.summary,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                if (step == 0) onSkip() else step = 0
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (step == 0) "跳过（通用模式）" else "上一步",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (picked != null) Color(0xFFFFA000)
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .clickable {
                                if (step == 0) {
                                    if (picked != null) step = 1
                                } else {
                                    picked?.let { onConfirm(it) }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (step == 0) "下一步" else "确定",
                            color = if (picked != null) Color.Black else Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 「机型与色彩」设置弹窗。关于页 / 首次启动后都能开。
 * 里面能改：机型系列、统一镜头色彩、锁定白平衡、快切开关、镜头色彩校准。
 */
@Composable
fun DeviceTuningDialog(
    context: Context,
    current: SeriesProfile?,
    colorUnify: Boolean,
    fastSwitch: Boolean,
    lockAwb: Boolean = false,
    calibrating: Boolean = false,
    currentLensKey: String = "",
    onColorUnify: (Boolean) -> Unit,
    onFastSwitch: (Boolean) -> Unit,
    onLockAwb: (Boolean) -> Unit = {},
    onPickSeries: () -> Unit,
    onCalibrate: () -> Unit,
    onResetCalibration: () -> Unit = {},
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF20D0D0D))
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // ★整屏撑满★：这样下面的「关闭」按钮才能被 weight 顶到屏幕最底部，
            // 而不是跟着卡片内容跑（内容一多就滚出屏幕外）
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF171719))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "机型与色彩",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "当前：${SeriesDb.describe(current)}",
                        color = Color(0xFFFFA000),
                        fontSize = 13.sp
                    )
                    current?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "策略：${it.strategy.label}　光学档位：${it.opticalStops.joinToString(" / ") { s -> "${s}x" }}　可用上限：${it.maxUsefulZoom.toInt()}x",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = it.notes,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    TuningRow("机型系列", "重新选择品牌 / 系列") { onPickSeries() }
                    TuningSwitch(
                        "统一镜头色彩",
                        "成片按每个镜头的校准值套 RGB 增益，换镜头不再偏色（需逐镜头校准）",
                        colorUnify
                    ) {
                        onColorUnify(it)
                    }
                    TuningSwitch(
                        "锁定白平衡",
                        "防止拍摄中白平衡自己漂移。★注意：它不会统一色彩，开错了反而会让副摄套用主摄的白平衡★",
                        lockAwb
                    ) {
                        onLockAwb(it)
                    }
                    TuningSwitch("快速切镜头", "复用镜头扫描结果、缩短收敛等待（关闭则更保守更稳）", fastSwitch) {
                        onFastSwitch(it)
                    }

                    // ★校准状态★：显示当前镜头校准了没，并实时反映「校准中」
                    val curKey = currentLensKey.ifEmpty { "Z10" }
                    Text(
                        text = if (colorUnify) {
                            LensColorUnifier.statusLine(curKey)
                        } else "统一镜头色彩已关闭",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TuningRow(
                        title = if (calibrating) "校准中…" else "校准当前镜头",
                        sub = if (calibrating)
                            "正在取样，最多 10 秒"
                        else "对准白纸 / 灰卡拍一次，自动记下这颗头的偏色；每个镜头各做一次",
                        onClick = { if (!calibrating) onCalibrate() }
                    )
                    TuningRow("清除校准数据", "镜头全部恢复未校准状态（换机 / 校准错了时用）") {
                        onResetCalibration()
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(56.dp))
                // ★「关闭」固定在屏幕底部★：不随卡片内容滚动。
                // 上方 56dp + 下方 40dp + 外层 Box 的 24dp 内边距，
                // 按钮整体离屏幕底边约 64dp —— 单手拇指自然落点，不贴边。
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF171719))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("关闭", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun TuningRow(title: String, sub: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(3.dp))
        Text(text = sub, color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
    }
}

@Composable
private fun TuningSwitch(
    title: String,
    sub: String,
    checked: Boolean,
    onChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onChanged(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = sub, color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp, lineHeight = 15.sp)
        }
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 26.dp)
                .clip(CircleShape)
                .background(if (checked) Color(0xFFFFA000) else Color.White.copy(alpha = 0.18f))
                .clickable { onChanged(!checked) },
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}


// ==================== 星空 / 星轨模式 ====================
//
// 机内堆栈：拍 N 张 → 边拍边堆 → 智能降噪 + 智能提亮 → 一次导出三样东西
//   ① 星轨长曝光合成图（JPEG）
//   ② 星轨动态照片（GIF，逐帧展示星轨一点点长出来的过程）
//   ③ 星轨视频（MP4，MediaCodec 硬编码）
//
// 设计要点：
//   · 降噪走「多帧平均底图 + 边缘感知空域降噪」：随机噪声按 √N 下降，星点不会被抹掉
//   · 星轨走「最大值混合（Lighten）」：这是星轨的标准算法，星点划过的轨迹会自然连成线
//   · 提亮走「自适应 Gamma + S 曲线」：按画面实际平均亮度算 gamma，暗部抬起、高光不爆

/** 星空 / 星轨的拍摄参数 */
data class StarTrailConfig(
    /** 单帧曝光（秒）。实际下发时会按 HAL 的 SENSOR_INFO_EXPOSURE_TIME_RANGE 再夹一次 */
    val exposureSec: Float = 8f,
    /** 单帧 ISO，0 = 自动 */
    val iso: Int = 1600,
    /** 堆栈张数 */
    val frames: Int = 60,
    /** 帧间隔（ms），给写卡和传感器留时间 */
    val intervalMs: Long = 400L,
    /** 智能降噪强度 0~100 */
    val denoise: Int = 55,
    /** 智能提亮强度 0~100 */
    val brighten: Int = 60,
    /** true = 星轨（最大值混合）；false = 纯堆栈降噪（多帧平均，星点会拉成点不是线） */
    val trailMode: Boolean = true,
    /** 导出 MP4 星轨视频 */
    val makeVideo: Boolean = true,
    /** 导出 GIF 动态照片 */
    val makeMotionPhoto: Boolean = true,
    /** 拍之前把对焦锁到无穷远（星空必须） */
    val focusInfinity: Boolean = true,
    /** 关闭机内降噪 / 锐化（长曝光下机内降噪会吃掉星点） */
    val rawPipeline: Boolean = true
) {
    fun exposureNs(): Long = (exposureSec * 1_000_000_000L).toLong().coerceAtLeast(1_000_000L)
}

/** 星空参数的持久化 */
object StarTrailStore {
    private const val PREFS = "silky_star_trail"
    private const val K_EXP = "exp"
    private const val K_ISO = "iso"
    private const val K_FRAMES = "frames"
    private const val K_INTERVAL = "interval"
    private const val K_DENOISE = "denoise"
    private const val K_BRIGHTEN = "brighten"
    private const val K_TRAIL = "trail"
    private const val K_VIDEO = "video"
    private const val K_GIF = "gif"
    private const val K_FOCUS = "focus"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): StarTrailConfig {
        val p = prefs(context)
        if (!p.contains(K_FRAMES)) return ActiveDevice.starTrail().let {
            StarTrailConfig(
                exposureSec = it.exposureSec, iso = it.iso, frames = it.frames,
                intervalMs = it.intervalMs, denoise = it.denoise, brighten = it.brighten
            )
        }
        return StarTrailConfig(
            exposureSec = p.getFloat(K_EXP, 8f),
            iso = p.getInt(K_ISO, 1600),
            frames = p.getInt(K_FRAMES, 60),
            intervalMs = p.getLong(K_INTERVAL, 400L),
            denoise = p.getInt(K_DENOISE, 55),
            brighten = p.getInt(K_BRIGHTEN, 60),
            trailMode = p.getBoolean(K_TRAIL, true),
            makeVideo = p.getBoolean(K_VIDEO, true),
            makeMotionPhoto = p.getBoolean(K_GIF, true),
            focusInfinity = p.getBoolean(K_FOCUS, true)
        )
    }

    fun save(context: Context, c: StarTrailConfig) {
        prefs(context).edit()
            .putFloat(K_EXP, c.exposureSec)
            .putInt(K_ISO, c.iso)
            .putInt(K_FRAMES, c.frames)
            .putLong(K_INTERVAL, c.intervalMs)
            .putInt(K_DENOISE, c.denoise)
            .putInt(K_BRIGHTEN, c.brighten)
            .putBoolean(K_TRAIL, c.trailMode)
            .putBoolean(K_VIDEO, c.makeVideo)
            .putBoolean(K_GIF, c.makeMotionPhoto)
            .putBoolean(K_FOCUS, c.focusInfinity)
            .apply()
    }
}

/** 星轨合成的结果 */
data class StarTrailResult(
    val jpegUri: Uri?,
    val videoUri: Uri?,
    val gifUri: Uri?,
    val frames: Int,
    val costMs: Long,
    val stopped: Boolean
)

/** 图像处理：降噪 / 提亮 / 混合 */
object StarTrailImaging {

    /** 缩放：把长边压到 maxEdge，省内存也省时间 */
    fun fit(bmp: android.graphics.Bitmap, maxEdge: Int): android.graphics.Bitmap {
        val w = bmp.width
        val h = bmp.height
        val long = maxOf(w, h)
        if (long <= maxEdge) return bmp
        val k = maxEdge.toFloat() / long
        val nw = (w * k).roundToInt().coerceAtLeast(2)
        val nh = (h * k).roundToInt().coerceAtLeast(2)
        return android.graphics.Bitmap.createScaledBitmap(bmp, nw, nh, true)
    }

    /**
     * 智能降噪：边缘感知的空域降噪。
     *
     * 做法：先把图缩到 1/4 再放大回来，得到一个「低频版本」；
     * 然后逐像素比较原图与低频版的差异 ——
     *   · 差异小 = 平坦区（天空、暗部）→ 多掺低频，噪声被压下去
     *   · 差异大 = 星点 / 地景边缘 → 少掺甚至不掺，细节保住
     *
     * @param strength 0~1
     */
    fun smartDenoise(src: android.graphics.Bitmap, strength: Float): android.graphics.Bitmap {
        val k = strength.coerceIn(0f, 1f)
        if (k < 0.02f) return src
        val w = src.width
        val h = src.height
        val sw = (w / 4).coerceAtLeast(1)
        val sh = (h / 4).coerceAtLeast(1)
        val small = android.graphics.Bitmap.createScaledBitmap(src, sw, sh, true)
        val blur = android.graphics.Bitmap.createScaledBitmap(small, w, h, true)

        val n = w * h
        val a = IntArray(n)
        val b = IntArray(n)
        src.getPixels(a, 0, w, 0, 0, w, h)
        blur.getPixels(b, 0, w, 0, 0, w, h)

        for (i in 0 until n) {
            val ca = a[i]
            val cb = b[i]
            val ra = (ca shr 16) and 0xFF
            val ga = (ca shr 8) and 0xFF
            val ba = ca and 0xFF
            val rb = (cb shr 16) and 0xFF
            val gb = (cb shr 8) and 0xFF
            val bb = cb and 0xFF
            val la = (ra * 77 + ga * 150 + ba * 29) shr 8
            val lb = (rb * 77 + gb * 150 + bb * 29) shr 8
            val diff = abs(la - lb)
            // 平坦区权重接近 1，边缘 / 星点权重迅速掉到 0
            val wgt = (k * (1f - diff / 34f)).coerceIn(0f, 1f)
            if (wgt <= 0.002f) continue
            val iw = ((1f - wgt) * 256f).toInt()
            val wb = (wgt * 256f).toInt()
            val rr = (ra * iw + rb * wb) shr 8
            val gg = (ga * iw + gb * wb) shr 8
            val bbb = (ba * iw + bb * wb) shr 8
            a[i] = 0xFF000000.toInt() or (rr shl 16) or (gg shl 8) or bbb
        }
        val out = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        out.setPixels(a, 0, w, 0, 0, w, h)
        small.recycle()
        blur.recycle()
        return out
    }

    /**
     * 智能提亮：自适应 Gamma + 高光 S 曲线 + 黑场保护。
     *
     * gamma 由「画面实际平均亮度」反推出来：
     *   拍出来平均亮度只有 12 → target 46 → gamma ≈ 1.9（暗部大幅抬起）
     *   拍出来已经很亮      → gamma 接近 1（几乎不动）
     * 所以不管现场多暗、参数怎么设，合成出来都是差不多的观感。
     *
     * @param strength 0~1，控制「算出来的 gamma 兑现多少」
     */
    fun smartBrighten(src: android.graphics.Bitmap, strength: Float, targetMean: Float = 46f): android.graphics.Bitmap {
        val k = strength.coerceIn(0f, 1f)
        if (k < 0.02f) return src
        val w = src.width
        val h = src.height
        val n = w * h
        val px = IntArray(n)
        src.getPixels(px, 0, w, 0, 0, w, h)

        // 抽样统计平均亮度（每 5 个像素抽 1 个，够准又快）
        var sum = 0.0
        var cnt = 0
        var i = 0
        while (i < n) {
            val c = px[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            sum += (r * 77 + g * 150 + b * 29) / 256.0
            cnt++
            i += 5
        }
        val mean = (sum / cnt.coerceAtLeast(1)).coerceAtLeast(1.0)

        // 反解 gamma：255 * (mean/255)^(1/gamma) = target
        val gRaw = ln(mean / 255.0) / ln(targetMean.coerceAtLeast(1f) / 255.0)
        val gamma = (1f + (gRaw.coerceIn(1.0, 3.6) - 1.0) * k.toDouble()).toFloat()
            .coerceIn(1f, 3.6f)
        val inv = 1f / gamma

        // 256 级查找表，逐像素只查表，不重复算 pow
        val lut = IntArray(256)
        for (v in 0..255) {
            val x = v / 255f
            var y = 255f * exp(ln(x.coerceAtLeast(0.0008f)) * inv)
            // 高光 S 曲线：235 以上压缩，避免星轨核心糊成一片白
            if (y > 235f) y = 235f + (y - 235f) * 0.55f
            // 黑场保护：极暗处轻微抬起一点点就够，不然噪点也会跟着起来
            if (v <= 6) y = y * 0.35f
            lut[v] = y.coerceIn(0f, 255f).roundToInt()
        }

        for (j in 0 until n) {
            val c = px[j]
            val r = lut[(c shr 16) and 0xFF]
            val g = lut[(c shr 8) and 0xFF]
            val b = lut[c and 0xFF]
            px[j] = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
        }
        val out = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        out.setPixels(px, 0, w, 0, 0, w, h)
        return out
    }

    /** 最大值混合（Lighten）：星轨的标准叠加方式 */
    fun lightenInto(dst: android.graphics.Bitmap, src: android.graphics.Bitmap) {
        val p = android.graphics.Paint().apply {
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.LIGHTEN)
        }
        android.graphics.Canvas(dst).drawBitmap(src, 0f, 0f, p)
    }
}

/** 机内堆栈器：多帧平均底图（降噪用）+ 最大值累积图（星轨用） */
class StarTrailStacker(private val w: Int, private val h: Int) {

    private val trail: android.graphics.Bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    private val trailCanvas = android.graphics.Canvas(trail)
    private val lighten = android.graphics.Paint().apply {
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.LIGHTEN)
    }
    private val sumR = IntArray(w * h)
    private val sumG = IntArray(w * h)
    private val sumB = IntArray(w * h)
    private val buf = IntArray(w * h)
    private var count = 0

    init {
        // 必须是【不透明黑】：PorterDuff.LIGHTEN 在透明底上不生效
        trail.eraseColor(android.graphics.Color.BLACK)
    }

    val frameCount: Int get() = count

    fun add(frame: android.graphics.Bitmap) {
        val fw = frame.width
        val fh = frame.height
        val src = if (fw == w && fh == h) frame
        else android.graphics.Bitmap.createScaledBitmap(frame, w, h, true)

        src.getPixels(buf, 0, w, 0, 0, w, h)
        for (i in buf.indices) {
            val c = buf[i]
            sumR[i] += (c shr 16) and 0xFF
            sumG[i] += (c shr 8) and 0xFF
            sumB[i] += c and 0xFF
        }
        count++
        trailCanvas.drawBitmap(src, 0f, 0f, lighten)
        if (src !== frame) src.recycle()
    }

    /** 多帧平均底图：噪声按 √N 下降 */
    fun skyBase(): android.graphics.Bitmap {
        val n = count.coerceAtLeast(1)
        val out = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val px = IntArray(w * h)
        for (i in px.indices) {
            val r = sumR[i] / n
            val g = sumG[i] / n
            val b = sumB[i] / n
            px[i] = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
        }
        out.setPixels(px, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * 合成当前进度预览：
     *   平均底图 → 智能降噪 → 智能提亮 → 叠上星轨（最大值混合）
     */
    fun composite(denoise: Float, brighten: Float, trailMode: Boolean): android.graphics.Bitmap {
        val base = StarTrailImaging.smartDenoise(skyBase(), denoise)
        val lifted = StarTrailImaging.smartBrighten(base, brighten)
        if (!trailMode) {
            base.recycle()
            return lifted
        }
        val out = lifted.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
        android.graphics.Canvas(out).drawBitmap(trail, 0f, 0f, lighten)
        base.recycle()
        lifted.recycle()
        return out
    }

    /** 最终成片：合成后再做一次轻微提亮，让星轨更跳 */
    fun finalImage(denoise: Float, brighten: Float, trailMode: Boolean): android.graphics.Bitmap {
        val c = composite(denoise, brighten, trailMode)
        val out = StarTrailImaging.smartBrighten(c, (brighten * 0.4f).coerceIn(0f, 1f))
        if (out !== c) c.recycle()
        return out
    }

    fun release() {
        trail.recycle()
    }
}

/** MP4 编码器：MediaCodec 硬编码 + MediaMuxer 封包，帧从 input surface 画进去 */
class StarTrailVideoEncoder(
    width: Int,
    height: Int,
    private val fps: Int,
    private val pfd: android.os.ParcelFileDescriptor
) {
    private val w = width - (width % 2)
    private val h = height - (height % 2)
    private val codec: android.media.MediaCodec
    private val surface: android.view.Surface
    private val muxer: android.media.MediaMuxer
    private var track = -1
    private var started = false
    private var outFrames = 0L
    private val paint = android.graphics.Paint().apply { isFilterBitmap = true }

    init {
        val fmt = android.media.MediaFormat.createVideoFormat(
            android.media.MediaFormat.MIMETYPE_VIDEO_AVC, w, h
        ).apply {
            setInteger(
                android.media.MediaFormat.KEY_COLOR_FORMAT,
                android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            setInteger(android.media.MediaFormat.KEY_BIT_RATE, 8_000_000)
            setInteger(android.media.MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(android.media.MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        codec = android.media.MediaCodec.createEncoderByType(
            android.media.MediaFormat.MIMETYPE_VIDEO_AVC
        )
        codec.configure(fmt, null, null, android.media.MediaCodec.CONFIGURE_FLAG_ENCODE)
        surface = codec.createInputSurface()
        codec.start()
        muxer = android.media.MediaMuxer(
            pfd.fileDescriptor,
            android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        )
    }

    fun encodeFrame(bmp: android.graphics.Bitmap) {
        val c = surface.lockCanvas(android.graphics.Rect(0, 0, w, h))
        c.drawBitmap(bmp, null, android.graphics.Rect(0, 0, w, h), paint)
        surface.unlockCanvasAndPost(c)
        drain()
    }

    private fun drain(end: Boolean = false) {
        if (end) {
            try {
                codec.signalEndOfInputStream()
            } catch (e: Exception) {
                Log.w("StarTrail", "signalEndOfInputStream 失败", e)
            }
        }
        val bi = android.media.MediaCodec.BufferInfo()
        while (true) {
            val idx = codec.dequeueOutputBuffer(bi, 10_000)
            if (idx == android.media.MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!end) break
                // 结束时等到 EOS 为止，但最多再等 10 次，避免死循环
                if (bi.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                continue
            }
            if (idx == android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                track = muxer.addTrack(codec.outputFormat)
                muxer.start()
                started = true
                continue
            }
            if (idx >= 0) {
                val buf = codec.getOutputBuffer(idx)
                if (started && buf != null &&
                    (bi.flags and android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                ) {
                    // ★重写时间戳★：surface 输入的时间戳是墙钟时间（几分钟），
                    //   直接封包会得到一段「几分钟长、画面不动」的视频，
                    //   这里改成均匀帧间隔，播放出来才是正常速度的星轨生长过程。
                    bi.presentationTimeUs = outFrames * 1_000_000L / fps
                    outFrames++
                    muxer.writeSampleData(track, buf, bi)
                }
                codec.releaseOutputBuffer(idx, false)
                if (bi.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
    }

    fun finish() {
        try {
            drain(true)
        } catch (e: Exception) {
            Log.w("StarTrail", "编码收尾失败", e)
        }
        try {
            codec.stop()
        } catch (e: Exception) {
        }
        try {
            codec.release()
        } catch (e: Exception) {
        }
        try {
            if (started) {
                muxer.stop()
            }
        } catch (e: Exception) {
            Log.w("StarTrail", "MediaMuxer stop 失败", e)
        }
        try {
            muxer.release()
        } catch (e: Exception) {
        }
        try {
            surface.release()
        } catch (e: Exception) {
        }
    }
}

/**
 * 纯 Kotlin 的 GIF89a 编码器（含 LZW 压缩）。
 * 用来导出「星轨动态照片」：一帧一帧展示星轨怎么长出来的。
 */
object Gif89AEncoder {

    private fun shortLE(out: java.io.OutputStream, v: Int) {
        out.write(v and 0xFF)
        out.write((v shr 8) and 0xFF)
    }

    /**
     * ★GIF LZW 的位深★
     *
     * ⚠️ 这是之前「系统相册打不开 .gif」的根因：
     *    调色板是 256 色（索引 0~255，需要 8 bit），
     *    但下面写 LZW 数据时用的却是 minCodeSize = 3（只够 8 色）。
     *    解码器按 3 bit 读第一批码字，而数据是按 4 bit 写的 —— 相位立刻错位，
     *    整个 GIF 被判为损坏，系统相册自然打不开（或部分打开一片花屏）。
     *    位深必须 >= log2(调色板颜色数)，256 色就必须是 8。
     */
    private const val GIF_BITS = 8

    /** 216 色 + 40 级灰度 = 256 色全局调色板 */
    private val palette: IntArray = IntArray(256).also { pal ->
        var i = 0
        for (r in 0..5) for (g in 0..5) for (b in 0..5) {
            pal[i++] = 0xFF000000.toInt() or ((r * 51) shl 16) or ((g * 51) shl 8) or (b * 51)
        }
        for (k in 0 until 40) {
            val v = (k * 255 / 39)
            pal[216 + k] = 0xFF000000.toInt() or (v shl 16) or (v shl 8) or v
        }
    }

    fun quantize(bmp: android.graphics.Bitmap): ByteArray {
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val out = ByteArray(w * h)
        for (i in px.indices) {
            val c = px[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val mx = maxOf(r, g, b)
            val mn = minOf(r, g, b)
            out[i] = if (mx - mn <= 12) {
                // 接近中性灰 → 走灰度斜坡，暗部过渡更细腻
                (216 + (mx * 39 / 255)).toByte()
            } else {
                val ri = (r * 5 / 255).coerceIn(0, 5)
                val gi = (g * 5 / 255).coerceIn(0, 5)
                val bi = (b * 5 / 255).coerceIn(0, 5)
                (ri * 36 + gi * 6 + bi).toByte()
            }
        }
        return out
    }

    /**
     * 直接写【已量化成调色板索引】的帧。
     *
     * ★为什么推荐这条路径★：一帧只占 w*h 字节（1280×720 ≈ 921KB），
     *   而存 ARGB8888 Bitmap 是 4 倍（≈3.7MB）。
     *   同一个内存预算下能多存 4 倍的帧数 —— 动图帧率直接受益。
     */
    fun writeIndices(
        out: java.io.OutputStream,
        frames: List<ByteArray>,
        w: Int,
        h: Int,
        delayMs: Int
    ) {
        if (frames.isEmpty() || w <= 0 || h <= 0) return
        writeHeader(out, w, h)
        val delay = (delayMs / 10).coerceIn(2, 65535)
        for (idx in frames) {
            writeFrame(out, idx, w, h, delay)
        }
        out.write(0x3B)
    }

    /** 兼容路径：传 Bitmap 进来，内部逐帧量化（内存占用比 writeIndices 高 4 倍） */
    fun write(out: java.io.OutputStream, frames: List<android.graphics.Bitmap>, delayMs: Int) {
        val first = frames.firstOrNull() ?: return
        val w = first.width
        val h = first.height
        writeHeader(out, w, h)
        val delay = (delayMs / 10).coerceIn(2, 65535)
        for (f in frames) {
            val bmp = if (f.width == w && f.height == h) f
            else android.graphics.Bitmap.createScaledBitmap(f, w, h, true)
            writeFrame(out, quantize(bmp), w, h, delay)
            if (bmp !== f) bmp.recycle()
        }
        out.write(0x3B)
    }

    /** GIF 文件头：签名 + 屏幕尺寸 + 全局色表 + 循环扩展 */
    private fun writeHeader(out: java.io.OutputStream, w: Int, h: Int) {
        out.write("GIF89a".toByteArray(charset("US-ASCII")))
        shortLE(out, w)
        shortLE(out, h)
        out.write(0xF7) // 全局色表 + 256 色
        out.write(0)
        out.write(0)
        for (c in palette) {
            out.write((c shr 16) and 0xFF)
            out.write((c shr 8) and 0xFF)
            out.write(c and 0xFF)
        }
        // NETSCAPE2.0 循环扩展：让动图无限循环播放
        out.write(0x21)
        out.write(0xFF)
        out.write(11)
        out.write("NETSCAPE2.0".toByteArray(charset("US-ASCII")))
        out.write(3)
        out.write(1)
        shortLE(out, 0)
        out.write(0)
    }

    /** 写一帧：图形控制扩展 + 图像描述符 + LZW 数据 */
    private fun writeFrame(
        out: java.io.OutputStream,
        idx: ByteArray,
        w: Int,
        h: Int,
        delay: Int
    ) {
        // 图形控制扩展：不处置（disposal = 1）
        out.write(0x21)
        out.write(0xF9)
        out.write(4)
        out.write(0x04)
        shortLE(out, delay)
        out.write(0)
        out.write(0)
        // 图像描述符
        out.write(0x2C)
        shortLE(out, 0)
        shortLE(out, 0)
        shortLE(out, w)
        shortLE(out, h)
        out.write(0)
        // LZW：位深必须跟调色板颜色数匹配（256 色 → 8）
        out.write(GIF_BITS)
        lzw(out, idx, GIF_BITS)
        out.write(0)
    }

    /**
     * GIF LZW 压缩。整个重写是因为上一版有两个致命问题：
     * ① 位深传 3 但调色板是 256 色（打不开的主因，见 GIF_BITS）
     * ② 码长增长时机写成 `nextCode == (1 shl codeSize) + 1`，比正确时机晚一个码，
     *    编解码器位宽不同步，从第 17 个码起数据就错位了。
     * 现在按 GIF 标准：位宽在 freeEnt 超过 maxcode 时增长，字典满（4096）时输出清除码重建。
     * @param minCodeSize 位深（本项目固定 8，对应 256 色调色板）
     */
    private fun lzw(out: java.io.OutputStream, data: ByteArray, minCodeSize: Int) {
        val initBits = minCodeSize + 1            // GIF 惯例：初始码长 = 位深 + 1
        val maxBits = 12
        val maxMaxCode = 1 shl maxBits            // 4096
        val clearCode = 1 shl minCodeSize         // 256（位深 8）
        val eofCode = clearCode + 1               // 257

        var nBits = initBits                      // 9
        var maxCode = (1 shl nBits) - 1           // 511
        var freeEnt = clearCode + 2               // 258
        var clearFlg = false

        // 字典：key = (c shl 12) + prefix，c<=255、prefix<4096，不会碰撞
        val dict = HashMap<Int, Int>(8192)
        fun keyOf(prefix: Int, c: Int): Int = (c shl maxBits) + prefix

        var curAccum = 0
        var curBits = 0
        val block = ArrayList<Byte>(256)

        fun flushBlock(force: Boolean) {
            if (block.isEmpty()) return
            if (block.size >= 255 || force) {
                out.write(block.size)
                for (b in block) out.write(b.toInt() and 0xFF)
                block.clear()
            }
        }

        fun emitByte(v: Int) {
            block.add((v and 0xFF).toByte())
            flushBlock(false)
        }

        /** 输出一个码字。位宽增长与清字典都在这里收口，保证编解码同步 */
        fun output(code: Int) {
            curAccum = curAccum or (code shl curBits)
            curBits += nBits
            while (curBits >= 8) {
                emitByte(curAccum and 0xFF)
                curAccum = curAccum shr 8
                curBits -= 8
            }
            if (freeEnt > maxCode || clearFlg) {
                if (clearFlg) {
                    maxCode = (1 shl initBits) - 1
                    nBits = initBits
                    clearFlg = false
                } else {
                    nBits++
                    maxCode = if (nBits == maxBits) maxMaxCode else (1 shl nBits) - 1
                }
            }
        }

        /** 字典满 → 输出清除码重建 */
        fun clBlock() {
            dict.clear()
            freeEnt = clearCode + 2
            clearFlg = true
            output(clearCode)
        }

        if (data.isEmpty()) return
        output(clearCode)
        var ent = data[0].toInt() and 0xFF
        var i = 1
        while (i < data.size) {
            val c = data[i].toInt() and 0xFF
            val key = keyOf(ent, c)
            val hit = dict[key]
            if (hit != null) {
                ent = hit
            } else {
                output(ent)
                if (freeEnt < maxMaxCode) {
                    dict[key] = freeEnt
                    freeEnt++
                } else {
                    clBlock()
                }
                ent = c
            }
            i++
        }
        output(ent)
        output(eofCode)
        // 冲刷残留位 + 收尾块
        if (curBits > 0) {
            emitByte(curAccum and 0xFF)
        }
        flushBlock(true)
    }
}

// ---------------- 星空拍摄：Camera2 手动曝光 ----------------

/**
 * 星空预览参数：进星空模式时给预览套一个「中等曝光」，方便对构图。
 * 不能直接下 8 秒 —— 预览会卡成幻灯片，根本没法取景。
 */
@OptIn(ExperimentalCamera2Interop::class)
fun applyStarTrailPreviewParams(cam: Camera, cfg: StarTrailConfig) {
    try {
        val info = Camera2CameraInfo.from(cam.cameraInfo)
        val range: Range<Long>? =
            info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val maxNs = range?.upper ?: 1_000_000_000L
        val want = cfg.exposureNs()
        val previewNs = minOf(want, minOf(maxNs, 500_000_000L)) // 预览最多 0.5s
        val b = CaptureRequestOptions.Builder()
        b.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
        b.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, previewNs)
        b.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, cfg.iso.coerceAtLeast(100))
        b.setCaptureRequestOption(
            CaptureRequest.SENSOR_FRAME_DURATION,
            maxOf(previewNs, 33_000_000L)
        )
        if (cfg.rawPipeline) {
            b.setCaptureRequestOption(
                CaptureRequest.NOISE_REDUCTION_MODE,
                CameraMetadata.NOISE_REDUCTION_MODE_MINIMAL
            )
            b.setCaptureRequestOption(CaptureRequest.EDGE_MODE, CameraMetadata.EDGE_MODE_OFF)
        }
        if (cfg.focusInfinity) {
            b.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            b.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, 0f)
        }
        val ctrl = Camera2CameraControl.from(cam.cameraControl)
        ctrl.clearCaptureRequestOptions()
        ctrl.setCaptureRequestOptions(b.build())
        Log.d("StarTrail", "预览曝光 ${previewNs / 1_000_000L}ms / ISO ${cfg.iso}")
    } catch (e: Exception) {
        Log.w("StarTrail", "下发星空预览参数失败（不影响拍摄）", e)
    }
}

/**
 * 星空拍摄参数：真正拍堆栈帧之前下发。
 * @return 实际生效的 (曝光ns, ISO)
 */
@OptIn(ExperimentalCamera2Interop::class)
fun applyStarTrailCaptureParams(cam: Camera, cfg: StarTrailConfig): Pair<Long, Int> {
    val info = Camera2CameraInfo.from(cam.cameraInfo)
    val expRange: Range<Long>? =
        info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
    val isoRange: Range<Int>? =
        info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
    val maxNs = expRange?.upper ?: 1_000_000_000L
    val minNs = expRange?.lower ?: 1_000_000L
    val expNs = cfg.exposureNs().coerceIn(minNs, maxNs)
    val iso = cfg.iso.coerceIn(
        isoRange?.lower ?: 100,
        isoRange?.upper ?: 6400
    )
    val b = CaptureRequestOptions.Builder()
    b.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)
    b.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, expNs)
    b.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
    b.setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, maxOf(expNs, 33_000_000L))
    if (cfg.rawPipeline) {
        // 长曝光下机内降噪会「擦」掉暗弱星点，也会让每张耗时翻倍
        b.setCaptureRequestOption(
            CaptureRequest.NOISE_REDUCTION_MODE,
            CameraMetadata.NOISE_REDUCTION_MODE_OFF
        )
        b.setCaptureRequestOption(CaptureRequest.EDGE_MODE, CameraMetadata.EDGE_MODE_OFF)
    }
    if (cfg.focusInfinity) {
        b.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
        b.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, 0f)
    }
    // 上架必须关防抖：三脚架上 OIS 会自己找位移，星点被拉成小圈
    b.setCaptureRequestOption(
        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
        CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
    )
    b.setCaptureRequestOption(
        CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
        CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
    )
    val ctrl = Camera2CameraControl.from(cam.cameraControl)
    ctrl.clearCaptureRequestOptions()
    ctrl.setCaptureRequestOptions(b.build())
    Log.d("StarTrail", "拍摄曝光 ${expNs / 1_000_000L}ms / ISO $iso（HAL 上限 ${maxNs / 1_000_000L}ms）")
    return expNs to iso
}

/** 拍完星轨把手动参数撤掉，回到常规场景参数 */
@OptIn(ExperimentalCamera2Interop::class)
fun clearStarTrailParams(cam: Camera?) {
    try {
        cam ?: return
        Camera2CameraControl.from(cam.cameraControl).clearCaptureRequestOptions()
    } catch (e: Exception) {
        Log.w("StarTrail", "恢复常规参数失败", e)
    }
}

/** ImageProxy → Bitmap。YUV_420_888 走 YuvImage（native，快），JPEG 直接解 */
fun imageProxyToBitmap(image: ImageProxy): android.graphics.Bitmap? {
    return try {
        if (image.format == ImageFormat.JPEG) {
            val buf = image.planes[0].buffer
            val bytes = ByteArray(buf.remaining())
            buf.get(bytes)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } else {
            val nv21 = yuv420ToNv21(image)
            val yuv = android.graphics.YuvImage(
                nv21, ImageFormat.NV21, image.width, image.height, null
            )
            val bos = java.io.ByteArrayOutputStream()
            yuv.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 95, bos)
            val arr = bos.toByteArray()
            android.graphics.BitmapFactory.decodeByteArray(arr, 0, arr.size)
        }
    } catch (e: Exception) {
        Log.w("StarTrail", "帧转换失败", e)
        null
    }
}

private fun yuv420ToNv21(image: ImageProxy): ByteArray {
    val w = image.width
    val h = image.height
    val y = image.planes[0]
    val u = image.planes[1]
    val v = image.planes[2]
    val out = ByteArray(w * h * 3 / 2)
    // Y
    val yBuf = y.buffer
    val yRowStride = y.rowStride
    val yPixelStride = y.pixelStride
    var pos = 0
    if (yPixelStride == 1 && yRowStride == w) {
        yBuf.get(out, 0, w * h)
        pos = w * h
    } else {
        val row = ByteArray(yRowStride)
        for (r in 0 until h) {
            yBuf.position(r * yRowStride)
            yBuf.get(row, 0, minOf(yRowStride, yBuf.remaining()))
            var c = 0
            for (ci in 0 until w) {
                out[pos++] = row[c]
                c += yPixelStride
            }
        }
    }
    // VU 交错（NV21）
    val uBuf = u.buffer
    val vBuf = v.buffer
    val uRowStride = u.rowStride
    val vRowStride = v.rowStride
    val uPixelStride = u.pixelStride
    val vPixelStride = v.pixelStride
    val chromaH = h / 2
    val chromaW = w / 2
    val uRow = ByteArray(uRowStride)
    val vRow = ByteArray(vRowStride)
    for (r in 0 until chromaH) {
        uBuf.position(r * uRowStride)
        uBuf.get(uRow, 0, minOf(uRowStride, uBuf.remaining()))
        vBuf.position(r * vRowStride)
        vBuf.get(vRow, 0, minOf(vRowStride, vBuf.remaining()))
        var cu = 0
        var cv = 0
        for (ci in 0 until chromaW) {
            out[pos++] = vRow[cv]
            out[pos++] = uRow[cu]
            cu += uPixelStride
            cv += vPixelStride
        }
    }
    return out
}

/** 存一张 Bitmap 到相册（DCIM/Silky Camera） */
fun saveBitmapToGallery(
    context: Context,
    bmp: android.graphics.Bitmap,
    name: String,
    mime: String
): Uri? {
    return try {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DCIM + "/Silky Camera"
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        ) ?: return null
        context.contentResolver.openOutputStream(uri, "w")?.use { os ->
            val fmt = if (mime == "image/png") android.graphics.Bitmap.CompressFormat.PNG
            else android.graphics.Bitmap.CompressFormat.JPEG
            bmp.compress(fmt, 95, os)
        }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
        uri
    } catch (e: Exception) {
        Log.w("StarTrail", "保存图片失败", e)
        null
    }
}

/** 在相册里建一个视频文件，返回 (Uri, 可写 fd) 给 MediaMuxer 用 */
fun createGalleryVideoFile(
    context: Context,
    name: String
): Pair<Uri, android.os.ParcelFileDescriptor>? {
    return try {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DCIM + "/Silky Camera"
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values
        ) ?: return null
        val pfd = context.contentResolver.openFileDescriptor(uri, "rw") ?: return null
        uri to pfd
    } catch (e: Exception) {
        Log.w("StarTrail", "创建视频文件失败", e)
        null
    }
}

fun closeGalleryVideoFile(context: Context, uri: Uri, pfd: android.os.ParcelFileDescriptor) {
    try {
        pfd.close()
    } catch (e: Exception) {
    }
    try {
        val v = ContentValues()
        v.put(MediaStore.MediaColumns.IS_PENDING, 0)
        context.contentResolver.update(uri, v, null, null)
    } catch (e: Exception) {
    }
}

/** 拍一帧到内存（不落盘，堆栈用） */
suspend fun captureFrameBitmap(
    context: Context,
    imageCapture: ImageCapture
): android.graphics.Bitmap? = kotlinx.coroutines.withTimeoutOrNull(60_000L) {
    kotlinx.coroutines.suspendCancellableCoroutine<android.graphics.Bitmap?> { cont ->
        try {
            imageCapture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        try {
                            var bmp = imageProxyToBitmap(image)
                            val rot = image.imageInfo.rotationDegrees
                            if (bmp != null && rot != 0) {
                                val m = android.graphics.Matrix().apply { postRotate(rot.toFloat()) }
                                bmp = android.graphics.Bitmap.createBitmap(
                                    bmp, 0, 0, bmp.width, bmp.height, m, true
                                )
                            }
                            cont.resumeWith(Result.success(bmp))
                        } catch (e: Exception) {
                            cont.resumeWith(Result.success(null))
                        } finally {
                            image.close()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.w("StarTrail", "单帧拍摄失败", exception)
                        if (cont.isActive) cont.resumeWith(Result.success(null))
                    }
                }
            )
        } catch (e: Exception) {
            if (cont.isActive) cont.resumeWith(Result.success(null))
        }
    }
}

/** 一次完整的星轨拍摄任务 */
class StarTrailSession(
    private val context: Context,
    private val imageCapture: ImageCapture,
    private val camera: Camera,
    private val config: StarTrailConfig
) {
    /** 工作分辨率：长边压到 1280，堆栈 + 编码都在这个尺寸上跑 */
    private val workEdge = 1280
    /**
     * ★动图帧尺寸★：直接跟工作分辨率一致（1280），不再二次缩水。
     *
     * 以前是 480，是为了省内存 —— 但代价是动图明显比合成图糊一大截。
     * 现在改成按需控制内存：【限制的是帧数，不是分辨率】
     *   · 帧数上限 24 帧（下面 gifSampleEvery 控制采样间隔）
     *   · 1280×720 ARGB8888 一帧 ≈ 3.7MB，24 帧 ≈ 88MB 峰值
     *   · 而且 GIF 写完之后会立刻回收，不会一直挂着
     * 低端机如果还是吃紧，把 workEdge 一起调小即可（合成图和动图会同步变小）。
     */
    private val gifEdge = 1280
    /**
     * 动图最多保留多少帧：既是体积上限，也是内存上限。
     * 40 帧 × 1280×720 索引帧 ≈ 37MB（若存 Bitmap 则要 147MB，手机上撑不住）
     */
    private val gifMaxFrames = 40

    suspend fun run(
        shouldStop: () -> Boolean = { false },
        onProgress: suspend (done: Int, total: Int, preview: android.graphics.Bitmap?) -> Unit
    ): StarTrailResult = withContext(Dispatchers.Default) {
        val t0 = System.currentTimeMillis()
        var stacker: StarTrailStacker? = null
        var encoder: StarTrailVideoEncoder? = null
        var videoPair: Pair<Uri, android.os.ParcelFileDescriptor>? = null
        // ★存【调色板索引】而不是 Bitmap★：一帧只占 w*h 字节（1280×720 ≈ 921KB），
        //   而 ARGB8888 Bitmap 是它的 4 倍。同样的内存预算能多存 4 倍帧数 → 动图更顺滑
        val gifFrames = ArrayList<ByteArray>()
        var gifW = 0
        var gifH = 0
        var done = 0
        var stopped = false
        var jpegUri: Uri? = null
        var videoUri: Uri? = null
        var gifUri: Uri? = null
        val ts = java.text.SimpleDateFormat(
            "yyyyMMdd_HHmmss", java.util.Locale.CHINA
        ).format(java.util.Date())

        try {
            applyStarTrailCaptureParams(camera, config)
            // 给 HAL 一点时间把手动参数吃进去，第一帧才不会是自动曝光
            delay(600)

            val total = config.frames.coerceIn(1, 1000)
            // ★按目标帧数反推采样间隔★：不管拍多少张，动图最多 gifMaxFrames 帧
            val gifEvery = (total / gifMaxFrames).coerceAtLeast(1)
            val denoiseK = config.denoise / 100f
            val brightenK = config.brighten / 100f

            for (i in 0 until total) {
                if (!isActive || shouldStop()) {
                    stopped = true
                    break
                }
                val raw = captureFrameBitmap(context, imageCapture)
                if (raw == null) {
                    delay(config.intervalMs)
                    continue
                }
                val frame = StarTrailImaging.fit(raw, workEdge)
                if (frame !== raw) raw.recycle()

                if (stacker == null) {
                    stacker = StarTrailStacker(frame.width, frame.height)
                    if (config.makeVideo) {
                        val vw = frame.width - (frame.width % 2)
                        val vh = frame.height - (frame.height % 2)
                        videoPair = createGalleryVideoFile(context, "星轨_${ts}.mp4")
                        videoPair?.let { (_, pfd) ->
                            encoder = StarTrailVideoEncoder(vw, vh, 24, pfd)
                        }
                    }
                }
                stacker!!.add(frame)
                done++

                // 当前进度合成图：既喂给视频，也拿给用户看
                val shot = stacker!!.composite(denoiseK, brightenK, config.trailMode)
                encoder?.encodeFrame(shot)
                if (config.makeMotionPhoto &&
                    gifFrames.size < gifMaxFrames &&
                    (i % gifEvery == 0 || i == total - 1)
                ) {
                    val g = StarTrailImaging.fit(shot, gifEdge)
                    if (gifW == 0) {
                        gifW = g.width
                        gifH = g.height
                    }
                    // 立刻量化成索引并丢掉 Bitmap：shot 下面马上要 recycle
                    gifFrames.add(Gif89AEncoder.quantize(g))
                    if (g !== shot && !g.isRecycled) g.recycle()
                }
                // 进度预览图：所有权交给 UI（UI 收到新的会回收上一张），这里不回收
                val smallRaw = StarTrailImaging.fit(shot, 480)
                val small = if (smallRaw === shot)
                    smallRaw.copy(android.graphics.Bitmap.Config.ARGB_8888, false) else smallRaw
                onProgress(done, total, small)
                shot.recycle()
                frame.recycle()

                if (i < total - 1) delay(config.intervalMs)
            }

            // ---------- 导出产物 ----------
            val st = stacker
            if (st != null && st.frameCount > 0) {
                // ① 星轨长曝光合成图
                val final = st.finalImage(denoiseK, brightenK, config.trailMode)
                jpegUri = saveBitmapToGallery(context, final, "星轨_${ts}.jpg", "image/jpeg")
                final.recycle()

                // ② 星轨视频
                if (config.makeVideo && encoder != null) {
                    encoder!!.finish()
                    videoPair?.let { (uri, pfd) ->
                        closeGalleryVideoFile(context, uri, pfd)
                        videoUri = uri
                    }
                    encoder = null
                    videoPair = null
                }

                // ③ 星轨动态照片（GIF：星轨一点点长出来的过程）
                if (config.makeMotionPhoto && gifFrames.size >= 2) {
                    val nowMs = System.currentTimeMillis()
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "星轨_动态_${ts}.gif")
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/gif")
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_DCIM + "/Silky Camera"
                        )
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                        // ★显式写时间★：部分 ROM 的相册按 DATE_ADDED / DATE_TAKEN 排序和收录，
                        //   不写的话新文件可能排到最后甚至不被收录
                        put(MediaStore.MediaColumns.DATE_ADDED, nowMs / 1000)
                        put(MediaStore.MediaColumns.DATE_MODIFIED, nowMs / 1000)
                        put(MediaStore.Images.ImageColumns.DATE_TAKEN, nowMs)
                    }
                    val uri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                    )
                    if (uri != null) {
                        var bytes = 0L
                        context.contentResolver.openOutputStream(uri, "w")?.use { os ->
                            // ★★ GIF 帧率自动适配 ★★
                            //
                            // 以前是写死「整段 4 秒、按帧数平分」，于是：
                            //   拍 10 张  → 每帧 400ms，卡成幻灯片
                            //   拍 300 张 → 每帧 13ms，快到看不清
                            // 观感跟实际拍摄过程完全脱节。
                            //
                            // 现在按【真实拍摄时长】来定：
                            //   ① 算真实耗时：单帧曝光 + 帧间隔，乘以实际拍到的张数
                            //   ② 压到 2~12 秒播放（约 30:1 压缩比），
                            //      既不会因为几分钟的原始时长而拖沓，也不会一闪而过
                            //   ③ 再除以动图帧数，得到每帧时长 → 帧率自然就出来了
                            //
                            // 效果：拍得越久，动图播放越长；帧数越多，帧率越高 ——
                            //       动图节奏始终跟"星轨一点点长出来"的真实过程对得上。
                            val realMs =
                                (config.exposureSec * 1000f + config.intervalMs) *
                                        done.coerceAtLeast(1)
                            // 压到 2.5~6 秒播放：既不会因为实拍几分钟而拖沓，也不会一闪而过
                            val playMs = (realMs / 50f).coerceIn(2500f, 6000f)
                            val frameCount = gifFrames.size.coerceAtLeast(1)
                            // 每帧 40~400ms（对应 2.5~25fps），两端夹住保证流畅又不拖沓
                            val perFrame = (playMs / frameCount).roundToInt().coerceIn(40, 400)
                            val fps = 1000f / perFrame
                            Log.d(
                                "StarTrail",
                                "动图：实拍 ${(realMs / 1000f).roundToInt()}s → 播放 ${(playMs / 1000f).roundToInt()}s，" +
                                        "${frameCount} 帧 × ${perFrame}ms ≈ ${"%.1f".format(fps)}fps"
                            )
                            //
                            // ★用带计数的流包一层，写完后能校验到底写了多少字节★
                            //
                            // ⚠️ 必须同时重写 write(ByteArray, off, len)：
                            //   java.io.OutputStream 的默认实现是【逐字节】调 write(Int)，
                            //   一个几 MB 的 GIF 就是几百万次虚函数调用 + 几百万次
                            //   os.write(1字节)，慢到能明显感觉到卡顿（且 GC 压力巨大）。
                            //   重写批量写之后，一次系统调用写一大批，快两个数量级。
                            //
                            val counting = java.io.DataOutputStream(
                                object : java.io.OutputStream() {
                                    override fun write(b: Int) {
                                        bytes++
                                        os.write(b)
                                    }

                                    override fun write(b: ByteArray) {
                                        bytes += b.size
                                        os.write(b)
                                    }

                                    override fun write(b: ByteArray, off: Int, len: Int) {
                                        bytes += len
                                        os.write(b, off, len)
                                    }
                                }
                            )
                            Gif89AEncoder.writeIndices(
                                counting, gifFrames, gifW, gifH, perFrame
                            )
                            counting.flush()
                        }
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(uri, values, null, null)
                        // ★通知系统扫一遍★：MediaStore 插入在部分 ROM 上不会立刻生成缩略图，
                        //   相册就会显示灰色占位或直接不显示。显式扫描一次最稳。
                        try {
                            android.media.MediaScannerConnection.scanFile(
                                context,
                                arrayOf(
                                    Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_DCIM
                                    ).absolutePath + "/Silky Camera/星轨_动态_${ts}.gif"
                                ),
                                arrayOf("image/gif"),
                                null
                            )
                        } catch (e: Exception) {
                            Log.w("StarTrail", "媒体扫描通知失败（不影响文件本身）", e)
                        }
                        if (bytes <= 0) {
                            Log.e("StarTrail", "GIF 写入 0 字节，丢弃该 URI")
                        } else {
                            gifUri = uri
                            Log.d("StarTrail", "GIF 写入完成 ${bytes / 1024}KB / ${gifFrames.size} 帧")
                        }
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // ★取消不是异常★：必须原样抛出，否则协程的取消语义被破坏
            // （父作用域取消后这里吞掉，finally 之后的逻辑仍会继续跑）
            Log.d("StarTrail", "星轨拍摄已取消")
            throw e
        } catch (e: Exception) {
            Log.e("StarTrail", "星轨拍摄异常", e)
        } finally {
            try {
                encoder?.finish()
            } catch (e: Exception) {
            }
            videoPair?.let { (uri, pfd) -> closeGalleryVideoFile(context, uri, pfd) }
            gifFrames.clear()
            stacker?.release()
            clearStarTrailParams(camera)
        }
        StarTrailResult(
            jpegUri = jpegUri,
            videoUri = videoUri,
            gifUri = gifUri,
            frames = done,
            costMs = System.currentTimeMillis() - t0,
            stopped = stopped
        )
    }
}

// ==================== 星空模式 UI ====================

/** 底部那条「星空参数」条 */
@Composable
fun StarTrailBar(
    cfg: StarTrailConfig,
    running: Boolean,
    onOpenSetup: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .clickable { if (!running) onOpenSetup() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "🌌 星轨　${cfg.exposureSec}s × ${cfg.frames}张　ISO ${cfg.iso}",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = if (running) "拍摄中…" else "参数 ›",
            color = Color(0xFFFFA000),
            fontSize = 13.sp
        )
    }
}

/** 星空参数设置弹窗 */
@Composable
fun StarTrailSetupDialog(
    cfg: StarTrailConfig,
    onChange: (StarTrailConfig) -> Unit,
    onClose: () -> Unit
) {
    var c by remember { mutableStateOf(cfg) }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF20D0D0D))
                .padding(horizontal = 18.dp, vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            // ★与「机型与色彩」弹窗同一套布局★：卡片可滚动占满剩余空间，
            // 按钮固定在屏幕底部，不随内容跑
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF171719))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "星轨参数",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "建议：上三脚架、对准北极星附近、曝光越长轨迹越长",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    StarSlider("单帧曝光", "%.1f s", c.exposureSec, 0.5f, 30f) {
                        c = c.copy(exposureSec = it)
                    }
                    StarSlider("ISO", "%d", c.iso.toFloat(), 100f, 6400f, step = 100f) {
                        c = c.copy(iso = it.roundToInt())
                    }
                    StarSlider("堆栈张数", "%d 张", c.frames.toFloat(), 5f, 600f, step = 5f) {
                        c = c.copy(frames = it.roundToInt())
                    }
                    StarSlider("帧间隔", "%d ms", c.intervalMs.toFloat(), 0f, 3000f, step = 50f) {
                        c = c.copy(intervalMs = it.roundToInt().toLong())
                    }
                    StarSlider("智能降噪", "%d", c.denoise.toFloat(), 0f, 100f) {
                        c = c.copy(denoise = it.roundToInt())
                    }
                    StarSlider("智能提亮", "%d", c.brighten.toFloat(), 0f, 100f) {
                        c = c.copy(brighten = it.roundToInt())
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    StarToggle("星轨模式（最大值混合）", c.trailMode) { c = c.copy(trailMode = it) }
                    StarToggle("导出星轨视频 MP4", c.makeVideo) { c = c.copy(makeVideo = it) }
                    StarToggle("导出动态照片 GIF", c.makeMotionPhoto) { c = c.copy(makeMotionPhoto = it) }
                    StarToggle("对焦锁定无穷远", c.focusInfinity) { c = c.copy(focusInfinity = it) }
                    StarToggle("关闭机内降噪 / 锐化", c.rawPipeline) { c = c.copy(rawPipeline = it) }

                    Spacer(modifier = Modifier.height(14.dp))
                    val totalSec =
                        (c.exposureSec * c.frames + c.frames * c.intervalMs / 1000f).roundToInt()
                    Text(
                        text = "预计总耗时 ≈ ${totalSec / 60} 分 ${totalSec % 60} 秒",
                        color = Color(0xFFFFA000),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(56.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF171719))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "取消",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFFA000))
                            .clickable { onChange(c); onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "确定",
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun StarSlider(
    title: String,
    format: String,
    value: Float,
    min: Float,
    max: Float,
    step: Float = 0f,
    onValue: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = Color.White, fontSize = 13.sp)
            Text(
                text = if (format.contains("d")) {
                    String.format(format, value.roundToInt())
                } else String.format(format, value),
                color = Color(0xFFFFA000),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = value.coerceIn(min, max),
            onValueChange = { v ->
                onValue(if (step > 0f) (round(v / step) * step).coerceIn(min, max) else v)
            },
            valueRange = min..max
        )
    }
}

@Composable
private fun StarToggle(title: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onChanged(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 24.dp)
                .clip(CircleShape)
                .background(if (checked) Color(0xFFFFA000) else Color.White.copy(alpha = 0.18f))
                .clickable { onChanged(!checked) },
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

/** 拍摄中的进度浮层：显示已拍张数、进度条、实时合成预览 */
@Composable
fun StarTrailProgressOverlay(
    done: Int,
    total: Int,
    preview: android.graphics.Bitmap?,
    onStop: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            preview?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.FillWidth
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
            Text(
                text = "堆栈中 $done / $total",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((done.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(Color(0xFFFFA000))
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable { onStop() }
                    .padding(horizontal = 26.dp, vertical = 10.dp)
            ) {
                Text("停止并合成", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

/** 拍完的结果弹窗 */
@Composable
fun StarTrailResultDialog(
    result: StarTrailResult,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF20D0D0D))
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF171719))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🌌 星轨完成", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "堆栈 ${result.frames} 张 · 耗时 ${result.costMs / 1000}s" +
                            if (result.stopped) "（已提前停止）" else "",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = buildString {
                        if (result.jpegUri != null) appendLine("✅ 星轨合成图（JPEG）")
                        if (result.gifUri != null) appendLine("✅ 星轨动态照片（GIF）")
                        if (result.videoUri != null) appendLine("✅ 星轨视频（MP4）")
                        appendLine()
                        append("已保存到 相册 / DCIM / Silky Camera")
                    },
                    color = Color(0xFFFFA000),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) { Text("好", color = Color.White, fontSize = 14.sp) }
            }
        }
    }
}


// ==================== 统一镜头色彩 ====================
//
// 多摄手机：同一场景 1x 偏冷、0.6x 偏黄、3x 又偏绿（传感器光谱响应、镀膜、
// 各摄 AWB 各自收敛、厂商色彩风格都不一样所致）。
// 两层一起做：① 校准层：拍一次白纸/灰卡记下这颗头的中性偏差；
// ② 软件层：成片套 ColorMatrix（R/G/B 增益），一定生效，能兜住所有机型。
//
// 【教训】早期两件错事已移除：
//   ✗ 按镜头类型给启发式增益（超广角压青、长焦压黄）—— 凭猜，等于凭空制造色差。
//   ✗ 无条件 AWB_LOCK=true —— 把参考镜头的白平衡强套给别的镜头，
//     不同传感器光谱响应不同，套过去反而是错的。现在默认不锁，各自自动收敛。

object LensColorUnifier {

    /** 总开关（「机型与色彩」里可关） */
    @Volatile
    var enabled: Boolean = true

    /**
     * 是否锁白平衡。
     * ⚠️ 默认【关】—— 见上面的教训：锁了会让非参考镜头套用参考镜头的白平衡，
     *    而不同传感器光谱响应不同，套过去是错的。
     *    只有在你发现「拍视频 / 连拍过程中白平衡自己跳来跳去」时才打开，
     *    它的作用是防止白平衡漂移，不是用来统一色彩的。
     */
    @Volatile
    var lockAwb: Boolean = false

    /**
     * 是否把校准结果用 Camera2 手动增益（COLOR_CORRECTION_GAINS）下发到 HAL。
     * 默认关：TRANSFORM_MATRIX 模式会直接关掉自动白平衡，
     * 在自动白平衡缺席的情况下增益接近 1 会让画面发绿发灰。
     * 软件层修正本来就一定会生效，没必要冒这个险。
     */
    @Volatile
    var manualGain: Boolean = false

    /** 每颗头在校准卡上测到的平均 RGB（未修正的原始值） */
    private val measured = ConcurrentHashMap<String, FloatArray>()

    /** 全局风格修正（来自机型档案的品牌色彩倾向），所有镜头一起套，不产生镜头间色差 */
    @Volatile
    private var style: FloatArray = floatArrayOf(1f, 1f, 1f)

    /** 中性灰：所有已校准镜头都往这个点收敛 —— 收敛到同一点 = 统一 */
    private val NEUTRAL = floatArrayOf(1f, 1f, 1f)

    private const val PREFS = "silky_lens_color"
    private const val K_PREFIX = "m_"

    fun onProfileChanged(p: SeriesProfile?) {
        style = floatArrayOf(
            p?.tint?.getOrNull(0) ?: 1f,
            p?.tint?.getOrNull(1) ?: 1f,
            p?.tint?.getOrNull(2) ?: 1f
        )
        Log.d("LensColor", "风格修正载入：R=${style[0]} G=${style[1]} B=${style[2]}")
    }

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        measured.clear()
        var dropped = 0
        p.all.forEach { (k, v) ->
            if (k.startsWith(K_PREFIX) && v is String) {
                val parts = v.split(",")
                if (parts.size == 3) {
                    val r = parts[0].toFloatOrNull() ?: return@forEach
                    val g = parts[1].toFloatOrNull() ?: return@forEach
                    val b = parts[2].toFloatOrNull() ?: return@forEach
                    //
                    // ★拒绝非有限值（NaN / Infinity）★
                    //
                    // Kotlin 的 "NaN".toFloatOrNull() 会返回 Float.NaN（不是 null！），
                    // "Infinity" 同理。存档一旦被写进这种值，后面全线失守：
                    //   neutralGain: mean=NaN → `mean <= 1f` 为 false →
                    //                mean/raw = NaN → coerceIn 对 NaN 返回 NaN
                    //   gainFor:     NaN.coerceIn(...) 仍是 NaN
                    //   → ColorMatrix 里带着 NaN → 成片全黑 / 全白 / 花屏
                    // 而且这类值会【一直留在存档里】，每次启动都触发。
                    //
                    // 顺便挡掉非正数：通道为 0 会让除法爆掉、负值无意义。
                    //
                    if (!r.isFinite() || !g.isFinite() || !b.isFinite() ||
                        r <= 0f || g <= 0f || b <= 0f
                    ) {
                        dropped++
                        return@forEach
                    }
                    measured[k.removePrefix(K_PREFIX)] = floatArrayOf(r, g, b)
                }
            }
        }
        Log.d("LensColor", "已载入 ${measured.size} 颗头的校准数据${if (dropped > 0) "（丢弃 $dropped 条损坏数据）" else ""}")
        // 有损坏数据就顺手清掉，避免它一直留在存档里
        if (dropped > 0) persist(context)
    }

    private fun persist(context: Context) {
        val e = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        e.clear()
        measured.forEach { (k, v) -> e.putString(K_PREFIX + k, "${v[0]},${v[1]},${v[2]}") }
        e.apply()
    }

    /**
     * 镜头 key：直连物理头时用 cameraId，逻辑头时用「档位倍率」。
     * 同一个 key 对应同一套色彩修正。
     */
    fun lensKeyOf(boundLensId: String?, zoomStop: Float): String =
        if (!boundLensId.isNullOrEmpty()) "L$boundLensId"
        else "Z${(zoomStop * 10).roundToInt()}"

    /**
     * 把测到的 RGB 拉回中性灰（各通道都等于平均亮度）。
     *
     * ★为什么是拉到「中性灰」而不是拉到某个镜头的原始值★：
     *   所有已校准的镜头都往同一个点收敛 —— 收敛点是同一个，成片就统一了。
     *   如果拿「主摄的原始值」当参考，一旦主摄和长焦是在不同光线下校准的，
     *   参考本身就带光照差异，反而会引入新色差。中性灰是光照无关的，最稳。
     */
    private fun neutralGain(raw: FloatArray): FloatArray {
        // ★保底★：上面的 load 已经过滤过，这里是双保险 ——
        // 万一有别处写入了非法值，也不能让 NaN 流进 ColorMatrix
        if (raw.size < 3 || !raw[0].isFinite() || !raw[1].isFinite() || !raw[2].isFinite()) {
            return floatArrayOf(1f, 1f, 1f)
        }
        val mean = (raw[0] + raw[1] + raw[2]) / 3f
        if (!mean.isFinite() || mean <= 1f) return floatArrayOf(1f, 1f, 1f)
        return floatArrayOf(
            (mean / raw[0].coerceAtLeast(1f)).coerceIn(0.78f, 1.28f),
            (mean / raw[1].coerceAtLeast(1f)).coerceIn(0.78f, 1.28f),
            (mean / raw[2].coerceAtLeast(1f)).coerceIn(0.78f, 1.28f)
        )
    }

    /**
     * 这颗头最终要套的 R/G/B 增益。
     *
     * ★未校准的镜头一律返回中性（1,1,1），不再猜★ —— 猜出来的偏差对没那种偏差的
     * 机器就是凭空制造色差。要完全统一，就对每个镜头各校准一次。
     */
    fun gainFor(key: String): FloatArray {
        if (!enabled) return floatArrayOf(1f, 1f, 1f)
        val base = measured[key]?.let { neutralGain(it) } ?: floatArrayOf(1f, 1f, 1f)
        return floatArrayOf(
            (base[0] * style[0]).coerceIn(0.75f, 1.35f),
            (base[1] * style[1]).coerceIn(0.75f, 1.35f),
            (base[2] * style[2]).coerceIn(0.75f, 1.35f)
        )
    }

    /**
     * 采样一张图的中心区域平均 RGB（校准时用）。
     * ⚠️ 调用方必须先把图【降采样】再传进来：
     *    高像素模式下一张原图可能 5000 万像素，直接 getPixels 会吃掉几百 MB → 卡死。
     */
    fun measureCenter(bmp: android.graphics.Bitmap): FloatArray {
        val w = bmp.width
        val h = bmp.height
        val x0 = (w * 0.30f).roundToInt()
        val y0 = (h * 0.30f).roundToInt()
        val cw = (w * 0.40f).roundToInt().coerceAtLeast(1)
        val ch = (h * 0.40f).roundToInt().coerceAtLeast(1)
        val px = IntArray(cw * ch)
        bmp.getPixels(px, 0, cw, x0, y0, cw, ch)
        var r = 0.0
        var g = 0.0
        var b = 0.0
        var n = 0
        var i = 0
        while (i < px.size) {
            val c = px[i]
            r += (c shr 16) and 0xFF
            g += (c shr 8) and 0xFF
            b += c and 0xFF
            n++
            i += 3
        }
        if (n == 0) return floatArrayOf(1f, 1f, 1f)
        return floatArrayOf((r / n).toFloat(), (g / n).toFloat(), (b / n).toFloat())
    }

    /** 记下这颗头的校准值（对准白纸 / 灰卡拍一次） */
    fun setCalibration(context: Context, key: String, raw: FloatArray) {
        // ★入口校验★：不让非法值进存档（一旦进去就要等下次 load 才清得掉）
        if (raw.size < 3 || !raw[0].isFinite() || !raw[1].isFinite() || !raw[2].isFinite() ||
            raw[0] <= 0f || raw[1] <= 0f || raw[2] <= 0f
        ) {
            Log.w("LensColor", "校准值非法，已丢弃：$key")
            return
        }
        measured[key] = raw
        persist(context)
        val g = gainFor(key)
        Log.d(
            "LensColor",
            "校准 $key：原始 R=${raw[0]} G=${raw[1]} B=${raw[2]} → 修正 ×${g[0]} ×${g[1]} ×${g[2]}"
        )
    }

    fun clearCalibration(context: Context) {
        measured.clear()
        persist(context)
    }

    fun calibratedCount(): Int = measured.size

    fun isCalibrated(key: String): Boolean = measured.containsKey(key)

    /**
     * 硬件层：把白平衡相关参数塞进统一参数包。
     * ⚠️ 必须跟帧率 / 降噪 / 防抖放在【同一个 bundle】里下发 ——
     *    setCaptureRequestOptions 是全量替换，分开调会互相覆盖。
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun appendTo(
        b: CaptureRequestOptions.Builder,
        key: String,
        /**
         * ★强制锁白平衡★（录制过程中传 true）
         *
         * 这是「录出来的视频一阵一阵偏色、像花屏」的直接解法。
         * AWB 是逐帧收敛的：画面内容一变（有人走过、云飘过、变焦改变了视野），
         * 白平衡就会重新收敛，收敛期间 R/B 增益是连续变化的 ——
         * 录下来就是一段一段的色彩漂移，观感跟花屏一样。
         * 录制全程把它锁死，色彩才恒定。
         *
         * 这也是所有原生相机 App 的做法：录像开始时锁 AWB，录完再解开。
         */
        forceLockAwb: Boolean = false,
        /**
         * ★不要重设 CONTROL_AWB_MODE★（录制过程中传 true）
         *
         * 每次把 AWB_MODE 写回 AUTO 都会让 HAL 【重启一次白平衡收敛】，
         * 哪怕 AWB_LOCK 是 true 也一样 —— 因为 MODE 的变更优先级更高。
         * 录制中任何一次 applySceneParams（变焦挂起防抖、恢复防抖、切镜头…）
         * 都会带这一句，于是每操作一次视频就闪一次色。
         * 所以录制期间一律不写这一项，让 AWB 状态保持不动。
         */
        keepAwbMode: Boolean = false
    ) {
        if (!enabled) return
        try {
            val lock = lockAwb || forceLockAwb

            // 只在【非录制】时才把 AWB 拉回 AUTO —— 见 keepAwbMode 的说明。
            // 专业模式 / 星空模式下发过手动参数后，个别 ROM 会把 AWB 留在非 AUTO 上，
            // 换镜头时各摄的收敛行为就会不一致，所以平时要明确拉回 AUTO。
            if (!keepAwbMode) {
                b.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CameraMetadata.CONTROL_AWB_MODE_AUTO
                )
            }
            // 用户主动开了「锁定白平衡」或正在录制 → 锁（默认关，见 lockAwb 的说明）
            b.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, lock)
            if (forceLockAwb) {
                Log.d("LensColor", "录制中：白平衡已锁定（AWB_LOCK=true，且不重设 AWB_MODE）")
            }

            // 可选：把校准出的增益直接下给 HAL（默认关，见 manualGain 的说明）
            if (manualGain && measured.containsKey(key)) {
                val g = gainFor(key)
                b.setCaptureRequestOption(
                    CaptureRequest.COLOR_CORRECTION_MODE,
                    CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX
                )
                b.setCaptureRequestOption(
                    CaptureRequest.COLOR_CORRECTION_GAINS,
                    android.hardware.camera2.params.RggbChannelVector(
                        g[0], g[1], g[1], g[2]
                    )
                )
            }
        } catch (e: Exception) {
            Log.w("LensColor", "下发色彩统一参数失败", e)
        }
    }

    /** 软件层：成片要套的 ColorMatrix（一定生效，是真正的统一手段） */
    fun colorMatrix(key: String): android.graphics.ColorMatrix {
        val g = gainFor(key)
        return android.graphics.ColorMatrix(
            floatArrayOf(
                g[0], 0f, 0f, 0f, 0f,
                0f, g[1], 0f, 0f, 0f,
                0f, 0f, g[2], 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    /** 软件层：直接把修正套到 Bitmap 上 */
    fun applyToBitmap(bmp: android.graphics.Bitmap, key: String): android.graphics.Bitmap {
        val g = gainFor(key)
        if (abs(g[0] - 1f) < 0.004f && abs(g[1] - 1f) < 0.004f && abs(g[2] - 1f) < 0.004f) {
            return bmp
        }
        val out = android.graphics.Bitmap.createBitmap(
            bmp.width, bmp.height, android.graphics.Bitmap.Config.ARGB_8888
        )
        val p = android.graphics.Paint().apply {
            isAntiAlias = true
            colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix(key))
        }
        android.graphics.Canvas(out).drawBitmap(bmp, 0f, 0f, p)
        return out
    }

    /** 给「机型与色彩」弹窗显示用 */
    fun describe(key: String): String {
        val g = gainFor(key)
        return "R×${"%.3f".format(g[0])} G×${"%.3f".format(g[1])} B×${"%.3f".format(g[2])}" +
                if (measured.containsKey(key)) "（已校准）" else "（未校准，不修正）"
    }

    /** 当前这颗头的状态摘要，给弹窗用 */
    fun statusLine(key: String): String {
        val n = calibratedCount()
        return "当前镜头 $key：${describe(key)}　已校准 $n 个镜头"
    }
}


// ==================== 数据模型 ====================

/**
 * 本次会话里拍到的一个文件。
 * @param version 缩略图刷新版本号：后处理在 IO 线程异步跑，而 add 在它之前执行，
 *   缩略图会在处理完成前就解出原图且再不重载（produceState 的 key 是 uri，不变）→
 *   相册里看到的是没调过的原色。处理完 +1 让它重新解码。
 */
data class CapturedMedia(
    val uri: Uri,
    val isVideo: Boolean,
    val version: Int = 0
)

enum class CaptureMode(val label: String) {
    PRO("专业"),
    VIDEO("录像"),
    PHOTO("拍照"),
    STAR_TRAIL("星空")
}

/** 顶部工具栏的胶囊小按钮。selected=true 时高亮成橙色 */
/**
 * 顶部状态栏上的小按钮（像素 / 画质 / 帧率 / 防抖 / 比例）。
 *
 * ★改成方块状★ 以前用 18.dp 圆角（胶囊形），现在收到 6.dp ——
 * 接近方形、只保留很小的圆角，看起来更硬朗，也不再像一颗颗圆形按键。
 * 需要胶囊形时把 corner 传大一点即可。
 *
 * @param compact 紧凑模式：缩小内边距和字号，让方块更窄。
 *   横屏左侧参数栏用它，避免黑边占掉太多横向空间、挤压取景框。
 */
/**
 * ★通用按压反馈★：所有可点击控件统一用它做"按下缩小、松手弹回"。
 *
 * 用 spring（DampingRatioMediumBouncy）而不是 tween，手指抬起时有轻微回弹，
 * 跟 iOS 控件的手感接近。enabled=false 时不缩放（灰掉的按钮不该有反馈）。
 *
 * 用法：Modifier.bouncyPress(interactionSource).clickable(interactionSource = ..., ...)
 * ⚠️ 同一个 interactionSource 必须同时传给 bouncyPress 和 clickable，
 *    否则 clickable 里的按压状态传不出来、动画不会触发。
 */
@Composable
private fun Modifier.bouncyPress(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    pressed: Float = 0.92f,
    selected: Boolean = false,
    selectedScale: Float = 1f
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val target = when {
        !enabled -> 1f
        isPressed -> pressed
        selected -> selectedScale
        else -> 1f
    }
    val scaleValue by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bouncyPress"
    )
    // 局部变量改名成 scaleValue，就是为了不和 import 的 Modifier.scale 撞名，
    // 这样 this.scale(...) 的解析毫无歧义（同名遮蔽是 Kotlin 里最容易踩的坑之一）
    return this.scale(scaleValue)
}

/** 按压时的背景色高亮：从"常态色"过渡到"按下色"，比瞬间切换柔和 */
@Composable
private fun rememberPressColor(
    interactionSource: MutableInteractionSource,
    normal: Color,
    pressedColor: Color
): Color {
    val isPressed by interactionSource.collectIsPressedAsState()
    return animateColorAsState(
        targetValue = if (isPressed) pressedColor else normal,
        animationSpec = tween(durationMillis = 120),
        label = "pressColor"
    ).value
}

@Composable
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

    // ★交互动画★：按下缩小到 0.90、松手弹回；底色也从常态色平滑过渡
    val interactionSource = remember { MutableInteractionSource() }
    val bgColor = rememberPressColor(
        interactionSource = interactionSource,
        normal = if (selected) Color(0xFFFFA000) else Color.Black.copy(alpha = 0.4f),
        pressedColor = if (selected) Color(0xFFFFB300) else Color.White.copy(alpha = 0.18f)
    )

    Box(
        modifier = modifier
            .bouncyPress(interactionSource, pressed = 0.90f)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = padH, vertical = padV),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.Black else Color.White,
            fontSize = fs,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            // ★只旋转文字★：界面没跟着屏幕转时，把文字单独转正
            modifier = Modifier.rotate(LocalLabelRotation.current)
        )
    }
}

/** 竖排滑条轨道的高度：竖着画就是为了让拖动行程够长，别再压扁 */
private val PRO_SLIDER_VERTICAL_TRACK_H = 150.dp

/**
 * ★横屏是否重排成"左右分栏"★
 * false = 横屏沿用竖屏布局，UI 不旋转，只把文字转正（本次需求）。
 * true  = 恢复成以前的左右分栏（左侧设置栏 + 右侧控制栏）。
 */
private const val LANDSCAPE_RELAYOUT = false

/**
 * ★UI 锁定竖屏：屏幕转，UI 不转★
 *
 * true  = Activity 强制 PORTRAIT。手机横过来时界面完全不重排、不旋转，
 *         整个竖屏界面侧躺在横屏屏幕上（左右正好填满，不会留黑边），
 *         只有文字单独转正 —— 这就是"只旋转文字，不旋转 UI"。
 * false = 界面跟随屏幕旋转（老行为）。
 *
 * ⚠️ 打开后设置栏里的"旋转"开关不再影响界面方向（界面恒定竖屏），
 *    它只影响成片 EXIF 的方向记录。
 */
private const val LOCK_UI_PORTRAIT = true

/**
 * ★文字旋转方向修正★
 * 不同机型传感器装向不同，如果实测发现文字转反了（倒着或反向 90°），
 * 把这个值改成 1f 即可，不用动其它地方。
 */
private const val TEXT_ROTATION_SIGN = -1f

/**
 * 采样设备的物理旋转角（0 / 90 / 180 / 270），返回 Float 便于直接喂给 Modifier.rotate。
 *
 * 为什么用传感器而不是 Display.getRotation()：
 *   UI 被锁定竖屏后，屏幕内容方向永远是 0，Display.getRotation() 读不到"手机真的横过来了"，
 *   只有加速度传感器能拿到设备相对重力的真实姿态。
 *
 * 归一化到 90 的整数倍：手机拿在手里会微微晃，不归零的话文字会跟着抖。
 */
@Composable
private fun rememberDeviceRotationDeg(context: Context): Float {
    var deg by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                // ORIENTATION_UNKNOWN = -1：设备平放或姿态不确定，保持上一次的值
                if (orientation == ORIENTATION_UNKNOWN) return
                val snapped = ((orientation + 45) / 90) * 90
                deg = ((snapped % 360) + 360) % 360
            }
        }
        if (listener.canDetectOrientation()) listener.enable()
        onDispose { listener.disable() }
    }
    return deg.toFloat()
}

/**
 * 录像时间码：红色圆角底块 + 白色等宽数字（00:00），前面一颗闪烁红点。
 * 不会被录进视频也不会被拍进照片 —— 它是 Compose 画在 PreviewView 上层的普通控件，
 * 而 VideoCapture / ImageCapture 直接取相机流编码，跟屏幕合成无关。
 * @param seconds 已录制秒数
 */
@Composable
private fun RecordingTimecode(seconds: Long) {
    val mm = seconds / 60
    val ss = seconds % 60
    val text = "%02d:%02d".format(mm, ss)

    //
    // ★★红点：按【秒数的奇偶】切两档，不用无限动画★★
    //
    // 这是【4K60 录制卡顿】最主要的元凶，而且它只在录制时出现 ——
    // 正好解释了"为什么偏偏是录制的时候卡"。
    //
    // 原来用 rememberInfiniteTransition + infiniteRepeatable 做 1 秒周期的
    // 呼吸渐变。问题是：animateFloat 的每一帧都会写 State →
    // 【每一帧】触发一次 CameraApp 整体重组（约 60 次/秒）。
    // 待机时时间码根本不显示，所以平时完全看不出来；一开始录像它就常驻跑，
    // 而 4K60 时 CPU/GPU 已被编码器吃满，这 60 次/秒的全量重组
    // 足以把预览帧和视频帧一起挤掉 —— 表现就是卡顿、掉帧。
    //
    // 改成按 seconds 的奇偶取两档：
    //   · 视觉上仍然是"一秒亮一秒暗"，而且跟右边秒数跳变【同步】，
    //     比原来的渐变更有节奏感，一眼就知道"还在录"
    //   · 每秒只变 1 次 → 重组次数从 60 次/秒降到 1 次/秒
    //   · 不需要任何动画驱动器，零常驻开销
    //
    val blink = if (seconds % 2L == 0L) 1f else 0.25f

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFE53935))   // 红色色块
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = blink))
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            // 等宽：数字跳动时不会左右挪动（"11:11" 和 "88:88" 一样宽）
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * 构图辅助线：三分线（井字四线，交点是视觉焦点）+ 水平仪（画面正中一条线，端平变绿）。
 * 画在 PreviewView 上层，不进视频也不进照片（理由同 [RecordingTimecode]）。
 *
 * 核心：只用 horizonDeg 一个输入，不依赖任何外部旋转源。
 * @param horizonDeg 地平线在设备坐标系里的角度（度），见 [rememberDeviceTilt]
 */
@Composable
private fun CompositionGuides(
    horizonDeg: Float,
    visible: Boolean,
    /**
     * ★水平仪是否显示★（默认跟 visible 一致）
     *
     * 传 false 的场景：【镜头朝地面】时。
     *   俯拍地面时画面里根本没有地平线，"左右端平"毫无意义 ——
     *   水平仪只会变成一条跟着手机乱转的线干扰构图。
     *   此时三分线仍然有用（构图参照跟拍什么无关），所以只隐藏水平仪。
     */
    levelVisible: Boolean = true
) {
    if (!visible) return

    //
    // 自吸附：从 horizonDeg 自己推出"当前朝向"（横屏水平仪竖着的最终解法）。
    //
    // 之前两轮想用外部屏幕旋转角做补偿：compDeg = horizonDeg - screenRotDeg，
    // 但那个值在本项目里【恒为 0】—— UI 锁成竖屏后 OrientationEventListener
    // 拿不到可靠的朝向变化，补偿等于没加，横屏端平 horizonDeg=±90 仍画成竖的。
    //
    // 现在：端平时地平线角必然落在 0/±90/180 附近，吸附到最近 90° 即为当前朝向，
    // 差值就是歪了多少（必在 ±45° 内）。
    //   snapDeg = 90 × round(horizonDeg / 90)；tiltDeg = horizonDeg - snapDeg
    //   竖屏端平 0→tilt 0；横屏端平 -90→tilt 0；横屏歪10° -100→tilt -10；倒置 180→0
    // 四个朝向一视同仁，且完全不依赖外部传感器状态。
    //
    val snapDeg = 90f * kotlin.math.round(horizonDeg / 90f)
    val tiltDeg = horizonDeg - snapDeg

    //
    // ★★画布在世界里被转了多少度（这是「横屏时线是竖的」的最后一块拼图）★★
    //
    // UI 被锁成竖屏（LOCK_UI_PORTRAIT）后，手机横过来时系统会【把竖屏的
    // 画布整体旋转后贴到横屏屏幕上】（也就是"整个竖屏界面侧躺"）。
    // 于是画布的 x 轴在真实世界里【不再水平】——横屏时它变成了竖直方向。
    //
    // 上一版把指示线直接画在 tiltDeg 上，等于默认"画布没转"：
    //   横屏端平 tiltDeg=0 → 画布里画成横的 → 画布整体转了 90° → 眼睛里是竖的 ✗
    // 这正是你看到的"横屏时水平仪是竖着的"。
    //
    // 修法：把画布自身的旋转减掉。
    //   canvasRotDeg = 画布在真实世界里的旋转角 = 设备当前姿态角 = horizonDeg
    //   指示线 canvasDeg = -tiltDeg - canvasRotDeg
    //   参考线 canvasDeg = -canvasRotDeg（端平时的目标位置，与指示线重合即端平）
    //
    // ⚠️ 如果你的 ROM 是另一种合成方式（画布恒定竖屏、不跟着设备转），
    //    把 LEVEL_CANVAS_FOLLOWS_DEVICE 改成 false 即可，其它都不用动。
    //
    val canvasRotDeg = if (LEVEL_CANVAS_FOLLOWS_DEVICE) horizonDeg else 0f
    val barDeg = -tiltDeg - canvasRotDeg
    val refDeg = -canvasRotDeg

    // 端平的判定容差：1.5° 以内算水平。
    // 太严（0.5°）永远对不准，太松（5°）失去了水平仪的意义。
    val leveled = abs(tiltDeg) < LEVEL_TOLERANCE_DEG
    val guideColor = Color.White
    val levelColor by animateColorAsState(
        targetValue = if (leveled) Color(0xFF4CAF50) else Color.White,
        animationSpec = tween(durationMillis = 200),
        label = "levelColor"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // ── 三分线：井字四根 ──
        // 半透明细线，既能看清又不抢画面。用 1px 而不是 dp，
        // 保证任何密度下都是最细的一根，不会显得笨重。
        val lineW = 1f
        val lineAlpha = 0.38f
        for (i in 1..2) {
            val x = w * i / 3f
            drawLine(
                color = guideColor.copy(alpha = lineAlpha),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = lineW
            )
        }
        for (i in 1..2) {
            val y = h * i / 3f
            drawLine(
                color = guideColor.copy(alpha = lineAlpha),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = lineW
            )
        }

        //
        // ── 水平仪：一条纯线 ──
        //
        // ⚠️★这里【不能】用 DrawScope 的 rotate { } ★
        //   文件顶部 import 了 androidx.compose.ui.draw.rotate
        //   （那是 Modifier 的扩展：fun Modifier.rotate(degrees: Float): Modifier）。
        //   两个同名函数在作用域里打架，裸写 rotate(degrees=…, pivot=…) 会解析到
        //   Modifier.rotate → 报 "No parameter with name 'pivot'" +
        //   "Too many arguments for fun Modifier.rotate"。
        //   所以改为【手动三角函数】算旋转后的端点坐标，彻底绕开命名冲突。
        //
        val cx = w / 2f
        val cy = h / 2f
        //
        // ★长度取短边的 36%★（早先 60% 太长，压画面）
        // 用 min(w, h) 而不是 w：任何角度下都稳稳待在画面里，不会顶出去。
        //
        val half = minOf(w, h) * 0.18f

        //
        // ★★Canvas 角度 = -tiltDeg（取负）★★
        //
        // 这是【倾斜方向反了】的根因，跟横屏竖着是两码事：
        //   · 传感器（设备坐标系）：y 轴指向设备【顶部】，角度按【逆时针】为正
        //   · Canvas（屏幕坐标系）：y 轴指向屏幕【下方】，角度按【顺时针】为正
        // 两个坐标系的 y 轴方向相反，角度的旋向也就相反。
        // 直接把设备系角度喂给 cos/sin，线会朝【相反的那一侧】偏 ——
        // 手机往左歪，线却往右跑，看着就像"乱转 / 跟着屏幕转"。
        // 取负之后，手机往哪边歪，线就往哪边偏。
        //
        //
        // ★★水平仪：镜头朝地面时不画★★
        //   见 [levelVisible] 的说明。三分线照画（构图参照与拍摄内容无关）。
        //
        if (levelVisible) {
            val barW = 2.dp.toPx()
            // 指示线：画在 barDeg（= 倾斜量 - 画布自身旋转）
            val rad = Math.toRadians(barDeg.toDouble())
            val cosA = kotlin.math.cos(rad).toFloat()
            val sinA = kotlin.math.sin(rad).toFloat()
            drawLine(
                color = levelColor,
                start = Offset(cx - half * cosA, cy - half * sinA),
                end = Offset(cx + half * cosA, cy + half * sinA),
                strokeWidth = barW,
                cap = StrokeCap.Round
            )

            //
            // 参考线（固定不动）：画在【屏幕水平方向】上，代表"端平时的位置"。
            // 它跟指示线的夹角就是 -tiltDeg —— 两条线重合即端平。
            // 只有没端平时才画（端平时它和指示线重叠，画了也看不出）。
            //
            if (!leveled) {
                // 参考线：端平时的目标位置。端平时它与指示线重合，所以只在没端平时画。
                val rr = Math.toRadians(refDeg.toDouble())
                val rc = kotlin.math.cos(rr).toFloat()
                val rs = kotlin.math.sin(rr).toFloat()
                val rh = half * 0.62f
                drawLine(
                    color = Color.White.copy(alpha = 0.22f),
                    start = Offset(cx - rh * rc, cy - rh * rs),
                    end = Offset(cx + rh * rc, cy + rh * rs),
                    strokeWidth = 1.dp.toPx()
                )
            }
        } // ← if (levelVisible) 闭合
    }
}

/**
 * ★画布是否跟着设备一起转★
 *
 * true（默认）= Activity 锁竖屏后，系统把【竖屏画布整体旋转】贴到屏幕上
 *   （横屏时整个竖屏界面侧躺，画布的 x 轴在真实世界里变成竖直方向）。
 *   绝大多数 ROM 是这种行为。
 *
 * false = 画布恒定竖屏渲染，不跟着设备转（画布 x 轴在世界里始终水平）。
 *
 * ⚠️ 怎么判断该用哪个：横屏端平手机，看水平仪的线 ——
 *     线是竖着的 → 改另一个值（true ↔ false 对调）即可，其它都不用动。
 */
private const val LEVEL_CANVAS_FOLLOWS_DEVICE = true

/**
 * ★重负载录制档位（4K / 60帧）是否【不锁 AE】★
 *
 * true（默认）= 4K60 这类档位只锁 AWB、放开 AE，曝光随场景自适应。
 *   这是按「切换场景时要能调整参数」的反馈做的调整 —— 详见
 *   applySceneParams 里 ⑧ 的说明。锁死曝光会让室内走到室外的整段素材过曝。
 *
 * false = 所有档位一律 AE + AWB 双锁（回到最保守的做法）。
 *   如果你更在意"亮度也绝对恒定"而不在意场景自适应，改成 false。
 */
private const val REC_AE_LOCK_EXEMPT_FOR_HEAVY = true

/** 水平仪"端平"的判定容差（度）。见 [CompositionGuides] */
private const val LEVEL_TOLERANCE_DEG = 1.5f

/**
 * 地平线在【屏幕坐标系】里的角度（度）：0=竖屏端平，±90=横屏端平，180=倒置端平。
 *
 * 用 atan2(-ax, ay) 而不是常见的 roll = atan2(ax, √(ay²+az²))：
 *   后者第二项恒为非负，丢掉 ay 符号 → roll 被夹在 ±90，
 *   横屏端平时 ax≈±g 让它顶死不动，水平仪在横屏完全失效。
 * 加速度计（连续角度）而非 OrientationEventListener（只报 0/90/180/270 四档）。
 * 低通滤波 α=0.15，否则线一直颤。
 */
@Composable
/**
 * @param lowPower ★录制中降载★：true 时把传感器回调的【生效频率】降下来。
 *   见函数体内 onSensorChanged 的说明 —— 这是 4K60 录制卡顿的主因之一。
 */
private fun rememberDeviceTilt(context: Context, lowPower: Boolean): Pair<Float, Boolean> {
    var horizonDeg by remember { mutableFloatStateOf(0f) }
    //
    // ★是否【镜头朝地面】★
    //   加速度计静止时读的是"上"方向：
    //     az > 0 → 屏幕朝上（正常取景 / 拍天空）
    //     az < 0 → 屏幕朝下（俯拍地面）
    //   俯拍地面时"左右端平"毫无意义 —— 画面里根本没有地平线，
    //   水平仪只会变成一条乱转的线干扰构图，所以要自动隐藏。
    //
    //   ★方向别搞反★：加速度计读的是"上"方向在各轴的投影，
    //     设备 +z = 屏幕法线朝外，镜头在背面（-z 方向）。
    //       az ≈ +g（+9.8）→ 屏幕【朝上】→ 镜头朝【地面】  ← 俯拍
    //       az ≈ 0         → 手机竖直 → 平拍前方
    //       az ≈ -g（-9.8）→ 屏幕朝下 → 镜头朝【天空】      ← 仰拍
    //     所以判据是 az > +0.35g，不是 az < -0.35g。
    //
    //   ⚠️ 用【原始值 az】而不是滤波后的 fz：滤波会让 az 在翻腕瞬间滞后，
    //      判断慢半拍。另外加了回差（开 > 0.5g / 关 < 0.3g），
    //      避免在临界角附近反复横跳导致水平仪闪烁。
    //
    var facingGround by remember { mutableStateOf(false) }
    //
    // ★用 rememberUpdatedState 拿到最新值★
    //   DisposableEffect(context) 只在 context 变化时重建，
    //   协程/监听器里若直接捕获 lowPower 参数会永远是启动时的那一个值。
    //
    val lowPowerNow by rememberUpdatedState(lowPower)
    DisposableEffect(context) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val acc = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sm == null || acc == null) {
            onDispose { }
            return@DisposableEffect onDispose { }
        }
        val listener = object : SensorEventListener {
            // 低通滤波的上一帧值
            var fx = 0f
            var fy = 0f
            var fz = 0f
            var primed = false
            //
            // ★★录制中降载（4K60 卡顿的主因之一）★★
            //
            // 加速度计按 SENSOR_DELAY_UI 注册，回调频率约 60Hz。
            // 每次回调都会：算角度 → 写 horizonDeg（State）→ 触发 CameraApp
            // 【整体重组】→ CompositionGuides 的 Canvas 重绘。
            // 待机时这没什么，但 4K60 录制时 CPU/GPU 已被编码器吃满，
            // 每秒 60 次全量重组 + Canvas 重绘就足以把预览帧挤掉 —— 表现就是卡顿。
            //
            // 降载两招（只在录制时生效）：
            //   ① 抽稀 stride：每 10 次回调才生效 1 次（60Hz → 约 6Hz）
            //   ② 阈值 eps：角度变化小于阈值就不写 State（手持微抖被完全挡掉）
            // 叠加之后录制中的重组次数降到每秒个位数，水平仪仍然缓慢跟随，
            // 不会因为"彻底冻结"而显得像坏了。
            //
            var strideCount = 0
            override fun onSensorChanged(e: SensorEvent) {
                val lp = lowPowerNow
                val stride = if (lp) TILT_STRIDE_REC else TILT_STRIDE_IDLE
                val eps = if (lp) TILT_EPS_REC else TILT_EPS_IDLE
                strideCount++
                if (strideCount < stride) return
                strideCount = 0

                val ax = e.values[0]
                val ay = e.values[1]
                val az = e.values[2]
                if (!primed) {
                    fx = ax; fy = ay; fz = az; primed = true
                } else {
                    val a = 0.15f
                    fx += a * (ax - fx)
                    fy += a * (ay - fy)
                    fz += a * (az - fz)
                }
                //
                // ★地平线在屏幕坐标系里的角度★
                //   重力在屏幕平面内的投影（ax, ay）指向"上"，
                //   地平线垂直于它：h = (ay, -ax) → atan2(-ax, ay)
                //
                val newDeg = Math.toDegrees(
                    kotlin.math.atan2(-fx, fy).toDouble()
                ).toFloat()
                // 变化太小就不写 State —— 不写 = 不重组 = 不重绘
                if (abs(newDeg - horizonDeg) >= eps) horizonDeg = newDeg
                //
                // 镜头朝地面判定（带回差，防临界抖动）：
                //   az < -0.35g → 判定朝地面；az > -0.15g → 解除
                //
                //
                // 阈值取 0.5g（俯角 ≈30°）/ 0.3g（≈17°）：
                //   0.35g（≈20°）偏敏感，正常手持看屏幕时轻微低头就可能误隐藏；
                //   真正"拍地面/桌面"的俯角一般都在 45° 以上，0.5g 足够覆盖。
                //
                if (az > 0.5f * 9.8f) facingGround = true
                else if (az < 0.3f * 9.8f) facingGround = false
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, acc, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }
    return horizonDeg to facingGround
}

/** 待机时传感器回调的抽稀步长：1 = 每次都生效（约 60Hz） */
private const val TILT_STRIDE_IDLE = 1
/** ★录制中★的抽稀步长：10 → 约 6Hz */
private const val TILT_STRIDE_REC = 10
/** 待机时的角度变化阈值（度）：小于它不写 State，挡掉手持微抖 */
private const val TILT_EPS_IDLE = 0.15f
/** ★录制中★的角度变化阈值（度）：放宽到 1°，进一步减少重组 */
private const val TILT_EPS_REC = 1.0f

// ==================== 自动挡曝光策略（暗处延长快门）====================

/**
 * ★环境光照度（lux）★
 *
 * 用途：判断"现在暗不暗"，据此放宽 AE 的帧率上限，让 HAL 自动选择更长的曝光。
 *
 * ⚠️ 为什么用【环境光传感器】而不是画面亮度或 AE 结果：
 *   · 画面亮度：要挂 ImageAnalysis 逐帧算，预览开销太大
 *   · AE 结果（SENSOR_EXPOSURE_TIME / SENSOR_SENSITIVITY）：CameraX 不暴露
 *     实时 CaptureResult，要拿就得绕开 CameraX 自己写 Camera2 会话
 *   · 环境光传感器：系统级、几乎零开销，且是"预判"——
 *     在 AE 还没收敛到暗处之前就能提前放宽，比事后补救快
 *
 * ⚠️ 没有光照传感器的机器（不少千元机）返回 -1，调用方按"未知"处理，
 *    不降级也不激进，走默认档。
 */
@Composable
fun rememberAmbientLux(): Float {
    val context = LocalContext.current
    var lux by remember { mutableStateOf(-1f) }

    DisposableEffect(context) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val light = sm?.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (sm == null || light == null) {
            Log.d("Camera", "无环境光传感器，自动挡按默认曝光档处理")
            return@DisposableEffect onDispose { }
        }
        val listener = object : SensorEventListener {
            //
            // ★节流★：光照每变一点就写 State 会触发 CameraApp 整体重组。
            //   只在【跨过档位边界附近】或【变化超过 15%】时才写，
            //   平时（比如在同一个房间里小幅波动）一次都不重组。
            //
            var last = -1f
            override fun onSensorChanged(e: SensorEvent) {
                val v = e.values[0]
                if (last < 0f) {
                    last = v
                    lux = v
                    Log.d("Camera", "环境光 ${"%.0f".format(v)} lux")
                    return
                }
                val rel = if (last > 1f) abs(v - last) / last else 1f
                if (rel >= 0.15f) {
                    last = v
                    lux = v
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        // 光照变化本来就慢，用 SENSOR_DELAY_NORMAL（最慢档），省电也省回调
        sm.registerListener(listener, light, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sm.unregisterListener(listener) }
    }
    return lux
}

/**
 * ★自动挡曝光档位★
 *
 * 核心原理：Camera2 的 CONTROL_AE_TARGET_FPS_RANGE 不只是"帧率"，
 * 它同时【约束了 AE 能用的最长曝光时间】——
 *   因为曝光时间的上限就是 1/minFps。
 *   锁死 Range(30,30) 时，AE 最快只能给到 1/30 ≈ 33ms；
 *   信号不够时 AE 只剩一条路：拉高 ISO → 噪点爆炸。
 *   放宽成 Range(10,18) 后，AE 可以用到 1/10 = 100ms，
 *   同样的亮度下 ISO 能低好几档，画面干净得多。
 *
 * 这正是"暗处快门调慢一点"在自动挡下的唯一正经实现 ——
 * 自动挡里 SENSOR_EXPOSURE_TIME 是【只读】的，设不了，
 * 只能靠放宽帧率区间让 HAL 自己去选长曝光。
 *
 * ⚠️ 帧率降太多预览会卡，所以最暗档的下限也只到 10fps，
 *    且上限仍留 15~18fps —— 光线回升时 AE 会自动回到流畅档，不用等传感器。
 */
private data class AutoExposurePlan(
    /** AE 帧率区间：min 越小 = 允许越长的曝光 */
    val fpsMin: Int,
    val fpsMax: Int,
    /** 暗到一定程度就别锐化了 —— 锐化会把噪点一起放大 */
    val sharpen: Boolean
)

private fun autoExposurePlan(lux: Float, forCapture: Boolean): AutoExposurePlan {
    //
    // 拍照瞬间（captureQualityBoost）整体再放宽一档：
    // 那一瞬间预览卡一下无所谓，换来的长曝光是实打实的画质。
    //
    val bias = if (forCapture) 1 else 0
    return when {
        // 未知（无传感器）→ 不动，避免误判
        lux < 0f -> AutoExposurePlan(30, 30, true)
        // 明亮（室外阴天 / 明亮室内）
        lux >= 200f -> if (forCapture) AutoExposurePlan(24, 30, true)
        else AutoExposurePlan(30, 30, true)
        // 普通室内
        lux >= 100f -> if (bias > 0) AutoExposurePlan(20, 30, true)
        else AutoExposurePlan(26, 30, true)
        // 偏暗
        lux >= 40f -> if (bias > 0) AutoExposurePlan(15, 24, false)
        else AutoExposurePlan(22, 30, true)
        // 暗
        lux >= 15f -> if (bias > 0) AutoExposurePlan(10, 18, false)
        else AutoExposurePlan(18, 26, false)
        // 很暗
        else -> if (bias > 0) AutoExposurePlan(6, 15, false)
        else AutoExposurePlan(12, 20, false)
    }
}


/** 物理头绑定后，等预览流起来的最长时间。超过就判定这次直连失败并回退 */
private const val PHYSICAL_STREAM_TIMEOUT_MS = 2500L

/**
 * ★防抖开着时的物理头出流超时★
 * 上面已经挂起了防抖，但 HAL 内部可能还在收尾上一个 EIS/OIS 状态，
 * 出流会比平时慢。2.5s 的干等就是"卡死"的体感来源，压到 900ms 快速失败快速回退。
 */
private const val PHYSICAL_STREAM_TIMEOUT_STAB_MS = 900L

/**
 * ★同一个目标反复切头失败多少次后，彻底放弃物理直连★
 *
 * 防抖开着时，物理头出流失败 → 退回逻辑头 → 重绑 → 用户又点同一个焦段 → 再切…
 * 没有上限的话就成了一个"每次点都卡 1 秒"的慢性卡死循环。
 * 记到 3 次就关掉长焦直连，改由逻辑头 setZoomRatio 承担 ——
 * 画质差一点，但至少不卡。
 */
private const val LENS_SWITCH_FAIL_LIMIT = 3

/**
 * ★菜单高度上限★
 * 菜单内容再多也不许超过这个值，超出部分纵向滚动。
 * 之前是写死 300dp —— 屏幕矮的机器（横屏沿用竖屏布局时尤其明显）会被顶出屏幕。
 */
private const val MENU_MAX_HEIGHT_DP = 220f
/** 菜单高度占屏幕高度的比例上限（跟上面的绝对值取小，双保险） */
private const val MENU_MAX_HEIGHT_FRAC = 0.36f
/** 菜单高度下限：哪怕屏幕再矮也保留这点空间，否则点不到项 */
private const val MENU_MIN_HEIGHT_DP = 150f
/** 竖屏时菜单距顶部的距离（要避开顶部设置栏那一排） */
private const val MENU_TOP_PAD_PORTRAIT_DP = 52f
/** 菜单距屏幕边缘的距离 */
private const val MENU_SIDE_PAD_DP = 8f

/**
 * ★想要菜单出现在【用户看到的】哪个角★
 *   0 = 左上    1 = 右上    2 = 左下    3 = 右下
 * 直接写你想要的视觉位置，Compose 锚点由 [composeAnchorForVisual] 自动反推。
 * 竖屏不受影响，恒为右上。
 */
private const val MENU_VISUAL_CORNER = 0

/**
 * 坐标系旋转方向，由实测反推（不是猜的）：菜单 align 到 Compose BottomStart
 * 时实测出现在【视觉右下】，符合 -1（(u,v)→(v,-u)），不符 +1。故定为 -1。
 * ⚠️ 只有当你机器上四个角整体对不上时才改它（菜单跑到右下就改成 +1）。
 */
private const val MENU_ROT_DIR = -1

/** 角落码 → Alignment。码的 bit0 = 靠右、bit1 = 靠下：0左上 1右上 2左下 3右下 */
private fun alignmentOfCorner(code: Int): Alignment = when (code) {
    0 -> Alignment.TopStart
    1 -> Alignment.TopEnd
    2 -> Alignment.BottomStart
    else -> Alignment.BottomEnd
}

/**
 * Compose 角落码 → 用户看到的角落码。
 * 绕中心旋转 90° 只有两种方向，[MENU_ROT_DIR] 决定用哪一种：
 *   -1: (u,v) → ( v, -u)   即 (bu,bv) → (bv, 1-bu)
 *   +1: (u,v) → (-v,  u)   即 (bu,bv) → (1-bv, bu)
 */
private fun visualCornerOf(composeCode: Int): Int {
    val bu = composeCode and 1
    val bv = (composeCode shr 1) and 1
    return if (MENU_ROT_DIR < 0) bv or ((1 - bu) shl 1)
    else (1 - bv) or (bu shl 1)
}

/** 想让菜单出现在视觉某角 → 该把菜单 align 到 Compose 的哪个角 */
private fun composeAnchorForVisual(visual: Int): Alignment {
    for (c in 0..3) if (visualCornerOf(c) == visual) return alignmentOfCorner(c)
    return Alignment.TopEnd
}

/**
 * 横屏菜单微调，按【视觉方向】写：MENU_NUDGE_RIGHT_DP 正值 = 屏幕上向右挪，
 * MENU_NUDGE_UP_DP 正值 = 向上挪。代码自动换算到 Compose 坐标系，
 * 且在安全区内部偏移（BiasAlignment），永远不会被推出屏幕。
 * 可调上限由安全区余量决定：向右最多 (S - 菜单高)/2，向上最多 (S - 菜单宽)/2。
 * 想挪更远就加大 [MENU_SAFE_BOX_DP]。
 */
private const val MENU_NUDGE_RIGHT_DP = 20f
private const val MENU_NUDGE_UP_DP = 42f

/** 菜单面板自身尺寸上限：宽对应 widthIn(max)，高 = 内容上限 + 标题行 + 内边距 */
private const val MENU_PANEL_MAX_W_DP = 220f
private const val MENU_PANEL_MAX_H_DP = 280f

/**
 * 旋转安全区边长：横屏时菜单整体旋转 90°，需要正方形容器兜住防溢出。
 * ⚠️ 它也是"菜单能挪多远"的唯一来源：余量 = (S - 菜单尺寸)/2，旋转后宽高互换，
 * 所以视觉左右可调量取决于 (S - 菜单高)、上下取决于 (S - 菜单宽)。
 * 280 时左右余量为 0（菜单高已顶满，这就是之前"往右移不动"的原因）；
 * 320 → 左右 20dp、上下 50dp。别超过屏幕短边（代码已自动夹）。
 */
private const val MENU_SAFE_BOX_DP = 320f

/**
 * 等预览流真正起来。
 *
 * CameraX 的 PreviewView 会把预览流状态暴露成 IDLE / STREAMING ——
 * 这是判断"到底有没有画面"最可靠的信号，比自己数帧或看 zoomState 准得多。
 * 注意：绑上相机成功 ≠ 有画面，中间还隔着 surface 申请、HAL 出帧两步，
 * 物理头（尤其超广角）在这两步上失败的机型不少。
 */
private suspend fun waitForPreviewStream(view: PreviewView, timeoutMs: Long): Boolean {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        if (view.previewStreamState.value == PreviewView.StreamState.STREAMING) return true
        delay(100)
    }
    return view.previewStreamState.value == PreviewView.StreamState.STREAMING
}

/**
 * ★文字旋转角★：通过 CompositionLocal 下发，UI 上的文字按需取用。
 * 默认是 0（不转）；只有界面没跟着屏幕转时才由外层设成 ±90。
 */
val LocalLabelRotation = compositionLocalOf { 0f }

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
    /** ★RAW 格式开关★：false=JPEG，true=RAW+JPEG 双输出 */
    val rawEnabled: Boolean = false,
    /** 本机是否支持 RAW（null=还没探测完，此时菜单项置灰） */
    val rawSupported: Boolean? = null,
    val rawForce: Boolean = false,
    /** ★RAW 单输出★：只出 DNG（小米机型推荐，双输出会截断） */
    val rawSingleOutput: Boolean = false,
    val highResLabel: String,
    val videoQuality: VideoQuality,
    val videoFrameRate: VideoFrameRate,
    /** ★防抖模式★：关 / 光学 / 电子 / 双重 */
    val stabMode: StabMode,
    val photoRatio: PhotoRatio,
    val flashMode: Int,
    val autoRotate: Boolean,
    /** 调色盘是否有色偏（用于在设置栏上点亮入口） */
    val toneActive: Boolean = false
)

/** 参数栏用到的回调 */
private data class SettingsBarActions(
    val onHaptic: () -> Unit,
    val onHighResChange: (Boolean) -> Unit,
    /** ★切换 RAW 格式★：不支持时调用方自行提示，这里只负责状态 */
    val onRawToggle: () -> Unit = {},
    val onRawForceToggle: () -> Unit = {},
    val onRawSingleToggle: () -> Unit = {},
    val onVideoQualityChange: (VideoQuality) -> Unit,
    val onVideoFrameRateChange: (VideoFrameRate) -> Unit,
    val onStabilizationChange: (StabMode) -> Unit,
    val onPhotoRatioChange: (PhotoRatio) -> Unit,
    val onFlashExpand: () -> Unit,
    val onAbout: () -> Unit,
    /** 打开苹果风格调色盘 */
    val onColorPalette: () -> Unit = {},
    /** 打开多级菜单 */
    val onMenu: () -> Unit = {}
)

/**
 * ★状态栏按键的"规格"★
 *
 * 以前按键是写死在 [SettingsChipItems] 里的一串 TopChip，装不下就只能被挤出屏幕
 * （录像模式那排尤其明显）。现在每个按键都描述成一份数据：
 *   · 装得下 → 画成 chip
 *   · 装不下 → 按 [menuEntries] 展开成菜单里的若干项
 * 菜单按键 [CHIP_MENU] 永远保留，不参与溢出。
 */
private data class ChipSpec(
    val id: String,
    /** chip 上显示的文字 */
    val label: String,
    val selected: Boolean = false,
    val onClick: () -> Unit = {},
    /** 溢出后在菜单里展开成的项 */
    val menuEntries: List<MenuEntry> = emptyList()
)

/** 菜单里的一项（文字 / 是否选中 / 点击动作） */
private data class MenuEntry(
    val text: String,
    val selected: Boolean = false,
    val action: () -> Unit
)

// 按键 id：溢出判定和菜单回填都靠它对应
private const val CHIP_HIGH_RES = "chip_high_res"
private const val CHIP_QUALITY = "chip_quality"
private const val CHIP_FPS = "chip_fps"
private const val CHIP_STAB = "chip_stab"
private const val CHIP_TONE = "chip_tone"
private const val CHIP_MENU = "chip_menu"
private const val CHIP_FLASH = "chip_flash"
private fun ratioChipId(r: PhotoRatio): String = "chip_ratio_" + r.name

/**
 * 按当前状态生成状态栏按键列表。
 * 纯函数（不依赖 Composition），所以设置栏和菜单两边调用得到的一定是同一份。
 */
private fun buildChipSpecs(
    st: SettingsBarState,
    act: SettingsBarActions
): List<ChipSpec> {
    val out = mutableListOf<ChipSpec>()

    // ── 像素：只在【后置 + 拍照/专业模式】显示 ──
    if (!st.isFront && st.mode != CaptureMode.VIDEO) {
        out += ChipSpec(
            id = CHIP_HIGH_RES,
            // 显示本机真实最大像素，不再写死 5000 万
            label = if (st.highResMode) st.highResLabel else "12M",
            selected = st.highResMode,
            onClick = {
                act.onHaptic()
                act.onHighResChange(!st.highResMode)
            },
            menuEntries = listOf(
                MenuEntry(
                    if (st.highResMode) "高像素：开" else "高像素：关",
                    st.highResMode
                ) { act.onHighResChange(!st.highResMode) }
            )
        )
    }

    // ── 录像模式：画质 / 帧率 / 防抖 替代像素按钮 ──
    if (st.mode == CaptureMode.VIDEO) {
        out += ChipSpec(
            id = CHIP_QUALITY,
            label = st.videoQuality.label,
            onClick = {
                act.onHaptic()
                val vals = VideoQuality.values()
                act.onVideoQualityChange(vals[(vals.indexOf(st.videoQuality) + 1) % vals.size])
            },
            menuEntries = VideoQuality.values().map { q ->
                MenuEntry("画质 ${q.label}", st.videoQuality == q) { act.onVideoQualityChange(q) }
            }
        )
        out += ChipSpec(
            id = CHIP_FPS,
            label = st.videoFrameRate.label,
            selected = st.videoFrameRate == VideoFrameRate.FPS_60,
            onClick = {
                act.onHaptic()
                act.onVideoFrameRateChange(
                    if (st.videoFrameRate == VideoFrameRate.FPS_30)
                        VideoFrameRate.FPS_60 else VideoFrameRate.FPS_30
                )
            },
            menuEntries = VideoFrameRate.values().map { f ->
                MenuEntry("帧率 ${f.label}", st.videoFrameRate == f) { act.onVideoFrameRateChange(f) }
            }
        )
        //
        // ★防抖改成 4 档★：关 / 光学(OIS) / 电子(EIS) / 双重
        //   chip 上点一下循环切到下一档；菜单里可以直选。
        //   label 用短名（chip 太窄放不下全称），全称放菜单里。
        //
        out += ChipSpec(
            id = CHIP_STAB,
            label = if (st.stabMode == StabMode.OFF) "防抖"
            else "防抖·${st.stabMode.short}",
            selected = st.stabMode != StabMode.OFF,
            onClick = {
                act.onHaptic()
                act.onStabilizationChange(st.stabMode.next())
            },
            menuEntries = StabMode.values().map { m ->
                MenuEntry(m.label, st.stabMode == m) { act.onStabilizationChange(m) }
            }
        )
    }

    // ── 拍摄比例：3:4 / 16:9 / 全屏 ──
    PhotoRatio.values().forEach { r ->
        out += ChipSpec(
            id = ratioChipId(r),
            label = r.label,
            selected = st.photoRatio == r,
            onClick = {
                act.onHaptic()
                if (st.photoRatio != r) act.onPhotoRatioChange(r)
            },
            menuEntries = PhotoRatio.values().map { rr ->
                MenuEntry("比例 ${rr.label}", st.photoRatio == rr) { act.onPhotoRatioChange(rr) }
            }
        )
    }

    // ★「旋转」按键已移除★（所有模式下都不再显示）
    // 原来这里是一个 id = CHIP_ROTATE 的 chip，用来切 autoRotate（界面方向跟随传感器）。
    // 现在界面方向恒由 LOCK_UI_PORTRAIT 决定（锁定竖屏），autoRotate 保留为内部状态但不再有入口，
    // 文字转正走的是另一套：labelRotationDeg / LocalLabelRotation（未改动，见下方注释）。

    out += ChipSpec(
        id = CHIP_TONE,
        label = "调色",
        selected = st.toneActive,
        onClick = {
            act.onHaptic()
            act.onColorPalette()
        },
        menuEntries = listOf(
            MenuEntry("调色盘", st.toneActive) { act.onColorPalette() }
        )
    )

    // ── 多级菜单入口：永不溢出 ──
    out += ChipSpec(
        id = CHIP_MENU,
        label = "菜单",
        onClick = {
            act.onHaptic()
            act.onMenu()
        }
    )

    // ── 闪光灯：图标按钮 ──
    out += ChipSpec(
        id = CHIP_FLASH,
        label = "闪光",
        selected = st.flashMode == 3,
        onClick = {
            act.onHaptic()
            act.onFlashExpand()
        },
        menuEntries = listOf(
            MenuEntry("闪光灯") { act.onFlashExpand() }
        )
    )
    return out
}

/**
 * 估算一个 chip 在"排布方向"上的尺寸：横排算宽度，竖排算高度。
 *
 * 不做真实测量是有原因的：设置栏在 [BoxWithConstraints] 里，真实测量要走
 * SubcomposeLayout 两趟，代价高还容易在重组时抖动。而这里文字都是固定的中文/数字，
 * 按"中日韩全角、其余约 0.58em"估算，误差不到 2dp，足够判断装不装得下。
 */
private fun estimateChipMain(label: String, compact: Boolean): Dp {
    val fs = if (compact) TOP_CHIP_FONT_COMPACT.value else 12f
    var text = 0f
    for (c in label) {
        // 0x2E80 以后基本是中日韩字符，按一个字宽算；其余（数字/字母）按 0.58em
        text += if (c.code > 0x2E80) fs else fs * 0.58f
    }
    return if (compact) {
        // 竖排：主方向是高度 = 上下内边距 + 一行字高
        (TOP_CHIP_PAD_V_COMPACT.value * 2f + fs * 1.35f + 2f).dp
    } else {
        // 横排：主方向是宽度 = 左右内边距 + 文字宽
        (10f * 2f + text + 2f).dp
    }
}

/**
 * ★溢出判定★：按 [specs] 的顺序累加尺寸，超出 [available] 的挪进菜单。
 * 菜单键 [CHIP_MENU] 强制保留（否则整个菜单入口都没了），其余按先后顺序溢出。
 */
private fun computeHiddenChips(
    specs: List<ChipSpec>,
    available: Dp,
    gap: Dp,
    compact: Boolean
): Set<String> {
    if (available <= 0.dp) return emptySet()
    val hidden = mutableSetOf<String>()
    // 先给菜单键留位置
    var used = estimateChipMain("菜单", compact)
    for (spec in specs) {
        if (spec.id == CHIP_MENU) continue
        val need = when {
            // 闪光是圆形图标，尺寸固定
            spec.id == CHIP_FLASH -> if (compact) TOP_CHIP_ICON_COMPACT else 36.dp
            else -> estimateChipMain(spec.label, compact)
        }
        val next = used + gap + need
        if (next <= available) used = next else hidden.add(spec.id)
    }
    return hidden.toSet()
}

/**
 * 设置栏按键的实际渲染。
 *
 * ★自动适配★：装不下的按键（菜单键除外）由 [computeHiddenChips] 判定后不再画 chip，
 * 而是按 [ChipSpec.menuEntries] 在菜单里出现，所以功能一个都不会丢，只是换了入口。
 */
@Composable
private fun SettingsChipItems(
    specs: List<ChipSpec>,
    st: SettingsBarState,
    act: SettingsBarActions,
    compact: Boolean = false,
    hidden: Set<String> = emptySet()
) {
    specs.forEach { spec ->
        when {
            // ★菜单键永远保留★：它是所有溢出项的唯一入口，不能被溢出掉
            spec.id == CHIP_MENU -> {
                TopChip(
                    text = spec.label,
                    selected = false,
                    onClick = spec.onClick,
                    compact = compact
                )
            }
            spec.id == CHIP_FLASH -> {
                if (spec.id in hidden) return@forEach
                // ★交互动画★：与 TopChip 同一套按压反馈
                val flashSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .bouncyPress(flashSource, pressed = 0.88f)
                        .size(if (compact) TOP_CHIP_ICON_COMPACT else 36.dp)
                        .clip(CircleShape)
                        .background(
                            if (st.flashMode == 3) Color(0xFFFFC107).copy(alpha = 0.9f)
                            else Color.Black.copy(alpha = 0.4f)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable(
                            interactionSource = flashSource,
                            indication = null
                        ) {
                            act.onHaptic()
                            act.onFlashExpand()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    FlashIcon(mode = st.flashMode)
                }
            }
            else -> {
                if (spec.id in hidden) return@forEach
                TopChip(
                    text = spec.label,
                    selected = spec.selected,
                    onClick = spec.onClick,
                    compact = compact
                )
            }
        }
    }
    // ★关于已移入"菜单 → 关于"★：设置栏上不再常驻"?"按钮
}

/** 竖屏：参数栏横着排在顶部 */
@Composable
private fun SettingsChipsRow(
    modifier: Modifier = Modifier,
    st: SettingsBarState,
    act: SettingsBarActions,
    hidden: Set<String> = emptySet(),
    onHiddenChange: (Set<String>) -> Unit = {}
) {
    val specs = buildChipSpecs(st, act)
    BoxWithConstraints(modifier = modifier) {
        // 用真实可用宽度判定：装不下的按键交给菜单
        val shouldHide = computeHiddenChips(specs, maxWidth, TOP_BAR_GAP_PORTRAIT, false)
        LaunchedEffect(shouldHide) { onHiddenChange(shouldHide) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TOP_BAR_GAP_PORTRAIT)
        ) {
            SettingsChipItems(specs, st, act, compact = false, hidden = hidden)
        }
    }
}

/** 横屏：参数栏竖着排在屏幕左边 */
@Composable
private fun SettingsChipsColumn(
    modifier: Modifier = Modifier,
    st: SettingsBarState,
    act: SettingsBarActions,
    hidden: Set<String> = emptySet(),
    onHiddenChange: (Set<String>) -> Unit = {}
) {
    val specs = buildChipSpecs(st, act)
    BoxWithConstraints(modifier = modifier) {
        // 竖排：可用高度就是这一栏的最大高度，间隔 6.dp
        val gap = 6.dp
        val shouldHide = computeHiddenChips(specs, maxHeight, gap, true)
        LaunchedEffect(shouldHide) { onHiddenChange(shouldHide) }
        Column(
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SettingsChipItems(specs, st, act, compact = true, hidden = hidden)
        }
    }
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

/**
 * 录像码率：以 1080p30 = 24Mbps 为锚点，其余档位按像素/秒等比缩放。
 * 4K 额外乘 0.72 效率折扣（分辨率越高压缩效率越高，线性给会浪费且撑爆编码器）。
 *
 * 720p30=11M  720p60=21M  1080p30=24M  1080p60=48M  4K30=69M  4K60=138M（夹前值）
 *
 * ⚠️ 末尾 coerceIn(2M, CAP) 是【全局】生效的，所以 4K 两档实际都夹到 60M。
 *    60M 是硬上限：之前「4K60 花屏」就是编码器过载吐坏帧，给满必然复发。
 */
private const val VIDEO_BITRATE_BASE_FHD30 = 24_000_000   // 锚点：1080p30
private const val VIDEO_BITRATE_CAP_4K60 = 60_000_000     // 4K60 硬上限（防编码器过载）

/** 分辨率相对 1080p 的像素量倍数 */
private fun pixelFactor(q: VideoQuality): Float = when (q) {
    VideoQuality.UHD -> (3840f * 2160f) / (1920f * 1080f)   // 4.0
    VideoQuality.FHD -> 1f
    VideoQuality.HD -> (1280f * 720f) / (1920f * 1080f)     // 0.444
}

private fun videoBitrateFor(q: VideoQuality, fps: VideoFrameRate): Int {
    val rate = VIDEO_BITRATE_BASE_FHD30 *
            pixelFactor(q) * (fps.fps / 30f) *
            (if (q == VideoQuality.UHD) 0.72f else 1f)
    val capped = if (q == VideoQuality.UHD && fps == VideoFrameRate.FPS_60) {
        minOf(rate, VIDEO_BITRATE_CAP_4K60.toFloat())
    } else rate
    return capped.toInt().coerceIn(2_000_000, VIDEO_BITRATE_CAP_4K60)
}

/**
 * ★录像码率档位★（对应建议"可以更高规格的配置视频录制"）
 * 在上面算出的基准上再乘一个倍率，给想手动拉满画质的用户。
 */
enum class VideoBitrateLevel(val label: String, val scale: Float) {
    NORMAL("标准", 1.0f),
    HIGH("高", 1.5f),
    EXTREME("极高", 2.2f)
}

/** 应用码率档位后的最终码率 */
fun videoBitrateFinal(q: VideoQuality, fps: VideoFrameRate, level: VideoBitrateLevel): Int =
    (videoBitrateFor(q, fps) * level.scale).toInt()
        .coerceIn(2_000_000, 120_000_000)

/**
 * ★录音码率★：256 kbps / 48kHz / 单声道或立体声。
 * ⚠️ 用户明确要求"录音码率不变"，所以这里固定 256k，不跟随视频码率档位变化。
 */
const val AUDIO_BITRATE = 256_000
const val AUDIO_SAMPLE_RATE = 48_000

/**
 * 哈苏风格快门声，程序合成 PCM（不用系统 MediaActionSound：那是日系单反的
 * "沉闷反光板"音色，各家 ROM 还不一样）。
 *
 * 镜间快门三特征：① 极短促（20ms 内衰减完）② 高频为主（2~6kHz，薄脆金属）
 * ③ 两声（开帘"咔" + 70ms 后收帘"嗒"，第二声更轻更钝）。
 * 合成 = 带通噪声爆发(1.4k~6.5kHz) + 三条共振峰(3.2k/4.7k/6.3k)，
 * 都套极快指数衰减（噪声 6.2ms / 共振 4.2ms），起振 0.6ms。
 * 48kHz/16bit/单声道，全长 170ms ≈ 16KB，合成一次后缓存。
 *
 * ⚠️ 强制快门声的 ROM 上开关会被系统覆盖 —— 系统策略，App 改不了。
 */
object ShutterSound {
    private const val SR = 48_000

    @Volatile private var pcmBytes: ByteArray? = null
    @Volatile private var track: AudioTrack? = null
    @Volatile private var trackBytes: Int = 0
    @Volatile private var exec: java.util.concurrent.ExecutorService? = null

    /** 合成一次，之后一直复用 */
    private fun build(): ByteArray {
        val n = SR * 170 / 1000
        val buf = FloatArray(n)

        // 第一声：开帘。最响、共振峰最高
        addClick(buf, SR, 0, 0.95f,
            floatArrayOf(3200f, 4700f, 6300f), 0.0062f, 0.0042f, 1L)
        // 第二声：收帘。70ms 后，轻一些、钝一些（共振峰整体下移）
        addClick(buf, SR, 70, 0.62f,
            floatArrayOf(2700f, 3950f, 5400f), 0.0045f, 0.0030f, 2L)

        // 归一化到 -0.7dBFS 左右，再转 16bit 小端
        var peak = 0f
        for (v in buf) { val a = if (v < 0f) -v else v; if (a > peak) peak = a }
        val norm = if (peak > 0.0001f) 0.92f / peak else 1f
        val bytes = ByteArray(n * 2)
        for (i in 0 until n) {
            val s = (buf[i] * norm).coerceIn(-1f, 1f)
            val q = (s * 32767f).toInt()
            bytes[i * 2] = (q and 0xFF).toByte()
            bytes[i * 2 + 1] = ((q shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    private fun addClick(
        out: FloatArray, sr: Int, startMs: Int, amp: Float,
        peaks: FloatArray, decayNoise: Float, decayTone: Float, seed: Long
    ) {
        val s0 = sr * startMs / 1000
        val dur = sr * 60 / 1000          // 60ms 窗口足够衰减干净
        val dt = 1.0 / sr
        val pi = Math.PI.toFloat()

        // 带通 = 一阶高通(1.4kHz) + 两级一阶低通(6.5kHz)
        val rcHp = 1.0 / (2.0 * Math.PI * 1400.0)
        val aHp = (rcHp / (rcHp + dt)).toFloat()
        val rcLp = 1.0 / (2.0 * Math.PI * 6500.0)
        val aLp = (dt / (rcLp + dt)).toFloat()

        val rnd = java.util.Random(seed)
        var xPrev = 0f
        var yHp = 0f
        var l1 = 0f
        var l2 = 0f
        val attack = sr / 1600            // 约 0.6ms 的硬起振

        for (i in 0 until dur) {
            val idx = s0 + i
            if (idx >= out.size) break
            val t = i.toFloat() / sr

            val x = rnd.nextFloat() * 2f - 1f
            yHp = aHp * (yHp + x - xPrev)
            xPrev = x
            l1 += aLp * (yHp - l1)
            l2 += aLp * (l1 - l2)

            var v = l2 * exp(-t / decayNoise) * 0.85f
            for (k in peaks.indices) {
                val a = 0.30f / (1f + k * 0.55f)
                val d = decayTone * (1f + k * 0.25f)
                v += a * sin(2f * pi * peaks[k] * t) * exp(-t / d)
            }
            if (i < attack) v *= i.toFloat() / attack.toFloat()
            out[idx] += v * amp
        }
    }

    @Synchronized
    private fun ensureTrack(len: Int): AudioTrack {
        var t = track
        if (t == null || t.state != AudioTrack.STATE_INITIALIZED || trackBytes < len) {
            try { t?.release() } catch (_: Exception) {}
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val fmt = AudioFormat.Builder()
                .setSampleRate(SR)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val min = AudioTrack.getMinBufferSize(
                SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            t = AudioTrack(attrs, fmt, maxOf(min, len), AudioTrack.MODE_STATIC, 0)
            track = t
            trackBytes = maxOf(min, len)
        }
        return t!!
    }

    @Synchronized
    fun play(enabled: Boolean) {
        if (!enabled) return
        try {
            val data = pcmBytes ?: build().also { pcmBytes = it }
            val ex = exec ?: java.util.concurrent.Executors.newSingleThreadExecutor()
                .also { exec = it }
            //
            // ★必须放到后台线程★
            //   MODE_STATIC 的 write() 是阻塞的（要等整段数据拷进缓冲区），
            //   虽然只有 16KB、几毫秒，但快门声是在 UI 线程触发的，
            //   一次卡顿就会让快门响应慢一帧，连拍时尤其明显。
            //
            ex.execute {
                try {
                    val t = ensureTrack(data.size)
                    // MODE_STATIC 播完一次后必须先 stop 才能再 write
                    try { t.stop() } catch (_: Exception) {}
                    t.write(data, 0, data.size)
                    t.play()
                } catch (e: Exception) {
                    Log.w("SilkyCamera", "哈苏快门声播放失败", e)
                }
            }
        } catch (e: Exception) {
            Log.w("SilkyCamera", "快门声合成失败", e)
        }
    }

    @Synchronized
    fun release() {
        try { track?.release() } catch (_: Exception) {}
        track = null
        trackBytes = 0
        try { exec?.shutdownNow() } catch (_: Exception) {}
        exec = null
    }
}

/**
 * ★左右滑动切换模式：触发阈值（dp）★
 * 60dp 大约是屏幕宽度的 1/6，比变焦轮盘那类滑动手势的阈值大得多 ——
 * 刻意调大，避免"想点一下、手抖了一下就切了模式"。
 */
const val SWIPE_MODE_THRESHOLD_DP = 60f

/**
 * ★左右滑动切换模式：方向判定系数★
 * 要求横向位移大于纵向位移的 [SWIPE_MODE_AXIS_RATIO] 倍才算"横滑"。
 * 斜着划（比如 45°）不算 —— 那种多半是想拖变焦轮盘。
 */
const val SWIPE_MODE_AXIS_RATIO = 1.5f

/** 录像帧率 */
enum class VideoFrameRate(val label: String, val fps: Int) {
    FPS_30("30", 30),
    FPS_60("60", 60)
}

/**
 * 防抖模式。⚠️ OIS 和 EIS 是两回事，必须分开控制。
 *
 * OIS（LENS_OPTICAL_STABILIZATION_MODE）：镜头马达位移补偿，硬件实现，
 *   不裁切画面，拍照录像都能用（拍照收益最大：压低快门、减少糊片），4K60 下照样可开。
 * EIS（CONTROL_VIDEO_STABILIZATION_MODE）：逐帧运动估计 + 重算 crop，软件实现，
 *   会裁掉约 10% 画面，只在录像有意义；4K60 下迫使 HAL 反复重配 crop → 3A 重收敛 → 闪色。
 *
 * 以前把两者绑成同一开关且要求 mode==VIDEO → 拍照时 OIS 永远关着，
 * 这正是小米反馈的"默认拍照模式下无法调用光学防抖"的根因。
 */
enum class StabMode(
    val label: String,
    /** 设置栏 chip 上的短标签（地方小，放不下全称） */
    val short: String,
    val ois: Boolean,
    val eis: Boolean
) {
    OFF("关闭", "关", false, false),
    OIS("光学防抖", "光学", true, false),
    EIS("电子防抖", "电子", false, true),
    BOTH("双重防抖", "双重", true, true);

    /** 点一下切到下一档：关 → 光学 → 电子 → 双重 → 关 */
    fun next(): StabMode = values()[(ordinal + 1) % values().size]

    companion object {
        /** 默认档：光学防抖。不裁切画面，拍照录像都受益，最安全 */
        val DEFAULT = OIS
    }
}

// ==================== 设置持久化 ====================

/** 需要跨会话保存的全部设置 */
data class CameraSettings(
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val mode: CaptureMode = CaptureMode.PHOTO,
    val photoRatio: PhotoRatio = PhotoRatio.RATIO_3_4,
    /** ★高像素模式★：用传感器在当前比例下能输出的最大尺寸拍照。默认关 */
    val highResMode: Boolean = false,
    /** ★防抖模式★（原来是 Boolean，现在 4 档） */
    val stabMode: StabMode = StabMode.DEFAULT,
    /** ★RAW（DNG）格式★：仅在机型支持时生效，见 [RawMode]。默认关 */
    val rawEnabled: Boolean = false,
    /** ★强制 RAW★：检测说不支持也强行按 RAW 绑定（逃生门），默认关 */
    val rawForce: Boolean = false,
    /**
     * ★RAW 单输出★：true = 只出 DNG，false = DNG + JPEG 双出。
     * 小米数字系列默认 true（双输出路径在该系列上会截断 RAW buffer）。
     */
    val rawSingleOutput: Boolean = false,
    /** ★快门声★：拍照时是否播放系统快门音 */
    val shutterSound: Boolean = true,
    /** ★左右滑动屏幕切换拍摄模式★ */
    val swipeModeEnabled: Boolean = true,
    val flashMode: Int = 0,
    val videoQuality: VideoQuality = VideoQuality.FHD,
    val videoFrameRate: VideoFrameRate = VideoFrameRate.FPS_30,
    /** 自动旋转：屏幕方向跟随传感器（关掉则跟随系统设置） */
    val autoRotate: Boolean = true,
    val zoom: Float = 1.0f,
    val proIso: Int = 0,
    val proShutterNs: Long = 0L,
    val proEv: Int = 0,
    val proFocus: Float = -1f,
    val proWb: Int = 0,
    /** 苹果风格调色盘：色温，负=偏冷 正=偏暖（-100 ~ 100） */
    val toneWarmth: Float = 0f,
    /** 苹果风格调色盘：色调，负=偏绿 正=偏洋红（-100 ~ 100） */
    val toneTint: Float = 0f,
    /** 调色强度（0 ~ 100），0 = 关闭 */
    val toneIntensity: Float = 100f,
    /**
     * ★UI隐藏（双击空白处隐藏全部 UI）的总开关★
     * false（默认）= 双击空白不生效，避免误触。
     * 打开后：拍摄界面双击空白处隐藏所有 UI，再双击恢复。
     */
    val uiHideEnabled: Boolean = false,
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
    /** ★新 key★：防抖从布尔改成 4 档，存 enum name。
     *  刻意不用旧 K_STAB —— 它是 putBoolean 写进去的，用 getString 读会
     *  ClassCastException。旧值直接忽略，落到默认档（光学防抖）。 */
    private const val K_STAB_MODE = "video_stab_mode"
    private const val K_RAW = "raw_enabled"
    private const val K_RAW_FORCE = "raw_force"
    private const val K_RAW_SINGLE = "raw_single"
    private const val K_SHUTTER_SOUND = "shutter_sound"
    private const val K_SWIPE_MODE = "swipe_mode"
    private const val K_ROTATE = "auto_rotate"
    private const val K_UI_HIDE = "ui_hide_enabled"
    private const val K_LM = "linear_map"
    private const val K_ZOOM = "zoom"
    private const val K_ISO = "pro_iso"
    private const val K_SHUTTER = "pro_shutter"
    private const val K_EV = "pro_ev"
    private const val K_FOCUS = "pro_focus"
    private const val K_WB = "pro_wb"
    private const val K_TONE_W = "tone_warmth"
    private const val K_TONE_T = "tone_tint"
    private const val K_TONE_I = "tone_intensity"

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
                stabMode = runCatching {
                    StabMode.valueOf(p.getString(K_STAB_MODE, null) ?: "")
                }.getOrDefault(StabMode.DEFAULT),
                rawEnabled = p.getBoolean(K_RAW, false),
                rawForce = p.getBoolean(K_RAW_FORCE, false),
                rawSingleOutput = p.getBoolean(K_RAW_SINGLE, DeviceCompatibility.isXiaomiDigitalSeries),
                shutterSound = p.getBoolean(K_SHUTTER_SOUND, true),
                swipeModeEnabled = p.getBoolean(K_SWIPE_MODE, true),
                autoRotate = p.getBoolean(K_ROTATE, true),
                linearMap = decodeLinearMap(p.getString(K_LM, null)),
                zoom = p.getFloat(K_ZOOM, 1.0f),
                proIso = p.getInt(K_ISO, 0),
                proShutterNs = p.getLong(K_SHUTTER, 0L),
                proEv = p.getInt(K_EV, 0),
                proFocus = p.getFloat(K_FOCUS, -1f),
                proWb = p.getInt(K_WB, 0),
                toneWarmth = p.getFloat(K_TONE_W, 0f),
                toneTint = p.getFloat(K_TONE_T, 0f),
                toneIntensity = p.getFloat(K_TONE_I, 100f),
                uiHideEnabled = p.getBoolean(K_UI_HIDE, false)
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
                .putString(K_STAB_MODE, s.stabMode.name)
                .putBoolean(K_RAW, s.rawEnabled)
                .putBoolean(K_RAW_FORCE, s.rawForce)
                .putBoolean(K_RAW_SINGLE, s.rawSingleOutput)
                .putBoolean(K_SHUTTER_SOUND, s.shutterSound)
                .putBoolean(K_SWIPE_MODE, s.swipeModeEnabled)
                .putBoolean(K_ROTATE, s.autoRotate)
                .putFloat(K_ZOOM, s.zoom)
                .putInt(K_ISO, s.proIso)
                .putLong(K_SHUTTER, s.proShutterNs)
                .putInt(K_EV, s.proEv)
                .putFloat(K_FOCUS, s.proFocus)
                .putInt(K_WB, s.proWb)
                .putFloat(K_TONE_W, s.toneWarmth)
                .putFloat(K_TONE_T, s.toneTint)
                .putFloat(K_TONE_I, s.toneIntensity)
                .putBoolean(K_UI_HIDE, s.uiHideEnabled)
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
// ==================== RAW（DNG）拍摄 ====================

/**
 * ★RAW 输出格式的常量值★
 * CameraX 从 1.5.0 才开始支持 RAW。这里写成【字面量】而不是引用
 * ImageCapture.OUTPUT_FORMAT_RAW_JPEG —— 引用真实常量在旧版 CameraX 上
 * 会直接 Unresolved reference 编译失败，字面量则任何版本都能编过。
 */
private const val CX_OUTPUT_FORMAT_JPEG = 0
private const val CX_OUTPUT_FORMAT_RAW = 2
private const val CX_OUTPUT_FORMAT_RAW_JPEG = 3

/**
 * ★RAW（DNG）拍摄支持★
 *
 * 为什么全部走【反射】：
 *   CameraX 1.5.0 才加入 RAW（ImageCapture 的 setOutputFormat / 双输出 takePicture）。
 *   直接引用这些 API，1.4.x 及以下会 Unresolved reference。
 *   反射则任何版本都能编译，运行时探测到就启用、探测不到就降级为"不支持"。
 *
 * ⚠️ 若你的工程已是 CameraX 1.5.0+，可以换回直接调用（功能完全一致，
 *    且能拿到编译期检查）；保留反射的好处是换 CameraX 版本不会编译失败。
 *
 * 三件事：
 *   ① 探测：ImageCapture.getImageCaptureCapabilities(cameraInfo)
 *            → supportedOutputFormats 里含 RAW / RAW_JPEG
 *   ② 配置：ImageCapture.Builder.setOutputFormat(RAW_JPEG)
 *   ③ 拍摄：takePicture(rawOptions, jpegOptions, executor, callback)
 *
 * 选 RAW_JPEG 而不是 RAW：
 *   只出 DNG 的话相册里没有可直接预览的图（多数图库不认 DNG 缩略图），
 *   而且 25~50MB 的 DNG 解码很慢。双输出同时拿到一张 JPEG 用于预览/分享，
 *   一张 DNG 用于后期，代价只是多写一次卡 —— 这是专业相机的通行做法。
 */
private object RawMode {
    //
    // ★★方法查找必须"遍历"而不能"精确匹配"★★
    //
    // CameraX 是 Kotlin 写的。Kotlin 的 companion object 方法如果没有
    // @JvmStatic，在 Java 反射里【不在】ImageCapture 类上，
    // 而在 ImageCapture.Companion 上（且是实例方法）。
    // 用 getDeclaredMethod(name, ...) 精确匹配会直接抛 NoSuchMethodException，
    // 于是被判成"CameraX 版本过低" —— 明明 1.5.0+ 却报不支持。
    //
    // 所以这里：按【名字】遍历方法，不校验签名；调用时再保证参数个数对得上。
    //
    private fun findMethod(cls: Class<*>, name: String, arity: Int): java.lang.reflect.Method? {
        return try {
            cls.declaredMethods.firstOrNull {
                it.name == name && it.parameterTypes.size == arity
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 在类本身、以及它挂着的【所有 Kotlin object 单例】上找同名方法。
     *
     * ⚠️ 不能只认名为 Companion 的字段：
     *    如果 companion object 被命名为别的（companion object Foo），
     *    字段名就变成 Foo 而不是 Companion；顶层 object 则是 INSTANCE。
     *    所以这里遍历所有 static 字段，凡像是单例的都试一遍。
     */
    private fun findAnywhere(cls: Class<*>, name: String, arity: Int): Pair<java.lang.reflect.Method?, Any?> {
        findMethod(cls, name, arity)?.let { return it to null }
        return try {
            for (f in cls.declaredFields) {
                if (!java.lang.reflect.Modifier.isStatic(f.modifiers)) continue
                val fn = f.name
                if (fn != "Companion" && fn != "INSTANCE" && !fn.endsWith("Companion")) continue
                f.isAccessible = true
                val inst = f.get(null) ?: continue
                val m = findMethod(inst.javaClass, name, arity) ?: continue
                return m to inst
            }
            null to null
        } catch (e: Exception) {
            null to null
        }
    }

    private val capsLookup: Pair<java.lang.reflect.Method?, Any?> =
        findAnywhere(ImageCapture::class.java, "getImageCaptureCapabilities", 1)
    private val mCaps: java.lang.reflect.Method? = capsLookup.first

    private val mSetOutputFormat: java.lang.reflect.Method? =
        findAnywhere(ImageCapture.Builder::class.java, "setOutputFormat", 1).first

    //
    // ★双输出 takePicture 要按【参数类型】挑，不能只按参数个数★
    //   ImageCapture 上有好几个 takePicture 重载：
    //     2 参 (Executor, OnImageCapturedCallback)
    //     3 参 (OutputFileOptions, Executor, OnImageSavedCallback)
    //     4 参 (OutputFileOptions, OutputFileOptions, Executor, OnImageSavedCallback) ← 1.5 新增
    //   declaredMethods 的返回顺序【不保证】，firstOrNull 可能先撞到别的重载，
    //   所以这里遍历所有同名方法，按类型精确匹配。
    //
    private val mTakeDual: java.lang.reflect.Method? = run {
        val want = listOf(
            ImageCapture.OutputFileOptions::class.java,
            ImageCapture.OutputFileOptions::class.java,
            java.util.concurrent.Executor::class.java,
            ImageCapture.OnImageSavedCallback::class.java
        )
        ImageCapture::class.java.declaredMethods.firstOrNull { m ->
            m.name == "takePicture" &&
                    m.parameterTypes.size == 4 &&
                    m.parameterTypes.toList() == want
        }
    }

    /** 双输出 takePicture 找没找到（参数类型完全匹配才算） */
    private fun takeDualMatches(): Boolean = mTakeDual != null

    /**
     * ★探测【实际打包进 APK 的】CameraX 版本★
     *
     * AndroidX 库会在 APK 的 META-INF 下放 androidx.{库}.version 文件，
     * 内容就是版本号。读它比翻 build.gradle 更可靠 ——
     * 能直接暴露"以为升了、其实没升（或被别的依赖拉回旧版）"的情况。
     *
     * ⚠️ 只用于诊断和提示，读不到返回 null，不影响任何功能。
     */
    fun detectVersion(context: Context): String? {
        return try {
            val apk = context.applicationInfo.sourceDir ?: return null
            java.util.zip.ZipFile(apk).use { zf ->
                val hit = zf.entries().asSequence().firstOrNull { e ->
                    val n = e.name
                    n.startsWith("META-INF/androidx.camera") && n.endsWith(".version")
                } ?: return null
                zf.getInputStream(hit).bufferedReader().readText().trim().ifEmpty { null }
            }
        } catch (e: Exception) {
            null
        }
    }

    /** CameraX 侧有没有 RAW 相关 API（等价于 CameraX >= 1.5.0） */
    val apiAvailable: Boolean
        get() = mCaps != null && mSetOutputFormat != null && takeDualMatches()

    /** 三个 API 各自的查找情况，专供排查"为什么判成不支持" */
    fun apiDetail(): String =
        "getImageCaptureCapabilities=${mCaps != null} " +
                "setOutputFormat=${mSetOutputFormat != null} " +
                "takePicture(4参)=${takeDualMatches()}（找到=${mTakeDual != null}）"

    /**
     * 这颗头支不支持 RAW 输出。
     *
     * ⚠️ 探测的是【CameraX 综合结论】，不是单纯看 HAL ——
     *    有的机器 HAL 声明了 RAW_SENSOR 尺寸，但 CameraX 因为
     *    分辨率 / 帧率组合约束仍然不支持，这时以 CameraX 的结论为准，
     *    否则会出现"切过去了但拍出来是 JPEG"的假象。
     */
    fun supportsRaw(cameraInfo: CameraInfo?): Boolean {
        if (cameraInfo == null) return false
        val m = mCaps ?: return false
        return try {
            m.isAccessible = true
            val caps = m.invoke(capsLookup.second, cameraInfo) ?: return false
            val raw = readSupportedFormats(caps)
            if (raw == null) {
                Log.w("Camera", "RAW 探测：取不到 supportedOutputFormats，按不支持算")
                return false
            }
            raw.any { it == CX_OUTPUT_FORMAT_RAW || it == CX_OUTPUT_FORMAT_RAW_JPEG }
        } catch (e: Exception) {
            Log.w("Camera", "RAW 能力探测失败", e)
            false
        }
    }

    /**
     * 从 capabilities 对象上读 supportedOutputFormats。
     *
     * ⚠️ 三种坑都要防：
     *   ① 方法可能定义在【接口 / 父类】上 —— getDeclaredMethod 找不到
     *      → 遍历 类 + 父类 + 所有接口
     *   ② 返回类型可能是 Set<Int> / IntArray / Array<Int>，
     *      直接 as? Collection<*> 会 null → 被误判成不支持
     *   ③ 属性有可能是 public field 而不是 getter
     */
    private fun readSupportedFormats(caps: Any): Set<Int>? {
        val names = listOf("getSupportedOutputFormats", "supportedOutputFormats")
        val out = LinkedHashSet<Int>()
        for (n in names) {
            val mm = findInHierarchy(caps.javaClass, n, 0)
            if (mm != null) {
                return try {
                    mm.isAccessible = true
                    toIntSet(mm.invoke(caps))
                } catch (e: Exception) {
                    null
                }
            }
        }
        // 兜底：直接读字段
        return try {
            val f = caps.javaClass.declaredFields.firstOrNull {
                it.name == "supportedOutputFormats"
            }
            if (f != null) {
                f.isAccessible = true
                toIntSet(f.get(caps))
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun findInHierarchy(cls: Class<*>?, name: String, arity: Int): java.lang.reflect.Method? {
        var c = cls
        while (c != null) {
            c.declaredMethods.firstOrNull { it.name == name && it.parameterTypes.size == arity }
                ?.let { return it }
            for (itf in c.interfaces) {
                try {
                    itf.declaredMethods.firstOrNull { m ->
                        m.name == name && m.parameterTypes.size == arity
                    }?.let { return it }
                } catch (e: Exception) {
                }
            }
            c = c.superclass
        }
        return null
    }

    private fun toIntSet(v: Any?): Set<Int>? {
        return when (v) {
            is IntArray -> v.toSet()
            is Collection<*> -> v.mapNotNull { (it as? Number)?.toInt() }.toSet()
            is Array<*> -> v.mapNotNull { (it as? Number)?.toInt() }.toSet()
            else -> null
        }
    }

    /**
     * 设 RAW 输出格式。single = 只出 DNG；否则 DNG + JPEG 双出。
     * ★小米建议 single★：双输出要同一次 capture 走 RAW_SENSOR + JPEG 两条流，
     * 小米部分 HAL 会截断 RAW buffer。代价是相册没有可预览的 JPEG。
     */
    fun applyRawFormat(builder: ImageCapture.Builder, single: Boolean): Boolean {
        if (!apiAvailable) return false
        return try {
            val m = mSetOutputFormat ?: return false
            m.isAccessible = true
            m.invoke(
                builder,
                if (single) CX_OUTPUT_FORMAT_RAW else CX_OUTPUT_FORMAT_RAW_JPEG
            )
            true
        } catch (e: Exception) {
            Log.w("Camera", "设置 RAW 输出格式失败", e)
            false
        }
    }

    /** 只出 DNG 的单输出拍摄（小米备选路径）：3 参 takePicture */
    fun takeRawOnly(
        ic: ImageCapture,
        rawOptions: ImageCapture.OutputFileOptions,
        executor: java.util.concurrent.Executor,
        callback: ImageCapture.OnImageSavedCallback
    ): Boolean {
        return try {
            ic.takePicture(rawOptions, executor, callback)
            true
        } catch (e: Exception) {
            Log.w("Camera", "单输出 RAW 拍摄失败", e)
            false
        }
    }

    /**
     * RAW + JPEG 双输出拍摄。
     * ⚠️ 回调会来【两次】（一次 DNG、一次 JPEG），调用方要能区分，
     *    否则相册里会出现两张重复的缩略图。
     * @return false = 没走成 RAW 路径，调用方应退回普通单 JPEG 拍摄
     */
    fun takeRawAndJpeg(
        ic: ImageCapture,
        rawOptions: ImageCapture.OutputFileOptions,
        jpegOptions: ImageCapture.OutputFileOptions,
        executor: java.util.concurrent.Executor,
        callback: ImageCapture.OnImageSavedCallback
    ): Boolean {
        if (!takeDualMatches()) return false
        return try {
            val m = mTakeDual ?: return false
            m.isAccessible = true
            m.invoke(ic, rawOptions, jpegOptions, executor, callback)
            true
        } catch (e: Exception) {
            Log.w("Camera", "RAW 拍摄调用失败，退回 JPEG", e)
            false
        }
    }

    /**
     * 取 OutputFileResults 的图像格式（getImageFormat() 是 1.5.0 新增）。
     * 取不到返回 null —— 调用方用文件名后缀兜底判断是哪一路。
     */
    fun imageFormatOf(results: ImageCapture.OutputFileResults): Int? {
        return try {
            results.javaClass.getDeclaredMethod("getImageFormat").invoke(results) as? Int
        } catch (e: Exception) {
            null
        }
    }

    /**
     * HAL 层兜底探测：这颗头有没有声明 RAW_SENSOR 输出尺寸。
     * 只用于日志说明"到底是 CameraX 不支持还是 HAL 不支持"，不参与判定。
     */
    /** 探测结果：三个判据分别留着，方便排查到底哪一层说不支持 */
    data class RawProbeResult(
        val supported: Boolean,
        /** CameraX 的结论 */
        val cameraX: Boolean,
        /** HAL 有没有声明 RAW_SENSOR 输出尺寸 */
        val hal: Boolean,
        val detail: String
    )

    /**
     * ★绑定前探测★
     *
     * 用 ProcessCameraProvider.availableCameraInfos 直接问，不依赖已经绑好的 camera。
     *
     * ⚠️ 这比"绑定后用 camera.cameraInfo 探"正确得多：
     *    输出格式必须在【建 ImageCapture 用例时】就定好，
     *    绑完再探就晚了 —— 那一轮绑定根本没设 RAW，
     *    要等下一次重绑（切镜头 / 改设置）才生效。
     *
     * ★★判定取【CameraX 或 HAL】任一为真★★
     *    原来只信 CameraX，于是出现"原厂相机能拍 RAW，本软件却说不支持"：
     *    CameraX 的 supportedOutputFormats 是【静态能力 + 内部额外条件】的综合结论，
     *    有些机器（尤其国产定制 ROM）它算不出 RAW，但 HAL 明明声明了 RAW_SENSOR
     *    —— 原厂相机走的就是 HAL 那条路，所以能拍。
     *    既然 HAL 声明了，就说明硬件真能出 RAW，值得一试：
     *    真设不上的话 applyRawFormat 会失败，自动退回 JPEG，不会崩。
     *
     * @param provider 已经 getInstance().get() 出来的 provider
     * @param lensFacing 要探的朝向（前后摄能力不同，不能混用）
     */
    fun probeBeforeBind(
        provider: ProcessCameraProvider,
        context: Context,
        lensFacing: Int
    ): RawProbeResult {
        val hal = halDeclaresRaw(context, lensFacing)
        val ver = detectVersion(context)
        val verText = ver?.let { "当前 CameraX $it" } ?: "CameraX 版本未知"
        if (!apiAvailable) {
            Log.w("Camera", "★RAW 不可用：$verText（需 1.5.0+）。${apiDetail()}")
            return RawProbeResult(
                supported = false, cameraX = false, hal = hal,
                detail = "CameraX 版本过低：$verText，需 1.5.0+。${apiDetail()}"
            )
        }
        return try {
            val infos = provider.availableCameraInfos
            var checked = 0
            var ok = false
            for (info in infos) {
                // 只探这个朝向的头。getLensFacing() 偶发返回 null，
                // 那种情况就不过滤，全探一遍更稳妥。
                val f = info.lensFacing
                if (f != null && f != lensFacing) continue
                checked++
                if (supportsRaw(info)) {
                    ok = true
                    break
                }
            }
            val final = ok || hal
            RawProbeResult(
                supported = final, cameraX = ok, hal = hal,
                detail = "检查 $checked 颗头；CameraX=$ok HAL=$hal API[${apiDetail()}]"
            )
        } catch (e: Exception) {
            Log.w("Camera", "RAW 能力探测异常", e)
            RawProbeResult(
                supported = hal, cameraX = false, hal = hal,
                detail = "探测异常 ${e.message}；退而按 HAL=$hal 判定"
            )
        }
    }

    fun halDeclaresRaw(context: Context, lensFacing: Int): Boolean {
        return try {
            val manager = DeviceCompatibility.cameraManager(context)
            for (id in manager.cameraIdList) {
                val chars = try {
                    manager.getCameraCharacteristics(id)
                } catch (e: Exception) {
                    continue
                }
                if (chars.get(CameraCharacteristics.LENS_FACING) != lensFacing) continue
                val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?: continue
                val sizes = map.getOutputSizes(android.graphics.ImageFormat.RAW_SENSOR)
                if (!sizes.isNullOrEmpty()) return true
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}

private const val FALLBACK_MAX_ZOOM = 8.0f

/**
 * ★照片模式的数码变焦上限 = 50x★
 * HAL 通常只上报 10x / 20x / 30x，超过它 CameraX 会把 zoomRatio clamp 回去。
 * 所以这里拆成两段：
 *   · HAL 上限以内 → 正常 setZoomRatio（画质最好）
 *   · HAL 上限以外 → 预览缩放 + 拍照时按同比例中心裁切（扩展数码变焦）
 * 这样"最高 50x"在任何机器上都能点到，且预览与成片一致。
 */
private const val MAX_PHOTO_ZOOM = 50.0f

/**
 * ★录像模式的数码变焦上限 = 15x★
 * 视频实时编码，数码变焦再往上画质崩得厉害，而且视频没法像照片那样后期裁切，
 * 所以直接按 15x 截断（HAL 上限更低时以 HAL 为准）。
 */
private const val MAX_VIDEO_ZOOM = 15.0f

/** 预览 / 拍照时的目标帧率。锁成 30-30 让 AE 不再拖帧，预览更稳、也减少拖影噪点 */
private const val DEFAULT_PREVIEW_FPS = 30

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
 * 对称变焦曲线：放大和缩小的速度手感必须一致。
 * 老实现两处不对称：① APPLE_EASE 是"起步猛冲+长尾"的非对称曲线，反向用完全变形
 * ② 线性插值 r = a+(b-a)*t 让人眼觉得缩小前段飞快、后段拖沓（感知的是比例不是差值）。
 * 现在：缓动换 FastOutSlowInEasing（近似对称），插值改到对数域
 * r = exp(lerp(ln a, ln b, t)) —— 每秒变化的倍率比例恒定，a/b 对调后曲线完全镜像。
 */
private val SYMMETRIC_ZOOM_EASE = FastOutSlowInEasing

/**
 * 对数域的跨度系数：每跨 ln 倍（约 2.718x）省下的时长(ms)。
 * 时长只跟【倍率跨度】有关，不跟方向有关 —— 这也是对称的一部分。
 */
private const val ZOOM_ANIM_SPAN_FACTOR_LOG = 120f

/**
 * 变焦卡顿的真正元凶是【Compose 重组】，不是 HAL。
 * displayedRatio 是 mutableFloatStateOf，在变焦循环里每步一写 →
 * 触发整个 CameraApp（含 PreviewView、所有按钮、所有动画）重组，
 * 34 步 = 34 次全屏重组，主线程被占满，中间帧全丢，看起来就像"硬切"。
 *
 * 解法：HAL 下发频率 与 UI 刷新频率解耦 ——
 * 给相机下发变焦很便宜（改个 crop region，可到 60fps）；
 * Compose 重组很贵，数字刷新到 ~14fps 肉眼就够。
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

/** 录制中恢复防抖前的等待：编码器也在跑，给足时间，避免恢复的瞬间又顿一下 */
private const val STAB_RESTORE_DELAY_MS_REC = 600L

/**
 * ★开录后等多久再锁白平衡★
 * 太短：AWB 没收敛完就锁，整段视频都带着那个偏色
 * 太长：开头一段仍然会飘
 * 400ms 是实测比较合适的点（多数机器 AWB 在 200~350ms 内收敛到位）
 */
private const val VIDEO_AWB_LOCK_DELAY_MS = 400L

/**
 * ★4K60 专用的、更长的 AWB 收敛等待★
 *
 * 4K60 是相机流水线的极限负载：传感器 readout、ISP、编码器全部跑满，
 * 3A（尤其是 AWB）的收敛周期会被明显拉长 ——
 * 实测部分机器在 4K60 下 AWB 要 800~1200ms 才收敛到位，
 * 而通用延时只有 400ms，于是【锁在了一个还在漂移的中间值上】。
 * 后面 HAL 继续收敛，增益每帧都在动 → 录出来就是一阵一阵的色彩闪烁。
 *
 * 所以 4K60 单独给一个更长的等待，宁可开头几百毫秒飘一点，
 * 也不要整段视频都锁错。
 */
private const val VIDEO_AWB_LOCK_DELAY_MS_4K60 = 1200L

/** 录制中把倍率刷新放宽到 ~8fps，把主线程让给编码器（性能优化） */
private const val ZOOM_UI_REFRESH_MS_REC = 120L

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
 * ★版本号 / 版本码★
 *
 * ⚠️ 这里的 APP_VERSION_CODE 必须 >= 服务器 update.json 里的 versionCode，
 *    否则"已是最新版"的判断会反过来，每次启动都提示更新（见 AboutScreen 里的说明）。
 * 发新版时改这两处，并且【记得同步 app/build.gradle 里的 versionName / versionCode】。
 */
private const val APP_VERSION_NAME = "v3.2.7"
private const val APP_VERSION_CODE = 11

/**
 * ★应用显示名★
 * 关于页顶部那一行。改这一处即可，不用去 strings.xml。
 * ⚠️ 桌面图标下面显示的名字在 AndroidManifest.xml / strings.xml 里，跟这个不是一回事。
 */
private const val APP_DISPLAY_NAME = "Silky Camera"

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
            // 读不到就用代码里的兜底值，至少不会误判成"有更新"
            Log.w("Update", "读取当前版本失败，用代码内的版本码兜底", e)
            APP_VERSION_CODE.toLong()
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
            Log.d(
                "Update",
                "版本文件: versionCode=${info.versionCode}, versionName=${info.versionName}, " +
                        "apkUrl=${info.apkUrl}"
            )
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

/**
 * 持久化预热：首次 getSharedPreferences() 是【同步阻塞】的
 * （要 awaitLoadedLocked 等磁盘 XML 解析完），低端机 + 磁盘忙时可达几百毫秒，
 * 而 onCreate 和 Compose 的 remember 初始值都在主线程直接读。
 * 做法：onCreate 最早处起后台线程提前 load 完，把阻塞从主线程挪走。
 */
object PrefsWarmUp {
    @Volatile
    private var started = false

    fun start(context: Context) {
        if (started) return
        started = true
        Thread {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
                // 只是"打开"这些 SP，触发各自的磁盘加载；读什么值无所谓
                context.getSharedPreferences("silky_device_profile", Context.MODE_PRIVATE).all
                context.getSharedPreferences("camera_settings", Context.MODE_PRIVATE).all
                context.getSharedPreferences("silky_zoom_stops", Context.MODE_PRIVATE).all
                context.getSharedPreferences("silky_lens_color", Context.MODE_PRIVATE).all
                context.getSharedPreferences("silky_star_trail", Context.MODE_PRIVATE).all
                Log.d("SilkyCamera", "持久化预热完成")
            } catch (e: Exception) {
                Log.w("SilkyCamera", "持久化预热失败（不影响主流程）", e)
            }
        }.apply { name = "prefs-warmup"; start() }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ★最先起：把 SP 的磁盘加载挪到后台线程，主线程后面读就是快速路径
        PrefsWarmUp.start(this)
        DeviceCompatibility.logInfo()

        // ★恢复用户选过的机型档案★（首次启动会弹窗让用户选，选完存在这里）
        // 必须在 setContent 之前：档案决定了变焦档位、直连策略、收敛等待
        ActiveDevice.apply(DeviceProfileStore.load(this))
        LensColorUnifier.enabled = DeviceProfileStore.colorUnify(this)
        LensColorUnifier.lockAwb = DeviceProfileStore.lockAwb(this)
        ActiveDevice.fastLensSwitch = DeviceProfileStore.fastLensSwitch(this)
        LensColorUnifier.load(this)
        Log.d("SilkyCamera", "启动机型：${SeriesDb.describe(ActiveDevice.profile)}")

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

        // 界面方向。
        // ★UI 锁定竖屏★ 打开 LOCK_UI_PORTRAIT 后恒定竖屏：手机横过来界面不转、不重排，
        // 只有文字转正（角度由 CameraApp 里的传感器采样算出）。
        // 关掉就恢复成跟随传感器，由设置栏的"旋转"开关决定跟系统还是跟重力。
        requestedOrientation = if (LOCK_UI_PORTRAIT) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
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

    // 横竖屏。横屏时变焦控件和模式栏要挪到屏幕两边，别糊在取景框正中间
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    /**
     * ★横屏：只旋转文字，不重排 UI★
     *
     * UI 布局恒定走竖屏那套（顶部设置栏、底部模式栏 + 快门组），横屏也不改成左右分栏。
     * 想恢复以前的左右分栏，把 [LANDSCAPE_RELAYOUT] 改回 true 即可 —— 所有布局分支
     * 读的都是这个变量，一处开关全量生效。
     */
    val relayoutLandscape = isLandscape && LANDSCAPE_RELAYOUT

    /**
     * ★文字补偿旋转角★
     *
     * UI 被锁成竖屏后，它不再跟着屏幕转 —— 手机横过来时整个竖屏界面侧躺在屏幕上，
     * 文字也跟着侧躺。这里给文字一个反向旋转，让它重新正过来。
     *
     * 角度取设备物理旋转角的相反数：OrientationEventListener 报 90 表示设备顺时针转了 90°，
     * 界面也就顺时针侧躺 90°，所以文字要逆时针转 90° 才正。
     * 设备没转时是 0，等于不动，零副作用。
     */
    val deviceRotationDeg = rememberDeviceRotationDeg(context)
    val labelRotationDeg = if (LOCK_UI_PORTRAIT) {
        TEXT_ROTATION_SIGN * deviceRotationDeg
    } else 0f

    /**
     * ★文字旋转动画★
     * 手机转动时角度会从 0 直接跳到 ±90，不加动画的话文字是"啪"地一下翻过来，很生硬。
     *
     * 另外做了【最短路径归一化】：直接对 0→270 做插值会绕一大圈（270°），
     * 归一化到 -180~180 之后就是 -90°，走的是最短的那一侧，转起来才自然。
     */
    val shortestLabelRotation: Float = labelRotationDeg.let {
        var d = it % 360f
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        d
    }
    val labelRotationAnim by animateFloatAsState(
        targetValue = shortestLabelRotation,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "labelRotation"
    )

    /**
     * ★竖向空间紧张★：横屏但沿用竖屏布局时，屏幕高度只有竖屏时的一半多点，
     * 底部那一坨（变焦轮盘 + 模式栏 + 快门）按竖屏的间距会顶出屏幕。
     * 这里统一把各处 padding / 间距收紧一档。
     */
    val uiCompact = isLandscape && !LANDSCAPE_RELAYOUT

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

    //
    // ★★机型档案版本号（deviceEpoch）★★
    //
    // 修「选完机型后卡住」的核心。
    // 绑定相机的 LaunchedEffect 以 (settingsLoaded, lensFacing, highResMode, photoRatio,
    // videoQuality, videoFrameRate, boundLensId) 为 key —— 里面【没有任何一项】
    // 跟"机型档案"有关。而 boundLensId 在常态下本来就是 null，
    // 选完机型把它设为 null 等于没变 → key 不变 → LaunchedEffect 根本不会重跑。
    //
    // 后果是 onConfirm 里那一串重置全部悬空，没人来收尾：
    //   · lensSwitching = true  → 黑遮罩没人收（只有 4 秒看门狗兜底，表现为黑屏 4 秒）
    //   · scanDone = false      → 永远停在"扫描中"
    //   · hasCalibrated = false → 变焦标定永远不执行，档位映射失效
    //   · ZoomStopsStore 已清空 → 却没人重新探测存档，下次启动档位又变
    // 表现就是用户说的"选完机型后卡住了"。
    //
    // 解法：加一个自增计数器作为 key 的一员，选完机型就 +1，强制 LaunchedEffect 重跑。
    //
    var deviceEpoch by remember { mutableIntStateOf(0) }

    // ── 物理摄像头直连 ──
    // 小米 / iQOO 上逻辑摄像头的 zoomState 常只报 min=1.0，setZoomRatio(0.6) 被 clamp 回 1.0，
    // 超广角永远切不过去。这时改为直接绑定那颗物理摄像头，等于原厂 App 的"切镜头"。
    var boundLensId by remember { mutableStateOf<String?>(null) }
    /**
     * ★当前是否通过 CaptureRequest.PHYSICAL_CAMERA_ID 切到物理头★
     * true 时相机会话仍然是逻辑摄像头，只是每帧指定出图的物理头 ——
     * 不 unbind/rebind，预览不闪、比例不跳。
     */
    var boundViaRequest by remember { mutableStateOf(false) }
    /** 上一次是不是通过 request 切的（退回逻辑头时也避免无谓重绑） */
    var lastBoundWasViaRequest by remember { mutableStateOf(false) }
    /** PHYSICAL_CAMERA_ID 这条路是否可用（出过异常就关掉，退回重新绑定） */
    var physicalRequestWorks by remember { mutableStateOf(true) }
    /** 正在切换物理镜头（重绑）中。用来遮住重绑瞬间预览比例的跳变 */
    var lensSwitching by remember { mutableStateOf(false) }
    /**
     * ★超广角专用：物理直连是否真的能出画面★
     * 跟 [physicalSwitchWorks]（整条物理直连链路）分开记 ——
     * 广角头出不了画面不代表长焦头也不行，不该一刀切把两条路都封死。
     */
    var physicalWideWorks by remember { mutableStateOf(true) }
    // 真实可绑定的物理头列表（由 probePhysicalCameras 独立扫描，保证是真 id）
    var physicalLensOptions by remember {
        mutableStateOf<List<DeviceCompatibility.LensProfile>>(emptyList())
    }
    // 本机允不允许单独打开某个摄像头 id。失败一次就永久关掉这条路径，退回数码变焦
    var physicalSwitchWorks by remember { mutableStateOf(true) }
    // 物理头绑定完成后要落到哪个"全局倍率"
    var pendingZoomTarget by remember { mutableFloatStateOf(0f) }
    /** 用户请求的、超过 HAL 上限的目标倍率（0 = 当前没有扩展数码变焦） */
    var boostTarget by remember { mutableFloatStateOf(0f) }
    // ★逻辑摄像头到不了广角★ HAL 把 minZoomRatio 报成 1.0，但本机确实有 0.6x 光学档。
    // 置 true 后，所有 <1.0 的目标一律走物理直连，不再浪费一次无效下发。
    var logicWideBlocked by remember { mutableStateOf(false) }
    // ★逻辑摄像头到不了长焦★ 真我 GT5 Pro / iQOO 10 Pro 等机型：
    // setZoomRatio(3x) 不报错、回读倍率也到位，但画面一直是主摄裁切。
    // 置 true 后，>=1.8x 的目标会去直连真实的长焦物理头。
    var telePreferPhysical by remember { mutableStateOf(DeviceCompatibility.isKnownTeleStubborn) }
    /** 长焦头绑定失败计数：达到 [TELE_BIND_FAIL_LIMIT] 才真正禁用，避免一次偶发失败就永久封死 */
    var teleBindFailCount by remember { mutableIntStateOf(0) }
    /**
     * ★切镜头（物理头直连/重绑）连续失败计数★
     * 达到 [LENS_SWITCH_FAIL_LIMIT] 就彻底关闭长焦直连 ——
     * 防抖开着时反复失败会形成"每次点都卡一秒"的慢性卡死，必须有上限。
     */
    var lensSwitchFailCount by remember { mutableIntStateOf(0) }
    /**
     * ★长焦专用：物理直连长焦是否可用★
     * 与 [physicalWideWorks]（广角）、[physicalSwitchWorks]（整条链路）分开记 ——
     * 长焦头绑不上不代表广角头也不行，不该一刀切把两条路都封死。
     */
    var physicalTeleWorks by remember { mutableStateOf(true) }

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

    /**
     * ★构图辅助线（三分线 + 水平仪）开关★
     * 默认【开】：这是摄影最常用的参照，多数人一直需要。
     * 可在「菜单 → 其他」里关掉。
     * ⚠️ 它只是画在预览上层，不会进视频也不会进照片。
     */
    var showGuides by remember { mutableStateOf(true) }

    /**
     * ★UI隐藏 总开关★（菜单 → 其他 → UI隐藏）
     * false（默认）= 双击空白不生效，避免误触。
     * 打开后：拍摄界面双击空白处隐藏所有 UI，再双击恢复。
     * 会随其它设置一起持久化。
     */
    var uiHideEnabled by remember { mutableStateOf(false) }

    /**
     * ★UI 当前是否处于"隐藏"状态★（双击切换的那个瞬时状态）
     *
     * ⚠️ 与 uiHideEnabled 是两回事：
     *   uiHideEnabled = 功能开关（菜单里那个）
     *   uiHidden      = 此刻 UI 藏没藏（双击切的是这个）
     *
     * ★只在本次会话有效，不持久化★：
     *   下次打开相机一定是"UI 正常显示"，否则用户忘了自己藏过 UI，
     *   一打开看到光秃秃的画面会以为相机坏了。
     */
    var uiHidden by remember { mutableStateOf(false) }

    /**
     * ★上一次在【录制中】实际下发出去的参数包签名★
     *
     * 用途：录制中若本次 bundle 与上次【完全相同】，就跳过提交。
     * 见 applySceneParams 里 ⑨ 的说明 —— 这是"既允许调参数、又不闪色"的关键。
     */
    var lastRecSceneSig by remember { mutableStateOf("") }
    /** 专业模式录制中的参数签名（用途同 [lastRecSceneSig]，两条路径各记一份） */
    var lastProRecSig by remember { mutableStateOf("") }

    /**
     * ★录制期间有参数改动被"合并"挂起★（仅用于日志与结束时的补发提示）
     * 现在不再阻止下发 —— 参数随时可调，见 applySceneParams 里 ⑨。
     */
    var sceneParamsDirty by remember { mutableStateOf(false) }

    /**
     * ★拍照瞬间的"画质优先"标志★
     * true 时 applySceneParams 会用 HIGH_QUALITY 降噪 + 锐化 + STILL_CAPTURE intent，
     * 让成片干净锐利；拍完自动恢复成"流畅优先"的预览参数。
     * 见 applySceneParams 里 ②③ 的说明。
     */
    var captureQualityBoost by remember { mutableStateOf(false) }

    /**
     * 设备姿态（水平仪数据源）
     *   first  = 屏幕坐标系下的真实地平线角度（度）
     *   second = 是否【镜头朝地面】（俯拍），是则自动隐藏水平仪
     */
    val (horizonDeg, facingGround) = rememberDeviceTilt(context, lowPower = isRecording)

    /**
     * ★已录制秒数★（时间码用）
     * 只在录制中走，用 SystemClock.elapsedRealtime 而不是 currentTimeMillis ——
     * 后者会受用户改系统时间影响，导致计时跳变甚至倒退。
     */
    var recordedSeconds by remember { mutableLongStateOf(0L) }
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
        Log.d(
            "Update",
            "本地=$APP_VERSION_NAME(code=$current) | " +
                    "服务器=${info.versionName}(code=${info.versionCode}) | 已跳过=$skipped"
        )
        when {
            // ★版本号字符串相同 = 已经是最新版★
            // 发版时只改了 APP_VERSION_NAME、忘了同步 build.gradle 的 versionCode，
            // 判断又只比 versionCode，结果就是"装的明明是最新版，却一直弹更新"。
            // 这里用名字兜底：名字对得上就认定是最新版，直接不弹。
            info.versionName.isNotBlank() &&
                    info.versionName.trim().equals(APP_VERSION_NAME.trim(), ignoreCase = true) -> {
                Log.d("Update", "不弹窗：版本号与服务器一致（$APP_VERSION_NAME），已是最新版")
            }
            info.versionCode <= current ->
                Log.d("Update", "不弹窗：服务器版本号不大于本地，请把 JSON 的 versionCode 调大")
            info.versionCode == skipped ->
                Log.d("Update", "不弹窗：用户已跳过此版本（卸载重装或发新版本可重置）")
            else -> updateInfo = info
        }
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var mode by remember { mutableStateOf(CaptureMode.PHOTO) }

    // ── 首次启动：品牌 / 系列 ──
    var showDevicePicker by remember { mutableStateOf(!DeviceProfileStore.isOnboarded(context)) }
    var showDeviceTuning by remember { mutableStateOf(false) }
    /** 开源使用许可【全屏页面】 */
    var showOssLicense by remember { mutableStateOf(false) }
    var colorUnify by remember { mutableStateOf(DeviceProfileStore.colorUnify(context)) }
    var fastLensSwitch by remember { mutableStateOf(DeviceProfileStore.fastLensSwitch(context)) }

    // ── 星空 / 星轨 ──
    var starTrailConfig by remember { mutableStateOf(StarTrailStore.load(context)) }
    var showStarTrailSetup by remember { mutableStateOf(false) }
    var starTrailRunning by remember { mutableStateOf(false) }
    var starTrailDone by remember { mutableIntStateOf(0) }
    var starTrailTotal by remember { mutableIntStateOf(0) }
    var starTrailPreview by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var starTrailResult by remember { mutableStateOf<StarTrailResult?>(null) }
    // 停止标志用普通数组：拍摄协程在 IO 线程读它，不能走 Compose State
    val starTrailStop = remember { booleanArrayOf(false) }
    /**
     * ★星轨协程的引用★：给全局自愈看门狗用。
     * 光靠 starTrailRunning 无法区分"还在拍"和"协程已经没了但标志没复位"，
     * 有了这个 job 引用，看门狗就能准确判断。
     */
    var starTrailJobRef by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // ── 镜头色彩校准 ──
    var calibrating by remember { mutableStateOf(false) }
    var lockAwb by remember { mutableStateOf(DeviceProfileStore.lockAwb(context)) }

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
    /**
     * 拖动变焦的 HAL 下发去重基准。
     * 用普通数组而不是 State：它只在手势回调里读写，改它不需要触发重组。
     * 拖过 HAL 上限进入 15x 段后 clampForHal 的结果恒定，重复下发纯属浪费。
     */
    val lastDragHalSent = remember { floatArrayOf(-1f) }

    // ★不触发重组的标志位★
    // 用数组包一层，这样改它的值不会引发 Compose 重组（State 会）。
    // 变焦动画期间置 true，告诉轮询循环别来更新 displayedRatio，
    // 避免"动画在高频下发"和"轮询在高频刷新"两股力量打架。
    val zoomAnimating = remember { booleanArrayOf(false) }
    /**
     * 变焦世代号：修「变焦中点另一个档位会卡顿」的核心。
     * 旧实现只有一个共享布尔量 zoomAnimating，点新档位时旧协程的 finally
     * 会把新动画刚置上的保护清掉（竞态），轮询立刻插进来抢写 displayedRatio；
     * 且旧协程收尾的 applySceneParams 与新协程开头的撞车，HAL 短时间被重配两次。
     * 现在每次 smoothZoomTo 领一个号，finally 里只认"自己是最新一代"才收尾。
     */
    var zoomGeneration by remember { mutableIntStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }

    var highResMode by remember { mutableStateOf(false) }

    // ── RAW（DNG）格式 ──
    /** 用户的选择：是否以 RAW+JPEG 双输出拍摄 */
    var rawEnabled by remember { mutableStateOf(false) }
    /**
     * 本机到底支不支持 RAW。null = 还没探测过。
     * ⚠️ 必须在绑定【之后】才有值 —— 探测要用真实 cameraInfo，
     *    而且 CameraX 的结论与分辨率/帧率组合有关，拿不到 cameraInfo 时不能瞎猜。
     */
    var rawSupported by remember { mutableStateOf<Boolean?>(null) }
    /**
     * ★环境光照度（lux）★：驱动自动挡曝光策略。
     * -1 = 本机没有光照传感器，此时走默认档（不做任何放宽）。
     */
    val ambientLux = rememberAmbientLux()
    /** 按朝向缓存的 RAW 能力：前后摄能力不同，不能复用同一份结论 */
    var rawSupportByFacing by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }
    /**
     * ★强制 RAW★：忽略一切检测结果强行按 RAW 绑定。
     * 给"原厂能拍、本软件检测说不行"的机器留的逃生门 ——
     * 真设不上格式时 applyRawFormat 会失败并自动退回 JPEG，不会崩。
     */
    var rawForce by remember { mutableStateOf(false) }
    /**
     * ★RAW 单输出★：true = 只出 DNG，false = DNG + JPEG。
     * 小米数字系列默认开 —— 双输出在该系列上会截断 RAW buffer。
     */
    var rawSingleOutput by remember { mutableStateOf(DeviceCompatibility.isXiaomiDigitalSeries) }
    /** 上一次探测的详细判据，排查用（菜单里也能看到） */
    var rawProbeDetail by remember { mutableStateOf("") }
    /** ★实际打包进 APK 的 CameraX 版本★：RAW 不可用时提示里直接显示，省得翻 gradle */
    var cameraXVersion by remember { mutableStateOf<String?>(null) }
    var flashMode by remember { mutableIntStateOf(0) }
    // 点闪光灯后展开那一排图标，再点收起
    var flashExpanded by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showReward by remember { mutableStateOf(false) }

    // 拍照比例：3:4 / 16:9 / 全屏
    var photoRatio by remember { mutableStateOf(PhotoRatio.RATIO_3_4) }

    /** ★防抖模式★：默认光学防抖（OIS 不裁切画面，拍照录像都受益） */
    var stabMode by remember { mutableStateOf(StabMode.DEFAULT) }
    /**
     * ★上一次的拍摄模式★
     * 用来判断"是从哪个模式切出来的" —— 只有从专业 / 星空切出来才需要清参数，
     * 普通切换（拍照 ↔ 录像）清了反而会引发画面短暂不对。
     */
    var modeBeforeSwitch by remember { mutableStateOf(mode) }

    /** ★本机 4K 跑不跑得到 60fps★（null = 没探测过）。key 带 lensFacing：探测按朝向做，前后摄能力不同，不能复用 */
    var uhd60Supported by remember(lensFacing) { mutableStateOf<Boolean?>(null) }
    /** ★快门声★：拍照时是否播放系统快门音 */
    var shutterSound by remember { mutableStateOf(true) }
    /** ★左右滑动屏幕切换拍摄模式★（默认开，用户明确要求的功能） */
    var swipeModeEnabled by remember { mutableStateOf(true) }
    var videoQuality by remember { mutableStateOf(VideoQuality.FHD) }
    var videoFrameRate by remember { mutableStateOf(VideoFrameRate.FPS_30) }
    //
    // ── 苹果风格调色盘 ──
    var toneWarmth by remember { mutableFloatStateOf(0f) }
    var toneTint by remember { mutableFloatStateOf(0f) }
    var toneIntensity by remember { mutableFloatStateOf(100f) }
    var showColorPalette by remember { mutableStateOf(false) }
    /** 多级菜单：null=关闭，否则是"一级/二级"路径 */
    var menuPath by remember { mutableStateOf<String?>(null) }
    /**
     * ★装不下的状态栏按键★
     * 由 SettingsChipsRow / SettingsChipsColumn 量完可用空间后回填，
     * 这些按键不再画成 chip，改到菜单里出现（菜单键本身永不溢出）。
     */
    var hiddenChipIds by remember { mutableStateOf<Set<String>>(emptySet()) }
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

    /** 专业模式当前展开的参数（横屏要把模式栏换成参数面板，所以提升到这） */
    var proActiveParam by remember { mutableStateOf<ProParam?>(null) }
    // ★离开专业模式（切模式 / 切前后摄）时把面板收起来★
    // 否则参数面板会一直占着模式栏的位置，模式栏再也回不来。
    // ⚠️ 必须放在 isFront 声明【之后】：Kotlin 局部变量要先声明后使用，
    //    放到上面会报 Unresolved reference 'isFront'。
    LaunchedEffect(mode, isFront) {
        if (mode != CaptureMode.PRO || isFront) proActiveParam = null
    }

    val sessionMedia = remember { mutableStateListOf<CapturedMedia>() }
    var showGallery by remember { mutableStateOf(false) }

    // vivo 适配：变焦范围改成从 zoomState 实时读，不再写死 0.6x~10x。
    // X80 Pro 的 0.6x 超广角 / 5x 潜望能不能被 HAL 正常上报因机而异，读出来才准。
    var minZoomRatio by remember { mutableFloatStateOf(0.6f) }
    /** HAL 原生上报的变焦上限（不含本次新增的"扩展数码变焦"部分） */
    var halMaxZoomRatio by remember { mutableFloatStateOf(10.0f) }
    var maxZoomRatio by remember { mutableFloatStateOf(10.0f) }
    val minRatio = minZoomRatio
    val maxRatio = maxZoomRatio
    /** ★照片最高 50x、录像最高 15x★：模式决定 UI 上限 */
    // ★机型档案里还给了一个「可用上限」★：入门机拖到 50x 只会得到一团糊，
    // 所以照片模式再按档案夹一道（录像固定 15x，跟档案无关）。
    val desiredMaxZoom: Float = if (mode == CaptureMode.VIDEO) MAX_VIDEO_ZOOM
    else minOf(MAX_PHOTO_ZOOM, ActiveDevice.maxUsefulZoom())
    /**
     * ★扩展数码变焦★：超过 HAL 上限的那部分倍率。
     * 预览靠缩放实现、拍照靠中心裁切实现，两者同比例，所以取景与成片一致。
     * 物理头直连时不做（那颗头的本地域另有换算），避免叠加出错误倍率。
     */
    val digitalBoost: Float =
        if (boundLensId != null) 1f
        else (displayedRatio / halMaxZoomRatio.coerceAtLeast(0.001f)).coerceAtLeast(1f)

    /**
     * ★扩展数码变焦的预览缩放做一层平滑★
     *
     * 修「15x 切换卡顿」的观感部分：
     *   displayedRatio 在变焦过程中是【节流提交】的（非录制 ~72ms、录制 ~120ms 一次），
     *   直接拿它当 scaleX/scaleY，预览就会"一跳一跳"——
     *   15x 附近 digitalBoost 从 1.0 爬到 1.5，每 72ms 跳一格，肉眼就是一顿一顿的。
     * 这里用一段很短的线性动画把中间值补上，画面就连续了。
     * 时长取 80ms：略大于节流间隔，够填平台阶，又不会拖慢收尾。
     *
     * ⚠️ 只有预览用这个动画值；拍照裁切必须用 [digitalBoost] 的精确值，
     *    否则成片会裁错比例。
     */
    val digitalBoostAnim by animateFloatAsState(
        targetValue = digitalBoost,
        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
        label = "digitalBoostAnim"
    )

    // 原厂镜头参数：每次绑定相机时读取，变焦档位/标定/吸附都以它为准
    var lensProfiles by remember { mutableStateOf<List<DeviceCompatibility.LensProfile>>(emptyList()) }
    val defaultStops = ActiveDevice.stops().takeIf { it.isNotEmpty() }
        ?: listOf(0.6f, 1.0f, 2.0f, 5.0f, 10.0f)
    var zoomStops by remember { mutableStateOf(defaultStops) }
    // 真实物理头的倍率（光学档位），与按钮档位分开：
    // 按钮档位 = 光学档位 ∪ 常用档位，保证 2x 这类没有物理头的档位按钮依然存在
    var opticalStops by remember { mutableStateOf<List<Float>>(emptyList()) }
    // 探测失败时为 false，此时退回写死的档位，行为跟以前一致
    var zoomStopsFromHardware by remember { mutableStateOf(false) }
    /**
     * 变焦档位是否已经定下来。
     * 标定 / 吸附 / UI 都要等它 —— 否则会拿上一轮残留的默认值去算，
     * 而那会儿 min/max 可能还是 HAL 上报前的占位值（0.6 / 10.0），
     * 算出来的档位跟稳定后不一样，又是一次"按键变了"。
     */
    var zoomStopsReady by remember { mutableStateOf(false) }

    /**
     * 焦段取整：小米 13/14 Ultra 长焦实测 3.2x、努比亚 2.4x、华为 P50 Pro 3.5x，
     * 按钮上出现"3.2x"这种零头既不齐也没意义。
     * 规则：≤1.05x（超广角 0.5/0.6 和主摄 1.0）原样保留（广角档显示 0.6x 有意义）；
     * 其余四舍五入到整数，最小 2x。
     * ⚠️ 只作用于按钮档位与光学匹配；物理头直连仍用真实倍率 3.2，画质不打折。
     */
    fun normalizeStop(v: Float): Float {
        if (!v.isFinite() || v <= 0f) return v
        if (v <= 1.05f) return v
        return v.roundToInt().toFloat().coerceAtLeast(2f)
    }

    /**
     * 去掉冗余焦段：小米数字系列上真实超广角是 0.5x，但厂商配置还回报一个 0.6x，
     * 于是按钮上同时出现两个几乎一样的档位。
     * 规则：相邻档位相对差 < [STOP_MERGE_RATIO](25%) 就合并，保留【更广（更小）】的那个。
     * 校验：0.5/0.6 差 20%→合并留 0.5；0.6/1 差 66%、4/5 差 25%（边界）→都留。
     */
    val STOP_MERGE_RATIO = 0.25f

    fun dedupeStops(sorted: List<Float>): List<Float> {
        if (sorted.size <= 1) return sorted
        val out = ArrayList<Float>()
        for (v in sorted) {
            val last = out.lastOrNull()
            if (last == null) { out.add(v); continue }
            // 相对差以【较小的那个】为基准
            if ((v - last) / last < STOP_MERGE_RATIO) {
                Log.d("Camera", "冗余焦段 $last / $v 已合并，保留 $last")
                continue
            }
            out.add(v)
        }
        return out
    }

    /** 一整列档位取整 + 去重 + 排序 */
    fun normalizeStops(list: List<Float>): List<Float> =
        dedupeStops(list.map { normalizeStop(it) }.distinct().sorted())

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
        // 物理头的 id 在 physicalLensOptions 里，只查 lensProfiles 会漏掉长焦头，
        // 导致松手时永远吸不到 3x 光学档。
        val pool = (lensProfiles + physicalLensOptions)
            .filter { DeviceCompatibility.isRealCameraId(it.id) }
            .distinctBy { it.id }
        if (pool.isEmpty()) return ratio
        val best = pool.minByOrNull { abs(it.zoomFactor - ratio) } ?: return ratio
        return if (onThisLens(ratio, best.zoomFactor)) normalizeStop(best.zoomFactor) else ratio
    }

    val isUltraWide = currentRatio < 0.95f && minZoomRatio < 0.95f && !isFront
    // ★超广角滤镜已删除★
    // 原来这里有一个 ultraWidePaint（对比度 1.25 + 偏移 -18），只在 <0.95x 时通过
    // saveLayer 套到整个预览上。两个问题：
    //   ① 1.25*x - 18 会把所有低于 14/255 的像素直接压成纯黑，
    //      暗光环境下整幅画面几乎全黑 —— 表现就是"切到超广角后不显示画面"；
    //   ② saveLayer + ColorFilter 每帧开一层离屏缓冲，部分 GPU 上直接渲染成空层。
    // 现在预览只走 drawContent()，不再套任何滤镜。

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
     * 当前这颗头对应的色彩 key：直连物理头时用 cameraId，逻辑头时用档位倍率。
     * 用「档位」而不是实时倍率，是为了让 key 稳定 —— 3.0x 和 2.97x 必须落到同一个 key，
     * 否则校准数据会查不到。
     */
    fun lensColorKey(): String =
        LensColorUnifier.lensKeyOf(boundLensId, nearestStop(displayedRatio))

    // ---------------- 物理摄像头直连 ----------------

    /** 当前绑定物理头的光学倍率；未直连（用逻辑摄像头）时为 1.0 */
    fun boundLensFactor(): Float =
        (lensProfiles + physicalLensOptions)
            .filter { DeviceCompatibility.isRealCameraId(it.id) }
            .firstOrNull { it.id == boundLensId }?.zoomFactor ?: 1f

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
     * 真正下发给相机的倍率。
     *
     * ★为什么要单独截一次★：UI 上最高能点到 50x，但 CameraX 的 setZoomRatio 超过
     * HAL 上限会被静默 clamp 回来。所以：
     *   · HAL 上限以内 → 正常下发（画质最好）
     *   · HAL 上限以外 → 下发 HAL 上限，多出来的倍数走[扩展数码变焦]
     *     （预览缩放 + 拍照中心裁切）
     */
    fun clampForHal(global: Float): Float {
        val halCap = halMaxZoomRatio * (if (boundLensId != null) boundLensFactor() else 1f)
        val cap = halCap.coerceAtLeast(minRatio)
        return localZoomOf(global).coerceAtMost(cap)
    }

    /** 照片 50x / 录像 15x：把 HAL 上限截到本模式允许的最大值 */
    /**
     * ★UI 倍率上限★
     *
     * ⚠️ 这里曾经写成 `halMax.coerceAtMost(desiredMaxZoom)` —— 于是 HAL 只报 2.5x 的机器
     *    （ColorOS 对第三方 App 限制变焦范围，OPPO Find X7 就报 0.7~2.5），
     *    UI 上限直接变成 2.5x，50x 的扩展数码变焦**完全用不上**，
     *    表现就是"轮盘只有 0.7x 到 2.5x"。
     *
     * 现在固定取模式上限（照片 50x / 录像 15x）：
     *   · HAL 上限以内 → 原生变焦（画质最好）
     *   · 超出 HAL 上限 → 走[扩展数码变焦]（预览缩放 + 拍照同比例中心裁切）
     * HAL 原生上限单独记在 [halMaxZoomRatio]，供 [clampForHal] 判断用哪条路。
     */
    fun zoomCapFor(halMax: Float): Float = desiredMaxZoom

    /**
     * 承载某个全局倍率的物理头：不超过它、且最接近的那颗。
     * ★优先用 probePhysicalCameras 扫出来的真实 id 池★
     * lensProfiles 探测失败时装的是画像虚拟 id，拿去 bind 会失败。
     */
    fun lensForTarget(global: Float): DeviceCompatibility.LensProfile? {
        // ★只挑"能独立打开"的头★
        // ⚠️ 这是 Find X7 上 0.6x 失效的根因：现在池里会包含只挂在 physicalCameraIds 下、
        //    不在 cameraIdList 里的头（ColorOS 常见）。这种头 CameraX 的 CameraFilter
        //    根本匹配不到，绑它必然失败 → 广角被判定"物理直连不可用"并永久封禁
        //    → 0.6x 从此彻底用不了。
        val real = physicalLensOptions.filter {
            DeviceCompatibility.isRealCameraId(it.id) && DeviceCompatibility.canBindDirectly(it.id)
        }
        val pool: List<DeviceCompatibility.LensProfile> =
            if (real.isNotEmpty()) real
            else lensProfiles.filter { DeviceCompatibility.isRealCameraId(it.id) }
        if (pool.isEmpty()) return null
        return pool.filter { it.zoomFactor <= global + 0.001f }
            .maxByOrNull { it.zoomFactor }
            ?: pool.minByOrNull { it.zoomFactor }
    }

    /**
     * ★挑一颗能到达 [target] 的长焦物理头★
     *
     * 三个调用点（smoothZoomTo / commitZoom / requestLensSwitchIfNeeded）以前各写一份
     * 15% 容差的挑选，导致判定不一致：同一个 3x 在某些路径挑得中、某些挑不中。
     *
     * 这里统一成两步，这是"OPPO Find 调不出长焦"的成因②：
     *   ① 先在 ±20% 内挑【最接近】的（比原来 15% 宽，覆盖 2.8x 潜望对 3x 档的情况）
     *   ② 挑不中再退一步：只要这颗头是长焦头(>= TELE_ENTER_RATIO)，取最接近的那颗。
     *      宁可让 3x 落在 2.8x 头上做一点点数码裁切，也别退回主摄裁切 ——
     *      后者才是"长焦调不出来"的表现。
     */
    fun pickTeleFor(target: Float): DeviceCompatibility.LensProfile? {
        if (!telePreferPhysical) return null
        if (!physicalTeleWorks) return null
        if (target < DeviceCompatibility.TELE_ENTER_RATIO) return null
        // ★只挑"能独立打开"的头★
        // 不在 cameraIdList 里的（ColorOS 挂在 physicalCameraIds 下的那些），
        // CameraX 的 CameraFilter 匹配不到，绑了必然失败 ——
        // 以前不区分，挑中一颗绑不上的头 → 失败 → 累计封禁 → 长焦彻底没戏。
        val pool = physicalLensOptions.filter { DeviceCompatibility.isRealCameraId(it.id) }
        val bindable = pool.filter { DeviceCompatibility.canBindDirectly(it.id) }
        // ★退一步★：能重新绑定的头优先（老路，已适配机型行为不变）；
        // 一台都没有时才考虑"只能靠 PHYSICAL_CAMERA_ID 切"的头（OPPO Find X7 的副摄）
        val usable = if (bindable.isNotEmpty()) bindable
        else pool.filter { DeviceCompatibility.isSwitchableViaRequest(it.id) }
        val tele = usable.filter { it.zoomFactor >= DeviceCompatibility.TELE_ENTER_RATIO - 0.01f }
        if (tele.isEmpty()) {
            val known = pool.filter { it.zoomFactor >= DeviceCompatibility.TELE_ENTER_RATIO - 0.01f }
            if (known.isNotEmpty()) {
                Log.w(
                    "Camera",
                    "本机有长焦头但都不可独立打开（不在 cameraIdList），" +
                            "物理直连不可用，长焦交给逻辑摄像头 setZoomRatio"
                )
                // 关掉物理直连，让逻辑头接手 —— 至少倍率能到，不会卡住
                telePreferPhysical = false
            }
            return null
        }

        // ★★只挑"扛得住目标倍率"的那颗★★
        // ⚠️ 这是"10x 切不了"的根因：原来没有这个约束，
        //    点 10x 时会挑中池里唯一那颗 3x 长焦头 → 直连过去 →
        //    本地要下发 10/3 = 3.33x，而单颗物理头本地往往只有 1.0~2.0x，
        //    于是倍率被 clamp 住、切不过去，还把"回逻辑头"这条路也堵死了。
        // 现在：目标必须 ≤ 该头倍率 × TELE_HEAD_ROOM，否则认为这颗头扛不住。
        val capable = tele.filter { target <= it.zoomFactor * DeviceCompatibility.TELE_HEAD_ROOM }
        if (capable.isEmpty()) {
            Log.d(
                "Camera",
                "目标 ${"%.2f".format(target)}x 超出所有长焦头的承载范围" +
                        "（最大 ${tele.maxOf { it.zoomFactor }}x × ${DeviceCompatibility.TELE_HEAD_ROOM}），" +
                        "改由逻辑摄像头 setZoomRatio 承担"
            )
            return null
        }

        // ① ±20% 内最接近的
        val near = capable.filter { abs(it.zoomFactor - target) <= target * 0.20f }
            .minByOrNull { abs(it.zoomFactor - target) }
        if (near != null) return near
        // ② 兜底：取【倍率最大的那颗】—— 优先让视野更近的那颗去承担，数码裁切更少。
        //    （在 capable 里挑，已保证扛得住）
        return capable.maxByOrNull { it.zoomFactor }
    }

    /**
     * 目标倍率如果"当前这颗头到不了"，就切到能到位那颗物理头。
     *
     * @return true 表示已发起切换（会重新绑定相机），调用方应直接 return，
     *         倍率会在绑定完成后按 pendingZoomTarget 落位。
     */
    /**
     * ★物理直连这条路径现在还能不能用★（按目标类型分开判定）
     *
     * ⚠️ 这里原来是 `if (!physicalSwitchWorks) return false` —— 一个全局总闸。
     *    只要【任何一次】物理头绑定失败（哪怕是广角头失败一次），它就永久 false，
     *    于是后面所有直连（包括长焦）全部被这道门禁挡掉 ——
     *    这才是"OPPO Find 怎么都调不出长焦"的真正最后一环：
     *    Find 有 preferPhysicalWide，一点 0.6x 就走广角直连，
     *    广角失败一次 → 总闸关闭 → 长焦这辈子都调不出来。
     *
     * 现在按"要去的那颗头是哪种"分开判定，广角失败不影响长焦，反之亦然。
     */
    fun physicalPathOpen(target: Float): Boolean = when {
        target < 0.95f -> physicalWideWorks
        target >= DeviceCompatibility.TELE_ENTER_RATIO -> physicalTeleWorks
        else -> physicalSwitchWorks
    }

    /**
     * ★★变焦/换头诊断 dump★★
     *
     * 调不出长焦时先看这个：它会把"池里到底有几颗头、各自倍率多少、哪个开关关着"
     * 一次性打出来，一眼就能定位卡在哪一环，不用再靠猜。
     */
    fun dumpLensSwitchState(tag: String, target: Float) {
        val pool = physicalLensOptions.filter { DeviceCompatibility.isRealCameraId(it.id) }
        val sb = StringBuilder()
        sb.append("===== 镜头切换诊断 [$tag] 目标=${"%.2f".format(target)}x =====\n")
        sb.append("机型: MODEL=${Build.MODEL} DEVICE=${Build.DEVICE} BRAND=${Build.BRAND}\n")
        sb.append("识别: Find系列=${DeviceCompatibility.isOppoFindSeries} " +
                "一加顽固=${DeviceCompatibility.isOnePlusTeleStubborn} " +
                "已知顽固=${DeviceCompatibility.isKnownTeleStubborn}\n")
        sb.append("开关: telePreferPhysical=$telePreferPhysical " +
                "physicalTeleWorks=$physicalTeleWorks " +
                "physicalWideWorks=$physicalWideWorks " +
                "physicalSwitchWorks=$physicalSwitchWorks\n")
        sb.append("路径对本目标开放=${physicalPathOpen(target)}  " +
                "当前直连=${boundLensId ?: "逻辑摄像头"}  " +
                "逻辑头范围=${"%.2f".format(minRatio)}~${"%.2f".format(maxRatio)}x\n")
        sb.append("物理头池(${pool.size} 颗):\n")
        for (p in pool) {
            sb.append(
                "    id=${p.id} 倍率=${p.zoomFactor}x " +
                        "焦距=${"%.2f".format(p.focalLengthMm)}mm " +
                        "等效=${"%.1f".format(p.equiv35mm)}mm " +
                        "maxJpeg=${p.maxJpegW}x${p.maxJpegH}\n"
            )
        }
        if (pool.isEmpty()) sb.append("    （池是空的！probePhysicalCameras 没扫到任何可绑定头）\n")
        val picked = pickTeleFor(target)
        sb.append("pickTeleFor 结果=${if (picked != null) "${picked.id}(${picked.zoomFactor}x)" else "null"}\n")
        if (picked == null) {
            sb.append("  → 为空的原因可能是：\n")
            sb.append("    ① telePreferPhysical=false（机型没识别 且 硬件兜底没触发）\n")
            sb.append("    ② physicalTeleWorks=false（长焦被封过）\n")
            sb.append("    ③ 池里没有 >=${DeviceCompatibility.TELE_ENTER_RATIO}x 的头" +
                    "（长焦头没上报焦距且 reconcileStops 没指派成功时会出现）\n")
        }
        sb.append("=========================================")
        Log.d("Camera", sb.toString())
    }

    //
    // ★★防抖相关函数必须声明在 requestLensSwitchIfNeeded 之前★★
    //
    // Kotlin 规则：局部函数只能引用【前面】已声明的局部函数。
    // 上一版在 requestLensSwitchIfNeeded 里调 wantStabilization() /
    // setStabilizationRaw()，而这两个声明在它【后面】→ Unresolved reference。
    // （代码里前面已经因为同样的问题踩过两次：restorePreviewParamsAfterCapture、
    //  LocalLabelRotation 相关声明都是按这个规则排的。）
    //
    // ⚠️ setStabilizationRaw 的实现依赖 applySceneParams，而后者在更后面
    //   （它自己又依赖 applyProParams 等），没法整块前移。
    //   所以用一个【延迟绑定的函数引用槽】：这里先声明占位，
    //   等 applySceneParams 声明完之后回填 —— 前面的代码就能调到它了。
    //
    var applySceneParamsRef: ((Boolean?, Boolean) -> Unit)? = null

    /**
     * 直接下发防抖开关（EIS + OIS），不做任何模式判断。
     * 给切镜头 / 变焦期间"临时挂起"用——那时需要绕过 mode 检查强行关掉。
     */
    fun setStabilizationRaw(enabled: Boolean) {
        applySceneParamsRef?.invoke(enabled, false)
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
     * EIS 要不要开：档位含 EIS + 机器支持 + 录像模式。
     * 拍照不开：EIS 是帧间稳定，单帧照片没有帧间抖动可稳，只会白白裁掉约 10% 画面。
     * 4K60 单独关掉（「4K60 闪烁/花屏」最主要的 4K60 专属诱因）：
     *   EIS 每帧重算 crop + 运动估计，4K60 下 HAL 已跑满，再叠一层会迫使
     *   反复重配 crop，每次重配 3A 都要重新收敛 → R/B 增益连续变化 → 一段段偏色。
     *   （CameraX 官方也只保证 1080p30 以下生效。）⚠️ 只关 EIS，OIS 照常开。
     */
    fun wantEisCore(): Boolean {
        if (!stabMode.eis) return false
        if (!stabilizationSupported) return false
        if (mode != CaptureMode.VIDEO) return false
        if (videoQuality == VideoQuality.UHD && videoFrameRate == VideoFrameRate.FPS_60) {
            Log.w(
                "Camera",
                "4K60 录像：EIS 已自动关闭（该档位会迫使 3A 反复重收敛，导致色彩闪烁/花屏）；OIS 不受影响"
            )
            return false
        }
        return true
    }

    /**
     * ★OIS（光学防抖）要不要开★
     *
     * ⚠️★这里不再要求 mode == VIDEO★ —— 那是老实现的 bug：
     *    拍照时 OIS 永远是关的，导致"默认拍照模式下镜头无法调用光学防抖"。
     *    OIS 是镜头马达做位移补偿，【不裁切画面、不影响画质】，
     *    拍照时收益反而最大（能压低快门、大幅减少糊片），必须开。
     *
     * 唯一例外是星空模式：三脚架上 OIS 会自己找位移，把星点拉成小圈。
     */
    fun wantOisCore(): Boolean {
        if (!stabMode.ois) return false
        if (!stabilizationSupported) return false
        // 上架必须关 OIS：见 starTrail 参数包里的说明
        if (mode == CaptureMode.STAR_TRAIL) return false
        return true
    }

    /**
     * 有没有任何一种防抖在生效。
     * 给"切镜头 / 拖动变焦时要不要挂起防抖"这类判断用 —— 那时两个要一起挂起。
     */
    fun wantStabilization(): Boolean = wantEisCore() || wantOisCore()

    fun requestLensSwitchIfNeeded(target: Float): Boolean {
        // ★每次尝试换头都打一份诊断★：调不出长焦时直接看 Logcat，不用猜
        dumpLensSwitchState("requestLensSwitch", target)
        if (!physicalPathOpen(target)) {
            Log.d(
                "Camera",
                "物理直连路径对 ${"%.2f".format(target)}x 已关闭 " +
                        "(wide=$physicalWideWorks, tele=$physicalTeleWorks, other=$physicalSwitchWorks)"
            )
            return false
        }
        if (isFront) return false
        // ★录像中绝不换头★ 换头要 unbindAll 重绑，会直接把正在录的视频打断
        if (isRecording) return false
        // 用真实可绑定物理头的数量判断，而不是 lensProfiles
        // （后者可能全是画像虚拟 id，size 看着够其实一颗都 bind 不了）
        // ★只算"能独立打开"的头★：不可直连的头凑数会让 pool.size >= 2 成立，
        //    但真去绑时一个都绑不上
        // 能重新绑定 或 能用 PHYSICAL_CAMERA_ID 切，都算"可用"
        val pool = physicalLensOptions.filter {
            DeviceCompatibility.isRealCameraId(it.id) &&
                    (DeviceCompatibility.canBindDirectly(it.id) ||
                            DeviceCompatibility.isSwitchableViaRequest(it.id))
        }
        if (pool.size < 2) {
            Log.d("Camera", "可独立绑定的物理头不足 2 颗（${pool.size}），跳过换头")
            return false
        }
        val lens = lensForTarget(target) ?: return false

        // ★长焦优先用真实长焦头★（统一走 pickTeleFor，见那里的说明）
        val tele: DeviceCompatibility.LensProfile? = pickTeleFor(target)
        val wanted = if (tele != null && tele.id != boundLensId) tele else lens

        val inRange = target >= minRatio - 0.01f && target <= maxRatio + 0.01f
        val sameLens = wanted.id == boundLensId
        // ★修复"切到广角之后只能调用广角"★（华为 P 系列 / OPPO Find 都中招）
        // 老逻辑里"已直连某颗头"只在目标跑出这颗头可用区间时才换头，
        // 而广角头(0.6x)的可用区间是 0.6~2.4x，1x / 2x 全落在区间内 → need=false，
        // 于是永远待在广角头上做数码裁切，别的镜头一颗都调不出来。
        // 现在补一条：目标明显偏离当前这颗头的原生倍率，就退回逻辑摄像头，
        // 让逻辑头（必要时再直连长焦）去接手。
        val curFactor: Float = boundLensFactor()
        val needBackToLogic: Boolean = boundLensId != null && inRange &&
                (target > curFactor * 1.15f + 0.01f || target < curFactor * 0.85f - 0.01f)

        val need: Boolean = when {
            // ★唯一允许"抢占"的情形：实测确认逻辑头到不了广角★
            // logicWideBlocked 只在下面两种情况被置 true：
            //   ① 绑定时读到 HAL 原生下限 >= 1.0，但本机确实有 <1.0 的光学档
            //   ② 变焦校验发现目标 0.6x 实际回读是 1.0x
            // 没坐实之前一律不直连 —— 否则会抢掉本来能正常工作的 setZoomRatio。
            boundLensId == null && logicWideBlocked && physicalWideWorks && target < 0.99f -> !sameLens

            // ★长焦走物理直连★：不管当前在哪颗头上（逻辑头 / 广角头 / 另一颗长焦头）
            // ⚠️ 这里原来写的是 `boundLensId == null && tele != null`，只在【逻辑头】上才生效。
            //    于是"广角头 → 3x"走不进来，被下面的退回逻辑头分支抢先处理 ——
            //    这就是 OPPO Find 调不出长焦的成因①（见下方"长焦优先"分支的说明）。
            tele != null -> !sameLens

            // 当前用逻辑摄像头：只在"逻辑头范围根本够不到"时才走直连。
            // ⚠️ 这里曾经按机型预判优先直连，结果把本来能正常变焦的小米机型搞坏了，
            //    所以现在不看机型，只看范围。
            boundLensId == null ->
                !inRange && abs(wanted.zoomFactor - target) < 0.01f

            // 已直连某颗头：目标跑出这颗头的可用区间了才换头
            else -> !sameLens && (target < effMinZoom() - 0.01f || target > effMaxZoom() + 0.01f)
        }

        if (!need && !needBackToLogic) return false

        //
        // 切镜头前必须挂起防抖 ——「开防抖 + 切中长焦卡死」的直接根因。
        // 切镜头是 unbindAll + bindToLifecycle（拆流再建流），而防抖开着时
        // EIS 的跨帧稳定窗口还在、OIS 马达还在中途位置，HAL 要么等它们收敛完才能
        // 重建流，要么卡在重配 crop region 上 → 预览流迟迟起不来（黑屏），像卡死。
        // 之前只在变焦动画里挂起过（smoothZoomTo 的 stabOverride=false），
        // 切物理头这条路径是 return true 直接出去的，压根没挂起 —— 点长焦按钮恰好走它。
        // 放这里是因为上面所有 return false 的守卫都已通过，这里是所有 return true
        // 的公共前置点；恢复由绑定流程末尾的 applySceneParams() 统一负责。
        //
        if (wantStabilization() && !stabSuspended) {
            setStabilizationRaw(false)
            stabSuspended = true
            Log.d("Camera", "★切镜头前挂起防抖★（EIS/OIS 与 unbind+bind 打架会卡死）")
        }

        // ★★已在长焦头上、但目标超出这颗头的承载范围 → 必须先退回逻辑头★★
        // 例：3x 头上点 10x。此时 pickTeleFor(10) 返回 null（扛不住），
        //     但下面的 needBackToLogic 用的阈值是 1.15 倍而 effMaxZoom 给的是 4 倍余量，
        //     两者不一致会出现"既不该直连、又判定不用退回"的死角。
        //     这里显式补上：只要当前在长焦头且目标超出承载，就退回逻辑头走 setZoomRatio。
        val curF: Float = boundLensFactor()
        if (boundLensId != null && curF >= DeviceCompatibility.TELE_ENTER_RATIO &&
            target > curF * DeviceCompatibility.TELE_HEAD_ROOM &&
            target >= minRatio - 0.01f && target <= maxRatio + 0.01f
        ) {
            Log.d(
                "Camera",
                "目标 ${"%.2f".format(target)}x 超出当前长焦头 ${boundLensId}(${curF}x) 的承载，" +
                        "退回逻辑摄像头"
            )
            pendingZoomTarget = target
            boundLensId = null
            boundViaRequest = false
            lensSwitching = true
            return true
        }

        // ★★长焦优先：目标在长焦档且有长焦头能到位 → 直接换过去，绝不退回逻辑头★★
        //
        // 这是"OPPO Find 调不出长焦"的主因。原来的流程是：
        //   广角头(0.6x) 上点 3x → needBackToLogic = true（3.0 远超 0.6×1.15）
        //   → 落到下面的"退回逻辑摄像头"分支 → 重绑逻辑头 → setZoomRatio(3.0)
        //   → 主摄数码裁切，长焦头永远没被绑过。
        // OPPO Find 有 preferPhysicalWide，ColorOS 上 logicWideBlocked 基本为 true，
        // 所以点过一次 0.6x 之后，长焦就再也调不出来了。
        //
        // 现在：只要目标是长焦档且选中了长焦头，就从当前这颗头【直接换】到那颗长焦头。
        if (tele != null && tele.id != boundLensId &&
            target >= DeviceCompatibility.TELE_ENTER_RATIO
        ) {
            Log.d(
                "Camera",
                "直连长焦头 ${tele.id}（${tele.zoomFactor}x）以到达 ${"%.2f".format(target)}x" +
                        "（从 ${boundLensId ?: "逻辑摄像头"} 直接切换）"
            )
            pendingZoomTarget = target
            lensSwitching = true
            boundLensId = tele.id      // 改它就会触发绑定流程重跑
            return true
        }

        // ★离开当前物理头：只有退到主摄区间（<1.55x）才回逻辑头★
        // 老代码是"目标落在逻辑头范围内就回逻辑头"，结果在长焦头上从 3x 拖到 2.8x
        // 也会解绑回逻辑头 —— 画面先闪一下又变回主摄裁切，这正是"长焦站不住"的元凶。
        if (boundLensId != null && (needBackToLogic || target < DeviceCompatibility.TELE_EXIT_RATIO) &&
            target >= minRatio - 0.01f && target <= maxRatio + 0.01f
        ) {
            Log.d(
                "Camera",
                "离开物理头 ${boundLensId}，退回逻辑摄像头并落到 ${"%.2f".format(target)}x"
            )
            pendingZoomTarget = target
            boundLensId = null
            boundViaRequest = false
            lensSwitching = true
            return true
        }

        // ★★优先用 CaptureRequest.PHYSICAL_CAMERA_ID 切（不重新绑定）★★
        //
        // 只有在这颗头【不能独立打开】、但【挂在某个逻辑头下】时才走这条路：
        //   · 能独立打开的头 → 继续走下面的重新绑定（老路）
        //   · 已经适配好的机型（小米 / vivo iQOO / 真我 / 华为 P / 红米 Note）
        //     它们的副摄都在 cameraIdList 里，永远走老路，行为完全不变
        //   · OPPO Find X7 这类副摄只挂在 physicalCameraIds 下的 → 走这条路
        //
        // 好处：不 unbind/rebind → 预览不闪、画面比例不跳、录像不会被打断。
        if (DeviceCompatibility.USE_PHYSICAL_REQUEST_SWITCH &&
            physicalRequestWorks &&
            !isRecording &&
            DeviceCompatibility.isSwitchableViaRequest(wanted.id)
        ) {
            Log.d(
                "Camera",
                "★用 PHYSICAL_CAMERA_ID 切到 ${wanted.id}（${wanted.zoomFactor}x）★ " +
                        "不重新绑定，会话仍是逻辑摄像头"
            )
            pendingZoomTarget = target
            lensSwitching = true
            boundViaRequest = true
            boundLensId = wanted.id     // 触发绑定流程；那里识别 boundViaRequest 后会跳过重绑
            return true
        }

        Log.d(
            "Camera",
            "切换物理头 ${wanted.id}（${wanted.zoomFactor}x）以到达 ${"%.2f".format(target)}x"
        )
        pendingZoomTarget = target
        lensSwitching = true
        boundViaRequest = false
        boundLensId = wanted.id      // 改它就会触发绑定流程重跑
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
     * 把"用哪颗物理头出图"写进 CaptureRequest 包（只追加，不清空）。
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun appendPhysicalCameraId(b: CaptureRequestOptions.Builder) {
        if (!DeviceCompatibility.USE_PHYSICAL_REQUEST_SWITCH) return
        if (!physicalRequestWorks) return
        val pid = if (boundViaRequest) boundLensId else null
        if (pid == null) return
        if (!DeviceCompatibility.isSwitchableViaRequest(pid)) return
        // PHYSICAL_CAMERA_ID 是 API 29 才有的 key；反射拿不到就退回老路
        val key = DeviceCompatibility.physicalCameraIdKey()
        if (key == null) {
            physicalRequestWorks = false
            Log.w("Camera", "本系统无 PHYSICAL_CAMERA_ID，退回重新绑定物理头的老路")
            return
        }
        try {
            b.setCaptureRequestOption(key, pid)
        } catch (e: Exception) {
            physicalRequestWorks = false
            Log.w("Camera", "PHYSICAL_CAMERA_ID 下发失败，退回重新绑定物理头的老路", e)
        }
    }

    /**
     * ★当前是否处于【重负载录制档位】★：4K 或 60 帧。
     *
     * 这类档位下 HAL 流水线余量最小，锁死参数的代价最明显
     * （一旦锁错，整段素材都废），所以 AE 放开让它随场景自适应。
     * 见 applySceneParams 里 ⑧ 的说明。
     */
    fun isHeavyRecording(): Boolean =
        mode == CaptureMode.VIDEO &&
                (videoFrameRate.fps >= 60 || videoQuality == VideoQuality.UHD)

    @OptIn(ExperimentalCamera2Interop::class)
    fun applyProParams(
        iso: Int,
        shutter: Long,
        ev: Int,
        focus: Float,
        wb: Int,
        /** 同 applySceneParams：忽略录制中的去重，无条件提交 */
        force: Boolean = false,
        /**
         * 临时挂起防抖（切镜头 / 拖动变焦期间传 false）。
         * ⚠️ 以前专业模式没有这个参数，于是 applySceneParams 在专业模式下
         *    转发到 applyProParams 时把 stabOverride 丢了 ——
         *    「专业模式 + 开防抖 + 切中长焦」不会挂起防抖，
         *    六之十一修好的卡死在专业模式下照样会犯。
         */
        stabOverride: Boolean? = null
    ) {
        //
        // ★专业模式同样要能"录制中调参数"★
        // 专业模式的滑条是【直接调 applyProParams】的（见 11670~11690 那批
        // onChange），绕过了 applySceneParams，所以这里要单独处理。
        // 做法与 ⑨ 一致：允许随时调，但内容没变就不重复提交。
        //
        val recordingNow = isRecording
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

            // ★物理镜头切换★：专业模式同样要带，否则下发参数包会把 PHYSICAL_CAMERA_ID 清掉
            appendPhysicalCameraId(b)

            // 防抖要跟手动参数一起下发：setCaptureRequestOptions 是全量替换，
            // 分开调会把对方清掉。录像模式下才开，拍照时开了会压低分辨率。
            // ★降噪 / 边缘★：专业模式同样以减噪点为主
            b.setCaptureRequestOption(
                CaptureRequest.NOISE_REDUCTION_MODE,
                CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY
            )
            b.setCaptureRequestOption(
                CaptureRequest.EDGE_MODE,
                if (mode == CaptureMode.VIDEO) CameraMetadata.EDGE_MODE_OFF
                else CameraMetadata.EDGE_MODE_HIGH_QUALITY
            )
            // 帧率：专业模式也锁住，避免自动曝光把帧率拉下来拖慢取景
            b.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range(
                    if (mode == CaptureMode.VIDEO) videoFrameRate.fps else DEFAULT_PREVIEW_FPS,
                    if (mode == CaptureMode.VIDEO) videoFrameRate.fps else DEFAULT_PREVIEW_FPS
                )
            )

            //
            // ★EIS / OIS 分开算，并支持 stabOverride★
            //   跟 applySceneParams 里 ④ 完全同一套规则：
            //   · EIS 只在录像档位有意义（wantEisCore 里已排除拍照 / 4K60）
            //   · OIS 是硬件马达，物理头直连时必须关（换 sensor 后马达归位会阻塞 HAL）
            //   · stabOverride 用于切头 / 拖变焦时两个一起挂起
            //
            val eisOn = if (stabOverride != null) stabOverride && wantEisCore() else wantEisCore()
            val oisOn = if (stabOverride != null) stabOverride && wantOisCore() else wantOisCore()
            val oisAllowed = oisOn && boundLensId == null
            if (oisOn && !oisAllowed) {
                Log.d("Camera", "专业模式·物理头直连中：仅保留 EIS，OIS 已禁用")
            }
            b.setCaptureRequestOption(
                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                if (eisOn) CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
                else CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
            )
            b.setCaptureRequestOption(
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                if (oisAllowed) CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
                else CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
            )

            //
            // ★★录制中的去重 + 不 clear★★
            //   与 applySceneParams 的 ⑧⑨ 同一套逻辑：
            //   · 录制中 clear() 会让 AWB_LOCK 消失 → HAL 重启 3A → 视频闪色
            //   · 录制中内容没变就别提交，纯粹是让 HAL 白重建一次
            //   专业模式尤其容易中招：滑条每动一下都直接调到这里，
            //   拖一次滑条就闪一下色。
            //
            val proSig = buildList {
                add(recordingNow)
                add(isHeavyRecording())
                add(iso); add(shutter); add(ev); add(focus); add(wb)
                add(eisOn); add(oisAllowed)
                add(boundLensId ?: "")
                add(mode)
                add(videoFrameRate.fps)
            }.joinToString("|")

            if (recordingNow && !force && proSig == lastProRecSig) {
                Log.d("Camera", "专业模式·录制中：参数包与上次相同，跳过重复提交")
                sceneParamsDirty = true
                return
            }

            // 先清空，保证切回逻辑头时 PHYSICAL_CAMERA_ID 真的消失。
            // ★录制期间例外★：清空会重启 3A，导致视频闪色。
            if (!recordingNow) {
                c2.clearCaptureRequestOptions()
            }
            c2.setCaptureRequestOptions(b.build())
            if (recordingNow) lastProRecSig = proSig else lastProRecSig = ""
            Log.d(
                "Camera",
                "专业参数: ISO=$iso, S=$shutter, EV=$ev(${limits.evToSteps(ev)}步), F=$focus, WB=$wb"
            )
        } catch (e: Exception) {
            Log.e("Camera", "应用专业参数失败", e)
        }
    }

    /**
     * ★统一 Camera2 参数包★：帧率 + 降噪 + 边缘 + 防抖 + 对焦模式，一次下发。
     *
     * 为什么必须合并到一个包里：Camera2CameraControl.setCaptureRequestOptions 是
     * 【全量替换】，以前"防抖""帧率""降噪"各下发一次，后一次会把前一次清掉，
     * 于是帧率永远落不下去 —— 这就是"选了 60 帧、实际还是 30 帧"的根因。
     *
     * @param stabOverride 临时覆盖防抖开关（变焦期间要挂起防抖）
     * @param afSmooth     变焦期间切 CONTINUOUS_VIDEO，避免每一步都重新寻焦造成卡顿
     */

    @OptIn(ExperimentalCamera2Interop::class)
    fun applySceneParams(
        stabOverride: Boolean? = null,
        afSmooth: Boolean = false,
        /**
         * ★强制提交★：忽略"与上次相同则跳过"的去重。
         * 用于录制结束那一刻（此时 isRecording 已 false，签名必然不同，
         * 这里只是双保险）以及需要无条件刷新的场合。
         */
        force: Boolean = false
    ) {
        //
        // 录制期间【允许调参数】，但【内容没变就不重复提交】。
        //
        // 为什么会闪：setCaptureRequestOptions 是一次全量提交，Camera2CameraControl
        // 收到新 bundle 会重建 repeating request，而很多 HAL 重建时会顺带重启一次
        // 3A 收敛 —— 哪怕内容一模一样，只要提交动作发生就可能触发。
        // 上一版"录制中一律冻结"能止闪，但参数完全调不动（用户明确反馈要能调）。
        //
        // 所以：真变了（切镜头/改防抖/换档位）→ 签名不同 → 提交，且带着完整
        // AWB_LOCK/AE_LOCK 一起提交（不像 clear() 那样出现"锁定消失"的空档）；
        // 没变（看门狗巡检、重复 LaunchedEffect）→ 跳过 —— 这些才是闪色真凶。
        //
        // ⚠️ 去重只在录制中生效：待机时相机可能刚重绑（参数一样但实例换了），
        //    那时必须重新下发，否则重绑后就再也下不出去了。
        //
        val recordingNow = isRecording
        val cam = camera ?: return
        if (mode == CaptureMode.PRO && manualControlSupported) {
            applyProParams(
                proIso, proShutterNs, proEv, proFocus, proWb,
                force = force,
                // ⚠️ 必须往下传：专业模式下切镜头 / 拖变焦要挂起防抖，
                //    不传的话 stabOverride 在这里就丢了（见 applyProParams 的说明）
                stabOverride = stabOverride
            )
            return
        }
        // ★星空模式★：曝光 / ISO / 对焦由星轨参数接管，不能被常规场景参数覆盖回去
        if (mode == CaptureMode.STAR_TRAIL) {
            if (starTrailRunning) return
            applyStarTrailPreviewParams(cam, starTrailConfig)
            return
        }
        try {
            val b = CaptureRequestOptions.Builder()

            //
            // ① 帧率 / AE 曝光区间
            //
            // ★录像★：锁死用户选的帧率。不能为了曝光偷偷降帧 ——
            //   用户选 30fps 就是为了 30fps 的观感，改了等于违背设置。
            //   （录像时画面亮度靠 NR 与编码码率补偿，见下方。）
            //
            // ★拍照 / 待机★：按环境光放宽区间，暗处让 AE 用更长的曝光时间，
            //   而不是一味拉高 ISO。详见 autoExposurePlan 的说明。
            //
            val fps = if (mode == CaptureMode.VIDEO) videoFrameRate.fps else DEFAULT_PREVIEW_FPS
            if (mode == CaptureMode.VIDEO) {
                b.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(fps, fps)
                )
            } else {
                val plan = autoExposurePlan(ambientLux, captureQualityBoost)
                b.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    Range(plan.fpsMin, plan.fpsMax)
                )
                if (ambientLux >= 0f) {
                    Log.d(
                        "Camera",
                        "自动挡曝光：${"%.0f".format(ambientLux)} lux → " +
                                "AE ${plan.fpsMin}~${plan.fpsMax}fps" +
                                "（最长曝光 ${1000 / plan.fpsMin}ms）" +
                                "${if (captureQualityBoost) " [拍照瞬间]" else ""}"
                    )
                }
            }

            //
            // ②③★降噪 / 锐化：预览流畅度 ↔ 噪点 的平衡★
            //
            // 原来的做法是"照片 / 30帧录像一律 HIGH_QUALITY 降噪 + HIGH_QUALITY 锐化"，
            // 问题在于 NOISE_REDUCTION_MODE 和 EDGE_MODE 是【对每一帧都生效】的，
            // 包括预览流。HIGH_QUALITY 会让 ISP 每帧做时域/空域多帧降噪，
            // 在中低端机上直接吃掉几十毫秒 —— 表现就是取景卡、变焦拖影。
            // 而预览本来就不该追求降噪：屏幕只有 1080p，噪点根本看不出来。
            //
            // 现在的策略（分场景取舍）：
            //   待机预览      → NR=FAST  EDGE=OFF：流畅优先。
            //                   预览是给你构图用的，不是给你看画质的；
            //                   FAST 保留轻度降噪，画面不至于"沙沙"的。
            //   拍照【那一瞬】 → NR=HIGH_QUALITY EDGE=HIGH_QUALITY：画质优先。
            //                   只在按下快门时临时切（见 takePhoto 里的 boostCaptureQuality），
            //                   成片干净锐利，而日常取景不受影响。
            //   录制中        → 看负载：4K 或 60帧 用 FAST（持续负载，必须减负），
            //                   1080p30 才用 HIGH_QUALITY（有余量，画质优先）。
            //
            val nrMode: Int
            val edgeMode: Int
            if (captureQualityBoost) {
                // 拍照瞬间：画质优先（由 takePhoto 临时置起）
                nrMode = CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY
                //
                // ★暗处不锐化★：噪点会被 EDGE_MODE 当成细节一起放大，
                //   暗光照片常见的"颗粒感 + 白边"就是这么来的。
                //   暗到 autoExposurePlan 判 sharpen=false 时（约 <40 lux）
                //   改成 OFF —— 宁可略软，也不要满屏噪点被锐化得刺眼。
                //
                edgeMode = if (autoExposurePlan(ambientLux, true).sharpen)
                    CameraMetadata.EDGE_MODE_HIGH_QUALITY
                else CameraMetadata.EDGE_MODE_OFF
            } else if (mode == CaptureMode.VIDEO) {
                val heavy = fps >= 60 || videoQuality == VideoQuality.UHD
                nrMode = if (heavy) CameraMetadata.NOISE_REDUCTION_MODE_FAST
                else CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY
                // 录像不锐化：锐化会把噪点和压缩块一起放大
                edgeMode = CameraMetadata.EDGE_MODE_OFF
            } else {
                // 待机预览：流畅优先
                nrMode = CameraMetadata.NOISE_REDUCTION_MODE_FAST
                edgeMode = CameraMetadata.EDGE_MODE_OFF
            }
            b.setCaptureRequestOption(CaptureRequest.NOISE_REDUCTION_MODE, nrMode)
            b.setCaptureRequestOption(CaptureRequest.EDGE_MODE, edgeMode)

            // ②③b ★CaptureIntent★：告诉 HAL 这一帧是要"预览"还是"出片"。
            //   这是被严重低估的一个参数 —— HAL 会据此切换整条流水线：
            //     PREVIEW       → 优先低延迟、高帧率，允许画质打折
            //     STILL_CAPTURE → 优先画质，允许慢
            //     VIDEO_RECORD  → 优先持续稳定输出
            //   以前压根没设过，HAL 只能按默认（通常是 PREVIEW）猜，
            //   于是拍照时画质调优没吃到、录像时持续稳定性也拿不到。
            b.setCaptureRequestOption(
                CaptureRequest.CONTROL_CAPTURE_INTENT,
                when {
                    captureQualityBoost -> CameraMetadata.CONTROL_CAPTURE_INTENT_STILL_CAPTURE
                    mode == CaptureMode.VIDEO -> CameraMetadata.CONTROL_CAPTURE_INTENT_VIDEO_RECORD
                    else -> CameraMetadata.CONTROL_CAPTURE_INTENT_PREVIEW
                }
            )

            // ②③c ★抗闪烁★：日光灯 / LED 下不设会自动选错频率，
            //   画面出现横向滚动条纹 —— 常被误认成"噪点"或"花屏"。
            //   AUTO 让 HAL 自己检测 50Hz / 60Hz。
            b.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO
            )
            //
            // ②③d ⚠️ 刻意【不设】CONTROL_AE_MODE★
            //   闪光灯就是靠 AE_MODE 实现的（ON_ALWAYS_FLASH / ON_AUTO_FLASH），
            //   Camera2Interop 下发的 key 优先级高于 CameraX 自己的设置，
            //   这里一旦写成 CONTROL_AE_MODE_ON，就会把闪光灯的 AE 模式覆盖掉，
            //   结果是"开了闪光却不闪"。AE 本来默认就是开的，不需要多此一举。
            //

            //
            // ④ 防抖。OIS 与 EIS 分开算：stabOverride 只用于"临时挂起"（切镜头/拖变焦，
            // 那时两个一起关）；正常情况各自按 wantEisCore / wantOisCore 判断。
            //
            // ★OIS 在物理头直连时禁用★ ——「开防抖 + 切中长焦卡死」第二个成因，比 EIS 更硬：
            // EIS 是软件裁切随时可开关；OIS 是【硬件马达】，补偿位置相对当前 sensor 标定，
            // 直连换了 sensor 后马达还在上一个的补偿位上，HAL 要等它归位才给帧 ——
            // 这个等待在很多机器上是同步阻塞的，直接表现为卡死。
            // 切回逻辑头（boundLensId == null）后 OIS 自动恢复。
            //
            val eisOn = if (stabOverride != null) stabOverride && stabMode.eis else wantEisCore()
            val oisOn = if (stabOverride != null) stabOverride && stabMode.ois else wantOisCore()
            val oisAllowed = oisOn && boundLensId == null
            if (oisOn && !oisAllowed) {
                Log.d("Camera", "物理头直连中：仅保留 EIS，OIS 已禁用（换 sensor 时马达归位会阻塞 HAL）")
            }
            b.setCaptureRequestOption(
                CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                if (eisOn) CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
                else CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
            )
            b.setCaptureRequestOption(
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                if (oisAllowed) CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
                else CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
            )

            // ⑤ 对焦模式：变焦期间用 CONTINUOUS_VIDEO，对焦不重新搜索，
            //    从大焦段切回小焦段时不再一卡一卡
            b.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                if (afSmooth) CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                else CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )

            // ⑥ ★统一镜头色彩★：锁住白平衡，换镜头时不再各自重新收敛一次。
            //    这是硬件层的统一；软件层（成片套 ColorMatrix）在 postProcessPhoto 里兜底。
            //
            // ★录制中：锁 AWB + 不重设 AWB_MODE★
            //   AWB_LOCK 防止增益漂移，不重设 AWB_MODE 防止 HAL 重启收敛。
            //   两者一起才能止住"视频色彩一阵一阵变"的问题，
            //   详见 LensColorUnifier.appendTo 的 forceLockAwb / keepAwbMode 注释。
            //   ⚠️ 这里只是"锁住白平衡"，【不是】锁住整个参数包 ——
            //      帧率 / 防抖 / 降噪 / 物理头这些照常随场景调整（见 ⑨）。
            LensColorUnifier.appendTo(
                b, lensColorKey(),
                forceLockAwb = recordingNow,
                keepAwbMode = recordingNow
            )

            // ⑦ ★物理镜头切换★：不重新绑定，直接指定逻辑摄像头用哪颗物理头出图。
            //    ⚠️ 必须放在【同一个 bundle】里：setCaptureRequestOptions 是全量替换，
            //       分开调会把前面 ①~⑥ 全部清掉（帧率/防抖/降噪互相覆盖就是这么来的）。
            appendPhysicalCameraId(b)

            // ⑧ 录制期间【不要 clear】——「4K60 闪烁/花屏」最隐蔽的一半根因。
            //   clearCaptureRequestOptions() + setCaptureRequestOptions() 是两次独立提交，
            //   中间那一瞬间 repeating request 上一个自定义 key 都没有 →
            //   AWB_LOCK 消失 → HAL 按默认 AUTO 重启一次白平衡收敛 → 增益重新爬 →
            //   录下来就是"每操作一次，视频就闪一次色"（4K60 下余量为零，抖动被放大）。
            //   clear() 只在"要删掉某个 key"时才必要（如退回逻辑头要让 PHYSICAL_CAMERA_ID
            //   真正消失）；录制期间物理头不变、3A 状态必须保持，可以跳过。
            //   录制结束（isRecording=false）时再恢复 clear，把 AE/AWB 锁摘掉。
            //
            if (recordingNow) {
                //
                // ★★AE 是否锁定：分档位处理★★
                //
                // 两个锁各管什么：
                //   AWB_LOCK → 管色彩，是闪色/偏色的直接成因，必须锁（否则必然复发）。
                //   AE_LOCK  → 管曝光，锁了之后场景亮度变化完全不自适应，
                //              整段视频会一直过曝或欠曝。
                // 所以 4K/60帧（重负载）只锁 AWB、放开 AE（余量小，锁错整段素材就废了）；
                // 1080p30 等轻负载双锁。
                // ⚠️ 放开 AE 不会把 AWB 带偏：AWB_LOCK=true 时 HAL 不重新收敛，
                //    锁定增益保持不变 —— 原生相机也是"锁 AWB 但保留 AE"。
                //
                val heavyRec = isHeavyRecording()
                val aeLock = !REC_AE_LOCK_EXEMPT_FOR_HEAVY || !heavyRec
                b.setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, aeLock)
                Log.d(
                    "Camera",
                    "录制中：AWB 已锁" +
                            if (aeLock) " + AE 已锁" else "，AE 放开（重负载档位，曝光随场景自适应）" +
                                    "，且本次【不 clear】参数包"
                )
            }

            //
            // ★录制中的去重签名★
            // 把本次 bundle 里所有【会影响画面】的项拼成一个字符串。
            // ⚠️ 必须覆盖 appendPhysicalCameraId / LensColorUnifier.appendTo
            //    写进去的东西，否则"参数其实变了、签名却一样"会导致该调的没调。
            //
            val sig = buildList {
                add(recordingNow)              // 录制状态（决定 AE/AWB 锁）
                add(isHeavyRecording())        // 重负载档位（决定是否锁 AE）
                add(fps)                       // ① 帧率
                add(nrMode); add(edgeMode)     // ②③ 降噪 / 锐化
                add(captureQualityBoost)
                add(mode)
                add(eisOn)                     // ④ 防抖（EIS）
                add(oisAllowed)                // ④ 防抖（OIS）
                add(afSmooth)                  // ⑤ 对焦模式
                add(LensColorUnifier.enabled)  // ⑥ 色彩统一总开关
                add(LensColorUnifier.lockAwb)  // ⑥ 用户主动锁
                add(boundLensId ?: "")         // ⑦ 物理头 id
                add(lensColorKey())            // ⑥ 当前镜头键
            }.joinToString("|")

            val ctrl = Camera2CameraControl.from(cam.cameraControl)
            // CaptureRequestOptions 没有"移除某一项"的 API，先清空再整体下发，
            // 这样切回逻辑摄像头时 PHYSICAL_CAMERA_ID 才会真正消失。
            // ★录制期间例外★：见上面 ⑧ 的说明 —— 清空会重启 3A，导致视频闪色。
            if (!recordingNow) {
                ctrl.clearCaptureRequestOptions()
            }

            //
            // ★★录制中：内容没变就跳过提交★★（见上面 ⑨ 的说明）
            //
            if (recordingNow && !force && sig == lastRecSceneSig) {
                Log.d("Camera", "录制中：参数包与上次相同，跳过重复提交（避免 3A 重启闪色）")
                //
                // ★这一次没发出去，要记账★
                //   录制结束时 applySceneParams() 会再来一次（那时 recordingNow=false、
                //   签名必然不同），所以参数不会真的丢。但 sceneParamsDirty 用来让
                //   结束日志如实说明"期间有过被推迟的改动"，以前它永远是 false（死变量）。
                //
                sceneParamsDirty = true
                return
            }
            ctrl.setCaptureRequestOptions(b.build())
            if (recordingNow) {
                lastRecSceneSig = sig
                sceneParamsDirty = false
                Log.d(
                    "Camera",
                    "录制中：参数包已更新并下发（场景切换生效，且带完整 AWB/AE 锁）"
                )
            } else {
                // 退出录制：清掉签名，下次录制从头比对
                lastRecSceneSig = ""
            }
        } catch (e: Exception) {
            Log.w("Camera", "下发场景参数失败", e)
        }
    }

    //
    // ★回填 [applySceneParamsRef]★
    //
    // 到这里 applySceneParams 已经声明完了，把引用槽填上，
    // 于是前面（requestLensSwitchIfNeeded 里）的 setStabilizationRaw 也能调到它。
    // 这是绕开 Kotlin"局部函数必须先声明后使用"的正经做法。
    //
    applySceneParamsRef = { stabOverride, afSmooth ->
        applySceneParams(stabOverride = stabOverride, afSmooth = afSmooth)
    }

    /**
     * 录像防抖开关。CameraX 1.3.4 还没有 setVideoStabilizationEnabled（1.4.0 才加），
     * 所以用 Camera2Interop 直接下发 CONTROL_VIDEO_STABILIZATION_MODE。
     * ⚠️ setCaptureRequestOptions 是全量替换，所以专业模式下必须走 applyProParams
     *    把防抖和手动参数一起下发，否则两边会互相清掉。
     * ⚠️ 必须定义在 applyProParams 之后（Kotlin 局部函数只能引用前面已声明的）。
     */
    fun applyStabilization(enabled: Boolean) {
        val cam = camera ?: return
        // ⚠️ on 必须先算出来：下面专业模式分支也要用它
        val on = enabled && wantStabilization()
        if (mode == CaptureMode.PRO && manualControlSupported) {
            applyProParams(
                proIso, proShutterNs, proEv, proFocus, proWb,
                stabOverride = on
            )
            return
        }
        applySceneParams(stabOverride = on)
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
        stabMode = s.stabMode
        //
        // ★恢复 RAW 开关★
        //   只恢复用户的选择，【不保证真的生效】——
        //   绑定时会再探测一次本机能力，不支持就自动退回 JPEG
        //   （见 toggleRaw 与绑定流程里的 rawSupported 判定）。
        //
        rawEnabled = s.rawEnabled
        rawForce = s.rawForce
        rawSingleOutput = s.rawSingleOutput
        shutterSound = s.shutterSound
        swipeModeEnabled = s.swipeModeEnabled
        autoRotate = s.autoRotate
        proIso = s.proIso
        proShutterNs = s.proShutterNs
        proEv = s.proEv
        proFocus = s.proFocus
        proWb = s.proWb
        toneWarmth = s.toneWarmth
        toneTint = s.toneTint
        toneIntensity = s.toneIntensity
        //
        // ★恢复 UI隐藏开关★
        //   只恢复【开关】，不恢复"当前 UI 藏没藏" ——
        //   uiHidden 恒定以 false 起步，下次打开相机一定是正常显示。
        //
        uiHideEnabled = s.uiHideEnabled
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
        stabMode = stabMode,
        rawEnabled = rawEnabled,
        rawForce = rawForce,
        rawSingleOutput = rawSingleOutput,
        shutterSound = shutterSound,
        swipeModeEnabled = swipeModeEnabled,
        autoRotate = autoRotate,
        zoom = selectedZoom,
        proIso = proIso,
        proShutterNs = proShutterNs,
        proEv = proEv,
        proFocus = proFocus,
        proWb = proWb,
        toneWarmth = toneWarmth,
        toneTint = toneTint,
        toneIntensity = toneIntensity,
        uiHideEnabled = uiHideEnabled
    )

    //
    // ── 环境光档位变化时重新下发曝光参数 ──
    //
    // 少了这一步的话，lux 变了只是 State 变了，
    // 相机那边拿到的还是上一次的 AE 帧率区间 —— 走进暗房间的画面不会变亮。
    //
    // ⚠️ key 用【档位】而不是 lux 原值：lux 是连续量，
    //    每变一点就下发一次会不停重建 repeating request（= 闪色 / 卡顿）。
    //    档位一共五六档，跨档才下发，频率很低。
    //
    // ⚠️ 录制中不拦：applySceneParams 内部有去重，签名相同时自动跳过，
    //    所以录制中进暗处也会正确地更新一次（切镜头同理）。
    //
    LaunchedEffect(
        autoExposurePlan(ambientLux, false).fpsMin,
        autoExposurePlan(ambientLux, false).fpsMax,
        settingsLoaded
    ) {
        if (!settingsLoaded) return@LaunchedEffect
        if (mode == CaptureMode.VIDEO) return@LaunchedEffect   // 录像帧率由用户锁定
        if (mode == CaptureMode.PRO && manualControlSupported) return@LaunchedEffect
        if (ambientLux < 0f) return@LaunchedEffect              // 无传感器，无需调整
        applySceneParams()
    }

    // ── 设置自动保存（防抖 400ms）──
    // 好处：就算 App 被系统直接杀掉（不走 onPause），最近一次操作也大概率已经落盘
    LaunchedEffect(
        settingsLoaded, lensFacing, mode, photoRatio, highResMode, flashMode,
        videoQuality, videoFrameRate, stabMode, rawEnabled, rawForce, rawSingleOutput,
        autoRotate, selectedZoom,
        proIso, proShutterNs, proEv, proFocus, proWb,
        toneWarmth, toneTint, toneIntensity, uiHideEnabled
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

    //
    // ── 释放快门声资源 ──
    // AudioTrack 是系统混音器里的一个实体，单线程池也会一直挂着一条非守护线程。
    // 以前只 play() 不 release()，整个进程生命周期内都占着，属于泄漏。
    //
    DisposableEffect(Unit) {
        onDispose { ShutterSound.release() }
    }

    // ── 界面方向 ──
    // ★UI 锁定竖屏★：LOCK_UI_PORTRAIT 打开后，Activity 恒定 PORTRAIT，
    // 界面既不重排也不旋转；手机横过来只是整个竖屏界面侧躺，文字由 labelRotationDeg 转正。
    // （关掉这个开关就回到原行为：跟随传感器 / 跟随系统）
    LaunchedEffect(autoRotate) {
        val activity = context as? ComponentActivity ?: return@LaunchedEffect
        activity.requestedOrientation = if (LOCK_UI_PORTRAIT) {
            // PORTRAIT 而不是 FULL_SENSOR：前者是"锁死竖屏"，后者是"跟着转"
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else if (autoRotate) {
            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        Log.d("Camera", "界面方向: LOCK_UI_PORTRAIT=$LOCK_UI_PORTRAIT 自动旋转=$autoRotate")
    }

    //
    // ── 沉浸式全屏 ──
    //
    // 两个来源，任一为真就隐藏系统栏（状态栏 + 导航栏）：
    //   ① 横屏：竖向空间紧张，系统状态栏横在顶部压住按钮、导航栏占掉底部挤到快门。
    //   ② ★UI隐藏（双击空白处）★：此时界面上只剩画面，顶上留一条状态栏
    //      就"不像全屏"了。隐藏后软件真正置于顶层，整块屏幕都是画面。
    //
    // ⚠️★必须写成一个 DisposableEffect、key 同时包含两者★
    //   如果拆成两个（各自 key 只含一个），那么另一个变化时这个 effect 不重建，
    //   而先重建的那个 effect 的 onDispose 会执行 show() ——
    //   结果就是"刚隐藏又被显示回来"，两个来源互相打架。
    //   合成一个之后，任一变化都重新计算，永远以最新状态为准。
    //
    // BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE：需要看时间时从边缘滑一下即可临时唤出。
    //
    DisposableEffect(isLandscape, uiHidden) {
        val window = (context as? ComponentActivity)?.window
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (isLandscape || uiHidden) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                Log.d(
                    "Camera",
                    "沉浸式全屏（${if (uiHidden) "UI隐藏" else "横屏"}）：隐藏状态栏与导航栏"
                )
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

    // 注意：stabMode 不放这里——它只是下发一个 Camera2 参数，
    // 不需要重新绑定（重绑会黑屏一下）。防抖单独用下面的 LaunchedEffect 处理。
    LaunchedEffect(
        settingsLoaded, lensFacing, highResMode, photoRatio,
        videoQuality, videoFrameRate, boundLensId,
        // ★切 RAW 必须重绑★：输出格式是建 ImageCapture 用例时定下的，
        //   改不了已绑定的用例，只能重建（跟 highResMode 一个道理）
        rawEnabled, rawForce, rawSingleOutput,
        // ★必须带上★：机型档案换了要强制重跑整套绑定 / 扫描 / 标定流程
        deviceEpoch
    ) {
        // 设置还没恢复完就别绑，否则会用默认值绑一次再重绑一次
        if (!settingsLoaded) return@LaunchedEffect

        // ★★通过 PHYSICAL_CAMERA_ID 切镜头时【不重新绑定】★★
        // 会话一直是逻辑摄像头，只是每帧指定出图的物理头。
        // 这里直接下发参数包 + 落位倍率，省掉 unbind/bind 的黑屏和比例跳动。
        if (boundViaRequest && boundLensId != null && physicalRequestWorks) {
            Log.d("Camera", "★下发 PHYSICAL_CAMERA_ID=${boundLensId}（不重新绑定）★")
            applySceneParams()
            val t0 = pendingZoomTarget
            if (t0 > 0f) {
                val f0 = boundLensFactor()
                // 物理头收敛慢一点，给 HAL 一点时间（跟重新绑定那条路一致）
                delay(DeviceCompatibility.teleSettleDelayMs)
                val local0 = (t0 / f0.coerceAtLeast(0.01f)).coerceAtLeast(1f)
                camera?.cameraControl?.setZoomRatio(local0)
                displayedRatio = t0
                currentRatio = t0
                selectedZoom = nearestStop(t0)
                pendingZoomTarget = 0f
                Log.d(
                    "Camera",
                    "PHYSICAL_CAMERA_ID 切头就位：本地 ${"%.2f".format(local0)}x → 全局 ${t0}x"
                )
            }
            lastBoundWasViaRequest = true
            // ★切头前挂起的防抖在这里恢复★
            // 这个分支是【提前返回】，不会走到函数末尾那次恢复 ——
            // 漏掉的话 stabSuspended 永远是 true，防抖再也回不来。
            if (stabSuspended) {
                stabSuspended = false
                Log.d("Camera", "★切镜头完成（PHYSICAL_CAMERA_ID 路径），防抖已恢复★")
            }
            lensSwitching = false
            scanDone = true
            return@LaunchedEffect
        }

        // 从 request 切的物理头退回逻辑摄像头：会话没变过，也【不需要重绑】，
        // 只要把 PHYSICAL_CAMERA_ID 从参数包里去掉即可。
        if (boundLensId == null && lastBoundWasViaRequest) {
            Log.d("Camera", "清除 PHYSICAL_CAMERA_ID，回到逻辑摄像头（不重新绑定）")
            lastBoundWasViaRequest = false
            // 同上：这也是提前返回分支，必须在这里恢复防抖
            if (stabSuspended) {
                stabSuspended = false
                Log.d("Camera", "★切镜头完成（退回逻辑头），防抖已恢复★")
            }
            applySceneParams()
            val t1 = pendingZoomTarget
            if (t1 > 0f) {
                val c1 = t1.coerceIn(minZoomRatio, maxZoomRatio)
                camera?.cameraControl?.setZoomRatio(c1)
                displayedRatio = c1
                currentRatio = c1
                selectedZoom = nearestStop(c1)
                pendingZoomTarget = 0f
            }
            lensSwitching = false
            scanDone = true
            return@LaunchedEffect
        }

        var waited = 0
        while (previewViewRef == null && waited < 3000) {
            delay(50); waited += 50
        }
        val pv = previewViewRef
        if (pv == null) {
            // PreviewView 还没建好就退出：把切换遮罩收掉，避免永久黑屏
            lensSwitching = false
            return@LaunchedEffect
        }

        if (DeviceCompatibility.waitForLayoutBeforeBind) {
            var waitLayout = 0
            while ((pv.width <= 0 || pv.height <= 0) && waitLayout < 2000) {
                delay(30)
                waitLayout += 30
            }
        }

        // 固定等待：只是给 PreviewView 一点喘息。快切模式下再压到 8ms
        if (!hasCalibrated) scanDone = false
        delay(if (DeviceCompatibility.fastLensSwitch) 8L else 30L)

        try {
            // ★两次探测并行发起★
            // probeLenses 和 probePhysicalCameras 都要遍历 CameraCharacteristics，
            // 串行跑是两份完整耗时；它们互不依赖，用 async 同时跑能省掉一半。
            // ★快切★：同一个朝向的镜头表只扫一次，换头时直接复用。
            // 这两次 CameraCharacteristics 遍历（probeLenses + probePhysicalCameras）
            // 是「切镜头慢」最大的一块开销，换头场景下完全没必要重跑。
            val reuseScan = DeviceCompatibility.reuseLensScan && LensScanCache.hit(lensFacing)
            val lensesDeferred = if (reuseScan) null else async(Dispatchers.IO) {
                DeviceCompatibility.probeLenses(context, lensFacing)
            }
            val physicalDeferred = if (reuseScan) null else async(Dispatchers.IO) {
                DeviceCompatibility.probePhysicalCameras(context, lensFacing)
            }
            if (reuseScan) {
                LensScanCache.noteReuse()
                Log.d("Camera", "复用镜头扫描缓存（快速切镜头）")
            }

            // 每次打开/切换相机都重新读取原厂镜头参数（焦距、传感器尺寸、光圈），
            // 后面的变焦档位、标定、吸附全部基于这份真实光学数据
            val lenses: List<DeviceCompatibility.LensProfile> =
                lensesDeferred?.await() ?: LensScanCache.lenses ?: emptyList()
            lensProfiles = lenses
            // 光学档位：只记真实物理头的倍率，供吸附 / 高画质模式 / 镜头标签使用。
            // 按钮档位不直接用它（见下方），因为很多机器没有 2x 物理头，会导致 2x 按钮消失。
            // ⚠️ knownOpticalStops 是 DeviceCompatibility 的成员，必须带前缀，
            //    裸写会报 Unresolved reference
            // ★读取原相机焦段★（系统属性 + 厂商配置文件，读不到就空，不阻塞主流程）
            val vendorStops = DeviceCompatibility.readVendorZoomStops(lensFacing)
            if (vendorStops.isNotEmpty()) {
                Log.d("Camera", "★原相机焦段★ ${vendorStops.joinToString("/")}")
            }
            // ★取整★：3.2x / 2.4x / 3.5x 这类真实焦距一律换成整数档，
            //   物理头直连挑头仍用真实倍率，不受影响
            opticalStops = normalizeStops(
                when {
                    // ① 优先 Camera2 实测到的物理头等效焦距
                    lenses.size >= 2 -> lenses.map { it.zoomFactor }.distinct().sorted()
                    // ② 其次原厂配置文件里读出来的焦段
                    vendorStops.size >= 2 -> vendorStops
                    // ③ 都拿不到才用出厂档位兜底
                    else -> DeviceCompatibility.knownOpticalStops
                }
            )
            zoomStopsFromHardware = opticalStops.isNotEmpty()
            Log.d("Camera", "光学档位: ${opticalStops.joinToString("/").ifEmpty { "未探测到" }}")

            // ★独立扫描真实可绑定物理头★
            // 这一步不看 probeLenses 的结果，直接问 CameraManager 要 cameraId 列表，
            // 保证"物理直连"有真实 id 可用 —— 广角能不能切出去全靠它。
            // 直接取并行任务的结果（已经在跑或已跑完，这里几乎不额外耗时）
            physicalLensOptions = physicalDeferred?.await() ?: LensScanCache.physical ?: emptyList()
            // 首次扫描结果入缓存，之后换头直接复用
            if (!reuseScan) LensScanCache.put(lensFacing, lenses, physicalLensOptions)
            //
            // ★扫出来是空的 → 主动作废缓存★
            // 后置摄像头不可能没有镜头表。扫空只可能是这次探测失败
            // （相机服务未就绪 / HAL 异常 / 权限未授予），
            // 而不是"这台机器没有摄像头"。作废掉，下次强制重扫。
            // 否则就是上面 LensScanCache 注释里那条永久卡死链。
            //
            if (!isFront && lensProfiles.isEmpty()) {
                LensScanCache.markInvalid("后置扫描结果为空")
            }
            if (physicalLensOptions.size >= 2) {
                // 扫到多颗真实头，说明确实是多摄机，光学档位以这份真实数据为准
                val realStops = normalizeStops(
                    physicalLensOptions.map { it.zoomFactor }.distinct().sorted()
                )
                if (realStops.size >= 2) {
                    opticalStops = realStops
                    zoomStopsFromHardware = true
                    Log.d(
                        "Camera",
                        "光学档位已按真实物理头修正(已取整): ${realStops.joinToString("/")} " +
                                "（原始: ${physicalLensOptions.map { it.zoomFactor }.distinct().sorted().joinToString("/")}）"
                    )
                }
            }

            // 扫描完就打一份，先看清楚这台机器到底扫到了什么
            Log.d(
                "Camera",
                "★物理头池★ " + physicalLensOptions
                    .filter { DeviceCompatibility.isRealCameraId(it.id) }
                    .joinToString(", ") {
                        "${it.id}=${it.zoomFactor}x(焦距${"%.2f".format(it.focalLengthMm)}mm" +
                                "/等效${"%.1f".format(it.equiv35mm)}mm)"
                    }
            )

            // ★长焦直连的硬件兜底★
            // telePreferPhysical 的初值来自机型表 isKnownTeleStubborn，但型号号段太多
            // （OPPO 的 PHK/CPH、一加的 KB、realme 的 RMX…）总有识别不到的情况，
            // 识别不到 → 初值 false → 长焦头永远不直连 → "调不出长焦"。
            // 这里按【硬件事实】补一刀：后置扫到 ≥1.8x 的真实物理头，就说明本机确实
            // 有独立长焦头，开启直连。探测结果比型号表可靠。
            // ⚠️ 只在后置方向生效，前置不做。
            if (!isFront && !telePreferPhysical) {
                val hasRealTele = physicalLensOptions
                    .filter { DeviceCompatibility.isRealCameraId(it.id) }
                    .any { it.zoomFactor >= DeviceCompatibility.TELE_ENTER_RATIO }
                if (hasRealTele) {
                    telePreferPhysical = true
                    Log.d(
                        "Camera",
                        "★硬件兜底★ 后置检测到独立长焦头，已开启长焦物理直连" +
                                "（机型表未命中：MODEL=${Build.MODEL} DEVICE=${Build.DEVICE}）"
                    )
                }
            }

            // vivo 适配：getInstance().get() 会阻塞（vivo 上 HAL 初始化更慢），
            // 必须切到 IO 线程，否则主线程掉帧甚至 ANR
            val provider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }

            //
            // ★★绑定前先探 RAW 能力★★
            //   输出格式是【建 ImageCapture 用例时】定下的，绑完再探就晚了 ——
            //   那一轮绑定根本没设 RAW，要等下一次重绑（切镜头/改设置）才生效。
            //   这正是"原厂相机支持 RAW，本软件点了却不支持"的头号成因。
            //
            //   ⚠️ 按 lensFacing 分开缓存：前后摄能力不同，
            //      探了后摄不能拿结论去用在前摄上（跟 uhd60Supported 一个道理）。
            //
            run {
                val cached = rawSupportByFacing[lensFacing]
                if (cached == null) {
                    val r = RawMode.probeBeforeBind(provider, context, lensFacing)
                    val ok = r.supported || rawForce
                    rawSupportByFacing = rawSupportByFacing + (lensFacing to ok)
                    rawSupported = ok
                    rawProbeDetail = r.detail
                    Log.d(
                        "Camera",
                        "★RAW 判定（朝向=$lensFacing）：支持=$ok " +
                                "[CameraX=${r.cameraX} HAL=${r.hal} 强制=$rawForce] ${r.detail}"
                    )
                } else {
                    rawSupported = cached
                }
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
            // 抽成函数：下面分级降级时会重建 Preview，每次重新 setSurfaceProvider 是安全的
            val previewSelector = ResolutionSelector.Builder().also { applyAspect(it) }.build()
            fun newPreview(withResolution: Boolean): Preview =
                Preview.Builder()
                    .apply { if (withResolution) setResolutionSelector(previewSelector) }
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

            val baseSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            // ── 物理摄像头直连 ──
            // 小米 / iQOO 上逻辑摄像头切不动副摄时，直接按 cameraId 绑定那颗头，
            // 等价于原厂 App 的"切镜头"。
            val lensId = boundLensId
            // ⚠️ 这里原来也是用 physicalSwitchWorks 一刀切。
            //    广角失败关掉总闸后，长焦头也会被判成"不走物理直连"，直接退回逻辑头。
            //    改成按【这颗头自己是哪一类】判定。
            val lensFactor: Float = if (lensId != null) {
                (lensProfiles + physicalLensOptions)
                    .filter { DeviceCompatibility.isRealCameraId(it.id) }
                    .firstOrNull { it.id == lensId }?.zoomFactor ?: 1f
            } else 1f
            val usePhysical = lensId != null &&
                    physicalPathOpen(lensFactor) &&
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

            // ★长焦头常见坑③：主摄那套分辨率它不接受★
            // iQOO 10 Pro 的长焦只有 14.6MP，真我 GT5 Pro 的潜望也不吃 VideoCapture 的
            // 4K + 三流共存，拿主摄探出来的最大 JPEG 去绑，CameraX 会抛
            // "No supported surface combination"。
            // 老代码只试 2 档就 physicalSwitchWorks = false 永久封杀 —— 长焦再也调不出来。
            // 现在按「带不带录像流 × 带不带分辨率约束」四档降级，只有全败才认输。
            val physMaxJpeg: Size? = if (targetId != null) {
                physicalLensOptions.firstOrNull { it.id == targetId }
                    ?.let {
                        if (it.maxJpegW > 0 && it.maxJpegH > 0) Size(it.maxJpegW, it.maxJpegH)
                        else null
                    }
            } else null
            if (targetId != null) {
                Log.d(
                    "Camera",
                    "物理头 $targetId JPEG 上限=" +
                            "${physMaxJpeg?.width ?: "?"}x${physMaxJpeg?.height ?: "?"}" +
                            "（逻辑头最大 ${maxPhotoPixels}px）"
                )
                // UI 上高像素数字要跟着这颗头走，别再显示主摄的 5000 万
                physMaxJpeg?.let { maxPhotoPixels = it.width * it.height }
            }

            var bound = false
            // 成功绑定的那个 Preview，绑定完成后要把 surface 再贴一次（见下方说明）
            var boundPreview: Preview? = null

            // ★切物理头前改用 TextureView（COMPATIBLE）★
            // 这是"切到超广角后直接不显示画面"的根因之一：
            //   PreviewView 默认 PERFORMANCE(SurfaceView)。SurfaceView 的 Surface 尺寸是
            //   创建时按【当前 cameraId 的预览分辨率】定死的；切到物理超广角头后分辨率变了，
            //   部分 ROM（华为 P / 红米 Note / OPPO Find 都有反馈）不会重新 request surface，
            //   surface 一直停在旧尺寸上 —— 相机还在出帧，画面却永远贴不上去。
            //   TextureView 没有这个限制，分辨率变了会自己重新布局。
            // 代价是多一次纹理拷贝，所以只在真要换物理头时切，切回逻辑头再切回来省电。
            if (usePhysical) {
                if (pv.implementationMode != PreviewView.ImplementationMode.COMPATIBLE) {
                    Log.d("Camera", "切物理头：PreviewView 切到 COMPATIBLE(TextureView) 避免 surface 不重建")
                    pv.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            } else {
                // 回到逻辑摄像头：恢复 PERFORMANCE(SurfaceView)，省一次纹理上传
                if (pv.implementationMode != PreviewView.ImplementationMode.PERFORMANCE) {
                    pv.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                }
            }

            // (带 VideoCapture, 带分辨率约束)
            val plans: List<Pair<Boolean, Boolean>> = if (usePhysical)
                listOf(true to true, true to false, false to true, false to false)
            else if (rawEnabled && rawSupported == true)
            //
            // ★RAW 时多给两条不带录像流的降级路径★
            //   RAW_SENSOR 是超大尺寸流，跟 Preview + VideoCapture 三流共存
            //   在部分 HAL 上会被拒（"No supported surface combination"）。
            //   平时只试两种带录像流的组合，开了 RAW 就得允许放弃录像流，
            //   否则绑定全失败 —— 表现是"切到 RAW 就黑屏"。
            //   放弃录像流的代价只是录像模式要重绑一次，比黑屏好得多。
            //
                listOf(true to true, false to true, true to false, false to false)
            else listOf(true to true, false to true)

            for ((withVideo, withResolution) in plans) {
                try {
                    provider.unbindAll()
                    val pvUse = newPreview(withResolution)

                    val icUse = ImageCapture.Builder()
                        .apply {
                            // 物理头：不锁主摄尺寸，让 CameraX 按这颗头自己的能力挑
                            // （默认就是这颗头支持的最大/最优，等价于原厂切镜头）
                            if (withResolution && physMaxJpeg == null) {
                                setResolutionSelector(resolutionSelector)
                            }
                            setCaptureMode(
                                if (highResMode) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                                else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                            )
                            //
                            // ★RAW 输出格式★
                            //   只在【本机支持】时才真的设 —— 见 rawSupported 的判定。
                            //   RAW_JPEG = 一次拍摄同时出 DNG + JPEG，
                            //   设失败（旧 CameraX / HAL 不配合）就退回默认 JPEG，
                            //   不会让整个绑定失败。
                            //
                            if (rawEnabled && (rawSupported == true || rawForce)) {
                                if (RawMode.applyRawFormat(this, rawSingleOutput)) {
                                    Log.d("Camera", "★RAW 输出格式已启用（DNG + JPEG 双输出）★")
                                } else {
                                    Log.w("Camera", "RAW 格式设置失败，本次退回 JPEG")
                                }
                            }
                        }
                        .build()

                    val vcUse = if (withVideo && !highResMode) {
                        //
                        // 4K60 编码器减负：「花屏」最字面的成因是编码器过载产生坏帧 ——
                        // 4K60 原始数据量是 1080p30 的 8 倍以上，部分机型 AVC 硬编码器
                        // 跑不到默认码率，间歇性吐坏帧（局部色块错乱、颜色一阵一阵跳）。
                        // 这里显式给保守码率上限，把编码器拉回它稳得住的区间。
                        // ⚠️ 报 "Unresolved reference: setTargetVideoEncodingBitRate" 说明
                        //    CameraX 版本还没有这方法，删掉这行即可，其余修复不受影响。
                        //
                        // 拍摄前先探测这个分辨率跑不跑得到这个帧率：vivo 上"无法拍摄 4K60"
                        // 是机器 HAL 的 4K 输出只到 30fps，而 CameraX 不会报错 ——
                        // 点了要么静默录成 30、要么绑定失败退回 1080p。
                        // 跑不到就【降帧保分辨率】：4K60 → 4K30，而不是让它自己掉到 1080p。
                        //
                        val wantFps: Int = if (
                            videoQuality == VideoQuality.UHD &&
                            videoFrameRate == VideoFrameRate.FPS_60
                        ) {
                            val ok = uhd60Supported ?: run {
                                val maxF = DeviceCompatibility.maxFpsForSize(
                                    context, lensFacing, 3840, 2160
                                )
                                Log.d("Camera", "4K 帧率能力探测：最高 ${maxF}fps")
                                (maxF >= 58).also { uhd60Supported = it }
                            }
                            if (!ok) {
                                Log.w(
                                    "Camera",
                                    "★本机 4K 跑不到 60fps，4K60 已自动降为 4K30★" +
                                            "（保分辨率优先；想用 60 帧请切到 1080P）"
                                )
                                30
                            } else videoFrameRate.fps
                        } else videoFrameRate.fps

                        val bitrate = videoBitrateFor(
                            videoQuality,
                            if (wantFps >= 60) VideoFrameRate.FPS_60 else VideoFrameRate.FPS_30
                        )
                        val recorder = Recorder.Builder()
                            .setQualitySelector(
                                QualitySelector.from(
                                    videoQuality.quality,
                                    FallbackStrategy.higherQualityOrLowerThan(Quality.FHD)
                                )
                            )
                            .setTargetVideoEncodingBitRate(bitrate)
                            .build()
                        Log.d(
                            "Camera",
                            "录像码率上限 ${bitrate / 1_000_000}Mbps（" +
                                    "${videoQuality.label}@${wantFps}）"
                        )
                        //
                        // ★帧率下发：能给死就给死，给不了就交给 HAL 自适应★
                        //   固定 Range(60,60) 在部分 HAL 上会被整条拒绝（直接绑不上），
                        //   这时改成 Range(30, 60) 让它自己在区间里挑，至少能出片。
                        //
                        VideoCapture.Builder(recorder)
                            .setTargetFrameRate(
                                if (uhd60Supported == false && wantFps >= 60) Range(30, 60)
                                else Range(wantFps, wantFps)
                            )
                            .build()
                    } else null

                    camera = if (vcUse != null) {
                        provider.bindToLifecycle(lifecycleOwner, selector, pvUse, icUse, vcUse)
                    } else {
                        provider.bindToLifecycle(lifecycleOwner, selector, pvUse, icUse)
                    }
                    imageCapture = icUse
                    videoCapture = vcUse
                    bound = true
                    boundPreview = pvUse
                    //
                    // ★RAW 能力已在【绑定前】探测★（见上方 probeBeforeBind），
                    //   这里只做一次结果校验：格式真设上了但 CameraX 最终没走 RAW，
                    //   说明是分辨率/帧率组合约束 —— 打日志提示排查方向。
                    //
                    if (rawEnabled && rawSupported == true) {
                        val info = camera?.cameraInfo
                        if (info != null && !RawMode.supportsRaw(info)) {
                            Log.w(
                                "Camera",
                                "注意：绑定前判为支持，绑定后 CameraX 判为不支持 —— " +
                                        "多为分辨率/帧率组合约束，可试关高像素或降帧率"
                            )
                        }
                    }
                    Log.d(
                        "Camera",
                        "绑定成功：物理头=${targetId ?: "逻辑"} " +
                                "视频流=$withVideo 分辨率约束=$withResolution"
                    )
                    break
                } catch (e: Exception) {
                    Log.w(
                        "Camera",
                        "绑定失败（物理头=${targetId ?: "逻辑"} " +
                                "视频流=$withVideo 分辨率约束=$withResolution）",
                        e
                    )
                }
            }

            // ★绑定完成后把 surface 再贴一次★
            // 切镜头（尤其是切到物理超广角头）时，部分 ROM 上 PreviewView 不会主动
            // 重新请求 surface，相机其实一直在出帧，只是画面贴不到屏幕上。
            // 重新 setSurfaceProvider 会强制走一遍"请求 surface → 出帧"，
            // 是 CameraX 社区针对这个现象的通用 workaround。
            // 物理头多等一会儿：它的流起得比逻辑头慢，贴太早会被后续 unbind 冲掉。
            if (bound) {
                val settle = if (usePhysical) DeviceCompatibility.zoomSettleDelayMs else 0L
                launch {
                    // 快切模式：不再白等一整个收敛周期，60ms 足够把 surface 重新挂上
                    delay(if (DeviceCompatibility.fastLensSwitch) 60L else (settle + 120L))
                    try {
                        boundPreview?.setSurfaceProvider(pv.surfaceProvider)
                        Log.d("Camera", "已重新贴附预览 surface（物理头=${targetId ?: "逻辑"}）")
                    } catch (e: Exception) {
                        Log.w("Camera", "重新贴附预览 surface 失败", e)
                    }
                }
            }

            // ★物理头出流校验★
            // 绑上了 ≠ 有画面。部分机型物理头能 bindToLifecycle 成功，
            // 但预览流一直起不来（surface 贴不上 / HAL 不给帧），表现就是黑屏。
            // CameraX 的 PreviewView 会自己暴露预览流状态（IDLE / STREAMING），
            // 拿它做判据：超时还没 STREAMING 就判定这次物理直连失败，
            // 立刻退回逻辑摄像头，改由 setZoomRatio 去到目标倍率。
            if (bound && usePhysical) {
                //
                // ★★出流超时：防抖开着时用更短的超时★★
                //
                // 卡死的【体感】主要来自这里：默认 2.5s，一旦物理头（在防抖干扰下）
                // 起不来流，就要干等 2.5 秒 —— 期间黑遮罩盖着、UI 不动，
                // 用户看到的就是"卡住了"。四档降级的话最坏能到 10 秒。
                // 防抖开着时压到 900ms：快速失败、快速退回逻辑头，
                // 宁可偶尔多退一次，也不要让用户对着黑屏等。
                //
                val streamTimeout = if (stabSuspended || stabMode != StabMode.OFF)
                    PHYSICAL_STREAM_TIMEOUT_STAB_MS else PHYSICAL_STREAM_TIMEOUT_MS
                val streamed = waitForPreviewStream(pv, streamTimeout)
                if (!streamed) {
                    val thatFactor = (lensProfiles + physicalLensOptions)
                        .firstOrNull { it.id == targetId }?.zoomFactor ?: 1f
                    if (thatFactor < 0.95f) {
                        physicalWideWorks = false
                        Log.w("Camera", "物理广角头 $targetId 无预览流，已封禁该路径（长焦不受影响）")
                    } else if (thatFactor >= DeviceCompatibility.TELE_ENTER_RATIO) {
                        // ★长焦无预览流：只封长焦，广角照旧★
                        teleBindFailCount++
                        Log.w("Camera", "长焦头 $targetId 无预览流（第 $teleBindFailCount 次）")
                        if (teleBindFailCount >= DeviceCompatibility.TELE_BIND_FAIL_LIMIT) {
                            telePreferPhysical = false
                            physicalTeleWorks = false
                            Log.w("Camera", "长焦连续无预览流 $teleBindFailCount 次，已禁用（广角不受影响）")
                        }
                        // ★慢性卡死循环的保护★
                        // 见 LENS_SWITCH_FAIL_LIMIT 的说明：累计到上限就彻底放弃直连
                        lensSwitchFailCount++
                        if (lensSwitchFailCount >= LENS_SWITCH_FAIL_LIMIT) {
                            telePreferPhysical = false
                            physicalTeleWorks = false
                            Log.w(
                                "Camera",
                                "切镜头连续失败 $lensSwitchFailCount 次，已彻底关闭长焦物理直连" +
                                        "（改由逻辑头变焦，避免反复卡住）"
                            )
                        }
                    } else {
                        Log.w("Camera", "物理头 $targetId 无预览流")
                    }
                    // ★切头前挂起的防抖在这里恢复★
                    // 这也是提前返回分支（跟上面两个一样），漏掉会让 stabSuspended 永久为 true。
                    // 而且这里防抖【必须】恢复：既然物理直连这条路走不通，
                    // 就应该让逻辑头带着防抖正常工作，而不是让防抖一直悬着。
                    if (stabSuspended) {
                        stabSuspended = false
                        Log.d("Camera", "★物理头出流失败，防抖已恢复（交回逻辑头）★")
                    }
                    // 退回逻辑摄像头：重绑后由 pendingZoomTarget 把倍率接上
                    pendingZoomTarget = pendingZoomTarget.takeIf { it > 0f } ?: displayedRatio
                    lensSwitching = true
                    boundLensId = null
                    return@LaunchedEffect
                }
            }

            // 四档全败：这台机器大概率不允许单独开物理头。
            // ⚠️ 原来是"一次失败就 telePreferPhysical = false 永久封禁" ——
            //    这是 OPPO Find 调不出长焦的成因③：绑定失败的原因可能是临时的
            //    （录制刚结束、分辨率瞬时冲突、HAL 忙），一次就封死等于这台机器
            //    这辈子都用不上物理长焦。改成累计 2 次才封。
            if (!bound && usePhysical) {
                // ⚠️ 原来这里无条件 physicalSwitchWorks = false 关总闸，见 [physicalPathOpen] 的说明
                val isTeleTarget = (lensProfiles + physicalLensOptions)
                    .firstOrNull { it.id == lensId }?.zoomFactor
                    ?.let { it >= DeviceCompatibility.TELE_ENTER_RATIO } ?: false
                if (isTeleTarget) {
                    // ★长焦失败只封长焦★：广角直连不受影响，别一刀切
                    teleBindFailCount++
                    Log.w("Camera", "长焦头 $lensId 绑定失败（第 $teleBindFailCount 次）")
                    if (teleBindFailCount >= DeviceCompatibility.TELE_BIND_FAIL_LIMIT) {
                        telePreferPhysical = false
                        physicalTeleWorks = false
                        Log.w("Camera", "长焦直连连续失败 $teleBindFailCount 次，已禁用该路径（广角不受影响）")
                    }
                } else {
                    // 广角 / 其它头失败：只封对应的那条路，长焦照旧可用
                    val thatFactor2 = (lensProfiles + physicalLensOptions)
                        .firstOrNull { it.id == lensId }?.zoomFactor ?: 1f
                    if (thatFactor2 < 0.95f) {
                        physicalWideWorks = false
                        Log.w("Camera", "物理广角头 $lensId 四档绑定全失败，已封禁（长焦不受影响）")
                    } else {
                        physicalSwitchWorks = false
                        Log.w("Camera", "物理头 $lensId 四档绑定全失败，已封禁该头（广角/长焦不受影响）")
                    }
                }
                try {
                    provider.unbindAll()
                    val pvBack = newPreview(true)
                    val icBack = ImageCapture.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setCaptureMode(
                            if (highResMode) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                            else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                        )
                        .build()
                    camera = provider.bindToLifecycle(lifecycleOwner, baseSelector, pvBack, icBack)
                    imageCapture = icBack
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
                    // 成功了就把失败计数清零，避免偶发失败被累计成永久封禁
                    teleBindFailCount = 0
                    lensSwitchFailCount = 0
                } else {
                    // ★按类型封★：广角被静默忽略不该连坐封掉长焦
                    val thatFactor3 = (lensProfiles + physicalLensOptions)
                        .firstOrNull { it.id == lensId }?.zoomFactor ?: 1f
                    when {
                        thatFactor3 < 0.95f -> {
                            physicalWideWorks = false
                            Log.w(
                                "Camera",
                                "广角头 $lensId 直连未生效（实际开的是 $actualId），已封禁广角路径（长焦不受影响）"
                            )
                        }
                        thatFactor3 >= DeviceCompatibility.TELE_ENTER_RATIO -> {
                            teleBindFailCount++
                            Log.w(
                                "Camera",
                                "长焦头 $lensId 直连未生效（实际开的是 $actualId，第 $teleBindFailCount 次）"
                            )
                            if (teleBindFailCount >= DeviceCompatibility.TELE_BIND_FAIL_LIMIT) {
                                telePreferPhysical = false
                                physicalTeleWorks = false
                                Log.w("Camera", "长焦直连连续失败，已禁用（广角不受影响）")
                            }
                        }
                        else -> {
                            physicalSwitchWorks = false
                            Log.w("Camera", "物理头 $lensId 直连未生效（实际开的是 $actualId）")
                        }
                    }
                }
            }

            // 录像防抖。CameraX 1.3.4 还没有 setVideoStabilizationEnabled（那是 1.4.0 才加的），
            // 所以走 Camera2Interop 直接下发 CONTROL_VIDEO_STABILIZATION_MODE。
            // 同时打开光学防抖(OIS)，iQOO 15 这类机器有独立的 OIS 马达。
            stabilizationSupported = DeviceCompatibility.isVideoStabilizationSupported(context, camera)
            //
            // ★切镜头前挂起的防抖，在这里恢复★
            // 绑定已经走完、预览流也确认起来了，EIS/OIS 可以安全重建稳定窗口。
            // 必须先清 stabSuspended：否则 applySceneParams 虽然会根据开关恢复防抖，
            // 但 stabSuspended 仍是 true，后续的拖动超时检测会再恢复一次（重复下发）。
            //
            if (stabSuspended) {
                stabSuspended = false
                Log.d("Camera", "★切镜头完成，防抖已恢复★")
            }
            // 统一参数包：帧率 + 降噪 + 边缘 + 防抖一次下发（避免互相覆盖）
            applySceneParams()

            // 绑定块里的 ic 局部变量已随分级降级改造移除，这里统一用 imageCapture 状态
            imageCapture?.let { ic ->
                ic.flashMode = when (flashMode) {
                    1 -> ImageCapture.FLASH_MODE_AUTO
                    2 -> ImageCapture.FLASH_MODE_ON
                    else -> ImageCapture.FLASH_MODE_OFF
                }
            }

            manualControlSupported = DeviceCompatibility.isManualControlAllowed(context, camera)
            manualLimits = DeviceCompatibility.probeManualLimits(context, camera)

            //
            // ★★ 等 zoomState 稳定后再读 ★★
            //
            // 修「每次打开相机变焦按键都不一样」的输入侧根因：
            //   CameraX 的 zoomState 是 StateFlow，刚 bindToLifecycle 完成时它还是初始占位值，
            //   HAL 真正把 min/max 报上来要几十到几百毫秒。
            //   直接读一次就拿来算档位 → 这次读到 1.0~10.0、下次读到 0.6~20.0，
            //   过滤条件 `it in min..max` 一变，按键数量和倍率就跟着变。
            // 这里轮询到【连续两次读到相同值】再往下走（最多 1.2 秒），
            // 保证每次启动拿到的都是同一份稳定数据。
            //
            run {
                var lastMin = -1f
                var lastMax = -1f
                var stableHits = 0
                val deadline = System.currentTimeMillis() + 1200L
                var firstMn = -1f
                var firstMx = -1f
                while (System.currentTimeMillis() < deadline) {
                    val zs = camera?.cameraInfo?.zoomState?.value
                    if (zs != null) {
                        val (mn0, mx0) = normalizeZoomRange(zs.minZoomRatio, zs.maxZoomRatio)
                        if (firstMn < 0f) {
                            firstMn = mn0
                            firstMx = mx0
                        }
                        if (abs(mn0 - lastMin) < 0.001f && abs(mx0 - lastMax) < 0.001f) {
                            stableHits++
                            if (stableHits >= 2) break
                        } else {
                            stableHits = 0
                            lastMin = mn0
                            lastMax = mx0
                        }
                    }
                    delay(40)
                }
                if (firstMn >= 0f && (abs(firstMn - lastMin) > 0.001f || abs(firstMx - lastMax) > 0.001f)) {
                    Log.d(
                        "Camera",
                        "zoomState 已稳定：首次读 ${firstMn}x~${firstMx}x → 稳定值 ${lastMin}x~${lastMax}x"
                    )
                }
            }

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
                    // 同时参考【真实物理头】的倍率：光学档位探测失败时 opticalStops 可能是空的，
                    // 但 physicalLensOptions 里那颗真实超广角头是实打实存在的
                    val realWide: Float? = physicalLensOptions
                        .filter { DeviceCompatibility.isRealCameraId(it.id) }
                        .filter { it.zoomFactor in 0.2f..0.95f }
                        .minByOrNull { it.zoomFactor }?.zoomFactor
                    val opticalMin: Float? = listOfNotNull(
                        opticalStops.filter { it > 0f }.minOrNull(), realWide
                    ).minOrNull()
                    val finalMin: Float =
                        if (opticalMin != null && opticalMin < mn - 0.01f) opticalMin else mn
                    if (finalMin < mn) {
                        Log.w(
                            "Camera",
                            "HAL 上报下限 ${mn}x 但存在 ${opticalMin}x 光学档，已放宽到 ${finalMin}x"
                        )
                    }
                    minZoomRatio = finalMin
                    halMaxZoomRatio = mx
                    maxZoomRatio = zoomCapFor(mx)

                    // ★检测"HAL 撒谎"★
                    // mn 是 HAL 原生上报的下限。它 >= 1.0 说明逻辑头根本不接受 <1.0 的倍率，
                    // 但本机光学档位里确实有 0.6x —— 那这个广角只能靠物理直连才拿得到。
                    val hasWideOptic: Boolean = opticalStops.any { it < 0.95f }
                    // ★本机是否真有一颗"能独立打开"的广角头★
                    // 光"存在广角"不够：它必须在 cameraIdList 里，否则 CameraX 绑不上。
                    val hasBindableWide: Boolean = physicalLensOptions.any {
                        DeviceCompatibility.isRealCameraId(it.id) &&
                                DeviceCompatibility.canBindDirectly(it.id) &&
                                it.zoomFactor in 0.2f..0.95f
                    }
                    // ★红米 Note 数字系列 / OPPO Find★：实测逻辑头 setZoomRatio(0.6) 完全无效，
                    // 直接判定"广角必须走物理直连"，省掉一次注定失败的下发 + 校验。
                    //
                    // ⚠️ 但只有在【真的有可直连的广角头】时才这么判：
                    //    Find X7 逻辑头下限是 0.7x（本身就是超广角在工作），
                    //    而超广角物理头不在 cameraIdList 里绑不上 ——
                    //    强制走物理直连只会失败并永久封禁，0.6x 反而彻底用不了。
                    //    这种情况就该让逻辑头接手（能到 0.7x，已经是最广）。
                    val blocked: Boolean = hasWideOptic &&
                            (mn >= 0.99f ||
                                    (DeviceCompatibility.preferPhysicalWide && hasBindableWide))
                    if (DeviceCompatibility.preferPhysicalWide && !hasBindableWide && mn < 0.99f) {
                        Log.w(
                            "Camera",
                            "本机标记为'广角需物理直连'，但没有可独立打开的广角头，" +
                                    "改由逻辑摄像头承担（下限 ${mn}x）"
                        )
                    }
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
            // ★原相机焦段也生成按键★：原厂 App 上有的档位，这里就应该有
            merged.addAll(vendorStops)
            // 最大数码变焦也补一个档位，但只在合理范围内。
            // iQOO 15 这类机器 zoomState 会报 100x，做成按钮没意义还挤爆一排。
            if (maxZoomRatio <= MAX_USEFUL_STOP) merged.add(maxZoomRatio)
            // ★取整放在过滤【之前】★：先合并去重，再统一取整，最后再过滤 + 去重。
            // 顺序很重要：如果先按原始值过滤，3.2x 可能因为超出 maxZoomRatio 被砍掉，
            // 而取整后的 3x 本来是合法档位；反过来先取整再过滤才是对的。
            val mergedInt = normalizeStops(merged)
            val freshStops = mergedInt
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

            //
            // ★★ 稳定化：本次探测结果 vs 存档，择优采用 ★★
            //
            // 修「每次打开相机变焦按键都不一样」：
            //   · 有存档 → 默认【沿用存档】，按键数量与倍率每次都一样
            //   · 只有本次探测确实更完整（光学档更多 / 是存档的超集）才更新存档
            //   · 首次启动没存档 → 用本次结果并存档
            // 这样既不丢新信息，又不会出现"开三次相机三个样"。
            //
            val cachedStops = ZoomStopsStore.load(context, lensFacing)
            val cachedOptical = ZoomStopsStore.loadOptical(context, lensFacing)
            val better = zoomStopsBetter(cachedStops, cachedOptical, freshStops, opticalStops)
            zoomStops = when {
                cachedStops.isEmpty() -> {
                    ZoomStopsStore.save(context, lensFacing, freshStops, opticalStops)
                    Log.d("Camera", "首次使用，存档本次档位")
                    freshStops
                }
                better -> {
                    ZoomStopsStore.save(context, lensFacing, freshStops, opticalStops)
                    Log.d(
                        "Camera",
                        "探测更完整（光学 ${cachedOptical.size}→${opticalStops.size} 个），更新存档\n" +
                                "  旧: ${cachedStops.joinToString("/")}\n" +
                                "  新: ${freshStops.joinToString("/")}"
                    )
                    freshStops
                }
                else -> {
                    Log.d(
                        "Camera",
                        "沿用存档档位（本次探测未更完整，避免按键抖动）\n" +
                                "  存档: ${cachedStops.joinToString("/")}\n" +
                                "  本次: ${freshStops.joinToString("/")}"
                    )
                    // 光学档位仍用本次实测的（它是"这颗头是不是物理头"的判据，
                    // 用存档的旧值反而可能不准），但【按键档位】保持存档不变
                    cachedStops
                }
            }
            Log.d(
                "Camera",
                "按钮档位: ${zoomStops.joinToString("/")} " +
                        "(光学=${opticalStops.size}个, 硬件上限=${maxZoomRatio}x)"
            )
            zoomStopsReady = true

            // 物理头直连时不做标定：标定是靠"逻辑摄像头的 zoomState"反推 linearZoom，
            // 单颗物理头没有多摄切换，标定出来的表是错的，还会把倍率搞乱。
            if (!hasCalibrated && zoomStopsReady &&
                lensFacing == CameraSelector.LENS_FACING_BACK && boundLensId == null
            ) {
                // 提前返回前必须复位：否则切换遮罩永远盖着，看起来就是"不显示画面"
                val cam = camera
                if (cam == null) {
                    lensSwitching = false
                    scanDone = true
                    zoomStopsReady = true
                    return@LaunchedEffect
                }
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
                // 用合并池查倍率：物理头 id 来自 physicalLensOptions，
                // 只查 lensProfiles 会兜底成 1f，长焦头就被当成 1x 数码变焦了
                val factor: Float = (lensProfiles + physicalLensOptions)
                    .filter { DeviceCompatibility.isRealCameraId(it.id) }
                    .firstOrNull { it.id == lid }?.zoomFactor ?: 1f
                val target = pendingZoomTarget
                if (target > 0f) {
                    // 潜望 / 长焦收敛更慢（马达行程 + 重新对焦），绑上来立刻下发会被 HAL 吞掉，
                    // 表现就是"切到 3x 后画面还停在 1x"。这里多等一会儿再落位。
                    if (factor >= DeviceCompatibility.TELE_ENTER_RATIO) {
                        delay(DeviceCompatibility.teleSettleDelayMs)
                    }
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
            lensSwitching = false

            if (mode == CaptureMode.PRO && manualControlSupported) {
                delay(DeviceCompatibility.manualSettleDelayMs)
                applyProParams(proIso, proShutterNs, proEv, proFocus, proWb)
            }
        } catch (e: Exception) {
            //
            // ★★绑定异常时也要把 scanDone 置回 true★★
            //
            // 这是「间接性卡死」最硬的一处：
            //   scanDone 是所有相机操作的闸门 ——
            //     smoothZoomTo / directSetRatio / takePhoto / 拖动手势 开头都是
            //     `if (!scanDone) return`，变焦按钮的 enabled 也是 scanDone && …
            //   只要它停在 false，整台机器就【全部操作无反应】：点档位没反应、
            //   拍照没反应、拖动没反应 —— 界面还在、也不崩，就是"卡死"。
            //
            //   绑定流程里任何一步抛异常（CameraCharacteristics 读取失败、
            //   bindToLifecycle 冲突、ROM 定制层的 RuntimeException…）
            //   都会跳到这个 catch，而以前这里只收了 lensSwitching，
            //   scanDone 就永远停在那次置 false 的时刻。
            //
            //   现在异常分支把它置回 true：参数可能是旧的/不完整的，
            //   但至少相机能用，比整台机器点什么都没反应好得多。
            //
            lensSwitching = false
            scanDone = true
            if (stabSuspended) {
                stabSuspended = false
                Log.d("Camera", "绑定异常：防抖挂起标志已复位")
            }
            Log.e("Camera", "相机绑定流程异常（已置 scanDone=true 保底可用）", e)
            e.printStackTrace()
        }
        cameraReadyAfterSwitch = true
    }

    //
    // ★★模式切换：只在"从专业 / 星空切出来"时才清参数★★
    //
    // 老实现是"只要不是专业模式就 resetProParams()"，有两个问题：
    //
    //   ① 误伤普通切换：PHOTO → VIDEO 也会 clear，
    //      把刚下发的帧率 / 防抖 / 降噪 / 物理头参数一起清掉，
    //      画面要等下一轮 LaunchedEffect 才补回来。
    //
    //   ② 与下面那个 LaunchedEffect 抢顺序：
    //      resetProParams 的 clear 和 applySceneParams 的 set 是【并发】跑的，
    //      谁后执行谁赢。clear 后执行就把常规参数全清了，
    //      表现是"切完模式画面不对，过一两秒才恢复"。
    //
    // 现在改成：先记下"从哪个模式切出来"，只在真需要清的两种情况下清，
    // 并且【清完立刻 force 补一次常规参数】，顺序由代码保证，不再靠运气。
    //
    LaunchedEffect(mode, camera, manualControlSupported) {
        val from = modeBeforeSwitch
        modeBeforeSwitch = mode
        try {
            if (mode == CaptureMode.PRO && camera != null && manualControlSupported) {
                delay(DeviceCompatibility.manualSettleDelayMs)
                applyProParams(proIso, proShutterNs, proEv, proFocus, proWb)
                return@LaunchedEffect
            }
            if (from == CaptureMode.PRO) {
                // 只有从专业模式切出来才需要清手动 ISO / 快门
                resetProParams()
                applySceneParams(force = true)
                Log.d("Camera", "退出专业模式：已清空手动参数并重建常规参数")
            } else if (from == CaptureMode.STAR_TRAIL) {
                //
                // ★★退出星空：手动曝光必须彻底清掉★★
                //
                // 星空预览下发的是一套很"重"的手动参数：
                //   CONTROL_AE_MODE = OFF
                //   SENSOR_EXPOSURE_TIME = 最多 0.5s
                //   SENSOR_SENSITIVITY = 手动 ISO
                //   SENSOR_FRAME_DURATION = 最长 0.5s（等于把预览压到 2fps）
                //
                // 不清的话，切回拍照后 repeating request 还带着 FRAME_DURATION=500ms，
                // 预览卡在 2fps、曝光也还是星空那套 —— 就是用户说的
                // "以星空模式那个参数下的画面持续两秒才恢复"。
                //
                resetProParams()
                delay(80)   // 给 HAL 一帧时间退出 AE_MODE_OFF
                applySceneParams(force = true)
                Log.d("Camera", "退出星空模式：已清空手动曝光并重建常规参数")
            }
        } catch (e: Exception) {
            Log.w("Camera", "模式切换参数重建失败（相机可能正在重绑）", e)
        }
    }

    LaunchedEffect(camera, scanDone) {
        if (!scanDone) return@LaunchedEffect
        while (true) {
            //
            // ★★循环体包 try/catch★★
            //   这是 while(true) 的【常驻轮询】—— 一旦循环体内抛出未捕获异常，
            //   整个协程就结束了，而它没有 key 会再触发一次，于是【倍率回读彻底停摆】：
            //   手指拖动变焦时 UI 倍率不再更新、松手后也不再回读校准。
            //   表现就是"变焦怎么弄都没有反馈"。
            //   包一层之后：单帧出错只是跳过这一次，下一轮照常轮询。
            //
            try {
                camera?.cameraInfo?.zoomState?.value?.let {
                    // vivo 适配：变焦上下限跟着实际会话刷新。
                    // 这两个值基本不变，只有真的变了才写入，避免无谓重组
                    // vivo 适配：变焦上下限跟着实际会话刷新。
                    // 这两个值基本不变，只有真的变了才写入，避免无谓重组。
                    // 物理头直连时不能刷新 —— 那只是这颗头内部的局部范围。
                    if (boundLensId == null) {
                        val (mn, mx) = normalizeZoomRange(it.minZoomRatio, it.maxZoomRatio)
                        if (mn != minZoomRatio) minZoomRatio = mn
                        if (mx != halMaxZoomRatio) {
                            halMaxZoomRatio = mx
                            maxZoomRatio = zoomCapFor(mx)
                        }
                    }

                    // ★变焦动画进行中就不要插手★
                    // 动画那边正按 16ms 高频下发，轮询如果同时每 50ms 也来写
                    // displayedRatio，两股重组叠加起来正好把中间帧全吃掉——
                    // 这就是之前"看着像硬切"的直接原因。
                    if (!zoomAnimating[0]) {
                        // 物理头直连时 zoomState 报的是"这颗头内部"的倍率，
                        // 乘上这颗头的光学倍率才是用户认知里的全局倍率
                        val global: Float = it.zoomRatio * boundLensFactor()
                        //
                        // ★写状态前先去重★
                        //   currentRatio 是 CameraApp 的 State，写一次 = 整体重组一次。
                        //   录制中倍率根本不动，却每 50ms 写一次（每秒 20 次全量重组），
                        //   这是 4K60 录制卡顿的另一半来源。
                        //   值没实质变化就不写 —— 不写 = 不重组。
                        //
                        if (abs(global - currentRatio) > 0.0005f) currentRatio = global
                        if (!isDragging && (zoomJob == null || zoomJob?.isActive == false)) {
                            // ★扩展数码变焦期间保持用户请求的目标★
                            // HAL 会把倍率 clamp 到它自己的上限（比如只能到 10x），
                            // 而用户点的是 50x，此时不能拿回读值覆盖 UI，否则倍率会被拽回去。
                            if (boostTarget > 0f) {
                                if (abs(boostTarget - displayedRatio) > 0.0005f)
                                    displayedRatio = boostTarget
                            } else if (global <= maxRatio * 1.05f) {
                                if (abs(global - displayedRatio) > 0.0005f)
                                    displayedRatio = global
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("Camera", "倍率回读本轮出错（已跳过，轮询继续）", e)
            }
            //
            // ★录制中降频★：50ms → 250ms。
            //   录制时变焦是偶发操作，没必要用待机时的高频回读去换精度，
            //   省下来的 15 次/秒重组直接还给编码器。
            //
            delay(if (isRecording) 250L else 50L)
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

    // 防抖 / 帧率 / 降噪：开关、帧率或模式变化时重新下发统一参数包，不重新绑定相机
    LaunchedEffect(stabMode, videoFrameRate, mode, camera) {
        //
        // ★必须包 try/catch★
        //   LaunchedEffect 协程体里抛出未捕获异常会直接崩掉整个 Compose 作用域
        //   （不是"这个 effect 失效"那么简单，是【整页崩溃】）。
        //   而 applySceneParams 走 Camera2CameraControl，
        //   相机刚被 unbind（切镜头 / 换前后摄）的那一瞬间去下发参数就可能抛。
        //   这里挡掉：参数没下发成功大不了这一轮参数不对，下次变化还会再下发。
        //
        try {
            applySceneParams()
        } catch (e: Exception) {
            Log.w("Camera", "下发场景参数失败（相机可能正在重绑）", e)
        }
    }

    // ★照片 50x / 录像 15x★：模式切换时重算 UI 倍率上限（不重新绑定相机）
    LaunchedEffect(mode, halMaxZoomRatio) {
        // ★同上：整页崩溃防护★
        try {
            val cap = zoomCapFor(halMaxZoomRatio)
            if (cap != maxZoomRatio) maxZoomRatio = cap
            if (displayedRatio > maxZoomRatio) {
                val c = maxZoomRatio
                camera?.cameraControl?.setZoomRatio(clampForHal(c))
                displayedRatio = c
                currentRatio = c
                selectedZoom = nearestStop(c)
            }
        } catch (e: Exception) {
            Log.w("Camera", "重算倍率上限失败（相机可能正在重绑）", e)
        }
    }

    // 拖动变焦没有明确的"结束"回调，这里靠超时检测：
    // 260ms 内没有新的手势事件，就认为手离开了屏幕，把挂起的防抖恢复回来
    LaunchedEffect(lastRulerInteraction) {
        if (!stabSuspended) return@LaunchedEffect
        delay(280)
        if (System.currentTimeMillis() - lastRulerInteraction >= 260) {
            // ★先把标志复位，再下发参数★
            //   顺序很关键：万一下面的下发抛异常（相机正在重绑），
            //   标志已经复位了，不会让防抖永远挂在"挂起"状态。
            //   反过来写的话，一次异常 → stabSuspended 永远 true → 防抖再也不回来。
            stabSuspended = false
            try {
                if (mode == CaptureMode.PRO && manualControlSupported) {
                    applyProParams(proIso, proShutterNs, proEv, proFocus, proWb)
                } else {
                    applyStabilization(stabMode != StabMode.OFF)
                }
                Log.d("Camera", "拖动结束，防抖已恢复")
            } catch (e: Exception) {
                Log.w("Camera", "拖动结束恢复防抖时下发失败（已复位标志）", e)
            }
        }
    }

    LaunchedEffect(flashMode, imageCapture, camera) {
        val ic = imageCapture ?: return@LaunchedEffect
        //
        // ★包 try/catch★：同上，未捕获异常会崩整页。
        //   切前后摄 / 切镜头的瞬间 imageCapture 可能已经失效，
        //   此时设 flashMode 或 enableTorch 会抛 IllegalStateException。
        //
        try {
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
        } catch (e: Exception) {
            Log.w("Camera", "设置闪光模式失败（相机可能正在重绑）", e)
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
        // 超过 HAL 上限的部分走扩展数码变焦：记下用户真正想要的目标
        boostTarget = if (targetRatio > halMaxZoomRatio + 0.01f) targetRatio else 0f
        // 只有在【实测坐实】逻辑头到不了广角时才换物理头。
        // ⚠️ 这里曾经无条件调用 requestLensSwitchIfNeeded，等于每次变焦都先尝试直连，
        //    结果把本来 setZoomRatio 能正常工作的机型搞坏了。现在严格限定条件。
        val wantWide = logicWideBlocked && physicalWideWorks && targetRatio < 0.99f
        // 统一用 pickTeleFor（容差与 requestLensSwitchIfNeeded 完全一致，避免两边判定打架）
        val teleForTarget: DeviceCompatibility.LensProfile? = pickTeleFor(targetRatio)
        //
        // ★★ 目标超过 HAL 上限时，不做预防性换头 ★★
        //
        // 这是"切到 15x 卡一下"的直接来源：
        //   点 15x → pickTeleFor(15) 挑中一颗长焦头 → requestLensSwitchIfNeeded
        //   → unbind + bind + 等收敛，几百毫秒黑屏。
        // 但 15x 靠「逻辑头走到 halMax + digitalBoost 预览缩放」本来就能覆盖，
        // 换物理头只是把数码放大的那一部分换了个承担者，画质差别很小，
        // 却要付一次完整重绑的代价 —— 完全不划算。
        //
        // ⚠️ 只挡"预防性换头"(wantTele)，不挡下面的 stuckOnLens：
        //    卡在某颗头上必须换是功能正确性问题，该换还得换。
        // ⚠️ 只在 boundLensId == null 时挡：已经直连在某颗头上时，
        //    halMax 要乘 boundLensFactor，判定基准不一样，不能套用。
        //
        val beyondHalTarget = boundLensId == null && targetRatio > halMaxZoomRatio + 0.01f
        val wantTele = !beyondHalTarget && teleForTarget != null && teleForTarget.id != boundLensId
        if (beyondHalTarget && teleForTarget != null) {
            Log.d(
                "Camera",
                "目标 ${"%.2f".format(targetRatio)}x 超过 HAL 上限 " +
                        "${"%.2f".format(halMaxZoomRatio)}x，跳过物理头直连" +
                        "（避免重绑卡顿，改由预览缩放承担）"
            )
        }
        // ★★卡在当前物理头上时必须换头★★
        // ⚠️ 这是 Find X7 上 10x 失效的根因：
        //    从 0.6x（直连广角头）点 10x 时，wantWide=false（10 >= 0.99）、wantTele=null，
        //    于是根本不会调用 requestLensSwitchIfNeeded ——
        //    停在那颗广角头上，endRatio 被 effMaxZoom()（0.6×4=2.4）夹成 2.4x。
        //    表现就是"点 10x 只到 2.4x / 完全没反应"。
        val stuckOnLens: Boolean = boundLensId != null &&
                (targetRatio > effMaxZoom() + 0.01f || targetRatio < effMinZoom() - 0.01f)
        if (wantWide || wantTele || stuckOnLens) {
            //
            // ★★换头前必须先掐掉正在跑的变焦动画★★
            //
            // 「变焦过程中点另一个档位会卡」的另一半原因：
            //   换头 = unbindAll + bindToLifecycle，期间 HAL 要拆流再建流。
            //   而旧的变焦协程还在按 16ms 高频 setZoomRatio ——
            //   它写的是一个【即将被 unbind 掉】的会话，
            //   HAL 一边拆流一边被塞变焦请求，很多机器上就是一段明显的卡顿，
            //   极端情况还会让新会话起来后倍率被旧动画拽回去（表现为"跳一下又回来"）。
            //
            //   cancel() 是协作式的，旧协程还要几毫秒才真正退出，
            //   所以同时把 zoomAnimating 复位（旧协程的 finally 有世代号保护，
            //   不会再抢着置回来 —— 它已不是最新一代）。
            //
            if (zoomJob?.isActive == true) {
                zoomJob?.cancel()
                zoomAnimating[0] = false
                Log.d("Camera", "换头前掐掉进行中的变焦动画")
            }
            if (requestLensSwitchIfNeeded(targetRatio)) return
        }
        val endRatio = targetRatio.coerceIn(effMinZoom(), effMaxZoom())
        val startRatio = displayedRatio

        // 记录"这次之前是否已有变焦在跑"，用于下面跳过重复的参数包下发
        val zoomJobWasActive = zoomJob?.isActive == true
        zoomJob?.cancel()
        // ★领一个新的世代号★：旧协程看到号变了就知道自己已被取代，会静默退出
        val myGen = ++zoomGeneration
        zoomJob = coroutineScope.launch {
            if (abs(startRatio - endRatio) < 0.005f) {
                cam.cameraControl.setZoomRatio(clampForHal(endRatio))
                displayedRatio = endRatio
                return@launch
            }
            // ★被取代则立即退出★：协程取消是协作式的，cancel() 之后这里可能还跑几句，
            // 用世代号做一次显式判断，避免旧代继续往下改状态。
            if (myGen != zoomGeneration) return@launch

            //
            // ★★ 15x 卡顿专项：判断这段变焦 HAL 到底动不动 ★★
            //
            // 15x 通常超过 HAL 原生上限（多数机器 halMax = 10x），此时：
            //   · clampForHal(r) 会把下发值夹在 halMax 上 → HAL 拿到的始终是同一个数
            //   · 超出部分由 [digitalBoost] 的预览缩放承担
            // 于是「12x → 15x」这种切换，HAL 从头到尾一动不动，
            // 却仍然走完整的「挂起防抖 → 32 步高频下发 → 等收敛 → 恢复防抖」流程 ——
            // 白白付了全部代价，画面还没任何变化。这就是"切到 15x 卡一下"的来源。
            //
            val halCapNow = halMaxZoomRatio * (if (boundLensId != null) boundLensFactor() else 1f)
            val halStatic: Boolean =
                startRatio >= halCapNow - 0.01f && endRatio >= halCapNow - 0.01f

            // ★防抖与变焦会互相打架★
            // EIS 每帧做运动估计、OIS 驱动马达位移，而变焦会不断改变 crop region，
            // 两者同时进行时 HAL 每帧都要重算。所以变焦期间先挂起防抖，结束后再恢复。
            // 挂起之后 EIS 就不再参与，变焦的代价回到普通水平，可以放心用高频下发。
            // ★变焦期间：防抖挂起 + AF 切连续视频对焦★
            // EIS/OIS 与 crop 变化打架会卡；AF 每步重新寻焦则是"从大焦段切小焦段
            // 一卡一卡"的另一半原因。两者一起处理，切换焦段就顺了。
            //
            //
            // ★录制中【完全不碰参数包】★
            // 挂起防抖要下发一次参数包，变焦结束恢复又要下发一次（还带 600ms 延迟）。
            // 哪怕 AWB 已经锁住，这两次「clearCaptureRequestOptions + setCaptureRequestOptions」
            // 也会让 HAL 短暂回到默认状态再重建 —— 3A 有概率被带着重启一次，
            // 视频里就是一帧突变。录制期间宁可让变焦略慢一点也不冒这个险，
            // 变焦结束的恢复分支（下面 finally）同样要跳过。
            //
            // ★HAL 全程不动时整套也跳过★：既然 crop region 一个字节都没变，
            // 就没有"防抖与变焦打架"这回事，也不用等收敛、不用恢复参数。
            val skipParams = halStatic || isRecording
            //
            // ★连续变焦时不重复下发参数包★
            //   上一次变焦已经把参数切成"防抖挂起 + CONTINUOUS_VIDEO 对焦"了，
            //   连点档位时每点一次再 clear+set 一遍，等于让 HAL 反复重配 ——
            //   这就是"连着点几个变焦键会一顿一顿"的来源。
            //   已经在同样的状态下就跳过（由上面掐掉的旧协程或本代自己的 finally 负责恢复）。
            //
            val alreadyInZoomParams = zoomJobWasActive || stabSuspended
            if (skipParams) {
                Log.d(
                    "Camera",
                    "变焦不重发参数包（${if (isRecording) "录制中" else "HAL 全程静止"}）：" +
                            "${"%.2f".format(startRatio)}→${"%.2f".format(endRatio)}"
                )
            } else if (alreadyInZoomParams) {
                Log.d(
                    "Camera",
                    "连续变焦：参数包已是变焦态，跳过重复下发"
                )
            } else {
                applySceneParams(stabOverride = false, afSmooth = true)
            }

            // 通知下面的轮询循环：变焦动画进行中，别来抢着更新 UI 状态
            zoomAnimating[0] = true

            try {
                // ★① 时长只看倍率跨度，且在对数域算★
                // ln(1→10) 与 ln(10→1) 绝对值相同 → 两个方向的时长天然一致。
                val spanLog = abs(ln(endRatio / startRatio.coerceAtLeast(0.01f)))
                val durationMs = (ZOOM_ANIM_BASE_MS - spanLog * ZOOM_ANIM_SPAN_FACTOR_LOG)
                    .coerceIn(ZOOM_ANIM_MIN_MS, ZOOM_ANIM_BASE_MS).toLong()
                val stepMs = if (isRecording) ZOOM_ANIM_STEP_MS_REC else ZOOM_ANIM_STEP_MS
                val steps = (durationMs / stepMs).toInt().coerceAtLeast(1)
                // ★② 插值改到对数域★：r = exp(lnStart + (lnEnd - lnStart) * eased)
                //    单位时间内变化的「倍率比例」恒定，正反向曲线互为镜像。
                val lnStart = ln(startRatio.coerceAtLeast(0.01f))
                val lnEnd = ln(endRatio.coerceAtLeast(0.01f))
                val lnRange = lnEnd - lnStart
                // ★③ 缓动换成前后对称的曲线★（见 SYMMETRIC_ZOOM_EASE 的注释）
                val easing = SYMMETRIC_ZOOM_EASE

                var uiCommits = 0
                var lastUiAt = 0L

                Log.d(
                    "Camera",
                    "变焦 ${"%.2f".format(startRatio)}→${"%.2f".format(endRatio)}" +
                            " 下发${steps}步/${stepMs}ms UI刷新${ZOOM_UI_REFRESH_MS}ms"
                )

                // ★HAL 下发去重★
                // 15x 段的后 1/3（r 从 halMax 爬到 15）里，clampForHal(r) 恒等于 halMax，
                // 每 16ms 给 CameraX 下发一个完全相同的倍率，
                // 会白白触发一轮 zoomState 更新与内部状态机 —— 这是"15x 卡"的一大块。
                // 记下上次真正下发的值，相同就跳过。
                var lastHalSent = -1f
                var skipped = 0

                for (step in 1..steps) {
                    val t = step.toFloat() / steps
                    val eased = easing.transform(t)
                    val r = exp(lnStart + lnRange * eased)

                    // ① 每帧都给相机下发——这个是顺滑感的来源，很便宜
                    //    物理头直连时要把全局倍率换算成这颗头的本地倍率
                    val halValue = clampForHal(r)
                    if (abs(halValue - lastHalSent) > 0.0015f) {
                        cam.cameraControl.setZoomRatio(halValue)
                        lastHalSent = halValue
                    } else {
                        skipped++
                    }

                    // ② UI 状态只在攒够间隔时才提交——重组很贵，必须节流
                    val now = System.currentTimeMillis()
                    val uiThrottle =
                        if (isRecording) ZOOM_UI_REFRESH_MS_REC else ZOOM_UI_REFRESH_MS
                    if (step == steps || now - lastUiAt >= uiThrottle) {
                        displayedRatio = r
                        lastUiAt = now
                        uiCommits++
                    }
                    delay(stepMs)
                }
                cam.cameraControl.setZoomRatio(clampForHal(endRatio))
                displayedRatio = endRatio
                Log.d(
                    "Camera",
                    "变焦完成，UI 实际重组 $uiCommits 次（共 $steps 步，HAL 下发去重省掉 $skipped 次）"
                )

                // ★下发后校验★ 不依赖任何预判，直接看相机实际到没到位。
                // HAL 把倍率 clamp 回 1.0 时不会报错、不抛异常，只有读回来才知道。
                // 这一步是所有广角兜底里最可靠的：只要没到位，就改用物理直连再试。
                // ★目标超过 HAL 上限时不做校验★
                // 15x 走的是扩展数码变焦：HAL 本来就只能到 halMax，
                // 拿 15 去跟 HAL 回读值比，结论必然是"没到位"，
                // 下面的 off 判定里虽然用 endRatio <= halMax 挡住了误判，
                // 但前面那一次 delay(zoomSettleDelayMs)（180~260ms）是白等的，
                // 而且一旦判定 off 还会触发物理头重绑 —— 那才是真正的卡顿。
                // 所以这里直接整段跳过。
                val beyondHal = endRatio > halMaxZoomRatio + 0.01f
                if (beyondHal) {
                    Log.d(
                        "Camera",
                        "目标 ${"%.2f".format(endRatio)}x 超过 HAL 上限 ${"%.2f".format(halMaxZoomRatio)}x，" +
                                "走扩展数码变焦，跳过收敛校验"
                    )
                } else if (boundLensId == null && physicalSwitchWorks && !isFront) {
                    delay(DeviceCompatibility.zoomSettleDelayMs)
                    val actual: Float = cam.cameraInfo.zoomState.value?.zoomRatio ?: endRatio
                    // 华为 P 系列按键与实际倍率对不上，这里把容差收紧一点；
                    // 扩展数码变焦（目标超过 HAL 上限）是预期行为，不算"没到位"
                    val tol: Float = if (DeviceCompatibility.isHuaweiPSeries) 0.08f else 0.12f
                    val off: Boolean = abs(actual - endRatio) > endRatio * tol &&
                            endRatio <= halMaxZoomRatio + 0.01f
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
                //
                // ★★只有"最新一代"才负责收尾★★
                //
                // 若自己已被新的变焦取代（myGen != zoomGeneration），
                // 这里一行都不做 —— 由新代去置标志、去恢复参数。
                // 否则就会出现上面 zoomGeneration 注释里说的：
                // 旧代把新代刚置上的 zoomAnimating 清掉，两股力量打架 → 卡顿。
                //
                val superseded = myGen != zoomGeneration
                if (!superseded) {
                    // 无论中途是否异常，都要撤掉"动画中"标志，
                    // 否则轮询循环会永远不再更新倍率显示
                    zoomAnimating[0] = false

                    // ★录制中 / HAL 全程静止 都不用恢复★：压根没挂起过防抖 / 没改过 AF，
                    // 再调一次 applySceneParams 反而会让 HAL 重新收敛，画面顿一下；
                    // 录制中还会让视频多一次突变风险。
                    if (skipParams) {
                        Log.d(
                            "Camera",
                            "变焦结束无需恢复（${if (isRecording) "录制中" else "HAL 静止段"}）"
                        )
                    } else {
                        // 恢复防抖与对焦模式。
                        // 录制中多等一会儿：EIS 刚恢复就要重新收敛稳定窗口，
                        // 紧接着的画面会顿一下 —— 那正是"录制时切焦段一卡一卡"的最后一环。
                        delay(if (isRecording) STAB_RESTORE_DELAY_MS_REC else STAB_RESTORE_DELAY_MS)
                        // ★恢复前再确认一次★：等待期间可能又点了新的档位，
                        //   这时的恢复会被新代自己的 applySceneParams 覆盖，属于白下发一次。
                        if (myGen == zoomGeneration) {
                            if (mode == CaptureMode.PRO && manualControlSupported) {
                                applyProParams(proIso, proShutterNs, proEv, proFocus, proWb)
                            } else {
                                applySceneParams()
                            }
                        }
                    }
                } else {
                    Log.d(
                        "Camera",
                        "变焦 ${"%.2f".format(startRatio)}→${"%.2f".format(endRatio)}" +
                                " 已被新的变焦取代，本代静默退出（不抢标志、不下发参数包）"
                    )
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
        boostTarget = if (clamped > halMaxZoomRatio + 0.01f) clamped else 0f
        displayedRatio = clamped
        cam.cameraControl.setZoomRatio(clampForHal(clamped))
        selectedZoom = nearestStop(clamped)
    }

    fun onZoomButtonClick(zoom: Float) {
        // 2x 再点一下切 3.5x：这是原厂 App 常见的"人像/长焦中间档"手感，
        // 但如果本机根本没有这个焦段的物理头，就直接吸附回最近的光学档
        // ★中间档也用整数★：原来这里写死 3.5x，跟"一律整数焦段"的要求冲突
        val target: Float = if (zoom == 2.0f && abs(selectedZoom - 2.0f) < 0.05f) {
            snapToOptical(3.0f)
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
        // 长焦档即使"没发生吸附"也要走一次 smoothZoomTo，否则换头逻辑永远不触发
        val teleForSnap: DeviceCompatibility.LensProfile? = pickTeleFor(snapped)
        val needSwitch = teleForSnap != null && teleForSnap.id != boundLensId
        if (abs(snapped - ratio) > 0.001f || needSwitch) {
            smoothZoomTo(snapped)
        } else {
            val clamped = snapped.coerceIn(effMinZoom(), effMaxZoom())
            boostTarget = if (clamped > halMaxZoomRatio + 0.01f) clamped else 0f
            cam.cameraControl.setZoomRatio(clampForHal(clamped))
            displayedRatio = clamped
        }
        selectedZoom = nearestStop(snapped)
    }

    /**
     * 成片后处理：① 扩展数码变焦的中心裁切 ② 苹果风格调色。
     *
     * 只在真的需要时才跑（boost > 1 或调色非中性），正常拍照完全不额外耗时。
     * 失败也无所谓 —— 原图已经存好了，只是没做裁切 / 调色。
     */
    //
    // ⚠️ 这两个函数【不能加 private】：它们是 CameraApp() 内部的【局部函数】，
    //    Kotlin 不允许局部函数带可见性修饰符（会报
    //    "Modifier 'private' is not applicable to 'local function'"）。
    //    类成员函数才可以加 private。
    //
    /**
     * ★单张 Bitmap 的内存预算（字节）★
     *
     * 取"堆上限的 22%"，留出余量给 ①解码临时缓冲 ②目标 bitmap ③GC 抖动。
     * 后处理里最多同时存在 2 张同尺寸图（源 + 目标），2×22%=44%，
     * 剩下的堆留给相机预览和其他对象。
     */
    fun photoMemoryBudgetBytes(): Long {
        val maxHeap = Runtime.getRuntime().maxMemory()
        return (maxHeap * 0.22f).toLong().coerceAtLeast(8L * 1024 * 1024)
    }

    /** 把 Bitmap 以 JPEG 95 写回原 Uri。写失败不抛，只打日志 */
    fun writeBitmap(uri: Uri, bmp: android.graphics.Bitmap) {
        try {
            context.contentResolver.openOutputStream(uri, "w")?.use { os ->
                bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, os)
            }
        } catch (e: Exception) {
            Log.w("Camera", "写回成片失败", e)
        }
    }

    fun postProcessPhoto(
        uri: Uri,
        boost: Float,
        warmth: Float,
        tint: Float,
        intensity: Float,
        lensKey: String = ""
    ) {
        try {
            //
            // 内存安全：不再无条件全尺寸解码。
            // 原实现 5000万像素（8160×6144）ARGB_8888 = 191MB，链路上同时存在 4 张
            // 同尺寸图（src/放大图/调色 dst/色彩统一）→ 峰值 500MB+，直接 OOM 崩溃。
            // 更糟的是 LensColorUnifier.enabled 默认 true，只要它开着就每次拍照都跑
            // 后处理 —— 默认配置下每拍一张都可能崩。
            // 三步改进：① inJustDecodeBounds 先读尺寸算安全 inSampleSize
            // ② 裁切改用 BitmapRegionDecoder，只解中心矩形（15x 时只要 1/15 面积）
            // ③ 调色与色彩统一合并成一张 ColorMatrix（都是对角增益，可逐通道相乘），
            //    少一整张全尺寸中间 bitmap。
            //
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val ow = bounds.outWidth
            val oh = bounds.outHeight
            if (ow <= 0 || oh <= 0) {
                Log.w("Camera", "读不到原图尺寸，跳过后处理")
                return
            }

            // ① 按内存预算算降采样倍率（取 2 的幂，JPEG 解码器要求）
            val budgetPx = photoMemoryBudgetBytes() / 4L
            var sample = 1
            while ((ow / sample).toLong() * (oh / sample) > budgetPx) sample *= 2
            val outW = (ow / sample).coerceAtLeast(1)
            val outH = (oh / sample).coerceAtLeast(1)
            if (sample > 1) {
                Log.w(
                    "Camera",
                    "原图 ${ow}x${oh}（${ow * oh / 1_000_000}MP）超出内存预算，" +
                            "后处理按 1/$sample 降采样处理（避免 OOM）"
                )
            }

            //
            // ② 裁切：优先用 BitmapRegionDecoder 只解码中心区域
            //
            // 原做法是先解码全图再 createBitmap(src, left, top, cw, ch)，
            // 全图那 191MB 已经付过了。RegionDecoder 直接在文件上定位到目标矩形，
            // 只把这一块解进内存 —— 15x 时只有 1/15 的面积。
            //
            val needCrop = boost > 1.001f
            var base: android.graphics.Bitmap? = null
            if (needCrop) {
                val cw = (ow / boost).roundToInt().coerceIn(2, ow)
                val ch = (oh / boost).roundToInt().coerceIn(2, oh)
                val left = ((ow - cw) / 2f).roundToInt().coerceIn(0, ow - cw)
                val top = ((oh - ch) / 2f).roundToInt().coerceIn(0, oh - ch)
                //
                // ⚠️ 这里不用 `?.use { }`：在 lambda 里给外层 var 赋值虽然合法，
                //    但 combined with 早期 return / 类型推导容易出问题，
                //    而且 use 结束时 stream 已关，region 也该回收 —— 顺序不好控制。
                //    改成显式的 try/finally，行为和可读性都更清楚。
                //
                var ins: java.io.InputStream? = null
                try {
                    ins = context.contentResolver.openInputStream(uri)
                    if (ins != null) {
                        //
                        // ⚠️ newInstance() 返回的是【可空】BitmapRegionDecoder?
                        //    （无法解码时会返回 null，而不是抛异常），
                        //    所以不能直接 region.decodeRegion(...) ——
                        //    会报 "Only safe (?.) or non-null asserted (!!.) calls
                        //    are allowed on a nullable receiver"。这里显式判空。
                        //
                        val region = android.graphics.BitmapRegionDecoder.newInstance(ins, false)
                        if (region != null) {
                            val opt = BitmapFactory.Options()
                            opt.inSampleSize = sample
                            opt.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                            val rect = android.graphics.Rect(left, top, left + cw, top + ch)
                            base = region.decodeRegion(rect, opt)
                            region.recycle()
                            // ★用 Long 算百分比★：5000万像素时 100*cw*ch 会超出
                            //   Int 上限（2.1e9）溢出成负数，日志会打出"占原图 -xx%"
                            val pct = (100L * cw.toLong() * ch.toLong() /
                                    (ow.toLong() * oh.toLong())).toInt()
                            Log.d(
                                "Camera",
                                "扩展数码变焦 ${"%.1f".format(boost)}x：RegionDecoder 只解码 " +
                                        "${cw}x${ch}（占原图 ${pct}%）"
                            )
                        } else {
                            Log.w("Camera", "RegionDecoder 创建失败（返回 null），回退到整图解码后裁切")
                        }
                    }
                } catch (e: Exception) {
                    Log.w("Camera", "RegionDecoder 不可用，回退到整图解码后裁切", e)
                    base = null
                } finally {
                    try { ins?.close() } catch (_: Exception) {}
                }
            }

            if (base == null) {
                val opt = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                }
                val full = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opt)
                }
                if (full == null) {
                    Log.w("Camera", "解码原图失败，跳过后处理")
                    return
                }
                base = if (needCrop) {
                    val cw = (full.width / boost).roundToInt().coerceIn(2, full.width)
                    val ch = (full.height / boost).roundToInt().coerceIn(2, full.height)
                    val left = ((full.width - cw) / 2f).roundToInt().coerceIn(0, full.width - cw)
                    val top = ((full.height - ch) / 2f).roundToInt().coerceIn(0, full.height - ch)
                    val cropped = android.graphics.Bitmap.createBitmap(full, left, top, cw, ch)
                    if (cropped !== full) full.recycle()
                    Log.d("Camera", "扩展数码变焦 ${"%.1f".format(boost)}x 已裁切到 ${cw}x${ch}")
                    cropped
                } else full
            }

            val src = base ?: return
            // 裁切后要放大回"降采样后的原图尺寸"；未裁切时保持解码尺寸
            val targetW = if (needCrop) outW else src.width
            val targetH = if (needCrop) outH else src.height

            //
            // ③ 调色 + 色彩统一【合并成一个 ColorMatrix】
            //
            // 两者都是纯对角增益（各自只缩放 R/G/B 三个通道），
            // 逐通道相乘即可合并，一次 drawBitmap 完成 —— 少一张全尺寸中间图。
            //
            val toneOn = intensity > 0f && (abs(warmth) > 0.5f || abs(tint) > 0.5f)
            val uniOn = LensColorUnifier.enabled && lensKey.isNotEmpty()

            //
            // ★★成片增益改用统一的 [toneGains]★★
            //   原来这里写的是 1±0.18w / 1∓0.12t 另一套系数，
            //   跟预览、盘面都不一样 —— 这正是"5 个影调色彩不对"的根源。
            //   现在三处共用同一个函数，预设值可以直接按成片效果验算。
            //
            val u = if (uniOn) LensColorUnifier.gainFor(lensKey) else floatArrayOf(1f, 1f, 1f)
            val tg = if (toneOn) toneGains(warmth, tint, intensity) else floatArrayOf(1f, 1f, 1f)
            val matrixValues = floatArrayOf(
                (tg[0] * u[0]).coerceIn(0f, 4f), 0f, 0f, 0f, 0f,
                0f, (tg[1] * u[1]).coerceIn(0f, 4f), 0f, 0f, 0f,
                0f, 0f, (tg[2] * u[2]).coerceIn(0f, 4f), 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            val rG = matrixValues[0]
            val gG = matrixValues[6]
            val bG = matrixValues[12]
            val noop = (!toneOn && !uniOn) ||
                    (abs(rG - 1f) < 0.002f && abs(gG - 1f) < 0.002f && abs(bG - 1f) < 0.002f)

            val scaledIn = if (src.width != targetW || src.height != targetH) {
                android.graphics.Bitmap.createScaledBitmap(src, targetW, targetH, true)
            } else src

            if (noop) {
                // 增益等于中性（或根本没开）：不必再建一张目标图
                writeBitmap(uri, scaledIn)
                if (scaledIn !== src) scaledIn.recycle()
                src.recycle()
                return
            }

            val dst = android.graphics.Bitmap.createBitmap(
                targetW, targetH, android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(dst)
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                colorFilter = android.graphics.ColorMatrixColorFilter(
                    android.graphics.ColorMatrix(matrixValues)
                )
            }
            canvas.drawBitmap(scaledIn, 0f, 0f, paint)

            writeBitmap(uri, dst)
            Log.d(
                "Camera",
                "成片后处理完成 ${targetW}x${targetH}" +
                        "（裁切=${if (needCrop) "${"%.1f".format(boost)}x" else "无"}，" +
                        "调色=${if (toneOn) "有" else "无"}，" +
                        "色彩统一=${if (uniOn) "有" else "无"}）"
            )
            dst.recycle()
            if (scaledIn !== src) scaledIn.recycle()
            src.recycle()
        } catch (e: OutOfMemoryError) {
            // ★OOM 单独 catch★：原图已经完整存好了，只是没做裁切/调色，
            // 不能让它冒泡成崩溃
            Log.e("Camera", "成片后处理内存不足（原图已完整保存，未丢失）", e)
        } catch (e: Exception) {
            Log.w("Camera", "成片后处理失败（原图已保存）", e)
        }
    }


    /**
     * 开始一次星轨拍摄：连拍 N 帧 → 机内堆栈 → 导出合成图 / 动态照片 / 视频。
     *
     * 全程在 IO 线程跑，只把进度回抛到主线程，界面不会卡。
     */
    fun startStarTrail() {
        val ic = imageCapture ?: return
        val cam = camera ?: return
        if (starTrailRunning) return
        starTrailStop[0] = false
        starTrailResult = null
        starTrailPreview = null
        starTrailDone = 0
        starTrailTotal = starTrailConfig.frames
        starTrailRunning = true
        StarTrailStore.save(context, starTrailConfig)
        starTrailJobRef = coroutineScope.launch(Dispatchers.Default) {
            val session = StarTrailSession(context, ic, cam, starTrailConfig)
            //
            // ★★用 try/finally 兜住 starTrailRunning 的复位★★
            //
            // session.run 现在会把 CancellationException 原样抛出（这是正确的协程语义），
            // 但这样一来：用户中途退出、或父作用域被取消时，
            // 下面那句 `starTrailRunning = false` 【永远不会执行】——
            // 按钮就一直停在「拍摄中」，进度浮层也一直挂着，而协程其实早没了。
            // 这正是"点了没反应"类卡死的标准形态。
            // 所以把复位挪进 finally：不管正常结束、异常还是被取消，都一定执行。
            //
            var res: StarTrailResult? = null
            try {
                res = session.run(shouldStop = { starTrailStop[0] }) { done, total, preview ->
                    withContext(Dispatchers.Main) {
                        val old = starTrailPreview
                        starTrailPreview = preview
                        starTrailDone = done
                        starTrailTotal = total
                        // 新图已经顶上去了，旧的就可以回收了（否则连拍几百张会 OOM）
                        if (old != null && !old.isRecycled) old.recycle()
                    }
                }
            } finally {
                val r = res
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) {
                    starTrailRunning = false
                    starTrailJobRef = null
                    if (r != null) {
                        starTrailResult = r
                        r.jpegUri?.let { sessionMedia.add(0, CapturedMedia(it, false)) }
                        r.videoUri?.let { sessionMedia.add(0, CapturedMedia(it, true)) }
                    }
                    // 手动曝光参数收回来，预览恢复正常
                    clearStarTrailParams(camera)
                    applySceneParams()
                }
            }
        }
    }

    /** 提前停止：已经拍到的帧照样会合成出图，不会白拍 */
    fun stopStarTrail() {
        if (!starTrailRunning) return
        starTrailStop[0] = true
    }

    /**
     * 镜头色彩校准：对准白纸 / 灰卡拍一帧，记下这颗头的中性偏差。
     *
     * ★卡死的三个原因，都已修掉★
     *   ① 原来直接拿【原图】去 getPixels —— 高像素模式下原图可能 5000 万像素，
     *      中心区域就要几百 MB 的 IntArray，轻则 ANR 重则 OOM。
     *      → 先压到 640 再采样，偏色只需要平均色，用不上那么高分辨率。
     *   ② 原来超时是 60 秒且【没有任何界面反馈】，用户点下去以为死机。
     *      → 超时改 10 秒，并用 calibrating 让弹窗显示「校准中…」。
     *   ③ 在星空模式下校准，手动曝光可能几十秒一帧，拍一张要等半天。
     *      → 校准前先把手动参数收回来，拍完再恢复。
     */
    fun calibrateLensColor() {
        val ic = imageCapture
        if (ic == null) {
            android.widget.Toast.makeText(context, "相机还没准备好，稍后再试", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (calibrating) return          // 连点防护
        val key = lensColorKey()
        calibrating = true
        val wasStarTrail = mode == CaptureMode.STAR_TRAIL
        coroutineScope.launch(Dispatchers.IO) {
            var msg = ""
            try {
                if (wasStarTrail) {
                    withContext(Dispatchers.Main) { clearStarTrailParams(camera) }
                    delay(400)
                }
                val full = kotlinx.coroutines.withTimeoutOrNull(10_000L) {
                    captureFrameBitmap(context, ic)
                }
                if (full == null) {
                    msg = "校准失败：10 秒内没拿到画面"
                } else {
                    // ★必须先降采样★：见上面 ① 的说明
                    val small = StarTrailImaging.fit(full, 640)
                    if (small !== full) full.recycle()
                    val raw = LensColorUnifier.measureCenter(small)
                    small.recycle()
                    val mean = (raw[0] + raw[1] + raw[2]) / 3f
                    // 太黑 / 过曝的样本不可信，记下去就是一组离谱的增益
                    if (mean < 24f || mean > 236f) {
                        msg = "校准失败：画面太暗或过曝（均值 ${mean.roundToInt()}），对准白纸再来"
                    } else {
                        LensColorUnifier.setCalibration(context, key, raw)
                        msg = "已校准 $key　${LensColorUnifier.describe(key)}\n" +
                                "对每个镜头各做一次，色彩才会完全统一"
                    }
                }
            } catch (e: Exception) {
                Log.w("LensColor", "校准异常", e)
                msg = "校准失败：${e.message ?: "未知错误"}"
            } finally {
                // ★无论成功失败都要收尾★：以前失败分支直接 return，界面永远停在「校准中」
                withContext(Dispatchers.Main) {
                    calibrating = false
                    if (wasStarTrail) applySceneParams()
                    if (msg.isNotEmpty()) {
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    //
    // ★拍照结束后把参数切回"预览流畅优先"★
    //
    // ⚠️ 必须声明在 takePhoto 【之前】：Kotlin 的【局部函数】只能引用前面已声明的，
    //    放在 takePhoto 后面就会报 Unresolved reference（本次就是这个原因）。
    //    这不是 Compose 的限制，是 Kotlin 作用域规则 —— 成员函数不受此限，局部函数受。
    //
    // 做成函数是因为有【两个】恢复点（900ms 兜底 / onError 回调），
    // 幂等：重复调用不会出问题。
    //
    //
    // ★RAW 残缺的真凶：写出期间被参数下发打断★
    //   DNG 25~50MB 要写 1~3 秒，JPEG 只要 300~700ms。
    //   JPEG 回调里立刻 restorePreviewParamsAfterCapture() 会重建
    //   repeating request，把还在写的 RAW 流水线掐断 → 半张图。
    //   所以 RAW 写出期间一律不碰相机参数。
    //
    /** DNG 正在写出（此期间禁止任何 setCaptureRequestOptions） */
    var rawBusy by remember { mutableStateOf(false) }

    // ⚠️ 局部函数必须先声明后使用：下面 restoreAfterCaptureIfIdle 要调它
    fun restorePreviewParamsAfterCapture() {
        if (!captureQualityBoost) return
        captureQualityBoost = false
        applySceneParams()
        Log.d("Camera", "拍照完成，参数已切回预览流畅优先")
    }

    /** 拍照后恢复预览参数；RAW 没写完就不动，等 DNG 路径自己来恢复 */
    fun restoreAfterCaptureIfIdle() {
        if (rawBusy) {
            Log.d("Camera", "DNG 仍在写出，暂不切换参数（避免打断 RAW 流水线）")
            return
        }
        restorePreviewParamsAfterCapture()
    }

    /**
     * ★★统一的模式切换★★
     *
     * 模式栏点击 和 【左右滑动屏幕】两个入口都走这里，行为完全一致。
     * 以前这段逻辑在横屏 / 竖屏两个 ModeSelector 里【各写了一遍】，
     * 加新入口（滑动）就必须再抄第三遍，以后改一处漏两处。
     */
    fun switchMode(next: CaptureMode) {
        // ★录制中 / 星空拍摄中禁止切模式★
        // 切模式会重绑相机（VideoCapture 用例增删），录制中的视频会被直接打断。
        if (isRecording || starTrailRunning) {
            android.widget.Toast.makeText(
                context,
                if (isRecording) "录制中无法切换模式" else "星空拍摄中无法切换模式",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (next == mode) return
        mode = next
        // ★震动反馈★：切模式这种"状态变了"的操作要给一下，确认已生效
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        // 高像素模式下没有录像用例（三流共存会把拍照分辨率压到 1200万），
        // 切进录像时要先退出高像素，否则点录制会没反应
        if (next == CaptureMode.VIDEO && highResMode) highResMode = false

        // ★进录像前回到逻辑摄像头★
        // 录制中换头要 unbindAll，会直接把视频打断，所以录制期间一律不换头；
        // 而单颗物理头的本地变焦范围很窄（广角头只有 0.6~2.4x），
        // 留在上面从大焦段拖回小焦段会拖不动、看起来一卡一卡。
        if (next == CaptureMode.VIDEO && boundLensId != null) {
            pendingZoomTarget = displayedRatio
            lensSwitching = true
            boundLensId = null
        }
        // ★进出星空模式★：进星空要下发长曝光预览参数；离开星空要把手动参数收回来
        if (next == CaptureMode.STAR_TRAIL) {
            camera?.let { applyStarTrailPreviewParams(it, starTrailConfig) }
        } else {
            if (starTrailRunning) stopStarTrail()
            camera?.let { clearStarTrailParams(it); applySceneParams() }
        }
        Log.d("Camera", "模式已切换到 ${next.label}")
    }

    /**
     * ★左右滑动要切到的那个模式★
     * @param dir +1 = 左滑（去下一个），-1 = 右滑（回上一个），按 CaptureMode 声明顺序循环
     */
    fun adjacentMode(dir: Int): CaptureMode {
        val arr = CaptureMode.values()
        val i = arr.indexOf(mode)
        return arr[(i + dir + arr.size) % arr.size]
    }

    //
    // ★★小米数字系列 RAW 照片"残缺"的根因与修法★★
    //
    // DNG 一张 25~50MB（JPEG 才 3~8MB），写卡要 1~3 秒。
    // 原实现把 DNG 【直接写进 MediaStore】：
    //   ContentValues 一 insert，行就对外可见，MediaScanner 立刻来扫、
    //   相册/图库/第三方软件马上就去读 —— 而此时文件【才写了一部分】。
    //   JPEG 小所以偶尔躲过去，DNG 大所以【几乎必中】，表现就是
    //   缺角、花屏、半张图、打不开、缩略图灰色。
    //
    // 这就是"小米数字系列拍 RAW 照片残缺"的主因（小米图库扫描最积极）。
    //
    // 修法三步：
    //   ① 先写【App 私有文件】—— 这一步完全不进 MediaStore，外界看不见
    //   ② 等落盘结束 + 验完整性（TIFF 头 + 体积下限）
    //   ③ 才插进 MediaStore，且插时就置 IS_PENDING=1，写完再置 0
    //

    //
    // ★判定"残缺"靠 IFD 声明的像素长度，不靠文件头★
    //   半截 DNG 的头是完全合法的，只有 StripOffsets(0x0111)
    //   与 StripByteCounts(0x0117) 能说明文件"应该"多长。
    //   ⚠️ Kotlin 没有 Quadruple，四返回值自己定义 data class。
    //
    data class IfdScan(
        val width: Long,
        val height: Long,
        val dataEnd: Long,
        val subIfds: List<Long>
    )

    /** DNG 体检报告 */
    data class DngReport(
        val fileSize: Long,
        val width: Long,
        val height: Long,
        /** IFD 声明的像素数据末尾位置（文件至少要有这么长才完整） */
        val expectedEnd: Long,
        val truncated: Boolean,
        val reason: String?
    )

    /** 读一个 IFD 条目里的数值（自动处理 SHORT/LONG 与内联/偏移） */
    fun readIfdValue(
        raf: java.io.RandomAccessFile,
        le: Boolean,
        type: Int,
        count: Long,
        valueField: Long
    ): List<Long> {
        val unit = when (type) {
            1, 2, 7 -> 1L          // BYTE / ASCII / UNDEFINED
            3 -> 2L                // SHORT
            4, 9, 11, 13 -> 4L     // LONG / SLONG / FLOAT / IFD
            5, 10, 12 -> 8L        // RATIONAL / SRATIONAL / DOUBLE
            else -> 0L
        }
        if (unit == 0L || count <= 0 || count > 65536) return emptyList()
        val total = unit * count
        return try {
            if (total <= 4) {
                // 内联：值就在 valueField 的 4 字节里
                val buf = java.nio.ByteBuffer.allocate(4)
                    .order(if (le) java.nio.ByteOrder.LITTLE_ENDIAN else java.nio.ByteOrder.BIG_ENDIAN)
                buf.putInt(valueField.toInt())
                buf.flip()
                listOf(if (type == 3) buf.getShort(0).toLong() and 0xFFFF else buf.getInt(0).toLong() and 0xFFFFFFFF)
            } else {
                val off = valueField and 0xFFFFFFFFL
                if (off <= 0 || off >= raf.length()) return emptyList()
                val need = (total.toInt()).coerceAtMost(8 * 1024 * 1024)
                raf.seek(off)
                val raw = ByteArray(need)
                val n = raf.read(raw)
                if (n <= 0) return emptyList()
                val bb = java.nio.ByteBuffer.wrap(raw, 0, n)
                    .order(if (le) java.nio.ByteOrder.LITTLE_ENDIAN else java.nio.ByteOrder.BIG_ENDIAN)
                val out = ArrayList<Long>(count.toInt().coerceAtMost(4096))
                for (k in 0 until count.toInt().coerceAtMost(4096)) {
                    if (bb.remaining() < unit.toInt()) break
                    out.add(
                        when (type) {
                            3 -> bb.short.toLong() and 0xFFFF
                            4, 9, 11, 13 -> bb.int.toLong() and 0xFFFFFFFF
                            else -> { bb.position(bb.position() + unit.toInt()); 0L }
                        }
                    )
                }
                out
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 扫一个 IFD，返回 (宽, 高, 像素数据末尾, SubIFD 偏移列表) */
    fun scanIfd(
        raf: java.io.RandomAccessFile,
        le: Boolean,
        ifdOff: Long
    ): IfdScan {
        if (ifdOff <= 0 || ifdOff + 2 > raf.length())
            return IfdScan(0L, 0L, 0L, emptyList())
        raf.seek(ifdOff)
        val cntBuf = ByteArray(2)
        if (raf.read(cntBuf) != 2) return IfdScan(0L, 0L, 0L, emptyList())
        val n = java.nio.ByteBuffer.wrap(cntBuf)
            .order(if (le) java.nio.ByteOrder.LITTLE_ENDIAN else java.nio.ByteOrder.BIG_ENDIAN)
            .short.toInt() and 0xFFFF
        if (n <= 0 || n > 512) return IfdScan(0L, 0L, 0L, emptyList())
        val entries = ByteArray(n * 12)
        if (raf.read(entries) != n * 12) return IfdScan(0L, 0L, 0L, emptyList())
        val bb = java.nio.ByteBuffer.wrap(entries)
            .order(if (le) java.nio.ByteOrder.LITTLE_ENDIAN else java.nio.ByteOrder.BIG_ENDIAN)
        var w = 0L
        var h = 0L
        var end = 0L
        val subs = ArrayList<Long>()
        for (i in 0 until n) {
            val tag = bb.short.toInt() and 0xFFFF
            val type = bb.short.toInt() and 0xFFFF
            val count = bb.int.toLong() and 0xFFFFFFFF
            val vf = bb.int.toLong() and 0xFFFFFFFF
            when (tag) {
                0x0100 -> w = readIfdValue(raf, le, type, count, vf).firstOrNull() ?: 0L
                0x0101 -> h = readIfdValue(raf, le, type, count, vf).firstOrNull() ?: 0L
                0x0111 -> {
                    val offs = readIfdValue(raf, le, type, count, vf)
                    // 记下第一条偏移，end 在下面按 counts 再算
                    if (offs.isNotEmpty()) end = maxOf(end, offs.maxOrNull() ?: 0L)
                }
                0x0117 -> {
                    val cnts = readIfdValue(raf, le, type, count, vf)
                    val sum = cnts.sum()
                    // 像素数据总长 ≈ 各 strip 之和
                    if (sum > 0) end = maxOf(end, sum)
                }
                0x014A -> subs.addAll(readIfdValue(raf, le, type, count, vf))
            }
        }
        return IfdScan(w, h, end, subs)
    }

    /** DNG 体检：解析 TIFF → IFD0 + SubIFDs，比对声明长度与实际长度 */
    fun inspectDng(f: java.io.File): DngReport {
        if (!f.exists()) return DngReport(0, 0, 0, 0, true, "文件不存在")
        val size = f.length()
        if (size < 8) return DngReport(size, 0, 0, 0, true, "文件不足 8 字节")
        return try {
            java.io.RandomAccessFile(f, "r").use { raf ->
                val head = ByteArray(4)
                raf.seek(0)
                raf.readFully(head)
                val le = head[0] == 0x49.toByte() && head[1] == 0x49.toByte() &&
                        head[2] == 0x2A.toByte() && head[3] == 0x00.toByte()
                val be = head[0] == 0x4D.toByte() && head[1] == 0x4D.toByte() &&
                        head[2] == 0x00.toByte() && head[3] == 0x2A.toByte()
                if (!le && !be) {
                    val hex = head.joinToString(",") { (it.toInt() and 0xFF).toString(16) }
                    return DngReport(size, 0, 0, 0, true, "不是 TIFF/DNG（头=$hex）")
                }
                val order = if (le) java.nio.ByteOrder.LITTLE_ENDIAN
                else java.nio.ByteOrder.BIG_ENDIAN
                raf.seek(4)
                val offBuf = ByteArray(4)
                raf.readFully(offBuf)
                val ifd0 = java.nio.ByteBuffer.wrap(offBuf).order(order).int.toLong() and 0xFFFFFFFF

                val (w0, h0, end0, subs) = scanIfd(raf, le, ifd0)
                var w = w0
                var h = h0
                var end = end0
                // DNG 常把真正的 RAW 数据放在 SubIFD 里，必须一起查
                for (so in subs.take(4)) {
                    val (sw, sh, se, _) = scanIfd(raf, le, so)
                    if (sw > w) w = sw
                    if (sh > h) h = sh
                    if (se > end) end = se
                }
                // end 只是"像素数据起始偏移 + 长度"的粗略上界，
                // 若 IFD 没给出有效信息就用文件大小当基线
                val expected = if (end > 0) end else 0L
                val truncated = expected > 0 && size < expected
                DngReport(
                    fileSize = size, width = w, height = h, expectedEnd = expected,
                    truncated = truncated,
                    reason = when {
                        truncated -> "被截断：IFD 声明需要 ${expected / 1024 / 1024}MB，实际只有 ${size / 1024 / 1024}MB"
                        w <= 0 || h <= 0 -> "读不到图像尺寸，IFD 可能损坏"
                        size < 1_000_000L -> "体积过小（${size / 1024}KB）"
                        else -> null
                    }
                )
            }
        } catch (e: Exception) {
            DngReport(size, 0, 0, 0, true, "解析异常：${e.message}")
        }
    }

    /** 等落盘结束：体积连续 3 次采样不变。返回最终体积，-1 = 文件不存在 */
    fun waitFileStable(f: java.io.File, timeoutMs: Long = 8000): Long {
        var last = -1L
        var stable = 0
        val t0 = System.currentTimeMillis()
        while (System.currentTimeMillis() - t0 < timeoutMs) {
            val cur = if (f.exists()) f.length() else -1L
            if (cur > 0 && cur == last) {
                stable++
                if (stable >= 3) return cur
            } else {
                stable = 0
            }
            last = cur
            try {
                Thread.sleep(120)
            } catch (e: Exception) {
                break
            }
        }
        return if (f.exists()) f.length() else -1L
    }

    /** 发布 DNG 到 DCIM。★IS_PENDING 置 1 期间外界读不到，写完才置 0★ */
    fun publishRawToDcim(f: java.io.File, stamp: Long): android.net.Uri? {
        val cr = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_$stamp.dng")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/x-adobe-dng")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Silky Camera")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        return try {
            val uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw Exception("MediaStore 插入失败")
            cr.openOutputStream(uri)?.use { out ->
                java.io.FileInputStream(f).use { ins -> ins.copyTo(out) }
            } ?: throw Exception("打不开输出流")
            // 写完才解除待处理标记，此刻外界才第一次看到它
            cr.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null, null
            )
            Log.d("Camera", "★DNG 已完整发布★ ${f.length() / 1024 / 1024}MB → $uri")
            uri
        } catch (e: Exception) {
            Log.e("Camera", "DNG 发布失败", e)
            null
        }
    }

    fun takePhoto() {
        val ic = imageCapture ?: return

        // ★快门声★：按下就响，不等出片 —— 反馈要跟手指同步
        ShutterSound.play(shutterSound)

        //
        // ★★拍照瞬间临时切到"画质优先"★★
        //
        // 待机预览用的是 FAST 降噪 + 关锐化（为了流畅），
        // 直接拿这套参数出片，成片会偏软、噪点也多。
        // 所以在按下快门时先把参数提到 HIGH_QUALITY，让 HAL 按"出片"标准处理这一帧，
        // 拍完再切回流畅优先 —— 预览流畅和成片画质两头都要。
        //
        // 两个延时的取值：
        //   前置 120ms：给 HAL 一点时间把新参数应用到流水线
        //               （太快切的话 capture 请求可能还是旧参数）
        //   后置 900ms：ImageCapture 从下达到 JPEG 落地通常 300~700ms，
        //               900ms 足够覆盖，又不会让预览长时间停在高负载模式
        //
        captureQualityBoost = true
        applySceneParams()

        // 拍照瞬间把"扩展数码变焦倍数"和调色参数快照下来：
        // 后处理在 IO 线程跑，等它跑完用户可能已经又变焦了
        val boostSnapshot = digitalBoost
        val wSnap = toneWarmth
        val tSnap = toneTint
        val iSnap = toneIntensity
        val lensKeySnapshot = lensColorKey()
        //
        // ★RAW 模式下同一个时间戳要出两张★（DNG + JPEG），
        //   所以先把 stamp 抽出来，保证两个文件名对得上、后期好配对。
        //
        val stamp = System.currentTimeMillis()
        val useRaw = rawEnabled && (rawSupported == true || rawForce)
        //
        // ★本次拍摄到底有没有收到 DNG★
        //   强制 RAW / 探测误判时，格式可能没真设上 ——
        //   那时只会来一次 JPEG 回调。3 秒兜底到期时据此提示，
        //   免得用户以为"切了 RAW"其实一直在拍 JPEG。
        //
        var gotDng = false
        if (useRaw) rawBusy = true

        // DNG 先落私有文件（不进 MediaStore），验完再发布，杜绝半截被扫
        val dngDir = java.io.File(context.cacheDir, "pending_raw").apply { mkdirs() }
        val dngFile = java.io.File(dngDir, "IMG_$stamp.dng")

        val jpegValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_$stamp.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Silky Camera")
        }
        val jpegOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            jpegValues
        ).build()

        val callback = object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val uri = outputFileResults.savedUri ?: return
                // RAW 模式回调会来【两次】（DNG + JPEG），必须分开处理
                // 判据优先 getImageFormat()，取不到就看 .dng 后缀
                val fmt = RawMode.imageFormatOf(outputFileResults)
                val seg = uri.lastPathSegment.orEmpty()
                val isDng = fmt == android.graphics.ImageFormat.RAW_SENSOR ||
                        seg.endsWith(".dng", ignoreCase = true)
                if (isDng) {
                    gotDng = true
                    Log.d("Camera", "DNG 已落到暂存文件：${dngFile.absolutePath}")
                    // 此刻 DNG 很可能还没写完，等落盘 + 验完整 + 发布，都在 IO 线程做
                    coroutineScope.launch(Dispatchers.IO) {
                        waitFileStable(dngFile)
                        val rep = inspectDng(dngFile)
                        // 体检不通过就不发布：宁可丢，也别让用户拿到坏文件
                        if (rep.reason != null) {
                            Log.e(
                                "Camera",
                                "★DNG 体检不合格，已丢弃★ ${rep.reason} | " +
                                        "尺寸=${rep.width}x${rep.height} " +
                                        "实际=${rep.fileSize} 需要=${rep.expectedEnd}"
                            )
                            withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    context,
                                    "RAW 文件不完整，已丢弃：${rep.reason}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                            dngFile.delete()
                            withContext(Dispatchers.Main) {
                                rawBusy = false
                                restorePreviewParamsAfterCapture()
                            }
                            return@launch
                        }
                        val published = publishRawToDcim(dngFile, stamp)
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                context,
                                if (published != null)
                                    "RAW 已保存 ${rep.width}x${rep.height}（${rep.fileSize / 1024 / 1024}MB）"
                                else "RAW 保存失败，DNG 未能写入相册",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        Log.d(
                            "Camera",
                            "★DNG 已完整发布★ ${rep.width}x${rep.height} " +
                                    "${rep.fileSize / 1024 / 1024}MB"
                        )
                        dngFile.delete()
                        // DNG 全部处理完，现在才允许切回流畅预览
                        withContext(Dispatchers.Main) {
                            rawBusy = false
                            restorePreviewParamsAfterCapture()
                        }
                    }
                    // ⚠️ 这里【不】恢复预览参数：JPEG 那一路回调才恢复。
                    //    DNG 通常先落地，若在这里恢复，JPEG 就会用回低画质参数出片。
                    return
                }
                //
                // ★震动：确认"照片真的写进去了"★
                //   按快门那一下已经震过了，这里是【存盘完成】的第二段反馈。
                //   两者间隔就是后处理 + 写卡的耗时，用户能凭手感判断快不快。
                //
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                // 扩展数码变焦 / 调色：成片与取景框保持一致
                // 统一镜头色彩开启时，哪怕没缩放没调色也要跑一次后处理（套色彩修正）
                if (boostSnapshot > 1.001f ||
                    (iSnap > 0f && (abs(wSnap) > 0.5f || abs(tSnap) > 0.5f)) ||
                    LensColorUnifier.enabled
                ) {
                    coroutineScope.launch(Dispatchers.IO) {
                        postProcessPhoto(
                            uri, boostSnapshot, wSnap, tSnap, iSnap, lensKeySnapshot
                        )
                        //
                        // ★后处理写回同一个 uri，但缩略图不会自动重载★
                        //   必须把 version +1 让 produceState 重新解码，
                        //   否则相册里看到的一直是没调过色的原图。
                        //
                        withContext(Dispatchers.Main) {
                            val idx = sessionMedia.indexOfFirst {
                                it.uri == uri && !it.isVideo
                            }
                            if (idx >= 0) {
                                val old = sessionMedia[idx]
                                sessionMedia[idx] = old.copy(version = old.version + 1)
                            }
                        }
                    }
                }
                sessionMedia.add(0, CapturedMedia(uri, false))
                // 成功路径也要恢复；但 RAW 没写完时只能等，不能掐断 DNG
                restoreAfterCaptureIfIdle()
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e("Camera", "拍照失败", exception)
                // 失败了也要恢复，否则预览会一直停在高负载的画质模式
                rawBusy = false
                restorePreviewParamsAfterCapture()
            }
        }

        if (useRaw) {
            // 小米四拜耳大底：5000万 + RAW 时 RAW_SENSOR 与 JPEG 裁剪区对不上，
            // DNG 会缺角或带黑边。所以开 RAW 就退出 5000万。
            if (highResMode) {
                Log.w("Camera", "RAW 与 5000万模式冲突，已自动退出 5000万（四拜耳机型 DNG 会残缺）")
                android.widget.Toast.makeText(
                    context, "RAW 模式已自动退出 5000万像素（避免 DNG 残缺）",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                highResMode = false
            }
            // 写私有 File，不写 MediaStore
            val dngOptions = ImageCapture.OutputFileOptions.Builder(dngFile).build()
            if (rawSingleOutput) {
                // 单输出：只走 RAW_SENSOR 一条流（小米双输出会截断 RAW buffer）
                // JPEG 由下面单独再拍一张
                if (!RawMode.takeRawOnly(
                        ic, dngOptions,
                        ContextCompat.getMainExecutor(context), callback
                    )
                ) {
                    Log.w("Camera", "单输出 RAW 不可用，退回 JPEG")
                    rawBusy = false
                    ic.takePicture(
                        jpegOptions, ContextCompat.getMainExecutor(context), callback
                    )
                } else {
                    // 单输出模式下 DNG 自己一路，JPEG 单独再拍一次
                    ic.takePicture(
                        jpegOptions, ContextCompat.getMainExecutor(context), callback
                    )
                }
            } else if (!RawMode.takeRawAndJpeg(
                    ic, dngOptions, jpegOptions,
                    ContextCompat.getMainExecutor(context), callback
                )
            ) {
                Log.w("Camera", "RAW 拍摄不可用，本次按 JPEG 出片")
                rawBusy = false
                ic.takePicture(
                    jpegOptions, ContextCompat.getMainExecutor(context), callback
                )
            }
        } else {
            ic.takePicture(
                jpegOptions, ContextCompat.getMainExecutor(context), callback
            )
        }

        // ★兜底恢复★：万一回调没来（极少见），也要把预览参数还原，
        // 否则取景会一直卡在"画质优先"的高负载模式下变卡。
        // ⚠️ RAW 给更长时间：DNG 一张 25~50MB，写卡 + 回调常常超过 900ms，
        //    按 JPEG 的时限恢复会让 DNG 还没写完就被切回低画质预览。
        coroutineScope.launch {
            delay(if (useRaw) 3000 else 900)
            if (useRaw && !gotDng) {
                Log.w("Camera", "★RAW 未生效★：本次只出了 JPEG（$rawProbeDetail）")
                android.widget.Toast.makeText(
                    context,
                    "RAW 未生效，本次仍为 JPEG（本机实际不支持）",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                rawBusy = false
            }
            restoreAfterCaptureIfIdle()
        }
        // 绝对上限：DNG 回调石沉大海时也不能让 rawBusy 一直挂着
        if (useRaw) {
            coroutineScope.launch {
                delay(12000)
                if (rawBusy) {
                    Log.w("Camera", "RAW 超时未完成，强制结束等待")
                    rawBusy = false
                    restorePreviewParamsAfterCapture()
                }
            }
        }
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
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Silky Camera")
        }
        val outputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(values).build()

        val pending = vc.output.prepareRecording(context, outputOptions)
        val withAudio = try { pending.withAudioEnabled() } catch (e: SecurityException) { pending }

        recording = withAudio.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    isRecording = true
                    //
                    // ★震动：确认"真的开始录了"★
                    //   点击和真正出流之间有一段延迟（编码器初始化），
                    //   没有这个反馈的话，用户会以为没点上、然后多点几下。
                    //
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    //
                    // ★开录后锁白平衡★
                    // 不能在这一刻就锁：刚 start 时 AWB 往往还没收敛稳，
                    // 立刻锁会锁在一个偏色的值上，整段视频都带着这个偏色。
                    // 等 VIDEO_AWB_LOCK_DELAY_MS 让它收敛完，再下发锁定包。
                    //
                    coroutineScope.launch {
                        // ★4K60 等更久★：该档位下 3A 收敛被明显拉长，
                        //   400ms 时会锁在一个还在漂移的中间值上 → 整段视频都在飘。
                        val is4K60 = videoQuality == VideoQuality.UHD &&
                                videoFrameRate == VideoFrameRate.FPS_60
                        val waitMs = if (is4K60) VIDEO_AWB_LOCK_DELAY_MS_4K60
                        else VIDEO_AWB_LOCK_DELAY_MS
                        delay(waitMs)
                        if (isRecording) {
                            //
                            // ★开录后第一次下发：3A 收敛完成后的锁定包★
                            //
                            // ⚠️ 这不是"唯一一次"。之后录制中场景变化（切镜头 /
                            //    改防抖 / 换档位…）仍会正常下发 —— 见 ⑨。
                            //    区别只是：内容没变时不重复提交，
                            //    所以"参数能调"与"色彩恒定"可以兼得。
                            //
                            applySceneParams(force = true)
                            Log.d(
                                "Camera",
                                "★录制 3A 已锁定★（等待 ${waitMs}ms，AWB + AE 双锁；" +
                                        "此后参数随场景正常调整，无变化则不重复提交）"
                            )
                        }
                    }
                }
                is VideoRecordEvent.Finalize -> {
                    isRecording = false
                    recording = null
                    // ★震动：确认"录制已结束并保存"★
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    //
                    // ★录完解锁★：预览回到自动白平衡，否则取景会一直带着录制时的色偏。
                    // 同时补发录制期间被冻结的改动（sceneParamsDirty 在下发时清掉）。
                    //
                    applySceneParams()
                    Log.d(
                        "Camera",
                        "录制结束，白平衡已解锁" +
                                if (sceneParamsDirty) "（并补发了录制期间的改动）" else ""
                    )
                    if (!event.hasError()) {
                        val uri = event.outputResults.outputUri
                        if (uri != Uri.EMPTY) {
                            sessionMedia.add(0, CapturedMedia(uri, true))
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
    // 调色盘是否生效（有色偏才生效，正常拍照不做任何后处理）
    val toneActive: Boolean =
        toneIntensity > 0f && (abs(toneWarmth) > 0.5f || abs(toneTint) > 0.5f)
    //
    // ★★预览叠加色：改用与成片【同一个】[toneGains]★★
    //
    // 原来这里用的是 0.5±0.5w/100 这套"另算的近似色"，
    // 跟成片的实际增益毫无关系 —— 预览挺明显、成片几乎没变，
    // 这就是"影调色彩不对"的直接表现。
    //
    // 现在：取成片作用下的【中性灰】(0.5,0.5,0.5) → (0.5·rG, 0.5·gG, 0.5·bG)，
    // 把它作为叠加色。方向严格一致（暖调 → 叠加色偏红 → 画面偏暖），
    // 于是"预览看到的"就是"成片得到的"。
    //
    val toneOverlayColor: Color = if (!toneActive) Color.Transparent else {
        //
        // 成片对【中性灰】的输出（0..1）—— 预览要严格复现它
        //
        val g = toneGains(toneWarmth, toneTint, toneIntensity)
        val o = floatArrayOf(0.5f * g[0], 0.5f * g[1], 0.5f * g[2])
        //
        // ★解析求出叠加色，使预览的中性灰 = 成片的中性灰★
        //
        val ov = tonePreviewOverlay(o[0], o[1], o[2], TONE_PREVIEW_ALPHA)
        Color(
            red = ov[0],
            green = ov[1],
            blue = ov[2],
            alpha = TONE_PREVIEW_ALPHA
        )
    }

    //
    // ★★切换 RAW（DNG）拍摄格式★★
    //
    // 三种情况：
    //   ① 还没探测完（rawSupported == null）→ 提示稍后再试，不改状态
    //   ② 探测出来不支持        → 明确提示「该机型暂不支持 RAW 格式，无法切换」
    //   ③ 支持                → 正常切换
    //
    // ⚠️ 切换后【不需要手动重绑】：绑定 LaunchedEffect 的 key 里带了 rawEnabled，
    //    改这个状态会自动触发一次重建 ImageCapture（跟 highResMode 一个道理）。
    //
    fun toggleRaw() {
        when (rawSupported) {
            null -> {
                android.widget.Toast.makeText(
                    context, "正在检测本机 RAW 能力，请稍后再试",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            false -> {
                // 两种"不支持"提示必须不同：① CameraX 太旧，只能升库
                // ② API 在但探测说不，可能误判 → 提示可强制开启
                // ⚠️ 与手机型号 / 安卓版本无关：CameraX 是打进 APK 的库
                val msg = if (!RawMode.apiAvailable) {
                    "RAW 需要 CameraX 1.5.0 以上版本" +
                            "（当前 ${cameraXVersion ?: "版本未知"}），" +
                            "请升级 build.gradle 里的 androidx.camera 依赖"
                } else {
                    "该机型暂不支持 RAW 格式，无法切换（可在菜单里强制开启试试）"
                }
                android.widget.Toast.makeText(
                    context, msg, android.widget.Toast.LENGTH_LONG
                ).show()
                Log.w("Camera", "切换 RAW 被拒绝：$rawProbeDetail")
            }
            true -> {
                rawEnabled = !rawEnabled
                //
                // ★开 RAW 就退出 5000万★
                //   小米数字系列（四拜耳大底）在这个组合下 DNG 会缺角 / 带黑边，
                //   与其拍出残图，不如自动降回默认像素。
                //
                var tip = if (rawEnabled) "已切换为 RAW 格式（同时保存 DNG + JPEG）"
                else "已切换为 JPEG 格式"
                if (rawEnabled && highResMode) {
                    highResMode = false
                    tip = "已切换为 RAW，并退出 5000万模式（避免 DNG 残缺）"
                    Log.w("Camera", "开启 RAW：自动退出 5000万模式")
                }
                android.widget.Toast.makeText(
                    context, tip, android.widget.Toast.LENGTH_SHORT
                ).show()
                Log.d("Camera", "照片格式：${if (rawEnabled) "RAW+JPEG" else "JPEG"}")
            }
        }
    }

    //
    // ★强制 RAW★：检测误判时的逃生门。
    //   开了之后不再看 rawSupported，一律按 RAW 绑定 + 双输出拍摄。
    //   真设不上格式时 applyRawFormat 返回 false，自动退回 JPEG，不会崩。
    //
    // 单输出只走 RAW_SENSOR 一条流（小米双输出会截断 RAW buffer），
    // 代价是没有配套 JPEG，相册缩略图多半是灰的
    fun toggleRawSingleOutput() {
        rawSingleOutput = !rawSingleOutput
        android.widget.Toast.makeText(
            context,
            if (rawSingleOutput) "RAW 单输出：只保存 DNG（小米机型推荐）"
            else "RAW 双输出：DNG + JPEG",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        Log.d("Camera", "RAW 单输出=$rawSingleOutput")
    }

    fun toggleRawForce() {
        rawForce = !rawForce
        android.widget.Toast.makeText(
            context,
            if (rawForce) "已强制启用 RAW（若实际不支持会自动退回 JPEG）"
            else "已取消强制 RAW",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        Log.d("Camera", "强制 RAW=$rawForce；探测详情：$rawProbeDetail")
    }

    val settingsBarState = SettingsBarState(
        isFront = isFront,
        mode = mode,
        highResMode = highResMode,
        highResLabel = highResLabel,
        rawEnabled = rawEnabled,
        rawSupported = rawSupported,
        rawForce = rawForce,
        rawSingleOutput = rawSingleOutput,
        videoQuality = videoQuality,
        videoFrameRate = videoFrameRate,
        stabMode = stabMode,
        photoRatio = photoRatio,
        flashMode = flashMode,
        autoRotate = autoRotate,
        toneActive = toneActive
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
        onRawToggle = { toggleRaw() },
        onRawForceToggle = { toggleRawForce() },
        onRawSingleToggle = { toggleRawSingleOutput() },
        onVideoQualityChange = { videoQuality = it },
        onVideoFrameRateChange = { videoFrameRate = it },
        onStabilizationChange = { stabMode = it },
        onPhotoRatioChange = { photoRatio = it },
        onFlashExpand = { flashExpanded = true },
        onAbout = { showAbout = true },
        onColorPalette = { showColorPalette = true },
        onMenu = { menuPath = "" }
    )

    /**
     * ★统一的"返回"★：按从上层到下层的顺序关掉浮层，
     * 侧边返回手势和系统返回键都走这里，行为完全一致。
     */
    fun goBack() {
        when {
            showColorPalette -> showColorPalette = false
            menuPath != null -> menuPath = null
            flashExpanded -> flashExpanded = false
            showReward -> showReward = false
            showAbout -> showAbout = false
            // 开源许可页是从设置页进来的，返回要回到设置页而不是退出
            showOssLicense -> { showOssLicense = false; showAbout = true }
            showGallery -> showGallery = false
            showRuler -> showRuler = false
            else -> (context as? ComponentActivity)?.finish()
        }
    }

    // ★切换遮罩看门狗★
    // lensSwitching 为 true 时会在取景框上盖一层黑（遮住重绑瞬间比例跳动）。
    // 如果绑定流程因任何原因没走到收尾（提前 return / 协程被取消），
    // 这层黑就永远盖着 —— 表现正是"切到超广角后直接不显示画面"。
    // 兜底：超过 4 秒还没收就强制收掉。宁可看到一瞬间的比例跳动，
    // 也不能让用户对着一块黑屏。
    LaunchedEffect(lensSwitching) {
        if (!lensSwitching) return@LaunchedEffect
        delay(4000)
        if (lensSwitching) {
            lensSwitching = false
            Log.w("Camera", "镜头切换超时未收尾，已强制收起遮罩")
        }
    }

    //
    // ★★全局卡死自愈看门狗★★
    //
    // 「间歇性卡死」的特点是不崩、无异常、界面还在，就是某些操作没反应。
    // 这类问题的共同成因是【某个状态标志停在异常值上没人收尾】：
    //   · lensSwitching  卡 true → 取景框一直盖着黑，看着像死机
    //   · zoomAnimating  卡 true → 轮询循环不再更新倍率，变焦完全没反应
    //   · stabSuspended  卡 true → 防抖永远不回来，参数包停在"变焦中"那一版
    //   · captureQualityBoost 卡 true → 一直跑 HIGH_QUALITY 降噪，预览持续卡
    //   · scanDone       卡 false → 所有相机操作闸门关闭，点什么都没反应
    //
    // 每 500ms 巡检一次，用【连续命中计数】而不是单次判断：
    //   正常流程也会短暂经过这些状态，只有"连续 N 次还是这样"才认定卡死，
    //   避免在正常切换的中途误触发恢复。
    //
    // CameraX 版本探测：只在启动时做一次，用于 RAW 的提示文案与日志
    LaunchedEffect(Unit) {
        cameraXVersion = RawMode.detectVersion(context)
        Log.d("Camera", "CameraX 版本 = ${cameraXVersion ?: "未知"}")
    }

    LaunchedEffect(Unit) {
        var lensTicks = 0
        var animTicks = 0
        var stabTicks = 0
        var boostTicks = 0
        var scanTicks = 0
        var calibTicks = 0
        var starTicks = 0
        while (true) {
            delay(500)

            //
            // ★★整个巡检体包 try/catch★★
            //
            // 这是本轮复查发现的【最隐蔽的一个坑】：
            //   下面的自愈动作里有两处会调真实函数
            //     · ③ 调 applySceneParams()
            //     · ④ 调 restorePreviewParamsAfterCapture()
            //   而 camera 可能还没绑好、Camera2CameraControl 可能已失效，
            //   这两处【都有可能抛异常】。
            //   LaunchedEffect 的协程体一旦抛出未捕获异常就【整体终止】——
            //   于是看门狗自己死掉，①②③④⑤ 全部失效，
            //   后面再卡任何标志都没人管了。等于"自愈机制本身会猝死"。
            //
            //   包一层 try/catch 之后：单次巡检出错只是跳过这一轮，
            //   下一轮照常巡检。自愈机制才真的可靠。
            //
            try {

                // ① lensSwitching：连续 5 次（≈2.5s）仍为 true → 强制收起黑遮罩
                if (lensSwitching) lensTicks++ else lensTicks = 0
                if (lensTicks == 5) {
                    lensSwitching = false
                    Log.w("Camera", "★自愈★ lensSwitching 卡住 2.5s，已强制收起遮罩")
                }

                // ② zoomAnimating：置上了但变焦协程其实没在跑 → 轮询循环被永久挡住
                val zoomRunning = zoomJob != null && zoomJob?.isActive == true
                if (zoomAnimating[0] && !zoomRunning) animTicks++ else animTicks = 0
                if (animTicks == 3) {
                    zoomAnimating[0] = false
                    Log.w("Camera", "★自愈★ zoomAnimating 卡住（无活动变焦协程），已复位")
                }

                // ③ stabSuspended：没有变焦、没有拖动、没有切镜头，却仍处于挂起
                val idle = !zoomRunning && !isDragging && !lensSwitching
                if (stabSuspended && idle) stabTicks++ else stabTicks = 0
                if (stabTicks == 4) {
                    stabSuspended = false
                    applySceneParams()
                    Log.w("Camera", "★自愈★ stabSuspended 卡住（已空闲 2s），防抖已恢复")
                }

                // ④ captureQualityBoost：拍照结束后没恢复 → 一直跑高负载降噪
                if (captureQualityBoost) boostTicks++ else boostTicks = 0
                if (boostTicks == 6) {
                    restorePreviewParamsAfterCapture()
                    Log.w("Camera", "★自愈★ captureQualityBoost 卡住 3s，已切回预览流畅优先")
                }

                // ⑤ scanDone：闸门卡关 → 整机点什么都没反应，最严重的一种
                if (!scanDone) scanTicks++ else scanTicks = 0
                if (scanTicks == 16) {
                    scanDone = true
                    lensSwitching = false
                    Log.w("Camera", "★自愈★ scanDone 卡关 8s，已强制放行（相机操作恢复）")
                }

                // ⑥ calibrating：校准弹窗停在「校准中…」→ 只能杀进程
                if (calibrating) calibTicks++ else calibTicks = 0
                if (calibTicks == 40) {
                    calibrating = false
                    Log.w("Camera", "★自愈★ calibrating 卡住 20s，已复位（校准弹窗恢复可操作）")
                }

                // ⑦ starTrailRunning：星轨协程已结束却没复位 → 按钮一直显示「拍摄中」
                //   判据：有引用记录的 job 且已不活跃；没有 job 引用时按 30s 兜底
                if (starTrailRunning) {
                    val j = starTrailJobRef
                    if (j != null) {
                        if (!j.isActive) starTicks++ else starTicks = 0
                    } else starTicks++
                } else starTicks = 0
                if (starTicks == 60) {
                    starTrailRunning = false
                    starTrailStop[0] = true
                    Log.w("Camera", "★自愈★ starTrailRunning 卡住 30s，已复位")
                }

            } catch (e: Exception) {
                // 单次巡检失败不影响后续巡检，只打日志
                Log.w("Camera", "自愈巡检本轮出错（已跳过，不影响后续巡检）", e)
            }
        }
    }

    // ★录制计时★：每秒 +1，驱动时间码刷新。
    // 只在 isRecording 为 true 时存在这个协程，录完自动结束，不空转。
    LaunchedEffect(isRecording) {
        if (!isRecording) {
            recordedSeconds = 0L
            return@LaunchedEffect
        }
        val base = android.os.SystemClock.elapsedRealtime()
        while (isActive && isRecording) {
            val el = android.os.SystemClock.elapsedRealtime() - base
            recordedSeconds = el / 1000L
            //
            // ★睡到【下一个整秒边界】再醒，而不是固定 delay(250)★
            //
            //   原来每 250ms 醒一次 → 每秒 4 次重组，但其中 3 次秒数根本没变，
            //   等于 3/4 的重组是纯浪费（而 recordedSeconds 是 CameraApp 的
            //   State，写一次就是一次整体重组）。
            //   现在每秒只醒一次，且刚好落在秒数跳变的那一刻 +20ms，
            //   时间码既不滞后也不空转 —— 重组次数降到 1/4。
            //   +20ms 是防止系统时钟取整误差导致"醒了但秒还没进"。
            //
            val next = (el / 1000L + 1L) * 1000L + 20L
            delay((next - el).coerceIn(50L, 1000L))
        }
    }

    // 系统返回键 / 手势导航：跟侧边返回走同一套逻辑
    BackHandler { goBack() }

    // ★下发文字旋转角★：界面没跟着屏幕转时，UI 上的文字靠这个转正。
    // 跟随旋转时是 0，等于什么都不做，不会有副作用。
    CompositionLocalProvider(LocalLabelRotation provides labelRotationAnim) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

            // ── 苹果风格调色盘 ──
            if (showColorPalette) {
                AppleColorPalette(
                    warmth = toneWarmth,
                    tint = toneTint,
                    intensity = toneIntensity,
                    onWarmthChange = { toneWarmth = it },
                    onTintChange = { toneTint = it },
                    onIntensityChange = { toneIntensity = it },
                    onReset = {
                        toneWarmth = 0f
                        toneTint = 0f
                        toneIntensity = 100f
                    },
                    landscape = relayoutLandscape,
                    onClose = { showColorPalette = false }
                )
            }

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

            // ── 首次启动：选品牌 → 选系列 ──
            if (showDevicePicker) {
                BrandSeriesPickerDialog(
                    detected = remember { SeriesDb.guess() },
                    onConfirm = { p ->
                        DeviceProfileStore.save(context, p)
                        ActiveDevice.apply(p)
                        showDevicePicker = false
                        // 机型档案换了 → 变焦档位、直连策略、收敛等待全要按新档案重来一遍
                        LensScanCache.clear()
                        //
                        // ★连进程级探测缓存一起清★
                        // DeviceCompatibility 里还有三份 static 缓存
                        // （lensProbeCache / physicalProbeCache / vendorStopsCache），
                        // 它们之前【从来没有清除入口】—— clearProbeCache() 定义了却从未被调用。
                        // 换机型后仍返回旧机型的镜头表/焦段，新档案的策略根本套不上。
                        //
                        DeviceCompatibility.clearProbeCache()
                        // ★连同变焦档位存档一起清★：换了机型 / 重置后要重新探测，
                        // 否则会一直沿用旧机型存档的档位
                        ZoomStopsStore.clear(context)
                        lensSwitching = true
                        pendingZoomTarget = displayedRatio
                        boundLensId = null
                        scanDone = false
                        hasCalibrated = false
                        // ★档位也要重新定★：上面把存档清了，如果 zoomStopsReady 还是 true，
                        // 标定那条 if 会立刻放行，而此时档位其实还没算出来 → 标定用空档位跑，
                        // 出来的映射是错的（表现就是变焦按键点了没反应或跳错档）
                        zoomStopsReady = false
                        // ★强制重跑绑定流程★：boundLensId 常态就是 null，
                        // 光把它设成 null 不会触发 LaunchedEffect，必须靠这个计数器
                        deviceEpoch++
                        Log.d("Camera", "已切换机型档案 → ${p?.name ?: "通用"}，重新扫描镜头")
                    },
                    onSkip = {
                        DeviceProfileStore.save(context, null)
                        ActiveDevice.apply(null)
                        showDevicePicker = false
                    }
                )
            }

            // ── 机型与色彩（关于页进入）──
            if (showDeviceTuning) {
                DeviceTuningDialog(
                    context = context,
                    current = ActiveDevice.profile,
                    colorUnify = colorUnify,
                    fastSwitch = fastLensSwitch,
                    lockAwb = lockAwb,
                    calibrating = calibrating,
                    currentLensKey = lensColorKey(),
                    onColorUnify = {
                        colorUnify = it
                        LensColorUnifier.enabled = it
                        DeviceProfileStore.setColorUnify(context, it)
                        applySceneParams()
                    },
                    onFastSwitch = {
                        fastLensSwitch = it
                        ActiveDevice.fastLensSwitch = it
                        DeviceProfileStore.setFastLensSwitch(context, it)
                    },
                    onLockAwb = {
                        lockAwb = it
                        LensColorUnifier.lockAwb = it
                        DeviceProfileStore.setLockAwb(context, it)
                        applySceneParams()
                    },
                    onPickSeries = { showDeviceTuning = false; showDevicePicker = true },
                    onCalibrate = { calibrateLensColor() },
                    onResetCalibration = {
                        LensColorUnifier.clearCalibration(context)
                        android.widget.Toast.makeText(context, "已清除全部校准数据", android.widget.Toast.LENGTH_SHORT).show()
                        applySceneParams()
                    },
                    onClose = { showDeviceTuning = false }
                )
            }

            // ── 星空：参数设置 / 拍摄进度 / 结果 ──
            if (showStarTrailSetup) {
                StarTrailSetupDialog(
                    cfg = starTrailConfig,
                    onChange = {
                        starTrailConfig = it
                        StarTrailStore.save(context, it)
                        camera?.let { c -> applyStarTrailPreviewParams(c, it) }
                    },
                    onClose = { showStarTrailSetup = false }
                )
            }
            if (starTrailRunning) {
                StarTrailProgressOverlay(
                    done = starTrailDone,
                    total = starTrailTotal.coerceAtLeast(1),
                    preview = starTrailPreview,
                    onStop = { stopStarTrail() }
                )
            }
            starTrailResult?.let { r ->
                StarTrailResultDialog(result = r) {
                    starTrailResult = null
                    starTrailPreview?.let { if (!it.isRecycled) it.recycle() }
                    starTrailPreview = null
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    //
                    // ★★双击空白处隐藏 / 显示 UI★★
                    //
                    // 挂载在【预览画面的父 Box】上（fillMaxSize），所以点画面
                    // 任意空白处都算，不用精确点到某个控件。
                    //
                    // 三个要点：
                    //   ① 只在 uiHideEnabled（菜单里那个开关）打开时才生效，
                    //      否则双击不响应，避免误触。
                    //   ② detectTapGestures 的 onDoubleTap 已经帮我们判好了
                    //      "两次点击间隔足够短"（默认 400ms 内），不用自己算时间。
                    //   ③ 这里【不消费】单击 —— 没有传 onTap，
                    //      所以单击照常走下面的对焦 / 缩放手势，不会互相打架。
                    //
                    .then(
                        if (uiHideEnabled)
                            Modifier.pointerInput(uiHideEnabled) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        uiHidden = !uiHidden
                                        Log.d(
                                            "Camera",
                                            if (uiHidden) "双击：UI 已隐藏" else "双击：UI 已显示"
                                        )
                                    }
                                )
                            }
                        else Modifier
                    )
                    //
                    // ★★左右滑动切换拍摄模式★★
                    //
                    // 为什么手写手势识别而不用 detectHorizontalDragGestures：
                    //   预览画面上已经挂了 detectTransformGestures（双指捏合变焦）。
                    //   Compose 的手势是【内层先收、外层后收】，等事件冒泡到这里时，
                    //   内层可能已经消费掉了，用现成的 detector 有时收不到。
                    //   改成 awaitPointerEvent(PointerEventPass.Initial)：
                    //   Initial 阶段让【父节点先于子节点】看到事件，不受内层影响。
                    //
                    //   ⚠️ awaitPointerEventScope / awaitPointerEvent 是 PointerInputScope /
                    //      AwaitPointerEventScope 的【成员函数】，不需要 import；
                    //      只有 awaitFirstDown 是顶层扩展，且包名是 foundation.gestures
                    //      （不是 ui.input.pointer —— 那个包里没有它，会 Unresolved reference）。
                    //
                    // 三个防误触的约束：
                    //   ① 没到阈值不消费 —— 点一下、轻微移动都不影响原有手势
                    //   ② 要求横向位移 > 纵向位移 × 1.5 —— 斜着划不算
                    //   ③ 多指（捏合变焦）直接放弃，不抢
                    //
                    // 触发之后才 consume()，把这次手势从内层抢过来，避免同时变焦。
                    //
                    .then(
                        if (swipeModeEnabled)
                            Modifier.pointerInput(swipeModeEnabled) {
                                val thresholdPx = SWIPE_MODE_THRESHOLD_DP.dp.toPx()
                                awaitPointerEventScope {
                                    while (true) {
                                        val down = awaitFirstDown(
                                            requireUnconsumed = false,
                                            pass = PointerEventPass.Initial
                                        )
                                        val startX = down.position.x
                                        val startY = down.position.y
                                        var fired = false
                                        while (true) {
                                            val ev = awaitPointerEvent(PointerEventPass.Initial)
                                            // 多指 = 捏合变焦，让给内层
                                            if (ev.changes.count { it.pressed } > 1) break
                                            val ch = ev.changes.firstOrNull { it.id == down.id }
                                                ?: break
                                            if (!ch.pressed) break
                                            val dx = ch.position.x - startX
                                            val dy = ch.position.y - startY
                                            if (!fired &&
                                                abs(dx) >= thresholdPx &&
                                                abs(dx) > abs(dy) * SWIPE_MODE_AXIS_RATIO
                                            ) {
                                                fired = true
                                                // 左滑（dx<0）→ 下一个；右滑 → 上一个
                                                val dir = if (dx < 0f) 1 else -1
                                                switchMode(adjacentMode(dir))
                                            }
                                            if (fired) ch.consume()
                                        }
                                    }
                                }
                            }
                        else Modifier
                    )
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
                        // ★15x 数码变焦的正确做法★
                        // 顺序非常关键：先 clip（把范围锁死在取景框内），再 graphicsLayer 缩放。
                        // 以前是先缩放后裁剪 —— 放大后的画面会溢出到取景框外面，
                        // 看上去就像「取景框自己变大了」。现在取景框尺寸恒定，
                        // 只有框【里面】的画面被放大，拍照时再按同一比例做中心裁切，
                        // 所以取景和成片完全一致。
                        .clip(RoundedCornerShape(0.dp))
                        .graphicsLayer {
                            // ★用平滑后的值★：直接用 digitalBoost 会因为 UI 节流而一跳一跳
                            scaleX = digitalBoostAnim
                            scaleY = digitalBoostAnim
                        }
                        // 横屏：在居中的基础上向左微调，平衡右侧"变焦+拍摄+模式栏"三列
                        // 占掉的空间。只是微调，画面整体仍然居中。
                        .offset(x = if (relayoutLandscape) (-40).dp else 0.dp)
                        .drawWithContent {
                            // 预览原样绘制，不再按焦段套滤镜（见上方 isUltraWide 处的说明）
                            drawContent()
                            // ★调色盘在预览上的近似呈现★
                            // 真实色偏在成片上由后处理套用，这里只是让你在取景时就能看到色调走向
                            if (toneActive) {
                                drawRect(color = toneOverlayColor)
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
                                boostTarget =
                                    if (newRatio > halMaxZoomRatio + 0.01f) newRatio else 0f
                                // ★拖动也做 HAL 下发去重★
                                // 拖过 halMax 之后（15x 段）clampForHal 恒等于 halMax，
                                // 每 60~150ms 下发一个相同值纯属浪费，还会拖累 zoomState。
                                val dragHal = clampForHal(newRatio)
                                if (abs(dragHal - lastDragHalSent[0]) > 0.0015f) {
                                    cam.cameraControl.setZoomRatio(dragHal)
                                    lastDragHalSent[0] = dragHal
                                }
                                displayedRatio = newRatio
                                selectedZoom = nearestStop(newRatio)
                                lastRulerInteraction = now
                            }
                        },
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            // ★性能优化★：只有"必须靠物理直连切镜头"的机型才用 COMPATIBLE
                            // （TextureView），其余一律走 PERFORMANCE（SurfaceView）——
                            // 后者的预览走独立合成层，省一次纹理上传，明显更流畅省电。
                            implementationMode =
                                if (DeviceCompatibility.isStubbornMultiCam)
                                    PreviewView.ImplementationMode.COMPATIBLE
                                else PreviewView.ImplementationMode.PERFORMANCE
                            previewViewRef = this
                        }
                    },
                    update = { }
                )
            }

            //
            //
            // ★★UI 隐藏（双击空白处切换）★★
            //
            // 只有【UI隐藏开关打开】时才生效（菜单 → 其他 → UI隐藏）。
            // 隐藏的是【除预览画面外的一切】：设置栏、模式栏、快门组、
            // 变焦轮盘、菜单、辅助线、时间码、各种弹窗按钮入口…
            //
            // ⚠️ 侧边返回手势（EdgeSwipeBack）【不隐藏】——
            //    它是从屏幕左边缘起的手势，隐藏 UI 时画面是干净的，
            //    保留它反而给用户一条"退回去"的路，不会卡在纯画面里。
            //
            // ⚠️ UI 隐藏状态【不持久化】：下次打开相机一定是正常显示，
            //    否则用户忘了自己藏过 UI，一打开看到光秃秃的画面会以为坏了。
            //
            if (!uiHidden) {
                // 构图辅助线（三分线 + 水平仪）。画在预览上层、黑遮罩下层：
                // 切镜头瞬间辅助线还留着，不会跟着画面一起闪没。
                // ⚠️ 不会进视频/照片：CameraX 直接从相机流取帧编码，跟屏幕合成无关。
                //
                // 必须画在【取景框】里而不是整个屏幕：取景框是 aspectRatio 居中的，
                // 以前 fillMaxSize 导致黑边上也有线、水平仪中心也不在画面中心。
                // ⚠️ 必须 .align(Alignment.Center)：上层 Box 没指定 contentAlignment
                //   （默认 TopStart），而预览在另一个明确 Center 的子 Box 里居中，
                //   只写 aspectRatio 会被摆到左上角 —— 这就是"参考线对不上取景框"的原因。
                //
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .aspectRatio(previewRatio)
                        .offset(x = if (relayoutLandscape) (-40).dp else 0.dp)
                ) {
                    CompositionGuides(
                        horizonDeg = horizonDeg,
                        visible = showGuides && !isFront,
                        // ★镜头朝地面 → 只隐藏水平仪，三分线保留★
                        levelVisible = !facingGround
                    )
                }

                //
                // ★★录像时间码【跟随屏幕方向】★★
                //
                //   · 竖屏（设备 0° / 180°） → 屏幕【上方】居中
                //   · 横屏（设备 90°）       → 屏幕【左侧】居中
                //   · 横屏（设备 270°）      → 屏幕【右侧】居中
                //
                // 之前是不管什么朝向都固定在画布顶部居中，手机横过来时
                // 时间码就跑到真实世界的【侧面】去了，看着很别扭。
                //
                // 注意判据用【设备物理方向 deviceRotationDeg】，不能用
                // relayoutLandscape / Configuration.ORIENTATION_LANDSCAPE：
                //   UI 被锁成竖屏（LOCK_UI_PORTRAIT）后，Configuration 恒为 PORTRAIT，
                //   读不到"手机真的横过来了"。
                //
                // 同理不进视频 / 照片（CameraX 直接从相机流取帧，与屏幕合成无关）。
                //
                if (isRecording) {
                    val rot = ((deviceRotationDeg.roundToInt() % 360) + 360) % 360
                    val portrait = rot == 0 || rot == 180
                    // 横屏放【左右两侧】：UI 锁竖屏，横屏时画布的上下被顶部设置栏
                    // 和底部模式栏占满（画布高度只剩原来的一半多点），塞不下；
                    // 而左右两侧是取景框外的黑边，正好空着。
                    val align = when (rot) {
                        90 -> Alignment.CenterStart
                        270 -> Alignment.CenterEnd
                        else -> Alignment.TopCenter
                    }
                    Box(
                        modifier = Modifier
                            .align(align)
                            //
                            // ★竖屏时躲开顶部设置栏★
                            // 设置栏在 TopCenter + statusBarsPadding + padding(top≈12dp)，
                            // 那排 chip 高约 32dp —— 时间码若也贴顶就会压在上面，
                            // 所以下移到 52dp。横屏放侧面则不需要躲它。
                            //
                            .then(if (portrait) Modifier.statusBarsPadding() else Modifier)
                            .padding(
                                top = if (portrait) 52.dp else 0.dp,
                                start = if (rot == 90) 8.dp else 0.dp,
                                end = if (rot == 270) 8.dp else 0.dp
                            )
                            //
                            // ★文字转正，但【不要动画】★
                            //
                            // 原来用的是 LocalLabelRotation.current（= labelRotationAnim），
                            // 它是 animateFloatAsState 的 300ms 缓动值 —— 转屏时时间码会
                            // 慢悠悠地转过去，录制中看着很拖沓、也跟"位置瞬间切换"
                            // 对不上。现在改用【瞬时值】shortestLabelRotation：
                            // 位置跟随方向瞬间切换，文字也瞬间转正，无过渡动画。
                            //
                            // 想让文字完全不转（横屏时数字侧躺），把这一行删掉即可。
                            //
                            .rotate(shortestLabelRotation)
                    ) {
                        RecordingTimecode(seconds = recordedSeconds)
                    }
                }

                // ★切换镜头时盖一层黑★：重绑会短暂改变预览分辨率，
                // 取景框比例会跟着跳一下。遮住这一瞬间，就看不到"比例突然变一下"。
                if (lensSwitching) {
                    // ★同样要 align(Center)★：原因同上，不然黑遮罩盖在左上角而不是取景框上
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .aspectRatio(previewRatio)
                            .offset(x = if (relayoutLandscape) (-40).dp else 0.dp)
                            .clip(RoundedCornerShape(0.dp))
                            .background(Color.Black)
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
                if (!relayoutLandscape) {
                    SettingsChipsRow(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = if (uiCompact) 6.dp else 12.dp),
                        st = settingsBarState,
                        act = settingsBarActions,
                        hidden = hiddenChipIds,
                        onHiddenChange = { hiddenChipIds = it }
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

                // ── 横屏：三块分开到两侧，中间整块留给取景框 ──
                // 竖屏时都堆在底部会糊在画面正中；横屏屏幕更矮，必须分开。
                //   左侧 = 参数栏（像素/画质/帧率/防抖/比例/旋转/闪光/关于），竖排
                //   右侧 = 模式切换 + 变焦（倍率+档位）+ 拍摄键，统一放进【同一个 Column】竖排
                //   底部 = 相册 | 快门 | 切换，其下方是模式栏；中间 = 取景框
                // ★右侧合成一个 Column 是关键★：以前三块各自 align，变焦档位一多就会
                //   盖住快门；放进同一 Column 按顺序排，永远不会重叠。
                // 挖孔避让跟着【左侧那栏】走（挖孔通常在某一条长边上，左侧栏要往里推）。
                if (relayoutLandscape) {
                    // ── 左侧：参数栏，竖排 ──
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .navigationBarsPadding()
                            // 挖孔在左边时往里推，避免被前置摄像头挡住点不到
                            // 基础留白 10.dp：比原来 12.dp 略收，但仍保证不至于贴在屏幕边缘
                            .padding(start = 10.dp + cutoutLeftDp)
                            // ★限高 + 滚动★：不设上限的话 Column 会被内容撑出屏幕，
                            // 滚动也就无从谈起了
                            .fillMaxHeight(0.9f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SettingsChipsColumn(
                            st = settingsBarState,
                            act = settingsBarActions,
                            hidden = hiddenChipIds,
                            onHiddenChange = { hiddenChipIds = it }
                        )
                    }

                }

                // 竖屏：整块控制区在底部居中；横屏：挪到屏幕【右侧】垂直居中
                // （参考图向左转 90° 后，原本在底部的东西都落到右边）
                Column(
                    modifier = if (relayoutLandscape) {
                        Modifier
                            .align(Alignment.CenterEnd)
                            .navigationBarsPadding()
                            .padding(end = 14.dp)
                    } else {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            // 横屏沿用竖屏布局时竖向很紧，底部留白收到 6dp
                            .padding(bottom = if (uiCompact) 6.dp else 24.dp)
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!isFront && !relayoutLandscape) {
                        // ★变焦提示已取消★：原来这里有一行 "1.0x · 23mm 主摄" 的倍率 + 镜头标签，
                        // 它把底部控制区又顶高了一截、还跟档位按钮上的倍率重复。现在只保留档位按钮。

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
                                // ★轮盘用【全局范围】，且在拖动开始前就退回逻辑摄像头★
                                //
                                // 不能用 effMinZoom()/effMaxZoom()：那是【当前这颗物理头】的可用区间
                                // （如 3x 头只有 3~12x），传过去轮盘会被压缩成一小段 ——
                                // 这就是"轮盘只有 0.7x 到 2.5x"的直接来源。
                                //
                                // 而"轮盘和倍率对不上"的真正原因是：直连物理头时下发的是本地倍率，
                                // 拖到 1x 在 3x 头上等于原地不动。解决办法不是缩小轮盘，
                                // 而是【拖动开始前先退回逻辑摄像头】—— 见下面的 onDragStateChange。
                                minRatio = minRatio, maxRatio = maxRatio,
                                onRatioChange = {
                                    lastRulerInteraction = System.currentTimeMillis()
                                    directSetRatio(it)
                                },
                                onDragStateChange = { dragging ->
                                    isDragging = dragging
                                    // 拖动全程走逻辑摄像头：它是唯一能覆盖完整全局范围的，
                                    // 这样轮盘每一个位置都对应真实倍率，不会"拖了不动"。
                                    // 松手时由 commitZoom 吸附到光学档，需要的话再直连物理头 ——
                                    // 这也正是原厂 App 的行为（拖动时数码、松手后切光学镜头）。
                                    if (dragging && boundLensId != null) {
                                        Log.d(
                                            "Camera",
                                            "开始拖动变焦，先从物理头 ${boundLensId} 退回逻辑摄像头以保证全程可拖"
                                        )
                                        pendingZoomTarget = displayedRatio
                                        lensSwitching = true
                                        boundLensId = null
                                    }
                                },
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
                            // ★一行排开，档位之间只留一小段距离★
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        if (!relayoutLandscape) {
                            Spacer(modifier = Modifier.height(if (uiCompact) 6.dp else 14.dp))
                        }
                    } else if (!relayoutLandscape) {
                        Spacer(modifier = Modifier.height(if (uiCompact) 6.dp else 14.dp))
                    }

                    // ── 专业模式参数区 ──
                    // ★横屏不再横铺★ 原来是 fillMaxWidth 的一整条，横屏时会横跨屏幕压住取景框。
                    // 现在抽成 lambda，竖屏照旧横铺在底部；横屏时塞进右侧那一竖条里，
                    // 和录像模式的横屏结构完全一致：左=设置栏，右=参数/变焦/快门/模式栏。
                    val proSection: @Composable (Boolean) -> Unit = { vertical ->
                        if (mode == CaptureMode.PRO && !isFront && scanDone) {
                            if (!manualControlSupported) {
                                Box(
                                    modifier = Modifier
                                        .then(
                                            if (vertical) Modifier.width(150.dp)
                                            else Modifier.fillMaxWidth()
                                        )
                                        .padding(
                                            horizontal = if (vertical) 0.dp else 24.dp,
                                            vertical = 8.dp
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "此设备不支持手动控制",
                                        color = Color(0xFFFFA000),
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        // ★只旋转文字★
                                        modifier = Modifier.rotate(LocalLabelRotation.current)
                                    )
                                }
                                if (!vertical) Spacer(modifier = Modifier.height(10.dp))
                            } else {
                                ProControls(
                                    vertical = vertical,
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
                                    },
                                    activeParam = proActiveParam,
                                    onActiveParamChange = { proActiveParam = it }
                                )
                                if (!vertical) Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }

                    // 竖屏：参数区横铺在底部（保持原样）
                    if (!relayoutLandscape) proSection(false)


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
                                // ★换成矢量图标★（原来是 Text("🖼") emoji）：
                                //   emoji 各品牌渲染不一样，缺字体的设备还会显示豆腐块
                                Image(
                                    painter = painterResource(R.drawable.ic_gallery),
                                    contentDescription = "相册",
                                    // tint 白色：图标本身是纯白，这里统一着色便于以后改配色
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter
                                        .tint(Color.White),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        // ★拍摄键常驻底部中央（跟参考图一致）★
                        // 横屏也放在这里：参考图的快门就是底部居中，
                        // 左侧变焦、右侧参数，中间底部留给快门和模式栏。
                        ShutterButton(
                            isVideoMode = mode == CaptureMode.VIDEO,
                            isRecording = isRecording,
                            landscape = relayoutLandscape,
                            onVideoClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isRecording) stopRecording() else startRecording()
                            },
                            onPhotoClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (mode == CaptureMode.STAR_TRAIL) {
                                    // 星空模式：快门 = 开始 / 停止堆栈
                                    if (starTrailRunning) stopStarTrail() else startStarTrail()
                                } else takePhoto()
                            }
                        )

                        Box(
                            modifier = Modifier
                                .size(52.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable {
                                    if (isFlipping) return@clickable
                                    //
                                    // ★录制中禁止切前后摄★
                                    // 切换要 unbindAll + bindToLifecycle，会把 VideoCapture 一起拆掉 ——
                                    // 录制直接中断，还可能留下一个损坏的 mp4
                                    // （CameraX 会以 ERROR_SOURCE_INACTIVE 之类的错误 finalize）。
                                    // 星空堆栈同理：连拍中途换摄像头，后面的帧全废。
                                    //
                                    if (isRecording || starTrailRunning) {
                                        android.widget.Toast.makeText(
                                            context,
                                            if (isRecording) "录制中无法切换摄像头" else "星空拍摄中无法切换摄像头",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                        return@clickable
                                    }
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
                            // ★换成矢量图标★（原来是 Text("🔄") emoji）
                            Image(
                                painter = painterResource(R.drawable.ic_flip_camera),
                                contentDescription = "切换前后摄",
                                colorFilter = androidx.compose.ui.graphics.ColorFilter
                                    .tint(Color.White),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    if (relayoutLandscape) {
                        // 横屏：整体放到屏幕【右侧】。从左到右依次是：
                        //   专业参数（只在专业模式出现）→ 变焦 → 拍摄键 → 模式栏
                        // 左侧那条是设置栏（像素/比例/闪光/关于），跟录像模式完全一致。
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            // 12dp → 8dp：这一排横向占用再收一点，给取景框让出空间
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // ⓪ 专业模式参数：竖排一列，点开滑条时向左展开
                            if (mode == CaptureMode.PRO) {
                                proSection(true)
                            }

                            // ① 变焦档位：竖排在拍摄键的【左边】
                            //    横屏时从左到右依次是 变焦 → 拍摄 → 模式栏。
                            if (!isFront) {
                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // ★变焦提示已取消★：原来这里有 "倍率一行 + 镜头标签一行"，
                                    // 跟下面的档位按钮重复，白占竖向空间。现在只保留档位按钮。

                                    // ★横屏：档位竖排，自下而上 = 广角 → 长焦★
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
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
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

                            // ★横屏：这一栏在"模式切换"和"专业参数面板"之间二选一★
                            // 点开某个参数（EV / S / ISO / WB / F）时，参数面板【竖着替换掉模式栏】，
                            // 而不是像以前那样往取景框里再塞一块：
                            //   · 预览空间一点不损失（总宽度没变）
                            //   · 面板就长在原来模式栏的位置，手指不用挪地方
                            //   · 再点一次那个参数按钮（或点重置）就切回模式栏
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    // ★给竖排滑条留出高度★：轨道 150dp + 标签/数值两行
                                    .height(190.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Crossfade(
                                    targetState = (proActiveParam != null) to mode,
                                    animationSpec = tween(durationMillis = 220)
                                ) { state ->
                                    val panelOpen = state.first
                                    val curMode = state.second
                                    if (panelOpen && curMode == CaptureMode.PRO && !isFront) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.Black.copy(alpha = 0.82f))
                                                .border(
                                                    1.dp,
                                                    Color.White.copy(alpha = 0.18f),
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 12.dp)
                                        ) {
                                            ProParamPanel(
                                                activeParam = proActiveParam,
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
                                                compact = true,
                                                // ★滑条竖着画★：这一列窄而高，横着画只有几十 dp 拖不准
                                                verticalTrack = true
                                            )
                                        }
                                    } else {
                                        ModeSelector(
                                            current = mode,
                                            vertical = true,
                                            onSelect = { next -> switchMode(next) }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // 竖屏：模式栏在上、快门组在下，保持原来的结构
                        ModeSelector(
                            current = mode,
                            onSelect = { next -> switchMode(next) }
                        )
                        if (mode == CaptureMode.STAR_TRAIL) {
                            StarTrailBar(
                                cfg = starTrailConfig,
                                running = starTrailRunning
                            ) { showStarTrailSetup = true }
                            Spacer(modifier = Modifier.height(10.dp))
                        }
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
                        onShowReward = { showReward = true },
                        onOpenDeviceTuning = { showAbout = false; showDeviceTuning = true },
                        onOpenOssLicense = { showAbout = false; showOssLicense = true }
                    )
                }

                //
                // ★开源使用许可【全屏页面】★
                //   跟设置页同一套进场动画（右侧滑入），层级也相同，
                //   返回时回到设置页而不是直接退出。
                //
                AnimatedVisibility(
                    visible = showOssLicense,
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(durationMillis = 300, easing = CubicBezierEasing(0.3f, 0.85f, 0.2f, 1f))
                    ) + fadeIn(tween(200)),
                    exit = slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(durationMillis = 260, easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f))
                    ) + fadeOut(tween(180))
                ) {
                    OssLicenseScreen(onClose = { showOssLicense = false; showAbout = true })
                }

                AnimatedVisibility(
                    visible = showReward,
                    enter = fadeIn(tween(durationMillis = 220)),
                    exit = fadeOut(tween(durationMillis = 180))
                ) {
                    RewardDialog(onClose = { showReward = false })
                }

                // ★点空白处关闭菜单★
                // 和闪光灯面板同一套做法：先铺一层全屏半透明层，点它任意位置就收起菜单；
                // 菜单本体画在它之上，所以点菜单项不会被这层吃掉。
                AnimatedVisibility(
                    visible = menuPath != null,
                    modifier = Modifier.fillMaxSize(),
                    enter = fadeIn(tween(140)),
                    exit = fadeOut(tween(120))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { menuPath = null }
                    )
                }

                // ── 多级菜单 ──
                //
                // ★横屏（UI 锁竖屏、界面侧躺）菜单定位★
                //
                // 【为什么之前会超出屏幕】
                // Modifier.rotate 只是绘制变换，**不占布局空间**。菜单 Box 布局尺寸是 w×h
                // （如 220×260），转 90° 后视觉上占 h×w = 260×220，但 align 仍按 w×h 那个盒子摆角 ——
                // 转出来的那条长边就溢出窗口边界了。这才是"太靠左 / 超出屏幕"的真正原因，
                // 跟选哪个角落无关。
                //
                // 【修法（纯几何，不靠猜测）】
                // 把菜单放进一个【正方形安全区】里居中旋转：正方形绕中心转 90° 还是它自己，
                // 所以旋转后的视觉内容一定完整落在正方形内；再把正方形 align 到某个角，
                // 就绝不会溢出屏幕。正方形边长取"菜单最大可能长边"，并按屏幕短边夹一下。
                val menuQuarterTurn: Boolean =
                    abs(shortestLabelRotation % 180f).let { it > 45f && it < 135f }
                // 屏幕短边：旋转后受约束的永远是较短的那一维
                val menuShortSide: Float =
                    minOf(configuration.screenWidthDp, configuration.screenHeightDp).toFloat()
                // 安全区边长：≥ 菜单最大长边，同时不超屏幕短边
                val menuSafeBox: Dp = MENU_SAFE_BOX_DP
                    .coerceAtMost(menuShortSide - MENU_SIDE_PAD_DP * 2)
                    .coerceAtLeast(MENU_MIN_HEIGHT_DP)
                    .dp
                // 菜单内容高度：安全区扣掉标题行和上下留白后剩下的空间
                val menuMaxHeight: Dp = (MENU_SAFE_BOX_DP - 46f)
                    .coerceAtMost(menuShortSide - 46f - MENU_SIDE_PAD_DP * 2)
                    .coerceAtMost(MENU_MAX_HEIGHT_DP)
                    .coerceAtLeast(MENU_MIN_HEIGHT_DP)
                    .dp
                // 菜单锚点：竖屏恒右上；横屏由"想要的视觉角落"反推出 Compose 锚点
                val menuAlign: Alignment = if (!menuQuarterTurn) {
                    Alignment.TopEnd
                } else {
                    composeAnchorForVisual(MENU_VISUAL_CORNER)
                }
                /**
                 * 横屏菜单微调：把"视觉上的右上"换算成 Compose 偏移。
                 * 映射 R: (u,v)→(v,-u)，视觉位移 (Vx,Vy) = (dy, -dx)。反解：
                 *   视觉向右(Vx>0) ⇔ Compose 向下 → verticalBias = +Vx
                 *   视觉向上(Vy<0) ⇔ Compose 向右 → horizontalBias = -Vy
                 * 两个方向余量必须分开算（旋转后宽高互换）：
                 *   Compose 水平余量 (S-菜单宽)/2 → 决定视觉上下；垂直余量 (S-菜单高)/2 → 决定视觉左右。
                 * 之前统一用 30dp，垂直余量实际只有 0dp，"向右"被夹成不动。
                 * 用 BiasAlignment 而非 offset：bias 只在安全区内部挪，不会被推出屏幕。
                 */
                val menuSlackH: Float =
                    ((menuSafeBox.value - MENU_PANEL_MAX_W_DP) / 2f).coerceAtLeast(1f)
                val menuSlackV: Float =
                    ((menuSafeBox.value - MENU_PANEL_MAX_H_DP) / 2f).coerceAtLeast(1f)
                val menuNudge: Alignment = BiasAlignment(
                    horizontalBias = (MENU_NUDGE_UP_DP / menuSlackH).coerceIn(-1f, 1f),
                    verticalBias = (MENU_NUDGE_RIGHT_DP / menuSlackV).coerceIn(-1f, 1f)
                )

                AnimatedVisibility(
                    visible = menuPath != null,
                    modifier = Modifier
                        .then(
                            // 横屏：套一个正方形安全区，杜绝旋转溢出（竖屏不需要）
                            if (menuQuarterTurn) Modifier
                                .align(menuAlign)
                                .size(menuSafeBox)
                            else Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(
                                    top = MENU_TOP_PAD_PORTRAIT_DP.dp,
                                    end = MENU_SIDE_PAD_DP.dp
                                )
                        ),
                    enter = fadeIn(tween(140)) + scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        initialScale = 0.9f
                    ),
                    exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.92f)
                ) {
                    if (menuQuarterTurn) {
                        // 安全区内放置并整体旋转：正方形转 90° 还是正方形，内容不会溢出。
                        // 用 menuNudge 而不是 Center：让菜单从角落往屏幕内侧挪一点，不贴边。
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = menuNudge
                        ) {
                            MultiLevelMenu(
                                path = menuPath ?: "",
                                onPathChange = { menuPath = it },
                                st = settingsBarState,
                                act = settingsBarActions,
                                overflow = buildChipSpecs(settingsBarState, settingsBarActions)
                                    .filter { it.id in hiddenChipIds },
                                // 整体转过来，而不是逐条旋转文字（逐条转会重叠，见 MenuItem 里的说明）
                                rotation = labelRotationAnim,
                                maxContentHeight = menuMaxHeight,
                                guidesOn = showGuides,
                                onGuidesChange = { showGuides = it },
                                uiHideEnabled = uiHideEnabled,
                                onUiHideChange = { uiHideEnabled = it },
                                shutterSoundOn = shutterSound,
                                onShutterSoundChange = { shutterSound = it },
                                swipeModeOn = swipeModeEnabled,
                                onSwipeModeChange = { swipeModeEnabled = it }
                            )
                        }
                    } else {
                        MultiLevelMenu(
                            path = menuPath ?: "",
                            onPathChange = { menuPath = it },
                            st = settingsBarState,
                            act = settingsBarActions,
                            overflow = buildChipSpecs(settingsBarState, settingsBarActions)
                                .filter { it.id in hiddenChipIds },
                            maxContentHeight = menuMaxHeight,
                            guidesOn = showGuides,
                            onGuidesChange = { showGuides = it },
                            uiHideEnabled = uiHideEnabled,
                            onUiHideChange = { uiHideEnabled = it },
                            shutterSoundOn = shutterSound,
                            onShutterSoundChange = { shutterSound = it },
                            swipeModeOn = swipeModeEnabled,
                            onSwipeModeChange = { swipeModeEnabled = it }
                        )
                    }
                }

            } // ← if (!uiHidden) 闭合

            //
            // ── 侧边返回：从左边缘往右滑 = 返回 ──
            //
            // ★★UI 隐藏时不能直接 goBack()★★
            //
            // 这是「隐藏 UI 后侧边一滑就把软件退掉了」的根因：
            //   goBack() 是按"从上层到下层"的顺序关浮层（调色盘→菜单→闪光→…），
            //   【全都关完了】就 finish() 退出 Activity。
            //   而 UI 隐藏时这些浮层本来就不在界面上（它们都在 if (!uiHidden) 里），
            //   于是一滑就一路落到 else 分支 —— 直接退出软件。
            //   用户只是想把 UI 唤回来，却被踢出了 App，体验很差。
            //
            // 修法：UI 隐藏时，侧边滑动 = 【恢复 UI】。
            //   这既修了 bug，也让隐藏状态有了一条明确的"退路"
            //   （不会因为 UI 全没了而困在纯画面里）。
            //
            EdgeSwipeBack(
                modifier = Modifier.align(Alignment.CenterStart),
                onBack = {
                    if (uiHidden) {
                        uiHidden = false
                        Log.d("Camera", "UI 隐藏中：侧边滑动 → 恢复 UI（不退出）")
                    } else {
                        goBack()
                    }
                }
            )
        }
    } // ★CompositionLocalProvider 闭合★
}

// ==================== 专业模式 UI ====================

/**
 * ★专业参数滑条面板★：从 ProControls 里抽出来的公共部分。
 *
 * 两个地方共用它，避免同一套滑条写两份：
 *   · 竖屏：展开在参数按钮行上方（老行为，保持不变）
 *   · 横屏：由 CameraApp 画成"覆盖模式切换栏"的浮层
 *
 * @param compact 紧凑布局：标签 / 数值 / 自动键挪到上一行，滑条独占整行
 */
@Composable
fun ProParamPanel(
    activeParam: ProParam?,
    proIso: Int, onIsoChange: (Int) -> Unit,
    proShutterNs: Long, onShutterChange: (Long) -> Unit,
    proEv: Int, onEvChange: (Int) -> Unit,
    proFocus: Float, onFocusChange: (Float) -> Unit,
    proWb: Int, onWbChange: (Int) -> Unit,
    compact: Boolean = false,
    /** ★竖排滑条★：横屏专业模式（面板竖着替换模式栏）时传 true */
    verticalTrack: Boolean = false
) {
    Column {
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
                    onResetAuto = { onEvChange(0) },
                    compact = compact,
                    verticalTrack = verticalTrack
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
                    onResetAuto = { onShutterChange(0L) },
                    compact = compact,
                    verticalTrack = verticalTrack
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
                    onResetAuto = { onIsoChange(0) },
                    compact = compact,
                    verticalTrack = verticalTrack
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
                    onResetAuto = { onWbChange(0) },
                    compact = compact,
                    verticalTrack = verticalTrack
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
                    onResetAuto = { onFocusChange(-1f) },
                    compact = compact,
                    verticalTrack = verticalTrack
                )
            }
            null -> { }
        }
    }
}

@Composable
fun ProControls(
    vertical: Boolean = false,
    proIso: Int, onIsoChange: (Int) -> Unit,
    proShutterNs: Long, onShutterChange: (Long) -> Unit,
    proEv: Int, onEvChange: (Int) -> Unit,
    proFocus: Float, onFocusChange: (Float) -> Unit,
    proWb: Int, onWbChange: (Int) -> Unit,
    onReset: () -> Unit,
    /**
     * ★当前展开的参数★
     * 横屏时滑条面板要做成"覆盖模式切换栏"的浮层（由 CameraApp 绘制），
     * 所以这个状态提升到外面持有，ProControls 只负责按钮列本身。
     */
    activeParam: ProParam? = null,
    onActiveParamChange: (ProParam?) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    /**
     * 滑条面板内容。竖屏展开在按钮行上方，横屏由外层浮层使用。
     */
    val panelContent: @Composable () -> Unit = {
        ProParamPanel(
            activeParam = activeParam,
            proIso = proIso, onIsoChange = onIsoChange,
            proShutterNs = proShutterNs, onShutterChange = onShutterChange,
            proEv = proEv, onEvChange = onEvChange,
            proFocus = proFocus, onFocusChange = onFocusChange,
            proWb = proWb, onWbChange = onWbChange,
            compact = vertical,
            verticalTrack = false
        )
    }


    /**
     * 参数按钮组（重置 / EV / S / ISO / WB / F）。
     * 同样抽成 lambda：横屏竖排、竖屏横排共用这一份。
     */
    // 横屏竖排：6 个 56dp 的圈叠起来要 336dp+，在 400dp 出头的横屏高度里会顶出去，
    // 所以竖排时统一缩到 46dp。
    val btnSize: Dp = if (vertical) 46.dp else 56.dp
    val proButtons: @Composable () -> Unit = {
        ProCircleButton(
            value = "↻",
            label = "",
            isActive = false,
            isReset = true,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onActiveParamChange(null)
                onReset()
            },
            size = btnSize
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
                onActiveParamChange(
                    if (activeParam == ProParam.EV) null else ProParam.EV
                )
            },
            size = btnSize
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
                onActiveParamChange(
                    if (activeParam == ProParam.SHUTTER) null else ProParam.SHUTTER
                )
            },
            size = btnSize
        )

        ProCircleButton(
            value = if (proIso == 0) "AUTO" else "$proIso",
            label = "ISO",
            isActive = activeParam == ProParam.ISO,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onActiveParamChange(
                    if (activeParam == ProParam.ISO) null else ProParam.ISO
                )
            },
            size = btnSize
        )

        ProCircleButton(
            value = wbShortLabel(proWb),
            label = "WB",
            isActive = activeParam == ProParam.WB,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onActiveParamChange(
                    if (activeParam == ProParam.WB) null else ProParam.WB
                )
            },
            size = btnSize
        )

        ProCircleButton(
            value = if (proFocus < 0f) "AF" else "%.1f".format(proFocus),
            label = "F",
            isActive = activeParam == ProParam.FOCUS,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onActiveParamChange(
                    if (activeParam == ProParam.FOCUS) null else ProParam.FOCUS
                )
            },
            size = btnSize
        )
    }

    if (vertical) {
        // ★横屏：这里只放按钮列，不再向左展开面板★
        // 面板改由 CameraApp 画成"覆盖模式切换栏"的浮层，
        // 于是展开参数时一点预览空间都不占，模式栏只是被临时盖住。
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            proButtons()
        }
    } else {
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
                        .border(
                            1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    panelContent()
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                proButtons()
            }
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
    isReset: Boolean = false,
    // 横屏竖排时 6 个 56dp 的圈会顶满甚至溢出屏幕高度，缩到 46dp
    size: Dp = 56.dp
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

    // ★交互动画★：按下缩到 0.88、选中时略微放大到 1.06，抬手回弹
    val src = remember { MutableInteractionSource() }
    val bgAnimated = rememberPressColor(
        interactionSource = src,
        normal = bgColor,
        pressedColor = if (isActive) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.28f)
    )

    Box(
        modifier = Modifier
            .bouncyPress(src, pressed = 0.88f, selected = isActive, selectedScale = 1.06f)
            .size(size)
            .clip(CircleShape)
            .background(bgAnimated)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = src,
                indication = null
            ) { onClick() },
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
                maxLines = 1,
                // ★只旋转文字★
                modifier = Modifier.rotate(LocalLabelRotation.current)
            )
            if (label.isNotEmpty()) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = label,
                    color = if (isActive) Color.Black.copy(alpha = 0.7f)
                    else Color.White.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    // ★只旋转文字★
                    modifier = Modifier.rotate(LocalLabelRotation.current)
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
    onResetAuto: () -> Unit,
    /**
     * ★紧凑布局★
     * 横屏时专业面板只有 210.dp 宽，"标签 + 滑条 + 数值 + 自动键"挤在一行，
     * 滑条只剩几 dp，手指根本拖不动 —— 这就是"横屏专业模式调不了参数"的根因。
     * 紧凑模式下把标签 / 数值 / 自动键挪到上一行，滑条独占一整行。
     */
    compact: Boolean = false,
    /**
     * ★竖排滑条★：轨道竖着画（上大下小 / 上慢下快看具体参数），手指上下拖。
     * 横屏专业模式里，参数面板是竖着替换掉模式栏的那一列 —— 那一列窄而高，
     * 横着画滑条只有几十 dp 根本拖不准，竖着画才有足够行程。
     */
    verticalTrack: Boolean = false
) {
    val haptic = LocalHapticFeedback.current

    /**
     * 滑条轨道。
     * @param vertical true = 竖着画（上 = range.endInclusive，下 = range.start），手指上下拖
     */
    val track: @Composable (Modifier, Boolean) -> Unit = { mod, vertical ->
        Box(
            modifier = mod
                .pointerInput(range) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val frac: Float = if (vertical) {
                                // 竖排：顶部是最大值，所以要反过来
                                1f - (offset.y / size.height).coerceIn(0f, 1f)
                            } else {
                                (offset.x / size.width).coerceIn(0f, 1f)
                            }
                            val v = range.start + frac * (range.endInclusive - range.start)
                            onValueChange(v)
                        }
                    ) { change, _ ->
                        change.consume()
                        val frac: Float = if (vertical) {
                            1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        } else {
                            (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                        val v = range.start + frac * (range.endInclusive - range.start)
                        onValueChange(v)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val h = size.height
                val w = size.width
                val frac = if (range.endInclusive > range.start) {
                    ((value - range.start) /
                            (range.endInclusive - range.start)).coerceIn(0f, 1f)
                } else 0f
                // 自动状态时用灰色轨道提示
                val activeColor = if (isAuto) Color.White.copy(alpha = 0.3f)
                else Color(0xFFFFC107)
                val thumbOuter = if (isAuto) Color.White.copy(alpha = 0.5f)
                else Color(0xFFFFC107)

                if (vertical) {
                    val x = w / 2f
                    // 底轨
                    drawLine(
                        color = Color.White.copy(alpha = 0.18f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // 刻度
                    for (i in 0..10) {
                        val yy = h * i / 10f
                        drawLine(
                            color = Color.White.copy(alpha = 0.3f),
                            start = Offset(x - 4.dp.toPx(), yy),
                            end = Offset(x + 4.dp.toPx(), yy),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    // 已选段：从底部往上画到 thumb
                    val cy = h * (1f - frac)
                    drawLine(
                        color = activeColor,
                        start = Offset(x, h),
                        end = Offset(x, cy),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = thumbOuter,
                        radius = 8.dp.toPx(),
                        center = Offset(x, cy)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(x, cy)
                    )
                } else {
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
                    drawLine(
                        color = activeColor,
                        start = Offset(0f, y),
                        end = Offset(w * frac, y),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = thumbOuter,
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
        }
    }

    // 自动按钮（🎯 自动）
    val autoBtn: @Composable () -> Unit = {
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
                fontWeight = FontWeight.Bold,
                // ★只旋转文字★
                modifier = Modifier.rotate(LocalLabelRotation.current)
            )
        }
    }

    if (verticalTrack) {
        // ★竖排★：标签 + 自动键一行 → 数值 → 竖着的轨道（给足上下拖动行程）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    // ★只旋转文字★
                    modifier = Modifier.rotate(LocalLabelRotation.current)
                )
                Spacer(modifier = Modifier.weight(1f))
                autoBtn()
            }
            Text(
                displayValue,
                color = if (isAuto) Color.White.copy(alpha = 0.6f) else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // ★只旋转文字★
                modifier = Modifier.rotate(LocalLabelRotation.current)
            )
            Spacer(modifier = Modifier.height(2.dp))
            track(
                Modifier
                    .fillMaxWidth()
                    .height(PRO_SLIDER_VERTICAL_TRACK_H),
                true
            )
        }
    } else if (compact) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    // ★只旋转文字★
                    modifier = Modifier.rotate(LocalLabelRotation.current)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    displayValue,
                    color = if (isAuto) Color.White.copy(alpha = 0.6f) else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    // ★只旋转文字★
                    modifier = Modifier.rotate(LocalLabelRotation.current)
                )
                Spacer(modifier = Modifier.width(6.dp))
                autoBtn()
            }
            track(
                Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                false
            )
        }
    } else {
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
                // ★只旋转文字★
                modifier = Modifier
                    .width(34.dp)
                    .rotate(LocalLabelRotation.current)
            )
            track(
                Modifier
                    .weight(1f)
                    .height(32.dp),
                false
            )
            Text(
                displayValue,
                color = if (isAuto) Color.White.copy(alpha = 0.6f) else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .width(52.dp)
                    // ★只旋转文字★
                    .rotate(LocalLabelRotation.current),
                textAlign = TextAlign.End
            )
            Spacer(modifier = Modifier.width(8.dp))
            autoBtn()
        }
    }
}

// ==================== 关于页 ====================

@Composable
fun AboutScreen(
    onClose: () -> Unit,
    onShowReward: () -> Unit,
    onOpenDeviceTuning: () -> Unit = {},
    onOpenOssLicense: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    // 排查"最新版还弹更新"时看这一行：它必须 >= update.json 里的 versionCode
    val context = LocalContext.current
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
                Text("设置", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(APP_DISPLAY_NAME, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
                    // 更新判断用的是这个整数（来自 build.gradle），不是上面那个字符串。
                    // 它小于 update.json 的 versionCode 就会一直弹更新 —— 发版务必同步递增
                    AboutRow(
                        label = "版本码",
                        value = AppUpdate.currentVersionCode(context).toString()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenDeviceTuning()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Column {
                            Text(
                                text = "机型与色彩",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "重新选择品牌 / 系列、开关统一镜头色彩、校准当前镜头",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 11.sp
                            )
                        }
                    }
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

                    // ── 开源使用声明：放在设置卡片【最后】──
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.10f))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OssLicenseEntry(onOpen = onOpenOssLicense)
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

// ==================== 开源使用声明 ====================

/**
 * 一条开源组件信息。
 * @param url 点进去的官网 / 许可证地址
 */
private data class OssItem(
    val name: String,
    val license: String,
    /** 一句白话说明这个许可证大概允许什么，方便非法律专业的人看懂 */
    val summary: String,
    val url: String
)

//
// ★本项目自身的许可证★
//
// ⚠️ 这里的文字是【法律声明】，不是装饰，请务必确认后再发版。
//
// 之前的"个人非商业作品，仅供学习交流，禁止二次分发"是【源码可得】
// （source-available）而不是【开源】—— 真正的开源许可证一定允许二次分发。
// 一旦你把仓库设为公开并挂上许可证，就等于授权他人按该许可证的条款
// 使用、修改、再分发你的代码，那句话必须删。
//
// ★本项目实际用的是【MIT】★（与 GitHub 仓库 LICENSE 保持一致）
//   仓库：https://github.com/hmjs2001/silky-camera-update
//
// ⚠️ 界面上的许可证名必须跟仓库根目录的 LICENSE 完全一致 ——
//    两处不一致会让使用者无所适从，也是最常见的开源合规问题。
//
// 顺带说明本项目为什么是 MIT 而第三方组件多是 Apache-2.0：
//   这是【两个不同的许可证】，各自独立生效，互不覆盖 ——
//   · 本项目代码（你自己写的）  → MIT
//   · 依赖的第三方库           → 各自的 Apache-2.0
//   再分发时两边的要求都要满足（MIT 要求留版权声明，
//   Apache-2.0 要求留声明 + NOTICE + 说明改动）。
//
// MIT 与 Apache-2.0 的主要差异（选 MIT 要知道的）：
//   · MIT 最简短，只有"保留版权声明"一条实质义务
//   · MIT 【不含】专利授权条款（Apache-2.0 有）
//   · MIT 允许他人改完拿去【闭源】商用
//
private const val OSS_PROJECT_NAME = "Silky Camera"
private const val OSS_PROJECT_LICENSE = "MIT"
/** 与仓库 LICENSE 文件里的 Copyright 行逐字一致 */
private const val OSS_PROJECT_COPYRIGHT = "Copyright (c) 2026 小码-初中 (hmjs)"
/** 点「查看许可证全文」跳仓库里的 LICENSE 文件 */
private const val OSS_PROJECT_URL =
    "https://github.com/hmjs2001/silky-camera-update/blob/main/LICENSE"

/** 本项目许可证的要点白话说明（界面上展示用，不是法律文本） */
private val OSS_PROJECT_LICENSE_POINTS = listOf(
    "允许自由使用、复制、修改、合并、出版、分发、再许可和出售副本",
    "允许用于商业目的，无需支付授权费，也无需开源你的衍生作品",
    "唯一实质义务：再分发时保留本版权声明与许可证全文",
    "不提供任何担保，作者不对使用后果承担责任"
)

/**
 * 本项目用到的第三方开源组件。
 * 许可证名称按各项目官方 LICENSE 如实填写。
 */
private val OSS_ITEMS = listOf(
    OssItem(
        "Jetpack Compose", "Apache-2.0", "可商用，保留声明即可",
        "https://github.com/androidx/androidx"
    ),
    OssItem(
        "CameraX (androidx.camera)", "Apache-2.0", "可商用，保留声明即可",
        "https://source.android.com/docs/core/camera/camerax"
    ),
    OssItem(
        "Kotlin", "Apache-2.0", "可商用，保留声明即可",
        "https://github.com/JetBrains/kotlin"
    ),
    OssItem(
        "Android Gradle Plugin", "Apache-2.0", "可商用，保留声明即可",
        "https://developer.android.com/build"
    ),
    OssItem(
        "Material Components / Icons", "Apache-2.0", "可商用，保留声明即可",
        "https://github.com/material-components/material-components-android"
    ),
    OssItem(
        "AndroidLiquidGlass (Kyant0)", "Apache-2.0", "可商用，保留声明即可",
        "https://github.com/Kyant0/AndroidLiquidGlass"
    )
)

//
// ── 设置页里的入口行 ──
// 只负责"点一下打开全屏页面"，内容全部交给 OssLicenseScreen。
//
@Composable
fun OssLicenseEntry(onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable { onOpen() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "开源使用许可",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "本项目及第三方组件的许可证与出处",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp
            )
        }
        Text(text = "›", color = Color.White.copy(alpha = 0.5f), fontSize = 18.sp)
    }
}

/**
 * ★开源使用许可【全屏页面】★
 *
 * 之前是折叠在设置卡片里的一小块，十几行挤在 190dp 的滚动框内很难读，
 * 所以改成独立整屏：顶部返回栏 + 可滚动正文，跟设置页一个层级。
 *
 * ⚠️ 打开链接用 LocalContext + Intent.ACTION_VIEW，不用 LocalUriHandler：
 *    LocalUriHandler 在不同 Compose 版本里包名/可用性不一致。
 *    打开失败给一句 Toast，而不是静默无反应。
 */
@Composable
fun OssLicenseScreen(onClose: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val openUrl: (String) -> Unit = { url ->
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            )
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                context, "无法打开链接", android.widget.Toast.LENGTH_SHORT
            ).show()
            Log.w("Camera", "打开开源链接失败: $url", e)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        // ── 顶部返回栏 ──
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
            Text(
                "开源使用许可",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ── 正文：整屏滚动 ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // ① 本项目
            //
            // ⚠️ 必须显式标出"本项目"—— 第三方组件多为 Apache-2.0，
            //    不标的话用户分不清哪一行是软件自己的许可证。
            //
            Text(
                text = "本项目",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = OSS_PROJECT_NAME,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = OSS_PROJECT_COPYRIGHT,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFC107).copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = OSS_PROJECT_LICENSE,
                        color = Color(0xFFFFC107),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "查看许可证全文 ↗",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { openUrl(OSS_PROJECT_URL) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "你可以：",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OSS_PROJECT_LICENSE_POINTS.forEach { p ->
                    Row(modifier = Modifier.padding(vertical = 3.dp)) {
                        Text(
                            text = "· ",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = p,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.10f))
            )
            Spacer(modifier = Modifier.height(18.dp))

            // ② 第三方组件
            Text(
                text = "第三方开源组件",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "本软件使用了以下开源项目。这些许可证与本项目（${OSS_PROJECT_LICENSE}）"
                        + "相互独立、各自生效，不因本项目采用 ${OSS_PROJECT_LICENSE} 而改变。",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            OSS_ITEMS.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { openUrl(item.url) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${item.license} · ${item.summary}",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 11.sp
                        )
                    }
                    // 外链图标用文本 ↗：Icons.AutoMirrored.Filled.OpenInNew
                    // 在 material-icons-extended 里，很多工程没引会编译失败
                    Text(
                        text = "↗",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "以上许可证名称按各项目官方 LICENSE 如实填写，完整条款以各自仓库中的 LICENSE 文件为准。",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            //
            // MIT 的唯一实质义务就是"保留版权声明"，这里原样展示它，
            // 既是合规要求，也让使用者知道署名该怎么写。
            //
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(14.dp)
            ) {
                Text(
                    text = "本项目版权声明（${OSS_PROJECT_LICENSE} 要求保留）",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = OSS_PROJECT_COPYRIGHT,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
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
    //
    // ⚠️ 原来这里的 .background(Color.White) + .border(...) 和"录像红点"都删掉了 ——
    //    白色圆环与内芯现在由 ic_shutter_photo / ic_shutter_video 两个矢量图标提供，
    //    环与芯之间那圈透明间隙是刻意的（跟 iOS 快门一致，透出一点预览画面）。
    //    想改粗细 / 颜色直接改 res/drawable 里那两个 XML，不用动这里。
    //
    Box(
        modifier = Modifier
            .size(outer)
            .clip(CircleShape)
            .clickable { if (isVideoMode) onVideoClick() else onPhotoClick() },
        contentAlignment = Alignment.Center
    ) {
        // ★换成矢量图标★
        // 拍照键 = ic_shutter_photo（白环 + 白心）
        // 录像键 = ic_shutter_video（白环 + 红心）
        // ⚠️ 录像键【不要】设 tint：它是白 + 红双色，一 tint 红心就被染成白色了
        Image(
            painter = painterResource(
                if (isVideoMode) R.drawable.ic_shutter_video
                else R.drawable.ic_shutter_photo
            ),
            contentDescription = if (isVideoMode) "开始录像" else "拍照",
            modifier = Modifier.fillMaxSize()
        )
        // 录制中：在红心上面再叠一个圆角方块（停止按钮）
        // 图标只负责"待机"造型，状态变化由代码叠加，避免做多套图标
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
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
    onSelect: (CaptureMode) -> Unit,
    /** 文字旋转角。竖排模式下外层已整列旋转，这里要传 0f，否则会转两次 */
    textRotation: Float = LocalLabelRotation.current
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

    // ★交互动画★：按下缩小，配合已有的下划线宽度 / 文字颜色过渡
    val src = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .bouncyPress(src, pressed = 0.90f)
            .clickable(
                interactionSource = src,
                indication = null
            ) {
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
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                // ★只旋转文字★（竖排时外层整列已转，这里收到 0f 不再转）
                modifier = Modifier.rotate(textRotation)
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
        // ★竖排时整列旋转★：跟菜单同一个道理 —— 竖排的一列里逐条转文字会互相重叠。
        Column(
            modifier = Modifier.rotate(LocalLabelRotation.current),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CaptureMode.values().forEach { m ->
                ModeItem(
                    m = m, selected = m == current, vertical = true,
                    onSelect = onSelect,
                    // 整列已经转过了，这里不能再转一次
                    textRotation = 0f
                )
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

/**
 * 用系统相册打开本次拍的某张照片 / 某段视频。
 * 带上 FLAG_GRANT_READ_URI_PERMISSION，第三方相册才有权限读这个 Uri。
 */
fun openInSystemGallery(context: Context, media: CapturedMedia) {
    try {
        val mime = if (media.isVideo) "video/*" else "image/*"
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(media.uri, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(Intent.createChooser(intent, "用系统相册打开"))
    } catch (e: Exception) {
        Log.w("Camera", "跳转系统相册失败", e)
    }
}

@Composable
fun SessionGallery(media: List<CapturedMedia>, onClose: () -> Unit) {
    val context = LocalContext.current
    //
    // ★haptic 必须在【顶层 Composable 里】单独取★
    //   SessionGallery 是独立函数，不在 CameraApp() 作用域内，
    //   拿不到 CameraApp 里那个 haptic（每个 @Composable 各有自己的 CompositionLocal）。
    //
    val haptic = LocalHapticFeedback.current
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
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClose()
                        },
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
                                // ★点一下就跳到系统相册里对应的那张照片 / 那段视频★
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    openInSystemGallery(context, item)
                                }
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
    //
    // ★key 里加上 media.version★
    //   只写 media.uri 的话，后处理写回同一个 uri 不会触发重载，
    //   缩略图会一直停留在"处理前"的画面。
    //
    val bitmap by produceState<ImageBitmap?>(null, media.uri, media.version) {
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
    onCommit: ((Float) -> Unit)? = null,
    // ★轮盘贴图★：盘面底纹 + 内圈，关掉就是最朴素的单线刻度
    showDialTexture: Boolean = true
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

        // ★轮盘贴图★：盘面底纹（径向渐变）+ 内圈细线，做出原厂相机刻度盘的质感
        if (showDialTexture) {
            val dialW = 26.dp.toPx()
            drawArc(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(cX, cY),
                    radius = R
                ),
                startAngle = 270f - thetaDeg,
                sweepAngle = thetaDeg * 2f,
                useCenter = false,
                topLeft = Offset(cX - R, cY - R),
                size = GeoSize(R * 2f, R * 2f),
                style = Stroke(width = dialW, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = 270f - thetaDeg,
                sweepAngle = thetaDeg * 2f,
                useCenter = false,
                topLeft = Offset(cX - R + dialW, cY - R + dialW),
                size = GeoSize((R - dialW) * 2f, (R - dialW) * 2f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

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
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            // ★只旋转文字★
            modifier = Modifier.rotate(LocalLabelRotation.current)
        )
    }
}

// ==================== 侧边返回 / 多级菜单 / 调色盘 ====================

/**
 * ★侧边返回★：屏幕左边缘一条 22dp 的竖条，从左往右滑过阈值即触发返回。
 *
 * 只占边缘一条，中间的取景框照样可以双指拖动变焦，不会互相抢手势。
 * 返回的层级由调用方的 onBack 决定（这里是 [CameraApp] 里的 goBack）。
 */
@Composable
fun EdgeSwipeBack(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    threshold: Dp = 56.dp,
    onBack: () -> Unit
) {
    if (!enabled) return
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(22.dp)
            .pointerInput(threshold) {
                var fired = false
                detectHorizontalDragGestures(
                    onDragStart = { fired = false },
                    onDragEnd = { fired = false },
                    onDragCancel = { fired = false }
                ) { change, dragAmount ->
                    change.consume()
                    // 向右滑、且累计位移过阈值才触发；fired 保证一次滑动只返回一次
                    if (!fired && dragAmount > 0f && change.position.x >= threshold.toPx()) {
                        fired = true
                        onBack()
                    }
                }
            }
    )
}

@Composable
private fun MenuItem(
    text: String,
    selected: Boolean = false,
    /** 行尾小字备注（如"此功能为测试功能"），不传就不显示 */
    note: String? = null,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    // ★交互动画★：按下时整行轻微缩小 + 底色泛白，抬手弹回
    val src = remember { MutableInteractionSource() }
    val bg = rememberPressColor(
        interactionSource = src,
        normal = Color.Transparent,
        pressedColor = Color.White.copy(alpha = 0.12f)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyPress(src, pressed = 0.97f)
            .background(bg)
            .clickable(
                interactionSource = src,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            // 纵向内边距 11dp → 8dp：一屏能多放两三行，菜单整体矮一截
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            color = if (selected) Color(0xFFFFC107) else Color.White,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            // ★防溢出★：文字过长时省略，绝不顶出菜单栏（以前会往左顶出去）
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // ⚠️ 这里曾经加过 .rotate(LocalLabelRotation.current)，那是"横屏菜单文字重叠"的根因：
            //    Modifier.rotate 只是【绘制变换】，不改变布局尺寸。菜单项是竖排的一列，
            //    每行的文字各自转 90° 后，视觉高度变成原来的"文字宽度"（约 60~100dp），
            //    而行高只有 40dp 出头 —— 相邻几行直接叠在一起。
            //    正确做法是旋转【整个面板】（见 MultiLevelMenu 的 rotation 参数），
            //    面板是浮层，整体转过去布局盒子也跟着转，不会互相压。
            modifier = Modifier.weight(1f)
        )
        // 小字备注放在标题右边、对勾左边，对勾永远贴最右，各行对齐
        if (note != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                note,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selected) Text("✓", color = Color(0xFFFFC107), fontSize = 13.sp)
    }
}

/**
 * ★多级菜单★：一级是分类（拍摄 / 录像 / 其他），点进去是二级具体项。
 * 顶部 "<" 退回上一级；侧边返回和系统返回键会直接把整个菜单关掉。
 *
 * 所有可选项跟设置栏上的芯片是同一份状态（[SettingsBarState] / [SettingsBarActions]），
 * 所以在菜单里改和在上面点效果完全一样，不会出现两边不同步。
 */
@Composable
private fun MultiLevelMenu(
    path: String,
    onPathChange: (String?) -> Unit,
    st: SettingsBarState,
    act: SettingsBarActions,
    modifier: Modifier = Modifier,
    /**
     * ★装不下的状态栏按键★：它们不再画成 chip，改在这里出现。
     * 每个按键自带 [ChipSpec.menuEntries]，所以点这里和点 chip 效果完全一样。
     */
    overflow: List<ChipSpec> = emptyList(),
    /**
     * ★整个面板的旋转角★
     * 界面没跟着屏幕转（UI 锁定竖屏）时，把整块菜单转过来，
     * 这样文字是正的、菜单项是竖排的，跟平常一样能点。
     */
    rotation: Float = 0f,
    /**
     * ★内容区最大高度★：由外层按屏幕短边算好传进来，
     * 菜单不再自己写死一个值，屏幕矮的机器也不会把菜单顶出屏幕。
     */
    maxContentHeight: Dp = MENU_MAX_HEIGHT_DP.dp,
    /** 构图辅助线（三分线 + 水平仪）当前是否显示 */
    guidesOn: Boolean = true,
    /** 切换构图辅助线 */
    onGuidesChange: (Boolean) -> Unit = {},
    /** ★UI隐藏（双击空白处隐藏全部 UI）开关★ 当前是否打开 */
    uiHideEnabled: Boolean = false,
    /** 切换 UI隐藏 开关 */
    onUiHideChange: (Boolean) -> Unit = {},
    /** ★快门声★ 当前是否打开 */
    shutterSoundOn: Boolean = true,
    /** 切换快门声 */
    onShutterSoundChange: (Boolean) -> Unit = {},
    /** ★左右滑动切换模式★ 当前是否打开 */
    swipeModeOn: Boolean = true,
    /** 切换 左右滑动切换模式 */
    onSwipeModeChange: (Boolean) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            // ★整体旋转★：必须放在最外层，让背景 / 边框 / 内容一起转。
            // 位置不需要补偿 —— 外层横屏时按 CenterEnd 摆放，旋转绕盒子中心，
            // 居中时旋转前后中心不动，所以一定完整落在屏幕内。
            .rotate(rotation)
            // ★宽度自适应★：溢出项的文字可能比分类名长，写死 190dp 会顶出去。
            // 给一个下限 + 上限，让 Box 按内容取宽度，同时不无限变宽。
            // 上限收到 220dp：旋转 90° 后"高度"会变成"宽度"，太宽会顶出屏幕。
            .widthIn(min = 190.dp, max = 220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.88f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                // 溢出项多的时候可以上下滑，不至于顶出屏幕。
                // 上限由外层按屏幕高度算好（MENU_MAX_HEIGHT_DP / FRAC 取小），
                // 旋转 90° 后它会变成面板的"宽度"，太大顶出屏幕。
                .heightIn(max = maxContentHeight)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (path.isEmpty()) onPathChange(null) else onPathChange("")
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("<", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    if (path.isEmpty()) "设置" else path,
                    color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            )

            // 菜单只放【设置栏上没有入口】的项：
            // 录像的画质/帧率/防抖、拍照的高像素/比例都能在设置栏直接改，
            // 装不下时 chip 自动进 overflow，功能不缺。
            val isVideoMode: Boolean = st.mode == CaptureMode.VIDEO
            // barHasHighRes 已删：高像素有 chip，菜单不再重复列，判据无用
            val barHasFlash: Boolean = true
            val barHasTone: Boolean = true

            // 分类里一项都不剩时，一级菜单就不列这个分类（避免点进去是空页面）
            val showPhotoGroup: Boolean = !isVideoMode
            // "录像设置"一级已删除：画质/帧率/防抖在设置栏能直接改
            // "其他"恒显示：原写法 !barHasFlash||!barHasTone 恒 false，
            // 这一级从来没出现过，构图辅助线开关等于点不到
            val showOtherGroup: Boolean = true

            // ★二级切换动画★：进子页时新页从右侧滑入，退回时从左侧滑回。
            //
            // ⚠️ 之前是按"层级"算一个固定位移，结果退回一级后内容停在 -26dp 不动，
            //    文字直接顶出菜单面板左侧 —— 就是这个 bug。
            //    现在改成"每次切页先 snapTo 到起点，再 animateTo(0f)"，
            //    动画结束一定回到 0，绝不会停在偏移位置。
            val slideX = remember { Animatable(0f) }
            LaunchedEffect(path) {
                slideX.snapTo(if (path.isNotEmpty()) 28f else -28f)
                slideX.animateTo(0f, tween(durationMillis = 220))
            }
            Crossfade(
                targetState = path,
                animationSpec = tween(durationMillis = 200)
            ) { cur ->
                Column(modifier = Modifier.offset(x = slideX.value.dp)) {
                    when (cur) {
                        "" -> {
                            if (showPhotoGroup) MenuItem("拍摄设置") { onPathChange("拍摄设置") }
                            if (showOtherGroup) MenuItem("其他") { onPathChange("其他") }
                            // ★装不下的状态栏按键，在这里补齐★
                            // 点这里和点 chip 是同一份动作，功能一个都不会少。
                            overflow.forEach { spec ->
                                spec.menuEntries.forEach { e ->
                                    MenuItem(e.text, selected = e.selected) {
                                        e.action()
                                        // 打开面板类的（调色盘 / 闪光灯）点完就关菜单，
                                        // 让面板盖上来；切换类的保留菜单，方便连着调
                                        if (spec.id == CHIP_TONE || spec.id == CHIP_FLASH) {
                                            onPathChange(null)
                                        }
                                    }
                                }
                            }
                            // ★关于放在这里★：设置栏上已无常驻的"?"按钮
                            MenuItem("设置") {
                                act.onAbout()
                                onPathChange(null)
                            }
                        }
                        "拍摄设置" -> {
                            // 与录像模式同规则：设置栏能直接改的（高像素 CHIP_HIGH_RES、
                            // 各比例 chip）不再重复列；下面几项设置栏上没有入口，必须留。
                            // 不支持 RAW 的机器点了会弹 Toast，这里不额外占位。
                            MenuItem(
                                if (st.rawEnabled) "格式：RAW" else "格式：JPEG",
                                selected = st.rawEnabled,
                                note = "此功能为测试功能"
                            ) { act.onRawToggle() }
                            // 强制 RAW：只在检测为"不支持"时出现，给误判的机器留逃生门。
                            // 真不行会自动退回 JPEG，不会崩。
                            if (st.rawSupported == false) {
                                MenuItem(
                                    if (st.rawForce) "强制 RAW：开" else "强制 RAW：关",
                                    selected = st.rawForce
                                ) { act.onRawForceToggle() }
                            }
                            // 开 = 只保存 DNG（小米默认，路径最稳）；关 = DNG + JPEG
                            MenuItem(
                                if (st.rawSingleOutput) "RAW 输出：仅DNG" else "RAW 输出：DNG+JPEG",
                                selected = st.rawSingleOutput
                            ) { act.onRawSingleToggle() }
                        }
                        "其他" -> {
                            // ★「自动旋转」菜单项已随旋转按键一起移除★
                            // ★构图辅助线★：三分线 + 水平仪的总开关
                            MenuItem(
                                if (guidesOn) "构图辅助线：开" else "构图辅助线：关",
                                selected = guidesOn
                            ) { onGuidesChange(!guidesOn) }
                            //
                            // ★UI隐藏★
                            //   开：拍摄界面【双击空白处】隐藏所有 UI，再双击恢复。
                            //   关：双击空白处不生效（默认关，避免误触）。
                            //
                            // ⚠️ 这里只是【开关】，"当前 UI 藏没藏"是另一个瞬时状态，
                            //    由双击切换，不进菜单也不持久化。
                            //
                            MenuItem(
                                if (uiHideEnabled) "UI隐藏：开" else "UI隐藏：关",
                                selected = uiHideEnabled
                            ) { onUiHideChange(!uiHideEnabled) }
                            //
                            // ★快门声★
                            //   开：拍照时播放系统快门音（跟原厂相机同一个音）
                            //   关：完全静音
                            //   ⚠️ 部分 ROM 按法规强制快门声，那种机器上关了也还是会响 ——
                            //      这是系统行为，App 层面改不了。
                            //
                            MenuItem(
                                if (shutterSoundOn) "快门声：开" else "快门声：关",
                                selected = shutterSoundOn
                            ) { onShutterSoundChange(!shutterSoundOn) }
                            //
                            // ★左右滑动切换模式★
                            //   开：在取景画面上左右滑动即可切换 拍照 / 录像 / 专业 / 星空
                            //   关：滑动不生效（想要避免误触就关掉）
                            //
                            MenuItem(
                                if (swipeModeOn) "滑动切模式：开" else "滑动切模式：关",
                                selected = swipeModeOn
                            ) { onSwipeModeChange(!swipeModeOn) }
                            if (!barHasFlash) {
                                MenuItem("闪光灯") {
                                    act.onFlashExpand()
                                    onPathChange(null)
                                }
                            }
                            if (!barHasTone) {
                                MenuItem("调色盘") {
                                    act.onColorPalette()
                                    onPathChange(null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ★苹果风格调色盘★
 * 横轴 = 色温（左冷右暖），纵轴 = 色调（上绿下洋红），下面一条强度滑条。
 *
 * 取景时预览会套上近似的色调（[CameraApp] 里的 toneOverlayColor），
 * 成片由拍照后处理（postProcessPhoto）套用同一组增益，所以所见即所得。
 */
/**
 * ★苹果柔光玻璃材质★
 *
 * 三层叠加模拟 iOS 的 materialThin / ultraThin 观感：
 *   ① 底层：半透明黑 —— 提供对比度，保证在任何画面上都看得清
 *   ② 中层：白色竖向渐变（上亮下暗）—— 模拟玻璃的厚度与透光
 *   ③ 面层：顶部 1dp 高光 + 内圈柔光 —— 苹果玻璃最标志性的那道亮边
 *
 * ⚠️ 刻意不用 Modifier.blur()：那是 Android 12(API 31) 才真正生效的 RenderEffect，
 *    低版本上是 no-op（还会打警告），而相机预览本身在动，玻璃后面透出画面就有"毛玻璃"感。
 */
/** 苹果柔光玻璃的白色渐变层（纯计算，不需要 Composable 上下文） */
private fun appleGlassBrush(): Brush = Brush.verticalGradient(
    //
    // ★已按"面板底 0.82"重新配平★
    //   原来是 0.20/0.11/0.06/0.09，是配 0.42 浅底用的。
    //   底色加深到 0.82 后，这些白色会把面板又提亮回去、把暗底冲掉，
    //   所以整体压到约原来的一半，只保留玻璃的"厚度感"。
    //
    colors = listOf(
        Color.White.copy(alpha = 0.10f),
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.03f),
        Color.White.copy(alpha = 0.045f)
    )
)

/**
 * ★影调预设★
 *
 * 三个字段跟调色盘的三组参数一一对应，点一下直接套用：
 *   warmth    -100(冷) ~ +100(暖)   色温
 *   tint      -100(绿) ~ +100(洋红) 色调
 *   intensity    0(关) ~ 100(满)    强度
 *
 * 数值是"风格化"的取向值，不是从真机标定出来的精确值 ——
 * 影调本来就是主观的。想微调直接改下面这张表即可，一行一个数。
 */
/**
 * ★调色轴的【最大范围】★：色温 ±150、色调 ±150（原来只有 ±100，不够用）
 *
 * 扩到 ±150 的理由：原来 ±100 配合旧的 0.18/0.12 小系数，
 * 即便拖到底成片也几乎没有明显变化，"调了跟没调一样"。
 * 现在范围更广 + 系数更大（见 [TONE_WARM_GAIN]），
 * 拖到底能拿到真正有风格的影调，同时中间段仍然细腻好控。
 */
private const val TONE_MAX = 150f

//
// ★★增益强度（v3.3.42 回调到折中）★★
//
// 版本演进：
//   v3.3.40  0.30 / 0.20   → 反馈"所有影调色彩不明显"（显著度 Δ15~20）
//   v3.3.41  0.45 / 0.32   → 反馈"是不是过于明显了"（显著度 Δ23~31）
//   v3.3.42  0.38 / 0.27   ← 折中（显著度 Δ19~26）
//
// ⚠️★这是【成片】的色彩浓淡旋钮，跟预览的浓淡是【两个独立的数】★
//   觉得成片太浓 / 太淡 → 改这两个
//   觉得预览太蒙 / 太淡 → 改 [TONE_PREVIEW_ALPHA]
//
/** 色温对 R/B 的增益强度（0.38 = 拖满时 R/B 各 ±38%） */
private const val TONE_WARM_GAIN = 0.38f
/** 色调对 R/B 与 G 的增益强度（0.27 = 拖满时洋红/绿 ±27%） */
private const val TONE_TINT_GAIN = 0.27f

/**
 * 预览叠加的不透明度。
 * 原来 0.16×intensity/100（最大 0.28）太淡，预览只呈现成片效果的约 10%（"色彩不明显"的主因）；
 * 试过 0.45 又反馈"过于明显"（取景框被压掉近一半对比度），现回调到 0.30。
 * ⚠️ 下限约 0.25：再低，强影调反解出的叠加色会超过 1.0 被截断，预览就不再等于成片。
 */
private const val TONE_PREVIEW_ALPHA = 0.30f

/**
 * 预览叠加色：解析求解，让预览严格等于成片。
 * 预览是 alpha 混合 out=(1-a)·v+a·overlay，成片是乘性增益 out=v·gain，两者不同。
 * 令 v=0.5（中性灰）时相等，反解：overlay = (o - 0.5)/a + 0.5（o = 成片对中性灰的输出）。
 * 这样"取景框看到的"就是"照片得到的"。o 会 clamp 到 [0,1]，极端增益下截断。
 */
private fun tonePreviewOverlay(oR: Float, oG: Float, oB: Float, alpha: Float): FloatArray {
    val a = alpha.coerceAtLeast(0.05f)
    fun ov(o: Float) = (((o.coerceIn(0f, 1f) - 0.5f) / a) + 0.5f).coerceIn(0f, 1f)
    return floatArrayOf(ov(oR), ov(oG), ov(oB))
}

/**
 * 影调的唯一真源：由 warmth / tint / intensity 算出 R/G/B 三通道增益。
 * ⚠️ 预览、成片、盘面三处必须共用这一个函数 —— 上一版"5 个影调色彩不对"正是因为
 * 三处各用一套公式（成片 1±0.18w、预览 0.5±0.5w/100、盘面第三套），
 * 导致"预览看着明显、成片几乎没变"。
 * 语义：warmth 正 = 暖（R↑B↓）；tint 正 = 洋红（R↑B↑G↓）、负 = 绿（G↑）；
 * intensity 是整体强度（0 = 不生效，100 = 满强度）。
 */
private fun toneGains(warmth: Float, tint: Float, intensity: Float): FloatArray {
    val k = (intensity / 100f).coerceIn(0f, 1f)
    val w = (warmth / TONE_MAX).coerceIn(-1f, 1f) * k
    val t = (tint / TONE_MAX).coerceIn(-1f, 1f) * k
    val r = ((1f + TONE_WARM_GAIN * w) * (1f + TONE_TINT_GAIN * t)).coerceIn(0f, 4f)
    val g = (1f - TONE_TINT_GAIN * t).coerceIn(0f, 4f)
    val b = ((1f - TONE_WARM_GAIN * w) * (1f + TONE_TINT_GAIN * t)).coerceIn(0f, 4f)
    return floatArrayOf(r, g, b)
}

@Composable
fun AppleColorPalette(
    warmth: Float,
    tint: Float,
    intensity: Float,
    onWarmthChange: (Float) -> Unit,
    onTintChange: (Float) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    /**
     * ★横屏模式★
     * 竖屏：底部弹出、铺满 82% 宽度
     * 横屏：贴右侧一小块（230dp）
     * ⚠️ 两种朝向都【不暗化背景】—— 见下方 Box 的注释。
     */
    landscape: Boolean = false
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        //
        // 关掉 Dialog 自带的系统级 dim：光把下面那层 Box 改成 Transparent 不够 ——
        // Compose 的 Dialog 是独立 Window，默认带 FLAG_DIM_BEHIND，
        // 会按主题的 backgroundDimAmount 把整个屏幕压暗（通常 30%~60%），
        // 这才是"打开调色盘背景就暗了"的主要来源。
        //
        // ⚠️ LocalView.current 必须在【外面】取：DisposableEffect 的 effect lambda
        //    不是 @Composable 上下文，直接在里面写会报
        //    "@Composable invocations can only happen from the context of a @Composable
        //     function"（报错列正好落在 .current 上）。
        //
        val hostView = LocalView.current
        DisposableEffect(hostView) {
            val w = (hostView.parent as? DialogWindowProvider)?.window
            //
            // ★只 clearFlags 在部分 ROM 上不够★
            //   clearFlags(FLAG_DIM_BEHIND) 是标准做法，但索尼 Xperia 等
            //   部分 OEM 的 dim surface 是独立图层，flag 清了它还在，
            //   会继续吃掉触摸（表现为"面板外的相机控件点不动"）。
            //   额外把 dimAmount 设成 0 才是双保险 —— 两个都做。
            //
            try {
                w?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                w?.setDimAmount(0f)
            } catch (e: Exception) {
                Log.w("Camera", "去除调色盘遮罩失败（不影响使用）", e)
            }
            onDispose {
                try {
                    w?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                } catch (e: Exception) { /* 窗口即将销毁，忽略 */ }
            }
        }

        //
        // ★面板高度上限★
        //   横屏时 UI 锁竖屏，可用高度 = 屏幕【短边】（常见 360dp），
        //   加了影调行后面板算下来接近 350dp，小屏机器可能顶出去。
        //   这里给个上限，横屏时再允许竖向滚动兜底。
        //   ⚠️ 只在横屏启用滚动：竖屏高度足够，且竖屏拖动调色是高频操作，
        //      多加一层滚动手势没必要（也避免跟盘面 drag 抢手势）。
        //
        val screenH = LocalConfiguration.current.screenHeightDp
        //
        // ⚠️ 竖屏用 Dp.Unspecified（= heightIn 的"无上限"默认值），
        //    不要写 Int.MAX_VALUE.dp —— 那个值在 roundToPx() 时会溢出。
        //
        val panelMaxH = if (landscape) (screenH * 0.92f).roundToInt().dp else Dp.Unspecified

        Box(
            modifier = Modifier
                .fillMaxSize()
                //
                // ★★背景加深（两种朝向一致）★★
                //
                // 背景保持透明：要加深的是【玻璃面板本身】（见下方 Column 的玻璃底），
                // 不是背后这一整屏 —— 背后压暗会把取景画面挡掉，
                // 而调色盘本来就是要对着画面调的。
                // 背景仍可点（点空白处关闭调色盘）：Dialog 是独立窗口，
                // 本来就在最上层，不会点到下面的相机控件。
                //
                .background(Color.Transparent)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClose() },
            contentAlignment = if (landscape) Alignment.CenterEnd else Alignment.BottomCenter
        ) {
            // ★减小占屏★：
            //   竖屏 → 宽度收到底部 82%，扁盘
            //   横屏 → 贴右侧固定 230dp 宽的一小块，绝不铺满屏
            Column(
                modifier = (if (landscape) {
                    Modifier
                        .width(230.dp)
                        .heightIn(max = panelMaxH)
                        .verticalScroll(rememberScrollState())
                        .padding(end = 10.dp)
                } else {
                    Modifier
                        .fillMaxWidth(0.82f)
                        .heightIn(max = panelMaxH)
                        // ★「完成」往上抬★：底部留白从 12dp 加到 52dp，
                        // 整个面板离屏幕底边远一点，"完成"就不会贴着边缘、
                        // 也不会压在手势导航条上（全面屏机型尤其明显）
                        .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 52.dp)
                })
                    // ③ 玻璃底：半透明黑打底
                    //    ★★加深：0.42 → 0.82★★
                    //    原来的 0.42 太透，面板里的滑条、数值、按钮跟背后的
                    //    取景画面糊在一起，边界和文字都看不清。
                    //    0.82 实测在亮画面（天空、雪地）和暗画面下都能读清，
                    //    同时仍保留一点"玻璃"的通透感（不是纯黑实心块）。
                    //    想更暗改 0.90，想更透改 0.70。
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    // ② 玻璃层：白色竖向渐变（已按加深后的底色重新压低亮度）
                    .background(brush = appleGlassBrush())
                    // ① 高光边：苹果玻璃最标志性的那道顶边 + 一圈极淡描边
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.10f),
                                Color.White.copy(alpha = 0.16f)
                            )
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* 吃掉点击，避免点到面板就关闭 */ }
                    .padding(12.dp)
            ) {
                // 标题 + 复位合成一行，省掉一整行的高度
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "调色",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // ★测试功能标注★：小字、低透明度，不抢主标题
                    Text(
                        "此功能为测试功能",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "复位",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable { onReset() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                // ── 二维调色盘（扁盘，不再占满整屏）──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 横屏也用 1.55（更扁），把高度让给下面新增的影调行
                        .aspectRatio(1.55f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                                val ny = (change.position.y / size.height).coerceIn(0f, 1f)
                                // ★范围 ±TONE_MAX（150），比原来的 ±100 更广
                                onWarmthChange((nx - 0.5f) * 2f * TONE_MAX)
                                onTintChange((ny - 0.5f) * 2f * TONE_MAX)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cell = 28f
                        var x = 0f
                        while (x < w) {
                            var y = 0f
                            while (y < h) {
                                val nx = x / w
                                val ny = y / h
                                val warm = (nx - 0.5f) * 2f * TONE_MAX
                                val tintV = (ny - 0.5f) * 2f * TONE_MAX
                                //
                                // ★★盘面颜色 = 该位置按【成片增益】作用后的中性灰★★
                                //   不再另写一套近似色 —— 盘面上看到的色块，
                                //   就是拖过去之后照片真正会偏成的颜色。
                                //   用满强度（100）画，让每个位置的取向一眼可辨。
                                //
                                val g3 = toneGains(warm, tintV, 100f)
                                val r = (0.5f * g3[0]).coerceIn(0f, 1f)
                                val g = (0.5f * g3[1]).coerceIn(0f, 1f)
                                val b = (0.5f * g3[2]).coerceIn(0f, 1f)
                                drawRect(
                                    color = Color(r, g, b),
                                    topLeft = Offset(x, y),
                                    size = GeoSize(cell, cell)
                                )
                                y += cell
                            }
                            x += cell
                        }

                        // ★柔光玻璃质感★：盘面上覆一层顶部亮、底部暗的柔和光晕，
                        // 再叠一道顶部高光 —— 就是 iOS 调色盘那种"光从上方打过来"的感觉
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.16f),
                                    Color.White.copy(alpha = 0.03f),
                                    Color.Black.copy(alpha = 0.06f)
                                ),
                                startY = 0f,
                                endY = h
                            )
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(0f, 1.dp.toPx()),
                            end = Offset(w, 1.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )

                        //
                        // ★刻度十字★：中心 = 不偏色，横轴色温、纵轴色调。
                        //   专业调色盘都有，用来判断"偏了多少"。
                        //
                        drawLine(
                            color = Color.White.copy(alpha = 0.18f),
                            start = Offset(w / 2f, 0f),
                            end = Offset(w / 2f, h),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.18f),
                            start = Offset(0f, h / 2f),
                            end = Offset(w, h / 2f),
                            strokeWidth = 1.dp.toPx()
                        )

                        // 当前所在位置：先描一圈黑边，再画白圈，深色区也看得清
                        val px = (warmth / (2f * TONE_MAX) + 0.5f).coerceIn(0f, 1f) * w
                        val py = (tint / (2f * TONE_MAX) + 0.5f).coerceIn(0f, 1f) * h
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.45f),
                            radius = 10.dp.toPx(),
                            center = Offset(px, py),
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 9.dp.toPx(),
                            center = Offset(px, py),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── 强度（与色温/色调数值合成一行）──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "强度",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                                    onIntensityChange(frac * 100f)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val y = size.height / 2f
                            drawLine(
                                color = Color.White.copy(alpha = 0.2f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            val fx = size.width * (intensity / 100f).coerceIn(0f, 1f)
                            drawLine(
                                color = Color(0xFFFFC107),
                                start = Offset(0f, y),
                                end = Offset(fx, y),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 7.dp.toPx(),
                                center = Offset(fx, y)
                            )
                        }
                    }
                    Text(
                        "${intensity.roundToInt()}",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.width(34.dp),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                //
                // ★★影调预设★★
                //
                // 五个一键套用的影调。点一下同时改 warmth / tint / intensity
                // 三个值，预览上立刻能看到（toneOverlayColor 会跟着变），
                // 成片由后处理套用同一组增益，所见即所得。
                //
                // 布局：3 + 2 两行。
                //   ⚠️ 不用 5 个挤一行 —— 面板竖屏只有 82% 屏宽（约 295dp），
                //      去掉内边距后每个不到 55dp，"港式风格"四个字根本放不下。
                //      weight(1f) 平均分，两行对齐，短名字（如"奥莱"）留白多点
                //      反而显得整齐。
                //
                // 选中态：当前三个参数与预设完全一致时高亮琥珀色，
                //   一眼能看出"现在用的是哪个影调"；自己拖过盘之后就不再高亮。
                //

                //
                // ★当前数值★：色温 / 色调 的实际值（范围 ±150）
                //   专业调色盘都会显示，不然只能凭感觉拖。
                //   "暖/冷" 与 "洋红/绿" 的标注对应拖动方向。
                //
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "色温 ${if (warmth >= 0) "暖" else "冷"} " +
                                "${abs(warmth).roundToInt()}",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "色调 ${if (tint >= 0) "洋红" else "绿"} " +
                                "${abs(tint).roundToInt()}",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "±${TONE_MAX.roundToInt()}",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 只剩一个"完成"，复位已挪到标题行
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFD54F),
                                    Color(0xFFFFC107)
                                )
                            )
                        )
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "完成",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
