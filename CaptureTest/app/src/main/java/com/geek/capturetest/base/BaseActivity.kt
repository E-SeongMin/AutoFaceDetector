package com.geek.capturetest.base

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

open class BaseActivity : ComponentActivity() {

    private val cameraPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        PermissionUtil.handleCameraPermissionResult(isGranted,
            onPermissionGranted = {

            },
            onPermissionDenied = {
                finish()
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!PermissionUtil.isCameraPermissionGranted(this)) {
            cameraPermissionRequest.launch(Manifest.permission.CAMERA)
        }
    }
}