package com.elsllak.jadwal

import android.graphics.Bitmap
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.text.InputType

class ExamDraft(
    val mode: String, val id: String?, var name: String = "", var code: String = "", var type: String = "mid",
    var date: String = "", var s: Int? = null, var e: Int? = null, var room: String = "", var note: String = ""
)

class ResRow(
    val code: String, val name: String, val found: Boolean, var inc: Boolean, var date: String,
    var s: Int?, var e: Int?, var room: String, val guess: Boolean, var err: String = ""
)

class OcrDraft(
    var uri: android.net.Uri? = null, var thumb: Bitmap? = null, var type: String = "fin",
    val codes: MutableList<String> = ArrayList(), var input: String = "", var busy: Boolean = false, var status: String = "",
    var err: String = "", var diag: String = "", var results: MutableList<ResRow>? = null, var raw: String = "", var showRaw: Boolean = false
)

// ---------- exams list ----------
fun Screens.exams(): View = a.page(L.t("امتحاناتي", "My exams"), "") { col ->
    val list = Logic.allExams(S)
    val per = S.period
    val acts = Ui.hbox(a)
    acts.addView(Ui.button(a, L.t("إضافة امتحان", "Add exam")) { openExamForm("new", null) }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
    acts.addView(View(a), LinearLayout.LayoutParams(a.dp(10), 1))
    acts.addView(Ui.button(a, L.t("من صورة", "From a photo"), false) { openOcr() }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
    col.addView(acts, lp())

    val pc = Ui.card(a, 14).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    val ptx = Ui.vbox(a)
    ptx.addView(Ui.tv(a, L.t("فترة الامتحانات", "Exam period"), 15f, true))
    ptx.addView(Ui.tv(a, if (per.from.isNotEmpty() && per.to.isNotEmpty()) Dt.fmtD(per.from) + " – " + Dt.fmtD(per.to) else L.t("لم تحدد بعد", "Not set yet"), 13.5f, false, Pal.muted).apply { setPadding(0, a.dp(3), 0, 0) })
    pc.addView(ptx, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
    pc.addView(Ui.button(a, L.t("تحديد", "Set"), false) { periodSheet() }.apply { minHeight = a.dp(42) })
    col.addView(pc, lp(12))

    val t = Dt.todayS()
    val now = java.time.LocalTime.now(); val nowMin = now.hour * 60 + now.minute
    val up = list.firstOrNull { it.date > t || (it.date == t && (it.e == null || it.e > nowMin)) }
    if (up != null) {
        val dd = Dt.diffDays(t, up.date)
        val rel = if (dd == 0) L.t("اليوم", "Today") else if (dd == 1) L.t("غدًا", "Tomorrow") else L.t("بعد " + L.durAr(dd, 'd'), "In " + L.durEn(dd, 'd'))
        val nb = Ui.vbox(a).apply {
            background = rounded(Pal.accent, a.dpf(20f)); setPadding(a.dp(18), a.dp(14), a.dp(18), a.dp(14))
        }
        nb.addView(Ui.tv(a, L.t("الامتحان القادم", "Next exam"), 12.5f, true, (Pal.accentFg and 0x00FFFFFF) or 0xCC000000.toInt()))
        nb.addView(Ui.tv(a, up.name + " · " + L.typeLbl(up.type), 18f, true, Pal.accentFg).apply { setPadding(0, a.dp(4), 0, a.dp(4)) })
        nb.addView(Ui.tv(a, rel + " · " + L.day(Dt.absDay(up.date)) + " " + Dt.fmtD(up.date) + (up.s?.let { " · " + L.fmtT(it) } ?: ""), 14f, false, Pal.accentFg))
        Fx.tilt(nb)
        col.addView(nb, lp(12))
    }

    val ds = list.map { it.date }.toMutableList()
    if (per.from.isNotEmpty()) ds.add(per.from)
    if (per.to.isNotEmpty()) ds.add(per.to)
    ds.sort()
    if (ds.isEmpty()) {
        col.addView(Ui.tv(a, L.t("لا توجد امتحانات بعد. أضفها يدويًا أو من صورة جدول الكلية.", "No exams yet. Add them manually or from a photo of the faculty timetable."), 14.5f, false, Pal.muted), lp(18))
        return@page
    }
    val from = ds.first()
    var to = ds.last()
    if (Dt.diffDays(from, to) > 400) to = Dt.addDays(from, 400)
    val hasP = per.from.isNotEmpty() && per.to.isNotEmpty()
    val exDates = list.map { it.date }.toSet()
    var wkStart = Dt.addDays(from, -Dt.absDay(from))
    var wn = 1
    while (wkStart <= to && wn <= 40) {
        val wkEx = (0..6).map { Dt.addDays(wkStart, it) }.filter { it in exDates }
        val days = ArrayList<Pair<String, Int>>()
        for (j in 0..6) {
            val dy = Dt.addDays(wkStart, j)
            val inP = hasP && dy >= per.from && dy <= per.to
            val inEx = wkEx.isNotEmpty() && !hasP && dy >= wkEx.first() && dy <= wkEx.last()
            if (inP || dy in exDates || inEx) days.add(Pair(dy, j))
        }
        if (days.isNotEmpty()) {
            col.addView(Ui.tv(a, L.t("الأسبوع $wn · ${Dt.fmtDM(days.first().first)} – ${Dt.fmtDM(days.last().first)}",
                "Week $wn · ${Dt.fmtDM(days.first().first)} – ${Dt.fmtDM(days.last().first)}"), 15f, true, Pal.muted).apply { setPadding(a.dp(4), a.dp(20), 0, a.dp(8)) })
            val box = Ui.card(a, 8)
            for ((dy, i) in days) {
                val tod = dy == t
                val xs = list.filter { it.date == dy }
                val row = Ui.hbox(a).apply { gravity = Gravity.TOP; setPadding(0, a.dp(3), 0, a.dp(3)) }
                val dl = Ui.vbox(a).apply {
                    gravity = Gravity.CENTER
                    background = rounded(if (tod) Pal.accent else Pal.soft, a.dpf(14f))
                    setPadding(a.dp(4), a.dp(10), a.dp(4), a.dp(10))
                }
                dl.addView(Ui.tv(a, L.dayShort(i), 13f, true, if (tod) Pal.accentFg else Pal.accent).apply { gravity = Gravity.CENTER })
                dl.addView(Ui.tv(a, Dt.fmtDM(dy), 12f, false, if (tod) Pal.accentFg else Pal.muted).apply { gravity = Gravity.CENTER; setPadding(0, a.dp(2), 0, 0) })
                row.addView(dl, LinearLayout.LayoutParams(a.dp(70), Ui.WRAP).apply { marginEnd = a.dp(8) })
                val bx = Ui.vbox(a)
                if (xs.isEmpty()) bx.addView(View(a), LinearLayout.LayoutParams(1, a.dp(8)))
                for (x in xs) bx.addView(examBlock(x), lp(0).apply { bottomMargin = a.dp(4) })
                row.addView(bx, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
                box.addView(row)
            }
            col.addView(box)
            wn++
        }
        wkStart = Dt.addDays(wkStart, 7)
    }
}

fun Screens.examBlock(x: ExamView): View {
    val color = Pal.courseColors[Logic.colorOf(S, x.name, x.code) % 8]
    val tm = if (x.s == null || x.e == null) (if (x.derived) L.t("اضغط لإضافة الوقت والمكان", "Tap to add time and place") else L.t("بدون وقت", "No time"))
    else L.fmtT(x.s) + " – " + L.fmtT(x.e)
    val v = Ui.vbox(a).apply {
        setPadding(a.dp(12), a.dp(10), a.dp(12), a.dp(10))
        background = if (x.derived) rounded((color and 0x00FFFFFF) or 0x22000000, a.dpf(14f), color, a.dpf(1.5f)) else rounded(color, a.dpf(14f))
    }
    val fg = if (x.derived) Pal.fg else 0xFFFFFFFF.toInt()
    val h = Ui.hbox(a)
    h.addView(Ui.tv(a, x.name, 14.5f, true, fg), LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
    h.addView(Ui.chip(a, L.typeLbl(x.type), if (x.derived) color else 0x33FFFFFF, 0xFFFFFFFF.toInt()).apply { textSize = 11f })
    v.addView(h)
    v.addView(Ui.tv(a, tm + (if (x.room.isNotEmpty()) " · " + x.room else ""), 12.5f, false, if (x.derived) Pal.muted else 0xDDFFFFFF.toInt()).apply { setPadding(0, a.dp(4), 0, 0) })
    v.setOnClickListener { examSheet(x) }
    Fx.press(v)
    return v
}

fun Screens.periodSheet() = Sheets.open(a) { box, dismiss ->
    var f = S.period.from; var t = S.period.to
    box.addView(Ui.tv(a, L.t("فترة الامتحانات", "Exam period"), 19f, true))
    box.addView(Ui.tv(a, L.t("حدد أول يوم وآخر يوم في الامتحانات ليُبنى الجدول بالتواريخ.", "Set the first and last day of the exams to build the table by date."), 14f, false, Pal.muted).apply { setPadding(0, a.dp(6), 0, 0) })
    val fb = Ui.vbox(a); val tb = Ui.vbox(a)
    fun draw() {
        fb.removeAllViews(); tb.removeAllViews()
        fb.addView(Ui.label(a, L.t("من تاريخ", "From date")))
        fb.addView(Ui.pickerField(a, if (f.isEmpty()) L.t("اختر التاريخ", "Pick a date") else Dt.fmtD(f), f.isEmpty()) {
            Sheets.pickDate(a, L.t("من تاريخ", "From date"), f, false) { f = it; draw() }
        })
        tb.addView(Ui.label(a, L.t("إلى تاريخ", "To date")))
        tb.addView(Ui.pickerField(a, if (t.isEmpty()) L.t("اختر التاريخ", "Pick a date") else Dt.fmtD(t), t.isEmpty()) {
            Sheets.pickDate(a, L.t("إلى تاريخ", "To date"), t, false) { t = it; draw() }
        })
    }
    draw()
    box.addView(fb); box.addView(tb)
    val err = errBox("")
    box.addView(err, lp(12))
    val row = Ui.hbox(a).apply { setPadding(0, a.dp(14), 0, 0) }
    row.addView(Ui.button(a, L.t("حفظ", "Save")) {
        if (f.isEmpty() || t.isEmpty() || f > t) { err.text = L.t("تاريخ البداية يجب أن يسبق النهاية.", "The start date must come before the end date."); err.visibility = View.VISIBLE }
        else { S.period.from = f; S.period.to = t; a.changed(); dismiss(); a.toast(L.t("تم حفظ الفترة", "Period saved")); a.refresh() }
    }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
    row.addView(View(a), LinearLayout.LayoutParams(a.dp(10), 1))
    row.addView(Ui.button(a, L.t("مسح الفترة", "Clear period"), false) {
        S.period.from = ""; S.period.to = ""; a.changed(); dismiss(); a.refresh()
    }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
    box.addView(row)
}

fun Screens.examSheet(x: ExamView) = Sheets.open(a) { box, dismiss ->
    val h = Ui.hbox(a)
    h.addView(View(a).apply { background = rounded(Pal.courseColors[Logic.colorOf(S, x.name, x.code) % 8], a.dpf(6f)) }, LinearLayout.LayoutParams(a.dp(12), a.dp(12)).apply { marginEnd = a.dp(10) })
    h.addView(Ui.tv(a, x.name, 20f, true), LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
    box.addView(h)
    kv(box, L.t("نوع الامتحان", "Exam type"), L.typeLbl(x.type))
    kv(box, L.t("التاريخ", "Date"), L.day(Dt.absDay(x.date)) + " " + Dt.fmtD(x.date))
    kv(box, L.t("الوقت", "Time"), if (x.s == null || x.e == null) L.t("بدون وقت", "No time") else L.fmtT(x.s) + " – " + L.fmtT(x.e))
    if (x.room.isNotEmpty()) kv(box, L.t("القاعة", "Room"), x.room)
    if (x.note.isNotEmpty()) kv(box, L.t("ملاحظة", "Note"), x.note)
    if (x.derived) {
        box.addView(Ui.tv(a, L.t("هذا التاريخ مأخوذ من بيانات المادة ولا يحتوي وقتًا. أضف الوقت والقاعة ليظهر كاملًا.", "This date comes from the course details and has no time. Add the time and room to complete it."), 13.5f, false, Pal.muted).apply { setPadding(0, a.dp(12), 0, 0) })
        val row = Ui.hbox(a).apply { setPadding(0, a.dp(14), 0, 0) }
        row.addView(Ui.button(a, L.t("إضافة الوقت والمكان", "Add time and room")) {
            dismiss(); openExamForm("new", ExamDraft("new", null, x.name, x.code, x.type, x.date, null, null, "", x.note))
        }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
        row.addView(View(a), LinearLayout.LayoutParams(a.dp(10), 1))
        row.addView(Ui.button(a, L.t("تعديل المادة", "Edit course"), false) { dismiss(); openCourse("edit", x.cid) }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
        box.addView(row)
    } else {
        val row = Ui.hbox(a).apply { setPadding(0, a.dp(16), 0, 0) }
        row.addView(Ui.button(a, L.t("تعديل", "Edit"), false) {
            dismiss(); openExamForm("edit", ExamDraft("edit", x.id, x.name, x.code, x.type, x.date, x.s, x.e, x.room, x.note))
        }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
        row.addView(View(a), LinearLayout.LayoutParams(a.dp(10), 1))
        row.addView(Ui.button(a, L.t("حذف", "Delete"), true, true) {
            dismiss()
            Sheets.confirm(a, L.t("حذف الامتحان", "Delete exam"), L.t("حذف امتحان «${x.name}»؟", "Delete the ${x.name} exam?"), L.t("حذف", "Delete"), true) {
                S.exams.removeAll { it.id == x.id }; a.changed(); a.toast(L.t("حذف الامتحان", "Exam deleted")); a.refresh()
            }
        }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
        box.addView(row)
    }
}

// ---------- exam form ----------
fun Screens.openExamForm(mode: String, pre: ExamDraft?) {
    S.exams.removeAll { it.sample }
    xd = pre ?: ExamDraft(mode, null)
    a.ret = "exams"
    a.go("xform")
}

fun Screens.examForm(): View {
    val d = xd ?: ExamDraft("new", null).also { xd = it }
    val edit = d.mode == "edit"
    return a.page(if (edit) L.t("تعديل الامتحان", "Edit exam") else L.t("إضافة امتحان", "Add exam"), back = "exams") { col ->
        col.addView(Ui.label(a, L.t("المادة", "Course")))
        if (S.courses.isNotEmpty()) col.addView(Ui.pickerField(a, L.t("اختر من جدولك", "Pick from your schedule"), true) {
            Sheets.pickList(a, L.t("المادة", "Course"), S.courses.map { it.name }, null) { i ->
                d.name = S.courses[i].name; d.code = S.courses[i].code; a.refresh()
            }
        }, lp().apply { bottomMargin = a.dp(8) })
        col.addView(Ui.field(a, L.t("أو اكتب الاسم", "Or type the name"), d.name).also { watch(it) { v -> d.name = v } })
        col.addView(Ui.label(a, L.t("رمز المادة", "Course code"), true))
        col.addView(Ui.field(a, L.t("مثال: CHEM101", "Example: CHEM101"), d.code, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS).also { watch(it) { v -> d.code = v } })
        col.addView(Ui.label(a, L.t("نوع الامتحان", "Exam type")))
        col.addView(Ui.segmented(a, listOf("mid" to L.t("جزئي", "Midterm"), "fin" to L.t("نهائي", "Final")), d.type) { d.type = it; a.refresh() })
        col.addView(Ui.label(a, L.t("التاريخ", "Date")))
        col.addView(Ui.pickerField(a, if (d.date.isEmpty()) L.t("اختر التاريخ", "Pick a date") else L.day(Dt.absDay(d.date)) + " " + Dt.fmtD(d.date), d.date.isEmpty()) {
            Sheets.pickDate(a, L.t("تاريخ الامتحان", "Exam date"), d.date, false) { d.date = it; a.refresh() }
        })
        val r = Ui.hbox(a)
        val c1 = Ui.vbox(a); val c2 = Ui.vbox(a)
        c1.addView(Ui.label(a, L.t("من", "From")))
        c1.addView(Ui.pickerField(a, d.s?.let { L.fmtT(it) } ?: L.t("اختر", "Select"), d.s == null) {
            Sheets.pickTime(a, L.t("بداية الامتحان", "Exam start"), d.s) { v -> d.s = v; if (d.e == null || d.e!! <= v) d.e = minOf(v + 120, 1439); a.refresh() }
        })
        c2.addView(Ui.label(a, L.t("إلى", "To")))
        c2.addView(Ui.pickerField(a, d.e?.let { L.fmtT(it) } ?: L.t("اختر", "Select"), d.e == null) {
            Sheets.pickTime(a, L.t("نهاية الامتحان", "Exam end"), d.e) { v -> d.e = v; a.refresh() }
        })
        r.addView(c1, LinearLayout.LayoutParams(0, Ui.WRAP, 1f)); r.addView(View(a), LinearLayout.LayoutParams(a.dp(10), 1)); r.addView(c2, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
        col.addView(r)
        col.addView(Ui.label(a, L.t("القاعة", "Room"), true))
        col.addView(Ui.field(a, L.t("مثال: قاعة 12", "Example: Room 12"), d.room).also { watch(it) { v -> d.room = v } })
        col.addView(Ui.label(a, L.t("ملاحظة تظهر في الإشعار", "Note shown in the notification"), true))
        col.addView(Ui.field(a, L.t("مثال: أحضر الآلة الحاسبة", "Example: bring a calculator"), d.note).also { watch(it) { v -> d.note = v } })
        val err = errBox("")
        col.addView(err, lp(14))
        col.addView(Ui.button(a, if (edit) L.t("حفظ التعديل", "Save changes") else L.t("إضافة", "Add")) { submitExam(d, err) }, lp(14))
        if (!edit) col.addView(Ui.button(a, L.t("عرض الجدول", "View schedule"), false) { a.go("exams") }, lp(12))
    }
}

private fun Screens.submitExam(d: ExamDraft, err: TextView) {
    var code = d.code.trim()
    val name = d.name.trim()
    if (code.isEmpty()) Logic.courseFor(S, name, "")?.let { code = it.code }
    val x = Exam(d.id ?: G.uid("x"), name, code, d.type, d.date, d.s, d.e, d.room.trim(), d.note.trim())
    val m = Logic.validateExam(S, x)
    if (m.isNotEmpty()) { showErr(err, m); return }
    if (d.mode == "edit") {
        val i = S.exams.indexOfFirst { it.id == x.id }
        if (i >= 0) S.exams[i] = x
        a.changed(); a.toast(L.t("تم حفظ الامتحان", "Exam saved")); a.go("exams")
    } else {
        S.exams.add(x); a.changed(); a.toast(L.t("أضيف امتحان $name", "$name exam added"))
        xd = ExamDraft("new", null, type = d.type)
        a.refresh(); a.scroller?.post { a.scroller?.scrollTo(0, 0) }
    }
}

// ---------- exams from a photo ----------
fun Screens.openOcr() {
    S.exams.removeAll { it.sample }
    val codes = S.courses.filter { it.code.isNotEmpty() }.map { it.code.uppercase() }.toMutableList()
    od = OcrDraft(codes = codes)
    a.ret = "exams"
    a.go("ocr")
}

fun Screens.ocr(): View {
    val d = od ?: OcrDraft().also { od = it }
    return a.page(L.t("امتحانات من صورة", "Exams from a photo"), back = "exams") { col ->
        col.addView(Ui.tv(a, L.t("ارفع صورة جدول امتحانات الكلية، وأدخل رموز موادك. سيبحث التطبيق عنها في الصورة ويستخرج التاريخ والوقت والقاعة. راجع النتيجة قبل الإضافة.",
            "Upload a photo of the faculty exam timetable and enter your course codes. The app looks for them in the photo and extracts the date, time and room. Review the result before adding."), 14.5f, false, Pal.muted))
        val card = Ui.card(a, 14)
        d.thumb?.let { b ->
            card.addView(ImageView(a).apply { setImageBitmap(b); adjustViewBounds = true; scaleType = ImageView.ScaleType.CENTER_CROP; clipToOutline = true
                background = rounded(Pal.field, a.dpf(14f)); outlineProvider = android.view.ViewOutlineProvider.BACKGROUND },
                LinearLayout.LayoutParams(Ui.MATCH, a.dp(170)).apply { bottomMargin = a.dp(10) })
        }
        card.addView(Ui.button(a, if (d.uri != null) L.t("تغيير الصورة", "Change photo") else L.t("اختيار صورة الجدول", "Choose the timetable photo"), false) {
            a.pickImage { uri ->
                d.uri = uri; d.results = null; d.err = ""; d.diag = ""
                d.thumb = try { Ocr.load(a, uri, 700) } catch (e: Throwable) { null }
                a.refresh()
            }
        })
        card.addView(Ui.label(a, L.t("نوع الامتحانات في الصورة", "Exam type in the photo")))
        card.addView(Ui.segmented(a, listOf("mid" to L.t("جزئي", "Midterm"), "fin" to L.t("نهائي", "Final")), d.type) { d.type = it; a.refresh() })
        card.addView(Ui.label(a, L.t("رموز موادك", "Your course codes")))
        if (d.codes.isNotEmpty()) {
            val flow = FlowLayout(a)
            for ((i, c) in d.codes.withIndex()) {
                flow.addView(Ui.tv(a, "$c  ×", 13.5f, true, Pal.accent).apply {
                    setPadding(a.dp(12), a.dp(8), a.dp(12), a.dp(8)); background = rounded(Pal.soft, a.dpf(12f))
                    setOnClickListener { d.codes.removeAt(i); a.refresh() }
                })
            }
            card.addView(flow, lp().apply { bottomMargin = a.dp(8) })
        }
        val ar = Ui.hbox(a)
        val inp = Ui.field(a, L.t("مثال: CS101", "Example: CS101"), d.input, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS)
        watch(inp) { v -> d.input = v }
        ar.addView(inp, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
        ar.addView(View(a), LinearLayout.LayoutParams(a.dp(8), 1))
        ar.addView(Ui.button(a, L.t("إضافة", "Add"), false) {
            d.input.split(Regex("[\\s,،;]+")).map { it.trim().uppercase() }.filter { it.isNotEmpty() && !d.codes.contains(it) }.forEach { d.codes.add(it) }
            d.input = ""; a.refresh()
        })
        card.addView(ar)
        val err = errBox(d.err + if (d.diag.isNotEmpty()) "\n\n" + d.diag else "")
        card.addView(err, lp(12))
        if (d.busy) {
            card.addView(Ui.tv(a, d.status, 14f, true, Pal.accent).apply { setPadding(0, a.dp(14), 0, 0) })
        } else {
            card.addView(Ui.button(a, L.t("ابحث وأنشئ جدولي", "Find and build my table")) { runOcr(d) }, lp(14))
        }
        col.addView(card, lp(12))

        val res = d.results
        if (res != null) {
            col.addView(Ui.tv(a, L.t("النتيجة", "Result"), 17f, true).apply { setPadding(a.dp(4), a.dp(20), 0, a.dp(4)) })
            col.addView(Ui.tv(a, L.t("راجع كل مادة وصحّح ما تحتاجه، ثم أضف المحدد.", "Review each course, fix what is needed, then add the selected ones."), 14f, false, Pal.muted))
            if (res.none { it.found }) col.addView(Ui.tv(a, L.t("لم أجد أيًّا من الرموز. تأكد من الرموز ومن وضوح الصورة، وانظر النص الذي قرأه التطبيق أدناه.",
                "None of the codes were found. Check the codes and the photo quality, and see the text the app read below."), 14f, true, Pal.danger).apply { setPadding(0, a.dp(8), 0, 0) })
            for (r in res) {
                val c = Ui.card(a, 14)
                val h = Ui.hbox(a)
                val box = View(a).apply { background = rounded(if (r.inc) Pal.accent else 0, a.dpf(7f), if (r.inc) Pal.accent else Pal.muted, a.dpf(2f)) }
                h.addView(box, LinearLayout.LayoutParams(a.dp(24), a.dp(24)).apply { marginEnd = a.dp(10) })
                h.addView(Ui.tv(a, r.code + (if (r.name != r.code) " · " + r.name else ""), 16f, true), LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
                h.addView(Ui.chip(a, if (r.found) L.t("وُجدت في الصورة", "Found in the photo") else L.t("لم أجد هذا الرمز في الصورة", "This code was not found in the photo"),
                    if (r.found) Pal.soft else (Pal.danger and 0x00FFFFFF) or 0x22000000, if (r.found) Pal.accent else Pal.danger).apply { textSize = 11f })
                h.setOnClickListener { r.inc = !r.inc; a.refresh() }
                c.addView(h)
                c.addView(Ui.label(a, L.t("التاريخ", "Date")))
                c.addView(Ui.pickerField(a, if (r.date.isEmpty()) L.t("اختر التاريخ", "Pick a date") else Dt.fmtD(r.date), r.date.isEmpty()) {
                    Sheets.pickDate(a, L.t("تاريخ الامتحان", "Exam date"), r.date, false) { v -> r.date = v; a.refresh() }
                })
                val tr = Ui.hbox(a); val t1 = Ui.vbox(a); val t2 = Ui.vbox(a)
                t1.addView(Ui.label(a, L.t("من", "From")))
                t1.addView(Ui.pickerField(a, r.s?.let { L.fmtT(it) } ?: L.t("اختر", "Select"), r.s == null) { Sheets.pickTime(a, L.t("من", "From"), r.s) { v -> r.s = v; a.refresh() } })
                t2.addView(Ui.label(a, L.t("إلى", "To")))
                t2.addView(Ui.pickerField(a, r.e?.let { L.fmtT(it) } ?: L.t("اختر", "Select"), r.e == null) { Sheets.pickTime(a, L.t("إلى", "To"), r.e) { v -> r.e = v; a.refresh() } })
                tr.addView(t1, LinearLayout.LayoutParams(0, Ui.WRAP, 1f)); tr.addView(View(a), LinearLayout.LayoutParams(a.dp(10), 1)); tr.addView(t2, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
                c.addView(tr)
                c.addView(Ui.label(a, L.t("القاعة", "Room"), true))
                c.addView(Ui.field(a, L.t("مثال: قاعة 12", "Example: Room 12"), r.room).also { watch(it) { v -> r.room = v } })
                if (r.guess && r.found) c.addView(Ui.tv(a, L.t("وقت النهاية غير ظاهر في الصورة، تم افتراضه بعد ساعتين.", "The end time is not visible in the photo. It was assumed to be 2 hours later."), 12.5f, false, Pal.muted).apply { setPadding(0, a.dp(8), 0, 0) })
                if (r.err.isNotEmpty()) c.addView(Ui.tv(a, r.err, 13.5f, true, Pal.danger).apply { setPadding(0, a.dp(8), 0, 0) })
                col.addView(c, lp(10))
            }
            val raw = Ui.card(a, 14)
            raw.addView(Ui.tv(a, (if (d.showRaw) "▾  " else "▸  ") + L.t("النص الذي قرأه التطبيق", "Text the app read"), 14.5f, true).apply { setOnClickListener { d.showRaw = !d.showRaw; a.refresh() } })
            if (d.showRaw) raw.addView(Ui.tv(a, d.raw.ifEmpty { "—" }, 12.5f, false, Pal.muted).apply { setPadding(0, a.dp(10), 0, 0); setTextIsSelectable(true) })
            col.addView(raw, lp(10))
            col.addView(Ui.button(a, L.t("إضافة المحدد إلى امتحاناتي (${res.count { it.inc }})", "Add selected to My exams (${res.count { it.inc }})")) { addOcr(d) }, lp(14))
        }
    }
}

private fun Screens.runOcr(d: OcrDraft) {
    d.err = ""; d.diag = ""
    if (d.codes.isEmpty()) { d.err = L.t("أدخل رمز مادة واحدًا على الأقل.", "Enter at least one course code."); a.refresh(); return }
    val uri = d.uri
    if (uri == null) { d.err = L.t("اختر صورة الجدول أولًا.", "Choose the timetable photo first."); a.refresh(); return }
    d.busy = true; d.status = L.t("جارٍ التحضير…", "Preparing…"); a.refresh()
    val codes = d.codes.toList()
    Thread {
        try {
            val out = Ocr.run(a, uri, codes, Dt.todayS()) { p ->
                a.runOnUiThread { d.status = if (p <= 2) L.t("جارٍ قراءة الصورة…", "Reading the photo…") else L.t("محاولة ثانية بصورة محسّنة…", "Second try with an enhanced image…"); if (a.view == "ocr") a.refresh() }
            }
            a.runOnUiThread {
                d.busy = false; d.raw = out.text
                d.results = out.results.map { r ->
                    val c = Logic.courseFor(S, "", r.code)
                    ResRow(r.code, c?.name ?: r.code, r.found, r.found, r.date, r.s, r.e, r.room, r.guessEnd)
                }.toMutableList()
                if (a.view == "ocr") a.refresh()
            }
        } catch (e: Throwable) {
            a.runOnUiThread {
                d.busy = false
                d.err = L.t("تعذر قراءة الصورة. جرّب صورة أوضح.", "Could not read the photo. Try a clearer one.")
                d.diag = (e.message ?: e.javaClass.simpleName)
                if (a.view == "ocr") a.refresh()
            }
        }
    }.start()
}

private fun Screens.addOcr(d: OcrDraft) {
    val res = d.results ?: return
    var added = 0
    val keep = ArrayList<ResRow>()
    for (r in res) {
        if (!r.inc) { keep.add(r); continue }
        val x = Exam(G.uid("x"), r.name, r.code, d.type, r.date, r.s, r.e, r.room.trim(), "")
        val m = if (r.date.isEmpty() || r.s == null || r.e == null) L.t("أكمل التاريخ والوقت.", "Complete the date and time.") else Logic.validateExam(S, x)
        if (m.isNotEmpty()) { r.err = m; keep.add(r) } else { S.exams.add(x); added++ }
    }
    if (added > 0) a.changed()
    d.results = keep.toMutableList()
    if (keep.none { it.inc } && added > 0) { a.toast(L.t(if (added == 1) "أضيف امتحان واحد" else "أضيف $added امتحانات", if (added == 1) "1 exam added" else "$added exams added")); a.go("exams"); return }
    if (added > 0) a.toast(L.t("أضيف $added", "$added added"))
    a.refresh()
}
