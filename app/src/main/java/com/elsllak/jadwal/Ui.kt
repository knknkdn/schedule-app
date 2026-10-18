package com.elsllak.jadwal

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** Colours for the light and dark looks. */
object Pal {
    @JvmField var dark = false
    @JvmField var bg = 0; @JvmField var card = 0; @JvmField var fg = 0; @JvmField var muted = 0
    @JvmField var line = 0; @JvmField var accent = 0; @JvmField var accentFg = 0
    @JvmField var danger = 0; @JvmField var soft = 0; @JvmField var field = 0

    val courseColors = intArrayOf(
        0xFF0E7C70.toInt(), 0xFF2F6FED.toInt(), 0xFFD9822B.toInt(), 0xFF8E44AD.toInt(),
        0xFFC0392B.toInt(), 0xFF2E8B57.toInt(), 0xFFC2185B.toInt(), 0xFF5D6D7E.toInt()
    )

    fun resolve(ctx: Context, theme: String) {
        dark = when (theme) {
            "dark" -> true
            "light" -> false
            else -> (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        if (dark) {
            bg = 0xFF0A0D12.toInt(); card = 0xFF131923.toInt(); fg = 0xFFEAF0F6.toInt(); muted = 0xFF93A1B0.toInt()
            line = 0xFF222B38.toInt(); accent = 0xFF2BC4B0.toInt(); accentFg = 0xFF04201C.toInt()
            danger = 0xFFFF6B5E.toInt(); soft = 0xFF16322F.toInt(); field = 0xFF0F141C.toInt()
        } else {
            bg = 0xFFF2F5F7.toInt(); card = 0xFFFFFFFF.toInt(); fg = 0xFF0F1720.toInt(); muted = 0xFF5B6672.toInt()
            line = 0xFFE0E6EB.toInt(); accent = 0xFF0E7C70.toInt(); accentFg = 0xFFFFFFFF.toInt()
            danger = 0xFFC0392B.toInt(); soft = 0xFFE0F1EE.toInt(); field = 0xFFF7F9FA.toInt()
        }
    }
}

object Fonts {
    private var reg: Typeface? = null
    private var bold: Typeface? = null
    private var reem: Typeface? = null
    fun regular(c: Context): Typeface = reg ?: load(c, "plex_regular").also { reg = it }
    fun bold(c: Context): Typeface = bold ?: load(c, "plex_bold").also { bold = it }
    fun display(c: Context): Typeface = reem ?: load(c, "reem").also { reem = it }
    private fun load(c: Context, n: String): Typeface =
        try { Typeface.createFromAsset(c.assets, "fonts/$n.ttf") } catch (e: Exception) { Typeface.DEFAULT }
}

fun Context.dp(x: Int): Int = (x * resources.displayMetrics.density + 0.5f).toInt()
fun Context.dpf(x: Float): Float = x * resources.displayMetrics.density

fun rounded(color: Int, radius: Float, stroke: Int = 0, strokeW: Float = 0f): GradientDrawable =
    GradientDrawable().apply {
        setColor(color); cornerRadius = radius
        if (stroke != 0) setStroke(strokeW.toInt().coerceAtLeast(1), stroke)
    }

fun ripple(base: android.graphics.drawable.Drawable, color: Int): android.graphics.drawable.Drawable =
    RippleDrawable(ColorStateList.valueOf(color), base, null)

object Ui {
    fun lp(w: Int, h: Int) = LinearLayout.LayoutParams(w, h)
    const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    fun tv(c: Context, text: String, sp: Float = 15f, bold: Boolean = false, color: Int = Pal.fg): TextView =
        TextView(c).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
            setTextColor(color)
            typeface = if (bold) Fonts.bold(c) else Fonts.regular(c)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            includeFontPadding = false
            setLineSpacing(0f, 1.15f)
        }

    fun title(c: Context, text: String) = tv(c, text, 22f, true).apply { typeface = Fonts.display(c) }

    fun vbox(c: Context): LinearLayout = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
    fun hbox(c: Context): LinearLayout = LinearLayout(c).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
    }

    fun space(c: Context, h: Int): View = View(c).apply { layoutParams = lp(1, c.dp(h)) }

    fun card(c: Context, pad: Int = 16): LinearLayout = vbox(c).apply {
        background = rounded(Pal.card, c.dpf(20f), Pal.line, c.dpf(1f))
        setPadding(c.dp(pad), c.dp(pad), c.dp(pad), c.dp(pad))
        elevation = if (Pal.dark) 0f else c.dpf(2f)
    }

    fun button(c: Context, text: String, primary: Boolean = true, danger: Boolean = false, onClick: () -> Unit): TextView {
        val bgc = when { danger -> Pal.danger; primary -> Pal.accent; else -> Pal.card }
        val fgc = when { danger -> 0xFFFFFFFF.toInt(); primary -> Pal.accentFg; else -> Pal.fg }
        return tv(c, text, 15f, true, fgc).apply {
            gravity = Gravity.CENTER
            minHeight = c.dp(50)
            setPadding(c.dp(18), c.dp(12), c.dp(18), c.dp(12))
            background = ripple(rounded(bgc, c.dpf(16f), if (primary || danger) 0 else Pal.line, c.dpf(1f)), 0x33888888)
            setOnClickListener { onClick() }
            Fx.press(this)
        }
    }

