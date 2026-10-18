package com.elsllak.jadwal

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    lateinit var root: FrameLayout
    lateinit var bg: Background3D
    lateinit var body: FrameLayout
    lateinit var layer: FrameLayout
    val sheetStack = ArrayList<() -> Unit>()
    var drawerClose: (() -> Unit)? = null
    var view = "main"
    var ret = "main"
    var splash: SplashView? = null
    private val ui = Handler(Looper.getMainLooper())
    lateinit var screens: Screens
    var pickedImage: ((Uri) -> Unit)? = null
    var scroller: ScrollView? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        Store.load(this)
        screens = Screens(this)
        Pal.resolve(this, G.p.theme)
        look()
        root = FrameLayout(this)
        root.setBackgroundColor(0xFF0A0D12.toInt())
        setContentView(root)
        // Welcome screen first, so something shows at once.
        val sp = SplashView(this)
        splash = sp
        root.addView(sp, FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH))
        sp.onDone = { finishSplash() }
        sp.start()
        root.post { buildApp() }
        root.postDelayed({ schedSoon() }, 600)
    }

    private fun finishSplash() {
        val sp = splash ?: return
        splash = null
        sp.animate().alpha(0f).setDuration(320).withEndAction { root.removeView(sp) }.start()
        askNotifPermission()
    }

    /** Colours of the status and navigation bars, and the text direction. */
    private fun look() {
        window.statusBarColor = Pal.bg
        window.navigationBarColor = Pal.bg
        var f = 0
        if (!Pal.dark) f = f or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or (if (Build.VERSION.SDK_INT >= 26) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0)
        window.decorView.systemUiVisibility = f
        window.decorView.layoutDirection = if (L.isAr) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Pal.bg))
    }

    private fun buildApp() {
        bg = Background3D(this)
        body = FrameLayout(this)
        layer = FrameLayout(this)
        root.addView(bg, 0, FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH))
        root.addView(body, 1, FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH))
        root.addView(layer, 2, FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH))
        render()
        bg.start()
    }

    /** Re-applies language, theme and effects, then redraws everything. */
    fun applyLook() {
        L.lang = G.p.lang; L.t12 = G.p.t12; Fx.enabled = G.p.fx
        Pal.resolve(this, G.p.theme)
        look()
        layer.removeAllViews(); sheetStack.clear(); drawerClose = null
        if (G.p.fx) bg.start() else bg.stop()
        bg.invalidate()
        render()
    }

    fun go(v: String) {
        closeLayers()
        view = v
        render()
    }

    /** Redraws the current screen and keeps the scroll position. */
    fun refresh() {
        val y = scroller?.scrollY ?: 0
        render(false)
        scroller?.post { scroller?.scrollTo(0, y) }
    }

    fun render(anim: Boolean = true) {
        body.removeAllViews()
        val v = when (view) {
            "exams" -> screens.exams()
            "xform" -> screens.examForm()
            "ocr" -> screens.ocr()
            "course" -> screens.courseForm()
            "wizard" -> screens.wizard()
            "notif" -> screens.notif()
            "settings" -> screens.settings()
            else -> screens.main()
        }
        body.addView(v, FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH))
        if (Fx.enabled && anim) {
            v.alpha = 0f; v.translationY = dp(14).toFloat()
            v.animate().alpha(1f).translationY(0f).setDuration(200).start()
        }
    }

    fun closeLayers() {
        while (sheetStack.isNotEmpty()) sheetStack.last().invoke()
        drawerClose?.invoke()
    }

    /** Screen frame: a top bar and a scrolling column. */
    fun page(title: String, sub: String = "", back: String? = null, fill: (LinearLayout) -> Unit): View {
        val frame = Ui.vbox(this)
        val bar = Ui.hbox(this).apply { setPadding(dp(10), dp(8), dp(10), dp(4)) }
        if (back != null) {
            bar.addView(Ui.iconButton(this, R.drawable.ic_back, Pal.fg) { go(back) }.apply { rotation = if (L.isAr) 0f else 180f })
        } else {
            bar.addView(Ui.iconButton(this, R.drawable.ic_menu, Pal.fg) { Drawer.open(this) })
        }
        val tb = Ui.vbox(this).apply { setPadding(dp(8), 0, dp(8), 0) }
        tb.addView(Ui.title(this, title))
        if (sub.isNotEmpty()) tb.addView(Ui.tv(this, sub, 13f, false, Pal.muted).apply { setPadding(0, dp(2), 0, 0) })
        bar.addView(tb, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
        if (back == null) bar.addView(android.widget.ImageView(this).apply { setImageResource(R.drawable.logo_tile) }, LinearLayout.LayoutParams(dp(40), dp(40)))
        frame.addView(bar, LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP))
        val col = Ui.vbox(this).apply { setPadding(dp(16), dp(8), dp(16), dp(32)) }
        fill(col)
        val sv = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER; isFillViewport = false
            addView(col)
            clipToPadding = false
        }
        scroller = sv
        frame.addView(sv, LinearLayout.LayoutParams(Ui.MATCH, 0, 1f))
        return frame
    }

    fun toast(msg: String) {
        val t = Ui.tv(this, msg, 14f, true, if (Pal.dark) Pal.fg else 0xFFFFFFFF.toInt()).apply {
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(12), dp(18), dp(12))
            background = rounded(if (Pal.dark) 0xFF263241.toInt() else 0xFF1B2530.toInt(), dpf(16f))
            elevation = dpf(6f)
        }
        val lp = FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(40); marginStart = dp(24); marginEnd = dp(24) }
        layer.addView(t, lp)
        t.alpha = 0f
        t.animate().alpha(1f).translationY(-dp(6).toFloat()).setDuration(180).withEndAction {
            t.animate().alpha(0f).setStartDelay(2000).setDuration(260).withEndAction { layer.removeView(t) }.start()
        }.start()
    }

    /** Called whenever courses, exams or the schedule change. */
    fun changed() {
        Store.saveState(this)
        schedSoon()
    }

    fun savePrefs() {
        Store.savePrefs(this)
        schedSoon()
    }

    private val reschedule = Runnable { try { Notifier.reschedule(this) } catch (e: Throwable) {} }

    /** Waits a moment so quick edits cause a single rescheduling. */
    fun schedSoon() {
        ui.removeCallbacks(reschedule)
        ui.postDelayed(reschedule, 500)
    }

    fun askNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
        }
    }

    fun notifAllowed(): Boolean =
        Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(code: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(code, p, r)
        if (view == "notif") render()
    }

    fun pickImage(cb: (Uri) -> Unit) {
        pickedImage = cb
        val i = Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(i, null), 11)
    }

    override fun onActivityResult(code: Int, res: Int, data: Intent?) {
        super.onActivityResult(code, res, data)
        if (code == 11 && res == RESULT_OK) data?.data?.let { pickedImage?.invoke(it) }
    }

    override fun onResume() {
        super.onResume()
        if (::bg.isInitialized && Fx.enabled) bg.start()
        if (::body.isInitialized && view == "notif") render()
    }

    override fun onPause() {
        super.onPause()
        if (::bg.isInitialized) bg.stop()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (sheetStack.isNotEmpty()) { sheetStack.last().invoke(); return }
        if (drawerClose != null) { drawerClose?.invoke(); return }
        if (splash != null) return
        when (view) {
            "main" -> super.onBackPressed()
            "xform", "ocr" -> go("exams")
            else -> go("main")
        }
    }
}
