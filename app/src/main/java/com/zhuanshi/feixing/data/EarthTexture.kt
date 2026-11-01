package com.zhuanshi.feixing.data

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 离线地球贴图：用内置的陆地轮廓在设备上直接生成一张等距圆柱投影的星球纹理
 * （1024 × 512），海面、雨林、温带、荒漠、极冠各有色阶，还带一层噪点。
 * 生成一次约 30 ms，之后缓存复用。整张图不依赖网络。
 */
object EarthTexture {

    const val WIDTH = 1024
    const val HEIGHT = 512

    private var builtPixels: IntArray? = null
    private var builtBitmap: ImageBitmap? = null

    val pixels: IntArray
        get() {
            val cached = builtPixels
            if (cached != null) return cached
            val made = build()
            builtPixels = made
            return made
        }

    val bitmap: ImageBitmap
        get() {
            val cached = builtBitmap
            if (cached != null) return cached
            val px = pixels
            val bmp = Bitmap.createBitmap(px, WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
            val img = bmp.asImageBitmap()
            builtBitmap = img
            return img
        }

    private fun hash(x: Int, y: Int): Float {
        var h = x * 374761393 + y * 668265263
        h = (h xor (h shr 13)) * 1274126177
        h = h xor (h shr 16)
        return (h and 0xFFFF) / 65535f
    }

    private fun ocean(lat: Float, noise: Float): Int {
        val cold = (abs(lat) / 90f).coerceIn(0f, 1f)
        val r = (6f + 6f * (1f - cold) + noise * 6f).roundToInt()
        val g = (16f + 30f * (1f - cold) + noise * 12f).roundToInt()
        val b = (44f + 54f * (1f - cold) + noise * 16f).roundToInt()
        return argb(255, r, g, b)
    }

    private fun land(lat: Float, lon: Float, noise: Float): Int {
        val a = abs(lat)
        val base: IntArray = when {
            a > 67f -> intArrayOf(226, 236, 240)
            a > 57f -> intArrayOf(74, 96, 74)
            a > 40f -> intArrayOf(62, 96, 62)
            a > 28f -> intArrayOf(96, 112, 70)
            a > 17f -> intArrayOf(148, 126, 78)
            else -> intArrayOf(46, 92, 52)
        }
        val tilt = if (lon > -20f && lon < 60f && lat > 15f && lat < 34f) -22 else 0
        val n = (noise - 0.5f) * 26f
        val r = (base[0] + n + tilt).coerceIn(0f, 255f).roundToInt()
        val g = (base[1] + n + tilt * 0.7f).coerceIn(0f, 255f).roundToInt()
        val b = (base[2] + n * 0.8f + tilt * 0.4f).coerceIn(0f, 255f).roundToInt()
        return argb(255, r, g, b)
    }

    private fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        (a shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    private fun build(): IntArray {
        val out = IntArray(WIDTH * HEIGHT)
        for (y in 0 until HEIGHT) {
            val lat = 90f - (y + 0.5f) * 180f / HEIGHT
            for (x in 0 until WIDTH) {
                val n = hash(x, y)
                out[y * WIDTH + x] = ocean(lat, n)
            }
        }

        for (ring in WorldLand.rings) {
            val n = ring.size / 2
            if (n < 3) continue
            var minLat = Float.MAX_VALUE
            var maxLat = -Float.MAX_VALUE
            var i = 0
            while (i < ring.size) {
                val la = ring[i]
                if (la < minLat) minLat = la
                if (la > maxLat) maxLat = la
                i += 2
            }
            val y0 = (((90f - maxLat) / 180f) * HEIGHT).toInt().coerceIn(0, HEIGHT - 1)
            val y1 = (((90f - minLat) / 180f) * HEIGHT).toInt().coerceIn(0, HEIGHT - 1)
            val xs = ArrayList<Float>(8)
            for (y in y0..y1) {
                val lat = 90f - (y + 0.5f) * 180f / HEIGHT
                xs.clear()
                var k = 0
                while (k < n) {
                    val k2 = (k + 1) % n
                    val la1 = ring[k * 2]
                    val lo1 = ring[k * 2 + 1]
                    val la2 = ring[k2 * 2]
                    val lo2 = ring[k2 * 2 + 1]
                    if ((la1 <= lat && la2 > lat) || (la2 <= lat && la1 > lat)) {
                        val t = (lat - la1) / (la2 - la1)
                        xs.add(lo1 + t * (lo2 - lo1))
                    }
                    k++
                }
                if (xs.size < 2) continue
                xs.sort()
                var s = 0
                while (s + 1 < xs.size) {
                    val loA = xs[s]
                    val loB = xs[s + 1]
                    var xa = (((loA + 180f) / 360f) * WIDTH).toInt()
                    var xb = (((loB + 180f) / 360f) * WIDTH).toInt()
                    if (xb < xa) {
                        val tmp = xa
                        xa = xb
                        xb = tmp
                    }
                    if (xa < 0) xa = 0
                    if (xb >= WIDTH) xb = WIDTH - 1
                    for (x in xa..xb) {
                        val noise = hash(x, y)
                        out[y * WIDTH + x] = land(lat, -180f + x * 360f / WIDTH, noise)
                    }
                    s += 2
                }
            }
        }
        return out
    }
}
