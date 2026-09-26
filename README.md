# Silky Camera

一款基于 **Kotlin + Jetpack Compose + CameraX** 的 Android 相机应用，
主打手动专业参数控制与真实光学变焦档位。

![版本](https://img.shields.io/badge/version-3.0.3-orange)
![License](https://img.shields.io/badge/license-MIT-green)
![Platform](https://img.shields.io/badge/platform-Android-blue)

---

## 📸 特性

- **原厂镜头参数探测** —— 运行时读取焦距、传感器尺寸、光圈，算出真实光学焦段
- **真实光学变焦档位** —— 档位按钮由物理镜头倍率生成，不再写死 0.6/1/2/5/10
- **光学吸附** —— 接近物理镜头时自动吸附，避免停在数码裁切的模糊区
- **专业模式** —— 手动 ISO / 快门 / EV / 对焦 / 白平衡，参数按 HAL 上报范围安全裁剪
- **多厂商适配** —— vivo / iQOO / 小米 / OPPO / 一加 / 荣耀 / 华为 / 三星
- **Android 16 适配** —— 边到边布局、16KB 页检测
- **自动更新检查** —— 启动时拉取远程版本文件并提示

---

## 📱 已验证机型

| 机型 | 系统 | 光学档位 |
|---|---|---|
| vivo X80 Pro | OriginOS | 0.6x / 1x / 2x / 5x |
| iQOO 15 | OriginOS 6 (Android 16) | 0.6x / 1x / **3x** |

> iQOO 15 没有独立的 2x 人像镜头，光学档位是 3x 潜望。

---

## 🛠 技术栈

- Kotlin + Jetpack Compose (Material3)
- CameraX 1.3.4（core / camera2 / lifecycle / video / view）
- Camera2 Interop（手动参数下发）
- Coroutines（异步探测）

---

## 🚀 开始使用

```bash
git clone https://github.com/hmjs2001/silky-camera.git
cd silky-camera
```

用 Android Studio 打开，Sync 后直接 Run。

最低要求 Android 8.0 (API 26)。

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

### ❌ 不提供

本项目按「原样」提供，**不含任何担保**。作者不对使用过程中产生的任何问题负责。

---

## 👤 作者

**hmjs**

- B站 / 抖音：**小码-初中**
- GitHub：[@hmjs2001](https://github.com/hmjs2001)

---

## 🙏 致谢

- [Android CameraX](https://developer.android.com/media/camera/camerax) —— Google 官方相机库
- [Jetpack Compose](https://developer.android.com/compose) —— 声明式 UI 框架

---

## ❓ 常见问题

**Q：为什么我这台机器只有 0.6x / 1x / 3x 三个档位？**
A：因为你的机器只有这三颗物理镜头。中间档位（如 2x）是主摄数码裁切，
   会显示「数码」标记。这是正确行为，不是 bug。

**Q：专业模式拉慢快门黑屏怎么办？**
A：已做安全裁剪。若仍出现，请在 Logcat 过滤 `Camera`，
   把 `手动范围` 那行日志提交 issue。

**Q：能商用吗？**
A：可以，MIT 允许商用，只需保留署名。
