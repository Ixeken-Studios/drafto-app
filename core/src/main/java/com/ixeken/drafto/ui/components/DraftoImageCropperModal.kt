package com.ixeken.drafto.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ixeken.drafto.core.R
import com.ixeken.drafto.ui.theme.DraftoAquaCyan
import com.ixeken.drafto.ui.theme.DraftoShapePill
import com.ixeken.drafto.ui.theme.DraftoTheme
import com.ixeken.drafto.ui.theme.LocalDraftoBlurEnabled
import com.ixeken.drafto.ui.utils.DraftoImageCropUtils
import kotlinx.coroutines.launch

enum class DraftoCropShape {
    CIRCLE_1_1,
    RECT_9_16
}

@Composable
fun DraftoImageCropperModal(
    imageUri: Uri?,
    cropShape: DraftoCropShape,
    title: String = if (cropShape == DraftoCropShape.CIRCLE_1_1) {
        stringResource(R.string.title_adjust_photo)
    } else {
        stringResource(R.string.title_adjust_wallpaper)
    },
    folderName: String = if (cropShape == DraftoCropShape.CIRCLE_1_1) "avatars" else "backgrounds",
    isBlurEnabled: Boolean = LocalDraftoBlurEnabled.current,
    onDismiss: () -> Unit,
    onConfirmCrop: (Uri) -> Unit
) {
    if (imageUri == null) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var sourceBitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        isLoading = true
        sourceBitmap = DraftoImageCropUtils.loadBitmap(context, imageUri)
        isLoading = false
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val extraColors = DraftoTheme.extraColors
        val navShadow = DraftoTheme.colors.navBarShadow

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F0F12))
        ) {
            if (isLoading || sourceBitmap == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = DraftoAquaCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else {
                val bitmap = sourceBitmap!!
                val density = LocalDensity.current

                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val containerWidthPx = with(density) { maxWidth.toPx() }
                    val containerHeightPx = with(density) { maxHeight.toPx() }

                    // Calcular tamaño del marco de corte
                    val (frameWidthPx, frameHeightPx) = remember(cropShape, containerWidthPx, containerHeightPx) {
                        if (cropShape == DraftoCropShape.CIRCLE_1_1) {
                            val size = minOf(containerWidthPx * 0.80f, containerHeightPx * 0.50f, with(density) { 320.dp.toPx() })
                            Pair(size, size)
                        } else {
                            val h = minOf(containerHeightPx * 0.62f, with(density) { 480.dp.toPx() })
                            val w = h * (9f / 16f)
                            Pair(w, h)
                        }
                    }

                    val frameCenterX = containerWidthPx / 2f
                    val frameCenterY = containerHeightPx / 2f
                    val frameRect = remember(frameCenterX, frameCenterY, frameWidthPx, frameHeightPx) {
                        Rect(
                            left = frameCenterX - frameWidthPx / 2f,
                            top = frameCenterY - frameHeightPx / 2f,
                            right = frameCenterX + frameWidthPx / 2f,
                            bottom = frameCenterY + frameHeightPx / 2f
                        )
                    }

                    val bmpW = bitmap.width.toFloat()
                    val bmpH = bitmap.height.toFloat()

                    val minScale = remember(frameWidthPx, frameHeightPx, bmpW, bmpH) {
                        maxOf(frameWidthPx / bmpW, frameHeightPx / bmpH)
                    }

                    var userScale by remember { mutableFloatStateOf(1f) }
                    var panOffset by remember { mutableStateOf(Offset.Zero) }

                    val totalScale = minScale * userScale
                    val renderedW = bmpW * totalScale
                    val renderedH = bmpH * totalScale

                    // Límites de desplazamiento para que el marco siempre quede cubierto
                    val maxPanX = (renderedW - frameWidthPx) / 2f
                    val maxPanY = (renderedH - frameHeightPx) / 2f

                    val clampedPanX = panOffset.x.coerceIn(-maxPanX, maxPanX)
                    val clampedPanY = panOffset.y.coerceIn(-maxPanY, maxPanY)

                    // Capa de Imagen manipulable con gestos
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(minScale) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (userScale * zoom).coerceIn(1f, 3.5f)
                                    userScale = newScale
                                    panOffset = Offset(
                                        x = panOffset.x + pan.x,
                                        y = panOffset.y + pan.y
                                    )
                                }
                            }
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(
                                    width = with(density) { bmpW.toDp() },
                                    height = with(density) { bmpH.toDp() }
                                )
                                .graphicsLayer {
                                    scaleX = totalScale
                                    scaleY = totalScale
                                    translationX = clampedPanX
                                    translationY = clampedPanY
                                }
                        )

                        // Máscara y Guías
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasW = size.width
                            val canvasH = size.height

                            val path = Path().apply {
                                addRect(Rect(0f, 0f, canvasW, canvasH))
                            }

                            val cropPath = Path().apply {
                                if (cropShape == DraftoCropShape.CIRCLE_1_1) {
                                    addOval(frameRect)
                                } else {
                                    addRoundRect(
                                        RoundRect(
                                            rect = frameRect,
                                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                                        )
                                    )
                                }
                            }

                            // Dibujar fondo oscurecido
                            clipPath(cropPath, clipOp = ClipOp.Difference) {
                                drawRect(color = Color.Black.copy(alpha = 0.70f))
                            }

                            // Marco exterior nítido
                            if (cropShape == DraftoCropShape.CIRCLE_1_1) {
                                drawOval(
                                    color = Color.White.copy(alpha = 0.85f),
                                    topLeft = Offset(frameRect.left, frameRect.top),
                                    size = androidx.compose.ui.geometry.Size(frameRect.width, frameRect.height),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            } else {
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.85f),
                                    topLeft = Offset(frameRect.left, frameRect.top),
                                    size = androidx.compose.ui.geometry.Size(frameRect.width, frameRect.height),
                                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }

                            // Cuadrícula de tercios sutil
                            val thirdW = frameRect.width / 3f
                            val thirdH = frameRect.height / 3f
                            val gridColor = Color.White.copy(alpha = 0.20f)
                            val gridStroke = 1.dp.toPx()

                            drawLine(gridColor, Offset(frameRect.left + thirdW, frameRect.top), Offset(frameRect.left + thirdW, frameRect.bottom), gridStroke)
                            drawLine(gridColor, Offset(frameRect.left + 2 * thirdW, frameRect.top), Offset(frameRect.left + 2 * thirdW, frameRect.bottom), gridStroke)
                            drawLine(gridColor, Offset(frameRect.left, frameRect.top + thirdH), Offset(frameRect.right, frameRect.top + thirdH), gridStroke)
                            drawLine(gridColor, Offset(frameRect.left, frameRect.top + 2 * thirdH), Offset(frameRect.right, frameRect.top + 2 * thirdH), gridStroke)
                        }
                    }

                    // Controles Superiores e Inferiores
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(
                                WindowInsets.statusBars
                                    .union(WindowInsets.displayCutout)
                                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                            )
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // TopBar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .shadow(elevation = 6.dp, shape = CircleShape, spotColor = navShadow, ambientColor = navShadow)
                                    .clip(CircleShape)
                                    .background(DraftoTheme.colors.cardSurface)
                                    .clickable { if (!isSaving) onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_cancel),
                                    tint = extraColors.onNavBarSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .shadow(elevation = 6.dp, shape = CircleShape, spotColor = navShadow, ambientColor = navShadow)
                                    .clip(CircleShape)
                                    .background(DraftoAquaCyan)
                                    .clickable {
                                        if (!isSaving) {
                                            isSaving = true
                                            coroutineScope.launch {
                                                val imageDisplayLeft = (containerWidthPx / 2f + clampedPanX) - renderedW / 2f
                                                val imageDisplayTop = (containerHeightPx / 2f + clampedPanY) - renderedH / 2f

                                                val normLeft = ((frameRect.left - imageDisplayLeft) / renderedW).coerceIn(0f, 1f)
                                                val normTop = ((frameRect.top - imageDisplayTop) / renderedH).coerceIn(0f, 1f)
                                                val normWidth = (frameRect.width / renderedW).coerceIn(0f, 1f - normLeft)
                                                val normHeight = (frameRect.height / renderedH).coerceIn(0f, 1f - normTop)

                                                val cropRectPercent = Rect(
                                                    left = normLeft,
                                                    top = normTop,
                                                    right = normLeft + normWidth,
                                                    bottom = normTop + normHeight
                                                )

                                                val croppedUri = DraftoImageCropUtils.cropAndSaveBitmap(
                                                    context = context,
                                                    sourceBitmap = bitmap,
                                                    cropRectPercent = cropRectPercent,
                                                    folderName = folderName
                                                )
                                                isSaving = false
                                                if (croppedUri != null) {
                                                    onConfirmCrop(croppedUri)
                                                } else {
                                                    onDismiss()
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        color = Color.Black,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(R.string.action_apply),
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // Bottom Hint Pill
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .shadow(elevation = 4.dp, shape = DraftoShapePill)
                                    .clip(DraftoShapePill)
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.hint_crop_drag),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
