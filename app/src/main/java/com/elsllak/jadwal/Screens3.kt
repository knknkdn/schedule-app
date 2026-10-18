package com.elsllak.jadwal

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout

// ---------- reminders ----------
fun Screens.notif(): View = a.page(L.t("التنبيهات", "Reminders"), "", back = null) { col ->
    if (!a.notifAllowed()) {
        val c = Ui.card(a, 14)
        c.addView(Ui.tv(a, L.t("التطبيق غير مسموح له بإرسال الإشعارات. اسمح له ليصلك التنبيه.", "The app is not allowed to send notifications. Allow it so reminders reach you."), 14.5f, true, Pal.danger))
        c.addView(Ui.button(a, L.t("السماح بالإشعارات", "Allow notifications")) { a.askNotifPermission() }, lp(12))
        col.addView(c, lp())
    }
    if (!Notifier.canExact(a)) {
        val c = Ui.card(a, 14)
        c.addView(Ui.tv(a, L.t("لتصل التنبيهات في وقتها بالدقيقة، اسمح للتطبيق بالمنبهات الدقيقة من إعدادات الهاتف.", "To get reminders on the exact minute, allow the app to use exact alarms in the phone settings."), 14.5f, false, Pal.muted))
        c.addView(Ui.button(a, L.t("فتح الإعداد", "Open setting"), false) {
            try {
                if (Build.VERSION.SDK_INT >= 31) a.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + a.packageName)))
            } catch (e: Exception) {}
        }, lp(12))
        col.addView(c, lp(10))
    }
    offPanel(col, "lec", L.t("تنبيهات المحاضرات", "Lecture reminders"), L.t("يصلك إشعار قبل كل محاضرة في كل أسبوع.", "You get a notification before every lecture, every week."))
    offPanel(col, "exam", L.t("تنبيهات الامتحانات", "Exam reminders"), L.t("يصلك إشعار قبل كل امتحان له وقت. الامتحان الذي له تاريخ فقط ينبّهك مساء اليوم السابق.", "You get a notification before each exam that has a time. An exam with only a date reminds you the evening before."))
    col.addView(Ui.button(a, L.t("إرسال تنبيه تجريبي بعد 5 ثوانٍ", "Send a test reminder in 5 seconds"), false) {
        if (!a.notifAllowed()) a.askNotifPermission() else { Notifier.test(a); a.toast(L.t("سيصلك التنبيه بعد 5 ثوانٍ. اخرج من التطبيق لترى الإشعار.", "The reminder arrives in 5 seconds. Leave the app to see the notification.")) }
    }, lp(16))
}

private val offInput = HashMap<String, String>()
private val offUnit = HashMap<String, String>()

