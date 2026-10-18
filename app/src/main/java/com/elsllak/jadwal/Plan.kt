package com.elsllak.jadwal

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Builds the list of upcoming reminders. Pure: no Android classes. */
class Alarm(val at: Long, val title: String, val body: String)

object Plan {
    private fun make(n: NotifCfg, name: String, whenTxt: String, defTitle: String, rest: String, note: String): Pair<String, String> {
        val cu = n.custom && n.text.isNotBlank()
        val title = if (cu) n.text.replace("{name}", name).replace("{when}", whenTxt).trim() else defTitle
        val body = (if (cu) name + (if (whenTxt.isNotEmpty()) " · $whenTxt" else "") + " · " else "") + rest +
            (if (note.isNotEmpty()) "\n$note" else "")
        return Pair(title, body)
    }

    fun lecTitle(n: String, d: String?) =
        if (d == null) L.t("محاضرة «$n» تبدأ الآن", "$n lecture starts now") else L.t("محاضرة «$n» بعد $d", "$n lecture in $d")

    fun examTitle(n: String, t: String, d: String?) =
        if (d == null) L.t("امتحان $t «$n» يبدأ الآن", "$n $t exam starts now")
        else L.t("امتحان $t «$n» بعد $d", "$n $t exam in $d")

    fun whenText(off: Int) = if (off == 0) L.t("الآن", "Now") else L.t("بعد " + L.dur(off), "in " + L.dur(off))

    fun build(S: AppState, P: Prefs, now: Long, zone: ZoneId): List<Alarm> {
        val list = ArrayList<Alarm>()
        val nowDt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), zone)
        val today = nowDt.toLocalDate()

        if (P.lec.on && !S.sample) {
            for (c in S.courses) for (l in c.lec) {
                val sl = S.cfg.slots.getOrNull(l.s) ?: continue
                val el = S.cfg.slots.getOrNull(l.e) ?: continue
                for (off in P.lec.offs) {
                    // Next date (today or later) on which the lecture's day occurs, then subtract the offset.
                    var found: Long? = null
                    for (k in 0..14) {
                        val d = today.plusDays(k.toLong())
                        if (Dt.absDay(d) != l.d) continue
                        val at = d.atStartOfDay().plusMinutes((sl.s - off).toLong()).atZone(zone).toInstant().toEpochMilli()
                        if (at > now) { found = at; break }
                    }
                    if (found == null) continue
                    val w = whenText(off)
                    val d = if (off == 0) null else L.dur(off)
                    val m = make(P.lec, c.name, w, lecTitle(c.name, d),
                        L.fmtT(sl.s) + " – " + L.fmtT(el.e) + (if (l.room.isNotEmpty()) " · " + L.roomLbl(l.room) else ""), c.note)
                    list.add(Alarm(found, m.first, m.second))
                }
            }
        }
        if (P.exam.on) {
            for (x in Logic.allExams(S)) {
                if (x.sample) continue
                val d = Dt.parse(x.date) ?: continue
                val tl = L.typeLbl(x.type)
                if (x.s == null || x.e == null) {
                    val at = d.minusDays(1).atTime(20, 0).atZone(zone).toInstant().toEpochMilli()
                    if (at > now) {
                        val m = make(P.exam, "${x.name} ($tl)", "", L.t("غدًا امتحان $tl «${x.name}»", "Tomorrow: ${x.name} $tl exam"), Dt.fmtD(x.date), x.note)
                        list.add(Alarm(at, m.first, m.second))
                    }
                } else {
                    for (off in P.exam.offs) {
                        val at = d.atStartOfDay().plusMinutes((x.s - off).toLong()).atZone(zone).toInstant().toEpochMilli()
                        if (at <= now) continue
                        val m = make(P.exam, "${x.name} ($tl)", whenText(off), examTitle(x.name, tl, if (off == 0) null else L.dur(off)),
                            Dt.fmtD(x.date) + " · " + L.fmtT(x.s) + " – " + L.fmtT(x.e) + (if (x.room.isNotEmpty()) " · " + L.roomLbl(x.room) else ""), x.note)
                        list.add(Alarm(at, m.first, m.second))
                    }
                }
            }
        }
        list.sortBy { it.at }
        return if (list.size > 400) list.subList(0, 400) else list
    }
}
