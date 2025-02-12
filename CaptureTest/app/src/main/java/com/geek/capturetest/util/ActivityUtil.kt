package com.geek.capturetest.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.geek.capturetest.CameraActivity

object ActivityUtil {
    fun startCameraActivity(context: Context) {
        Intent(context, CameraActivity::class.java).run {
            context.startActivity(this)
        }
    }
}