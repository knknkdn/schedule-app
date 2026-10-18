package com.elsllak.jadwal

import java.time.LocalDate
import java.util.Locale

/** Data model. Minutes are counted from midnight. Day index: 0 = Saturday ... 6 = Friday. */
class Slot(var s: Int, var e: Int)

class Cfg(var days: MutableList<Int>, var slots: MutableList<Slot>)

class Lec(var d: Int, var s: Int, var e: Int, var room: String)

class Course(
    val id: String,
    var name: String,
    var code: String,
    var note: String,
    var mid: String,
    var fin: String,
    var color: Int,
    var lec: MutableList<Lec>
)

class Exam(
    val id: String,
    var name: String,
    var code: String,
    var type: String,          // "mid" or "fin"
    var date: String,          // yyyy-MM-dd
    var s: Int?,
    var e: Int?,
    var room: String,
    var note: String,
    var sample: Boolean = false,
    var derived: Boolean = false,
    var cid: String = ""
)

class Period(var from: String, var to: String)

class AppState(
    var cfg: Cfg,
    var courses: MutableList<Course>,
    var exams: MutableList<Exam>,
    var period: Period,
    var sample: Boolean
)

class NotifCfg(var on: Boolean, var offs: MutableList<Int>, var custom: Boolean, var text: String)

class Prefs(
    var lang: String,          // "ar" or "en"
    var theme: String,         // "auto", "light", "dark"
    var t12: Boolean,
    var fx: Boolean,
    var lec: NotifCfg,
    var exam: NotifCfg
)

fun defaultPrefs() = Prefs(
    "ar", "auto", true, true,
    NotifCfg(true, mutableListOf(30), false, ""),
    NotifCfg(true, mutableListOf(1440, 60), false, "")
)

fun defaultCfg(): Cfg {
    val starts = listOf(510, 600, 690, 780, 870, 960, 1050)
    return Cfg(mutableListOf(0, 1, 2, 3, 4, 5), starts.map { Slot(it, it + 90) }.toMutableList())
}

/** Dates and times. */
object Dt {
    @JvmField var todayOverride: LocalDate? = null

    fun today(): LocalDate = todayOverride ?: LocalDate.now()
    fun todayS(): String = today().toString()

    fun parse(s: String?): LocalDate? {
        if (s == null || s.isEmpty()) return null
        return try { LocalDate.parse(s) } catch (e: Exception) { null }
    }

    /** Saturday = 0 ... Friday = 6. */
    fun absDay(d: LocalDate): Int = (d.dayOfWeek.value % 7 + 1) % 7

    fun absDay(s: String): Int = parse(s)?.let { absDay(it) } ?: 0

    fun addDays(s: String, n: Int): String = parse(s)!!.plusDays(n.toLong()).toString()

    fun diffDays(a: String, b: String): Int =
        (parse(b)!!.toEpochDay() - parse(a)!!.toEpochDay()).toInt()

    fun fmtD(s: String): String {
        val d = parse(s) ?: return ""
        return String.format(Locale.ENGLISH, "%02d/%02d/%04d", d.dayOfMonth, d.monthValue, d.year)
    }

    fun fmtDM(s: String): String {
        val d = parse(s) ?: return ""
        return String.format(Locale.ENGLISH, "%02d/%02d", d.dayOfMonth, d.monthValue)
    }

    fun hm(str: String?): Int? {
        if (str == null) return null
        val m = Regex("^(\\d{1,2}):(\\d{2})").find(str) ?: return null
        return m.groupValues[1].toInt() * 60 + m.groupValues[2].toInt()
    }

    fun toHM(min: Int): String = String.format(Locale.ENGLISH, "%02d:%02d", min / 60, min % 60)

    fun fmtT(min: Int, t12: Boolean): String {
        if (!t12) return toHM(min)
        val h = (min / 60) % 24
        val m = min % 60
        val h12 = if (h % 12 == 0) 12 else h % 12
        return String.format(Locale.ENGLISH, "%d:%02d", h12, m) + " " + (if (h < 12) L.t("ص", "AM") else L.t("م", "PM"))
    }
}
