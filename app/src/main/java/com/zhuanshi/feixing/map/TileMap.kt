package com.zhuanshi.feixing.map

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sinh

/**
 * 墨卡托相机：中心经纬度 + 浮点 zoom。
 * zoom 3 约等于大陆级，zoom 15 能看清机场跑道。
 */
data class Camera(val lat: Double, val lon: Double, val zoom: Float)

object Mercator {
    fun worldSize(z: Double): Double = 256.0 * 2.0.pow(z)

    fun xOf(lon: Double, z: Double): Double = (lon + 180.0) / 360.0 * worldSize(z)

    fun yOf(lat: Double, z: Double): Double {
        val s = sin(Math.toRadians(lat.coerceIn(-85.0, 85.0)))
        return (0.5 - ln((1.0 + s) / (1.0 - s)) / (4.0 * PI)) * worldSize(z)
    }

    fun lonOf(x: Double, z: Double): Double = x / worldSize(z) * 360.0 - 180.0

    fun latOf(y: Double, z: Double): Double {
        val n = PI - 2.0 * PI * y / worldSize(z)
        return Math.toDegrees(atan(sinh(n)))
    }

    fun metersPerPixel(lat: Double, zoom: Float): Double =
        156543.03392804097 * cos(Math.toRadians(lat.coerceIn(-85.0, 85.0))) / 2.0.pow(zoom.toDouble())
}

/** 当前屏幕的投影，供航线、光点等覆盖层共用。 */
class Projection(
    private val camera: Camera,
    val viewport: Size,
    val tileZoom: Int,
    val scale: Float,
    val gcj: Boolean = false
) {
    private val z: Double = tileZoom.toDouble()
    private val world: Double = Mercator.worldSize(z)
    private val center: DoubleArray =
        if (gcj) Gcj02.wgs2gcj(camera.lat, camera.lon) else doubleArrayOf(camera.lat, camera.lon)
    private val cx: Double = Mercator.xOf(center[1], z)
    private val cy: Double = Mercator.yOf(center[0], z)

    fun toScreen(lat: Double, lon: Double): Offset {
        val pt = if (gcj) Gcj02.wgs2gcj(lat, lon) else doubleArrayOf(lat, lon)
        var dx = Mercator.xOf(pt[1], z) - cx
        if (dx > world / 2.0) dx -= world
        if (dx < -world / 2.0) dx += world
        val dy = Mercator.yOf(pt[0], z) - cy
        return Offset(
            (viewport.width / 2.0 + dx * scale).toFloat(),
            (viewport.height / 2.0 + dy * scale).toFloat()
        )
    }

    fun metersPerPixel(): Double = Mercator.metersPerPixel(camera.lat, camera.zoom)
}

/**
 * 瓦片源。两个都是免 key 的公开服务：
 *  Esri World Imagery —— 真实卫星影像，能看到跑道和停机位
 *  CARTO dark_all —— 暗色路网，贴合夜间主题
 */
class TileSource(
    val id: String,
    val attribution: String,
    val maxZoom: Int,
    val ext: String,
    val label: String,
    val gcj: Boolean = false,
    val dim: Float = 0f,
    val urlFor: (Int, Int, Int) -> String
) {
    companion object {
        val SATELLITE = TileSource(
            id = "esri",
            attribution = "© Esri · Maxar · Earthstar Geographics",
            maxZoom = 17,
            ext = "jpg",
            label = "卫星"
        ) { z, x, y ->
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$y/$x"
        }

        /** 高德矢量路网：中文 POI 最全。坐标系 GCJ-02，用前必须过 Gcj02。 */
        val AMAP_VEC = TileSource(
            id = "amapv",
            attribution = "\u00a9 \u9ad8\u5fb7\u5730\u56fe AutoNavi",
            maxZoom = 18,
            ext = "png",
            label = "\u4e2d\u6587",
            gcj = true
        ) { z, x, y ->
            "https://webrd0" + (1 + (x + y) % 4) +
                ".is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x=" + x + "&y=" + y + "&z=" + z
        }

        /** 高德卫星：真实影像，同样 GCJ-02。 */
        val AMAP_SAT = TileSource(
            id = "amaps",
            attribution = "\u00a9 \u9ad8\u5fb7\u5730\u56fe AutoNavi",
            maxZoom = 18,
            ext = "jpg",
            label = "\u536b\u661f",
            gcj = true
        ) { z, x, y ->
            "https://webst0" + (1 + (x + y) % 4) +
                ".is.autonavi.com/appmaptile?style=6&x=" + x + "&y=" + y + "&z=" + z
        }

        val DARK = TileSource(
            id = "carto",
            attribution = "© CARTO · © OpenStreetMap contributors",
            maxZoom = 18,
            ext = "png",
            label = "暗色"
        ) { z, x, y ->
            "https://basemaps.cartocdn.com/dark_all/$z/$x/$y.png"
        }
    }
}

