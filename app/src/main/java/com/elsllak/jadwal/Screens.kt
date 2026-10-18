package com.elsllak.jadwal

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

class LecDraft(var d: Int? = null, var s: Int? = null, var e: Int? = null, var room: String = "")

class CourseDraft(
    val mode: String, val id: String?, var name: String = "", var code: String = "", var note: String = "",
    var mid: String = "", var fin: String = "", var count: Int = 0, val lecs: MutableList<LecDraft> = ArrayList(),
    val added: MutableList<String> = ArrayList()
)

class WizDraft(var days: MutableList<Int>, val slots: MutableList<Array<Int?>>, var err: String = "")

class Screens(val a: MainActivity) {
    var cd: CourseDraft? = null
    var wd: WizDraft? = null
    var xd: ExamDraft? = null
    var od: OcrDraft? = null
    val S get() = G.s

    // ---------- shared helpers ----------
    fun watch(et: EditText, set: (String) -> Unit) {
        et.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { set(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, af: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    fun errBox(msg: String): TextView = Ui.tv(a, msg, 14f, true, Pal.danger).apply {
        setPadding(a.dp(14), a.dp(12), a.dp(14), a.dp(12))
        background = rounded((Pal.danger and 0x00FFFFFF) or 0x22000000, a.dpf(14f), Pal.danger, a.dpf(1f))
        visibility = if (msg.isEmpty()) View.GONE else View.VISIBLE
    }

    fun showErr(box: TextView, msg: String) {
        box.text = msg; box.visibility = View.VISIBLE
        a.scroller?.post { a.scroller?.smoothScrollTo(0, box.bottom + a.dp(80)) }
    }

    fun lp(top: Int = 0) = LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP).apply { topMargin = a.dp(top) }

    fun lecWord(n: Int) = if (L.isAr) (if (n == 1) "محاضرة" else if (n == 2) "محاضرتان" else "محاضرات") else (if (n == 1) "lecture" else "lectures")

    fun clearSample() {
        if (S.sample) { S.courses.clear(); S.sample = false }
        S.exams.removeAll { it.sample }
    }

    // ---------- main schedule ----------
    fun main(): View = a.page(L.t("جدول المحاضرات", "Lecture Schedule"), L.t("جدولك الأسبوعي", "Your weekly timetable")) { col ->
        if (S.sample) {
            col.addView(Ui.tv(a, L.t("هذا جدول تجريبي لتشاهد الشكل. اضغط «جدول جديد» لتبدأ جدولك.",
                "This is a sample schedule so you can see the layout. Tap New schedule to start your own."), 14f, false, Pal.accent).apply {
                setPadding(a.dp(14), a.dp(12), a.dp(14), a.dp(12))
                background = rounded(Pal.soft, a.dpf(16f))
            }, lp())
        }
        val days = S.cfg.days.sorted()
        val card = Ui.card(a, 10)
        val row = Ui.hbox(a).apply { gravity = Gravity.TOP }
        row.addView(DayLabelsView(a, days))
        val hs = HorizontalScrollView(a).apply {
            isHorizontalScrollBarEnabled = false; overScrollMode = View.OVER_SCROLL_NEVER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        val tt = TimetableView(a, S, days, L.t12).apply { onCourse = { c -> courseSheet(c) } }
        hs.addView(tt)
        row.addView(hs, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
        if (L.isAr) hs.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            var done = false
            override fun onGlobalLayout() {
                if (done || tt.width == 0) return
                done = true; hs.scrollTo(tt.width, 0)
            }
        })
        card.addView(row)
        card.addView(Ui.tv(a, L.t("اسحب الجدول جانبيًا لرؤية باقي الأوقات", "Swipe the table sideways to see the other times"), 12f, false, Pal.muted).apply {
            setPadding(a.dp(4), a.dp(8), a.dp(4), 0)
        })
        Fx.tilt(card)
        col.addView(card, lp(6))
        col.addView(Ui.button(a, L.t("إضافة مادة", "Add course")) { openCourse("new", null) }, lp(14))

        col.addView(Ui.tv(a, L.t("المواد", "Courses"), 17f, true).apply { setPadding(a.dp(4), a.dp(22), 0, a.dp(8)) })
        if (S.courses.isEmpty()) {
            col.addView(Ui.tv(a, L.t("الجدول فارغ. اضغط «إضافة مادة» لتبدأ.", "The schedule is empty. Tap Add course to start."), 14f, false, Pal.muted))
        }
        for (c in S.courses) {
            val r = Ui.hbox(a).apply {
                background = rounded(Pal.card, a.dpf(18f), Pal.line, a.dpf(1f))
                setPadding(a.dp(12), a.dp(12), a.dp(14), a.dp(12))
            }
            r.addView(View(a).apply { background = rounded(Pal.courseColors[c.color % 8], a.dpf(4f)) }, LinearLayout.LayoutParams(a.dp(6), a.dp(42)).apply { marginEnd = a.dp(12) })
            val tx = Ui.vbox(a)
            tx.addView(Ui.tv(a, c.name, 16f, true))
            if (c.code.isNotEmpty()) tx.addView(Ui.tv(a, c.code, 13f, false, Pal.muted).apply { setPadding(0, a.dp(3), 0, 0) })
            r.addView(tx, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
            r.addView(Ui.chip(a, "${c.lec.size} " + lecWord(c.lec.size), Pal.soft, Pal.accent))
            r.setOnClickListener { courseSheet(c) }
            Fx.tilt(r)
            col.addView(r, lp(8))
        }
    }

    fun kv(box: LinearLayout, k: String, v: String) {
        val r = Ui.hbox(a).apply { setPadding(0, a.dp(8), 0, a.dp(8)) }
        r.addView(Ui.tv(a, k, 14f, false, Pal.muted), LinearLayout.LayoutParams(a.dp(110), Ui.WRAP))
        r.addView(Ui.tv(a, v, 14.5f, true), LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
        box.addView(r)
        box.addView(View(a).apply { setBackgroundColor(Pal.line) }, LinearLayout.LayoutParams(Ui.MATCH, 1))
    }

    fun courseSheet(c: Course) = Sheets.open(a) { box, dismiss ->
        val h = Ui.hbox(a)
        h.addView(View(a).apply { background = rounded(Pal.courseColors[c.color % 8], a.dpf(6f)) }, LinearLayout.LayoutParams(a.dp(12), a.dp(12)).apply { marginEnd = a.dp(10) })
        h.addView(Ui.tv(a, c.name, 20f, true), LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
        box.addView(h, lp())
        if (c.code.isNotEmpty()) kv(box, L.t("رمز المادة", "Course code"), c.code)
        if (c.note.isNotEmpty()) kv(box, L.t("ملاحظة", "Note"), c.note)
        if (c.mid.isNotEmpty()) kv(box, L.t("الامتحان الجزئي", "Midterm exam"), Dt.fmtD(c.mid))
        if (c.fin.isNotEmpty()) kv(box, L.t("الامتحان النهائي", "Final exam"), Dt.fmtD(c.fin))
        for ((i, l) in c.lec.withIndex()) {
            kv(box, L.t("المحاضرة ${i + 1}", "Lecture ${i + 1}"), Logic.lecRange(S, l) + (if (l.room.isNotEmpty()) "\n" + L.roomLbl(l.room) else ""))
        }
        val row = Ui.hbox(a).apply { setPadding(0, a.dp(16), 0, 0) }
        row.addView(Ui.button(a, L.t("تعديل", "Edit"), false) { dismiss(); openCourse("edit", c.id) }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
        row.addView(View(a), LinearLayout.LayoutParams(a.dp(10), 1))
        row.addView(Ui.button(a, L.t("حذف", "Delete"), true, true) {
            dismiss()
            Sheets.confirm(a, L.t("حذف المادة", "Delete course"),
                L.t("حذف مادة «${c.name}» مع كل محاضراتها (${c.lec.size})؟", "Delete ${c.name} and all of its lectures (${c.lec.size})?"),
                L.t("حذف نهائي", "Delete for good"), true) {
                S.courses.remove(c); a.changed(); a.toast(L.t("حذفت مادة ${c.name}", "${c.name} deleted")); a.refresh()
            }
        }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
        box.addView(row)
    }

    // ---------- course form ----------
    fun openCourse(mode: String, id: String?) {
        val c = id?.let { i -> S.courses.firstOrNull { it.id == i } }
        cd = if (mode == "edit" && c != null) CourseDraft("edit", c.id, c.name, c.code, c.note, c.mid, c.fin, c.lec.size,
            c.lec.map { LecDraft(it.d, it.s, it.e, it.room) }.toMutableList())
        else CourseDraft("new", null)
        a.ret = "main"
        a.go("course")
    }

    private fun resizeLecs(d: CourseDraft, n: Int) {
        while (d.lecs.size < n) d.lecs.add(LecDraft())
        while (d.lecs.size > n) d.lecs.removeAt(d.lecs.size - 1)
        d.count = n
    }

    fun courseForm(): View {
        val d = cd ?: CourseDraft("new", null).also { cd = it }
        val edit = d.mode == "edit"
        return a.page(if (edit) L.t("تعديل المادة", "Edit course") else L.t("إضافة مادة", "Add course"), back = "main") { col ->
            col.addView(Ui.label(a, L.t("اسم المادة", "Course name")))
            col.addView(Ui.field(a, L.t("مثال: الكيمياء العامة", "Example: General Chemistry"), d.name).also { watch(it) { v -> d.name = v } })
            col.addView(Ui.label(a, L.t("رمز المادة", "Course code"), true))
            col.addView(Ui.field(a, L.t("مثال: CHEM101", "Example: CHEM101"), d.code, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS).also { watch(it) { v -> d.code = v } })
            col.addView(Ui.label(a, L.t("ملاحظة تظهر في الإشعار", "Note shown in the notification"), true))
            col.addView(Ui.field(a, L.t("مثال: أحضر الآلة الحاسبة", "Example: bring a calculator"), d.note).also { watch(it) { v -> d.note = v } })
            col.addView(Ui.label(a, L.t("تاريخ الامتحان الجزئي", "Midterm exam date"), true))
            col.addView(Ui.pickerField(a, if (d.mid.isEmpty()) L.t("اختر التاريخ", "Pick a date") else Dt.fmtD(d.mid), d.mid.isEmpty()) {
                Sheets.pickDate(a, L.t("الامتحان الجزئي", "Midterm exam"), d.mid, true) { d.mid = it; a.refresh() }
            })
            col.addView(Ui.label(a, L.t("تاريخ الامتحان النهائي", "Final exam date"), true))
            col.addView(Ui.pickerField(a, if (d.fin.isEmpty()) L.t("اختر التاريخ", "Pick a date") else Dt.fmtD(d.fin), d.fin.isEmpty()) {
                Sheets.pickDate(a, L.t("الامتحان النهائي", "Final exam"), d.fin, true) { d.fin = it; a.refresh() }
            })
            col.addView(Ui.label(a, L.t("كم محاضرة لها في الأسبوع؟", "How many lectures per week?")))
            col.addView(Ui.pickerField(a, if (d.count == 0) L.t("اختر العدد", "Choose a number") else "${d.count} " + lecWord(d.count), d.count == 0) {
                Sheets.pickList(a, L.t("عدد المحاضرات", "Lectures per week"), (1..7).map { "$it " + lecWord(it) }, if (d.count == 0) null else d.count - 1) { i ->
                    resizeLecs(d, i + 1); a.refresh()
                }
            })
            val days = S.cfg.days.sorted()
            for ((i, l) in d.lecs.withIndex()) {
                val card = Ui.card(a, 14)
                card.addView(Ui.tv(a, L.t("المحاضرة ${i + 1}", "Lecture ${i + 1}"), 15f, true, Pal.accent))
                card.addView(Ui.label(a, L.t("اليوم", "Day")))
                card.addView(Ui.pickerField(a, l.d?.let { L.day(it) } ?: L.t("اختر", "Select"), l.d == null) {
                    Sheets.pickList(a, L.t("اليوم", "Day"), days.map { L.day(it) }, l.d?.let { days.indexOf(it) }) { k -> l.d = days[k]; a.refresh() }
                })
                val r = Ui.hbox(a)
                val c1 = Ui.vbox(a); val c2 = Ui.vbox(a)
                c1.addView(Ui.label(a, L.t("من", "From")))
                c1.addView(Ui.pickerField(a, l.s?.let { L.fmtT(S.cfg.slots[it].s) } ?: L.t("اختر", "Select"), l.s == null) {
                    Sheets.pickList(a, L.t("من", "From"), S.cfg.slots.map { L.fmtT(it.s) }, l.s) { k -> l.s = k; if (l.e == null || l.e!! < k) l.e = k; a.refresh() }
                })
                c2.addView(Ui.label(a, L.t("إلى", "To")))
                c2.addView(Ui.pickerField(a, l.e?.let { L.fmtT(S.cfg.slots[it].e) } ?: L.t("اختر", "Select"), l.e == null) {
                    val from = l.s ?: 0
                    val idx = (from until S.cfg.slots.size).toList()
                    Sheets.pickList(a, L.t("إلى", "To"), idx.map { L.fmtT(S.cfg.slots[it].e) }, l.e?.let { idx.indexOf(it) }?.takeIf { it >= 0 }) { k -> l.e = idx[k]; a.refresh() }
                })
                r.addView(c1, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
                r.addView(View(a), LinearLayout.LayoutParams(a.dp(10), 1))
                r.addView(c2, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
                card.addView(r)
                card.addView(Ui.label(a, L.t("القاعة", "Room"), true))
                card.addView(Ui.field(a, L.t("مثال: قاعة 12", "Example: Room 12"), l.room).also { watch(it) { v -> l.room = v } })
                col.addView(card, lp(12))
            }
            val err = errBox("")
            col.addView(err, lp(14))
            col.addView(Ui.button(a, if (edit) L.t("حفظ التعديل", "Save changes") else L.t("إضافة", "Add")) { submitCourse(d, err) }, lp(14))
            if (!edit) {
                if (d.added.isNotEmpty()) {
                    col.addView(Ui.tv(a, L.t("في الجدول الآن", "In the schedule now"), 14f, true, Pal.muted).apply { setPadding(a.dp(4), a.dp(18), 0, a.dp(6)) })
                    val flow = Ui.vbox(a)
                    d.added.forEach { flow.addView(Ui.chip(a, "✓ $it", Pal.soft, Pal.accent), lp(4).apply { width = Ui.WRAP }) }
                    col.addView(flow)
                }
                col.addView(Ui.button(a, L.t("عرض الجدول", "View schedule"), false) { a.go("main") }, lp(14))
            }
        }
    }

    private fun submitCourse(d: CourseDraft, err: TextView) {
        val name = d.name.trim(); val code = d.code.trim()
        fun fail(m: String) = showErr(err, m)
        if (name.isEmpty()) return fail(L.t("اكتب اسم المادة.", "Enter the course name."))
        if (Logic.dupCourse(S, name, code, d.id) != null) return fail(L.t("المادة «$name» موجودة في الجدول. لا يمكن إضافتها مرتين.", "$name is already in the schedule. A course cannot be added twice."))
        if (d.mid.isNotEmpty() && d.fin.isNotEmpty() && d.fin < d.mid) return fail(L.t("تاريخ الامتحان النهائي يجب أن يكون بعد الجزئي.", "The final exam date must be after the midterm."))
        if (d.count == 0) return fail(L.t("اختر عدد المحاضرات في الأسبوع.", "Choose how many lectures per week."))
        for ((i, l) in d.lecs.withIndex()) if (l.d == null || l.s == null || l.e == null)
            return fail(L.t("أكمل اليوم والوقت للمحاضرة ${i + 1}.", "Complete the day and time for lecture ${i + 1}."))
        val lecs = d.lecs.map { Lec(it.d!!, it.s!!, it.e!!, it.room.trim()) }.toMutableList()
        val cf = Logic.findLecConflict(S, lecs, d.id)
        if (cf != null) {
            return if (cf.self) fail(L.t("تعارض بين محاضرتين لنفس المادة: ${Logic.lecRange(S, cf.a)} و${Logic.lecRange(S, cf.b)}.",
                "Two lectures of the same course overlap: ${Logic.lecRange(S, cf.a)} and ${Logic.lecRange(S, cf.b)}."))
            else fail(L.t("تعارض: ${Logic.lecRange(S, cf.a)} يتداخل مع مادة «${cf.course!!.name}» (${Logic.lecRange(S, cf.b)}). غيّر اليوم أو الوقت.",
                "Conflict: ${Logic.lecRange(S, cf.a)} overlaps with ${cf.course!!.name} (${Logic.lecRange(S, cf.b)}). Change the day or time."))
        }
        if (d.mode == "edit") {
            val c = S.courses.first { it.id == d.id }
            c.name = name; c.code = code; c.note = d.note.trim(); c.mid = d.mid; c.fin = d.fin; c.lec = lecs
            a.changed(); a.toast(L.t("تم حفظ التعديل", "Changes saved")); a.go("main")
        } else {
            clearSample()
            S.courses.add(Course(G.uid("c"), name, code, d.note.trim(), d.mid, d.fin, Logic.nextColor(S), lecs))
            a.changed(); a.toast(L.t("أضيفت مادة $name", "$name added"))
            d.added.add(name)
            cd = CourseDraft("new", null).also { it.added.addAll(d.added) }
            a.refresh()
            a.scroller?.post { a.scroller?.scrollTo(0, 0) }
        }
    }

    // ---------- new schedule wizard ----------
    fun openWizard() {
        wd = WizDraft(S.cfg.days.toMutableList(), S.cfg.slots.map { arrayOf<Int?>(it.s, it.e) }.toMutableList())
        a.go("wizard")
    }

    fun wizard(): View {
        val w = wd ?: WizDraft(S.cfg.days.toMutableList(), S.cfg.slots.map { arrayOf<Int?>(it.s, it.e) }.toMutableList()).also { wd = it }
        return a.page(L.t("جدول جديد", "New schedule"), back = "main") { col ->
            col.addView(Ui.tv(a, L.t("اختر أيام الدراسة وأوقات المحاضرات. كل فترة تحدد ساعتها ودقائقها كما تريد.",
                "Choose the study days and the lecture times. Set each time slot to the exact hour and minute."), 14.5f, false, Pal.muted))
            col.addView(Ui.tv(a, L.t("أيام الأسبوع", "Days of the week"), 17f, true).apply { setPadding(a.dp(4), a.dp(18), 0, a.dp(8)) })
            val flow = FlowLayout(a)
            for (d in 0..6) {
                val on = w.days.contains(d)
                flow.addView(Ui.tv(a, L.day(d), 14.5f, true, if (on) Pal.accentFg else Pal.fg).apply {
                    gravity = Gravity.CENTER
                    minHeight = a.dp(44)
                    setPadding(a.dp(16), a.dp(8), a.dp(16), a.dp(8))
                    background = if (on) rounded(Pal.accent, a.dpf(14f)) else rounded(Pal.card, a.dpf(14f), Pal.line, a.dpf(1f))
                    setOnClickListener { if (on) w.days.remove(d) else w.days.add(d); a.refresh() }
                })
            }
            col.addView(flow)
            col.addView(Ui.tv(a, L.t("الأوقات", "Time slots"), 17f, true).apply { setPadding(a.dp(4), a.dp(20), 0, a.dp(8)) })
            for ((i, sl) in w.slots.withIndex()) {
                val card = Ui.hbox(a).apply {
                    background = rounded(Pal.card, a.dpf(18f), Pal.line, a.dpf(1f))
                    setPadding(a.dp(12), a.dp(10), a.dp(6), a.dp(12))
                }
                val c1 = Ui.vbox(a); val c2 = Ui.vbox(a)
                c1.addView(Ui.tv(a, L.t("من", "From"), 12.5f, true, Pal.muted).apply { setPadding(0, 0, 0, a.dp(4)) })
                c1.addView(Ui.pickerField(a, sl[0]?.let { L.fmtT(it) } ?: L.t("اختر", "Select"), sl[0] == null) {
                    Sheets.pickTime(a, L.t("الفترة ${i + 1} · من", "Slot ${i + 1} · From"), sl[0]) { v -> sl[0] = v; if (sl[1] == null) sl[1] = minOf(v + 90, 1439); a.refresh() }
                })
                c2.addView(Ui.tv(a, L.t("إلى", "To"), 12.5f, true, Pal.muted).apply { setPadding(0, 0, 0, a.dp(4)) })
                c2.addView(Ui.pickerField(a, sl[1]?.let { L.fmtT(it) } ?: L.t("اختر", "Select"), sl[1] == null) {
                    Sheets.pickTime(a, L.t("الفترة ${i + 1} · إلى", "Slot ${i + 1} · To"), sl[1]) { v -> sl[1] = v; a.refresh() }
                })
                card.addView(c1, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
                card.addView(View(a), LinearLayout.LayoutParams(a.dp(8), 1))
                card.addView(c2, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
                card.addView(Ui.iconButton(a, R.drawable.ic_x, Pal.danger) { w.slots.removeAt(i); a.refresh() }.apply { setPadding(0, a.dp(14), 0, 0) })
                col.addView(card, lp(8))
            }
            val acts = Ui.hbox(a)
            acts.addView(Ui.button(a, L.t("إضافة وقت", "Add a time"), false) {
                val last = w.slots.lastOrNull()?.get(1) ?: 480
                w.slots.add(arrayOf(minOf(last, 1380), minOf(last + 90, 1439))); a.refresh()
            }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
            acts.addView(View(a), LinearLayout.LayoutParams(a.dp(10), 1))
            acts.addView(Ui.button(a, L.t("الأوقات الافتراضية", "Default times"), false) {
                w.slots.clear(); defaultCfg().slots.forEach { w.slots.add(arrayOf(it.s, it.e)) }; a.refresh()
            }, LinearLayout.LayoutParams(0, Ui.WRAP, 1f))
            col.addView(acts, lp(12))
            val err = errBox(w.err)
            col.addView(err, lp(14))
            col.addView(Ui.button(a, L.t("إنشاء الجدول", "Create schedule")) { createSchedule(w, err) }, lp(14))
        }
    }

    private fun createSchedule(w: WizDraft, err: TextView) {
        val days = w.days.sorted()
        val m = Logic.validateSlots(days, w.slots.map { Pair(it[0], it[1]) })
        if (m.isNotEmpty()) { w.err = m; showErr(err, m); return }
        val slots = w.slots.map { Slot(it[0]!!, it[1]!!) }.sortedBy { it.s }.toMutableList()
        val go = {
            S.cfg = Cfg(days.toMutableList(), slots); S.courses.clear(); S.sample = false; S.exams.removeAll { it.sample }
            a.changed(); a.toast(L.t("جدول جديد", "New schedule")); openCourse("new", null)
        }
        if (S.courses.isNotEmpty() && !S.sample) Sheets.confirm(a, L.t("جدول جديد", "New schedule"),
            L.t("سيتم حذف المواد الحالية (${S.courses.size}) عند إنشاء الجدول الجديد. هل تريد المتابعة؟ امتحاناتك لن تتأثر.",
                "Your current courses (${S.courses.size}) will be deleted when the new schedule is created. Continue? Your exams are not affected."),
            L.t("إنشاء الجدول", "Create schedule"), true) { go() } else go()
    }
}

/** A simple wrapping row of views. */
class FlowLayout(c: android.content.Context) : android.view.ViewGroup(c) {
    private val gap = c.dp(8)
    override fun onMeasure(w: Int, h: Int) {
        val maxW = MeasureSpec.getSize(w)
        var x = 0; var y = 0; var rowH = 0
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            v.measure(MeasureSpec.makeMeasureSpec(maxW, MeasureSpec.AT_MOST), MeasureSpec.UNSPECIFIED)
            if (x + v.measuredWidth > maxW && x > 0) { x = 0; y += rowH + gap; rowH = 0 }
            x += v.measuredWidth + gap; rowH = maxOf(rowH, v.measuredHeight)
        }
        setMeasuredDimension(maxW, y + rowH)
    }
    override fun onLayout(ch: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val maxW = r - l
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        var x = 0; var y = 0; var rowH = 0
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            if (x + v.measuredWidth > maxW && x > 0) { x = 0; y += rowH + gap; rowH = 0 }
            if (rtl) v.layout(maxW - x - v.measuredWidth, y, maxW - x, y + v.measuredHeight)
            else v.layout(x, y, x + v.measuredWidth, y + v.measuredHeight)
            x += v.measuredWidth + gap; rowH = maxOf(rowH, v.measuredHeight)
        }
    }
}
