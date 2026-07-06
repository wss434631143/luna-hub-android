# Luna Hub Android

Luna Hub Android 是面向 Insta360 Luna Ultra / Insta360 相机用户的原生 Android 素材管理工具。

当前版本不是 WebView 套壳，也不是 Capacitor 原型，而是基于 Kotlin、Jetpack Compose 和 Material 3 重建的原生 App。目标体验接近 HyperOS / 小米系统工具类应用：浅灰背景、白色圆角卡片、清晰分组、自然反馈、完整深色模式。

## 当前版本

- 版本：`0.1.3`
- 包名：`com.lunahub.android`
- 最低系统：Android 7.0 / API 24
- 目标系统：Android 15 / API 36
- 当前发布类型：Debug APK

## 已实现能力

### 原生 Android 架构

- Kotlin + Jetpack Compose + Material 3
- Hilt 依赖注入
- Navigation Compose 页面路由
- MVVM + Clean Architecture 分层
- Coroutines + Flow 状态流
- Room 保存下载任务
- OkHttp / Retrofit 接入相机 HTTP 目录服务

### 页面

- 首页：相机连接状态、最近素材、快捷入口
- 相机连接页：Mock / 真实相机模式切换后的连接状态展示
- 相机素材库：按日期分组、照片 / 视频筛选、自适应网格、多选
- 素材预览：文件信息、下载到 App、导出到手机占位
- 下载任务：队列、进度、等待中、下载中、成功、失败、取消、重试
- 设置：外观、相机、导出、存储、关于

### 相机接入

真实模式默认访问：

```text
http://192.168.42.1/storage_internal/DCIM/
```

当前相机媒体读取方式来自 PC 版 Luna AI Cut 的通信逻辑：

- 读取相机 HTTP directory index
- 解析照片、视频和 `.lrv` 预览文件
- 递归读取 `Camera01/` 等相机目录
- 从目录条目解析文件名、大小、拍摄时间、媒体类型和下载 URL

### 下载

- 支持单个下载
- 支持素材库多选批量下载
- 使用 OkHttp 流式下载大文件
- 使用协程在后台执行，避免阻塞 UI
- 使用 Room 保存下载记录
- 下载完成后标记素材为已下载
- 失败时显示可读错误，支持重试
- 下载中和等待中任务支持取消
- Android 10+ 默认保存到 App 专属外部目录，不申请 `MANAGE_EXTERNAL_STORAGE`

保存目录示例：

```text
Android/data/com.lunahub.android/files/Pictures/Luna Hub/
```

## 构建

需要 JDK 21 和 Android SDK。

```bash
cd android
ANDROID_HOME=/opt/android-sdk ./gradlew assembleDebug
```

构建完成后 APK 位于：

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions

仓库已配置自动打包：

- 推送到 `main` 后自动运行
- 也支持手动 `workflow_dispatch`
- 产物名称：`luna-hub-android-debug-apk`

工作流文件：

```text
.github/workflows/android-apk.yml
```

## 使用真实相机

1. 打开 Luna / Insta360 相机热点。
2. 在 Android 系统 Wi-Fi 中连接名称以 `Luna` 开头的相机热点。
3. 打开 Luna Hub。
4. 进入“连接相机”，点击“扫描相机”。
5. 连接成功后进入相机相册查看和下载素材。

如果扫描失败，请检查 Android 是否开启了 WLAN+ / 自动切换网络。部分手机会因为相机热点不能上网而自动切回普通 Wi-Fi，需要临时关闭自动切换或忘记 Luna 热点后重新连接。

## 当前限制

- 目前发布的是 Debug APK，尚未做正式签名。
- 视频预览播放尚未接入 Media3。
- 图片大图查看和手势缩放尚未完成。
- Type-C / USB 相机连接尚未接入。
- 水印导出仍是占位入口。
- 下载目前保存到 App 专属目录，后续可扩展到 MediaStore 相册目录。
- 后台下载通知栏进度尚未接入。

## 后续计划

1. 接入 Media3 视频播放和图片大图预览。
2. 支持 MediaStore 导出到系统相册。
3. 增加后台下载和通知栏进度。
4. 增加 Type-C / USB 连接能力。
5. 完成水印图片导出。
6. 增加正式签名、Release APK / AAB 发布流程。
