package com.example.canvas

import androidx.compose.ui.geometry.Offset
import com.example.model.CircuitComponent
import com.example.model.CircuitComponent.Companion.COMPONENT_HEIGHT
import com.example.model.CircuitComponent.Companion.COMPONENT_WIDTH
import com.example.model.WireConnection
import com.example.model.WirePoint
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * مسیریابی خودکار اورتوگونال سیم‌ها با الگوریتم A* روی شبکه گرید
 * - اجتناب از عبور روی جعبه قطعات (Bounding Box)
 * - جریمه خم شدن سیم (Turn Penalty) برای کاهش شکستگی و تولید خطوط تمیز
 * - جریمه تقاطع با سایر سیم‌ها (Crossing Penalty)
 * - بازگشت ایمن به مسیر ساده منهتن در صورت مسدود بودن مسیر یا مدار خیلی بزرگ
 */
object WireRouter {

    private const val GRID_SIZE = 20f
    private const val TURN_PENALTY = 15f
    private const val CROSSING_PENALTY = 25f
    private const val MAX_EXPANDED_NODES = 1200

    private data class GridNode(
        val gx: Int,
        val gy: Int,
        val dirX: Int, // جهت ورود به این گره (-1, 0, 1)
        val dirY: Int,
        val gCost: Float,
        val hCost: Float,
        val parent: GridNode?
    ) : Comparable<GridNode> {
        val fCost: Float get() = gCost + hCost
        override fun compareTo(other: GridNode): Int = fCost.compareTo(other.fCost)
    }

    /**
     * محاسبه مسیر بهینه بین دو نقطه
     */
    fun routeWire(
        start: Offset,
        end: Offset,
        fromCompId: String,
        toCompId: String,
        components: List<CircuitComponent>,
        existingWires: List<WireConnection> = emptyList(),
        wireToIgnoreId: String? = null
    ): List<WirePoint> {
        // اگر مدار بسیار بزرگ باشد یا فاصله خیلی زیاد، مستقیماً از مسیر منهتن ساده استفاده می‌کنیم
        if (components.size > 60 || (abs(start.x - end.x) > 3000f || abs(start.y - end.y) > 3000f)) {
            return generateManhattanFallback(start, end)
        }

        val startGx = (start.x / GRID_SIZE).roundToInt()
        val startGy = (start.y / GRID_SIZE).roundToInt()
        val targetGx = (end.x / GRID_SIZE).roundToInt()
        val targetGy = (end.y / GRID_SIZE).roundToInt()

        if (startGx == targetGx && startGy == targetGy) {
            return listOf(WirePoint(start.x, start.y), WirePoint(end.x, end.y))
        }

        // محدوده جستجوی گرید
        val minX = min(startGx, targetGx) - 15
        val maxX = max(startGx, targetGx) + 15
        val minY = min(startGy, targetGy) - 15
        val maxY = max(startGy, targetGy) + 15

        // ساخت نقشه موانع ناشی از کادر قطعات (به جز مبدأ و مقصد در پایانه‌ها)
        val obstacleBoxes = components.mapNotNull { comp ->
            // در نظر گرفتن حاشیه ایمنی
            val left = (comp.x - 4f) / GRID_SIZE
            val right = (comp.x + COMPONENT_WIDTH + 4f) / GRID_SIZE
            val top = (comp.y - 4f) / GRID_SIZE
            val bottom = (comp.y + COMPONENT_HEIGHT + 4f) / GRID_SIZE

            val isStartComp = comp.id == fromCompId
            val isTargetComp = comp.id == toCompId

            ObstacleBox(
                left = left.toInt(),
                right = (right + 0.99f).toInt(),
                top = top.toInt(),
                bottom = (bottom + 0.99f).toInt(),
                allowStart = isStartComp,
                allowTarget = isTargetComp
            )
        }

        fun isBlocked(gx: Int, gy: Int): Boolean {
            // آیا این نقطه روی مانعی قرار دارد؟
            for (box in obstacleBoxes) {
                if (gx in box.left..box.right && gy in box.top..box.bottom) {
                    // اگر در مجاورت بلافاصله پایانه مبدأ یا مقصد باشد، مجاز است
                    if (box.allowStart && abs(gx - startGx) <= 1 && abs(gy - startGy) <= 1) continue
                    if (box.allowTarget && abs(gx - targetGx) <= 1 && abs(gy - targetGy) <= 1) continue
                    return true
                }
            }
            return false
        }

        // نقشه تقاطع سیم‌های قبلی
        val wireOccupiedCells = mutableSetOf<Pair<Int, Int>>()
        for (w in existingWires) {
            if (w.id == wireToIgnoreId) continue
            for (wp in w.waypoints) {
                wireOccupiedCells.add(Pair((wp.x / GRID_SIZE).roundToInt(), (wp.y / GRID_SIZE).roundToInt()))
            }
        }

        val openSet = PriorityQueue<GridNode>()
        // نگاشت کمترین gCost برای هر (gx, gy, dirX, dirY)
        val closedSet = HashMap<Long, Float>()

        fun encodeState(gx: Int, gy: Int, dirX: Int, dirY: Int): Long {
            return (gx.toLong() and 0xFFFFL) or
                    ((gy.toLong() and 0xFFFFL) shl 16) or
                    (((dirX + 2).toLong() and 0xFL) shl 32) or
                    (((dirY + 2).toLong() and 0xFL) shl 36)
        }

        fun heuristic(gx: Int, gy: Int): Float {
            return (abs(gx - targetGx) + abs(gy - targetGy)) * GRID_SIZE
        }

        val startNode = GridNode(
            gx = startGx,
            gy = startGy,
            dirX = 0,
            dirY = 0,
            gCost = 0f,
            hCost = heuristic(startGx, startGy),
            parent = null
        )
        openSet.add(startNode)

        val directions = arrayOf(
            Pair(1, 0),
            Pair(-1, 0),
            Pair(0, 1),
            Pair(0, -1)
        )

        var foundGoalNode: GridNode? = null
        var expandedCount = 0

        while (openSet.isNotEmpty() && expandedCount < MAX_EXPANDED_NODES) {
            val current = openSet.poll() ?: break
            expandedCount++

            if (current.gx == targetGx && current.gy == targetGy) {
                foundGoalNode = current
                break
            }

            val stateKey = encodeState(current.gx, current.gy, current.dirX, current.dirY)
            val bestG = closedSet[stateKey]
            if (bestG != null && bestG <= current.gCost) {
                continue
            }
            closedSet[stateKey] = current.gCost

            for ((dx, dy) in directions) {
                val nextGx = current.gx + dx
                val nextGy = current.gy + dy

                if (nextGx < minX || nextGx > maxX || nextGy < minY || nextGy > maxY) continue
                if (isBlocked(nextGx, nextGy)) continue

                // محاسبه جریمه‌ها
                var moveCost = GRID_SIZE
                // جریمه خمیدگی اگر جهت تغییر کند
                if (current.dirX != 0 || current.dirY != 0) {
                    if (current.dirX != dx || current.dirY != dy) {
                        moveCost += TURN_PENALTY
                    }
                }
                // جریمه تقاطع با سیم‌های دیگر
                if (wireOccupiedCells.contains(Pair(nextGx, nextGy))) {
                    moveCost += CROSSING_PENALTY
                }

                val newGCost = current.gCost + moveCost
                val nextStateKey = encodeState(nextGx, nextGy, dx, dy)
                val prevNextG = closedSet[nextStateKey]

                if (prevNextG == null || newGCost < prevNextG) {
                    val neighbor = GridNode(
                        gx = nextGx,
                        gy = nextGy,
                        dirX = dx,
                        dirY = dy,
                        gCost = newGCost,
                        hCost = heuristic(nextGx, nextGy),
                        parent = current
                    )
                    openSet.add(neighbor)
                }
            }
        }

        return if (foundGoalNode != null) {
            reconstructAndSimplifyPath(start, end, foundGoalNode)
        } else {
            // در صورت عدم دستیابی به هدف (مانع بودن مسیر)، فال‌بک تمیز منهتن
            generateManhattanFallback(start, end)
        }
    }

