# RawCam

Aplikasi kamera Android (Camera2 API) dengan kontrol manual penuh.

## Fitur
- Foto: JPEG, RAW (DNG), atau RAW + JPEG
- Mode AUTO dengan target eksposur (EV) + kunci AE
- Mode MANUAL: ISO, shutter, aperture (jika lensa punya diafragma variabel), fokus manual
- Video: Standar (H.264), SOOC (minim olahan, HEVC), LOG S-Log3 (HEVC), RAW (urutan DNG)
- Pilih kamera belakang (utama / ultrawide / tele), resolusi, dan fps
- Hasil: foto/RAW di `DCIM/RawCam`, video di `Movies/RawCam`, klip RAW di `DCIM/RawCam/CLIP_*`

## Cara build APK
**A. Tanpa Android Studio (GitHub Actions)**
1. Buat repo GitHub baru, upload seluruh isi folder ini (termasuk folder `.github`).
2. Buka tab Actions → "Build APK" → Run workflow.
3. Setelah selesai, unduh artifact `RawCam-debug-apk` → ekstrak → `app-debug.apk`.

**B. Android Studio**
1. File → Open → pilih folder ini, tunggu Gradle sync.
2. Build → Build APK(s). Hasil: `app/build/outputs/apk/debug/app-debug.apk`.

Butuh Android 10+ (API 29).
