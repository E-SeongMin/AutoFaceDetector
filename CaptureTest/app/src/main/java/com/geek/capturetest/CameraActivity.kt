package com.geek.capturetest

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.geek.autofacecapture.listener.ResultCallbackListener
import com.geek.autofacecapture.view.AutoFaceCaptureView
import com.geek.capturetest.base.BaseActivity

class CameraActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FaceCaptureScreen()
        }
    }

    @Composable
    fun FaceCaptureScreen() {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    AutoFaceCaptureView(context).apply {
                        addCaptureListener(object : ResultCallbackListener {
                            override fun onCaptureSuccess(byteArray: ByteArray) {
                                val resultIntent = Intent()
                                resultIntent.putExtra("capturedImage", byteArray)

                                setResult(RESULT_OK, resultIntent)
                                finish()
                            }

                            override fun onCaptureFail() {
                                Toast.makeText(context, "Capture Failed", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}