# VirtualCam – Android 14 Virtual Camera App

A virtual camera app for Android 14+ that lets you stream a custom video source
(still image, video file, or your screen) to other apps like Zoom, Google Meet,
and WhatsApp via the Android VirtualCamera API.

---

## Requirements

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog 2023.1+ |
| Android Gradle Plugin | 8.3+ |
| Kotlin | 1.9.23 |
| Device / Emulator | Android 14 (API 34) |

---

## Getting the APK (no Android Studio needed)

GitHub builds the APK for free in ~4 minutes. All you need is a free GitHub account.

### Step-by-step

1. Go to **https://github.com/new** and create a new **public** repository (name it `VirtualCam`).
2. Upload **all the files from this zip** into that repo — drag and drop them in the GitHub web UI,
   or use `git push` if you have Git installed.
3. GitHub will automatically start the **Build VirtualCam APK** workflow (the `.github/workflows/build.yml` file).
4. Click the **Actions** tab on your repo page.
5. Click the workflow run → scroll to **Artifacts** at the bottom → click **VirtualCam-debug** to download the APK.
6. Transfer the APK to your Android 14 phone (AirDrop, email, USB, Google Drive — any way).
7. On the phone enable **Settings → Install unknown apps** for your file manager, then tap the APK to install.

> You can also re-trigger the build any time from Actions → Run workflow, without pushing new code.

---

## Project structure

```
app/src/main/java/com/virtualcam/
│
├── MainActivity.kt              – Entry point, permissions, Compose host
│
├── service/
│   ├── VirtualCamService.kt     – Foreground service + VirtualCamera API stub
│   └── NotificationHelper.kt   – Persistent notification builder
│
├── source/
│   ├── VideoSource.kt           – Interface + SourceType enum
│   ├── ImageSource.kt           – Renders a still image at 30 fps
│   ├── VideoFileSource.kt       – MediaCodec decode loop for video files
│   └── ScreenCaptureSource.kt  – MediaProjection screen mirror
│
├── manager/
│   └── VideoSourceManager.kt   – Owns all sources; handles hot-swapping
│
└── ui/
    ├── MainViewModel.kt         – State + commands for the UI
    ├── MainScreen.kt            – Full Compose UI
    └── theme/
        └── Theme.kt             – Dark colour palette, Material 3 scheme
```

---

## Wiring in the Android 14 VirtualCamera API

The service (`VirtualCamService.kt`) has a `TODO` block clearly marked with the
integration steps. Here is the short version:

```kotlin
// 1. Extend VirtualCameraService (SDK 34 class)
class VirtualCamService : VirtualCameraService() {

    // 2. System calls this when another app opens your virtual camera
    override fun onVirtualCameraOpened(cameraId: String) {
        val session = VirtualCamera.open(cameraId, this, mainExecutor) { sess ->
            val cfg = sess.currentStreamConfig          // resolution chosen by consumer
            sourceManager?.attachSurface(
                sess.surface, cfg.width, cfg.height
            )
            sourceManager?.startStreaming()
        }
    }

    // 3. System calls this when the consumer app releases the camera
    override fun onVirtualCameraClosed(cameraId: String) {
        sourceManager?.stopStreaming()
    }
}
```

> **Note:** The exact class path for `VirtualCameraService` may vary between
> SDK preview builds. Search for `VirtualCamera` in Android Studio's "Navigate
> → Class" dialog (Ctrl+N) after syncing the SDK 34 sources.

---

## Adding sources

Implement `VideoSource` and register it in `VideoSourceManager`:

```kotlin
class MyCustomSource(context: Context) : VideoSource {
    override val displayName = "My Source"
    override val type = SourceType.IMAGE   // reuse or extend SourceType

    override fun start(surface: Surface, width: Int, height: Int) {
        // push frames to surface here
    }
    override fun stop()    { /* stop rendering */ }
    override fun release() { /* free resources */ }
}
```

---

## Permissions summary

| Permission | Why |
|-----------|-----|
| `FOREGROUND_SERVICE_CAMERA` | Required for camera-type foreground service |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Required for screen capture source |
| `CAMERA` | Needed if you add a live camera passthrough source |
| `READ_MEDIA_VIDEO / IMAGES` | Needed to read files from storage |
| `POST_NOTIFICATIONS` | Shows the persistent "camera active" notification |

---

## Roadmap / improvements

- [ ] Wire in Android 14 `VirtualCamera` / `VirtualCameraService` API
- [ ] OpenGL ES filter pipeline (blur, colour grade, background removal)
- [ ] ML Kit background segmentation
- [ ] Network / IP camera source (RTSP / NDI)
- [ ] Slideshow mode for the image source
- [ ] In-app preview using `SurfaceView` mirroring the output surface
