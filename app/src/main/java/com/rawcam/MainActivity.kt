package com.rawcam

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.Size
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.rawcam.CameraController.PhotoFmt
import com.rawcam.CameraController.VideoFmt
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

class MainActivity : Activity(), CameraController.Listener {

    private lateinit var tv: AutoFitTextureView
    private lateinit var ctl: CameraController

    private lateinit var tvStatus: TextView
    private lateinit var spCam: Spinner
    private lateinit var spFmt: Spinner
    private lateinit var spSize: Spinner
    private lateinit var spFps: Spinner
    private lateinit var btnPhoto: Button
    private lateinit var btnVideo: Button
    private lateinit var btnAuto: Button
    private lateinit var btnManual: Button
    private lateinit var btnCapture: Button
    private lateinit var grpAuto: LinearLayout
    private lateinit var grpManual: LinearLayout
    private lateinit var grpVideo: LinearLayout
    private lateinit var sbEv: SeekBar
    private lateinit var sbIso: SeekBar
    private lateinit var sbShutter: SeekBar
    private lateinit var sbAp: SeekBar
    private lateinit var sbFocus: SeekBar
    private lateinit var lbEv: TextView
    private lateinit var lbIso: TextView
    private lateinit var lbShutter: TextView
    private lateinit var lbAp: TextView
    private lateinit var lbFocus: TextView
    private lateinit var cbAeLock: CheckBox

    private var camIds = listOf<String>()
    private var photoFmts = listOf<PhotoFmt>()
    private var videoFmts = listOf<VideoFmt>()
    private var sizes = listOf<Size>()
    private var fpsList = listOf<Int>()

