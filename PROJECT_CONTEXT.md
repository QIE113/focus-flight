# 专注飞行 Focus Flight · 项目上下文存档

> 用途：把这个文件发给 AI，就能恢复我们所有的背景、决定和进度。
> 最后更新：v1.4.0 发布之后（2026 年）。

---

## 1. 一句话背景

我在做一个**完全离线**的安卓 App「专注飞行」：选一个目的地 → 选座位 → 选机舱场景 → 撕登机牌 → 进入飞行专注计时 → 落地记录。
代码放在 GitHub：`QIE113/focus-flight`，主分支 `main`。我全程用中文沟通。

---

## 2. 链接与交付物

| 项目 | 地址 |
| --- | --- |
| 仓库 | https://github.com/QIE113/focus-flight |
| 最新版 | https://github.com/QIE113/focus-flight/releases/tag/v1.4.0 |
| APK 直链 | https://github.com/QIE113/focus-flight/releases/download/v1.4.0/focus-flight-v1.4.0-debug.apk |
| 上一版 | https://github.com/QIE113/focus-flight/releases/tag/v1.3.0 |
| 上传页 | https://github.com/QIE113/focus-flight/upload/main |

APK 是 **debug 签名**，安装时提示「未知来源」是正常的。

---

## 3. 硬约束（不要动）

- 全程离线：`AndroidManifest.xml` **不放 INTERNET 权限**，断网也能用。
- 不接 Google Maps SDK、不需要任何 API Key、不产生任何费用。
- 包名 `com.zhuanshi.feixing`；compileSdk 34 / minSdk 26 / JVM 17；Compose BOM 2024.06.00；Kotlin Compose 插件扩展 1.5.14。
- 主题色只有四个：`Gold` / `NightBg` / `TextPrimary` / `TextDim`，定义在 `com.zhuanshi.feixing.ui.theme`。
- 界面全中文，深色夜空 + 金色点缀，克制、不花哨。

---

## 4. v1.4.0 这一轮我提的要求（原意）

1. **地球太卡了** → 必须任何时刻都稳定 60fps。
2. **每个按钮点下去都要有动画**（参考抖音视频的手感）。
3. 流程顺序改成：**先选地址（目的地）→ 再选座位**。
4. 要有**推镜头进入机舱**的过渡动画。
5. 撕票改成：**向右划开，然后票根往下掉**。
6. 参考视频：https://v.douyin.com/C8U7Es_yewo/
7. 交付标准（原话精神）：**你自己觉得和视频一样流畅了再发我**，没做到别急着汇报。

---

## 5. v1.4.0 已经实现的东西

### 5.1 60fps 地球（`ui/globe/GlobeCanvas.kt`）

- `object SphereMath`：`TILT_DEG = 16f`、`midLon()`、`project()`。
- `class SphereRenderer(diameter)`：在 `init` 里把整张球的**球面投影、光照、边缘抗锯齿**全部预计算成查找表（`uBase` / `vIdx` / `shade` / `edge`，ShortArray / ByteArray）。
- 每帧只做**整数查表**写进一个 IntArray，再 `Bitmap.setPixels`；用两张 Bitmap 交替做双缓冲。
- 自转 2.4°/s；只有经度变化超过 0.45° 才重新渲染；直径 `roundToInt().coerceIn(140, 720)` 封顶。
- 大气辉光用 radial gradient；大圆航线用 slerp 48 段插值，飞机三角随航向旋转，起降点两个圆点。
- 纹理尺寸**硬编码 1024×512**，不再依赖 `EarthTexture.WIDTH/HEIGHT`（那两个常量签名不稳，踩过坑）。

### 5.2 动效系统（`ui/Motion.kt`）