private fun Screens.offPanel(col: LinearLayout, k: String, title: String, hint: String) {
    val n = if (k == "lec") G.p.lec else G.p.exam
    val c = Ui.card(a, 16)
    c.addView(Ui.switchRow(a, title, n.on) { v -> n.on = v; a.savePrefs() })
    c.addView(Ui.tv(a, hint, 13.5f, false, Pal.muted).apply { setPadding(0, a.dp(2), 0, a.dp(10)) })
    // offsets
    if (n.offs.isEmpty()) c.addView(Ui.tv(a, L.t("لا توجد أوقات تنبيه. أضف وقتًا.", "No reminder times. Add one."), 13.5f, true, Pal.danger))
    val flow = FlowLayout(a)
    for (o in n.offs.sorted()) {
        val label = if (o == 0) L.t("عند البدء", "At start") else L.t("قبل " + L.dur(o), L.dur(o) + " before")
        flow.addView(Ui.tv(a, "$label  ×", 13.5f, true, Pal.accent).apply {
            setPadding(a.dp(12), a.dp(9), a.dp(12), a.dp(9)); background = rounded(Pal.soft, a.dpf(12f))
            setOnClickListener { n.offs.remove(o); a.savePrefs(); a.refresh() }
        })
    }
    c.addView(flow)
    c.addView(Ui.label(a, L.t("إضافة وقت تنبيه", "Add a reminder time")))
    val unit = offUnit[k] ?: "m"
    c.addView(Ui.segmented(a, listOf("m" to L.t("دقيقة", "minutes"), "h" to L.t("ساعة", "hours"), "d" to L.t("يوم", "days")), unit) { offUnit[k] = it; a.refresh() })
    val ar = Ui.hbox(a)
    val inp = Ui.field(a, "30", offInput[k] ?: "", InputType.TYPE_CLASS_NUMBER)
    watch(inp) { offInput[k] = it }
    ar.addView(inp, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
    ar.addView(View(a), LinearLayout.LayoutParams(a.dp(8), 1))
    ar.addView(Ui.button(a, L.t("إضافة", "Add"), false) {
        val v = (offInput[k] ?: "").toIntOrNull()
        if (v == null || v <= 0) { a.toast(L.t("اكتب رقمًا صحيحًا أكبر من صفر.", "Enter a whole number greater than zero.")); return@button }
        val mins = v * (when (unit) { "h" -> 60; "d" -> 1440; else -> 1 })
        if (mins > 60 * 24 * 60) { a.toast(L.t("القيمة كبيرة جدًا.", "That value is too large.")); return@button }
        if (!n.offs.contains(mins)) n.offs.add(mins)
        offInput[k] = ""; a.savePrefs(); a.refresh()
    }, LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP))
    c.addView(ar, lp(8))
    // reason text
    c.addView(Ui.label(a, L.t("نص الإشعار (السبب)", "Notification text (the reason)")))
    c.addView(Ui.segmented(a, listOf("auto" to L.t("تلقائي", "Automatic"), "custom" to L.t("نص مخصص", "Custom text")), if (n.custom) "custom" else "auto") { n.custom = it == "custom"; a.savePrefs(); a.refresh() })
    if (n.custom) {
        c.addView(Ui.field(a, L.t("مثال: استعد، {name} قريبًا", "Example: Get ready, {name} soon"), n.text).also { f -> watch(f) { v -> n.text = v; a.savePrefs() } }, lp(8))
        c.addView(Ui.tv(a, L.t("اكتب {name} ليُستبدل باسم المادة، و{when} ليُستبدل بالمدة المتبقية.", "Write {name} for the course name and {when} for the time left."), 12.5f, false, Pal.muted).apply { setPadding(0, a.dp(6), 0, 0) })
    } else {
        val ex = if (k == "lec") Plan.lecTitle(L.t("الرياضيات", "Math"), L.dur(30)) else Plan.examTitle(L.t("الرياضيات", "Math"), L.typeLbl("mid"), L.dur(60))
        c.addView(Ui.tv(a, L.t("مثال: ", "Example: ") + ex, 13.5f, false, Pal.muted).apply { setPadding(0, a.dp(8), 0, 0) })
    }
    col.addView(c, lp(12))
}

// ---------- settings ----------
fun Screens.settings(): View = a.page(L.t("الإعدادات", "Settings"), "", back = null) { col ->
    val p = G.p
    fun sect(t: String) = col.addView(Ui.tv(a, t, 15f, true, Pal.muted).apply { setPadding(a.dp(4), a.dp(18), 0, a.dp(8)) })
    sect(L.t("اللغة", "Language"))
    col.addView(Ui.segmented(a, listOf("ar" to "العربية", "en" to "English"), p.lang) { p.lang = it; Store.savePrefs(a); a.applyLook() })
    sect(L.t("المظهر", "Appearance"))
    col.addView(Ui.segmented(a, listOf("auto" to L.t("تلقائي", "Auto"), "light" to L.t("فاتح", "Light"), "dark" to L.t("داكن", "Dark")), p.theme) { p.theme = it; Store.savePrefs(a); a.applyLook() })
    sect(L.t("شكل الوقت", "Time format"))
    col.addView(Ui.segmented(a, listOf("12" to L.t("12 ساعة", "12-hour"), "24" to L.t("24 ساعة", "24-hour")), if (p.t12) "12" else "24") { p.t12 = it == "12"; Store.savePrefs(a); a.applyLook(); a.savePrefs() })
    val c = Ui.card(a, 16)
    c.addView(Ui.switchRow(a, L.t("المؤثرات ثلاثية الأبعاد", "3D effects"), p.fx) { v -> p.fx = v; Store.savePrefs(a); Fx.enabled = v; if (v) a.bg.start() else a.bg.stop(); a.bg.invalidate() })
    c.addView(Ui.tv(a, L.t("خلفية مجسّمة تتحرك مع إمالة الهاتف، وميلان للبطاقات عند اللمس، وانتقالات عميقة. أوقفها إن أردت.",
        "A depth background that moves as you tilt the phone, cards that tilt when touched, and deep transitions. Turn them off if you like."), 13.5f, false, Pal.muted).apply { setPadding(0, a.dp(4), 0, 0) })
    col.addView(c, lp(18))
}

