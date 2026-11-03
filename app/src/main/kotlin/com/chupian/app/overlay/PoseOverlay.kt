package com.chupian.app.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.chupian.app.pose.PoseFrame
import com.chupian.app.pose.PoseImageAnalyzer
import kotlin.math.max

/**
 * 骨架叠加层。
 *
 * 坐标映射：PreviewView 使用 FILL_CENTER（等比放大裁切填满），
 * 归一化坐标 (nx, ny) 须按"正立帧宽高比 vs 画布宽高比"做 center-crop 变换：
 *   scale = max(canvasW / (aspect * canvasH), canvasH * aspect / canvasW) 的等价实现见下。
 *
 * TODO(F4)：骨架校验提示（"下巴收一点"）在此层之上叠文字气泡；
 * TODO(F3)：构图框线/站位圈随推荐拍法切换，与本层并列绘制。
 */
@Composable
fun PoseOverlay(
    frame: PoseFrame?,
    modifier: Modifier = Modifier,
    strokeColor: Color = Color(0xFFFFC107),
    dotColor: Color = Color(0xFF03A9F4),
) {
    Canvas(modifier = modifier) {
        val f = frame ?: return@Canvas
        if (f.landmarks.isEmpty()) return@Canvas

        val canvasW = size.width
        val canvasH = size.height
        val frameW: Float
        val frameH: Float
        if (f.frameAspect >= canvasW / canvasH) {
            // 帧比画布更宽 -> 等比缩放到宽满，竖向裁切
            frameW = canvasW
            frameH = canvasW / f.frameAspect
        } else {
            frameH = canvasH
            frameW = canvasH * f.frameAspect
        }
        val offsetX = (canvasW - frameW) / 2f
        val offsetY = (canvasH - frameH) / 2f

        fun map(nx: Float, ny: Float): Offset =
            Offset(offsetX + nx * frameW, offsetY + ny * frameH)

        // 连线
        for ((a, b) in PoseImageAnalyzer.SKELETON_CONNECTIONS) {
            val la = f.landmarks[a] ?: continue
            val lb = f.landmarks[b] ?: continue
            drawLine(
                color = strokeColor,
                start = map(la.first, la.second),
                end = map(lb.first, lb.second),
                strokeWidth = 6f,
                cap = StrokeCap.Round,
            )
        }
        // 关节点
        for ((_, p) in f.landmarks) {
            drawCircle(
                color = dotColor,
                radius = 10f,
                center = map(p.first, p.second),
            )
        }
    }
}
