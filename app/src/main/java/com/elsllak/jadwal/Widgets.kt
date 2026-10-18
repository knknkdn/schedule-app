package com.elsllak.jadwal

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Small touch effects. */
object Fx {
    @JvmField var enabled = true

    fun press(v: View) {
        v.setOnTouchListener { view, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> view.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
            }
            false
        }
    }

    /** Tilts a card toward the finger. Does not consume the touch. */
    fun tilt(v: View) {
        v.setOnTouchListener { view, ev ->
            if (enabled) {
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        val nx = ((ev.x / max(1, view.width)) - 0.5f).coerceIn(-0.5f, 0.5f)
                        val ny = ((ev.y / max(1, view.height)) - 0.5f).coerceIn(-0.5f, 0.5f)
                        view.cameraDistance = view.resources.displayMetrics.density * 9000f
                        view.rotationY = nx * 10f
                        view.rotationX = -ny * 10f
                        view.scaleX = 0.985f; view.scaleY = 0.985f
                        // A card that is not clickable never gets the lift event, so let go by itself.
                        if (ev.actionMasked == MotionEvent.ACTION_DOWN && !view.isClickable)
                            view.postDelayed({ view.animate().rotationX(0f).rotationY(0f).scaleX(1f).scaleY(1f).setDuration(220).start() }, 260)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        view.animate().rotationX(0f).rotationY(0f).scaleX(1f).scaleY(1f).setDuration(220).start()
                }
            }
            false
        }
    }
}

/** Soft depth background that shifts with the phone's tilt. */
class Background3D(ctx: Context) : View(ctx), SensorEventListener {
    private val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val acc: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var tx = 0f; private var ty = 0f
    private var cx = 0f; private var cy = 0f
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 18000; repeatCount = ValueAnimator.INFINITE; interpolator = android.view.animation.LinearInterpolator()
        addUpdateListener { phase = it.animatedValue as Float; smooth(); invalidate() }
    }
    private var running = false

    fun start() {
        if (running || !Fx.enabled) return
        running = true
        acc?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        anim.start()
    }

    fun stop() {
        if (!running) return
        running = false
        sm.unregisterListener(this)
        anim.cancel()
    }

    private fun smooth() { cx += (tx - cx) * 0.08f; cy += (ty - cy) * 0.08f }

    override fun onSensorChanged(e: SensorEvent) {
        tx = (-e.values[0] / 9.8f).coerceIn(-1f, 1f)
        ty = (e.values[1] / 9.8f - 0.6f).coerceIn(-1f, 1f)
    }
    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        c.drawColor(Pal.bg)
        if (!Fx.enabled) return
        val a = Pal.accent
        val dark = Pal.dark
        val ang = (phase * Math.PI * 2).toFloat()
        fun blob(x: Float, y: Float, r: Float, color: Int, alpha: Int, depth: Float) {
            val px = x + cx * depth * 40f + Math.sin(ang.toDouble() + depth).toFloat() * 10f
            val py = y + cy * depth * 40f + Math.cos(ang.toDouble() + depth).toFloat() * 10f
            p.shader = RadialGradient(px, py, r, (color and 0x00FFFFFF) or (alpha shl 24), color and 0x00FFFFFF, Shader.TileMode.CLAMP)
            c.drawCircle(px, py, r, p)
            p.shader = null
        }
        blob(w * 0.15f, h * 0.12f, w * 0.75f, a, if (dark) 70 else 46, 1.0f)
        blob(w * 0.95f, h * 0.45f, w * 0.65f, if (dark) 0xFF3B6FED.toInt() else 0xFF2F6FED.toInt(), if (dark) 50 else 28, 1.6f)
        blob(w * 0.2f, h * 0.95f, w * 0.7f, 0xFFD9822B.toInt(), if (dark) 36 else 24, 2.2f)
        // floating rounded squares for depth
        p.style = Paint.Style.STROKE
        p.strokeWidth = resources.displayMetrics.density * 1.5f
        p.color = (a and 0x00FFFFFF) or ((if (dark) 60 else 40) shl 24)
        val s = w * 0.22f
        for ((i, pos) in listOf(Pair(0.8f, 0.2f), Pair(0.12f, 0.6f), Pair(0.7f, 0.82f)).withIndex()) {
            val d = 1.2f + i * 0.8f
            val x = w * pos.first + cx * d * 30f
            val y = h * pos.second + cy * d * 30f
            c.save()
            c.rotate(20f * (i + 1) + cx * 8f, x, y)
            c.drawRoundRect(x - s / 2, y - s / 2, x + s / 2, y + s / 2, s * 0.25f, s * 0.25f, p)
            c.restore()
        }
        p.style = Paint.Style.FILL
    }
}