data class TileId(val z: Int, val x: Int, val y: Int)

/** 当前视口需要的瓦片（已处理经度环绕）。 */
fun visibleTiles(camera: Camera, width: Float, height: Float, maxZoom: Int, gcj: Boolean = false): List<TileId> {
    if (width <= 0f || height <= 0f) return emptyList()
    val tz = floor(camera.zoom.toDouble()).toInt().coerceIn(2, maxZoom)
    val ct = if (gcj) Gcj02.wgs2gcj(camera.lat, camera.lon) else doubleArrayOf(camera.lat, camera.lon)
    val cx = Mercator.xOf(ct[1], tz.toDouble())
    val cy = Mercator.yOf(ct[0], tz.toDouble())
    val left = cx - width / 2.0
    val top = cy - height / 2.0
    val n = 1 shl tz
    val x0 = floor(left / 256.0).toInt()
    val y0 = floor(top / 256.0).toInt()
    val x1 = floor((left + width) / 256.0).toInt()
    val y1 = floor((top + height) / 256.0).toInt()
    val out = ArrayList<TileId>(32)
    var tx = x0
    while (tx <= x1) {
        var ty = y0
        while (ty <= y1) {
            if (ty in 0 until n) out.add(TileId(tz, ((tx % n) + n) % n, ty))
            ty++
        }
        tx++
    }
    return out
}

/** 内存 LRU + 磁盘缓存 + 4 并发下载，全部在 IO 线程。 */
class TileStore(private val source: TileSource, cacheRoot: File) {

    private val root = File(cacheRoot, "tiles-" + source.id).apply { mkdirs() }

