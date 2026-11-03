package com.chupian.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.chupian.app.R
import com.chupian.app.overlay.PoseOverlay
import com.chupian.app.pose.PoseFrame
import com.chupian.app.pose.PoseImageAnalyzer
import kotlinx.coroutines.guava.await

/**
 * M2 骨架阶段主界面：
 * CameraX 取景 + ML Kit 骨架实时叠加 + 功能占位（F2 点位匹配 / F3 场景推荐待接入）。
 */
@Composable
fun CameraScreen() {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            CameraWithPoseOverlay()
        } else {
            PermissionDeniedPane { permissionLauncher.launch(Manifest.permission.CAMERA) }
        }
    }
}

@Composable
private fun CameraWithPoseOverlay() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var poseFrame by remember { mutableStateOf<PoseFrame?>(null) }

    LaunchedEffect(Unit) {
        val provider = ProcessCameraProvider.getInstance(context).await()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analyzer ->
                analyzer.setAnalyzer(
                    ContextCompat.getMainExecutor(context),
                    PoseImageAnalyzer { poseFrame = it },
                )
            }

        provider.unbindAll()
        provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analysis,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        PoseOverlay(frame = poseFrame, modifier = Modifier.fillMaxSize())
        StatusChips(hasPose = poseFrame?.landmarks?.isNotEmpty() == true)
        BottomBar()
    }
}

@Composable
private fun StatusChips(hasPose: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusChip(
            text = if (hasPose) stringResource(R.string.chip_pose_tracking)
            else stringResource(R.string.chip_pose_no_person),
            color = if (hasPose) Color(0xCC00C853) else Color(0x889E9E9E),
        )
        StatusChip(text = stringResource(R.string.chip_f2_pending), color = Color(0x66757575))
        StatusChip(text = stringResource(R.string.chip_f3_pending), color = Color(0x66757575))
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(color = color, shape = CircleShape) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun BottomBar() {
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(Color(0x99000000))
            .height(120.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Button(
            onClick = { /* TODO(F3)：调云端 VLM 场景分析 + 知识卡 + 偏好画像，输出拍法 */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFC107),
                contentColor = Color.Black,
            ),
        ) {
            Text(
                text = stringResource(R.string.btn_how_to_shoot),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun PermissionDeniedPane(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.camera_permission_rationale),
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        Button(onClick = onRetry) {
            Text(stringResource(R.string.camera_permission_denied))
        }
    }
}
