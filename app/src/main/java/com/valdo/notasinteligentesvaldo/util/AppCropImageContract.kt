package com.valdo.notasinteligentesvaldo.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import com.canhub.cropper.CropImage
import com.valdo.notasinteligentesvaldo.cropper.AppCropImageActivity
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.parcelable

// Copia funcional del contract de la libreria, sin la deprecacion.
data class AppCropImageContractOptions(
    val uri: Uri?,
    val cropImageOptions: CropImageOptions
)

class AppCropImageContract : ActivityResultContract<AppCropImageContractOptions, CropImageView.CropResult>() {
    override fun createIntent(context: Context, input: AppCropImageContractOptions): Intent {
        return Intent(context, AppCropImageActivity::class.java).apply {
            putExtra(
                CropImage.CROP_IMAGE_EXTRA_BUNDLE,
                Bundle(2).apply {
                    putParcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE, input.uri)
                    putParcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS, input.cropImageOptions)
                }
            )
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): CropImageView.CropResult {
        val result = intent?.parcelable<CropImage.ActivityResult>(CropImage.CROP_IMAGE_EXTRA_RESULT)
        return if (result == null || resultCode == Activity.RESULT_CANCELED) {
            CropImage.CancelledResult
        } else {
            result
        }
    }
}
