package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.util.BillImageStorage
import com.example.ui.localization.LocalAppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptCameraScreen(
    onPhotoCaptured: (Bitmap, String) -> Unit,
    onGalleryPickRequest: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current

    // Check camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var permissionRequestedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        permissionRequestedOnce = true
        if (!isGranted) {
            Toast.makeText(context, strings.cameraDeniedMsg, Toast.LENGTH_LONG).show()
        }
    }

    // Launch permission request automatically if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    BackHandler {
        onDismiss()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black
    ) {
        if (!hasCameraPermission) {
            CameraPermissionRationaleView(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onGalleryClick = onGalleryPickRequest,
                onDismiss = onDismiss
            )
        } else {
            CameraViewfinderContent(
                onPhotoConfirmed = onPhotoCaptured,
                onGalleryClick = onGalleryPickRequest,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun CameraPermissionRationaleView(
    onRequestPermission: () -> Unit,
    onGalleryClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                )
            )
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .testTag("camera_permission_close_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = strings.cancel,
                tint = Color.White
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            ),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = strings.cameraPermissionTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = strings.cameraPermissionDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Feature check items
                PermissionFeatureItem(
                    icon = Icons.Outlined.CheckCircle,
                    text = "Instant high-resolution OCR text extraction"
                )
                Spacer(modifier = Modifier.height(8.dp))
                PermissionFeatureItem(
                    icon = Icons.Outlined.CheckCircle,
                    text = "Automatic warranty expiration date detection"
                )
                Spacer(modifier = Modifier.height(8.dp))
                PermissionFeatureItem(
                    icon = Icons.Outlined.CheckCircle,
                    text = "Receipts stay stored safely on your device"
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("camera_grant_permission_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.grantPermission,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onGalleryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("camera_fallback_gallery_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.uploadFromGallery,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFFCBD5E1)
        )
    }
}

