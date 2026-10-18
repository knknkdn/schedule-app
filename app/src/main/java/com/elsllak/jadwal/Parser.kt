package com.elsllak.jadwal

import java.time.LocalDate

/** Reads exam rows out of OCR text. Ported from the web version. */
object Parser {
    class Found(val iso: String, val text: String)
    class Time(val min: Int, val text: String)
    class Result(
        val code: String, var found: Boolean = false, var date: String = "",
        var s: Int? = null, var e: Int? = null, var room: String = "",
        var guessEnd: Boolean = false, var line: String = ""
    )

    fun normDigits(s: String): String {
        val sb = StringBuilder()
        for (ch in s) {
            val a = "٠١٢٣٤٥٦٧٨٩".indexOf(ch)
            val b = "۰۱۲۳۴۵۶۷۸۹".indexOf(ch)
            sb.append(if (a >= 0) ('0' + a) else if (b >= 0) ('0' + b) else ch)
        }
        return sb.toString()
    }

    private val groups = listOf("O0Q", "IL1|!", "S5", "B8", "Z2")

    fun codeRegex(code: String): Regex? {
        val chars = normDigits(code).uppercase().replace(Regex("[\\s\\-_.]"), "")
        if (chars.isEmpty()) return null
        val parts = ArrayList<String>()
        for (c in chars) {
            val g = groups.lastOrNull { it.indexOf(c) >= 0 }
            parts.add(if (g != null) "[" + g + "]" else Regex.escape(c.toString()))
        }
        return Regex("(?<![A-Za-z0-9])" + parts.joinToString("[\\s\\-_.]*") + "(?![A-Za-z0-9])", RegexOption.IGNORE_CASE)
    }

    private val months = mapOf(
        "jan" to 1, "january" to 1, "feb" to 2, "february" to 2, "mar" to 3, "march" to 3, "apr" to 4, "april" to 4,
        "may" to 5, "jun" to 6, "june" to 6, "jul" to 7, "july" to 7, "aug" to 8, "august" to 8,
        "sep" to 9, "sept" to 9, "september" to 9, "oct" to 10, "october" to 10, "nov" to 11, "november" to 11,
        "dec" to 12, "december" to 12,
        "يناير" to 1, "فبراير" to 2, "مارس" to 3, "أبريل" to 4, "ابريل" to 4, "مايو" to 5, "يونيو" to 6, "يوليو" to 7,
        "أغسطس" to 8, "اغسطس" to 8, "سبتمبر" to 9, "أكتوبر" to 10, "اكتوبر" to 10, "نوفمبر" to 11, "ديسمبر" to 12
    )
    private const val MON_RE = "(january|february|march|april|may|june|july|august|september|october|november|december|sept|jan|feb|mar|apr|jun|jul|aug|sep|oct|nov|dec|يناير|فبراير|مارس|أبريل|ابريل|مايو|يونيو|يوليو|أغسطس|اغسطس|سبتمبر|أكتوبر|اكتوبر|نوفمبر|ديسمبر)"

    private fun mk(y: Int, mo: Int, d: Int): String? {
        if (mo < 1 || mo > 12 || d < 1 || d > 31) return null
        return try { LocalDate.of(y, mo, d).toString() } catch (e: Exception) { null }
    }

    fun findDate(text: String, todayIso: String): Found? {
        val today = LocalDate.parse(todayIso)
        val ty = today.year
        fun nearYear(mo: Int, d: Int, yy0: Int): String? {
            var yy = yy0
            if (yy != 0) { if (yy < 100) yy += 2000; return mk(yy, mo, d) }
            val r = mk(ty, mo, d) ?: return null
            val diff = LocalDate.parse(r).toEpochDay() - today.toEpochDay()
            return if (diff < -90) mk(ty + 1, mo, d) else r
        }
        var m = Regex("(\\d{4})\\s*[/\\-.]\\s*(\\d{1,2})\\s*[/\\-.]\\s*(\\d{1,2})").find(text)
        if (m != null) { val r = mk(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()); if (r != null) return Found(r, m.value) }
        m = Regex("(\\d{1,2})\\s*[/\\-.]\\s*(\\d{1,2})\\s*[/\\-.]\\s*(\\d{4}|\\d{2})(?!\\d)").find(text)
        if (m != null) { val r = nearYear(m.groupValues[2].toInt(), m.groupValues[1].toInt(), m.groupValues[3].toInt()); if (r != null) return Found(r, m.value) }
        m = Regex("(\\d{1,2})\\s*(?:st|nd|rd|th)?\\s*$MON_RE\\.?,?\\s*(\\d{4})?", RegexOption.IGNORE_CASE).find(text)
        if (m != null) {
            val mo = months[m.groupValues[2].lowercase()]
            val yy = m.groupValues[3].let { if (it.isEmpty()) 0 else it.toInt() }
            val r = if (mo != null) nearYear(mo, m.groupValues[1].toInt(), yy) else null
            if (r != null) return Found(r, m.value)
        }
        m = Regex("$MON_RE\\.?\\s*(\\d{1,2})(?:st|nd|rd|th)?(?!\\d)(?:,?\\s*(\\d{4}))?", RegexOption.IGNORE_CASE).find(text)
        if (m != null) {
            val mo = months[m.groupValues[1].lowercase()]
            val yy = m.groupValues[3].let { if (it.isEmpty()) 0 else it.toInt() }
            val r = if (mo != null) nearYear(mo, m.groupValues[2].toInt(), yy) else null
            if (r != null) return Found(r, m.value)
        }
        m = Regex("(?<![\\d:])(\\d{1,2})\\s*[/\\-]\\s*(\\d{1,2})(?![\\d:/\\-])").find(text)
        if (m != null) { val r = nearYear(m.groupValues[2].toInt(), m.groupValues[1].toInt(), 0); if (r != null) return Found(r, m.value) }
        return null
    }