/** A drum-style picker column. */
class WheelView(ctx: Context, private val items: List<String>, start: Int) : View(ctx) {
    private val itemH = ctx.dp(46).toFloat()
    private var offset = start.toFloat()
    private val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = ctx.dpf(24f); typeface = Fonts.bold(ctx); textAlign = Paint.Align.CENTER
    }
    private var vt: VelocityTracker? = null
    private var lastY = 0f
    private var anim: ValueAnimator? = null
    private var moved = false
    var onChange: ((Int) -> Unit)? = null
    val index: Int get() = Math.round(offset).coerceIn(0, items.size - 1)

    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension(MeasureSpec.getSize(w).let { if (MeasureSpec.getMode(w) == MeasureSpec.UNSPECIFIED) context.dp(80) else it }, (itemH * 5).toInt())
    }

    override fun onDraw(c: Canvas) {
        val cy = height / 2f
        val first = max(0, (offset - 3).toInt())
        val last = min(items.size - 1, (offset + 3).toInt())
        for (i in first..last) {
            val d = i - offset
            val y = cy + d * itemH
            val k = min(1f, abs(d) / 2.6f)
            val scale = 1f - 0.28f * k
            paint.color = if (abs(d) < 0.5f) Pal.fg else Pal.muted
            paint.alpha = ((1f - 0.78f * k) * 255).toInt().coerceIn(30, 255)
            c.save()
            c.scale(1f, scale, width / 2f, y)
            c.drawText(items[i], width / 2f, y + paint.textSize * 0.36f, paint)
            c.restore()
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                anim?.cancel(); lastY = e.y; moved = false
                vt = VelocityTracker.obtain().also { it.addMovement(e) }
            }
            MotionEvent.ACTION_MOVE -> {
                vt?.addMovement(e)
                val dy = e.y - lastY; lastY = e.y
                if (abs(dy) > 0) moved = true
                offset = (offset - dy / itemH).coerceIn(-0.4f, items.size - 0.6f)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                vt?.computeCurrentVelocity(1000)
                val v = vt?.yVelocity ?: 0f
                vt?.recycle(); vt = null
                if (!moved && e.actionMasked == MotionEvent.ACTION_UP) {
                    val d = (e.y - height / 2f) / itemH
                    settle(Math.round(offset + d).toFloat())
                } else {
                    settle(Math.round(offset - v / itemH * 0.18f).toFloat())
                }
            }
        }
        return true
    }

    private fun settle(target: Float) {
        val t = target.coerceIn(0f, (items.size - 1).toFloat())
        anim?.cancel()
        anim = ValueAnimator.ofFloat(offset, t).apply {
            duration = 260; interpolator = DecelerateInterpolator()
            addUpdateListener { offset = it.animatedValue as Float; invalidate() }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: android.animation.Animator) { onChange?.invoke(index) }
            })
            start()
        }
    }
}

/** Day labels (pinned) for the timetable: one box per row. */
object TT {
    fun rowH(c: Context) = c.dp(86)
    fun headH(c: Context) = c.dp(54)
    fun colW(c: Context) = c.dp(108)
    fun dayW(c: Context) = c.dp(76)
}

class DayLabelsView(ctx: Context, private val days: List<Int>) : View(ctx) {
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Fonts.bold(ctx); textSize = ctx.dpf(13.5f); textAlign = Paint.Align.CENTER }
    private val bp = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(TT.dayW(context), TT.headH(context) + TT.rowH(context) * days.size)
    override fun onDraw(c: Canvas) {
        val hh = TT.headH(context).toFloat(); val rh = TT.rowH(context).toFloat()
        val pad = context.dpf(3f)
        for ((i, d) in days.withIndex()) {
            val top = hh + i * rh
            bp.color = Pal.soft
            c.drawRoundRect(pad, top + pad, width - pad, top + rh - pad, context.dpf(14f), context.dpf(14f), bp)
            tp.color = Pal.accent
            val name = L.day(d)
            val lay = StaticLayout.Builder.obtain(name, 0, name.length, tp, (width - pad * 4).toInt())
                .setAlignment(Layout.Alignment.ALIGN_CENTER).setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END).build()
            c.save(); c.translate(pad * 2, top + (rh - lay.height) / 2f); lay.draw(c); c.restore()
        }
    }
}