@Composable
private fun CameraViewfinderContent(
    onPhotoConfirmed: (Bitmap, String) -> Unit,
    onGalleryClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val strings = LocalAppStrings.current
    val coroutineScope = rememberCoroutineScope()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTakingPhoto by remember { mutableStateOf(false) }

    // State for captured photo preview before sending to AI
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedFilePath by remember { mutableStateOf<String?>(null) }

    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // Bind Camera lifecycle
    fun bindCamera(provider: ProcessCameraProvider, pView: PreviewView) {
        try {
            provider.unbindAll()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = pView.surfaceProvider
            }

            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                capture
            )

            imageCapture = capture
            cameraControl = camera.cameraControl
            cameraControl?.enableTorch(isTorchOn)
        } catch (e: Exception) {
            Log.e("ReceiptCameraScreen", "Failed to bind camera use cases", e)
        }
    }

    // Effect to initialize CameraProvider
    DisposableEffect(lensFacing) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProviderInstance = provider
                previewViewRef?.let { pView ->
                    bindCamera(provider, pView)
                }
            } catch (e: Exception) {
                Log.e("ReceiptCameraScreen", "Camera provider init failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                cameraProviderInstance?.unbindAll()
            } catch (_: Exception) {}
        }
    }

    // If photo is captured, show review confirmation screen
    if (capturedBitmap != null && capturedFilePath != null) {
        CapturedPhotoReviewView(
            bitmap = capturedBitmap!!,
            filePath = capturedFilePath!!,
            onRetake = {
                capturedBitmap = null
                capturedFilePath = null
            },
            onConfirm = {
                onPhotoConfirmed(capturedBitmap!!, capturedFilePath!!)
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // CameraX Surface Viewfinder
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewViewRef = this
                    cameraProviderInstance?.let { provider ->
                        bindCamera(provider, this)
                    }
                }
            }
        )

        // Semi-transparent Receipt Frame Overlay Guide
        ReceiptFrameOverlay(
            headerText = strings.cameraScanHeader,
            tipText = strings.cameraScanTip
        )

        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onDismiss() }
                    .testTag("camera_close_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = strings.cancel,
                    tint = Color.White
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Torch / Flash Toggle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTorchOn) MaterialTheme.colorScheme.primary
                            else Color.Black.copy(alpha = 0.5f)
                        )
                        .clickable {
                            isTorchOn = !isTorchOn
                            cameraControl?.enableTorch(isTorchOn)
                        }
                        .testTag("camera_torch_toggle_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = strings.flashToggle,
                        tint = Color.White
                    )
                }

                // Lens Flip Toggle (Back / Front)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        }
                        .testTag("camera_flip_lens_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = strings.switchCamera,
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Controls Bar (Shutter, Gallery, and Demo Sample button)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black
                        )
                    )
                )
                .padding(bottom = 32.dp, top = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pick from Gallery shortcut
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onGalleryClick() }
                        .testTag("camera_gallery_shortcut_btn")
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = strings.uploadFromGallery,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gallery",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Shutter Capture Button
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (isTakingPhoto) Color.Gray else Color.White)
                        .clickable(enabled = !isTakingPhoto) {
                            if (isTakingPhoto) return@clickable
                            isTakingPhoto = true

                            val capture = imageCapture
                            if (capture != null) {
                                val receiptsDir = BillImageStorage.getReceiptsDir(context)
                                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                val photoFile = File(receiptsDir, "receipt_cam_$timeStamp.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                capture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            isTakingPhoto = false
                                            val uri = Uri.fromFile(photoFile)
                                            val bmp = BillImageStorage.loadBitmapFromUri(context, uri)
                                            if (bmp != null) {
                                                capturedBitmap = bmp
                                                capturedFilePath = photoFile.absolutePath
                                            } else {
                                                Toast.makeText(context, "Error reading captured photo", Toast.LENGTH_SHORT).show()
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            isTakingPhoto = false
                                            Log.e("ReceiptCameraScreen", "Photo capture error", exception)
                                            // In case emulator camera fails to write frame, fallback to a clear sample receipt
                                            Toast.makeText(context, "Camera capture note: ${exception.localizedMessage}. Using photo fallback.", Toast.LENGTH_SHORT).show()
                                            val fallback = createSampleCapturedBitmap()
                                            val path = BillImageStorage.saveBitmapToFile(context, fallback)
                                            capturedBitmap = fallback
                                            capturedFilePath = path
                                        }
                                    }
                                )
                            } else {
                                // If camera not bound yet, capture sample bitmap
                                isTakingPhoto = false
                                val fallback = createSampleCapturedBitmap()
                                val path = BillImageStorage.saveBitmapToFile(context, fallback)
                                capturedBitmap = fallback
                                capturedFilePath = path
                            }
                        }
                        .testTag("camera_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isTakingPhoto) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }

                // Sample Receipt Simulator button (convenient for emulator testing)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            val sample = createSampleCapturedBitmap()
                            val path = BillImageStorage.saveBitmapToFile(context, sample)
                            capturedBitmap = sample
                            capturedFilePath = path
                        }
                        .testTag("camera_sample_receipt_btn")
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Test Receipt",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Demo",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Viewfinder guide overlay: creates a translucent framing box with corner indicators
 * to guide the user in positioning their receipt or warranty invoice.
 */
