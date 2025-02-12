package com.geek.autofacecapture.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.geek.autofacecapture.databinding.AutoFaceCaptureViewBinding
import com.geek.autofacecapture.listener.ResultCallbackListener
import com.geek.autofacecapture.util.CustomLog
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.os.CountDownTimer
import com.geek.autofacecapture.util.DataUtil

class AutoFaceCaptureView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var resultCallbackListener: ResultCallbackListener? = null
    private val binding: AutoFaceCaptureViewBinding by lazy {
        AutoFaceCaptureViewBinding.inflate(LayoutInflater.from(context), this, true)
    }

    private lateinit var cameraProvider: ProcessCameraProvider
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var faceDetector: FaceDetector
    private var countdownTimer: CountDownTimer? = null

    private var currentImageProxy: ImageProxy? = null

    init {
        CustomLog.d("AutoFaceCaptureView init")
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
        faceDetector = FaceDetection.getClient(options)
    }

    fun addCaptureListener(listener: ResultCallbackListener) {
        this.resultCallbackListener = listener
    }

    fun removeCaptureListener() {
        this.resultCallbackListener = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startCamera()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopCamera()
        cancelCountdownTimer()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                surfaceProvider = binding.surfaceView.surfaceProvider
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImage(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            if (context is LifecycleOwner) {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    context as LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            }

        }, ContextCompat.getMainExecutor(context))
    }

    private fun stopCamera() {
        cameraProvider.unbindAll()
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                val cameraWidth = imageProxy.width
                val cameraHeight = imageProxy.height

                val leftPadding = 30f
                val rightPadding = 30f
                val radius = (cameraWidth - leftPadding - rightPadding) / 2f
                val centerX = cameraWidth / 2f
                val centerY = cameraHeight / 2f

                val faceInsideCircle = faces.any { face ->
                    val faceCenterX = (face.boundin gBox.left + face.boundingBox.right) / 2
                    val faceCenterY = (face.boundingBox.top + face.boundingBox.bottom) / 2

                    val distance = Math.sqrt(
                        Math.pow((faceCenterX - centerX).toDouble(), 2.0) +
                                Math.pow((faceCenterY - centerY).toDouble(), 2.0)
                    )
                    distance <= radius
                }

                if (faceInsideCircle) {
                    currentImageProxy = imageProxy
                    startCountdownTimer()
                } else {
                    cancelCountdownTimer()
                    binding.cameraOverlay.resetCurrentState()
                    imageProxy.close()
                }
            }
            .addOnFailureListener {
                cancelCountdownTimer()
                binding.cameraOverlay.resetCurrentState()
                imageProxy.close()
            }
    }

    private fun startCountdownTimer() {
        CustomLog.d("AutoFaceCaptureView startCountdownTimer")
        cancelCountdownTimer()

        var currentTimer = 3
        countdownTimer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.cameraOverlay.setCurrentState(currentTimer)
                currentTimer--
            }

            override fun onFinish() {
                captureImage()
            }
        }.start()
    }

    private fun cancelCountdownTimer() {
        CustomLog.d("AutoFaceCaptureView cancelCountdownTimer")
        countdownTimer?.cancel()
    }

    private fun captureImage() {
        CustomLog.d("AutoFaceCaptureView captureImage")

        currentImageProxy?.let { imageProxy ->
            val bitmap = DataUtil.yuv420ToBitmap(imageProxy)

            bitmap?.let {
                val cameraWidth = it.width
                val cameraHeight = it.height

                val radius = Math.min(cameraWidth, cameraHeight) / 2f
                val centerX = cameraWidth / 2f
                val centerY = cameraHeight / 2f

                val croppedBitmap = DataUtil.cropToCircle(it, centerX, centerY, radius.toInt())
                val croppedByteArray = DataUtil.bitmapToByteArray(croppedBitmap)

                resultCallbackListener?.onCaptureSuccess(croppedByteArray)
            } ?: resultCallbackListener?.onCaptureFail()

            imageProxy.close()
            currentImageProxy = null
        } ?: resultCallbackListener?.onCaptureFail()
    }
}