    fun field(c: Context, hint: String, value: String = "", inputType: Int = InputType.TYPE_CLASS_TEXT): EditText =
        EditText(c).apply {
            setHint(hint); setText(value)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(Pal.fg); setHintTextColor(Pal.muted)
            typeface = Fonts.regular(c)
            this.inputType = inputType
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            minHeight = c.dp(50)
            setSingleLine(true)
            setPadding(c.dp(14), c.dp(10), c.dp(14), c.dp(10))
            background = rounded(Pal.field, c.dpf(14f), Pal.line, c.dpf(1f))
            highlightColor = (Pal.accent and 0x00FFFFFF) or 0x55000000
            // cursor colour
            try {
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    textCursorDrawable = rounded(Pal.accent, 2f).apply { setSize(c.dp(2), c.dp(22)) }
                }
            } catch (_: Throwable) {}
        }

    fun label(c: Context, text: String, optional: Boolean = false): TextView {
        val t = if (optional) text + "  " + L.t("(اختياري)", "(optional)") else text
        return tv(c, t, 13.5f, true, Pal.muted).apply { setPadding(0, c.dp(14), 0, c.dp(6)) }
    }

    /** A tappable field showing a value; opens a custom picker. */
    fun pickerField(c: Context, text: String, placeholder: Boolean, onClick: () -> Unit): TextView =
        tv(c, text, 15f, false, if (placeholder) Pal.muted else Pal.fg).apply {
            minHeight = c.dp(50)
            setPadding(c.dp(14), c.dp(10), c.dp(14), c.dp(10))
            background = ripple(rounded(Pal.field, c.dpf(14f), Pal.line, c.dpf(1f)), 0x33888888)
            setOnClickListener { onClick() }
            Fx.press(this)
        }

    fun icon(c: Context, res: Int, tint: Int = Pal.fg, size: Int = 24): ImageView = ImageView(c).apply {
        setImageResource(res); setColorFilter(tint)
        layoutParams = LinearLayout.LayoutParams(c.dp(size), c.dp(size))
    }

    fun iconButton(c: Context, res: Int, tint: Int = Pal.fg, onClick: () -> Unit): FrameLayout = FrameLayout(c).apply {
        layoutParams = LinearLayout.LayoutParams(c.dp(46), c.dp(46))
        background = ripple(rounded(0x00000000, c.dpf(14f)), 0x33888888)
        addView(ImageView(c).apply { setImageResource(res); setColorFilter(tint) },
            FrameLayout.LayoutParams(c.dp(24), c.dp(24), Gravity.CENTER))
        setOnClickListener { onClick() }
    }

    fun chip(c: Context, text: String, bgc: Int, fgc: Int): TextView = tv(c, text, 12.5f, true, fgc).apply {
        gravity = Gravity.CENTER
        setPadding(c.dp(10), c.dp(5), c.dp(10), c.dp(5))
        background = rounded(bgc, c.dpf(10f))
    }

    fun segmented(c: Context, items: List<Pair<String, String>>, cur: String, onPick: (String) -> Unit): LinearLayout =
        hbox(c).apply {
            background = rounded(Pal.field, c.dpf(14f), Pal.line, c.dpf(1f))
            setPadding(c.dp(4), c.dp(4), c.dp(4), c.dp(4))
            for ((k, label) in items) {
                val on = k == cur
                addView(tv(c, label, 14f, true, if (on) Pal.accentFg else Pal.fg).apply {
                    gravity = Gravity.CENTER
                    minHeight = c.dp(42)
                    background = if (on) rounded(Pal.accent, c.dpf(11f)) else null
                    setOnClickListener { onPick(k) }
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }
        }

    fun switchRow(c: Context, text: String, on: Boolean, onToggle: (Boolean) -> Unit): LinearLayout = hbox(c).apply {
        minimumHeight = c.dp(48)
        addView(tv(c, text, 15f, true), LinearLayout.LayoutParams(0, WRAP, 1f))
        val track = FrameLayout(c)
        val thumb = View(c)
        fun paint(v: Boolean) {
            track.background = rounded(if (v) Pal.accent else Pal.line, c.dpf(16f))
            thumb.background = rounded(if (v) Pal.accentFg else Pal.muted, c.dpf(11f))
            (thumb.layoutParams as FrameLayout.LayoutParams).gravity = (if (v) Gravity.END else Gravity.START) or Gravity.CENTER_VERTICAL
            thumb.requestLayout()
        }
        track.addView(thumb, FrameLayout.LayoutParams(c.dp(22), c.dp(22)).apply {
            marginStart = c.dp(5); marginEnd = c.dp(5)
        })
        addView(track, LinearLayout.LayoutParams(c.dp(52), c.dp(32)))
        var cur = on
        paint(cur)
        setOnClickListener { cur = !cur; paint(cur); onToggle(cur) }
    }
}