class TimetableView(ctx: Context, private val S: AppState, private val days: List<Int>, private val t12: Boolean) : View(ctx) {
    var onCourse: ((Course) -> Unit)? = null
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val bp = Paint(Paint.ANTI_ALIAS_FLAG)
    private class Hit(val r: RectF, val c: Course)
    private val hits = ArrayList<Hit>()
    private val mirror = L.isAr

    private fun totalW() = TT.colW(context) * S.cfg.slots.size
    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(totalW(), TT.headH(context) + TT.rowH(context) * days.size)

    private fun rx(l: Float, r: Float): Pair<Float, Float> = if (mirror) Pair(width - r, width - l) else Pair(l, r)

    override fun onDraw(c: Canvas) {
        hits.clear()
        val cw = TT.colW(context).toFloat(); val hh = TT.headH(context).toFloat(); val rh = TT.rowH(context).toFloat()
        val pad = context.dpf(3f)
        val rad = context.dpf(14f)
        // slot headers
        for ((i, sl) in S.cfg.slots.withIndex()) {
            val (l, r) = rx(i * cw + pad, (i + 1) * cw - pad)
            bp.color = Pal.card
            c.drawRoundRect(l, pad, r, hh - pad, rad, rad, bp)
            bp.style = Paint.Style.STROKE; bp.strokeWidth = context.dpf(1f); bp.color = Pal.line
            c.drawRoundRect(l, pad, r, hh - pad, rad, rad, bp); bp.style = Paint.Style.FILL
            tp.typeface = Fonts.bold(context); tp.textSize = context.dpf(13f); tp.color = Pal.fg; tp.textAlign = Paint.Align.CENTER
            val a = Dt.fmtT(sl.s, t12); val b = Dt.fmtT(sl.e, t12)
            c.drawText(a, (l + r) / 2, hh / 2 - context.dpf(3f), tp)
            tp.typeface = Fonts.regular(context); tp.color = Pal.muted; tp.textSize = context.dpf(11.5f)
            c.drawText(b, (l + r) / 2, hh / 2 + context.dpf(13f), tp)
        }
        // empty cells
        for (ri in days.indices) for (i in S.cfg.slots.indices) {
            val (l, r) = rx(i * cw + pad, (i + 1) * cw - pad)
            val top = hh + ri * rh + pad
            bp.color = if (Pal.dark) 0x14FFFFFF else 0x0A000000
            c.drawRoundRect(l, top, r, top + rh - pad * 2, rad, rad, bp)
        }
        // lectures
        for (course in S.courses) {
            val color = Pal.courseColors[course.color % 8]
            for (lec in course.lec) {
                val ri = days.indexOf(lec.d)
                if (ri < 0 || lec.s >= S.cfg.slots.size || lec.e >= S.cfg.slots.size) continue
                val (l, r) = rx(lec.s * cw + pad, (lec.e + 1) * cw - pad)
                val top = hh + ri * rh + pad
                val rect = RectF(l, top, r, top + rh - pad * 2)
                bp.color = color
                c.drawRoundRect(rect, rad, rad, bp)
                bp.shader = LinearGradient(0f, rect.top, 0f, rect.bottom, 0x30FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
                c.drawRoundRect(rect, rad, rad, bp); bp.shader = null
                tp.typeface = Fonts.bold(context); tp.color = Color.WHITE; tp.textSize = context.dpf(13.5f); tp.textAlign = Paint.Align.LEFT
                val inner = (rect.width() - context.dpf(18f)).toInt()
                val nameL = StaticLayout.Builder.obtain(course.name, 0, course.name.length, tp, inner)
                    .setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END)
                    .setAlignment(if (mirror) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL).build()
                c.save(); c.translate(rect.left + context.dpf(9f), rect.top + context.dpf(9f)); nameL.draw(c); c.restore()
                val sub = if (lec.room.isNotEmpty()) lec.room else course.code
                if (sub.isNotEmpty()) {
                    tp.typeface = Fonts.regular(context); tp.textSize = context.dpf(11.5f); tp.color = 0xDDFFFFFF.toInt()
                    val sl = StaticLayout.Builder.obtain(sub, 0, sub.length, tp, inner).setMaxLines(1).setEllipsize(TextUtils.TruncateAt.END)
                        .setAlignment(if (mirror) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL).build()
                    c.save(); c.translate(rect.left + context.dpf(9f), rect.bottom - sl.height - context.dpf(8f)); sl.draw(c); c.restore()
                }
                hits.add(Hit(rect, course))
            }
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_DOWN) return hits.any { it.r.contains(e.x, e.y) }
        if (e.actionMasked == MotionEvent.ACTION_UP) {
            hits.firstOrNull { it.r.contains(e.x, e.y) }?.let { performClick(); onCourse?.invoke(it.c) }
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}

/** Welcome screen drawn directly on a canvas, so it appears instantly. */
class SplashView(ctx: Context) : View(ctx) {
    private val logo: Bitmap? = try { BitmapFactory.decodeResource(resources, R.drawable.logo_tile) } catch (e: Throwable) { null }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private var t = 0f
    var onDone: (() -> Unit)? = null
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2900
        addUpdateListener { t = it.animatedValue as Float; invalidate() }
        addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(a: android.animation.Animator) { onDone?.invoke() }
        })
    }
    fun start() = anim.start()
    fun cancel() = anim.cancel()

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        p.shader = LinearGradient(0f, 0f, 0f, h, 0xFF0A0D12.toInt(), 0xFF0E1A1F.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, p); p.shader = null
        val cx = w / 2; val cy = h * 0.40f
        val intro = min(1f, t / 0.28f)
        val ease = 1f - (1f - intro) * (1f - intro) * (1f - intro)
        val size = context.dpf(132f) * (0.7f + 0.3f * ease)
        // rings
        p.style = Paint.Style.STROKE; p.strokeWidth = context.dpf(2f)
        for (i in 0..2) {
            val k = ((t * 1.6f + i / 3f) % 1f)
            val r = size * 0.62f + k * size * 0.9f
            p.color = (0x2BC4B0 and 0xFFFFFF) or (((1f - k) * 90 * ease).toInt() shl 24)
            c.drawCircle(cx, cy, r, p)
        }
        p.style = Paint.Style.FILL
        // glow
        p.shader = RadialGradient(cx, cy, size * 1.1f, 0x552BC4B0, 0x002BC4B0, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, size * 1.1f, p); p.shader = null
        // logo with a gentle 3D turn
        logo?.let {
            c.save()
            val cam = android.graphics.Camera()
            val m = Matrix()
            cam.save(); cam.rotateY((1f - ease) * 70f + Math.sin(t * 6.0).toFloat() * 4f); cam.getMatrix(m); cam.restore()
            m.preTranslate(-cx, -cy); m.postTranslate(cx, cy)
            c.concat(m)
            p.alpha = (ease * 255).toInt()
            c.drawBitmap(it, null, RectF(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2), p)
            p.alpha = 255
            c.restore()
        }
        val ta = (min(1f, max(0f, (t - 0.22f) / 0.25f)) * 255).toInt()
        tp.typeface = Fonts.display(context); tp.textSize = context.dpf(34f); tp.color = 0xFFEAF0F6.toInt(); tp.alpha = ta
        c.drawText(L.t("مرحبًا بك", "Welcome"), cx, cy + size * 0.5f + context.dpf(64f), tp)
        tp.typeface = Fonts.regular(context); tp.textSize = context.dpf(15f); tp.color = 0xFF93A1B0.toInt(); tp.alpha = ta
        c.drawText(L.t("نظّم محاضراتك بسهولة", "Organize your lectures with ease"), cx, cy + size * 0.5f + context.dpf(96f), tp)
        tp.typeface = Fonts.bold(context); tp.textSize = context.dpf(14f); tp.letterSpacing = 0.08f; tp.color = 0xFF2BC4B0.toInt(); tp.alpha = ta
        c.drawText("Mohammed Elsllak", cx, h - context.dpf(44f), tp)
        tp.letterSpacing = 0f
    }
}
