package com.chupian.app.pose

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

/**
 * 单帧骨架检测结果（归一化坐标，竖屏参考系）。
 *
 * @param landmarks landmarkType -> (nx, ny)，nx/ny ∈ [0,1]，相对于"旋转为正立"的相机帧
 * @param frameAspect 旋转为正立后的帧宽高比（w/h），叠加层据此做 center-crop 映射
 */
data class PoseFrame(
    val landmarks: Map<Int, Pair<Float, Float>>,
    val frameAspect: Float,
)

/**
 * CameraX ImageAnalysis 分析器：帧 -> ML Kit Pose Detection (STREAM_MODE) -> 归一化地标。
 *
 * 后续接入 F4 骨架校验提示时，在此层之上加 PoseRuleChecker（"下巴收一点"等），不动本类。
 */
class PoseImageAnalyzer(
    private val onPose: (PoseFrame?) -> Unit,
) : ImageAnalysis.Analyzer {

    private val detector by lazy {
        PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build()
        )
    }

    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
            return
        }
        val rotation = image.imageInfo.rotationDegrees
        val input = InputImage.fromMediaImage(mediaImage, rotation)

        // ML Kit 返回的坐标已换算到"旋转为正立"的参考系：
        // 90°/270° 旋转时，正立帧的宽高 = 原始帧的高宽互换
        val isSideways = rotation == 90 || rotation == 270
        val uprightWidth = if (isSideways) mediaImage.height else mediaImage.width
        val uprightHeight = if (isSideways) mediaImage.width else mediaImage.height

        detector.process(input)
            .addOnSuccessListener { pose -> onPose(pose.toPoseFrame(uprightWidth.toFloat(), uprightHeight.toFloat())) }
            .addOnFailureListener { onPose(null) }
            .addOnCompleteListener { image.close() }
    }

    private fun Pose.toPoseFrame(w: Float, h: Float): PoseFrame {
        val map = allPoseLandmarks.associate { lm ->
            lm.poseLandmarkType to Pair(
                (lm.position.x / w).coerceIn(0f, 1f),
                (lm.position.y / h).coerceIn(0f, 1f),
            )
        }
        return PoseFrame(landmarks = map, frameAspect = w / h)
    }

    companion object {
        // F4 用：关键连线（肩-肩、臂、躯干、腿）
        val SKELETON_CONNECTIONS: List<Pair<Int, Int>> = listOf(
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
        )
    }
}
