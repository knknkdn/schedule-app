package com.elsllak.jadwal

/** Language helper. Strings are given inline as (arabic, english). */
object L {
    @JvmField var lang = "ar"
    @JvmField var t12 = true

    val isAr get() = lang == "ar"
    fun t(ar: String, en: String) = if (lang == "ar") ar else en

    private val daysAr = listOf("السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
    private val daysEn = listOf("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    private val shortEn = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")
    private val monthsAr = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
    private val monthsEn = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    private val wdAr = listOf("سب", "أح", "إث", "ثل", "أر", "خم", "جم")
    private val wdEn = listOf("Sa", "Su", "Mo", "Tu", "We", "Th", "Fr")

    fun day(i: Int) = (if (isAr) daysAr else daysEn)[i]
    fun dayShort(i: Int) = if (isAr) daysAr[i] else shortEn[i]
    fun month(i: Int) = (if (isAr) monthsAr else monthsEn)[i - 1]
    fun wd(i: Int) = (if (isAr) wdAr else wdEn)[i]

    fun durAr(n: Int, u: Char): String {
        val one = when (u) { 'm' -> "دقيقة"; 'h' -> "ساعة"; else -> "يوم" }
        val two = when (u) { 'm' -> "دقيقتين"; 'h' -> "ساعتين"; else -> "يومين" }
        val few = when (u) { 'm' -> "دقائق"; 'h' -> "ساعات"; else -> "أيام" }
        val many = when (u) { 'm' -> "دقيقة"; 'h' -> "ساعة"; else -> "يومًا" }
        return when {
            n == 1 -> one
            n == 2 -> two
            n in 3..10 -> "$n $few"
            else -> "$n $many"
        }
    }

    fun durEn(n: Int, u: Char): String {
        val w = when (u) { 'm' -> "minute"; 'h' -> "hour"; else -> "day" }
        return "$n $w" + (if (n == 1) "" else "s")
    }

    fun dur(min: Int): String {
        val (n, u) = when {
            min % 1440 == 0 -> Pair(min / 1440, 'd')
            min % 60 == 0 -> Pair(min / 60, 'h')
            else -> Pair(min, 'm')
        }
        return if (isAr) durAr(n, u) else durEn(n, u)
    }

    fun typeLbl(t: String) = if (t == "fin") t("نهائي", "Final") else t("جزئي", "Midterm")
    fun roomLbl(r: String) = t("القاعة: ", "Room: ") + r
    fun fmtT(min: Int) = Dt.fmtT(min, t12)
}
