package com.zhuanshi.feixing.map

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * WGS-84 与 GCJ-02 互转。
 *
 * 高德/腾讯/百度这批国内底图用的是 GCJ-02（"火星坐标"），
 * 直接拿 WGS-84 的经纬度去取瓦片，zoom 15 时会整体偏 300–500 米，
 * 跑道、航站楼会歪到隔壁去。这里用公开的逆向拟合算法做纠偏，
 * 误差 1–2 米，对看地图足够。
 */
object Gcj02 {

    private const val PI = 3.1415926535897932384626
    private const val A = 6378245.0
    private const val EE = 0.00669342162296594323

    /** 粗略判断是否在中国境外：境外不做偏移。 */
    fun outsideChina(lat: Double, lon: Double): Boolean =
        lon < 73.66 || lon > 135.05 || lat < 3.86 || lat > 53.55

    private fun tLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI) + 320.0 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun tLon(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }

    fun wgs2gcj(lat: Double, lon: Double): DoubleArray {
        if (outsideChina(lat, lon)) return doubleArrayOf(lat, lon)
        var dLat = tLat(lon - 105.0, lat - 35.0)
        var dLon = tLon(lon - 105.0, lat - 35.0)
        val radLat = lat / 180.0 * PI
        var magic = sin(radLat)
        magic = 1 - EE * magic * magic
        val sq = sqrt(magic)
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sq) * PI)
        dLon = (dLon * 180.0) / (A / sq * cos(radLat) * PI)
        return doubleArrayOf(lat + dLat, lon + dLon)
    }

    fun gcj2wgs(lat: Double, lon: Double): DoubleArray {
        if (outsideChina(lat, lon)) return doubleArrayOf(lat, lon)
        val g = wgs2gcj(lat, lon)
        return doubleArrayOf(lat * 2 - g[0], lon * 2 - g[1])
    }
}