- `Modifier.pressScale(source, scaleDown)`：按下缩到 0.94 左右，`spring(dampingRatio = 0.42f, stiffness = 1400f)` 回弹。所有按钮都挂了这个。
- `Appear(visible, delayMillis, riseDp)`：460ms 淡入 + 上浮 + 0.97→1.0 缩放；列表用它逐条错开出现。
- 页面切换：`AnimatedContent` + `fadeIn(tween(340)) togetherWith fadeOut(tween(200))`。

### 5.3 流程

`enum class Stage { Home, Seat, Scene, Ticket, Fly, Done, Records, Trend, World }`

- `RoutePickerSheet`：**两步**，step 0 选出发地，step 1 选目的地，底部按钮「下一步 · 选座位」。
- 座位：4 列 × 10 排，列号 A / C / D / F。
- 场景：5 个（机舱氛围）。
- 登机牌 → 撕票 → 推镜头 → 机舱。

### 5.4 撕票（`BoardingPassScreen`）

- 两个 `Animatable`：`gap`（横向划开）和 `fall`（下坠）。`maxGap = 210.dp`，阈值 `maxGap * 0.3f`。
- `detectDragGestures` → `gap.snapTo(...)`，拖起来震一下，越过阈值再震一下。
- 松手若 `gap > 阈值`：`gap` 先补到 1.3 倍（200ms）→ `fall` 0→1（640ms, FastOutSlowIn）→ `onBoarded()`。
- 票根 `graphicsLayer`：`translationX = gap`、`translationY = gap*0.10 + fall*900`、`rotationZ = gap*0.05 + fall*36`、`alpha = 1 - fall*0.92`。
- 没划过阈值就 spring 弹回 0。

### 5.5 推镜头进机舱

`Animatable zoom` 0→1 / 1700ms / FastOutSlowInEasing；地球 `graphicsLayer` 的 `scale = 1 + zoom*3.8`、位移 `-zoom*90 / -zoom*40`；HUD 透明度 `((zoom-0.45)/0.55).coerceIn(0,1)` 渐显。

### 5.6 其它

- `MapScreen`：`detectTransformGestures` 双指缩放 1–14x + 拖动，画经纬网、航线、剩余时间 / 剩余公里。
- `FlightScreen`：暂停 / 静音 / 地图 / 归位 / 免打扰 五个圆钮。
- `TrendScreen`：7 日柱状图；`RecordScreen`：历史记录；`WorldScreen`：世界数据。
- 到达 `Stage.Done` 时把 `FlightRecord` 写进 `FlightStore`（SharedPreferences `focus_flight`，最多 200 条）。

---

## 6. 代码结构

```
app/src/main/java/com/zhuanshi/feixing/
├── data/
│   ├── Geo.kt            大圆距离、时长（巡航 840km/h + 26min 起降开销）、格式化
│   ├── Cities.kt         城市表
│   ├── FlightRecord.kt   记录数据类
│   ├── FlightStore.kt    SharedPreferences 存取
│   └── EarthTexture.kt   地球贴图（世界地图点阵）
└── ui/
    ├── theme/            Gold / NightBg / TextPrimary / TextDim
    ├── Motion.kt         pressScale、Appear
    ├── Ui.kt             StarField、GoldButton、GhostButton、RoundAction、TopBar、HairLine、KeyValueRow、ScreenColumn
    ├── FlightApp.kt      Stage 枚举、AnimatedContent 路由、记录落盘
    ├── globe/GlobeCanvas.kt  SphereMath / SphereRenderer / Globe / drawGreatCircle
    └── screens/
        ├── HomeScreen.kt          地球 + RouteCard + 选择目的地 + 世界/记录/趋势入口
        ├── RoutePickerSheet.kt    出发地 → 目的地 两步选
        ├── FlightSessionScreen.kt 选座 / 场景 / 登机牌(撕票) / 飞行 / 落地
        ├── MapScreen.kt           航图（可缩放）
        ├── RecordScreen.kt        历史
        ├── TrendScreen.kt         趋势
        └── WorldScreen.kt         世界
```

---

