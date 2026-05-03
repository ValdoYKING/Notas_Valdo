package com.valdo.notasinteligentesvaldo.cropper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.parcelable

class AppCropImageActivity :
    ComponentActivity(),
    CropImageView.OnSetImageUriCompleteListener,
    CropImageView.OnCropImageCompleteListener {

    private var cropImageUri: Uri? = null
    private lateinit var cropImageOptions: CropImageOptions
    private var cropImageView: CropImageView? = null
    private var imageLoaded = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = intent.getBundleExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE)
        cropImageUri = bundle?.parcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE)
        cropImageOptions = bundle?.parcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS) ?: CropImageOptions()

        if (cropImageUri == null || cropImageUri == Uri.EMPTY) {
            setResultCancel()
            return
        }

        setContent {
            CropperScreen()
        }

        onBackPressedDispatcher.addCallback {
            setResultCancel()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun CropperScreen() {
        val colorScheme = MaterialTheme.colorScheme
        val toolbarColor = cropImageOptions.toolbarColor?.let { Color(it) } ?: colorScheme.surface
        val toolbarContent = cropImageOptions.toolbarTitleColor?.let { Color(it) } ?: colorScheme.onSurface
        val navIconColor = cropImageOptions.toolbarBackButtonColor?.let { Color(it) } ?: toolbarContent
        val actionColor = if (cropImageOptions.activityMenuIconColor != 0) {
            Color(cropImageOptions.activityMenuIconColor)
        } else {
            toolbarContent
        }
        val backgroundColor = if (cropImageOptions.activityBackgroundColor != 0) {
            Color(cropImageOptions.activityBackgroundColor)
        } else {
            colorScheme.background
        }
        val titleText = cropImageOptions.activityTitle.ifEmpty { "" }.toString()
        val confirmText = (cropImageOptions.cropMenuCropButtonTitle ?: "Listo").toString()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(titleText, color = toolbarContent) },
                    navigationIcon = {
                        IconButton(onClick = { setResultCancel() }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = navIconColor)
                        }
                    },
                    actions = {
                        IconButton(onClick = { cropImage() }) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = confirmText, tint = actionColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = toolbarColor)
                )
            },
            containerColor = backgroundColor
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(backgroundColor)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        CropImageView(context).also { view ->
                            cropImageView = view
                            view.setOnSetImageUriCompleteListener(this@AppCropImageActivity)
                            view.setOnCropImageCompleteListener(this@AppCropImageActivity)
                            view.setImageCropOptions(cropImageOptions)
                            view.setBackgroundColor(backgroundColor.toArgb())
                        }
                    }
                )
            }
        }

        LaunchedEffect(cropImageUri, cropImageView) {
            if (!imageLoaded) {
                cropImageView?.setImageUriAsync(cropImageUri)
                imageLoaded = true
            }
        }
    }

    override fun onSetImageUriComplete(view: CropImageView, uri: Uri, error: Exception?) {
        if (error == null) {
            cropImageOptions.initialCropWindowRectangle?.let { rect ->
                cropImageView?.cropRect = rect
            }

            if (cropImageOptions.initialRotation > 0) {
                cropImageView?.rotatedDegrees = cropImageOptions.initialRotation
            }

            if (cropImageOptions.skipEditing) {
                cropImage()
            }
        } else {
            finishWithResult(null, error, 1)
        }
    }

    override fun onCropImageComplete(view: CropImageView, result: CropImageView.CropResult) {
        finishWithResult(result.uriContent, result.error, result.sampleSize)
    }

    private fun cropImage() {
        if (cropImageOptions.noOutputImage) {
            finishWithResult(null, null, 1)
        } else {
            cropImageView?.croppedImageAsync(
                saveCompressFormat = cropImageOptions.outputCompressFormat,
                saveCompressQuality = cropImageOptions.outputCompressQuality,
                reqWidth = cropImageOptions.outputRequestWidth,
                reqHeight = cropImageOptions.outputRequestHeight,
                options = cropImageOptions.outputRequestSizeOptions,
                customOutputUri = cropImageOptions.customOutputUri,
            )
        }
    }

    private fun finishWithResult(uri: Uri?, error: Exception?, sampleSize: Int) {
        val resultCode = error?.let { CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE } ?: Activity.RESULT_OK
        setResult(resultCode, getResultIntent(uri, error, sampleSize))
        finish()
    }

    private fun setResultCancel() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun getResultIntent(uri: Uri?, error: Exception?, sampleSize: Int): Intent {
        val result = CropImage.ActivityResult(
            originalUri = cropImageView?.imageUri,
            uriContent = uri,
            error = error,
            cropPoints = cropImageView?.cropPoints,
            cropRect = cropImageView?.cropRect,
            rotation = cropImageView?.rotatedDegrees ?: 0,
            wholeImageRect = cropImageView?.wholeImageRect,
            sampleSize = sampleSize,
        )
        return Intent().apply {
            putExtra(CropImage.CROP_IMAGE_EXTRA_RESULT, result)
        }
    }
}

