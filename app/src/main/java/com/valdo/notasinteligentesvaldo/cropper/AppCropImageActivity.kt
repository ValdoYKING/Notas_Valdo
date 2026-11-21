package com.valdo.notasinteligentesvaldo.cropper

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.canhub.cropper.parcelable

/**
 * Actividad del Editor y Recortador de Fotografía.
 * Proporciona una interfaz oscura, moderna y profesional con controles interactivos
 * de rotación, volteo, selección dinámica de relación de aspecto (1:1, 4:3, 16:9, Círculo, Libre)
 * y restablecimiento de imagen.
 */
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
            ModernCropperTheme {
                CropperScreen()
            }
        }

        onBackPressedDispatcher.addCallback {
            setResultCancel()
        }
    }

    @Composable
    private fun ModernCropperTheme(content: @Composable () -> Unit) {
        val darkColorScheme = darkColorScheme(
            background = Color(0xFF0F0F12),
            surface = Color(0xFF1A1A1E),
            surfaceVariant = Color(0xFF2A2A30),
            primary = Color(0xFF6366F1),
            onPrimary = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFC7C7D0)
        )
        MaterialTheme(colorScheme = darkColorScheme, content = content)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun CropperScreen() {
        var selectedRatio by remember { mutableStateOf("1:1") }
        var isCircleCrop by remember { mutableStateOf(false) }

        val darkBg = Color(0xFF0F0F12)
        val containerBg = Color(0xFF18181C)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Editor de Fotografía",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { setResultCancel() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancelar",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        Button(
                            onClick = { cropImage() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Listo", style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = containerBg)
                )
            },
            containerColor = darkBg
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(darkBg)
            ) {
                // Lienzo interactivo para recortar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(darkBg)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            CropImageView(context).also { view ->
                                cropImageView = view
                                view.setOnSetImageUriCompleteListener(this@AppCropImageActivity)
                                view.setOnCropImageCompleteListener(this@AppCropImageActivity)
                                view.setImageCropOptions(cropImageOptions)
                                view.setBackgroundColor(darkBg.toArgb())
                                view.guidelines = CropImageView.Guidelines.ON_TOUCH
                                view.isShowCropOverlay = true
                            }
                        }
                    )
                }

                // Panel de herramientas de edición en la parte inferior
                Surface(
                    color = containerBg,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        // Fila de herramientas (Rotar, Voltear, Restablecer)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rotar a la izquierda
                            IconButton(onClick = { cropImageView?.rotateImage(-90) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                                    contentDescription = "Rotar Izquierda",
                                    tint = Color.White
                                )
                            }

                            // Rotar a la derecha
                            IconButton(onClick = { cropImageView?.rotateImage(90) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                    contentDescription = "Rotar Derecha",
                                    tint = Color.White
                                )
                            }

                            // Voltear horizontalmente
                            IconButton(onClick = { cropImageView?.flipImageHorizontally() }) {
                                Icon(
                                    imageVector = Icons.Default.Flip,
                                    contentDescription = "Voltear Horizontal",
                                    tint = Color.White
                                )
                            }

                            // Restablecer recorte
                            IconButton(onClick = {
                                cropImageView?.resetCropRect()
                                selectedRatio = "Libre"
                                isCircleCrop = false
                                cropImageView?.clearAspectRatio()
                                cropImageView?.cropShape = CropImageView.CropShape.RECTANGLE
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Restablecer",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Selector de Relaciones de Aspecto (Chips)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val ratioOptions = listOf(
                                "1:1" to Pair(1, 1),
                                "Círculo" to Pair(1, 1),
                                "4:3" to Pair(4, 3),
                                "16:9" to Pair(16, 9),
                                "3:2" to Pair(3, 2),
                                "Libre" to null
                            )

                            ratioOptions.forEach { (label, ratio) ->
                                val isSelected = selectedRatio == label
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedRatio = label
                                        if (label == "Círculo") {
                                            isCircleCrop = true
                                            cropImageView?.setAspectRatio(1, 1)
                                            cropImageView?.cropShape = CropImageView.CropShape.OVAL
                                        } else {
                                            isCircleCrop = false
                                            cropImageView?.cropShape = CropImageView.CropShape.RECTANGLE
                                            if (ratio != null) {
                                                cropImageView?.setAspectRatio(ratio.first, ratio.second)
                                            } else {
                                                cropImageView?.clearAspectRatio()
                                            }
                                        }
                                    },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color(0xFF25252A),
                                        labelColor = Color(0xFFD0D0D8),
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }
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