    // ------------------------------------------------------------ lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUi()
        tv = AutoFitTextureView(this)
        ctl = CameraController(this, { rotation() }, { tv.surfaceTexture }, this)
        buildUi()
        tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(s: SurfaceTexture, w: Int, h: Int) { openCamera() }
            override fun onSurfaceTextureSizeChanged(s: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(s: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(s: SurfaceTexture) {}
        }
        if (!hasCam()) requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 1)
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        (getSystemService(DISPLAY_SERVICE) as DisplayManager).registerDisplayListener(dl, null)
        if (tv.isAvailable) openCamera()
    }

    override fun onPause() {
        (getSystemService(DISPLAY_SERVICE) as DisplayManager).unregisterDisplayListener(dl)
        ctl.close()
        super.onPause()
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(code, perms, res)
        if (hasCam()) openCamera() else toast("Izin kamera diperlukan")
    }

    private val dl = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) { tv.post { tv.updateTransform(rotation()) } }
    }

    private fun hasCam() = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun openCamera() {
        if (!hasCam() || !tv.isAvailable) return
        val id = ctl.cameraId.ifEmpty { ctl.backCameras().firstOrNull()?.first ?: return }
        ctl.open(id)
    }

    @Suppress("DEPRECATION")
    private fun rotation(): Int = windowManager.defaultDisplay.rotation

    @Suppress("DEPRECATION")
    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    // ------------------------------------------------------------ listener dari controller
    override fun onCameraReady() { runOnUiThread { refreshAll() } }
    override fun onPreviewSize(w: Int, h: Int) {
        runOnUiThread { tv.setAspectRatio(w, h); tv.updateTransform(rotation()) }
    }
    override fun onStatus(text: String) { runOnUiThread { tvStatus.text = text } }
    override fun onRecording(on: Boolean) {
        runOnUiThread { btnCapture.text = if (on) "■" else "●"; setPanelEnabled(!on) }
    }
    override fun onMessage(msg: String) { runOnUiThread { toast(msg) } }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    // ------------------------------------------------------------ UI
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(t: String) = TextView(this).apply { text = t; setTextColor(Color.WHITE); textSize = 12f }

    private fun btn(t: String, onClick: () -> Unit) = Button(this).apply {
        text = t; textSize = 12f; isAllCaps = false; setOnClickListener { onClick() }
    }

    private fun seek(max: Int, cb: (Int) -> Unit) = SeekBar(this).apply {
        this.max = max
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) { if (fromUser) cb(p) }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {}
        })
    }

    private fun spinner(cb: (Int) -> Unit) = Spinner(this).apply {
        onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { cb(pos) }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun setItems(sp: Spinner, items: List<String>, sel: Int) {
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        if (sel >= 0) sp.setSelection(sel, false)
    }

    private fun LinearLayout.add(v: View) = addView(v, LinearLayout.LayoutParams(-1, -2))

    private fun row(a: View, b: View) = LinearLayout(this).apply {
        addView(a, LinearLayout.LayoutParams(0, -2, 1f))
        addView(b, LinearLayout.LayoutParams(0, -2, 1f))
    }

    private fun buildUi() {
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        root.addView(tv, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        tv.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> tv.updateTransform(rotation()) }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(0xB3000000.toInt())
        }
        root.addView(ScrollView(this).apply { addView(panel) }, FrameLayout.LayoutParams(dp(300), -1, Gravity.END))

        tvStatus = TextView(this).apply {
            setTextColor(Color.rgb(120, 255, 160)); textSize = 12f; typeface = Typeface.MONOSPACE; text = "…"
        }
        panel.add(tvStatus)

        spCam = spinner { pos ->
            val id = camIds.getOrNull(pos)
            if (id != null && id != ctl.cameraId && !ctl.recording) ctl.open(id)
        }
        panel.add(spCam)

        btnPhoto = btn("FOTO") { setMode(false) }
        btnVideo = btn("VIDEO") { setMode(true) }
        panel.add(row(btnPhoto, btnVideo))

        spFmt = spinner { pos ->
            if (ctl.recording) return@spinner
            if (ctl.videoMode) {
                val f = videoFmts.getOrNull(pos) ?: return@spinner
                if (f != ctl.videoFmt) ctl.videoFmt = f
            } else {
                val f = photoFmts.getOrNull(pos) ?: return@spinner
                if (f != ctl.photoFmt) ctl.photoFmt = f
            }
        }
        panel.add(spFmt)

        spSize = spinner { pos ->
            val s = sizes.getOrNull(pos) ?: return@spinner
            if (s != ctl.videoSize && !ctl.recording) { ctl.videoSize = s; refreshVideoOpts() }
        }
        spFps = spinner { pos ->
            val f = fpsList.getOrNull(pos) ?: return@spinner
            if (f != ctl.fps && !ctl.recording) { ctl.fps = f; refreshShutter() }
        }
        grpVideo = LinearLayout(this).apply { addView(spSize, LinearLayout.LayoutParams(0, -2, 1f)); addView(spFps, LinearLayout.LayoutParams(0, -2, 1f)) }
        panel.add(grpVideo)

        btnAuto = btn("AUTO") { ctl.auto = true; applyVisibility() }
        btnManual = btn("MANUAL") { ctl.auto = false; refreshShutter(); applyVisibility() }
        panel.add(row(btnAuto, btnManual))

        // --- grup AUTO: target eksposur (EV) + kunci AE
        grpAuto = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        lbEv = label("Target eksposur: 0.0 EV")
        sbEv = seek(1) { p -> ctl.ev = p + ctl.evRange.lower; updateEvLabel() }
        cbAeLock = CheckBox(this).apply {
            text = "Kunci AE"; setTextColor(Color.WHITE)
            setOnCheckedChangeListener { _, c -> ctl.aeLock = c }
        }
        grpAuto.add(lbEv); grpAuto.add(sbEv); grpAuto.add(cbAeLock)
        panel.add(grpAuto)

        // --- grup MANUAL: ISO, shutter, aperture
        grpManual = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        lbIso = label("ISO")
        sbIso = seek(1000) { p -> ctl.iso = progToIso(p); lbIso.text = "ISO ${ctl.iso}" }
        lbShutter = label("Shutter")
        sbShutter = seek(1000) { p -> ctl.shutterNs = progToShutter(p); lbShutter.text = "Shutter " + fmtShutter(ctl.shutterNs) }
        lbAp = label("Aperture")
        sbAp = seek(1) { p ->
            val ap = ctl.apertures
            if (p in ap.indices) { ctl.aperture = ap[p]; lbAp.text = String.format(Locale.US, "Aperture f/%.1f", ap[p]) }
        }
        grpManual.add(lbIso); grpManual.add(sbIso)
        grpManual.add(lbShutter); grpManual.add(sbShutter)
        grpManual.add(lbAp); grpManual.add(sbAp)
        panel.add(grpManual)

        lbFocus = label("Fokus: AF")
        sbFocus = seek(100) { p -> ctl.focusPos = p; lbFocus.text = if (p == 0) "Fokus: AF" else "Fokus: MF $p%" }
        panel.add(lbFocus); panel.add(sbFocus)

        btnCapture = Button(this).apply {
            text = "●"; textSize = 28f; setTextColor(Color.RED)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.WHITE) }
            setOnClickListener {
                if (!ctl.videoMode) ctl.takePhoto()
                else if (ctl.recording) ctl.stopRecording() else ctl.startRecording()
            }
        }
        root.addView(btnCapture, FrameLayout.LayoutParams(dp(84), dp(84), Gravity.START or Gravity.CENTER_VERTICAL).apply { leftMargin = dp(24) })

        setContentView(root)
        applyVisibility()
    }

    private fun setPanelEnabled(en: Boolean) {
        listOf<View>(spCam, spFmt, spSize, spFps, btnPhoto, btnVideo).forEach { it.isEnabled = en }
    }

    private fun applyVisibility() {
        grpAuto.visibility = if (ctl.auto) View.VISIBLE else View.GONE
        grpManual.visibility = if (ctl.auto) View.GONE else View.VISIBLE
        grpVideo.visibility = if (ctl.videoMode) View.VISIBLE else View.GONE
        btnPhoto.alpha = if (ctl.videoMode) 0.5f else 1f
        btnVideo.alpha = if (ctl.videoMode) 1f else 0.5f
        btnAuto.alpha = if (ctl.auto) 1f else 0.5f
        btnManual.alpha = if (ctl.auto) 0.5f else 1f
    }

    private fun setMode(video: Boolean) {
        if (ctl.recording) return
        ctl.videoMode = video
        refreshFormats(); refreshVideoOpts(); refreshShutter(); applyVisibility()
    }

    // ------------------------------------------------------------ sinkronisasi UI <-> controller
    private fun refreshAll() {
        val cams = ctl.backCameras()
        camIds = cams.map { it.first }
        setItems(spCam, cams.map { it.second }, camIds.indexOf(ctl.cameraId))
        refreshFormats(); refreshVideoOpts()

        val r = ctl.evRange
        sbEv.max = (r.upper - r.lower).coerceAtLeast(1)
        sbEv.progress = ctl.ev - r.lower
        sbEv.isEnabled = r.upper > r.lower
        updateEvLabel()

        sbIso.progress = isoToProg(ctl.iso)
        lbIso.text = "ISO ${ctl.iso}"
        refreshShutter()

        val ap = ctl.apertures
        if (ap.size > 1) {
            sbAp.visibility = View.VISIBLE
            sbAp.max = ap.size - 1
            sbAp.progress = ap.indexOfFirst { it == ctl.aperture }.coerceAtLeast(0)
            lbAp.text = String.format(Locale.US, "Aperture f/%.1f", ctl.aperture)
        } else {
            sbAp.visibility = View.GONE
            lbAp.text = if (ap.size == 1) String.format(Locale.US, "Aperture f/%.1f (tetap, lensa ini)", ap[0])
            else "Aperture: tidak tersedia"
        }
        sbFocus.progress = ctl.focusPos
        lbFocus.text = if (ctl.focusPos == 0) "Fokus: AF" else "Fokus: MF ${ctl.focusPos}%"
        applyVisibility()
    }

    private fun refreshFormats() {
        if (ctl.videoMode) {
            videoFmts = ctl.availableVideoFmts()
            setItems(spFmt, videoFmts.map { videoLabel(it) }, videoFmts.indexOf(ctl.videoFmt))
        } else {
            photoFmts = ctl.availablePhotoFmts()
            setItems(spFmt, photoFmts.map { photoLabel(it) }, photoFmts.indexOf(ctl.photoFmt))
        }
    }

    private fun refreshVideoOpts() {
        sizes = ctl.videoSizes()
        setItems(spSize, sizes.map { "${it.width}x${it.height}" }, sizes.indexOf(ctl.videoSize))
        fpsList = ctl.fpsOptions(ctl.videoSize)
        setItems(spFps, fpsList.map { "$it fps" }, fpsList.indexOf(ctl.fps))
    }

    private fun refreshShutter() {
        val mn = ctl.shutterMin.toDouble()
        val mx = maxOf(ctl.shutterMax.toDouble(), mn)
        val ns = ctl.shutterNs.coerceIn(ctl.shutterMin, mx.toLong())
        if (ns != ctl.shutterNs) ctl.shutterNs = ns
        sbShutter.progress = if (mx <= mn) 0 else (ln(ns / mn) / ln(mx / mn) * 1000).roundToInt().coerceIn(0, 1000)
        lbShutter.text = "Shutter " + fmtShutter(ns)
    }

    private fun updateEvLabel() {
        val r = ctl.evRange
        lbEv.text = if (r.upper <= r.lower) "Target eksposur: tidak didukung"
        else String.format(Locale.US, "Target eksposur: %+.1f EV", ctl.ev * ctl.evStepF)
    }

    private fun videoLabel(f: VideoFmt) = when (f) {
        VideoFmt.STANDARD -> "Video standar (H.264)"
        VideoFmt.SOOC -> "SOOC – minim olahan (HEVC)"
        VideoFmt.LOG -> "LOG S-Log3 (HEVC)"
        VideoFmt.RAW_DNG -> "RAW – urutan DNG"
    }

    private fun photoLabel(f: PhotoFmt) = when (f) {
        PhotoFmt.JPEG -> "JPEG"
        PhotoFmt.RAW -> "RAW (DNG)"
        PhotoFmt.RAW_JPEG -> "RAW + JPEG"
    }

    // ------------------------------------------------------------ pemetaan slider (logaritmik)
    private fun progToIso(p: Int): Int {
        val r = ctl.isoRange
        return (r.lower * (r.upper.toDouble() / r.lower).pow(p / 1000.0)).roundToInt().coerceIn(r.lower, r.upper)
    }

    private fun isoToProg(iso: Int): Int {
        val r = ctl.isoRange
        if (r.upper <= r.lower) return 0
        return (ln(iso.toDouble() / r.lower) / ln(r.upper.toDouble() / r.lower) * 1000).roundToInt().coerceIn(0, 1000)
    }

    private fun progToShutter(p: Int): Long {
        val mn = ctl.shutterMin.toDouble()
        val mx = maxOf(ctl.shutterMax.toDouble(), mn)
        return (mn * (mx / mn).pow(p / 1000.0)).toLong().coerceIn(ctl.shutterMin, mx.toLong())
    }

    private fun fmtShutter(ns: Long) =
        if (ns >= 500_000_000L) String.format(Locale.US, "%.1fs", ns / 1e9) else "1/${(1e9 / ns).roundToInt()}"
}
