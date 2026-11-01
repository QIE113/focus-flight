package com.zhuanshi.feixing.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class FlightStore(context: Context) {

    private val sp = context.getSharedPreferences("focus_flight", Context.MODE_PRIVATE)

    fun records(): List<FlightRecord> {
        val raw = sp.getString(KEY_RECORDS, "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<FlightRecord>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    FlightRecord(
                        fromCode = o.optString("fc"),
                        fromName = o.optString("fn"),
                        toCode = o.optString("tc"),
                        toName = o.optString("tn"),
                        minutes = o.optInt("m"),
                        finishedAt = o.optLong("t")
                    )
                )
            }
            out.sortedBy { it.finishedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(record: FlightRecord) {
        val list = records().toMutableList()
        list.add(record)
        while (list.size > 200) {
            list.removeAt(0)
        }
        val arr = JSONArray()
        list.forEach { r ->
            val o = JSONObject()
            o.put("fc", r.fromCode)
            o.put("fn", r.fromName)
            o.put("tc", r.toCode)
            o.put("tn", r.toName)
            o.put("m", r.minutes)
            o.put("t", r.finishedAt)
            arr.put(o)
        }
        sp.edit().putString(KEY_RECORDS, arr.toString()).apply()
    }

    fun originCode(): String = sp.getString(KEY_ORIGIN, "") ?: ""

    fun saveOrigin(code: String) {
        sp.edit().putString(KEY_ORIGIN, code).apply()
    }

    fun destinationCode(): String = sp.getString(KEY_ROUTE, "") ?: ""

    fun saveDestination(code: String) {
        sp.edit().putString(KEY_ROUTE, code).apply()
    }

    companion object {
        private const val KEY_RECORDS = "records"
        private const val KEY_ROUTE = "destination"
        private const val KEY_ORIGIN = "origin"
    }
}
