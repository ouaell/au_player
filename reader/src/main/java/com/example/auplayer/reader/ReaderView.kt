package com.example.auplayer.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Own viewers (MS-037 fallback path, pre-approved in §7.1: Mihon's viewers
 * are View-system; a Compose rewrite is cheaper and fully ours).
 * Viewer interaction tests run on T2; slicing/budget math is JVM-tested
 * (Tiling, Memory).
 */
@Composable
fun PagedReader(
    pageCount: Int,
    current: Int,
    onPageChange: (Int) -> Unit,
    content: @Composable (Int) -> Unit
) {
    val state = rememberPagerState(initialPage = current, pageCount = { pageCount })
    LaunchedEffect(state) {
        snapshotFlow { state.currentPage }.collect { onPageChange(it) }
    }
    HorizontalPager(state = state) { index ->
        content(index)
    }
}

@Composable
fun WebtoonReader(pageCount: Int, content: @Composable (Int) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(pageCount) { index ->
            Box(modifier = Modifier.fillMaxWidth()) {
                content(index)
            }
        }
    }
}

/** Pinch + double-tap zoom layer (MS-037). */
@Composable
fun ZoomablePage(content: @Composable () -> Unit) {
    var scale by remember { mutableFloatStateOf(MIN_SCALE) }
    val transform = rememberTransformableState { zoom, _, _ ->
        scale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .transformable(transform)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > MIN_SCALE) MIN_SCALE else DOUBLE_TAP_SCALE
                    }
                )
            }
            .graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        content()
    }
}

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 4f
private const val DOUBLE_TAP_SCALE = 2f