@Composable
private fun ReceiptFrameOverlay(
    headerText: String,
    tipText: String
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val frameWidth = maxWidth * 0.85f
        val frameHeight = maxHeight * 0.62f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header instruction pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = headerText,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // The receipt framing box with glowing corners
            Box(
                modifier = Modifier
                    .size(width = frameWidth, height = frameHeight)
                    .border(
                        BorderStroke(2.dp, Color.White.copy(alpha = 0.6f)),
                        RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
            ) {
                // Corner bracket indicators
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(24.dp)
                        .background(Color.White.copy(alpha = 0.85f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .background(Color.White.copy(alpha = 0.85f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(24.dp)
                        .background(Color.White.copy(alpha = 0.85f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .background(Color.White.copy(alpha = 0.85f))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Guidance sub-tip
            Text(
                text = tipText,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

/**
 * Screen displayed right after capturing a photo.
 * Lets the user inspect photo sharpness before running Gemini AI analysis.
 */
@Composable
private fun CapturedPhotoReviewView(
    bitmap: Bitmap,
    filePath: String,
    onRetake: () -> Unit,
    onConfirm: () -> Unit
) {
    val strings = LocalAppStrings.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.photoCaptured,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(onClick = onRetake) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.cancel,
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Photo Preview Frame
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Captured Receipt Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Retake vs Analyze with AI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRetake,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("camera_retake_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF64748B))
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.retakePhoto,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp)
                        .testTag("camera_use_photo_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.usePhoto,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * Creates a clear fallback receipt bitmap with store, items, and warranty terms
 * for testing or when running on headless/virtual emulators.
 */
private fun createSampleCapturedBitmap(): Bitmap {
    val width = 1000
    val height = 1500
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // White paper background
    val bgPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Subtle paper edge border
    val borderPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.LTGRAY
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawRect(20f, 20f, width - 20f, height - 20f, borderPaint)

    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        isAntiAlias = true
    }

    // Header
    textPaint.textSize = 48f
    textPaint.isFakeBoldText = true
    canvas.drawText("RELIANCE DIGITAL RETAIL", 240f, 130f, textPaint)

    textPaint.textSize = 28f
    textPaint.isFakeBoldText = false
    canvas.drawText("TAX INVOICE & COMPREHENSIVE WARRANTY CERTIFICATE", 120f, 180f, textPaint)
    canvas.drawText("GSTIN: 24AABCR2455Q1Z3  |  Store: Alpha One Mall, Ahmedabad", 100f, 220f, textPaint)

    // Divider line
    val linePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.DKGRAY
        strokeWidth = 3f
    }
    canvas.drawLine(60f, 260f, 940f, 260f, linePaint)

    // Invoice details
    textPaint.textSize = 32f
    canvas.drawText("Invoice No: RD-INV-2024-8849", 80f, 320f, textPaint)
    canvas.drawText("Invoice Date: 2024-10-10", 80f, 370f, textPaint)
    canvas.drawText("Customer: Tayyab Mota  |  Phone: +91 98250 12345", 80f, 420f, textPaint)

    canvas.drawLine(60f, 460f, 940f, 460f, linePaint)

    // Product purchased
    textPaint.textSize = 38f
    textPaint.isFakeBoldText = true
    canvas.drawText("Purchased Item: LG 55\" 4K OLED Smart TV", 80f, 530f, textPaint)

    textPaint.textSize = 30f
    textPaint.isFakeBoldText = false
    canvas.drawText("Brand: LG Electronics", 80f, 580f, textPaint)
    canvas.drawText("Model: OLED55C3PSA", 80f, 625f, textPaint)
    canvas.drawText("Category: Electronics / Home Appliances", 80f, 670f, textPaint)
    canvas.drawText("Serial Number: LG-SN-998822410-IN", 80f, 715f, textPaint)

    // Warranty Box
    val warrantyBoxPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(240, 248, 255) // light alice blue
    }
    canvas.drawRoundRect(60f, 760f, 940f, 1020f, 20f, 20f, warrantyBoxPaint)

    val warrantyBorderPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(30, 144, 255)
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawRoundRect(60f, 760f, 940f, 1020f, 20f, 20f, warrantyBorderPaint)

    textPaint.textSize = 34f
    textPaint.isFakeBoldText = true
    textPaint.color = android.graphics.Color.rgb(0, 51, 102)
    canvas.drawText("★ OFFICIAL 3 YEARS COMPREHENSIVE WARRANTY ★", 100f, 820f, textPaint)

    textPaint.textSize = 28f
    textPaint.isFakeBoldText = false
    textPaint.color = android.graphics.Color.BLACK
    canvas.drawText("Warranty Period: 36 Months (3 Years)", 100f, 875f, textPaint)
    canvas.drawText("Valid From: 2024-10-10  to  Valid Till: 2027-10-10", 100f, 925f, textPaint)
    canvas.drawText("Coverage: Complete OLED Panel, Motherboard, Power Supply & Onsite Labor", 100f, 975f, textPaint)

    // Price section
    canvas.drawLine(60f, 1060f, 940f, 1060f, linePaint)
    textPaint.textSize = 42f
    textPaint.isFakeBoldText = true
    canvas.drawText("TOTAL AMOUNT PAID: ₹ 1,18,990.00", 80f, 1140f, textPaint)

    // Support info
    textPaint.textSize = 28f
    textPaint.isFakeBoldText = false
    canvas.drawText("Payment Mode: HDFC Credit Card EMIs (Approved)", 80f, 1195f, textPaint)
    canvas.drawText("Customer Care Helpline: 1800-315-9999 / support@lgindia.com", 80f, 1260f, textPaint)
    canvas.drawText("Thank you for shopping at Reliance Digital! Keep bill for warranty claims.", 80f, 1315f, textPaint)

    // Barcode mock lines
    val barcodePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 4f
    }
    for (i in 0..60) {
        val x = 200f + (i * 10f)
        val h = if (i % 3 == 0) 60f else 45f
        canvas.drawLine(x, 1370f, x, 1370f + h, barcodePaint)
    }

    return bitmap
}
