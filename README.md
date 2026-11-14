# Silky Camera

一款基于 **Kotlin + Jetpack Compose + CameraX** 的 Android 相机应用，
主打手动专业参数控制、真实光学变焦档位，以及针对国产 ROM 的录制稳定性调校。

![版本](https://img.shields.io/badge/version-v3.2.8-orange)
![License](https://img.shields.io/badge/license-MIT-green)
![Platform](https://img.shields.io/badge/platform-Android-blue)

> 当前版本 **v3.2.8**（`versionCode = 12`）。

---

## 📸 特性

### 光学与变焦

- **原厂镜头参数探测** —— 运行时读取焦距、传感器尺寸、光圈，算出真实光学焦段
- **真实光学变焦档位** —— 档位按钮由物理镜头倍率生成，不再写死 0.6 / 1 / 2 / 5 / 10
- **光学吸附** —— 接近物理镜头时自动吸附，避免停在数码裁切的模糊区
- **非线性对称变焦** —— 对数域插值让放大 / 缩小互为镜像；短焦段自动放慢，长焦区更紧凑
- **弹簧式模式切换** —— 阻尼比 1 的弹簧带松手速度，连点承接上次速度，滑到两端越过边界再回弹
- **大跨度回缩优化** —— 针对 OPPO 等机型 20x → 5x 的卡顿，抑制无谓的镜头重绑并放宽下发节流

### 专业拍摄

- **专业模式** —— 手动 ISO / 快门 / EV / 对焦 / 白平衡，参数按 HAL 上报范围安全裁剪
- **RAW / DNG** —— JPEG / RAW 可切换，支持 DNG + JPEG 双出与仅 DNG 单出
- **调色盘** —— 成片级影调增益，面板内实时预览「成片效果」；录像模式不提供
- **哈苏风格快门声** —— 程序合成，可开关
- **构图辅助** —— 三分线、水平仪（俯角达 45° 才自动隐藏，轻微低头不会误消失）
- **防抖四档** —— 关 / 光学 / 电子 / 双重，拍照录像均可用

### 录像

- **画质两档** —— 仅保留 1080P / 4K，整条滑出选择、点空白处关闭
- **4K60 镜头锁定** —— 按开录瞬间的实际画面判定：主摄锁定 **1 至 6x**，长焦 / 中长焦锁定 **5 至 15x**；全程不换头、禁用广角
- **录制变焦非线性** —— 4K60 与 1080P60 走同一条渐进曲线，不再硬切
- **4K 帧率能力探测** —— 跑不到 60 帧自动降为 4K30
- **稳定性修复** —— 缓解 4K60 彩色闪屏、缓存导致摄像头卡死、拖动调色盘预览变黑等问题

### 界面

- **双材质体系** —— 取景界面为透明亚克力柔光玻璃 + 透明底白字参数栏；设置 / 关于等全屏页为白底黑字普通材质
- **视觉对齐** —— 统一左边缘与同心圆角，菜单、面板、弹窗共用一套间距
- **渐进式披露** —— 设置页默认只显示高频项，低频明细按需展开
- **菜单收纳** —— 菜单键固定在参数栏末尾，装不下的按键一次性收进菜单
- **翻转适配** —— 横竖屏切换时参数栏保持端正，短文案连续旋转、长文案（> 4 字）不旋转
- **更新弹窗** —— 更新内容过长时只在中间滚动，「前往安装」始终固定在底部
- **设置页释放摄像头** —— 打开全屏设置时 `unbindAll()`，录制中打开则刻意不暂停，优先保录像

### 协议与合规

- **用户协议门** —— 首次使用及版本 / 协议更新后需重新同意；不同意直接退出且每次启动都弹
- **关于页可查看全文** —— 设置 → 关于 → 用户协议
- **内置开源使用许可** —— 10 条条目，分别标明编译依赖与设计参考的原作者、出处和许可证

### 适配与稳定性

- **多厂商适配** —— vivo / iQOO / 小米 / OPPO / 一加 / 荣耀 / 华为 / 三星
- **小米专项** —— 长焦物理头直连、冗余焦段合并、DNG 暂存私有目录 + IS_PENDING 发布 + TIFF 截断体检
- **Android 16 适配** —— 边到边布局、16KB 页检测
- **自动更新检查** —— 启动时拉取远程版本文件并提示

---

## 📱 已验证机型

| 机型 | 系统 | 光学档位 |
|---|---|---|
| vivo X80 Pro | OriginOS | 0.6x / 1x / 2x / 5x |
| iQOO 15 | OriginOS 6 (Android 16) | 0.6x / 1x / **3x** |

> iQOO 15 没有独立的 2x 人像镜头，光学档位是 3x 潜望。
>
> 档位由运行时探测的物理镜头生成，不同机型结果不同；中间倍率是主摄数码裁切，会显示「数码」标记。
> 部分 ROM 不向第三方暴露真实长焦 id，此时「长焦」为逻辑头数码变焦，但录制期间不跨镜头这条不变。

---

## 🛠 技术栈

- Kotlin + Jetpack Compose (Material3)
- CameraX 1.3.4（core / camera2 / lifecycle / video / view）
- Camera2 Interop（手动参数下发、镜头能力探测）
- Coroutines（异步探测与绑定）

---

## 🚀 开始使用

```bash
git clone https://github.com/hmjs2001/silky-camera.git
cd silky-camera
```

用 Android Studio 打开，Sync 后直接 Run。

最低要求 Android 8.0 (API 26)。

包名：`com.example.hmjs`。

> 版本文件与 Release 发布在 [silky-camera-update](https://github.com/hmjs2001/silky-camera-update)。

---

## 🔄 更新文件（维护者必读）

`update.json` 放在 `silky-camera-update` 仓库的 `main` 根目录，App 通过 jsDelivr 读取：

```
https://cdn.jsdelivr.net/gh/hmjs2001/silky-camera-update@main/update.json
```

发版时三个地方必须同步，否则会出现「已是最新版却一直弹更新」：

1. `app/build.gradle(.kts)` 的 `versionName` / `versionCode`
2. 源码里的 `APP_VERSION_NAME` / `APP_VERSION_CODE`
3. `update.json` 的 `versionName` / `versionCode`

注意：

- `versionName` **带 `v` 前缀**，与源码常量做字符串比对（例：`v3.2.8`）
- `versionCode` 必须**等于**发版包的 code，写大了会让已装新版的用户反复被提示
- `changelog` 是纯文本按 `\n` 逐行加 `• ` 显示，**不渲染 Markdown**；`**标题**` 会原样出现，请用【】分组
- 改完务必在浏览器打开一次 `https://purge.jsdelivr.net/gh/hmjs2001/silky-camera-update@main/update.json` 清 CDN 缓存

---

## 📄 开源许可

本项目采用 **MIT License**。

### ✅ 你可以

- 免费使用，包括**商业用途**
- 修改源码
- 二次分发
- 闭源使用（修改后的代码可以不公开）
- 出售

### ⚠️ 你必须

**保留原作者署名和出处** —— 即在你的项目或应用中，明确标注本项目的作者与来源。

具体做法（满足其一即可，推荐全部）：

1. 在你的项目中保留完整的 `LICENSE` 文件
2. 在应用的「关于」页面或显著位置注明：

   ```
   部分相机功能基于 Silky Camera
   作者：小码-初中 (hmjs)
   项目地址：https://github.com/hmjs2001/silky-camera
   许可：MIT License
   ```

3. 如果你的项目有开源依赖列表（如 `NOTICE`、`THIRD_PARTY` 或 About 页面），
   请将本项目加入其中

应用内「设置 → 开源使用许可」已内置条目：编译进 APK 的依赖与仅参考视觉配方的设计项目分开列示，
后者明确标注「仅参考视觉配方，未引入依赖」，避免把未分发的代码写成使用了。

### ❌ 不提供

本项目按「原样」提供，**不含任何担保**。作者不对使用过程中产生的任何问题负责。

---

## 👤 作者

**hmjs**

- B站 / 抖音：**小码-初中**
- GitHub：[@hmjs2001](https://github.com/hmjs2001)

---

## 🙏 致谢

**编译依赖 / 官方库**

- [Android CameraX](https://developer.android.com/media/camera/camerax) —— Google 官方相机库
- [Jetpack Compose](https://developer.android.com/compose) —— 声明式 UI 框架
- [Kotlin](https://github.com/JetBrains/kotlin) —— JetBrains，Apache-2.0
- [Android Gradle Plugin](https://developer.android.com/build) —— Google AOSP，Apache-2.0
- [Material Components / Icons](https://github.com/material-components/material-components-android) —— Google AOSP，Apache-2.0

**设计参考**（仅参考视觉配方，**未引入依赖**，应用内已署名；链接与「设置 → 开源使用许可」一致）

- [Haze](https://github.com/chrisbanes/haze) —— Chris Banes，Apache-2.0
- [LiquidGlass (iOS 26 风格)](https://github.com/NadeemIqbal/liquid-glass) —— Nadeem Iqbal，Apache-2.0
- [ComposeGlassmorphism](https://github.com/Deiivid/Glassmorphism-Compose) —— David Navarro，MIT
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) —— Kyant0，Apache-2.0
- [Fluent Design · Acrylic](https://learn.microsoft.com/en-us/windows/apps/design/style/acrylic) —— Microsoft，设计规范（非代码库）

> 取景预览使用 `PERFORMANCE` 模式的 SurfaceView，像素不可被捕获，
> 因此玻璃材质以纯色配方模拟，未使用离屏模糊层（叠离屏层会盖住透明洞导致预览全黑）。

---

## ❓ 常见问题

**Q：为什么我这台机器只有 0.6x / 1x / 3x 三个档位？**
A：因为你的机器只有这三颗物理镜头。中间档位（如 2x）是主摄数码裁切，
   会显示「数码」标记。这是正确行为，不是 bug。

**Q：专业模式拉慢快门黑屏怎么办？**
A：已做安全裁剪。若仍出现，请在 Logcat 过滤 `Camera`，
   把 `手动范围` 那行日志提交 issue。

**Q：4K60 录制时为什么变焦被限制、也不能换镜头？**
A：这是刻意的。开录瞬间按实际镜头锁定主摄 1 至 6x 或长焦 5 至 15x，
   全程不换头、不重绑，用来规避 HAL 重配引发的彩色闪屏。停录后恢复正常变焦。

**Q：为什么拖动调色盘时取景画面不变色？**
A：调色增益只作用于成片 JPEG。预览是 SurfaceView，Compose 叠加离屏层会盖住
   透明洞导致全屏黑，所以改为面板里的「当前影调（成片效果）」色块实时显示。
   录像模式不提供调色盘。

**Q：为什么第一次打开 / 更新后又要同意用户协议？**
A：版本钥匙由 `versionCode` + 协议修订号组成，发新版或改协议正文都会重新弹。
   点「不同意」会退出，且每次启动都弹；设置 → 关于里可随时查看全文。

**Q：打开设置页预览黑了一下？**
A：全屏设置打开时会主动释放摄像头（`unbindAll()`），关闭后重绑。
   录制中打开设置则刻意不暂停，优先保证录像不中断。

**Q：装了最新版却还提示更新？**
A：`app/build.gradle` 的 `versionCode` / `versionName` 没和 `update.json` 同步，
   或忘了清 jsDelivr 缓存。见上方「更新文件」小节。

**Q：能商用吗？**
A：可以，MIT 允许商用，只需保留署名。
