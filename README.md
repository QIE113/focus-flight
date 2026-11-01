# 专注飞行 · Android 原生版（完整版）

Kotlin + Jetpack Compose 原生实现。不是套壳，包内不含任何网址。

## 完成度：四个阶段全部完成

| 阶段 | 内容 | 状态 |
|---|---|---|
| 1 | 骨架、全屏沉浸、三屏切换、底部栏 | 完成 |
| 2 | 地球：城市光点、辉光、大圆弧、拖拽惯性 | 完成 |
| 3 | 番茄钟：倒计时、进度环、暂停继续、记录保存 | 完成 |
| 4 | 记录列表、趋势柱状图、航线选择 | 完成 |

## 功能清单

- 首页：时段问候、出发城市、航线（代码 / 时长 / 公里）、地球、开始旅程、底部三格
- 地球：48 座城市光点，可见半球剔除，径向辉光，两点间大圆弧，拖拽可转、松手有惯性
- 飞行：整屏计时，圆环进度，剩余时分秒，暂停 / 继续，结束旅程
- 记录：按时间倒序，显示航线、时间、时长，累计次数与总时长，最多 200 条
- 趋势：最近 7 次柱状图，带星期与分钟标注
- 世界：从当前城市出发的热门航线，点击即切换目的地
- 选择目的地：全部 48 座城市按距离排序，点击生效并持久保存

## 数据与隐私

- **不声明 INTERNET 权限**。系统层面禁止联网，这是非套壳最硬的证明
- 记录与目的地存在 SharedPreferences 本地，不上传任何服务器
- 首次启动无需登录、无需权限弹窗

## 无黑边的三层保证

1. MainActivity 调用 enableEdgeToEdge()，状态栏与导航栏透明
2. themes.xml 里 statusBarColor / navigationBarColor 设为 transparent，windowDrawsSystemBarBackgrounds 为 true
3. 根布局铺满全屏背景，内容区用 statusBarsPadding / navigationBarsPadding 避让系统栏
   结果是背景延伸到四边和刘海区，文字不被遮挡，也没有浏览器地址栏压缩布局

## 怎么出 APK（不用装 Android Studio）

1. 注册 GitHub
2. 新建仓库，把本文件夹里所有文件拖进去
   注意 .github 是隐藏目录，不能漏
3. 打开仓库 Actions 标签，等 Build APK 跑完（3-5 分钟）
4. 点进那次运行，页面底部 Artifacts 下载 focus-flight-debug-apk
5. 解压得到 app-debug.apk，发到手机安装

## 本机出包（可选）

需要 JDK 17 + Android SDK，工程根目录执行：

    gradle assembleDebug

产物：app/build/outputs/apk/debug/app-debug.apk

## 上架 Google Play 出 AAB

先建签名密钥（只做一次，然后备份两处）：

    keytool -genkeypair -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias focusflight

转成 base64 存进仓库 Secrets（Windows PowerShell）：

    [Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks")) | Set-Content ks.txt

仓库 Settings → Secrets and variables → Actions 添加：

| Secret | 内容 |
|---|---|
| KEYSTORE_BASE64 | ks.txt 全部内容 |
| KEYSTORE_PASSWORD | 密钥库密码 |
| KEY_ALIAS | focusflight |
| KEY_PASSWORD | 密钥密码 |

在 .github/workflows/build.yml 末尾追加两步：

    - name: Restore keystore
      run: echo "$KEYSTORE_BASE64" | base64 -d > release.jks
      env:
        KEYSTORE_BASE64: ${{ secrets.KEYSTORE_BASE64 }}

    - name: Build AAB
      run: gradle bundleRelease --no-daemon
      env:
        KEYSTORE_PATH: release.jks
        KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
        KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
        KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}

产物：app/build/outputs/bundle/release/app-release.aab

密钥丢了就永远无法更新这个应用。离线备份两份，不要只存电脑里。

## 上架前需要准备的材料

- 图标 512×512 PNG（无圆角）
- 特色图 1024×500
- 手机截图至少 2 张（进 App 后用系统截图，16:9 或 9:16）
- 简介 80 字、完整描述 4000 字以内
- 隐私政策可访问 URL（可用 Netlify 免费托管一页说明）
- 数据安全表：本项目无联网，可填「不收集任何数据」
- 内容分级问卷

新注册的个人开发者账号还需完成封闭测试：至少 12 人报名、连续 14 天，才能申请正式发布。

## 包名

com.zhuanshi.feixing

上架前如需改成自己的域名反写，现在还来得及。一旦发布就不能再改。