package com.example.smartsnap.ui.screen

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartsnap.camera.CameraManager
import com.example.smartsnap.domain.model.RecognizedItem
import com.example.smartsnap.ui.state.RecognitionUiState
import com.example.smartsnap.ui.viewmodel.CameraViewModel
import com.example.smartsnap.util.PermissionUtils
import kotlinx.coroutines.launch

/**
 * 主界面：全屏相机预览 + 底部结果面板 + 拍照按钮
 *
 * 布局（从上到下）：
 * ┌─────────────────────┐
 * │    相机预览区域       │  AndroidView 嵌入 CameraX PreviewView
 * │                     │
 * │  ┌───────────────┐  │
 * │  │ 识别结果面板    │  │  仅 Success/Failure/Loading 时显示
 * │  │ 1. 手机  92%  │  │
 * │  └───────────────┘  │
 * │        [📷]         │  圆形拍照按钮，Loading 时禁用
 * └─────────────────────┘
 */
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    val cameraManager = remember { CameraManager(context, lifecycleOwner) }
    val previewView = remember { cameraManager.getPreviewView() }

    var hasCameraPermission by remember {
        mutableStateOf(PermissionUtils.hasCameraPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            cameraManager.startCamera(previewView)
        } else {
            Toast.makeText(
                context,
                "相机权限被拒绝，请在设置中开启",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // 当权限状态变化时重新绑定相机，Composable 销毁时释放资源
    DisposableEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            cameraManager.startCamera(previewView)
        }
        onDispose {
            cameraManager.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!hasCameraPermission) {
            PermissionRequestScreen(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        } else {
            // 全屏相机预览
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // 底部浮层：识别结果 + 拍照按钮
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                when (val state = uiState) {
                    is RecognitionUiState.Idle -> { /* 空闲状态不显示任何面板 */ }
                    is RecognitionUiState.Loading -> {
                        LoadingIndicator()
                    }
                    is RecognitionUiState.Success -> {
                        ResultPanel(items = state.items)
                    }
                    is RecognitionUiState.Failure -> {
                        FailurePanel(onRetry = { viewModel.resetState() })
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                CaptureButton(
                    enabled = uiState !is RecognitionUiState.Loading,
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val bitmap = cameraManager.takePhoto()
                                viewModel.onPhotoTaken(bitmap)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "拍照失败，请重试",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

/** 权限未授予时的引导界面 */
@Composable
private fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Camera,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "需要相机权限",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "SmartSnap 需要使用相机来拍照识别物品，请授予相机权限以继续使用",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRequestPermission) {
            Text("授予权限")
        }
    }
}

/** 识别中加载指示器 */
@Composable
private fun LoadingIndicator() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "正在识别...",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 识别结果面板
 * 按置信度降序显示 Top 2-3 个物品名称及其置信度百分比
 */
@Composable
private fun ResultPanel(items: List<RecognizedItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
            .padding(16.dp)
    ) {
        Text(
            text = "识别结果",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 序号
                Text(
                    text = "${index + 1}.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp)
                )

                // 物品名称
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )

                // 置信度（≥70% 高亮为主色）
                Text(
                    text = "${(item.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.confidence >= 0.7f) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** 识别失败提示面板，提供重试按钮 */
@Composable
private fun FailurePanel(onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "识别失败",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(modifier = Modifier.width(12.dp))
        IconButton(
            onClick = onRetry,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "重试",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** 圆形拍照按钮，Loading 状态时禁用 */
@Composable
private fun CaptureButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = Modifier.size(72.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
    ) {
        Icon(
            imageVector = Icons.Default.Camera,
            contentDescription = "拍照",
            modifier = Modifier.size(32.dp)
        )
    }
}