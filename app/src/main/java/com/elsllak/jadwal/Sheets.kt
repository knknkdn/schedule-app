package com.elsllak.jadwal

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.time.YearMonth

class MaxScroll(c: Context, private val maxH: Int) : ScrollView(c) {
    override fun onMeasure(w: Int, h: Int) {
        super.onMeasure(w, MeasureSpec.makeMeasureSpec(maxH, MeasureSpec.AT_MOST))
    }
}

/** Bottom sheets and the pickers made from them. All drawn by the app, not the system. */
object Sheets {
    fun open(act: MainActivity, build: (box: LinearLayout, dismiss: () -> Unit) -> Unit) {
        val c = act
        val scrim = FrameLayout(c).apply { setBackgroundColor(0x99000000.toInt()); isClickable = true }
        val box = Ui.vbox(c).apply {
            background = GradientDrawable().apply {
                setColor(Pal.card); val r = c.dpf(26f); cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
            setPadding(c.dp(20), c.dp(10), c.dp(20), c.dp(20))
            isClickable = true
        }
        val grip = View(c).apply { background = rounded(Pal.line, c.dpf(3f)) }
        box.addView(grip, LinearLayout.LayoutParams(c.dp(44), c.dp(5)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = c.dp(12) })
        val host = Ui.vbox(c)
        val maxH = (c.resources.displayMetrics.heightPixels * 0.78f).toInt()
        val sv = MaxScroll(c, maxH).apply { isVerticalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER; addView(host) }
        box.addView(sv, LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP))
        scrim.addView(box, FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP, Gravity.BOTTOM))
        var closed = false
        lateinit var dismiss: () -> Unit
        dismiss = {
            if (!closed) {
                closed = true
                act.sheetStack.remove(dismiss)
                box.animate().translationY(box.height.toFloat() + 40f).setDuration(180).start()
                scrim.animate().alpha(0f).setDuration(180).withEndAction { (scrim.parent as? ViewGroup)?.removeView(scrim) }.start()
            }
        }
        scrim.setOnClickListener { dismiss() }
        build(host, dismiss)
        act.layer.addView(scrim, FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH))
        act.sheetStack.add(dismiss)
        box.visibility = View.INVISIBLE
        box.post {
            box.translationY = box.height.toFloat() + 40f
            box.visibility = View.VISIBLE
            ObjectAnimator.ofFloat(box, "translationY", box.translationY, 0f).setDuration(240).start()
        }
        scrim.alpha = 0f; scrim.animate().alpha(1f).setDuration(200).start()
    }

    fun confirm(act: MainActivity, title: String, msg: String, okText: String, danger: Boolean, onOk: () -> Unit) {
        open(act) { box, dismiss ->
            box.addView(Ui.tv(act, title, 19f, true).apply { setPadding(0, 0, 0, act.dp(8)) })
            box.addView(Ui.tv(act, msg, 15f, false, Pal.muted).apply { setPadding(0, 0, 0, act.dp(18)) })
            val row = Ui.hbox(act)
            row.addView(Ui.button(act, okText, true, danger) { dismiss(); onOk() }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
            row.addView(View(act), LinearLayout.LayoutParams(act.dp(10), 1))
            row.addView(Ui.button(act, L.t("إلغاء", "Cancel"), false) { dismiss() }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
            box.addView(row)
        }
    }

    fun pickList(act: MainActivity, title: String, options: List<String>, cur: Int?, onPick: (Int) -> Unit) {
        open(act) { box, dismiss ->
            box.addView(Ui.tv(act, title, 19f, true).apply { setPadding(0, 0, 0, act.dp(10)) })
            for ((i, o) in options.withIndex()) {
                val on = cur == i
                box.addView(Ui.tv(act, o, 16f, on, if (on) Pal.accentFg else Pal.fg).apply {
                    minHeight = act.dp(50)
                    setPadding(act.dp(16), act.dp(10), act.dp(16), act.dp(10))
                    background = if (on) rounded(Pal.accent, act.dpf(14f)) else ripple(rounded(0, act.dpf(14f)), 0x33888888)
                    setOnClickListener { dismiss(); onPick(i) }
                }, LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP).apply { bottomMargin = act.dp(4) })
            }
        }
    }

    /** Hour and minute wheels (plus AM/PM in 12-hour mode). Calls back with minutes since midnight. */
    fun pickTime(act: MainActivity, title: String, cur: Int?, onPick: (Int) -> Unit) {
        open(act) { box, dismiss ->
            val t12 = L.t12
            val start = cur ?: 8 * 60 + 30
            val h = start / 60; val m = start % 60
            box.addView(Ui.tv(act, title, 19f, true).apply { setPadding(0, 0, 0, act.dp(8)) })
            val wheels = Ui.hbox(act).apply { layoutDirection = View.LAYOUT_DIRECTION_LTR; gravity = Gravity.CENTER }
            val hours = if (t12) (1..12).map { it.toString() } else (0..23).map { String.format("%02d", it) }
            val hw = WheelView(act, hours, if (t12) (if (h % 12 == 0) 12 else h % 12) - 1 else h)
            val mw = WheelView(act, (0..59).map { String.format("%02d", it) }, m)
            val aw = WheelView(act, listOf(L.t("ص", "AM"), L.t("م", "PM")), if (h < 12) 0 else 1)
            val band = FrameLayout(act)
            val bandBg = View(act).apply { background = rounded(Pal.soft, act.dpf(14f)) }
            band.addView(bandBg, FrameLayout.LayoutParams(Ui.MATCH, act.dp(46), Gravity.CENTER_VERTICAL))
            band.addView(wheels, FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP, Gravity.CENTER))
            wheels.addView(hw, LinearLayout.LayoutParams(act.dp(84), Ui.WRAP))
            wheels.addView(Ui.tv(act, ":", 24f, true, Pal.accent).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(act.dp(16), Ui.WRAP))
            wheels.addView(mw, LinearLayout.LayoutParams(act.dp(84), Ui.WRAP))
            if (t12) wheels.addView(aw, LinearLayout.LayoutParams(act.dp(84), Ui.WRAP).apply { marginStart = act.dp(10) })
            box.addView(band, LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP).apply { bottomMargin = act.dp(14) })
            box.addView(Ui.button(act, L.t("تم", "Done")) {
                val mm = mw.index
                val hh = if (t12) {
                    val base = (hw.index + 1) % 12
                    base + if (aw.index == 1) 12 else 0
                } else hw.index
                dismiss(); onPick(hh * 60 + mm)
            })
        }
    }

    /** A month grid that starts on Saturday, like the timetable. */
    fun pickDate(act: MainActivity, title: String, cur: String, allowClear: Boolean, onPick: (String) -> Unit) {
        open(act) { box, dismiss ->
            var ym = YearMonth.from(Dt.parse(cur) ?: Dt.today())
            var sel = cur
            box.addView(Ui.tv(act, title, 19f, true).apply { setPadding(0, 0, 0, act.dp(8)) })
            val head = Ui.hbox(act)
            val monthT = Ui.tv(act, "", 17f, true).apply { gravity = Gravity.CENTER }
            val grid = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; tag = "grid" }
            lateinit var redraw: () -> Unit
            redraw = {
                monthT.text = L.month(ym.monthValue) + " " + ym.year
                grid.removeAllViews()
                val wd = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
                for (i in 0 until 7) wd.addView(Ui.tv(act, L.wd(i), 12.5f, true, Pal.muted).apply { gravity = Gravity.CENTER; setPadding(0, act.dp(6), 0, act.dp(6)) }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
                grid.addView(wd)
                val blanks = Dt.absDay(ym.atDay(1))
                val total = ym.lengthOfMonth()
                val today = Dt.todayS()
                val cells = blanks + total
                val rows = (cells + 6) / 7
                for (r in 0 until rows) {
                    val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
                    for (i in 0 until 7) {
                        val d = r * 7 + i - blanks + 1
                        val lpp = LinearLayout.LayoutParams(0, act.dp(44), 1f).apply { setMargins(act.dp(2), act.dp(2), act.dp(2), act.dp(2)) }
                        if (d < 1 || d > total) { row.addView(View(act), lpp); continue }
                        val ds = ym.atDay(d).toString()
                        val on = ds == sel
                        row.addView(Ui.tv(act, d.toString(), 15f, on || ds == today, if (on) Pal.accentFg else Pal.fg).apply {
                            gravity = Gravity.CENTER
                            background = if (on) rounded(Pal.accent, act.dpf(14f)) else if (ds == today) rounded(0, act.dpf(14f), Pal.accent, act.dpf(1.5f)) else null
                            setOnClickListener { sel = ds; redraw() }
                        }, lpp)
                    }
                    grid.addView(row)
                }
            }
            head.removeAllViews()
            val pv = Ui.iconButton(act, R.drawable.ic_back, Pal.fg) { ym = ym.minusMonths(1); redraw() }
            val nx = Ui.iconButton(act, R.drawable.ic_back, Pal.fg) { ym = ym.plusMonths(1); redraw() }
            // chevron icon points toward the end side; flip so "previous" points start-ward
            pv.rotation = if (L.isAr) 0f else 180f
            nx.rotation = if (L.isAr) 180f else 0f
            head.addView(pv); head.addView(monthT, LinearLayout.LayoutParams(0, Ui.WRAP, 1f)); head.addView(nx)
            box.addView(head)
            box.addView(grid)
            redraw()
            val row = Ui.hbox(act).apply { setPadding(0, act.dp(12), 0, 0) }
            row.addView(Ui.button(act, L.t("تم", "Done")) { if (sel.isNotEmpty()) { dismiss(); onPick(sel) } }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
            row.addView(View(act), LinearLayout.LayoutParams(act.dp(8), 1))
            row.addView(Ui.button(act, L.t("اليوم", "Today"), false) { sel = Dt.todayS(); ym = YearMonth.from(Dt.today()); redraw() }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
            if (allowClear) {
                row.addView(View(act), LinearLayout.LayoutParams(act.dp(8), 1))
                row.addView(Ui.button(act, L.t("مسح", "Clear"), false) { dismiss(); onPick("") }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
            }
            box.addView(row)
        }
    }
}