## 7. 构建与发布（CI）

- `.github/workflows/build.yml`：push 到 `main` 触发；JDK 17 + Gradle 8.7；跑 `gradle assembleDebug`。
- **编译失败时会把所有 `e:` 开头的错误行写进 Actions 的 Step Summary**（标题「编译错误」）。这是专门为排查加的，别删。
- 成功则把产物复制成 `out/focus-flight-v1.4.0-debug.apk`，上传 artifact，并用 `softprops/action-gh-release` 发 Release（tag `v1.4.0`）。
- 版本号在 `app/build.gradle.kts`：`versionCode 4` / `versionName "1.4.0"`。**发新版要同时改**：版本号、workflow 里的文件名、tag、Release 名字和正文。
- Release 签名（可选）：配 `KEYSTORE_PATH` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` 四个环境变量，否则用 debug 签名。

### 踩过的坑（别再犯）

1. `Column(horizontalAlignment = Alignment.Center)` → 编译报 `Type mismatch: inferred type is Alignment but Alignment.Horizontal was expected`。必须写 `Alignment.CenterHorizontally`。
2. `import androidx.compose.foundation.layout.weight` 是无效导入，`weight` 是 RowScope/ColumnScope 的扩展函数，不用 import。
3. 不要依赖 `EarthTexture` 的常量或位图 API（`WIDTH` / `HEIGHT` / `pixels` / `bitmap`），签名不稳；球体渲染自己硬编码 1024×512。
4. 网页 multipart 上传多文件时，必须用 `DataTransfer` + `new File([content], path)` 塞进 `#upload-manifest-files-input` 再 dispatch `change`，这样能保留目录结构；要等文件名都出现在列表、`Uploading` 字样消失再点提交。

---

## 8. 还没解决的问题

- **到达仪式**：现在落到 `Done` 就收尾了，要不要做个「落地 → 下机 → 记录盖章」的收尾动画，我还没定。
- **航图形态**：现在是可以缩放的平面投影，要不要改成球面 3D，没定。
- **7.92 小时那条航线**是哪一条？之前我提过这个数字，还没确认对应城市。
- **真机 60fps 采样**：逻辑上每帧只剩整数查表 + setPixels，但还没在真机上跑过 profiler 出数据。
- **正式签名**：目前发的都是 debug 包。

---

## 9. 我的偏好（接手前先看这段）

- 全中文回答，**直接、别客套、别铺垫**，讲清楚取舍和风险。
- 能自己判断的就做完再说话，不要频繁问「要不要继续」。
- **交付前自己先检查一遍**，别让我当第一个测试员。
- 每次给我：APK 链接 + 这次改了什么（列点）+ 已知问题。
- 代码风格：Kotlin + Jetpack Compose，一个屏幕一个文件，注释写中文，不要引入额外 SDK。

---

## 10. 怎么改这份代码（没有本地环境，全在 GitHub 网页上操作）

- 改文件：打开 `https://github.com/QIE113/focus-flight/upload/main`，用脚本构造 `DataTransfer` 上传（见第 7 节第 4 条），提交信息用中文。
- 提交按钮：文本恰好等于 `Commit changes` 的那个按钮；摘要框要用原生 `HTMLInputElement` 的 value setter 再触发 `input` 事件，直接改 value 不生效。
- 看编译错误：`https://github.com/QIE113/focus-flight/actions` → 最新 run → 页面里搜「编译错误」。
- 读源码：打开 `blob/main/...` 页面，从 `package com.zhuanshi` 开始截 `document.body.innerText`（raw 接口不可靠）。

---

## 11. 版本线

| 版本 | 内容 |
| --- | --- |
| v1.3.0 | 第一版能用的完整流程，但地球卡、按钮没动画 |
| v1.4.0 | 重建渲染器（60fps）+ 全流程动效 + 地址→座位顺序 + 右划撕票下坠 + 推镜头进舱 |