    private data class ObstacleBox(
        val left: Int,
        val right: Int,
        val top: Int,
        val bottom: Int,
        val allowStart: Boolean,
        val allowTarget: Boolean
    )

    /**
     * بازسازی مسیر و حذف نقاط اضافی روی خطوط مستقیم (Collinear Compression)
     */
    private fun reconstructAndSimplifyPath(
        realStart: Offset,
        realEnd: Offset,
        goalNode: GridNode
    ): List<WirePoint> {
        val rawPoints = mutableListOf<WirePoint>()
        var curr: GridNode? = goalNode
        while (curr != null) {
            rawPoints.add(WirePoint(curr.gx * GRID_SIZE, curr.gy * GRID_SIZE))
            curr = curr.parent
        }
        rawPoints.reverse()

        if (rawPoints.isEmpty()) {
            return generateManhattanFallback(realStart, realEnd)
        }

        // جایگزینی ابتدا و انتها با مختصات دقیق پایانه‌ها
        val withExactEnds = mutableListOf<WirePoint>()
        withExactEnds.add(WirePoint(realStart.x, realStart.y))
        if (rawPoints.size > 2) {
            withExactEnds.addAll(rawPoints.subList(1, rawPoints.size - 1))
        }
        withExactEnds.add(WirePoint(realEnd.x, realEnd.y))

        // فشرده‌سازی خطوط هم‌جهت (حذف نقاط بین دو سر خط راست)
        val simplified = mutableListOf<WirePoint>()
        simplified.add(withExactEnds.first())

        for (i in 1 until withExactEnds.size - 1) {
            val pPrev = simplified.last()
            val pCurr = withExactEnds[i]
            val pNext = withExactEnds[i + 1]

            val isCollinearX = abs(pPrev.x - pCurr.x) < 0.1f && abs(pCurr.x - pNext.x) < 0.1f
            val isCollinearY = abs(pPrev.y - pCurr.y) < 0.1f && abs(pCurr.y - pNext.y) < 0.1f

            if (!isCollinearX && !isCollinearY) {
                simplified.add(pCurr)
            }
        }
        simplified.add(withExactEnds.last())

        return simplified
    }

    /**
     * مسیر پیش‌فرض اورتوگونال ۳ تکه‌ای منهتن
     */
    fun generateManhattanFallback(start: Offset, end: Offset): List<WirePoint> {
        val midX = (start.x + end.x) / 2f
        return listOf(
            WirePoint(start.x, start.y),
            WirePoint(midX, start.y),
            WirePoint(midX, end.y),
            WirePoint(end.x, end.y)
        )
    }
}