    private val mem = object : LruCache<String, ImageBitmap>(26 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Semaphore(4)
    private val inFlight: MutableSet<String> = java.util.Collections.synchronizedSet(HashSet())
    private val dead: MutableSet<String> = java.util.Collections.synchronizedSet(HashSet())
    private val versionFlow = MutableStateFlow(0)
    val version: StateFlow<Int> = versionFlow

    private fun key(z: Int, x: Int, y: Int): String = "$z/$x/$y"

    fun peek(z: Int, x: Int, y: Int): ImageBitmap? = mem.get(key(z, x, y))

    fun ensure(z: Int, x: Int, y: Int) {
        if (z < 2 || z > source.maxZoom) return
        val k = key(z, x, y)
        if (mem.get(k) != null) return
        if (dead.contains(k)) return
        if (!inFlight.add(k)) return
        scope.launch {
            try {
                gate.withPermit {
                    val img = fetch(z, x, y)
                    if (img != null) {
                        mem.put(k, img)
                        versionFlow.update { it + 1 }
                    } else {
                        dead.add(k)
                    }
                }
            } finally {
                inFlight.remove(k)
            }
        }
    }

    private fun fetch(z: Int, x: Int, y: Int): ImageBitmap? {
        val f = File(root, "$z/$x/$y." + source.ext)
        if (f.exists() && f.length() > 0L) {
            val cached = BitmapFactory.decodeFile(f.absolutePath)
            if (cached != null) return cached.asImageBitmap()
            f.delete()
        }
        try {
            val c = (URL(source.urlFor(z, x, y)).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 12000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Accept", "image/*,*/*;q=0.8")
            }
            try {
                if (c.responseCode != 200) return null
                val bytes = c.inputStream.use { it.readBytes() }
                if (bytes.size < 256) return null
                f.parentFile?.mkdirs()
                try { f.writeBytes(bytes) } catch (_: Throwable) { }
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                return bmp?.asImageBitmap()
            } finally {
                try { c.disconnect() } catch (_: Throwable) { }
            }
        } catch (_: Throwable) {
            return null
        }
    }

    val maxZoom: Int get() = source.maxZoom
    val gcj: Boolean get() = source.gcj
    val dim: Float get() = source.dim

    companion object {
        const val UA = "FocusFlight/1.5 (Android; offline-first focus timer)"
    }
}

@Composable
fun rememberTileStore(source: TileSource): TileStore {
    val ctx = LocalContext.current
    return remember(source) { TileStore(source, ctx.cacheDir) }
}

/** 把一张瓦片画到屏幕上；找不到就向上找最多 3 级父瓦片顶着，绝不留空。 */
private fun DrawScope.drawTileLayer(store: TileStore, camera: Camera): Pair<Boolean, Projection> {
    val tz = floor(camera.zoom.toDouble()).toInt().coerceIn(2, store.maxZoom)
    val scale = 2.0.pow((camera.zoom - tz).toDouble()).toFloat()
    val tilePx = (256f * scale).roundToInt()
    val proj = Projection(camera, size, tz, scale, store.gcj)
    val ct = if (store.gcj) Gcj02.wgs2gcj(camera.lat, camera.lon) else doubleArrayOf(camera.lat, camera.lon)
    val cx = Mercator.xOf(ct[1], tz.toDouble())
    val cy = Mercator.yOf(ct[0], tz.toDouble())
    val left = cx - size.width / 2.0
    val top = cy - size.height / 2.0
    val n = 1 shl tz
    val x0 = floor(left / 256.0).toInt()
    val y0 = floor(top / 256.0).toInt()
    val x1 = floor((left + size.width) / 256.0).toInt()
    val y1 = floor((top + size.height) / 256.0).toInt()
    var any = false
    var tx = x0
    while (tx <= x1) {
        var ty = y0
        while (ty <= y1) {
            if (ty >= 0 && ty < n) {
                val wx = ((tx % n) + n) % n
                val dstX = (tx * 256.0 - left).roundToInt()
                val dstY = (ty * 256.0 - top).roundToInt()
                val exact = store.peek(tz, wx, ty)
                if (exact != null) {
                    any = true
                    drawImage(
                        image = exact,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(exact.width, exact.height),
                        dstOffset = IntOffset(dstX, dstY),
                        dstSize = IntSize(tilePx, tilePx),
                        filterQuality = FilterQuality.Medium
                    )
                } else {
                    var level = 1
                    var pz = tz - 1
                    var done = false
                    while (pz >= 2 && level <= 3 && !done) {
                        val per = 1 shl level
                        val pimg = store.peek(pz, wx / per, ty / per)
                        if (pimg != null) {
                            val sub = pimg.width / per
                            if (sub > 0) {
                                any = true
                                drawImage(
                                    image = pimg,
                                    srcOffset = IntOffset((wx % per) * sub, (ty % per) * sub),
                                    srcSize = IntSize(sub, sub),
                                    dstOffset = IntOffset(dstX, dstY),
                                    dstSize = IntSize(tilePx, tilePx),
                                    filterQuality = FilterQuality.Low
                                )
                                done = true
                            }
                        }
                        pz--
                        level++
                    }
                }
            }
            ty++
        }
        tx++
    }
    return any to proj
}

/** 一张瓦片都没有时的兜底经纬网，保证永远不白屏。 */
fun DrawScope.drawGraticule(proj: Projection) {
    var lon = -180.0
    while (lon <= 180.0) {
        val a = proj.toScreen(85.0, lon)
        val b = proj.toScreen(-85.0, lon)
        if (abs(a.x - b.x) < size.width * 2f) drawLine(Color(0x12FFFFFF), a, b, 1f)
        lon += 15.0
    }
    var lat = -75.0
    while (lat <= 75.0) {
        val a = proj.toScreen(lat, -179.9)
        val b = proj.toScreen(lat, 179.9)
        if (abs(a.y - b.y) < size.height * 2f) drawLine(Color(0x12FFFFFF), a, b, 1f)
        lat += 15.0
    }
}

/**
 * 真实瓦片地图。camera 变就自动请求新瓦片；瓦片到货自动重绘。
 * overlay 用来在上面画航线、飞机、比例尺。
 */
@Composable
fun TileMap(
    camera: Camera,
    modifier: Modifier = Modifier,
    source: TileSource = TileSource.SATELLITE,
    overlay: DrawScope.(Projection) -> Unit = {}
) {
    val store = rememberTileStore(source)
    var vw by remember { mutableIntStateOf(0) }
    var vh by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(store) {
        store.version.collect { tick = it }
    }
    LaunchedEffect(camera, vw, vh, source) {
        if (vw > 0 && vh > 0) {
            val list = visibleTiles(camera, vw.toFloat(), vh.toFloat(), source.maxZoom, source.gcj)
            for (t in list) store.ensure(t.z, t.x, t.y)
        }
    }

    val stamp = tick
    Canvas(
        modifier.onSizeChanged {
            if (it.width != vw || it.height != vh) {
                vw = it.width
                vh = it.height
            }
        }
    ) {
        if (stamp < 0) return@Canvas
        drawRect(Color(0xFF070A10))
        val pair = drawTileLayer(store, camera)
        if (!pair.first) drawGraticule(pair.second)
        if (store.dim > 0f) {
            drawRect(Color.Black.copy(alpha = store.dim.coerceIn(0f, 0.95f)))
        }
        overlay(pair.second)
    }
}
