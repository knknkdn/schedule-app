package com.elsllak.jadwal

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Global app data. */
object G {
    lateinit var s: AppState
    lateinit var p: Prefs
    fun uid(p: String) = p + java.lang.Long.toString(System.currentTimeMillis(), 36) + (Math.random() * 1e6).toInt().toString(36)
}

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("jadwal", Context.MODE_PRIVATE)

    fun load(c: Context) {
        val sp = prefs(c)
        G.p = try { sp.getString("prefs", null)?.let { parsePrefs(JSONObject(it)) } ?: defaultPrefs() } catch (e: Exception) { defaultPrefs() }
        G.s = try { sp.getString("state", null)?.let { parseState(JSONObject(it)) } ?: sample() } catch (e: Exception) { sample() }
        L.lang = G.p.lang; L.t12 = G.p.t12; Fx.enabled = G.p.fx
    }

    fun saveState(c: Context) { prefs(c).edit().putString("state", stateJson(G.s).toString()).apply() }
    fun savePrefs(c: Context) { prefs(c).edit().putString("prefs", prefsJson(G.p).toString()).apply() }

    private fun notifJson(n: NotifCfg) = JSONObject().put("on", n.on).put("offs", JSONArray(n.offs)).put("custom", n.custom).put("text", n.text)
    private fun parseNotif(o: JSONObject?, d: NotifCfg): NotifCfg {
        if (o == null) return d
        val offs = ArrayList<Int>()
        o.optJSONArray("offs")?.let { for (i in 0 until it.length()) offs.add(it.getInt(i)) }
        return NotifCfg(o.optBoolean("on", true), offs, o.optBoolean("custom", false), o.optString("text", ""))
    }

    fun prefsJson(p: Prefs) = JSONObject().put("lang", p.lang).put("theme", p.theme).put("t12", p.t12).put("fx", p.fx)
        .put("lec", notifJson(p.lec)).put("exam", notifJson(p.exam))

    fun parsePrefs(o: JSONObject): Prefs {
        val d = defaultPrefs()
        return Prefs(o.optString("lang", d.lang), o.optString("theme", d.theme), o.optBoolean("t12", d.t12), o.optBoolean("fx", d.fx),
            parseNotif(o.optJSONObject("lec"), d.lec), parseNotif(o.optJSONObject("exam"), d.exam))
    }

    fun stateJson(s: AppState): JSONObject {
        val cfg = JSONObject().put("days", JSONArray(s.cfg.days))
            .put("slots", JSONArray().also { a -> s.cfg.slots.forEach { a.put(JSONObject().put("s", it.s).put("e", it.e)) } })
        val courses = JSONArray()
        for (c in s.courses) courses.put(JSONObject().put("id", c.id).put("name", c.name).put("code", c.code).put("note", c.note)
            .put("mid", c.mid).put("fin", c.fin).put("color", c.color)
            .put("lec", JSONArray().also { a -> c.lec.forEach { a.put(JSONObject().put("d", it.d).put("s", it.s).put("e", it.e).put("room", it.room)) } }))
        val exams = JSONArray()
        for (x in s.exams) exams.put(JSONObject().put("id", x.id).put("name", x.name).put("code", x.code).put("type", x.type)
            .put("date", x.date).put("s", x.s ?: JSONObject.NULL).put("e", x.e ?: JSONObject.NULL)
            .put("room", x.room).put("note", x.note).put("sample", x.sample))
        return JSONObject().put("cfg", cfg).put("courses", courses).put("exams", exams)
            .put("period", JSONObject().put("from", s.period.from).put("to", s.period.to)).put("sample", s.sample)
    }

    fun parseState(o: JSONObject): AppState {
        val cfgO = o.getJSONObject("cfg")
        val days = ArrayList<Int>(); cfgO.getJSONArray("days").let { for (i in 0 until it.length()) days.add(it.getInt(i)) }
        val slots = ArrayList<Slot>(); cfgO.getJSONArray("slots").let { for (i in 0 until it.length()) slots.add(Slot(it.getJSONObject(i).getInt("s"), it.getJSONObject(i).getInt("e"))) }
        val cfg = if (days.isEmpty() || slots.isEmpty()) defaultCfg() else Cfg(days, slots)
        val courses = ArrayList<Course>()
        o.optJSONArray("courses")?.let { a ->
            for (i in 0 until a.length()) {
                val c = a.getJSONObject(i)
                val lecs = ArrayList<Lec>()
                c.optJSONArray("lec")?.let { la -> for (j in 0 until la.length()) { val l = la.getJSONObject(j); lecs.add(Lec(l.getInt("d"), l.getInt("s"), l.getInt("e"), l.optString("room", ""))) } }
                courses.add(Course(c.getString("id"), c.getString("name"), c.optString("code", ""), c.optString("note", ""),
                    c.optString("mid", ""), c.optString("fin", ""), c.optInt("color", 0), lecs))
            }
        }
        val exams = ArrayList<Exam>()
        o.optJSONArray("exams")?.let { a ->
            for (i in 0 until a.length()) {
                val x = a.getJSONObject(i)
                exams.add(Exam(x.getString("id"), x.getString("name"), x.optString("code", ""), x.optString("type", "mid"), x.getString("date"),
                    if (x.isNull("s")) null else x.getInt("s"), if (x.isNull("e")) null else x.getInt("e"),
                    x.optString("room", ""), x.optString("note", ""), x.optBoolean("sample", false)))
            }
        }
        val per = o.optJSONObject("period")
        return AppState(cfg, courses, exams, Period(per?.optString("from", "") ?: "", per?.optString("to", "") ?: ""), o.optBoolean("sample", false))
    }

    fun sample(): AppState {
        val t = Dt.todayS()
        val cs = mutableListOf(
            Course("s1", "الرياضيات", "MATH101", "", "", "", 0, mutableListOf(Lec(0, 0, 0, "قاعة 3"), Lec(2, 0, 0, "قاعة 3"))),
            Course("s2", "الفيزياء", "PHYS102", "", "", "", 1, mutableListOf(Lec(0, 1, 1, "قاعة 5"), Lec(3, 2, 2, "معمل 1"))),
            Course("s3", "البرمجة", "CS110", "", "", "", 2, mutableListOf(Lec(1, 1, 2, "مختبر 2"), Lec(4, 0, 0, "مختبر 2"))),
            Course("s4", "اللغة الإنجليزية", "ENG100", "", "", "", 3, mutableListOf(Lec(2, 3, 3, ""), Lec(5, 1, 1, ""))),
            Course("s5", "الإحصاء", "STAT201", "", "", "", 4, mutableListOf(Lec(3, 4, 4, "قاعة 8")))
        )
        val ex = mutableListOf(
            Exam("sx1", "الرياضيات", "MATH101", "mid", Dt.addDays(t, 9), 600, 690, "قاعة 3", "", sample = true),
            Exam("sx2", "الفيزياء", "PHYS102", "mid", Dt.addDays(t, 10), 540, 630, "قاعة 5", "", sample = true)
        )
        return AppState(defaultCfg(), cs, ex, Period("", ""), true)
    }
}