// ---------- side menu ----------
object Drawer {
    fun open(a: MainActivity) {
        if (a.drawerClose != null) return
        val scrim = FrameLayout(a).apply { setBackgroundColor(0x88000000.toInt()); isClickable = true }
        val w = a.dp(300)
        val panel = Ui.vbox(a).apply {
            background = rounded(Pal.card, 0f)
            setPadding(a.dp(16), a.dp(40), a.dp(16), a.dp(20))
            isClickable = true
            elevation = a.dpf(16f)
        }
        val hdr = Ui.hbox(a).apply { setPadding(a.dp(4), 0, 0, a.dp(20)) }
        hdr.addView(ImageView(a).apply { setImageResource(R.drawable.logo_tile) }, LinearLayout.LayoutParams(a.dp(52), a.dp(52)).apply { marginEnd = a.dp(12) })
        val ht = Ui.vbox(a)
        ht.addView(Ui.title(a, L.t("جدول المحاضرات", "Lecture Schedule")).apply { textSize = 19f })
        ht.addView(Ui.tv(a, L.t("القائمة", "Menu"), 13f, false, Pal.muted).apply { setPadding(0, a.dp(2), 0, 0) })
        hdr.addView(ht)
        panel.addView(hdr)
        lateinit var close: () -> Unit
        fun item(icon: Int, text: String, v: String?, action: (() -> Unit)? = null) {
            val on = v != null && a.view == v
            val r = Ui.hbox(a).apply {
                minimumHeight = a.dp(52)
                setPadding(a.dp(14), a.dp(8), a.dp(14), a.dp(8))
                background = if (on) rounded(Pal.soft, a.dpf(16f)) else ripple(rounded(0, a.dpf(16f)), 0x33888888)
            }
            r.addView(Ui.icon(a, icon, if (on) Pal.accent else Pal.fg, 24).apply { (layoutParams as LinearLayout.LayoutParams).marginEnd = a.dp(14) })
            r.addView(Ui.tv(a, text, 16f, true, if (on) Pal.accent else Pal.fg))
            r.setOnClickListener { close(); if (action != null) action() else a.go(v!!) }
            panel.addView(r, LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP).apply { bottomMargin = a.dp(4) })
        }
        item(R.drawable.ic_cal, L.t("الجدول", "Schedule"), "main")
        item(R.drawable.ic_exam, L.t("امتحاناتي", "My exams"), "exams")
        item(R.drawable.ic_plus, L.t("جدول جديد", "New schedule"), null) { a.screens.openWizard() }
        item(R.drawable.ic_bell, L.t("التنبيهات", "Reminders"), "notif")
        item(R.drawable.ic_gear, L.t("الإعدادات", "Settings"), "settings")
        panel.addView(View(a), LinearLayout.LayoutParams(1, 0, 1f))
        panel.addView(View(a).apply { setBackgroundColor(Pal.line) }, LinearLayout.LayoutParams(Ui.MATCH, 1).apply { bottomMargin = a.dp(12) })
        panel.addView(Ui.tv(a, L.t("تواصل معي", "Contact me"), 13.5f, true, Pal.muted).apply { setPadding(a.dp(4), 0, 0, a.dp(10)) })
        val soc = Ui.hbox(a)
        fun social(icon: Int, label: String, intent: () -> Intent) {
            soc.addView(FrameLayout(a).apply {
                background = ripple(rounded(Pal.field, a.dpf(16f), Pal.line, a.dpf(1f)), 0x33888888)
                contentDescription = label
                addView(ImageView(a).apply { setImageResource(icon) }, FrameLayout.LayoutParams(a.dp(28), a.dp(28), Gravity.CENTER))
                setOnClickListener { try { a.startActivity(intent()) } catch (e: Exception) { a.toast(L.t("تعذر الفتح", "Could not open")) } }
            }, LinearLayout.LayoutParams(a.dp(56), a.dp(56)).apply { marginEnd = a.dp(12) })
        }
        social(R.drawable.ic_facebook, "Facebook") { Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/share/1HvVWE8XdM/")) }
        social(R.drawable.ic_gmail, "Gmail") { Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:elsllakmohammed@gmail.com")) }
        panel.addView(soc)
        panel.addView(Ui.tv(a, "Mohammed Elsllak", 12.5f, true, Pal.muted).apply { setPadding(a.dp(4), a.dp(16), 0, 0) })

        scrim.addView(panel, FrameLayout.LayoutParams(w, Ui.MATCH, Gravity.START))
        val hidden = (if (L.isAr) w else -w).toFloat()
        var closed = false
        close = {
            if (!closed) {
                closed = true; a.drawerClose = null
                panel.animate().translationX(hidden).setDuration(200).start()
                scrim.animate().alpha(0f).setDuration(200).withEndAction { (scrim.parent as? ViewGroup)?.removeView(scrim) }.start()
            }
        }
        scrim.setOnClickListener { close() }
        a.drawerClose = close
        a.layer.addView(scrim, FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH))
        panel.translationX = hidden
        panel.animate().translationX(0f).setDuration(240).start()
        scrim.alpha = 0f; scrim.animate().alpha(1f).setDuration(220).start()
    }
}
