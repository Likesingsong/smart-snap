package com.example.smartsnap.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartsnap.data.repository.ObjectRecognitionRepository
import com.example.smartsnap.ui.state.RecognitionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 相机界面 ViewModel
 *
 * 管理识别状态机：Idle → Loading → Success / Failure
 * UI 通过 collect uiState (StateFlow) 驱动界面更新
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val repository: ObjectRecognitionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecognitionUiState>(
        RecognitionUiState.Idle
    )
    val uiState: StateFlow<RecognitionUiState> = _uiState.asStateFlow()

    /**
     * 拍照后触发识别
     * @param bitmap 相机返回的原始照片（本方法不负责 recycle，由 Repository 处理）
     */
    fun onPhotoTaken(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = RecognitionUiState.Loading
            val result = withContext(Dispatchers.Default) {
                repository.recognizeObjects(bitmap)
            }
            _uiState.value = result
        }
    }

    fun resetState() {
        _uiState.value = RecognitionUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        repository.release()
    }
}