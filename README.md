# Luna Hub Android

Luna Hub 的 Android 版本原型，基于 React + Vite + Capacitor 构建。

## 当前能力

- 移动端媒体库布局，支持图片和视频卡片。
- 长按进入选择模式，手指滑过卡片可连续多选。
- 预览页支持左右滑动切换上一张 / 下一张图片或视频。
- Capacitor Android 工程骨架，可同步到 Android Studio 继续接入原生能力。

## 开发

```bash
npm install
npm run dev
```

## 构建 Web 资源

```bash
npm run build
```

## 同步 Android 工程

```bash
npx cap sync android
```

## 后续原生接入方向

- 使用 Android MediaStore / Storage Access Framework 读取本机媒体。
- 通过移动端 HTTP 能力复用 Luna 相机 Wi-Fi 媒体协议。
- 补充 Android 保存、分享、后台下载和权限请求。
