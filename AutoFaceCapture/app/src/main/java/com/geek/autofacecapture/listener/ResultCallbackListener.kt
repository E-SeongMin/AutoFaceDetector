package com.geek.autofacecapture.listener

interface ResultCallbackListener {
    fun onCaptureSuccess(imageData: ByteArray)
    fun onCaptureFail()
}