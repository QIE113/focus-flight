package com.zhuanshi.feixing.data

import java.util.Locale
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {

    private const val EARTH_RADIUS_KM = 6371.0

    fun distanceKm(a: City, b: City): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val la1 = Math.toRadians(a.lat)
        val la2 = Math.toRadians(b.lat)
        val h = sin(dLat / 2) * sin(dLat / 2) +
                cos(la1) * cos(la2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_KM * asin(min(1.0, sqrt(h)))
    }

    /** 整段航程的分钟数：约 27 分钟起降滑行 + 巡航 780 km/h */
    fun durationMinutes(a: City, b: City): Int {
        val hours = 0.45 + distanceKm(a, b) / 780.0
        return (hours * 60.0).roundToInt().coerceAtLeast(25)
    }

    fun formatDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) h.toString() + "h " + m + "m" else m.toString() + "min"
    }

    fun formatDistance(km: Double): String =
        String.format(Locale.CHINA, "%,.0f 公里", km)

    fun formatClock(totalSeconds: Int): String {
        val safe = if (totalSeconds < 0) 0 else totalSeconds
        val h = safe / 3600
        val m = (safe % 3600) / 60
        val s = safe % 60
        return if (h > 0) {
            String.format(Locale.CHINA, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.CHINA, "%02d:%02d", m, s)
        }
    }

    fun greeting(hourOfDay: Int): String = when {
        hourOfDay in 0..4 -> "凌晨"
        hourOfDay in 5..7 -> "清晨"
        hourOfDay in 8..10 -> "上午"
        hourOfDay in 11..12 -> "中午"
        hourOfDay in 13..16 -> "下午"
        hourOfDay in 17..19 -> "傍晚"
        else -> "夜晚"
    }

    fun formatDate(epochMillis: Long): String {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = epochMillis
        val mo = cal.get(java.util.Calendar.MONTH) + 1
        val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val hh = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val mm = cal.get(java.util.Calendar.MINUTE)
        return String.format(Locale.CHINA, "%d月%d日 %02d:%02d", mo, d, hh, mm)
    }

    fun lonDelta(a: City, b: City): Double = abs(a.lon - b.lon)
}
