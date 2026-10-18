package com.elsllak.jadwal

/** Pure app logic: names, conflicts, duplicates, exam validation. */
object Names {
    fun normN(s: String?): String =
        (s ?: "").lowercase()
            .replace(Regex("[ً-ٰٟ]"), "")
            .replace(Regex("[أإآٱ]"), "ا")
            .replace("ة", "ه").replace("ى", "ي")
            .replace(Regex("\\s+"), " ").trim()

    fun normC(s: String?): String = (s ?: "").uppercase().replace(Regex("[\\s\\-_.]"), "")

    fun same(an: String, ac: String, bn: String, bc: String): Boolean {
        if (ac.isNotEmpty() && bc.isNotEmpty() && normC(ac) == normC(bc)) return true
        val x = normN(an)
        return x.isNotEmpty() && x == normN(bn)
    }
}

class LecConflict(val a: Lec, val b: Lec, val self: Boolean, val course: Course?)

class ExamView(
    val id: String, val name: String, val code: String, val type: String, val date: String,
    val s: Int?, val e: Int?, val room: String, val note: String,
    val derived: Boolean, val cid: String, val sample: Boolean
)

object Logic {
    fun ov(a: Lec, b: Lec) = a.d == b.d && !(a.e < b.s || b.e < a.s)

    fun findLecConflict(S: AppState, lecs: List<Lec>, ignoreId: String?): LecConflict? {
        for (i in lecs.indices) {
            for (j in i + 1 until lecs.size) if (ov(lecs[i], lecs[j])) return LecConflict(lecs[i], lecs[j], true, null)
            for (c in S.courses) {
                if (c.id == ignoreId) continue
                for (m in c.lec) if (ov(lecs[i], m)) return LecConflict(lecs[i], m, false, c)
            }
        }
        return null
    }

    fun dupCourse(S: AppState, name: String, code: String, ignoreId: String?): Course? =
        S.courses.firstOrNull { it.id != ignoreId && Names.same(it.name, it.code, name, code) }

    fun courseFor(S: AppState, name: String, code: String): Course? =
        S.courses.firstOrNull { Names.same(it.name, it.code, name, code) }

    fun dupExam(S: AppState, c: Exam): Exam? =
        S.exams.firstOrNull { it.id != c.id && it.type == c.type && Names.same(it.name, it.code, c.name, c.code) }

    fun examConflict(S: AppState, c: Exam): Exam? =
        S.exams.firstOrNull {
            it.id != c.id && it.date == c.date && it.s != null && it.e != null && c.s != null && c.e != null &&
                c.s!! < it.e!! && it.s!! < c.e!!
        }

    /** Returns an error message or "". */
    fun validateExam(S: AppState, c: Exam): String {
        if (c.name.isBlank()) return L.t("اكتب اسم المادة.", "Enter the course name.")
        if (c.date.isEmpty()) return L.t("اختر تاريخ الامتحان.", "Choose the exam date.")
        if (c.s == null || c.e == null) return L.t("حدد وقت بداية الامتحان ونهايته.", "Set the exam start and end time.")
        if (c.e!! <= c.s!!) return L.t("يجب أن ينتهي الامتحان بعد أن يبدأ.", "The exam must end after it starts.")
        if (dupExam(S, c) != null) {
            val t = L.typeLbl(c.type)
            return L.t("امتحان $t لمادة «${c.name}» مضاف مسبقًا. لا يمكن إضافة المادة مرتين.",
                "A $t exam for ${c.name} already exists. A course cannot be added twice.")
        }
        val cf = examConflict(S, c)
        if (cf != null) {
            val r = L.fmtT(cf.s!!) + " – " + L.fmtT(cf.e!!)
            return L.t("تعارض: يتداخل مع امتحان «${cf.name}» ($r) في نفس اليوم.",
                "Conflict: it overlaps the ${cf.name} exam ($r) on the same day.")
        }
        return ""
    }

    fun allExams(S: AppState): List<ExamView> {
        val list = ArrayList<ExamView>()
        for (e in S.exams) list.add(ExamView(e.id, e.name, e.code, e.type, e.date, e.s, e.e, e.room, e.note, false, "", e.sample))
        for (c in S.courses) for (k in listOf("mid", "fin")) {
            val d = if (k == "mid") c.mid else c.fin
            if (d.isEmpty()) continue
            val has = S.exams.any { it.type == k && Names.same(it.name, it.code, c.name, c.code) }
            if (!has) list.add(ExamView("d:${c.id}:$k", c.name, c.code, k, d, null, null, "", c.note, true, c.id, false))
        }
        list.sortWith(compareBy<ExamView>({ it.date }, { it.s ?: 9999 }))
        return list
    }

    fun nextColor(S: AppState): Int {
        val cnt = IntArray(8)
        S.courses.forEach { cnt[it.color % 8]++ }
        var best = 0
        for (i in 1 until 8) if (cnt[i] < cnt[best]) best = i
        return best
    }

    fun hashColor(s: String): Int {
        var h = 0L
        for (ch in s) h = (h * 31 + ch.code) and 0xFFFFFFFFL
        return (h % 8).toInt()
    }

    fun colorOf(S: AppState, name: String, code: String): Int =
        courseFor(S, name, code)?.color ?: hashColor(Names.normN(name).ifEmpty { Names.normC(code) })

    fun lecRange(S: AppState, l: Lec): String {
        val a = L.fmtT(S.cfg.slots[l.s].s)
        val b = L.fmtT(S.cfg.slots[l.e].e)
        return L.day(l.d) + (if (L.isAr) " " else ", ") + L.t("من $a إلى $b", "$a to $b")
    }

    /** Wizard validation: returns error text or "". Slots are in minutes (null = not filled). */
    fun validateSlots(days: List<Int>, slots: List<Pair<Int?, Int?>>): String {
        if (days.isEmpty()) return L.t("اختر يومًا واحدًا على الأقل.", "Choose at least one day.")
        if (slots.isEmpty()) return L.t("أضف وقتًا واحدًا على الأقل.", "Add at least one time slot.")
        for ((i, p) in slots.withIndex()) {
            if (p.first == null || p.second == null || p.second!! <= p.first!!)
                return L.t("أكمل وقت البداية والنهاية للفترة ${i + 1}، ويجب أن تنتهي بعد أن تبدأ.",
                    "Complete the start and end time of slot ${i + 1}. It must end after it starts.")
        }
        val order = slots.indices.sortedBy { slots[it].first!! }
        for (k in 0 until order.size - 1) {
            val a = order[k]; val b = order[k + 1]
            if (slots[b].first!! < slots[a].second!!)
                return L.t("الفترتان ${minOf(a, b) + 1} و${maxOf(a, b) + 1} متداخلتان. اجعل كل فترة تبدأ بعد انتهاء التي قبلها.",
                    "Slots ${minOf(a, b) + 1} and ${maxOf(a, b) + 1} overlap. Each slot must start after the previous one ends.")
        }
        return ""
    }
}
