package app.pixo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

/**
 * 底栏可见性：由内容滚动方向驱动。
 *
 * - 内容向上滚（手指上滑）→ 收起
 * - 内容向下滚（手指下滑）→ 弹出
 * - 回到顶部 → 一定弹出
 *
 * 用单例而不是 CompositionLocal：滚动事件来自各标签页的列表，
 * 渲染在 App 层，中间隔着 NavHost，用 CompositionLocal 需要多层包裹；
 * 这里状态极小（一个布尔），与 [SettingsRevision] / [FeedCache] 的做法一致。
 */
internal object BottomBarVisibility {
    var visible by mutableStateOf(true)
        private set

    fun show() {
        visible = true
    }

    fun hide() {
        visible = false
    }
}

/** 触发收起的滚动量（像素）；太小会被惯性抖动的滚动反复触发。 */
private const val HIDE_THRESHOLD_PX = 32

/**
 * 跟踪列表滚动方向，驱动 [BottomBarVisibility]。
 *
 * 由列表页在拿到 [androidx.compose.foundation.lazy.LazyListState] /
 * [androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState] 后调用：
 *
 * ```kotlin
 * TrackBottomBarScroll { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
 * ```
 */
@Composable
internal fun TrackBottomBarScroll(position: () -> Pair<Int, Int>) {
    LaunchedEffect(Unit) {
        var last = position()
        snapshotFlow(position).collect { now ->
            val dy = (now.first - last.first) * 100_000 + (now.second - last.second)
            last = now
            when {
                // 回到顶部：一定显示
                now.first == 0 && now.second == 0 -> BottomBarVisibility.show()
                dy > HIDE_THRESHOLD_PX -> BottomBarVisibility.hide()
                dy < -HIDE_THRESHOLD_PX -> BottomBarVisibility.show()
            }
        }
    }
}