    fun findTimes(text: String): List<Time> {
        val out = ArrayList<Time>()
        val re = Regex("(\\d{1,2})\\s*[:.]\\s*(\\d{2})\\s*(AM|PM|am|pm|A\\.M\\.|P\\.M\\.|ص|م)?")
        for (m in re.findAll(text)) {
            var h = m.groupValues[1].toInt()
            val mi = m.groupValues[2].toInt()
            if (h > 23 || mi > 59) continue
            val suf = m.groupValues[3].lowercase().replace(".", "")
            if (suf == "pm" || suf == "م") { if (h < 12) h += 12 }
            else if (suf == "am" || suf == "ص") { if (h == 12) h = 0 }
            else if (h in 1..6) h += 12
            out.add(Time(h * 60 + mi, m.value))
        }
        return out
    }

    fun findRoom(text: String): String {
        val m = Regex("(?:قاعة|القاعة|قاعه|مدرج|معمل|مختبر|hall|room|rm|lab|amphi|theat(?:er|re))\\.?\\s*[:\\-]?\\s*([A-Za-z0-9\\u0600-\\u06FF]+(?:[\\-/][A-Za-z0-9]+)?)", RegexOption.IGNORE_CASE).find(text)
        return m?.value?.replace(Regex("\\s+"), " ")?.trim() ?: ""
    }

    private fun anyCode(line: String, codes: List<String>) =
        codes.any { c -> codeRegex(c)?.containsMatchIn(line) == true }

    fun parseExamText(text: String, codes: List<String>, todayIso: String): List<Result> {
        val lines = normDigits(text).split(Regex("\\r?\\n")).map { it.trim() }.filter { it.isNotEmpty() }
        val res = ArrayList<Result>()
        for (code in codes) {
            val re = codeRegex(code)
            val out = Result(code)
            if (re == null) { res.add(out); continue }
            val idx = lines.indexOfFirst { re.containsMatchIn(it) }
            if (idx < 0) { res.add(out); continue }
            out.found = true
            out.line = lines[idx]
            val line = lines[idx].replace(re, " ")
            var dt = findDate(line, todayIso)
            if (dt == null) {
                var k = idx - 1
                while (k >= 0 && k >= idx - 10) { dt = findDate(lines[k], todayIso); if (dt != null) break; k-- }
            }
            var rest = line
            if (dt != null) { out.date = dt.iso; rest = rest.replace(dt.text, " ") }
            var times = findTimes(rest)
            if (times.isEmpty()) {
                val nb = listOf(lines.getOrNull(idx + 1), lines.getOrNull(idx - 1))
                for (n in nb) {
                    if (times.isNotEmpty()) break
                    if (n != null && !anyCode(n, codes)) times = findTimes(if (dt != null) n.replace(dt.text, " ") else n)
                }
            }
            if (times.isNotEmpty()) {
                out.s = times[0].min
                var e: Int? = null
                if (times.size > 1) {
                    e = times[1].min
                    if (e <= out.s!! && e < 12 * 60) e += 12 * 60
                    if (e <= out.s!!) e = null
                }
                if (e == null || times.size == 1) {
                    e = minOf(out.s!! + 120, 23 * 60 + 59)
                    out.guessEnd = true
                }
                out.e = e
                for (t in times) rest = rest.replace(t.text, " ")
            }
            out.room = findRoom(rest)
            res.add(out)
        }
        return res
    }
}
